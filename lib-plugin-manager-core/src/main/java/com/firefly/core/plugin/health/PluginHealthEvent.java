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
