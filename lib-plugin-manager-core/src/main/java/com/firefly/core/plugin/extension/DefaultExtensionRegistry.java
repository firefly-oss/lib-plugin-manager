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


package com.firefly.core.plugin.extension;

import com.firefly.core.plugin.api.ExtensionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of the ExtensionRegistry interface.
 * This class manages the registration and retrieval of extension points and their implementations.
 */
@Component
public class DefaultExtensionRegistry implements ExtensionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExtensionRegistry.class);

    /**
     * Creates a new DefaultExtensionRegistry.
     * Initializes the internal data structures for tracking extension points and extensions.
     */
    public DefaultExtensionRegistry() {
        // Initialize the internal data structures
    }

    private final Map<String, Class<?>> extensionPoints = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<ExtensionEntry<?>>> extensions = new ConcurrentHashMap<>();

    @Override
    public <T> Mono<Void> registerExtensionPoint(String extensionPointId, Class<T> extensionPointClass) {
        logger.info("Registering extension point: {}", extensionPointId);

        return Mono.fromRunnable(() -> {
            extensionPoints.put(extensionPointId, extensionPointClass);
            extensions.putIfAbsent(extensionPointId, new CopyOnWriteArrayList<>());
        });
    }

    @Override
    public <T> Mono<Void> registerExtension(String extensionPointId, T extension, int priority) {
        logger.info("Registering extension for extension point: {}", extensionPointId);

        return Mono.fromRunnable(() -> {
            Class<?> extensionPointClass = extensionPoints.get(extensionPointId);
            if (extensionPointClass == null) {
                throw new IllegalArgumentException("Extension point not found: " + extensionPointId);
            }

            if (!extensionPointClass.isInstance(extension)) {
                throw new IllegalArgumentException(
                        "Extension does not implement extension point: " + extensionPointClass.getName());
            }

            CopyOnWriteArrayList<ExtensionEntry<?>> extensionList = extensions.computeIfAbsent(
                    extensionPointId, k -> new CopyOnWriteArrayList<>());

            ExtensionEntry<T> entry = new ExtensionEntry<>(extension, priority);
            extensionList.add(entry);

            // Sort by priority (higher values first)
            extensionList.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        });
    }

    @Override
    public <T> Mono<Void> unregisterExtension(String extensionPointId, T extension) {
        logger.info("Unregistering extension from extension point: {}", extensionPointId);

        return Mono.fromRunnable(() -> {
            CopyOnWriteArrayList<ExtensionEntry<?>> extensionList = extensions.get(extensionPointId);
            if (extensionList != null) {
                extensionList.removeIf(entry -> entry.extension().equals(extension));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Flux<T> getExtensions(String extensionPointId) {
        CopyOnWriteArrayList<ExtensionEntry<?>> extensionList = extensions.get(extensionPointId);
        if (extensionList == null) {
            return Flux.empty();
        }

        return Flux.fromIterable(extensionList)
                .map(entry -> (T) entry.extension());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Mono<T> getHighestPriorityExtension(String extensionPointId) {
        CopyOnWriteArrayList<ExtensionEntry<?>> extensionList = extensions.get(extensionPointId);
        if (extensionList == null || extensionList.isEmpty()) {
            return Mono.empty();
        }

        return Mono.just((T) extensionList.get(0).extension());
    }

    @Override
    public Flux<String> getExtensionPoints() {
        return Flux.fromIterable(extensionPoints.keySet());
    }

    /**
     * Record to store an extension along with its priority.
     *
     * @param extension the extension implementation
     * @param priority the priority of the extension
     * @param <T> the type of the extension
     */
    private record ExtensionEntry<T>(T extension, int priority) {
    }
}
