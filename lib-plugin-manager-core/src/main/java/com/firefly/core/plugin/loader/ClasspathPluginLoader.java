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


package com.firefly.core.plugin.loader;

import com.firefly.core.plugin.model.PluginMetadata;
import com.firefly.core.plugin.spi.AbstractPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Plugin loader that loads plugins from the classpath.
 */
@Component
public class ClasspathPluginLoader implements PluginLoader {

    private static final Logger logger = LoggerFactory.getLogger(ClasspathPluginLoader.class);
    private final ApplicationContext applicationContext;
    private final DefaultPluginLoader defaultPluginLoader;

    /**
     * Creates a new ClasspathPluginLoader.
     *
     * @param applicationContext the Spring application context
     * @param defaultPluginLoader the default plugin loader to delegate to for JAR loading
     */
    public ClasspathPluginLoader(ApplicationContext applicationContext, DefaultPluginLoader defaultPluginLoader) {
        this.applicationContext = applicationContext;
        this.defaultPluginLoader = defaultPluginLoader;
    }

    @Override
    public Mono<com.firefly.core.plugin.api.Plugin> loadPlugin(Path pluginPath) {
        // Delegate to the default plugin loader
        return defaultPluginLoader.loadPlugin(pluginPath);
    }

    @Override
    public Flux<com.firefly.core.plugin.api.Plugin> loadPluginsFromClasspath(String basePackage) {
        logger.info("Scanning classpath for plugins in package: {}", basePackage != null ? basePackage : "all packages");

        return Flux.defer(() -> {
            try {
                // Create a scanner that looks for @Plugin annotations
                ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
                scanner.addIncludeFilter(new AnnotationTypeFilter(com.firefly.core.plugin.annotation.Plugin.class));

                // Scan for classes with @Plugin annotation
                Set<BeanDefinition> beanDefinitions;
                if (basePackage != null && !basePackage.isEmpty()) {
                    beanDefinitions = scanner.findCandidateComponents(basePackage);
                } else {
                    // Scan common base packages if no specific package is provided
                    beanDefinitions = new HashSet<>();
                    beanDefinitions.addAll(scanner.findCandidateComponents("com"));
                    beanDefinitions.addAll(scanner.findCandidateComponents("org"));
                    beanDefinitions.addAll(scanner.findCandidateComponents("net"));
                    beanDefinitions.addAll(scanner.findCandidateComponents("io"));
                }

                // Process each found class
                return Flux.fromIterable(beanDefinitions)
                        .flatMap(beanDefinition -> {
                            try {
                                String className = beanDefinition.getBeanClassName();
                                logger.info("Found plugin class: {}", className);

                                // Load the class
                                Class<?> pluginClass = Class.forName(className);

                                // Check if it's a Plugin implementation
                                if (com.firefly.core.plugin.api.Plugin.class.isAssignableFrom(pluginClass)) {
                                    // Try to get from Spring context first
                                    try {
                                        @SuppressWarnings("unchecked")
                                        Class<? extends com.firefly.core.plugin.api.Plugin> pluginImplClass =
                                                (Class<? extends com.firefly.core.plugin.api.Plugin>) pluginClass;

                                        com.firefly.core.plugin.api.Plugin plugin = applicationContext.getBean(pluginImplClass);
                                        return Mono.just(plugin);
                                    } catch (Exception e) {
                                        logger.debug("Plugin not found in Spring context, creating new instance: {}", className);

                                        // Create a new instance
                                        @SuppressWarnings("unchecked")
                                        com.firefly.core.plugin.api.Plugin plugin =
                                                ((Class<? extends com.firefly.core.plugin.api.Plugin>) pluginClass)
                                                .getDeclaredConstructor().newInstance();

                                        return Mono.just(plugin);
                                    }
                                } else {
                                    // It's a class with @Plugin annotation but doesn't implement Plugin interface
                                    // Create a wrapper plugin
                                    return createWrapperPlugin(pluginClass);
                                }
                            } catch (Exception e) {
                                logger.error("Failed to load plugin class: {}", beanDefinition.getBeanClassName(), e);
                                return Mono.empty();
                            }
                        });
            } catch (Exception e) {
                logger.error("Error scanning classpath for plugins", e);
                return Flux.error(e);
            }
        });
    }

    @Override
    public Flux<com.firefly.core.plugin.api.Plugin> loadPluginsFromClasspath() {
        return loadPluginsFromClasspath(null);
    }

