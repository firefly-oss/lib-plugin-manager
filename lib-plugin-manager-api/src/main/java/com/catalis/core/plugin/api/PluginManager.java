package com.catalis.core.plugin.api;

import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.model.PluginDescriptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.Map;

/**
 * Main interface for the plugin manager, which provides high-level operations for managing plugins.
 */
public interface PluginManager {
    
    /**
     * Gets the plugin registry.
     * 
     * @return the plugin registry
     */
    PluginRegistry getPluginRegistry();
    
    /**
     * Gets the extension registry.
     * 
     * @return the extension registry
     */
    ExtensionRegistry getExtensionRegistry();
    
    /**
     * Gets the plugin event bus.
     * 
     * @return the plugin event bus
     */
    PluginEventBus getEventBus();
    
    /**
     * Installs a plugin from a JAR file.
     * 
     * @param pluginPath the path to the plugin JAR file
     * @return a Mono that emits the plugin descriptor when the plugin has been installed
     */
    Mono<PluginDescriptor> installPlugin(Path pluginPath);
    
    /**
     * Uninstalls a plugin.
     * 
     * @param pluginId the ID of the plugin to uninstall
     * @return a Mono that completes when the plugin has been uninstalled
     */
    Mono<Void> uninstallPlugin(String pluginId);
    
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
     * Restarts a plugin.
     * 
     * @param pluginId the ID of the plugin to restart
     * @return a Mono that completes when the plugin has been restarted
     */
    Mono<Void> restartPlugin(String pluginId);
    
    /**
     * Updates the configuration of a plugin.
     * 
     * @param pluginId the ID of the plugin
     * @param configuration the new configuration
     * @return a Mono that completes when the configuration has been updated
     */
    Mono<Void> updatePluginConfiguration(String pluginId, Map<String, Object> configuration);
    
    /**
     * Gets all installed plugins.
     * 
     * @return a Flux of all plugin descriptors
     */
    Flux<PluginDescriptor> getAllPlugins();
    
    /**
     * Gets a plugin by its ID.
     * 
     * @param pluginId the ID of the plugin
     * @return a Mono that emits the plugin descriptor, or an empty Mono if the plugin is not found
     */
    Mono<PluginDescriptor> getPlugin(String pluginId);
    
    /**
     * Initializes the plugin manager.
     * This method should be called during application startup.
     * 
     * @return a Mono that completes when the plugin manager has been initialized
     */
    Mono<Void> initialize();
    
    /**
     * Shuts down the plugin manager.
     * This method should be called during application shutdown.
     * 
     * @return a Mono that completes when the plugin manager has been shut down
     */
    Mono<Void> shutdown();
}
