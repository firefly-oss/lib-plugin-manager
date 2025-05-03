package com.catalis.core.plugin;

import com.catalis.core.plugin.api.ExtensionRegistry;
import com.catalis.core.plugin.api.Plugin;
import com.catalis.core.plugin.api.PluginManager;
import com.catalis.core.plugin.api.PluginRegistry;
import com.catalis.core.plugin.dependency.PluginDependencyResolver;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.loader.PluginLoader;
import com.catalis.core.plugin.model.PluginDescriptor;
import com.catalis.core.plugin.model.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final PluginDependencyResolver dependencyResolver;

    /**
     * Creates a new DefaultPluginManager with the specified dependencies.
     *
     * @param pluginRegistry the plugin registry
     * @param extensionRegistry the extension registry
     * @param eventBus the plugin event bus
     * @param pluginLoader the plugin loader
     * @param dependencyResolver the plugin dependency resolver
     */
    @Autowired
    public DefaultPluginManager(
            PluginRegistry pluginRegistry,
            ExtensionRegistry extensionRegistry,
            PluginEventBus eventBus,
            PluginLoader pluginLoader,
            PluginDependencyResolver dependencyResolver) {
        this.pluginRegistry = pluginRegistry;
        this.extensionRegistry = extensionRegistry;
        this.eventBus = eventBus;
        this.pluginLoader = pluginLoader;
        this.dependencyResolver = dependencyResolver;
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
        return pluginRegistry.getPluginDescriptor(pluginId)
                .flatMap(descriptor -> {
                    // Get all plugins
                    return pluginRegistry.getAllPluginDescriptors()
                            .collectList()
                            .flatMap(allPlugins -> {
                                try {
                                    // Resolve dependencies for this plugin
                                    List<PluginDescriptor> dependencyOrder = dependencyResolver.resolveDependencies(allPlugins);

                                    // Filter to include only this plugin and its dependencies
                                    List<String> pluginsToStart = dependencyOrder.stream()
                                            .map(PluginDescriptor::getId)
                                            .takeWhile(id -> !id.equals(pluginId))
                                            .collect(Collectors.toList());
                                    pluginsToStart.add(pluginId);

                                    logger.debug("Starting plugin {} and its dependencies: {}", pluginId, pluginsToStart);

                                    // Start each plugin in dependency order
                                    return Flux.fromIterable(pluginsToStart)
                                            .concatMap(id -> {
                                                // Only start plugins that aren't already started
                                                return pluginRegistry.getPluginDescriptor(id)
                                                        .filter(desc -> desc.state() != PluginState.STARTED)
                                                        .flatMap(desc -> pluginRegistry.startPlugin(id))
                                                        .onErrorResume(e -> {
                                                            logger.error("Error starting dependency {} for plugin {}: {}",
                                                                    id, pluginId, e.getMessage(), e);
                                                            return Mono.empty();
                                                        });
                                            })
                                            .then();
                                } catch (Exception e) {
                                    logger.error("Error resolving dependencies for plugin {}: {}", pluginId, e.getMessage(), e);
                                    return Mono.error(e);
                                }
                            });
                });
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

    /**
     * Starts all plugins in dependency order.
     *
     * @return a Mono that completes when all plugins have been started
     */
    public Mono<Void> startAllPlugins() {
        logger.info("Starting all plugins in dependency order");
        return pluginRegistry.getAllPluginDescriptors()
                .collectList()
                .flatMap(plugins -> {
                    try {
                        // Resolve dependencies for all plugins
                        List<PluginDescriptor> dependencyOrder = dependencyResolver.resolveDependencies(plugins);
                        logger.debug("Resolved dependency order: {}",
                                dependencyOrder.stream().map(PluginDescriptor::getId).collect(Collectors.toList()));

                        // Start each plugin in dependency order
                        return Flux.fromIterable(dependencyOrder)
                                .concatMap(plugin -> {
                                    String id = plugin.getId();
                                    // Only start plugins that aren't already started
                                    return pluginRegistry.getPluginDescriptor(id)
                                            .filter(desc -> desc.state() != PluginState.STARTED)
                                            .flatMap(desc -> {
                                                logger.info("Starting plugin {} in dependency order", id);
                                                return pluginRegistry.startPlugin(id);
                                            })
                                            .onErrorResume(e -> {
                                                logger.error("Error starting plugin {}: {}", id, e.getMessage(), e);
                                                return Mono.empty();
                                            });
                                })
                                .then();
                    } catch (Exception e) {
                        logger.error("Error resolving dependencies for all plugins: {}", e.getMessage(), e);
                        return Mono.error(e);
                    }
                });
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
