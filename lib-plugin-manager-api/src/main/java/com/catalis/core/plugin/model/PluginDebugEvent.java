package com.catalis.core.plugin.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a debug event that occurs during plugin execution.
 */
public class PluginDebugEvent {

    /**
     * The type of debug event.
     */
    public enum Type {
        /**
         * A breakpoint was hit.
         */
        BREAKPOINT_HIT,
        
        /**
         * A step operation completed.
         */
        STEP_COMPLETED,
        
        /**
         * An exception was thrown.
         */
        EXCEPTION_THROWN,
        
        /**
         * A debug session started.
         */
        SESSION_STARTED,
        
        /**
         * A debug session ended.
         */
        SESSION_ENDED,
        
        /**
         * A variable was inspected.
         */
        VARIABLE_INSPECTED,
        
        /**
         * An expression was evaluated.
         */
        EXPRESSION_EVALUATED
    }
    
    private final String sessionId;
    private final String pluginId;
    private final Type type;
    private final String message;
    private final Instant timestamp;
    private final Map<String, Object> details;
    
    /**
     * Creates a new PluginDebugEvent.
     *
     * @param sessionId the debug session ID
     * @param pluginId the plugin ID
     * @param type the event type
     * @param message a message describing the event
     * @param details additional details about the event
     */
    public PluginDebugEvent(String sessionId, String pluginId, Type type, String message, Map<String, Object> details) {
        this.sessionId = sessionId;
        this.pluginId = pluginId;
        this.type = type;
        this.message = message;
        this.timestamp = Instant.now();
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
    
    /**
     * Gets the debug session ID.
     *
     * @return the debug session ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Gets the plugin ID.
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * Gets the event type.
     *
     * @return the event type
     */
    public Type getType() {
        return type;
    }
    
    /**
     * Gets the message describing the event.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the timestamp when the event occurred.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the additional details about the event.
     *
     * @return the details
     */
    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }
    
    /**
     * Creates a new PluginDebugEvent with the same values but with an additional detail.
     *
     * @param key the detail key
     * @param value the detail value
     * @return a new PluginDebugEvent
     */
    public PluginDebugEvent withDetail(String key, Object value) {
        Map<String, Object> newDetails = new HashMap<>(details);
        newDetails.put(key, value);
        return new PluginDebugEvent(sessionId, pluginId, type, message, newDetails);
    }
    
    @Override
    public String toString() {
        return "PluginDebugEvent{" +
                "sessionId='" + sessionId + '\'' +
                ", pluginId='" + pluginId + '\'' +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", details=" + details +
                '}';
    }
}
