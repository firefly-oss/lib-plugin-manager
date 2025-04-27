package com.catalis.core.plugin.event;

import java.time.Instant;

/**
 * Base class for all plugin-related events.
 */
public abstract class PluginEvent {
    private final String pluginId;
    private final Instant timestamp;
    private final String eventType;
    
    /**
     * Creates a new plugin event.
     * 
     * @param pluginId the ID of the plugin associated with this event
     * @param eventType the type of the event
     */
    protected PluginEvent(String pluginId, String eventType) {
        this.pluginId = pluginId;
        this.timestamp = Instant.now();
        this.eventType = eventType;
    }
    
    /**
     * Gets the ID of the plugin associated with this event.
     * 
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * Gets the timestamp when this event was created.
     * 
     * @return the event timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the type of this event.
     * 
     * @return the event type
     */
    public String getEventType() {
        return eventType;
    }
}
