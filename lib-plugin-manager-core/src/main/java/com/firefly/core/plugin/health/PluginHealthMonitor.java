/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.core.plugin.health;

import com.firefly.core.plugin.api.Plugin;
import com.firefly.core.plugin.api.PluginHealthIndicator;
import com.firefly.core.plugin.api.PluginManager;
import com.firefly.core.plugin.config.PluginManagerProperties;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.model.PluginHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitors the health of plugins and publishes health events.
 */
@Component
public class PluginHealthMonitor implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(PluginHealthMonitor.class);

    private final PluginManager pluginManager;
    private final PluginEventBus eventBus;
    private final PluginManagerProperties properties;
    private final Map<String, PluginHealth> healthCache = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Disposable monitoringDisposable;

    /**
     * Creates a new PluginHealthMonitor.
     *
     * @param pluginManager the plugin manager
     * @param eventBus the plugin event bus
     * @param properties the plugin manager properties
     */
    @Autowired
    public PluginHealthMonitor(
            PluginManager pluginManager,
            PluginEventBus eventBus,
            PluginManagerProperties properties) {
        this.pluginManager = pluginManager;
        this.eventBus = eventBus;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    /**
     * Starts monitoring plugin health.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting plugin health monitor");

            // Start monitoring at regular intervals
            long intervalMs = properties.getHealth().getMonitoringIntervalMs();

            monitoringDisposable = Flux.interval(Duration.ofMillis(intervalMs))
                    .takeWhile(i -> running.get())
                    .flatMap(i -> checkAllPluginHealth())
                    .subscribe(
                            health -> {
                                // Cache the health status
                                healthCache.put(health.getPluginId(), health);

                                // Publish a health event
                                eventBus.publish(new PluginHealthEvent(health));

                                // Check if we need to take action based on the health status
                                if (properties.getHealth().isAutoRecoveryEnabled() &&
                                        health.getStatus() == PluginHealth.Status.DOWN) {
                                    recoverPlugin(health.getPluginId());
                                }
                            },
                            e -> logger.error("Error in health monitoring", e),
                            () -> logger.info("Health monitoring completed")
                    );
        }
    }

    /**
     * Stops monitoring plugin health.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping plugin health monitor");

            // Dispose of the monitoring disposable
            if (monitoringDisposable != null && !monitoringDisposable.isDisposed()) {
                monitoringDisposable.dispose();
                monitoringDisposable = null;
            }

            // Clear the health cache
            healthCache.clear();
        }
    }

    /**
     * Checks the health of all plugins.
     *
     * @return a Flux that emits the health of each plugin
     */
    public Flux<PluginHealth> checkAllPluginHealth() {
        return pluginManager.getAllPlugins()
                .flatMap(descriptor -> {
                    String pluginId = descriptor.getId();
                    return checkPluginHealth(pluginId)
                            .onErrorResume(e -> {
                                logger.error("Error checking health for plugin: {}", pluginId, e);
                                return Mono.just(PluginHealth.down(pluginId, "Error checking health: " + e.getMessage()));
                            });
                });
    }

    /**
     * Checks the health of a specific plugin.
     *
     * @param pluginId the plugin ID
     * @return a Mono that emits the plugin health
     */
    public Mono<PluginHealth> checkPluginHealth(String pluginId) {
        return pluginManager.getPluginRegistry().getPlugin(pluginId)
                .flatMap(plugin -> {
                    // Check if the plugin implements PluginHealthIndicator
                    if (plugin instanceof PluginHealthIndicator) {
                        return ((PluginHealthIndicator) plugin).health();
                    } else {
                        // Use default health check
                        return defaultHealthCheck(plugin);
                    }
                })
                .defaultIfEmpty(PluginHealth.unknown(pluginId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Gets the cached health status for a plugin.
     *
     * @param pluginId the plugin ID
     * @return the cached health status, or null if not available
     */
    public PluginHealth getCachedHealth(String pluginId) {
        return healthCache.get(pluginId);
    }

    /**
     * Gets the cached health status for all plugins.
     *
     * @return a map of plugin IDs to health statuses
     */
    public Map<String, PluginHealth> getAllCachedHealth() {
        return new ConcurrentHashMap<>(healthCache);
    }

    /**
     * Performs a default health check for a plugin.
     *
     * @param plugin the plugin
     * @return a Mono that emits the plugin health
     */
    private Mono<PluginHealth> defaultHealthCheck(Plugin plugin) {
        String pluginId = plugin.getMetadata().id();

        // Check if the plugin is in the registry and has the correct state
        return pluginManager.getPluginRegistry().getPluginDescriptor(pluginId)
                .map(descriptor -> {
                    switch (descriptor.getState()) {
                        case STARTED:
                            return PluginHealth.up(pluginId);
                        case STOPPED:
                            return PluginHealth.down(pluginId, "Plugin is stopped");
                        case FAILED:
                            return PluginHealth.down(pluginId, "Plugin failed to start");
                        default:
                            return PluginHealth.unknown(pluginId);
                    }
                });
    }

    /**
     * Attempts to recover a plugin that is in a DOWN state.
     *
     * @param pluginId the plugin ID
     */
    private void recoverPlugin(String pluginId) {
        logger.info("Attempting to recover plugin: {}", pluginId);

        // Get the current health status
        PluginHealth health = healthCache.get(pluginId);
        if (health == null || health.getStatus() != PluginHealth.Status.DOWN) {
            return;
        }

        // Check if we've exceeded the maximum recovery attempts
        int maxRecoveryAttempts = properties.getHealth().getMaxRecoveryAttempts();
        Integer currentAttempts = (Integer) health.getDetails().getOrDefault("recoveryAttempts", 0);

        if (currentAttempts >= maxRecoveryAttempts) {
            logger.warn("Maximum recovery attempts ({}) reached for plugin: {}", maxRecoveryAttempts, pluginId);
            return;
        }

        // Increment the recovery attempts
        healthCache.put(pluginId, health.withDetail("recoveryAttempts", currentAttempts + 1));

        // Try to restart the plugin
        try {
            pluginManager.restartPlugin(pluginId)
                    .doOnSuccess(v -> {
                        logger.info("Successfully recovered plugin: {}", pluginId);
                        // Reset the recovery attempts on success
                        PluginHealth currentHealth = healthCache.get(pluginId);
                        if (currentHealth != null) {
                            healthCache.put(pluginId, currentHealth.withDetail("recoveryAttempts", 0));
                        }
                    })
                    .doOnError(e -> logger.error("Failed to recover plugin: {}", pluginId, e))
                    .subscribe();
        } catch (Exception e) {
            logger.error("Exception while attempting to recover plugin: {}", pluginId, e);
        }
    }
}
