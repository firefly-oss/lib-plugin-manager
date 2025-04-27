package com.catalis.core.plugin.api;

import com.catalis.core.plugin.model.PluginMetadata;
import reactor.core.publisher.Mono;

/**
 * Interface that all plugins must implement.
 * This interface defines the lifecycle methods for a plugin.
 */
public interface Plugin {
    
    /**
     * Gets the metadata for this plugin.
     * 
     * @return the plugin metadata
     */
    PluginMetadata getMetadata();
    
    /**
     * Initializes the plugin.
     * This method is called once when the plugin is first loaded.
     * 
     * @return a Mono that completes when initialization is done
     */
    Mono<Void> initialize();
    
    /**
     * Starts the plugin.
     * This method is called when the plugin is started.
     * 
     * @return a Mono that completes when the plugin has started
     */
    Mono<Void> start();
    
    /**
     * Stops the plugin.
     * This method is called when the plugin is stopped.
     * 
     * @return a Mono that completes when the plugin has stopped
     */
    Mono<Void> stop();
    
    /**
     * Uninstalls the plugin.
     * This method is called when the plugin is being uninstalled.
     * 
     * @return a Mono that completes when the plugin has been uninstalled
     */
    Mono<Void> uninstall();
}
