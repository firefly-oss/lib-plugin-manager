/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.core.plugin.debug;

import com.firefly.core.plugin.api.PluginDebugger;
import com.firefly.core.plugin.api.PluginManager;
import com.firefly.core.plugin.config.PluginManagerProperties;
import com.firefly.core.plugin.debug.jdi.DebuggerConnection;
import com.firefly.core.plugin.debug.jdi.DebuggerVMManager;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.model.PluginDebugEvent.Type;
import com.firefly.core.plugin.model.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of the PluginDebugger interface.
 */
@Component
public class DefaultPluginDebugger implements PluginDebugger, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginDebugger.class);

    private final PluginManager pluginManager;
    private final PluginEventBus eventBus;
    private final PluginManagerProperties properties;
    private final Map<String, DebugSession> sessions = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Disposable sessionCleanupDisposable;
    private final DebuggerVMManager vmManager;

    /**
     * Creates a new DefaultPluginDebugger.
     *
     * @param pluginManager the plugin manager
     * @param eventBus the plugin event bus
     * @param properties the plugin manager properties
     */
    @Autowired
    public DefaultPluginDebugger(
            PluginManager pluginManager,
            PluginEventBus eventBus,
            PluginManagerProperties properties) {
        this.pluginManager = pluginManager;
        this.eventBus = eventBus;
        this.properties = properties;
        this.vmManager = new DebuggerVMManager(eventBus);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    /**
     * Starts the debugger.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting plugin debugger");

            // Start session cleanup at regular intervals
            long sessionTimeoutMs = properties.getDebugger().getSessionTimeoutMs();

            sessionCleanupDisposable = Flux.interval(Duration.ofMinutes(5))
                    .takeWhile(i -> running.get())
                    .flatMap(i -> cleanupInactiveSessions(sessionTimeoutMs))
                    .subscribe(
                            count -> {
                                if (count > 0) {
                                    logger.info("Cleaned up {} inactive debug sessions", count);
                                }
                            },
                            e -> logger.error("Error in session cleanup", e),
                            () -> logger.info("Session cleanup completed")
                    );
        }
    }

    /**
     * Stops the debugger.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping plugin debugger");

            // Dispose of the session cleanup
            if (sessionCleanupDisposable != null && !sessionCleanupDisposable.isDisposed()) {
                sessionCleanupDisposable.dispose();
                sessionCleanupDisposable = null;
            }

            // Disconnect all VM connections
            vmManager.disconnectAll().subscribe();

            // Stop all active sessions
            for (DebugSession session : sessions.values()) {
                if (session.isActive()) {
                    session.setActive(false);

                    // Publish a session ended event
                    Map<String, Object> details = new HashMap<>();
                    details.put("reason", "Debugger stopped");

                    PluginDebugEvent event = new PluginDebugEvent(
                            session.getId(),
                            session.getPluginId(),
                            Type.SESSION_ENDED,
                            "Debug session ended: Debugger stopped",
                            details
                    );

                    eventBus.publish(event).subscribe();
                }
            }

            // Clear the sessions map
            sessions.clear();
        }
    }

    @Override
    public Mono<String> startDebugSession(String pluginId) {
        return Mono.defer(() -> {
            // Check if debugging is enabled
            if (!properties.getDebugger().isEnabled()) {
                return Mono.error(new IllegalStateException("Debugging is not enabled"));
            }

            // Check if debugging is enabled for this plugin
            return isDebuggingEnabled(pluginId)
                    .flatMap(enabled -> {
                        if (!enabled) {
                            return Mono.error(new IllegalStateException("Debugging is not enabled for plugin: " + pluginId));
                        }

                        // Check if we've reached the maximum number of concurrent sessions
                        int maxSessions = properties.getDebugger().getMaxConcurrentSessions();
                        long activeSessions = sessions.values().stream()
                                .filter(DebugSession::isActive)
                                .count();

                        if (activeSessions >= maxSessions) {
                            return Mono.error(new IllegalStateException("Maximum number of concurrent debug sessions reached"));
                        }

                        // Get the debug port from properties or use a default
                        int debugPort = properties.getDebugger().getPort();

                        // Create a new debug session with the port
                        DebugSession session = new DebugSession(pluginId, debugPort);
                        sessions.put(session.getId(), session);

                        // Publish a session started event
                        Map<String, Object> details = new HashMap<>();
                        details.put("pluginId", pluginId);
                        details.put("debugPort", debugPort);

                        PluginDebugEvent event = new PluginDebugEvent(
                                session.getId(),
                                pluginId,
                                Type.SESSION_STARTED,
                                "Debug session started for plugin: " + pluginId,
                                details
                        );

                        // Connect to the VM
                        return vmManager.connectToVM(session.getId(), pluginId, debugPort)
                                .then(eventBus.publish(event))
                                .then(Mono.fromCallable(() -> {
                                    if (properties.getDebugger().isLogDebugEvents()) {
                                        logger.info("Started debug session: {} for plugin: {} on port {}",
                                                session.getId(), pluginId, debugPort);
                                    }
                                    session.setJdiConnected(true);
                                    return session.getId();
                                }));
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> stopDebugSession(String sessionId) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Disconnect from the VM if connected
            Mono<Void> disconnectMono = Mono.empty();
            if (session.isJdiConnected()) {
                disconnectMono = vmManager.disconnectFromVM(sessionId);
                session.setJdiConnected(false);
            }

            session.setActive(false);

            // Publish a session ended event
            Map<String, Object> details = new HashMap<>();
            details.put("reason", "User requested");

            PluginDebugEvent event = new PluginDebugEvent(
                    sessionId,
                    session.getPluginId(),
                    Type.SESSION_ENDED,
                    "Debug session ended: User requested",
                    details
            );

            return disconnectMono
                    .then(eventBus.publish(event))
                    .then(Mono.defer(() -> {
                        if (properties.getDebugger().isLogDebugEvents()) {
                            logger.info("Stopped debug session: {} for plugin: {}", sessionId, session.getPluginId());
                        }

                        // Remove the session
                        sessions.remove(sessionId);
                        return Mono.<Void>empty();
                    }));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Map.Entry<String, String>> getActiveSessions() {
        return Flux.fromIterable(sessions.values())
                .filter(DebugSession::isActive)
                .map(session -> (Map.Entry<String, String>) new AbstractMap.SimpleEntry<>(session.getId(), session.getPluginId()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> setBreakpoint(String sessionId, String className, int lineNumber) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if breakpoints are enabled
            if (!properties.getDebugger().isBreakpointsEnabled()) {
                return Mono.error(new IllegalStateException("Breakpoints are not enabled"));
            }

            // Check if breakpoints are enabled for this plugin
            String pluginId = session.getPluginId();
            PluginManagerProperties.PluginDebuggerProperties pluginProps = properties.getDebugger().getPlugins().get(pluginId);
            if (pluginProps != null && pluginProps.getBreakpointsEnabled() != null && !pluginProps.getBreakpointsEnabled()) {
                return Mono.error(new IllegalStateException("Breakpoints are not enabled for plugin: " + pluginId));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Mono.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Mono.error(new IllegalStateException("No VM connection found for debug session"));
            }

            // Set the breakpoint in the VM
            return connection.setBreakpoint(className, lineNumber)
                    .flatMap(jdiBreakpointId -> {
                        // Add the breakpoint to the session
                        String breakpointId = session.addBreakpoint(className, lineNumber);

                        // Store the JDI breakpoint ID in the session data
                        session.putData("jdi-breakpoint-" + breakpointId, jdiBreakpointId);

                        if (properties.getDebugger().isLogDebugEvents()) {
                            logger.info("Set breakpoint: {} at {}:{} in session: {}", breakpointId, className, lineNumber, sessionId);
                        }

                        return Mono.just(breakpointId);
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> removeBreakpoint(String sessionId, String breakpointId) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Mono.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Mono.error(new IllegalStateException("No VM connection found for debug session"));
            }

            // Get the JDI breakpoint ID from the session data
            String jdiBreakpointId = (String) session.getData("jdi-breakpoint-" + breakpointId);
            if (jdiBreakpointId == null) {
                return Mono.error(new IllegalArgumentException("JDI breakpoint not found: " + breakpointId));
            }

            // Remove the breakpoint from the VM
            return connection.removeBreakpoint(jdiBreakpointId)
                    .then(Mono.defer(() -> {
                        // Remove the breakpoint from the session
                        boolean removed = session.removeBreakpoint(breakpointId);
                        if (!removed) {
                            return Mono.error(new IllegalArgumentException("Breakpoint not found: " + breakpointId));
                        }

                        // Remove the JDI breakpoint ID from the session data
                        session.removeData("jdi-breakpoint-" + breakpointId);

                        if (properties.getDebugger().isLogDebugEvents()) {
                            logger.info("Removed breakpoint: {} in session: {}", breakpointId, sessionId);
                        }

                        return Mono.<Void>empty();
                    }));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Map<String, Object>> getBreakpoints(String sessionId) {
        return Flux.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Flux.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                // If not connected to the VM, just return the session breakpoints
                return Flux.fromIterable(session.getBreakpoints().values())
                        .map(DebugSession.Breakpoint::toMap);
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Flux.error(new IllegalStateException("No VM connection found for debug session"));
            }

            // Get the breakpoints from the VM
            Map<String, Map<String, Object>> jdiBreakpoints = connection.getBreakpoints();

            // Merge the session breakpoints with the JDI breakpoints
            return Flux.fromIterable(session.getBreakpoints().values())
                    .map(breakpoint -> {
                        Map<String, Object> breakpointMap = breakpoint.toMap();

                        // Get the JDI breakpoint ID from the session data
                        String jdiBreakpointId = (String) session.getData("jdi-breakpoint-" + breakpoint.getId());
                        if (jdiBreakpointId != null && jdiBreakpoints.containsKey(jdiBreakpointId)) {
                            // Add JDI-specific information
                            Map<String, Object> jdiBreakpoint = jdiBreakpoints.get(jdiBreakpointId);
                            breakpointMap.put("jdiBreakpointId", jdiBreakpointId);
                            breakpointMap.put("methodName", jdiBreakpoint.get("methodName"));
                            breakpointMap.put("enabled", jdiBreakpoint.get("enabled"));
                        }

                        return breakpointMap;
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> continueExecution(String sessionId) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Mono.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Mono.error(new IllegalStateException("No VM connection found for debug session"));
            }

            // Continue execution in the VM
            return connection.continueExecution()
                    .doOnSuccess(v -> {
                        if (properties.getDebugger().isLogDebugEvents()) {
                            logger.info("Continued execution in session: {}", sessionId);
                        }
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> stepOver(String sessionId) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if step execution is enabled
            if (!properties.getDebugger().isStepExecutionEnabled()) {
                return Mono.error(new IllegalStateException("Step execution is not enabled"));
            }

            // Check if step execution is enabled for this plugin
            String pluginId = session.getPluginId();
            PluginManagerProperties.PluginDebuggerProperties pluginProps = properties.getDebugger().getPlugins().get(pluginId);
            if (pluginProps != null && pluginProps.getStepExecutionEnabled() != null && !pluginProps.getStepExecutionEnabled()) {
                return Mono.error(new IllegalStateException("Step execution is not enabled for plugin: " + pluginId));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Mono.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Mono.error(new IllegalStateException("No VM connection found for debug session"));
            }

            // Step over in the VM
            return connection.stepOver()
                    .doOnSuccess(v -> {
                        if (properties.getDebugger().isLogDebugEvents()) {
                            logger.info("Stepped over in session: {}", sessionId);
                        }
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> stepInto(String sessionId) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if step execution is enabled
            if (!properties.getDebugger().isStepExecutionEnabled()) {
                return Mono.error(new IllegalStateException("Step execution is not enabled"));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Mono.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Mono.error(new IllegalStateException("No VM connection found for debug session"));
            }

            // Step into in the VM
            return connection.stepInto()
                    .doOnSuccess(v -> {
                        if (properties.getDebugger().isLogDebugEvents()) {
                            logger.info("Stepped into in session: {}", sessionId);
                        }
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> stepOut(String sessionId) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if step execution is enabled
            if (!properties.getDebugger().isStepExecutionEnabled()) {
                return Mono.error(new IllegalStateException("Step execution is not enabled"));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Mono.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Mono.error(new IllegalStateException("No VM connection found for debug session"));
            }

            // Step out in the VM
            return connection.stepOut()
                    .doOnSuccess(v -> {
                        if (properties.getDebugger().isLogDebugEvents()) {
                            logger.info("Stepped out in session: {}", sessionId);
                        }
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Object> getVariableValue(String sessionId, String variableName) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if variable inspection is enabled
            if (!properties.getDebugger().isVariableInspectionEnabled()) {
                return Mono.error(new IllegalStateException("Variable inspection is not enabled"));
            }

            // Check if variable inspection is enabled for this plugin
            String pluginId = session.getPluginId();
            PluginManagerProperties.PluginDebuggerProperties pluginProps = properties.getDebugger().getPlugins().get(pluginId);
            if (pluginProps != null && pluginProps.getVariableInspectionEnabled() != null && !pluginProps.getVariableInspectionEnabled()) {
                return Mono.error(new IllegalStateException("Variable inspection is not enabled for plugin: " + pluginId));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Mono.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Mono.error(new IllegalStateException("No VM connection found for debug session"));
            }

            try {
                // Get the variable value from the VM
                Object value = connection.getVariableValue(variableName);

                if (properties.getDebugger().isLogDebugEvents()) {
                    logger.info("Got value of variable: {} in session: {}", variableName, sessionId);
                }

                // Publish a variable inspected event
                Map<String, Object> details = new HashMap<>();
                details.put("variableName", variableName);
                details.put("value", value);

                PluginDebugEvent event = new PluginDebugEvent(
                        sessionId,
                        pluginId,
                        Type.VARIABLE_INSPECTED,
                        "Variable inspected: " + variableName,
                        details
                );

                return eventBus.publish(event)
                        .then(Mono.just(value));
            } catch (Exception e) {
                logger.error("Failed to get variable value: {}", variableName, e);
                return Mono.error(new IllegalArgumentException("Failed to get variable value: " + e.getMessage(), e));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Map.Entry<String, Object>> getLocalVariables(String sessionId) {
        return Flux.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Flux.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if variable inspection is enabled
            if (!properties.getDebugger().isVariableInspectionEnabled()) {
                return Flux.error(new IllegalStateException("Variable inspection is not enabled"));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Flux.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Flux.error(new IllegalStateException("No VM connection found for debug session"));
            }

            try {
                // Get the local variables from the VM
                Map<String, Object> variables = connection.getLocalVariables();

                if (properties.getDebugger().isLogDebugEvents()) {
                    logger.info("Got local variables in session: {}", sessionId);
                }

                return Flux.fromIterable(variables.entrySet());
            } catch (Exception e) {
                logger.error("Failed to get local variables", e);
                return Flux.error(new RuntimeException("Failed to get local variables: " + e.getMessage(), e));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Object> evaluateExpression(String sessionId, String expression) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Mono.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Mono.error(new IllegalStateException("No VM connection found for debug session"));
            }

            try {
                // Evaluate the expression in the VM
                Object result = connection.evaluateExpression(expression);

                if (properties.getDebugger().isLogDebugEvents()) {
                    logger.info("Evaluated expression: {} in session: {}", expression, sessionId);
                }

                // Publish an expression evaluated event
                Map<String, Object> details = new HashMap<>();
                details.put("expression", expression);
                details.put("result", result);

                PluginDebugEvent event = new PluginDebugEvent(
                        sessionId,
                        session.getPluginId(),
                        Type.EXPRESSION_EVALUATED,
                        "Expression evaluated: " + expression,
                        details
                );

                return eventBus.publish(event)
                        .then(Mono.just(result));
            } catch (Exception e) {
                logger.error("Failed to evaluate expression: {}", expression, e);
                return Mono.error(new RuntimeException("Failed to evaluate expression: " + e.getMessage(), e));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Map<String, Object>> getStackTrace(String sessionId) {
        return Flux.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Flux.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            // Check if the session is connected to the VM
            if (!session.isJdiConnected()) {
                return Flux.error(new IllegalStateException("Debug session is not connected to the VM"));
            }

            // Get the VM connection
            DebuggerConnection connection = vmManager.getConnection(sessionId);
            if (connection == null) {
                return Flux.error(new IllegalStateException("No VM connection found for debug session"));
            }

            try {
                // Get the stack trace from the VM
                List<Map<String, Object>> stackTrace = connection.getStackTrace();

                if (properties.getDebugger().isLogDebugEvents()) {
                    logger.info("Got stack trace in session: {}", sessionId);
                }

                return Flux.fromIterable(stackTrace);
            } catch (Exception e) {
                logger.error("Failed to get stack trace", e);
                return Flux.error(new RuntimeException("Failed to get stack trace: " + e.getMessage(), e));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PluginDescriptor> getPluginInfo(String sessionId) {
        return Mono.defer(() -> {
            DebugSession session = getSession(sessionId);
            if (session == null) {
                return Mono.error(new IllegalArgumentException("Debug session not found: " + sessionId));
            }

            return pluginManager.getPlugin(session.getPluginId());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> isDebuggingEnabled(String pluginId) {
        return Mono.fromCallable(() -> {
            // Check if debugging is enabled globally
            if (!properties.getDebugger().isEnabled()) {
                return false;
            }

            // Check if debugging is enabled for this plugin
            PluginManagerProperties.PluginDebuggerProperties pluginProps = properties.getDebugger().getPlugins().get(pluginId);
            if (pluginProps != null && pluginProps.getEnabled() != null) {
                return pluginProps.getEnabled();
            }

            // Default to enabled if not specified
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> setDebuggingEnabled(String pluginId, boolean enabled) {
        return Mono.defer(() -> {
            // Get or create plugin-specific properties
            PluginManagerProperties.PluginDebuggerProperties pluginProps = properties.getDebugger().getPlugins()
                    .computeIfAbsent(pluginId, k -> new PluginManagerProperties.PluginDebuggerProperties());

            // Set the enabled flag
            pluginProps.setEnabled(enabled);

            if (properties.getDebugger().isLogDebugEvents()) {
                logger.info("Set debugging {} for plugin: {}", enabled ? "enabled" : "disabled", pluginId);
            }

            return Mono.<Void>empty();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Gets a debug session by its ID.
     *
     * @param sessionId the session ID
     * @return the debug session, or null if not found
     */
    private DebugSession getSession(String sessionId) {
        DebugSession session = sessions.get(sessionId);
        if (session != null && !session.isActive()) {
            return null;
        }
        return session;
    }

    /**
     * Cleans up inactive sessions.
     *
     * @param sessionTimeoutMs the session timeout in milliseconds
     * @return a Mono that emits the number of sessions cleaned up
     */
    private Mono<Integer> cleanupInactiveSessions(long sessionTimeoutMs) {
        return Mono.fromCallable(() -> {
            int count = 0;
            Instant now = Instant.now();

            for (DebugSession session : sessions.values()) {
                if (session.isActive()) {
                    // Check if the session has timed out
                    Duration inactivity = Duration.between(session.getLastActivityTime(), now);
                    if (inactivity.toMillis() > sessionTimeoutMs) {
                        session.setActive(false);

                        // Publish a session ended event
                        Map<String, Object> details = new HashMap<>();
                        details.put("reason", "Session timeout");

                        PluginDebugEvent event = new PluginDebugEvent(
                                session.getId(),
                                session.getPluginId(),
                                Type.SESSION_ENDED,
                                "Debug session ended: Session timeout",
                                details
                        );

                        eventBus.publish(event).subscribe();

                        if (properties.getDebugger().isLogDebugEvents()) {
                            logger.info("Session timed out: {} for plugin: {}", session.getId(), session.getPluginId());
                        }

                        count++;
                    }
                }
            }

            // Remove inactive sessions
            sessions.entrySet().removeIf(entry -> !entry.getValue().isActive());

            return count;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
