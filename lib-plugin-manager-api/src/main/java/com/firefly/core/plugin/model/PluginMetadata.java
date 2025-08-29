package com.firefly.core.plugin.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

/**
 * Contains metadata information about a plugin.
 */
public record PluginMetadata(
    String id,
    String name,
    String version,
    String description,
    String author,
    String minPlatformVersion,
    String maxPlatformVersion,
    Set<String> dependencies,
    Instant installTime,
    Instant lastStartTime
) {
    /**
     * Creates a new builder for PluginMetadata.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for creating PluginMetadata instances.
     */
    public static class Builder {
        private String id;
        private String name;
        private String version;
        private String description = "";
        private String author = "";
        private String minPlatformVersion = "1.0.0";
        private String maxPlatformVersion = "";
        private Set<String> dependencies = Collections.emptySet();
        private Instant installTime = Instant.now();
        private Instant lastStartTime;
        
        /**
         * Sets the plugin ID.
         * 
         * @param id the plugin ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        /**
         * Sets the plugin name.
         * 
         * @param name the plugin name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Sets the plugin version.
         * 
         * @param version the plugin version
         * @return this builder
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        /**
         * Sets the plugin description.
         * 
         * @param description the plugin description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the plugin author.
         * 
         * @param author the plugin author
         * @return this builder
         */
        public Builder author(String author) {
            this.author = author;
            return this;
        }
        
        /**
         * Sets the minimum platform version.
         * 
         * @param minPlatformVersion the minimum platform version
         * @return this builder
         */
        public Builder minPlatformVersion(String minPlatformVersion) {
            this.minPlatformVersion = minPlatformVersion;
            return this;
        }
        
        /**
         * Sets the maximum platform version.
         * 
         * @param maxPlatformVersion the maximum platform version
         * @return this builder
         */
        public Builder maxPlatformVersion(String maxPlatformVersion) {
            this.maxPlatformVersion = maxPlatformVersion;
            return this;
        }
        
        /**
         * Sets the plugin dependencies.
         * 
         * @param dependencies the plugin dependencies
         * @return this builder
         */
        public Builder dependencies(Set<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }
        
        /**
         * Sets the plugin install time.
         * 
         * @param installTime the plugin install time
         * @return this builder
         */
        public Builder installTime(Instant installTime) {
            this.installTime = installTime;
            return this;
        }
        
        /**
         * Sets the plugin last start time.
         * 
         * @param lastStartTime the plugin last start time
         * @return this builder
         */
        public Builder lastStartTime(Instant lastStartTime) {
            this.lastStartTime = lastStartTime;
            return this;
        }
        
        /**
         * Builds a new PluginMetadata instance.
         * 
         * @return a new PluginMetadata instance
         */
        public PluginMetadata build() {
            return new PluginMetadata(
                id, name, version, description, author,
                minPlatformVersion, maxPlatformVersion, dependencies,
                installTime, lastStartTime
            );
        }
    }
}
