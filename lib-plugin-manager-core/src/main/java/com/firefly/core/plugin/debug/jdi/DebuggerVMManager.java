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


package com.firefly.core.plugin.debug.jdi;

import com.firefly.core.plugin.debug.PluginDebugEvent;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.model.PluginDebugEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages JVM connections for debugging plugins.
 */
public class DebuggerVMManager {
    private static final Logger logger = LoggerFactory.getLogger(DebuggerVMManager.class);

    private final PluginEventBus eventBus;
    private final Map<String, DebuggerConnection> connections = new ConcurrentHashMap<>();

    /**
     * Creates a new DebuggerVMManager.
     *
     * @param eventBus the plugin event bus
     */
    public DebuggerVMManager(PluginEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Connects to a plugin's JVM.
     *
     * @param sessionId the debug session ID
     * @param pluginId the plugin ID
     * @param port the port to connect to
     * @return a Mono that completes when the connection is established
     */
    public Mono<Void> connectToVM(String sessionId, String pluginId, int port) {
        return Mono.defer(() -> {
            // Check if we already have a connection for this session
            if (connections.containsKey(sessionId)) {
                logger.warn("Debug session already has a VM connection: {}", sessionId);
                return Mono.empty();
            }

            // Create a new connection
            DebuggerConnection connection = new DebuggerConnection(sessionId, pluginId, port);
            
            // Set up the event handler
            connection.setEventHandler(new DebuggerConnection.EventHandler() {
                @Override
                public void onBreakpointHit(String className, int lineNumber) {
                    Map<String, Object> details = new HashMap<>();
                    details.put("className", className);
                    details.put("lineNumber", lineNumber);
                    
                    PluginDebugEvent event = new PluginDebugEvent(
                            sessionId,
                            pluginId,
                            Type.BREAKPOINT_HIT,
                            "Breakpoint hit at " + className + ":" + lineNumber,
                            details
                    );
                    
                    eventBus.publish(event).subscribe();
                }

                @Override
                public void onStepCompleted(String className, int lineNumber) {
                    Map<String, Object> details = new HashMap<>();
                    details.put("className", className);
                    details.put("lineNumber", lineNumber);
                    
                    PluginDebugEvent event = new PluginDebugEvent(
                            sessionId,
                            pluginId,
                            Type.STEP_COMPLETED,
                            "Step completed at " + className + ":" + lineNumber,
                            details
                    );
                    
                    eventBus.publish(event).subscribe();
                }

                @Override
                public void onDisconnect() {
                    Map<String, Object> details = new HashMap<>();
                    details.put("reason", "VM disconnected");
                    
                    PluginDebugEvent event = new PluginDebugEvent(
                            sessionId,
                            pluginId,
                            Type.SESSION_ENDED,
                            "Debug session ended: VM disconnected",
                            details
                    );
                    
                    eventBus.publish(event).subscribe();
                    
                    // Remove the connection
                    connections.remove(sessionId);
                }
            });
            
            // Store the connection
            connections.put(sessionId, connection);
            
            // Connect to the VM
            return connection.connect();
        });
    }

    /**
     * Disconnects from a plugin's JVM.
     *
     * @param sessionId the debug session ID
     * @return a Mono that completes when the connection is closed
     */
    public Mono<Void> disconnectFromVM(String sessionId) {
        return Mono.defer(() -> {
            DebuggerConnection connection = connections.get(sessionId);
            if (connection == null) {
                logger.warn("No VM connection found for debug session: {}", sessionId);
                return Mono.empty();
            }
            
            return connection.disconnect()
                    .doOnSuccess(v -> connections.remove(sessionId));
        });
    }

    /**
     * Gets a debugger connection.
     *
     * @param sessionId the debug session ID
     * @return the debugger connection, or null if not found
     */
    public DebuggerConnection getConnection(String sessionId) {
        return connections.get(sessionId);
    }

    /**
     * Checks if a session has a VM connection.
     *
     * @param sessionId the debug session ID
     * @return true if the session has a VM connection, false otherwise
     */
    public boolean hasConnection(String sessionId) {
        return connections.containsKey(sessionId);
    }

    /**
     * Disconnects all VM connections.
     *
     * @return a Mono that completes when all connections are closed
     */
    public Mono<Void> disconnectAll() {
        return Mono.defer(() -> {
            return Mono.when(
                    connections.keySet().stream()
                            .map(this::disconnectFromVM)
                            .toArray(Mono[]::new)
            );
        });
    }
}
