package com.catalis.core.plugin;

import com.catalis.core.plugin.api.ExtensionRegistry;
import com.catalis.core.plugin.api.Plugin;
import com.catalis.core.plugin.api.PluginManager;
import com.catalis.core.plugin.api.PluginRegistry;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.loader.PluginLoader;
import com.catalis.core.plugin.model.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

/**
 * Default implementation of the PluginManager interface.
 */
@Component
public class DefaultPluginManager implements PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginManager.class);

    private final PluginRegistry pluginRegistry;
    private final ExtensionRegistry extensionRegistry;
    private final PluginEventBus eventBus;
    private final PluginLoader pluginLoader;

    /**
     * Creates a new DefaultPluginManager with the specified dependencies.
     *
     * @param pluginRegistry the plugin registry
     * @param extensionRegistry the extension registry
     * @param eventBus the plugin event bus
     * @param pluginLoader the plugin loader
     */
    @Autowired
    public DefaultPluginManager(
            PluginRegistry pluginRegistry,
            ExtensionRegistry extensionRegistry,
            PluginEventBus eventBus,
            PluginLoader pluginLoader) {
        this.pluginRegistry = pluginRegistry;
        this.extensionRegistry = extensionRegistry;
        this.eventBus = eventBus;
        this.pluginLoader = pluginLoader;
    }

    @Override
    public PluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }

    @Override
    public ExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
    }

    @Override
    public PluginEventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Mono<PluginDescriptor> installPlugin(Path pluginPath) {
        logger.info("Installing plugin from path: {}", pluginPath);
        return pluginLoader.loadPlugin(pluginPath)
                .flatMap(plugin -> pluginRegistry.registerPlugin(plugin)
                        .then(pluginRegistry.getPluginDescriptor(plugin.getMetadata().id())));
    }

    public Mono<PluginDescriptor> installPluginFromGit(URI repositoryUri, String branch) {
        logger.info("Installing plugin from Git repository: {}, branch: {}", repositoryUri, branch);
        return pluginLoader.loadPluginFromGit(repositoryUri, branch)
                .flatMap(plugin -> pluginRegistry.registerPlugin(plugin)
                        .then(pluginRegistry.getPluginDescriptor(plugin.getMetadata().id())));
    }

    public Mono<PluginDescriptor> installPluginFromGit(URI repositoryUri) {
        logger.info("Installing plugin from Git repository: {}", repositoryUri);
        return installPluginFromGit(repositoryUri, null);
    }

    public Flux<PluginDescriptor> installPluginsFromClasspath(String basePackage) {
        logger.info("Installing plugins from classpath, base package: {}", basePackage);
        return pluginLoader.loadPluginsFromClasspath(basePackage)
                .flatMap(plugin -> pluginRegistry.registerPlugin(plugin)
                        .then(pluginRegistry.getPluginDescriptor(plugin.getMetadata().id())));
    }

    public Flux<PluginDescriptor> installPluginsFromClasspath() {
        logger.info("Installing plugins from classpath");
        return installPluginsFromClasspath(null);
    }

    @Override
    public Mono<Void> uninstallPlugin(String pluginId) {
        logger.info("Uninstalling plugin: {}", pluginId);
        return pluginRegistry.getPlugin(pluginId)
                .flatMap(Plugin::uninstall)
                .then(pluginRegistry.unregisterPlugin(pluginId));
    }

    @Override
    public Mono<Void> startPlugin(String pluginId) {
        logger.info("Starting plugin: {}", pluginId);
        return pluginRegistry.startPlugin(pluginId);
    }

    @Override
    public Mono<Void> stopPlugin(String pluginId) {
        logger.info("Stopping plugin: {}", pluginId);
        return pluginRegistry.stopPlugin(pluginId);
    }

    @Override
    public Mono<Void> restartPlugin(String pluginId) {
        logger.info("Restarting plugin: {}", pluginId);
        return stopPlugin(pluginId)
                .then(startPlugin(pluginId));
    }

    @Override
    public Mono<Void> updatePluginConfiguration(String pluginId, Map<String, Object> configuration) {
        logger.info("Updating configuration for plugin: {}", pluginId);
        return pluginRegistry.updatePluginConfiguration(pluginId, configuration);
    }

    @Override
    public Flux<PluginDescriptor> getAllPlugins() {
        return pluginRegistry.getAllPluginDescriptors();
    }

    @Override
    public Mono<PluginDescriptor> getPlugin(String pluginId) {
        return pluginRegistry.getPluginDescriptor(pluginId);
    }

    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing plugin manager");
        // Initialize all components
        return Mono.empty();
    }

    @Override
    public Mono<Void> shutdown() {
        logger.info("Shutting down plugin manager");
        // Stop all plugins
        return pluginRegistry.getAllPlugins()
                .flatMap(plugin -> pluginRegistry.stopPlugin(plugin.getMetadata().id()))
                .then();
    }
}
