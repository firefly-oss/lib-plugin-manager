package com.catalis.core.plugin.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Event that is fired when a plugin's configuration changes.
 */
public class PluginConfigurationEvent extends PluginEvent {
    private final Map<String, Object> previousConfiguration;
    private final Map<String, Object> newConfiguration;

    /**
     * Creates a new plugin configuration event.
     *
     * @param pluginId the ID of the plugin
     * @param previousConfiguration the previous configuration
     * @param newConfiguration the new configuration
     */
    public PluginConfigurationEvent(
            String pluginId,
            Map<String, Object> previousConfiguration,
            Map<String, Object> newConfiguration) {
        super(pluginId, "CONFIGURATION");
        this.previousConfiguration = previousConfiguration;
        this.newConfiguration = newConfiguration;
    }

    /**
     * Creates a new plugin configuration event with a specific timestamp.
     * This constructor is used for deserialization.
     *
     * @param pluginId the ID of the plugin
     * @param previousConfiguration the previous configuration
     * @param newConfiguration the new configuration
     * @param timestamp the timestamp of the event
     */
    @JsonCreator
    public PluginConfigurationEvent(
            @JsonProperty("pluginId") String pluginId,
            @JsonProperty("previousConfiguration") Map<String, Object> previousConfiguration,
            @JsonProperty("newConfiguration") Map<String, Object> newConfiguration,
            @JsonProperty("timestamp") Instant timestamp) {
        super(pluginId, "CONFIGURATION", timestamp);
        this.previousConfiguration = previousConfiguration;
        this.newConfiguration = newConfiguration;
    }

    /**
     * Gets the previous configuration of the plugin.
     *
     * @return the previous configuration
     */
    public Map<String, Object> getPreviousConfiguration() {
        return previousConfiguration;
    }

    /**
     * Gets the new configuration of the plugin.
     *
     * @return the new configuration
     */
    public Map<String, Object> getNewConfiguration() {
        return newConfiguration;
    }

    @Override
    public String toString() {
        return "PluginConfigurationEvent{" +
                "pluginId='" + getPluginId() + '\'' +
                ", previousConfiguration=" + previousConfiguration +
                ", newConfiguration=" + newConfiguration +
                ", timestamp=" + getTimestamp() +
                "}";
    }
}
