package com.catalis.core.plugin.config;

import com.catalis.core.plugin.DefaultPluginManager;
import com.catalis.core.plugin.api.ExtensionRegistry;
import com.catalis.core.plugin.api.PluginManager;
import com.catalis.core.plugin.api.PluginRegistry;
import com.catalis.core.plugin.event.DefaultPluginEventBus;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.extension.DefaultExtensionRegistry;
import com.catalis.core.plugin.loader.DefaultPluginLoader;
import com.catalis.core.plugin.loader.PluginLoader;
import com.catalis.core.plugin.registry.DefaultPluginRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the plugin manager.
 */
@Configuration
@EnableConfigurationProperties(PluginManagerProperties.class)
public class PluginManagerAutoConfiguration {
    
    /**
     * Creates a plugin event bus bean if one doesn't exist.
     * 
     * @return the plugin event bus
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginEventBus pluginEventBus() {
        return new DefaultPluginEventBus();
    }
    
    /**
     * Creates a plugin registry bean if one doesn't exist.
     * 
     * @param eventBus the plugin event bus
     * @return the plugin registry
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry pluginRegistry(PluginEventBus eventBus) {
        return new DefaultPluginRegistry(eventBus);
    }
    
    /**
     * Creates an extension registry bean if one doesn't exist.
     * 
     * @return the extension registry
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtensionRegistry extensionRegistry() {
        return new DefaultExtensionRegistry();
    }
    
    /**
     * Creates a plugin loader bean if one doesn't exist.
     * 
     * @return the plugin loader
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginLoader pluginLoader() {
        return new DefaultPluginLoader();
    }
    
    /**
     * Creates a plugin manager bean if one doesn't exist.
     * 
     * @param pluginRegistry the plugin registry
     * @param extensionRegistry the extension registry
     * @param eventBus the plugin event bus
     * @param pluginLoader the plugin loader
     * @return the plugin manager
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginManager pluginManager(
            PluginRegistry pluginRegistry,
            ExtensionRegistry extensionRegistry,
            PluginEventBus eventBus,
            PluginLoader pluginLoader) {
        return new DefaultPluginManager(pluginRegistry, extensionRegistry, eventBus, pluginLoader);
    }
}
