package com.firefly.core.plugin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom class loader for loading plugin classes with isolation.
 * This class loader provides strict isolation between plugins and controls
 * which classes from the parent class loader are accessible to plugins.
 */
public class PluginClassLoader extends URLClassLoader {

    private static final Logger logger = LoggerFactory.getLogger(PluginClassLoader.class);

    private final Set<String> allowedPackages;
    private final String pluginId;
    private final Set<String> exportedPackages;
    private final ConcurrentHashMap<String, Class<?>> loadedPluginClasses;
    private final ProtectionDomain protectionDomain;

    /**
     * Creates a new PluginClassLoader with the specified URLs and parent class loader.
     *
     * @param pluginId the ID of the plugin
     * @param urls the URLs from which to load classes and resources
     * @param parent the parent class loader
     * @param allowedPackages additional packages that are allowed to be accessed by the plugin
     * @param protectionDomain the protection domain for the plugin (can be null)
     */
    public PluginClassLoader(String pluginId, URL[] urls, ClassLoader parent,
                            Collection<String> allowedPackages, ProtectionDomain protectionDomain) {
        super(urls, parent);
        this.pluginId = pluginId;
        this.allowedPackages = new HashSet<>();
        this.exportedPackages = new HashSet<>();
        this.loadedPluginClasses = new ConcurrentHashMap<>();
        this.protectionDomain = protectionDomain;

        // Add default allowed packages
        addDefaultAllowedPackages();

        // Add additional allowed packages
        if (allowedPackages != null) {
            this.allowedPackages.addAll(allowedPackages);
        }

        logger.debug("Created PluginClassLoader for plugin: {}", pluginId);
    }

    /**
     * Creates a new PluginClassLoader with the specified URLs and parent class loader.
     *
     * @param pluginId the ID of the plugin
     * @param urls the URLs from which to load classes and resources
     * @param parent the parent class loader
     */
    public PluginClassLoader(String pluginId, URL[] urls, ClassLoader parent) {
        this(pluginId, urls, parent, null, null);
    }

    /**
     * Adds the default allowed packages to the set of allowed packages.
     */
    private void addDefaultAllowedPackages() {
        // Allow core plugin API packages
        allowedPackages.add("com.firefly.core.plugin.api");
        allowedPackages.add("com.firefly.core.plugin.annotation");
        allowedPackages.add("com.firefly.core.plugin.event");
        allowedPackages.add("com.firefly.core.plugin.model");
        allowedPackages.add("com.firefly.core.plugin.spi");

        // Allow Java standard packages
        allowedPackages.add("java.");
        allowedPackages.add("javax.");
        allowedPackages.add("jakarta.");

        // Allow Spring packages
        allowedPackages.add("org.springframework.");

        // Allow Reactor packages
        allowedPackages.add("reactor.");

        // Allow logging packages
        allowedPackages.add("org.slf4j.");
        allowedPackages.add("ch.qos.logback.");
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded by this class loader
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                if (resolve) {
                    resolveClass(loadedClass);
                }
                return loadedClass;
            }

            // Check if the class is from an allowed package
            boolean isAllowed = isPackageAllowed(name);

            if (isAllowed) {
                // Delegate to parent class loader for allowed packages
                try {
                    Class<?> parentClass = getParent().loadClass(name);
                    if (resolve) {
                        resolveClass(parentClass);
                    }
                    return parentClass;
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

                // Cache the loaded class
                loadedPluginClasses.put(name, pluginClass);

                return pluginClass;
            } catch (ClassNotFoundException e) {
                // If not allowed and not found in plugin, throw exception
                if (!isAllowed) {
                    logger.warn("Plugin {} attempted to access unauthorized class: {}", pluginId, name);
                    throw new SecurityException("Access to class " + name + " is not allowed for plugin " + pluginId);
                }

                // If allowed but not found anywhere, throw the original exception
                throw e;
            }
        }
    }

    /**
     * Checks if the specified package is allowed to be accessed by the plugin.
     *
     * @param className the fully qualified class name
     * @return true if the package is allowed, false otherwise
     */
    private boolean isPackageAllowed(String className) {
        return allowedPackages.stream()
                .anyMatch(className::startsWith);
    }

    /**
     * Adds a package to the list of allowed packages.
     *
     * @param packageName the package name to allow
     */
    public void allowPackage(String packageName) {
        allowedPackages.add(packageName);
        logger.debug("Added allowed package for plugin {}: {}", pluginId, packageName);
    }

    /**
     * Adds multiple packages to the list of allowed packages.
     *
     * @param packageNames the package names to allow
     */
    public void allowPackages(Collection<String> packageNames) {
        if (packageNames != null) {
            allowedPackages.addAll(packageNames);
            logger.debug("Added allowed packages for plugin {}: {}", pluginId, packageNames);
        }
    }

    /**
     * Exports a package from this plugin, making it available to other plugins.
     *
     * @param packageName the package name to export
     */
    public void exportPackage(String packageName) {
        exportedPackages.add(packageName);
        logger.debug("Exported package from plugin {}: {}", pluginId, packageName);
    }

    /**
     * Gets the set of exported packages from this plugin.
     *
     * @return the set of exported packages
     */
    public Set<String> getExportedPackages() {
        return Collections.unmodifiableSet(exportedPackages);
    }

    /**
     * Gets the ID of the plugin associated with this class loader.
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Gets the set of classes loaded by this class loader.
     *
     * @return the set of loaded classes
     */
    public Set<String> getLoadedClasses() {
        return Collections.unmodifiableSet(loadedPluginClasses.keySet());
    }

    @Override
    public void close() throws IOException {
        try {
            // Clear the cache of loaded classes
            loadedPluginClasses.clear();

            // Close the class loader
            super.close();

            logger.debug("Closed PluginClassLoader for plugin: {}", pluginId);
        } catch (IOException e) {
            logger.error("Error closing PluginClassLoader for plugin {}: {}", pluginId, e.getMessage(), e);
            throw e;
        }
    }
}
