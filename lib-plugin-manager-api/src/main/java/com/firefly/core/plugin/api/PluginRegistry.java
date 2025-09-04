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


package com.firefly.core.plugin.api;

import com.firefly.core.plugin.model.PluginDescriptor;
import com.firefly.core.plugin.model.PluginState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Interface for the plugin registry, which manages the lifecycle of plugins.
 */
public interface PluginRegistry {
    
    /**
     * Registers a plugin with the registry.
     * 
     * @param plugin the plugin to register
     * @return a Mono that completes when the plugin has been registered
     */
    Mono<Void> registerPlugin(Plugin plugin);
    
    /**
     * Unregisters a plugin from the registry.
     * 
     * @param pluginId the ID of the plugin to unregister
     * @return a Mono that completes when the plugin has been unregistered
     */
    Mono<Void> unregisterPlugin(String pluginId);
    
    /**
     * Gets a plugin by its ID.
     * 
     * @param pluginId the ID of the plugin
     * @return a Mono that emits the plugin, or an empty Mono if the plugin is not found
     */
    Mono<Plugin> getPlugin(String pluginId);
    
    /**
     * Gets a plugin descriptor by its ID.
     * 
     * @param pluginId the ID of the plugin
     * @return a Mono that emits the plugin descriptor, or an empty Mono if the plugin is not found
     */
    Mono<PluginDescriptor> getPluginDescriptor(String pluginId);
    
    /**
     * Gets all registered plugins.
     * 
     * @return a Flux of all registered plugins
     */
    Flux<Plugin> getAllPlugins();
    
    /**
     * Gets all plugin descriptors.
     * 
     * @return a Flux of all plugin descriptors
     */
    Flux<PluginDescriptor> getAllPluginDescriptors();
    
    /**
     * Gets all plugins in a specific state.
     * 
     * @param state the state to filter by
     * @return a Flux of plugins in the specified state
     */
    Flux<Plugin> getPluginsByState(PluginState state);
    
    /**
     * Starts a plugin.
     * 
     * @param pluginId the ID of the plugin to start
     * @return a Mono that completes when the plugin has been started
     */
    Mono<Void> startPlugin(String pluginId);
    
    /**
     * Stops a plugin.
     * 
     * @param pluginId the ID of the plugin to stop
     * @return a Mono that completes when the plugin has been stopped
     */
    Mono<Void> stopPlugin(String pluginId);
    
    /**
     * Updates the configuration of a plugin.
     * 
     * @param pluginId the ID of the plugin
     * @param configuration the new configuration
     * @return a Mono that completes when the configuration has been updated
     */
    Mono<Void> updatePluginConfiguration(String pluginId, Map<String, Object> configuration);
}
