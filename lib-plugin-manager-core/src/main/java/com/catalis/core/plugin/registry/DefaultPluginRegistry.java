package com.catalis.core.plugin.registry;

import com.catalis.core.plugin.api.Plugin;
import com.catalis.core.plugin.api.PluginRegistry;
import com.catalis.core.plugin.event.PluginConfigurationEvent;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.event.PluginLifecycleEvent;
import com.catalis.core.plugin.model.PluginDescriptor;
import com.catalis.core.plugin.model.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the PluginRegistry interface.
 */
@Component
public class DefaultPluginRegistry implements PluginRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginRegistry.class);

    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    private final Map<String, PluginDescriptor> descriptors = new ConcurrentHashMap<>();
    private final PluginEventBus eventBus;

    /**
     * Creates a new DefaultPluginRegistry with the specified event bus.
     *
     * @param eventBus the plugin event bus
     */
    @Autowired
    public DefaultPluginRegistry(PluginEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public Mono<Void> registerPlugin(Plugin plugin) {
        String pluginId = plugin.getMetadata().id();
        logger.info("Registering plugin: {}", pluginId);

        return Mono.fromRunnable(() -> {
            plugins.put(pluginId, plugin);

            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .metadata(plugin.getMetadata())
                    .state(PluginState.INSTALLED)
                    .configuration(Map.of())
                    .build();

            descriptors.put(pluginId, descriptor);
        }).then(plugin.initialize())
        .then(Mono.defer(() -> {
            PluginDescriptor currentDescriptor = descriptors.get(pluginId);
            PluginDescriptor updatedDescriptor = PluginDescriptor.builder()
                    .metadata(currentDescriptor.metadata())
                    .state(PluginState.INITIALIZED)
                    .configuration(currentDescriptor.configuration())
                    .classLoaderInfo(currentDescriptor.classLoaderInfo())
                    .location(currentDescriptor.location())
                    .build();

            descriptors.put(pluginId, updatedDescriptor);

            // Publish lifecycle event
            return eventBus.publish(new PluginLifecycleEvent(
                    pluginId, PluginState.INSTALLED, PluginState.INITIALIZED));
        }));
    }

    @Override
    public Mono<Void> unregisterPlugin(String pluginId) {
        logger.info("Unregistering plugin: {}", pluginId);

        return Mono.defer(() -> {
            plugins.remove(pluginId);

            PluginDescriptor currentDescriptor = descriptors.get(pluginId);
            if (currentDescriptor != null) {
                PluginState previousState = currentDescriptor.state();

                PluginDescriptor updatedDescriptor = PluginDescriptor.builder()
                        .metadata(currentDescriptor.metadata())
                        .state(PluginState.UNINSTALLED)
                        .configuration(currentDescriptor.configuration())
                        .classLoaderInfo(currentDescriptor.classLoaderInfo())
                        .location(currentDescriptor.location())
                        .build();

                descriptors.put(pluginId, updatedDescriptor);

                // Publish lifecycle event
                return eventBus.publish(new PluginLifecycleEvent(
                        pluginId, previousState, PluginState.UNINSTALLED));
            }
            return Mono.empty();
        });
    }

    @Override
    public Mono<Plugin> getPlugin(String pluginId) {
        return Mono.justOrEmpty(plugins.get(pluginId));
    }

    @Override
    public Mono<PluginDescriptor> getPluginDescriptor(String pluginId) {
        return Mono.justOrEmpty(descriptors.get(pluginId));
    }

    @Override
    public Flux<Plugin> getAllPlugins() {
        return Flux.fromIterable(plugins.values());
    }

    @Override
    public Flux<PluginDescriptor> getAllPluginDescriptors() {
        return Flux.fromIterable(descriptors.values());
    }

    @Override
    public Flux<Plugin> getPluginsByState(PluginState state) {
        return getAllPluginDescriptors()
                .filter(descriptor -> descriptor.state() == state)
                .flatMap(descriptor -> getPlugin(descriptor.metadata().id()));
    }

    @Override
    public Mono<Void> startPlugin(String pluginId) {
        logger.info("Starting plugin: {}", pluginId);

        return getPlugin(pluginId)
                .flatMap(plugin -> {
                    PluginDescriptor currentDescriptor = descriptors.get(pluginId);
                    if (currentDescriptor.state() == PluginState.STARTED) {
                        return Mono.empty(); // Already started
                    }

                    return plugin.start()
                            .then(Mono.defer(() -> {
                                PluginState previousState = currentDescriptor.state();

                                PluginDescriptor updatedDescriptor = PluginDescriptor.builder()
                                        .metadata(currentDescriptor.metadata())
                                        .state(PluginState.STARTED)
                                        .configuration(currentDescriptor.configuration())
                                        .classLoaderInfo(currentDescriptor.classLoaderInfo())
                                        .location(currentDescriptor.location())
                                        .build();

                                descriptors.put(pluginId, updatedDescriptor);

                                // Publish lifecycle event
                                return eventBus.publish(new PluginLifecycleEvent(
                                        pluginId, previousState, PluginState.STARTED));
                            }));
                })
                .onErrorResume(error -> {
                    logger.error("Error starting plugin {}: {}", pluginId, error.getMessage(), error);

                    PluginDescriptor currentDescriptor = descriptors.get(pluginId);
                    PluginState previousState = currentDescriptor.state();

                    PluginDescriptor updatedDescriptor = PluginDescriptor.builder()
                            .metadata(currentDescriptor.metadata())
                            .state(PluginState.FAILED)
                            .configuration(currentDescriptor.configuration())
                            .classLoaderInfo(currentDescriptor.classLoaderInfo())
                            .location(currentDescriptor.location())
                            .build();

                    descriptors.put(pluginId, updatedDescriptor);

                    // Publish lifecycle event
                    return eventBus.publish(new PluginLifecycleEvent(
                            pluginId, previousState, PluginState.FAILED))
                            .then(Mono.error(error));
                });
    }

    @Override
    public Mono<Void> stopPlugin(String pluginId) {
        logger.info("Stopping plugin: {}", pluginId);

        return getPlugin(pluginId)
                .flatMap(plugin -> {
                    PluginDescriptor currentDescriptor = descriptors.get(pluginId);
                    if (currentDescriptor.state() != PluginState.STARTED) {
                        return Mono.empty(); // Not started
                    }

                    return plugin.stop()
                            .then(Mono.defer(() -> {
                                PluginState previousState = currentDescriptor.state();

                                PluginDescriptor updatedDescriptor = PluginDescriptor.builder()
                                        .metadata(currentDescriptor.metadata())
                                        .state(PluginState.STOPPED)
                                        .configuration(currentDescriptor.configuration())
                                        .classLoaderInfo(currentDescriptor.classLoaderInfo())
                                        .location(currentDescriptor.location())
                                        .build();

                                descriptors.put(pluginId, updatedDescriptor);

                                // Publish lifecycle event
                                return eventBus.publish(new PluginLifecycleEvent(
                                        pluginId, previousState, PluginState.STOPPED));
                            }));
                })
                .onErrorResume(error -> {
                    logger.error("Error stopping plugin {}: {}", pluginId, error.getMessage(), error);

                    PluginDescriptor currentDescriptor = descriptors.get(pluginId);
                    PluginState previousState = currentDescriptor.state();

                    PluginDescriptor updatedDescriptor = PluginDescriptor.builder()
                            .metadata(currentDescriptor.metadata())
                            .state(PluginState.FAILED)
                            .configuration(currentDescriptor.configuration())
                            .classLoaderInfo(currentDescriptor.classLoaderInfo())
                            .location(currentDescriptor.location())
                            .build();

                    descriptors.put(pluginId, updatedDescriptor);

                    // Publish lifecycle event
                    return eventBus.publish(new PluginLifecycleEvent(
                            pluginId, previousState, PluginState.FAILED))
                            .then(Mono.error(error));
                });
    }

    @Override
    public Mono<Void> updatePluginConfiguration(String pluginId, Map<String, Object> configuration) {
        logger.info("Updating configuration for plugin: {}", pluginId);

        return getPluginDescriptor(pluginId)
                .flatMap(currentDescriptor -> {
                    Map<String, Object> previousConfiguration = currentDescriptor.configuration();

                    PluginDescriptor updatedDescriptor = PluginDescriptor.builder()
                            .metadata(currentDescriptor.metadata())
                            .state(currentDescriptor.state())
                            .configuration(configuration)
                            .classLoaderInfo(currentDescriptor.classLoaderInfo())
                            .location(currentDescriptor.location())
                            .build();

                    descriptors.put(pluginId, updatedDescriptor);

                    // Publish configuration event
                    return eventBus.publish(new PluginConfigurationEvent(
                            pluginId, previousConfiguration, configuration));
                });
    }
}
