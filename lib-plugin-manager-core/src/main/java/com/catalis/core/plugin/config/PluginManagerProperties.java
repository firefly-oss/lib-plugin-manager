package com.catalis.core.plugin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the plugin manager.
 */
@Component
@ConfigurationProperties(prefix = "firefly.plugin-manager")
public class PluginManagerProperties {
    
    /**
     * The directory where plugins are stored.
     */
    private Path pluginsDirectory = Path.of("plugins");
    
    /**
     * Whether to automatically start plugins when they are loaded.
     */
    private boolean autoStartPlugins = true;
    
    /**
     * Whether to scan for plugins on startup.
     */
    private boolean scanOnStartup = true;
    
    /**
     * List of plugin IDs that should be loaded on startup.
     */
    private List<String> autoLoadPlugins = new ArrayList<>();
    
    /**
     * List of packages that are allowed to be accessed by plugins.
     */
    private List<String> allowedPackages = new ArrayList<>();
    
    /**
     * Gets the plugins directory.
     * 
     * @return the plugins directory
     */
    public Path getPluginsDirectory() {
        return pluginsDirectory;
    }
    
    /**
     * Sets the plugins directory.
     * 
     * @param pluginsDirectory the plugins directory
     */
    public void setPluginsDirectory(Path pluginsDirectory) {
        this.pluginsDirectory = pluginsDirectory;
    }
    
    /**
     * Gets whether to automatically start plugins when they are loaded.
     * 
     * @return true if plugins should be automatically started, false otherwise
     */
    public boolean isAutoStartPlugins() {
        return autoStartPlugins;
    }
    
    /**
     * Sets whether to automatically start plugins when they are loaded.
     * 
     * @param autoStartPlugins true if plugins should be automatically started, false otherwise
     */
    public void setAutoStartPlugins(boolean autoStartPlugins) {
        this.autoStartPlugins = autoStartPlugins;
    }
    
    /**
     * Gets whether to scan for plugins on startup.
     * 
     * @return true if plugins should be scanned on startup, false otherwise
     */
    public boolean isScanOnStartup() {
        return scanOnStartup;
    }
    
    /**
     * Sets whether to scan for plugins on startup.
     * 
     * @param scanOnStartup true if plugins should be scanned on startup, false otherwise
     */
    public void setScanOnStartup(boolean scanOnStartup) {
        this.scanOnStartup = scanOnStartup;
    }
    
    /**
     * Gets the list of plugin IDs that should be loaded on startup.
     * 
     * @return the list of plugin IDs
     */
    public List<String> getAutoLoadPlugins() {
        return autoLoadPlugins;
    }
    
    /**
     * Sets the list of plugin IDs that should be loaded on startup.
     * 
     * @param autoLoadPlugins the list of plugin IDs
     */
    public void setAutoLoadPlugins(List<String> autoLoadPlugins) {
        this.autoLoadPlugins = autoLoadPlugins;
    }
    
    /**
     * Gets the list of packages that are allowed to be accessed by plugins.
     * 
     * @return the list of allowed packages
     */
    public List<String> getAllowedPackages() {
        return allowedPackages;
    }
    
    /**
     * Sets the list of packages that are allowed to be accessed by plugins.
     * 
     * @param allowedPackages the list of allowed packages
     */
    public void setAllowedPackages(List<String> allowedPackages) {
        this.allowedPackages = allowedPackages;
    }
}
