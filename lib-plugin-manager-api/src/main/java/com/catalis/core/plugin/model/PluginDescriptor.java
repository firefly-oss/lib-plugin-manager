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
     * Gets the plugin ID.
     *
     * @return the plugin ID
     */
    public String getId() {
        return metadata.id();
    }

    /**
     * Gets the plugin name.
     *
     * @return the plugin name
     */
    public String getName() {
        return metadata.name();
    }

    /**
     * Gets the plugin version.
     *
     * @return the plugin version
     */
    public String getVersion() {
        return metadata.version();
    }

    /**
     * Gets the plugin state.
     *
     * @return the plugin state
     */
    public PluginState getState() {
        return state;
    }
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
         * Sets the plugin ID.
         * This is a convenience method that creates or updates the metadata with the given ID.
         *
         * @param id the plugin ID
         * @return this builder
         */
        public Builder id(String id) {
            if (this.metadata == null) {
                this.metadata = PluginMetadata.builder().id(id).build();
            } else {
                this.metadata = PluginMetadata.builder()
                    .id(id)
                    .name(this.metadata.name())
                    .version(this.metadata.version())
                    .description(this.metadata.description())
                    .author(this.metadata.author())
                    .minPlatformVersion(this.metadata.minPlatformVersion())
                    .maxPlatformVersion(this.metadata.maxPlatformVersion())
                    .dependencies(this.metadata.dependencies())
                    .installTime(this.metadata.installTime())
                    .lastStartTime(this.metadata.lastStartTime())
                    .build();
            }
            return this;
        }

        /**
         * Sets the plugin name.
         * This is a convenience method that creates or updates the metadata with the given name.
         *
         * @param name the plugin name
         * @return this builder
         */
        public Builder name(String name) {
            if (this.metadata == null) {
                this.metadata = PluginMetadata.builder().name(name).build();
            } else {
                this.metadata = PluginMetadata.builder()
                    .id(this.metadata.id())
                    .name(name)
                    .version(this.metadata.version())
                    .description(this.metadata.description())
                    .author(this.metadata.author())
                    .minPlatformVersion(this.metadata.minPlatformVersion())
                    .maxPlatformVersion(this.metadata.maxPlatformVersion())
                    .dependencies(this.metadata.dependencies())
                    .installTime(this.metadata.installTime())
                    .lastStartTime(this.metadata.lastStartTime())
                    .build();
            }
            return this;
        }

        /**
         * Sets the plugin version.
         * This is a convenience method that creates or updates the metadata with the given version.
         *
         * @param version the plugin version
         * @return this builder
         */
        public Builder version(String version) {
            if (this.metadata == null) {
                this.metadata = PluginMetadata.builder().version(version).build();
            } else {
                this.metadata = PluginMetadata.builder()
                    .id(this.metadata.id())
                    .name(this.metadata.name())
                    .version(version)
                    .description(this.metadata.description())
                    .author(this.metadata.author())
                    .minPlatformVersion(this.metadata.minPlatformVersion())
                    .maxPlatformVersion(this.metadata.maxPlatformVersion())
                    .dependencies(this.metadata.dependencies())
                    .installTime(this.metadata.installTime())
                    .lastStartTime(this.metadata.lastStartTime())
                    .build();
            }
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
