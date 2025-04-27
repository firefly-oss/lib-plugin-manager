package com.catalis.core.plugin.spi;

import com.catalis.core.plugin.api.Plugin;
import com.catalis.core.plugin.model.PluginMetadata;
import reactor.core.publisher.Mono;

/**
 * Abstract base class for plugins that provides default implementations of the Plugin interface.
 * Plugin developers can extend this class to create their own plugins.
 */
public abstract class AbstractPlugin implements Plugin {
    
    private final PluginMetadata metadata;
    
    /**
     * Creates a new abstract plugin with the specified metadata.
     * 
     * @param metadata the plugin metadata
     */
    protected AbstractPlugin(PluginMetadata metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public Mono<Void> initialize() {
        // Default implementation does nothing
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> start() {
        // Default implementation does nothing
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> stop() {
        // Default implementation does nothing
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> uninstall() {
        // Default implementation does nothing
        return Mono.empty();
    }
}
