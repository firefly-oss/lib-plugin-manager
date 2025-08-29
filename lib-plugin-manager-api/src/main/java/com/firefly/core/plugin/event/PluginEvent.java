package com.firefly.core.plugin.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

/**
 * Base class for all plugin-related events.
 * This class is designed to be serializable for distributed event processing.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PluginLifecycleEvent.class, name = "LIFECYCLE"),
        @JsonSubTypes.Type(value = PluginConfigurationEvent.class, name = "CONFIGURATION")
})
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
     * Creates a new plugin event with a specific timestamp.
     * This constructor is used for deserialization.
     *
     * @param pluginId the ID of the plugin associated with this event
     * @param eventType the type of the event
     * @param timestamp the timestamp of the event
     */
    @JsonCreator
    protected PluginEvent(
            @JsonProperty("pluginId") String pluginId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("timestamp") Instant timestamp) {
        this.pluginId = pluginId;
        this.eventType = eventType;
        this.timestamp = timestamp;
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "pluginId='" + pluginId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                "}";
    }
}
