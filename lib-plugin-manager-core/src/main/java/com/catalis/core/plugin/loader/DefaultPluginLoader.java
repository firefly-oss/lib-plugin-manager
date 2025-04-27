package com.catalis.core.plugin.loader;

import com.catalis.core.plugin.annotation.Plugin;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.security.PluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Default implementation of the PluginLoader interface.
 */
@Component
public class DefaultPluginLoader implements PluginLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginLoader.class);
    
    @Override
    public Mono<com.catalis.core.plugin.api.Plugin> loadPlugin(Path pluginPath) {
        logger.info("Loading plugin from path: {}", pluginPath);
        
        return Mono.fromCallable(() -> {
            try {
                // Create a class loader for the plugin
                URL pluginUrl = pluginPath.toUri().toURL();
                PluginClassLoader classLoader = new PluginClassLoader(
                        new URL[]{pluginUrl},
                        getClass().getClassLoader());
                
                // Look for plugin classes using ServiceLoader
                ServiceLoader<com.catalis.core.plugin.api.Plugin> serviceLoader =
                        ServiceLoader.load(com.catalis.core.plugin.api.Plugin.class, classLoader);
                
                com.catalis.core.plugin.api.Plugin pluginInstance = serviceLoader.findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No plugin implementation found in " + pluginPath));
                
                // Validate plugin metadata
                validatePluginMetadata(pluginInstance.getMetadata());
                
                return pluginInstance;
            } catch (Exception e) {
                logger.error("Error loading plugin from {}: {}", pluginPath, e.getMessage(), e);
                throw new RuntimeException("Failed to load plugin: " + e.getMessage(), e);
            }
        });
    }
    
    private void validatePluginMetadata(PluginMetadata metadata) {
        if (metadata.id() == null || metadata.id().isBlank()) {
            throw new IllegalArgumentException("Plugin ID cannot be null or blank");
        }
        
        if (metadata.name() == null || metadata.name().isBlank()) {
            throw new IllegalArgumentException("Plugin name cannot be null or blank");
        }
        
        if (metadata.version() == null || metadata.version().isBlank()) {
            throw new IllegalArgumentException("Plugin version cannot be null or blank");
        }
    }
    
    /**
     * Extracts plugin metadata from a plugin class.
     * 
     * @param pluginClass the plugin class
     * @param pluginPath the path to the plugin JAR file
     * @return the plugin metadata
     */
    private PluginMetadata extractMetadata(Class<?> pluginClass, Path pluginPath) {
        Plugin annotation = pluginClass.getAnnotation(Plugin.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Plugin class does not have @Plugin annotation");
        }
        
        Set<String> dependencies = new HashSet<>(Arrays.asList(annotation.dependencies()));
        
        return PluginMetadata.builder()
                .id(annotation.id())
                .name(annotation.name())
                .version(annotation.version())
                .description(annotation.description())
                .author(annotation.author())
                .minPlatformVersion(annotation.minPlatformVersion())
                .maxPlatformVersion(annotation.maxPlatformVersion())
                .dependencies(dependencies)
                .installTime(Instant.now())
                .build();
    }
}
