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


package com.firefly.core.plugin.config;

import com.firefly.core.plugin.DefaultPluginManager;
import com.firefly.core.plugin.api.ExtensionRegistry;
import com.firefly.core.plugin.api.PluginDebugger;
import com.firefly.core.plugin.api.PluginManager;
import com.firefly.core.plugin.api.PluginRegistry;
import com.firefly.core.plugin.dependency.PluginDependencyResolver;
import com.firefly.core.plugin.debug.DefaultPluginDebugger;
import com.firefly.core.plugin.event.DefaultPluginEventBus;
import com.firefly.core.plugin.event.KafkaPluginEventBus;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.event.PluginEventSerializer;
import com.firefly.core.plugin.extension.DefaultExtensionRegistry;
import com.firefly.core.plugin.health.PluginHealthMonitor;
import com.firefly.core.plugin.hotdeploy.PluginDirectoryWatcher;
import com.firefly.core.plugin.loader.DefaultPluginLoader;
import com.firefly.core.plugin.loader.PluginLoader;
import com.firefly.core.plugin.registry.DefaultPluginRegistry;
import com.firefly.core.plugin.security.PluginClassLoader;
import com.firefly.core.plugin.security.PluginResourceLimiter;
import com.firefly.core.plugin.security.PluginSecurityManager;
import com.firefly.core.plugin.security.PluginSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Auto-configuration for the plugin manager.
 * This class provides Spring beans for the plugin manager and its dependencies.
 */
@Configuration
@EnableConfigurationProperties(PluginManagerProperties.class)
public class PluginManagerAutoConfiguration {

    /**
     * Creates a new PluginManagerAutoConfiguration instance.
     */
    public PluginManagerAutoConfiguration() {
        // Default constructor
    }

    /**
     * Creates an ObjectMapper bean for plugin event serialization if one doesn't exist.
     *
     * @return the object mapper
     */
    @Bean
    @ConditionalOnMissingBean(name = "pluginEventObjectMapper")
    public ObjectMapper pluginEventObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * Creates a plugin event serializer bean if one doesn't exist.
     *
     * @param pluginEventObjectMapper the object mapper for plugin events
     * @param applicationContext the application context
     * @return the plugin event serializer
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginEventSerializer pluginEventSerializer(ObjectMapper pluginEventObjectMapper, ApplicationContext applicationContext) {
        return new PluginEventSerializer(pluginEventObjectMapper, applicationContext);
    }

    /**
     * Creates an in-memory plugin event bus bean if the event bus type is "in-memory".
     *
     * @return the in-memory plugin event bus
     */
    @Bean
    @ConditionalOnProperty(name = "firefly.plugin-manager.event-bus.type", havingValue = "in-memory", matchIfMissing = true)
    @ConditionalOnMissingBean(PluginEventBus.class)
    public PluginEventBus inMemoryPluginEventBus() {
        return new DefaultPluginEventBus();
    }

    /**
     * Creates a Kafka plugin event bus bean if the event bus type is "kafka".
     * This bean is only created if Kafka classes are available on the classpath and the event bus type
     * is explicitly set to "kafka" in the configuration.
     *
     * @param pluginEventObjectMapper the object mapper for plugin events
     * @param properties the plugin manager properties
     * @return the Kafka plugin event bus
     */
    @Bean
    @ConditionalOnProperty(name = "firefly.plugin-manager.event-bus.type", havingValue = "kafka")
    @ConditionalOnClass(name = {"reactor.kafka.sender.KafkaSender", "org.apache.kafka.clients.producer.KafkaProducer"})
    @ConditionalOnMissingBean(PluginEventBus.class)
    public PluginEventBus kafkaPluginEventBus(ObjectMapper pluginEventObjectMapper, PluginManagerProperties properties) {
        return new KafkaPluginEventBus(pluginEventObjectMapper, properties);
    }

