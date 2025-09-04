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


package com.firefly.core.plugin.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the health status of a plugin.
 */
public class PluginHealth {

    /**
     * The status of a plugin.
     */
    public enum Status {
        /**
         * The plugin is up and running normally.
         */
        UP,
        
        /**
         * The plugin is down or not functioning properly.
         */
        DOWN,
        
        /**
         * The plugin is in an unknown state.
         */
        UNKNOWN,
        
        /**
         * The plugin is in a degraded state but still functioning.
         */
        DEGRADED
    }
    
    private final String pluginId;
    private final Status status;
    private final String message;
    private final Instant timestamp;
    private final Map<String, Object> details;
    
    /**
     * Creates a new PluginHealth instance.
     *
     * @param pluginId the plugin ID
     * @param status the health status
     * @param message a message describing the health status
     * @param details additional details about the health status
     */
    public PluginHealth(String pluginId, Status status, String message, Map<String, Object> details) {
        this.pluginId = pluginId;
        this.status = status;
        this.message = message;
        this.timestamp = Instant.now();
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }
    
    /**
     * Creates a new PluginHealth instance with the UP status.
     *
     * @param pluginId the plugin ID
     * @return a new PluginHealth instance
     */
    public static PluginHealth up(String pluginId) {
        return new PluginHealth(pluginId, Status.UP, "Plugin is up and running", null);
    }
    
    /**
     * Creates a new PluginHealth instance with the UP status and details.
     *
     * @param pluginId the plugin ID
     * @param details additional details about the health status
     * @return a new PluginHealth instance
     */
    public static PluginHealth up(String pluginId, Map<String, Object> details) {
        return new PluginHealth(pluginId, Status.UP, "Plugin is up and running", details);
    }
    
    /**
     * Creates a new PluginHealth instance with the DOWN status.
     *
     * @param pluginId the plugin ID
     * @param message a message describing the health status
     * @return a new PluginHealth instance
     */
    public static PluginHealth down(String pluginId, String message) {
        return new PluginHealth(pluginId, Status.DOWN, message, null);
    }
    
    /**
     * Creates a new PluginHealth instance with the DOWN status and details.
     *
     * @param pluginId the plugin ID
     * @param message a message describing the health status
     * @param details additional details about the health status
     * @return a new PluginHealth instance
     */
    public static PluginHealth down(String pluginId, String message, Map<String, Object> details) {
        return new PluginHealth(pluginId, Status.DOWN, message, details);
    }
    
    /**
     * Creates a new PluginHealth instance with the UNKNOWN status.
     *
     * @param pluginId the plugin ID
     * @return a new PluginHealth instance
     */
    public static PluginHealth unknown(String pluginId) {
        return new PluginHealth(pluginId, Status.UNKNOWN, "Plugin health status is unknown", null);
    }
    
    /**
     * Creates a new PluginHealth instance with the DEGRADED status.
     *
     * @param pluginId the plugin ID
     * @param message a message describing the health status
     * @return a new PluginHealth instance
     */
    public static PluginHealth degraded(String pluginId, String message) {
        return new PluginHealth(pluginId, Status.DEGRADED, message, null);
    }
    
    /**
     * Creates a new PluginHealth instance with the DEGRADED status and details.
     *
     * @param pluginId the plugin ID
     * @param message a message describing the health status
     * @param details additional details about the health status
     * @return a new PluginHealth instance
     */
    public static PluginHealth degraded(String pluginId, String message, Map<String, Object> details) {
        return new PluginHealth(pluginId, Status.DEGRADED, message, details);
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
     * Gets the health status.
     *
     * @return the health status
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Gets the message describing the health status.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the timestamp when the health status was created.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the additional details about the health status.
     *
     * @return the details
     */
    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }
    
    /**
     * Adds a detail to the health status.
     *
     * @param key the detail key
     * @param value the detail value
     * @return a new PluginHealth instance with the added detail
     */
    public PluginHealth withDetail(String key, Object value) {
        Map<String, Object> newDetails = new HashMap<>(details);
        newDetails.put(key, value);
        return new PluginHealth(pluginId, status, message, newDetails);
    }
    
    /**
     * Creates a new PluginHealth instance with the same values but a different status.
     *
     * @param status the new status
     * @return a new PluginHealth instance
     */
    public PluginHealth withStatus(Status status) {
        return new PluginHealth(pluginId, status, message, details);
    }
    
    /**
     * Creates a new PluginHealth instance with the same values but a different message.
     *
     * @param message the new message
     * @return a new PluginHealth instance
     */
    public PluginHealth withMessage(String message) {
        return new PluginHealth(pluginId, status, message, details);
    }
    
    @Override
    public String toString() {
        return "PluginHealth{" +
                "pluginId='" + pluginId + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", details=" + details +
                '}';
    }
}
