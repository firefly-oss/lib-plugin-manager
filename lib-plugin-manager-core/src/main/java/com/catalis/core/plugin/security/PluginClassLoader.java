package com.catalis.core.plugin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom class loader for loading plugin classes with isolation.
 */
public class PluginClassLoader extends URLClassLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginClassLoader.class);
    
    private final Set<String> allowedPackages;
    
    /**
     * Creates a new PluginClassLoader with the specified URLs and parent class loader.
     * 
     * @param urls the URLs from which to load classes and resources
     * @param parent the parent class loader
     */
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.allowedPackages = new HashSet<>();
        
        // Allow core plugin API packages
        allowedPackages.add("com.catalis.core.plugin.api");
        allowedPackages.add("com.catalis.core.plugin.annotation");
        allowedPackages.add("com.catalis.core.plugin.event");
        allowedPackages.add("com.catalis.core.plugin.model");
        allowedPackages.add("com.catalis.core.plugin.spi");
        
        // Allow Java standard packages
        allowedPackages.add("java.");
        allowedPackages.add("javax.");
        
        // Allow Spring packages
        allowedPackages.add("org.springframework.");
        
        // Allow Reactor packages
        allowedPackages.add("reactor.");
    }
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }
        
        // Check if the class is from an allowed package
        boolean isAllowed = allowedPackages.stream()
                .anyMatch(name::startsWith);
        
        if (isAllowed) {
            // Delegate to parent class loader for allowed packages
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                // Fall through to try loading from plugin
            }
        }
        
        try {
            // Try to load the class from the plugin
            Class<?> pluginClass = findClass(name);
            if (resolve) {
                resolveClass(pluginClass);
            }
            return pluginClass;
        } catch (ClassNotFoundException e) {
            // If not found in plugin, delegate to parent
            return super.loadClass(name, resolve);
        }
    }
    
    /**
     * Adds a package to the list of allowed packages.
     * 
     * @param packageName the package name to allow
     */
    public void allowPackage(String packageName) {
        allowedPackages.add(packageName);
    }
}
