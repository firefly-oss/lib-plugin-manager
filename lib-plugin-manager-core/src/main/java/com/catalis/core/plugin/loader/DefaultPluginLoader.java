package com.catalis.core.plugin.loader;

import com.catalis.core.plugin.annotation.Plugin;
import com.catalis.core.plugin.config.PluginManagerProperties;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.security.PluginClassLoader;
import com.catalis.core.plugin.security.PluginResourceLimiter;
import com.catalis.core.plugin.security.PluginSecurityManager;
import com.catalis.core.plugin.security.PluginSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Default implementation of the PluginLoader interface.
 * This class is responsible for loading plugins from JAR files.
 */
@Component
public class DefaultPluginLoader implements PluginLoader {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginLoader.class);

    private final PluginManagerProperties properties;
    private final PluginSecurityManager securityManager;
    private final PluginResourceLimiter resourceLimiter;
    private final PluginSignatureVerifier signatureVerifier;

    /**
     * Creates a new DefaultPluginLoader.
     *
     * @param properties the plugin manager properties
     * @param securityManager the plugin security manager
     * @param resourceLimiter the plugin resource limiter
     * @param signatureVerifier the plugin signature verifier
     */
    @Autowired
    public DefaultPluginLoader(
            PluginManagerProperties properties,
            PluginSecurityManager securityManager,
            PluginResourceLimiter resourceLimiter,
            PluginSignatureVerifier signatureVerifier) {
        this.properties = properties;
        this.securityManager = securityManager;
        this.resourceLimiter = resourceLimiter;
        this.signatureVerifier = signatureVerifier;
    }

    /**
     * Creates a new DefaultPluginLoader with default security components.
     * This constructor is used for testing purposes.
     */
    public DefaultPluginLoader() {
        this.properties = new PluginManagerProperties();
        this.securityManager = new PluginSecurityManager(false);
        this.resourceLimiter = new PluginResourceLimiter(false);
        this.signatureVerifier = new PluginSignatureVerifier(false);
    }

    @Override
    public Mono<com.catalis.core.plugin.api.Plugin> loadPlugin(Path pluginPath) {
        logger.info("Loading plugin from path: {}", pluginPath);

        return Mono.fromCallable(() -> {
            try {
                // Verify the plugin signature if required
                if (properties.getSecurity().isRequireSignature() || properties.getSecurity().isEnforceSecurityChecks()) {
                    boolean validSignature = signatureVerifier.verifyPluginSignature(pluginPath);
                    if (!validSignature && properties.getSecurity().isRequireSignature()) {
                        throw new SecurityException("Plugin signature verification failed: " + pluginPath);
                    }
                }

                // Extract plugin ID from JAR manifest
                String pluginId = extractPluginIdFromJar(pluginPath);
                if (pluginId == null) {
                    pluginId = "plugin-" + pluginPath.getFileName().toString().replace(".jar", "");
                    logger.warn("No plugin ID found in JAR manifest, using generated ID: {}", pluginId);
                }

                // Create a class loader for the plugin with appropriate security settings
                URL pluginUrl = pluginPath.toUri().toURL();
                PluginClassLoader classLoader = new PluginClassLoader(
                        pluginId,
                        new URL[]{pluginUrl},
                        getClass().getClassLoader(),
                        properties.getAllowedPackages(),
                        null);

                // Register the plugin with the security manager
                if (properties.getSecurity().isEnforceSecurityChecks()) {
                    // Create permissions based on configuration
                    Set<com.catalis.core.plugin.security.PluginPermission> permissions = createPermissionsForPlugin(pluginId);
                    securityManager.registerPlugin(pluginId, classLoader, permissions);
                }

                // Register the plugin with the resource limiter
                if (properties.getResources().isEnforceResourceLimits()) {
                    PluginResourceLimiter.ResourceLimits limits = createResourceLimitsForPlugin(pluginId);
                    resourceLimiter.registerPlugin(pluginId, limits);
                }

                // Look for plugin classes using ServiceLoader
                ServiceLoader<com.catalis.core.plugin.api.Plugin> serviceLoader =
                        ServiceLoader.load(com.catalis.core.plugin.api.Plugin.class, classLoader);

                // Store pluginId in a final variable for use in lambda
                final String finalPluginId = pluginId;
                final PluginClassLoader finalClassLoader = classLoader;

                com.catalis.core.plugin.api.Plugin pluginInstance = serviceLoader.findFirst()
                        .orElseThrow(() -> {
                            // Clean up resources if plugin loading fails
                            securityManager.unregisterPlugin(finalPluginId, finalClassLoader);
                            resourceLimiter.unregisterPlugin(finalPluginId);
                            try {
                                finalClassLoader.close();
                            } catch (IOException e) {
                                logger.warn("Failed to close class loader for plugin {}", finalPluginId, e);
                            }
                            return new IllegalStateException("No plugin implementation found in " + pluginPath);
                        });

                // Validate plugin metadata
                validatePluginMetadata(pluginInstance.getMetadata());

                // If the plugin ID from metadata doesn't match the one from the JAR, update the class loader
                String metadataPluginId = pluginInstance.getMetadata().id();
                if (!metadataPluginId.equals(pluginId)) {
                    logger.info("Updating plugin ID from {} to {} based on metadata", pluginId, metadataPluginId);

                    // Unregister with the old ID
                    securityManager.unregisterPlugin(pluginId, classLoader);
                    resourceLimiter.unregisterPlugin(pluginId);

                    // Register with the new ID
                    if (properties.getSecurity().isEnforceSecurityChecks()) {
                        Set<com.catalis.core.plugin.security.PluginPermission> permissions = createPermissionsForPlugin(metadataPluginId);
                        securityManager.registerPlugin(metadataPluginId, classLoader, permissions);
                    }

                    if (properties.getResources().isEnforceResourceLimits()) {
                        PluginResourceLimiter.ResourceLimits limits = createResourceLimitsForPlugin(metadataPluginId);
                        resourceLimiter.registerPlugin(metadataPluginId, limits);
                    }
                }

                return pluginInstance;
            } catch (Exception e) {
                logger.error("Error loading plugin from {}: {}", pluginPath, e.getMessage(), e);
                throw new RuntimeException("Failed to load plugin: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public Mono<com.catalis.core.plugin.api.Plugin> loadPluginFromGit(URI repositoryUri, String branch) {
        logger.error("Git repository plugin loading is not supported by this loader");
        return Mono.error(new UnsupportedOperationException("Git repository plugin loading is not supported by this loader"));
    }

    @Override
    public Mono<com.catalis.core.plugin.api.Plugin> loadPluginFromGit(URI repositoryUri) {
        return loadPluginFromGit(repositoryUri, null);
    }

    @Override
    public Flux<com.catalis.core.plugin.api.Plugin> loadPluginsFromClasspath(String basePackage) {
        logger.error("Classpath plugin loading is not supported by this loader. Use ClasspathPluginLoader instead.");
        return Flux.error(new UnsupportedOperationException("Classpath plugin loading is not supported by this loader. Use ClasspathPluginLoader instead."));
    }

    @Override
    public Flux<com.catalis.core.plugin.api.Plugin> loadPluginsFromClasspath() {
        return loadPluginsFromClasspath(null);
    }

    /**
     * Extracts the plugin ID from a JAR file's manifest.
     *
     * @param pluginPath the path to the plugin JAR file
     * @return the plugin ID, or null if not found
     */
    private String extractPluginIdFromJar(Path pluginPath) {
        try (JarFile jarFile = new JarFile(pluginPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                String pluginId = manifest.getMainAttributes().getValue("Plugin-Id");
                if (pluginId != null && !pluginId.isBlank()) {
                    return pluginId;
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to extract plugin ID from JAR manifest: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Creates a set of permissions for a plugin based on configuration.
     *
     * @param pluginId the ID of the plugin
     * @return the set of permissions
     */
    private Set<com.catalis.core.plugin.security.PluginPermission> createPermissionsForPlugin(String pluginId) {
        Set<com.catalis.core.plugin.security.PluginPermission> permissions = new HashSet<>();

        // Get global security settings
        boolean allowFileAccess = properties.getSecurity().isAllowFileAccess();
        boolean allowNetworkAccess = properties.getSecurity().isAllowNetworkAccess();
        List<String> allowedHosts = properties.getSecurity().getAllowedHosts();
        List<String> allowedDirectories = properties.getSecurity().getAllowedDirectories();

        // Check for plugin-specific overrides
        PluginManagerProperties.PluginSecurityProperties pluginProps =
                properties.getSecurity().getPlugins().get(pluginId);

        if (pluginProps != null) {
            if (pluginProps.getAllowFileAccess() != null) {
                allowFileAccess = pluginProps.getAllowFileAccess();
            }

            if (pluginProps.getAllowNetworkAccess() != null) {
                allowNetworkAccess = pluginProps.getAllowNetworkAccess();
            }

            if (!pluginProps.getAllowedHosts().isEmpty()) {
                allowedHosts = pluginProps.getAllowedHosts();
            }

            if (!pluginProps.getAllowedDirectories().isEmpty()) {
                allowedDirectories = pluginProps.getAllowedDirectories();
            }
        }

        // Add file system permissions
        if (allowFileAccess) {
            if (allowedDirectories.isEmpty()) {
                // Allow access to the plugin's own directory
                permissions.add(new com.catalis.core.plugin.security.PluginPermission(
                        com.catalis.core.plugin.security.PluginPermission.Type.FILE_SYSTEM));
            } else {
                // Allow access to specific directories
                for (String dir : allowedDirectories) {
                    permissions.add(new com.catalis.core.plugin.security.PluginPermission(
                            com.catalis.core.plugin.security.PluginPermission.Type.FILE_SYSTEM, dir));
                }
            }
        }

        // Add network permissions
        if (allowNetworkAccess) {
            if (allowedHosts.isEmpty()) {
                // Allow access to all hosts
                permissions.add(new com.catalis.core.plugin.security.PluginPermission(
                        com.catalis.core.plugin.security.PluginPermission.Type.NETWORK));
            } else {
                // Allow access to specific hosts
                for (String host : allowedHosts) {
                    permissions.add(new com.catalis.core.plugin.security.PluginPermission(
                            com.catalis.core.plugin.security.PluginPermission.Type.NETWORK, host));
                }
            }
        }

        // Add system properties permission (read-only)
        permissions.add(new com.catalis.core.plugin.security.PluginPermission(
                com.catalis.core.plugin.security.PluginPermission.Type.SYSTEM_PROPERTIES, null, "read"));

        // Add thread permission (limited)
        permissions.add(new com.catalis.core.plugin.security.PluginPermission(
                com.catalis.core.plugin.security.PluginPermission.Type.THREAD));

        return permissions;
    }

    /**
     * Creates resource limits for a plugin based on configuration.
     *
     * @param pluginId the ID of the plugin
     * @return the resource limits
     */
    private PluginResourceLimiter.ResourceLimits createResourceLimitsForPlugin(String pluginId) {
        // Get global resource limits
        int maxMemoryMb = properties.getResources().getMaxMemoryMb();
        int maxCpuPercentage = properties.getResources().getMaxCpuPercentage();
        int maxThreads = properties.getResources().getMaxThreads();
        int maxFileHandles = properties.getResources().getMaxFileHandles();
        int maxNetworkConnections = properties.getResources().getMaxNetworkConnections();

        // Check for plugin-specific overrides
        PluginManagerProperties.PluginResourceProperties pluginProps =
                properties.getResources().getPlugins().get(pluginId);

        if (pluginProps != null) {
            if (pluginProps.getMaxMemoryMb() != null) {
                maxMemoryMb = pluginProps.getMaxMemoryMb();
            }

            if (pluginProps.getMaxCpuPercentage() != null) {
                maxCpuPercentage = pluginProps.getMaxCpuPercentage();
            }

            if (pluginProps.getMaxThreads() != null) {
                maxThreads = pluginProps.getMaxThreads();
            }

            if (pluginProps.getMaxFileHandles() != null) {
                maxFileHandles = pluginProps.getMaxFileHandles();
            }

            if (pluginProps.getMaxNetworkConnections() != null) {
                maxNetworkConnections = pluginProps.getMaxNetworkConnections();
            }
        }

        // Convert memory from MB to bytes
        long maxMemoryBytes = (long) maxMemoryMb * 1024 * 1024;

        return new PluginResourceLimiter.ResourceLimits(
                maxMemoryBytes,
                maxThreads,
                maxCpuPercentage,
                maxFileHandles,
                maxNetworkConnections);
    }

    /**
     * Validates plugin metadata.
     *
     * @param metadata the plugin metadata to validate
     * @throws IllegalArgumentException if the metadata is invalid
     */
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
