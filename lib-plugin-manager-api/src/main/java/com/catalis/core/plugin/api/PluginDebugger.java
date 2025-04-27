package com.catalis.core.plugin.api;

import com.catalis.core.plugin.model.PluginDescriptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Interface for the plugin debugger, which provides debugging capabilities for plugins.
 */
public interface PluginDebugger {
    
    /**
     * Starts a debug session for a plugin.
     *
     * @param pluginId the ID of the plugin to debug
     * @return a Mono that emits the debug session ID
     */
    Mono<String> startDebugSession(String pluginId);
    
    /**
     * Stops a debug session.
     *
     * @param sessionId the debug session ID
     * @return a Mono that completes when the session has been stopped
     */
    Mono<Void> stopDebugSession(String sessionId);
    
    /**
     * Gets all active debug sessions.
     *
     * @return a Flux of session IDs and their associated plugin IDs
     */
    Flux<Map.Entry<String, String>> getActiveSessions();
    
    /**
     * Sets a breakpoint in a plugin.
     *
     * @param sessionId the debug session ID
     * @param className the fully qualified class name
     * @param lineNumber the line number
     * @return a Mono that emits the breakpoint ID
     */
    Mono<String> setBreakpoint(String sessionId, String className, int lineNumber);
    
    /**
     * Removes a breakpoint.
     *
     * @param sessionId the debug session ID
     * @param breakpointId the breakpoint ID
     * @return a Mono that completes when the breakpoint has been removed
     */
    Mono<Void> removeBreakpoint(String sessionId, String breakpointId);
    
    /**
     * Gets all breakpoints for a debug session.
     *
     * @param sessionId the debug session ID
     * @return a Flux of breakpoint information
     */
    Flux<Map<String, Object>> getBreakpoints(String sessionId);
    
    /**
     * Continues execution after a breakpoint.
     *
     * @param sessionId the debug session ID
     * @return a Mono that completes when execution has been resumed
     */
    Mono<Void> continueExecution(String sessionId);
    
    /**
     * Steps over the current line.
     *
     * @param sessionId the debug session ID
     * @return a Mono that completes when the step is complete
     */
    Mono<Void> stepOver(String sessionId);
    
    /**
     * Steps into a method.
     *
     * @param sessionId the debug session ID
     * @return a Mono that completes when the step is complete
     */
    Mono<Void> stepInto(String sessionId);
    
    /**
     * Steps out of the current method.
     *
     * @param sessionId the debug session ID
     * @return a Mono that completes when the step is complete
     */
    Mono<Void> stepOut(String sessionId);
    
    /**
     * Gets the value of a variable.
     *
     * @param sessionId the debug session ID
     * @param variableName the variable name
     * @return a Mono that emits the variable value
     */
    Mono<Object> getVariableValue(String sessionId, String variableName);
    
    /**
     * Gets all local variables at the current execution point.
     *
     * @param sessionId the debug session ID
     * @return a Flux of variable names and values
     */
    Flux<Map.Entry<String, Object>> getLocalVariables(String sessionId);
    
    /**
     * Evaluates an expression in the context of the current execution point.
     *
     * @param sessionId the debug session ID
     * @param expression the expression to evaluate
     * @return a Mono that emits the result of the evaluation
     */
    Mono<Object> evaluateExpression(String sessionId, String expression);
    
    /**
     * Gets the current stack trace.
     *
     * @param sessionId the debug session ID
     * @return a Flux of stack frame information
     */
    Flux<Map<String, Object>> getStackTrace(String sessionId);
    
    /**
     * Gets information about a plugin being debugged.
     *
     * @param sessionId the debug session ID
     * @return a Mono that emits the plugin descriptor
     */
    Mono<PluginDescriptor> getPluginInfo(String sessionId);
    
    /**
     * Checks if debugging is enabled for a plugin.
     *
     * @param pluginId the plugin ID
     * @return a Mono that emits true if debugging is enabled, false otherwise
     */
    Mono<Boolean> isDebuggingEnabled(String pluginId);
    
    /**
     * Enables or disables debugging for a plugin.
     *
     * @param pluginId the plugin ID
     * @param enabled whether debugging should be enabled
     * @return a Mono that completes when the operation is done
     */
    Mono<Void> setDebuggingEnabled(String pluginId, boolean enabled);
}
