package com.catalis.core.plugin.event;

import com.catalis.core.plugin.model.PluginState;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Event that is fired when a plugin's lifecycle state changes.
 */
public class PluginLifecycleEvent extends PluginEvent {
    private final PluginState previousState;
    private final PluginState newState;

    /**
     * Creates a new plugin lifecycle event.
     *
     * @param pluginId the ID of the plugin
     * @param previousState the previous state of the plugin
     * @param newState the new state of the plugin
     */
    public PluginLifecycleEvent(String pluginId, PluginState previousState, PluginState newState) {
        super(pluginId, "LIFECYCLE");
        this.previousState = previousState;
        this.newState = newState;
    }

    /**
     * Creates a new plugin lifecycle event with a specific timestamp.
     * This constructor is used for deserialization.
     *
     * @param pluginId the ID of the plugin
     * @param previousState the previous state of the plugin
     * @param newState the new state of the plugin
     * @param timestamp the timestamp of the event
     */
    @JsonCreator
    public PluginLifecycleEvent(
            @JsonProperty("pluginId") String pluginId,
            @JsonProperty("previousState") PluginState previousState,
            @JsonProperty("newState") PluginState newState,
            @JsonProperty("timestamp") Instant timestamp) {
        super(pluginId, "LIFECYCLE", timestamp);
        this.previousState = previousState;
        this.newState = newState;
    }

    /**
     * Gets the previous state of the plugin.
     *
     * @return the previous state
     */
    public PluginState getPreviousState() {
        return previousState;
    }

    /**
     * Gets the new state of the plugin.
     *
     * @return the new state
     */
    public PluginState getNewState() {
        return newState;
    }

    @Override
    public String toString() {
        return "PluginLifecycleEvent{" +
                "pluginId='" + getPluginId() + '\'' +
                ", previousState=" + previousState +
                ", newState=" + newState +
                ", timestamp=" + getTimestamp() +
                "}";
    }
}
