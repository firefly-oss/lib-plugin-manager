package com.firefly.core.plugin.health;

import com.firefly.core.plugin.event.PluginEvent;
import com.firefly.core.plugin.model.PluginHealth;

/**
 * Event that is published when a plugin's health status changes.
 */
public class PluginHealthEvent extends PluginEvent {

    private static final String EVENT_TYPE = "plugin.health";
    private final PluginHealth health;

    /**
     * Creates a new PluginHealthEvent.
     *
     * @param health the plugin health
     */
    public PluginHealthEvent(PluginHealth health) {
        super(health.getPluginId(), EVENT_TYPE);
        this.health = health;
    }

    /**
     * Gets the plugin health.
     *
     * @return the plugin health
     */
    public PluginHealth getHealth() {
        return health;
    }

    @Override
    public String toString() {
        return "PluginHealthEvent{" +
                "pluginId='" + getPluginId() + '\'' +
                ", type='" + EVENT_TYPE + '\'' +
                ", timestamp=" + getTimestamp() +
                ", health=" + health +
                '}';
    }
}
