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


package com.firefly.core.plugin.hotdeploy;

import com.firefly.core.plugin.api.PluginManager;
import com.firefly.core.plugin.config.PluginManagerProperties;
import com.firefly.core.plugin.model.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches the plugins directory for changes and automatically deploys, updates, or removes plugins.
 * This component is only active if hot deployment is enabled in the configuration.
 */
@Component
@ConditionalOnProperty(prefix = "firefly.plugin-manager.hot-deployment", name = "enabled", havingValue = "true")
public class PluginDirectoryWatcher implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(PluginDirectoryWatcher.class);

    private final PluginManager pluginManager;
    private final PluginManagerProperties properties;
    private final Map<Path, Long> knownPlugins = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private WatchService watchService;
    private Disposable watchDisposable;
    private Disposable pollingDisposable;

    /**
     * Creates a new PluginDirectoryWatcher.
     *
     * @param pluginManager the plugin manager
     * @param properties the plugin manager properties
     */
    @Autowired
    public PluginDirectoryWatcher(PluginManager pluginManager, PluginManagerProperties properties) {
        this.pluginManager = pluginManager;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (properties.getHotDeployment().isEnabled()) {
            start();
        }
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    /**
     * Starts watching the plugins directory.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting plugin directory watcher");

            // Check if hot deployment is enabled
            if (!properties.getHotDeployment().isEnabled()) {
                logger.info("Hot deployment is disabled, not starting directory watcher");
                running.set(false);
                return;
            }

            // Create the plugins directory if it doesn't exist
            Path pluginsDir = properties.getPluginsDirectory();
            try {
                Files.createDirectories(pluginsDir);
            } catch (IOException e) {
                logger.error("Failed to create plugins directory: {}", pluginsDir, e);
                running.set(false);
                return;
            }

            // Initialize the known plugins map
            try {
                Files.walkFileTree(pluginsDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isPluginFile(file)) {
                            knownPlugins.put(file, attrs.lastModifiedTime().toMillis());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                logger.error("Failed to scan plugins directory: {}", pluginsDir, e);
                running.set(false);
                return;
            }

            // Start watching for changes using WatchService
            if (properties.getHotDeployment().getPollingIntervalMs() <= 0) {
                startWatchService();
            } else {
                // Start polling for changes
                startPolling();
            }
        }
    }

    /**
     * Stops watching the plugins directory.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("Stopping plugin directory watcher");

            // Dispose of the watch service
            if (watchService != null) {
                try {
                    watchService.close();
                    watchService = null;
                } catch (IOException e) {
                    logger.error("Failed to close watch service", e);
                }
            }

            // Dispose of the watch disposable
            if (watchDisposable != null && !watchDisposable.isDisposed()) {
                watchDisposable.dispose();
                watchDisposable = null;
            }

            // Dispose of the polling disposable
            if (pollingDisposable != null && !pollingDisposable.isDisposed()) {
                pollingDisposable.dispose();
                pollingDisposable = null;
            }

            // Clear the known plugins map
            knownPlugins.clear();
        }
    }

    /**
     * Starts watching for changes using the WatchService API.
     */
    private void startWatchService() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path pluginsDir = properties.getPluginsDirectory();

            // Register for events
            pluginsDir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            // Start a thread to watch for events
            watchDisposable = Flux.create(sink -> {
                try {
                    while (running.get()) {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();

                            // Skip OVERFLOW events
                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }

                            // Get the file name from the event
                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                            Path fileName = pathEvent.context();
                            Path fullPath = pluginsDir.resolve(fileName);

                            // Only process plugin files
                            if (!isPluginFile(fullPath)) {
                                continue;
                            }

                            // Emit the event
                            sink.next(new PluginFileEvent(fullPath, kind));
                        }

                        // Reset the key for further events
                        boolean valid = key.reset();
                        if (!valid) {
                            sink.complete();
                            break;
                        }
                    }
                } catch (Exception e) {
                    if (running.get()) {
                        sink.error(e);
                    } else {
                        sink.complete();
                    }
                }
            })
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(this::handleWatchEvent,
                    e -> logger.error("Error in watch service", e),
                    () -> logger.info("Watch service completed"));

        } catch (IOException e) {
            logger.error("Failed to start watch service", e);
            running.set(false);
        }
    }

    /**
     * Starts polling for changes at regular intervals.
     */
    private void startPolling() {
        long intervalMs = properties.getHotDeployment().getPollingIntervalMs();
        Path pluginsDir = properties.getPluginsDirectory();

        pollingDisposable = Flux.interval(Duration.ofMillis(intervalMs))
                .takeWhile(i -> running.get())
                .flatMap(i -> scanPluginsDirectory(pluginsDir))
                .subscribe(
                        changes -> logger.debug("Processed {} plugin changes", changes),
                        e -> logger.error("Error in polling", e),
                        () -> logger.info("Polling completed")
                );
    }

    /**
     * Scans the plugins directory for changes.
     *
     * @param pluginsDir the plugins directory
     * @return a Mono that emits the number of changes processed
     */
    private Mono<Integer> scanPluginsDirectory(Path pluginsDir) {
        return Mono.fromCallable(() -> {
            Map<Path, Long> currentPlugins = new HashMap<>();
            int changes = 0;

            // Scan the directory for current plugins
            try {
                Files.walkFileTree(pluginsDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isPluginFile(file)) {
                            currentPlugins.put(file, attrs.lastModifiedTime().toMillis());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                logger.error("Failed to scan plugins directory: {}", pluginsDir, e);
                return 0;
            }

            // Check for new or modified plugins
            for (Map.Entry<Path, Long> entry : currentPlugins.entrySet()) {
                Path path = entry.getKey();
                Long currentTimestamp = entry.getValue();
                Long knownTimestamp = knownPlugins.get(path);

                if (knownTimestamp == null) {
                    // New plugin
                    if (properties.getHotDeployment().isWatchForNewPlugins()) {
                        handleNewPlugin(path);
                        changes++;
                    }
                    knownPlugins.put(path, currentTimestamp);
                } else if (!currentTimestamp.equals(knownTimestamp)) {
                    // Modified plugin
                    if (properties.getHotDeployment().isWatchForPluginUpdates()) {
                        handleModifiedPlugin(path);
                        changes++;
                    }
                    knownPlugins.put(path, currentTimestamp);
                }
            }

            // Check for deleted plugins
            if (properties.getHotDeployment().isWatchForPluginDeletions()) {
                for (Path path : knownPlugins.keySet().toArray(new Path[0])) {
                    if (!currentPlugins.containsKey(path)) {
                        // Deleted plugin
                        handleDeletedPlugin(path);
                        knownPlugins.remove(path);
                        changes++;
                    }
                }
            }

            return changes;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Handles a watch event.
     *
     * @param event the watch event
     */
    private void handleWatchEvent(Object event) {
        if (!(event instanceof PluginFileEvent)) {
            return;
        }

        PluginFileEvent fileEvent = (PluginFileEvent) event;
        Path path = fileEvent.getPath();
        WatchEvent.Kind<?> kind = fileEvent.getKind();

        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            if (properties.getHotDeployment().isWatchForNewPlugins()) {
                handleNewPlugin(path);
            }
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            if (properties.getHotDeployment().isWatchForPluginUpdates()) {
                handleModifiedPlugin(path);
            }
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            if (properties.getHotDeployment().isWatchForPluginDeletions()) {
                handleDeletedPlugin(path);
            }
        }
    }

    /**
     * Handles a new plugin file.
     *
     * @param path the path to the plugin file
     */
    private void handleNewPlugin(Path path) {
        logger.info("New plugin detected: {}", path);

        if (properties.getHotDeployment().isAutoReload()) {
            pluginManager.installPlugin(path)
                    .doOnSuccess(descriptor -> {
                        logger.info("Successfully installed new plugin: {}", descriptor.getId());
                        if (properties.isAutoStartPlugins()) {
                            pluginManager.startPlugin(descriptor.getId())
                                    .doOnSuccess(v -> logger.info("Successfully started new plugin: {}", descriptor.getId()))
                                    .doOnError(e -> logger.error("Failed to start new plugin: {}", descriptor.getId(), e))
                                    .subscribe();
                        }
                    })
                    .doOnError(e -> logger.error("Failed to install new plugin: {}", path, e))
                    .subscribe();
        }
    }

    /**
     * Handles a modified plugin file.
     *
     * @param path the path to the plugin file
     */
    private void handleModifiedPlugin(Path path) {
        logger.info("Plugin update detected: {}", path);

        if (properties.getHotDeployment().isAutoReload()) {
            // Find the plugin ID for this path
            findPluginIdForPath(path)
                    .flatMap(pluginId -> {
                        logger.info("Reloading plugin: {}", pluginId);
                        return pluginManager.uninstallPlugin(pluginId)
                                .then(pluginManager.installPlugin(path))
                                .doOnSuccess(descriptor -> {
                                    logger.info("Successfully reloaded plugin: {}", descriptor.getId());
                                    if (properties.isAutoStartPlugins()) {
                                        pluginManager.startPlugin(descriptor.getId())
                                                .doOnSuccess(v -> logger.info("Successfully started reloaded plugin: {}", descriptor.getId()))
                                                .doOnError(e -> logger.error("Failed to start reloaded plugin: {}", descriptor.getId(), e))
                                                .subscribe();
                                    }
                                });
                    })
                    .doOnError(e -> logger.error("Failed to reload plugin: {}", path, e))
                    .subscribe();
        }
    }

    /**
     * Handles a deleted plugin file.
     *
     * @param path the path to the plugin file
     */
    private void handleDeletedPlugin(Path path) {
        logger.info("Plugin deletion detected: {}", path);

        if (properties.getHotDeployment().isAutoReload()) {
            // Find the plugin ID for this path
            findPluginIdForPath(path)
                    .flatMap(pluginId -> {
                        logger.info("Uninstalling plugin: {}", pluginId);
                        return pluginManager.uninstallPlugin(pluginId);
                    })
                    .doOnSuccess(v -> logger.info("Successfully uninstalled plugin: {}", path))
                    .doOnError(e -> logger.error("Failed to uninstall plugin: {}", path, e))
                    .subscribe();
        }
    }

    /**
     * Finds the plugin ID for a given path.
     *
     * @param path the path to the plugin file
     * @return a Mono that emits the plugin ID, or an empty Mono if not found
     */
    private Mono<String> findPluginIdForPath(Path path) {
        return pluginManager.getAllPlugins()
                .filter(descriptor -> {
                    // Check if this descriptor matches the path
                    // This is a simplistic approach; in a real implementation, you would need
                    // a more robust way to map paths to plugin IDs
                    String fileName = path.getFileName().toString();
                    String pluginId = descriptor.getId();
                    return fileName.contains(pluginId);
                })
                .map(PluginDescriptor::getId)
                .next()
                .switchIfEmpty(Mono.defer(() -> {
                    // If we can't find the plugin ID, try to install it temporarily to get the ID
                    return pluginManager.installPlugin(path)
                            .map(PluginDescriptor::getId)
                            .doOnSuccess(id -> {
                                // Uninstall it again since we're just trying to get the ID
                                pluginManager.uninstallPlugin(id).subscribe();
                            });
                }));
    }

    /**
     * Checks if a file is a plugin file (JAR file).
     *
     * @param path the path to check
     * @return true if the file is a plugin file, false otherwise
     */
    private boolean isPluginFile(Path path) {
        return path.toString().toLowerCase().endsWith(".jar");
    }

    /**
     * Event class for plugin file events.
     */
    private static class PluginFileEvent {
        private final Path path;
        private final WatchEvent.Kind<?> kind;

        public PluginFileEvent(Path path, WatchEvent.Kind<?> kind) {
            this.path = path;
            this.kind = kind;
        }

        public Path getPath() {
            return path;
        }

        public WatchEvent.Kind<?> getKind() {
            return kind;
        }
    }
}
