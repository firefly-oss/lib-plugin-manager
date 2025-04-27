package com.catalis.core.plugin.config;

import com.catalis.core.plugin.DefaultPluginManager;
import com.catalis.core.plugin.api.ExtensionRegistry;
import com.catalis.core.plugin.api.PluginManager;
import com.catalis.core.plugin.api.PluginRegistry;
import com.catalis.core.plugin.event.DefaultPluginEventBus;
import com.catalis.core.plugin.event.KafkaPluginEventBus;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.event.PluginEventSerializer;
import com.catalis.core.plugin.extension.DefaultExtensionRegistry;
import com.catalis.core.plugin.loader.DefaultPluginLoader;
import com.catalis.core.plugin.loader.PluginLoader;
import com.catalis.core.plugin.registry.DefaultPluginRegistry;
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
    public PluginLoader pluginLoader() {
        return new DefaultPluginLoader();
    }

    /**
     * Creates a plugin manager bean if one doesn't exist.
     *
     * @param pluginRegistry the plugin registry
     * @param extensionRegistry the extension registry
     * @param eventBus the plugin event bus
     * @param pluginLoader the plugin loader
     * @return the plugin manager
     */
    @Bean
    @ConditionalOnMissingBean
    public PluginManager pluginManager(
            PluginRegistry pluginRegistry,
            ExtensionRegistry extensionRegistry,
            PluginEventBus eventBus,
            PluginLoader pluginLoader) {
        return new DefaultPluginManager(pluginRegistry, extensionRegistry, eventBus, pluginLoader);
    }
}
