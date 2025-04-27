package com.catalis.core.plugin.api;

import com.catalis.core.plugin.model.PluginHealth;
import reactor.core.publisher.Mono;

/**
 * Interface for components that provide health information about a plugin.
 * Plugins can implement this interface to provide custom health checks.
 */
public interface PluginHealthIndicator {
    
    /**
     * Gets the health of the plugin.
     *
     * @return a Mono that emits the plugin health
     */
    Mono<PluginHealth> health();
}
