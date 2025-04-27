package com.catalis.core.plugin.loader;

import com.catalis.core.plugin.api.Plugin;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

/**
 * Interface for loading plugins from external sources.
 */
public interface PluginLoader {
    
    /**
     * Loads a plugin from a JAR file.
     * 
     * @param pluginPath the path to the plugin JAR file
     * @return a Mono that emits the loaded plugin
     */
    Mono<Plugin> loadPlugin(Path pluginPath);
}
