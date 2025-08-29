package com.firefly.core.plugin.model;

/**
 * Represents the possible states of a plugin in its lifecycle.
 */
public enum PluginState {
    /**
     * The plugin has been installed but not yet initialized.
     */
    INSTALLED,
    
    /**
     * The plugin has been initialized but not yet started.
     */
    INITIALIZED,
    
    /**
     * The plugin is currently running.
     */
    STARTED,
    
    /**
     * The plugin has been stopped but is still initialized.
     */
    STOPPED,
    
    /**
     * The plugin has encountered an error and is in a failed state.
     */
    FAILED,
    
    /**
     * The plugin has been uninstalled.
     */
    UNINSTALLED
}