    /**
     * Creates a plugin registry bean if one doesn't exist.
     *
     * @param eventBus the plugin event bus
     * @return the plugin registry
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginRegistry pluginRegistry(PluginEventBus eventBus) {
        return new DefaultPluginRegistry(eventBus);
    }

    /**
     * Creates an extension registry bean if one doesn't exist.
     *
     * @return the extension registry
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtensionRegistry extensionRegistry() {
        return new DefaultExtensionRegistry();
    }

    /**
     * Creates a plugin loader bean if one doesn't exist.
     *
     * @return the plugin loader
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginLoader pluginLoader(
            PluginManagerProperties properties,
            PluginSecurityManager securityManager,
            PluginResourceLimiter resourceLimiter,
            PluginSignatureVerifier signatureVerifier
    ) {
        return new DefaultPluginLoader(properties, securityManager, resourceLimiter, signatureVerifier);
    }

    /**
     * Creates a plugin security manager bean if one doesn't exist.
     *
     * @param properties the plugin manager properties
     * @return the plugin security manager
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginSecurityManager pluginSecurityManager(PluginManagerProperties properties) {
        return new PluginSecurityManager(properties.getSecurity().isEnforceSecurityChecks());
    }

    /**
     * Creates a plugin resource limiter bean if one doesn't exist.
     *
     * @param properties the plugin manager properties
     * @return the plugin resource limiter
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginResourceLimiter pluginResourceLimiter(PluginManagerProperties properties) {
        return new PluginResourceLimiter(properties.getResources().isEnforceResourceLimits());
    }

    /**
     * Creates a plugin signature verifier bean if one doesn't exist.
     *
     * @param properties the plugin manager properties
     * @return the plugin signature verifier
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginSignatureVerifier pluginSignatureVerifier(PluginManagerProperties properties) {
        return new PluginSignatureVerifier(properties.getSecurity().isRequireSignature());
    }

    /**
     * Creates a plugin dependency resolver bean if one doesn't exist.
     *
     * @return the plugin dependency resolver
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginDependencyResolver pluginDependencyResolver() {
        return new PluginDependencyResolver();
    }

    /**
     * Creates a plugin manager bean if one doesn't exist.
     *
     * @param pluginRegistry the plugin registry
     * @param extensionRegistry the extension registry
     * @param eventBus the plugin event bus
     * @param pluginLoader the plugin loader
     * @param dependencyResolver the plugin dependency resolver
     * @return the plugin manager
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginManager pluginManager(
            PluginRegistry pluginRegistry,
            ExtensionRegistry extensionRegistry,
            PluginEventBus eventBus,
            PluginLoader pluginLoader,
            PluginDependencyResolver dependencyResolver) {
        return new DefaultPluginManager(pluginRegistry, extensionRegistry, eventBus, pluginLoader, dependencyResolver);
    }

    /**
     * Creates a plugin directory watcher bean if one doesn't exist and hot deployment is enabled.
     *
     * @param pluginManager the plugin manager
     * @param properties the plugin manager properties
     * @return the plugin directory watcher
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "firefly.plugin-manager.hot-deployment", name = "enabled", havingValue = "true")
    public PluginDirectoryWatcher pluginDirectoryWatcher(
            PluginManager pluginManager,
            PluginManagerProperties properties) {
        return new PluginDirectoryWatcher(pluginManager, properties);
    }

    /**
     * Creates a plugin health monitor bean if one doesn't exist and health monitoring is enabled.
     *
     * @param pluginManager the plugin manager
     * @param eventBus the plugin event bus
     * @param properties the plugin manager properties
     * @return the plugin health monitor
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "firefly.plugin-manager.health", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PluginHealthMonitor pluginHealthMonitor(
            PluginManager pluginManager,
            PluginEventBus eventBus,
            PluginManagerProperties properties) {
        return new PluginHealthMonitor(pluginManager, eventBus, properties);
    }

    /**
     * Creates a plugin debugger bean if one doesn't exist and debugging is enabled.
     *
     * @param pluginManager the plugin manager
     * @param eventBus the plugin event bus
     * @param properties the plugin manager properties
     * @return the plugin debugger
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "firefly.plugin-manager.debugger", name = "enabled", havingValue = "true")
    public PluginDebugger pluginDebugger(
            PluginManager pluginManager,
            PluginEventBus eventBus,
            PluginManagerProperties properties) {
        return new DefaultPluginDebugger(pluginManager, eventBus, properties);
    }
}
