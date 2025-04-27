package com.catalis.core.plugin.model;

import java.util.Map;

/**
 * Represents a complete description of a plugin, including its metadata,
 * current state, and configuration.
 */
public record PluginDescriptor(
    PluginMetadata metadata,
    PluginState state,
    Map<String, Object> configuration,
    String classLoaderInfo,
    String location
) {
    /**
     * Creates a new builder for PluginDescriptor.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for creating PluginDescriptor instances.
     */
    public static class Builder {
        private PluginMetadata metadata;
        private PluginState state = PluginState.INSTALLED;
        private Map<String, Object> configuration = Map.of();
        private String classLoaderInfo = "";
        private String location = "";
        
        /**
         * Sets the plugin metadata.
         * 
         * @param metadata the plugin metadata
         * @return this builder
         */
        public Builder metadata(PluginMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        /**
         * Sets the plugin state.
         * 
         * @param state the plugin state
         * @return this builder
         */
        public Builder state(PluginState state) {
            this.state = state;
            return this;
        }
        
        /**
         * Sets the plugin configuration.
         * 
         * @param configuration the plugin configuration
         * @return this builder
         */
        public Builder configuration(Map<String, Object> configuration) {
            this.configuration = configuration;
            return this;
        }
        
        /**
         * Sets the plugin class loader information.
         * 
         * @param classLoaderInfo the class loader information
         * @return this builder
         */
        public Builder classLoaderInfo(String classLoaderInfo) {
            this.classLoaderInfo = classLoaderInfo;
            return this;
        }
        
        /**
         * Sets the plugin location.
         * 
         * @param location the plugin location
         * @return this builder
         */
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        /**
         * Builds a new PluginDescriptor instance.
         * 
         * @return a new PluginDescriptor instance
         */
        public PluginDescriptor build() {
            return new PluginDescriptor(
                metadata, state, configuration, classLoaderInfo, location
            );
        }
    }
}
