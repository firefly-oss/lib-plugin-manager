package com.catalis.core.plugin.event;

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
}