    @Override
    public Mono<com.firefly.core.plugin.api.Plugin> loadPluginFromGit(URI repositoryUri, String branch) {
        logger.error("Git repository plugin loading is not supported by this loader. Use GitPluginLoader instead.");
        return Mono.error(new UnsupportedOperationException("Git repository plugin loading is not supported by this loader. Use GitPluginLoader instead."));
    }

    @Override
    public Mono<com.firefly.core.plugin.api.Plugin> loadPluginFromGit(URI repositoryUri) {
        return loadPluginFromGit(repositoryUri, null);
    }

    /**
     * Creates a wrapper plugin for a class that has the @Plugin annotation but doesn't implement the Plugin interface.
     *
     * @param pluginClass the class with @Plugin annotation
     * @return a Mono that emits the wrapper plugin
     */
    private Mono<com.firefly.core.plugin.api.Plugin> createWrapperPlugin(Class<?> pluginClass) {
        try {
            // Get the @Plugin annotation
            com.firefly.core.plugin.annotation.Plugin annotation = pluginClass.getAnnotation(com.firefly.core.plugin.annotation.Plugin.class);
            if (annotation == null) {
                return Mono.error(new IllegalArgumentException("Class does not have @Plugin annotation: " + pluginClass.getName()));
            }

            // Create metadata from the annotation
            Set<String> dependencies = new HashSet<>(Arrays.asList(annotation.dependencies()));
            PluginMetadata metadata = PluginMetadata.builder()
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

            // Create an instance of the class
            Object instance;
            try {
                // Try to get from Spring context first
                instance = applicationContext.getBean(pluginClass);
            } catch (Exception e) {
                // Create a new instance
                Constructor<?> constructor = pluginClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                instance = constructor.newInstance();
            }

            // Create a wrapper plugin
            final Object finalInstance = instance;
            com.firefly.core.plugin.api.Plugin wrapperPlugin = new AbstractPlugin(metadata) {
                @Override
                public Mono<Void> initialize() {
                    logger.info("Initializing wrapper plugin for: {}", pluginClass.getName());
                    try {
                        // Try to call initialize method if it exists
                        try {
                            java.lang.reflect.Method initMethod = pluginClass.getMethod("initialize");
                            Object result = initMethod.invoke(finalInstance);
                            if (result instanceof Mono) {
                                return (Mono<Void>) result;
                            }
                        } catch (NoSuchMethodException e) {
                            // Method doesn't exist, ignore
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        logger.error("Error initializing wrapper plugin", e);
                        return Mono.error(e);
                    }
                }

                @Override
                public Mono<Void> start() {
                    logger.info("Starting wrapper plugin for: {}", pluginClass.getName());
                    try {
                        // Try to call start method if it exists
                        try {
                            java.lang.reflect.Method startMethod = pluginClass.getMethod("start");
                            Object result = startMethod.invoke(finalInstance);
                            if (result instanceof Mono) {
                                return (Mono<Void>) result;
                            }
                        } catch (NoSuchMethodException e) {
                            // Method doesn't exist, ignore
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        logger.error("Error starting wrapper plugin", e);
                        return Mono.error(e);
                    }
                }

                @Override
                public Mono<Void> stop() {
                    logger.info("Stopping wrapper plugin for: {}", pluginClass.getName());
                    try {
                        // Try to call stop method if it exists
                        try {
                            java.lang.reflect.Method stopMethod = pluginClass.getMethod("stop");
                            Object result = stopMethod.invoke(finalInstance);
                            if (result instanceof Mono) {
                                return (Mono<Void>) result;
                            }
                        } catch (NoSuchMethodException e) {
                            // Method doesn't exist, ignore
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        logger.error("Error stopping wrapper plugin", e);
                        return Mono.error(e);
                    }
                }

                @Override
                public Mono<Void> uninstall() {
                    logger.info("Uninstalling wrapper plugin for: {}", pluginClass.getName());
                    try {
                        // Try to call uninstall method if it exists
                        try {
                            java.lang.reflect.Method uninstallMethod = pluginClass.getMethod("uninstall");
                            Object result = uninstallMethod.invoke(finalInstance);
                            if (result instanceof Mono) {
                                return (Mono<Void>) result;
                            }
                        } catch (NoSuchMethodException e) {
                            // Method doesn't exist, ignore
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        logger.error("Error uninstalling wrapper plugin", e);
                        return Mono.error(e);
                    }
                }
            };

            return Mono.just(wrapperPlugin);
        } catch (Exception e) {
            logger.error("Failed to create wrapper plugin for class: {}", pluginClass.getName(), e);
            return Mono.error(e);
        }
    }
}
