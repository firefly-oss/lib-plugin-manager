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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginManagerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PluginManagerAutoConfiguration.class));

    @Test
    void testDefaultConfiguration() {
        contextRunner.run(context -> {
            // Verify all beans are created with default configuration
            assertThat(context).hasSingleBean(PluginEventBus.class);
            assertThat(context).hasSingleBean(PluginRegistry.class);
            assertThat(context).hasSingleBean(ExtensionRegistry.class);
            assertThat(context).hasSingleBean(PluginLoader.class);
            assertThat(context).hasSingleBean(PluginManager.class);
            
            // Verify default implementations
            assertThat(context).getBean(PluginEventBus.class).isInstanceOf(DefaultPluginEventBus.class);
            assertThat(context).getBean(PluginRegistry.class).isInstanceOf(DefaultPluginRegistry.class);
            assertThat(context).getBean(ExtensionRegistry.class).isInstanceOf(DefaultExtensionRegistry.class);
            assertThat(context).getBean(PluginLoader.class).isInstanceOf(DefaultPluginLoader.class);
            assertThat(context).getBean(PluginManager.class).isInstanceOf(DefaultPluginManager.class);
        });
    }

    @Test
    void testKafkaEventBusConfiguration() {
        contextRunner
                .withPropertyValues("firefly.plugin-manager.event-bus.type=kafka")
                .run(context -> {
                    // Verify Kafka event bus is created
                    assertThat(context).hasSingleBean(PluginEventBus.class);
                    assertThat(context).getBean(PluginEventBus.class).isInstanceOf(KafkaPluginEventBus.class);
                });
    }

    @Test
    void testCustomBeans() {
        contextRunner
                .withUserConfiguration(CustomConfiguration.class)
                .run(context -> {
                    // Verify custom beans are used
                    assertThat(context).hasSingleBean(PluginEventBus.class);
                    assertThat(context).hasSingleBean(PluginRegistry.class);
                    assertThat(context).hasSingleBean(ExtensionRegistry.class);
                    assertThat(context).hasSingleBean(PluginLoader.class);
                    assertThat(context).hasSingleBean(PluginManager.class);
                    
                    // Verify custom implementations
                    assertThat(context).getBean(PluginEventBus.class).isInstanceOf(CustomPluginEventBus.class);
                    assertThat(context).getBean(PluginRegistry.class).isInstanceOf(CustomPluginRegistry.class);
                    assertThat(context).getBean(ExtensionRegistry.class).isInstanceOf(CustomExtensionRegistry.class);
                    assertThat(context).getBean(PluginLoader.class).isInstanceOf(CustomPluginLoader.class);
                    assertThat(context).getBean(PluginManager.class).isInstanceOf(CustomPluginManager.class);
                });
    }

    @Test
    void testObjectMapperConfiguration() {
        contextRunner.run(context -> {
            // Verify ObjectMapper is created
            assertThat(context).hasSingleBean(ObjectMapper.class);
            
            // Verify PluginEventSerializer is created
            assertThat(context).hasSingleBean(PluginEventSerializer.class);
        });
    }

    // Custom implementations for testing
    static class CustomPluginEventBus implements PluginEventBus {
        @Override
        public reactor.core.publisher.Mono<Void> publish(com.catalis.core.plugin.event.PluginEvent event) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> publish(String topic, com.catalis.core.plugin.event.PluginEvent event) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public <T extends com.catalis.core.plugin.event.PluginEvent> reactor.core.publisher.Flux<T> subscribe(Class<T> eventType) {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.event.PluginEvent> subscribeToPlugin(String pluginId) {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public <T extends com.catalis.core.plugin.event.PluginEvent> reactor.core.publisher.Flux<T> subscribeToPlugin(String pluginId, Class<T> eventType) {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.event.PluginEvent> subscribeTopic(String topic) {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public <T extends com.catalis.core.plugin.event.PluginEvent> reactor.core.publisher.Flux<T> subscribeTopic(String topic, Class<T> eventType) {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public String getTransportType() {
            return "custom";
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> initialize() {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> shutdown() {
            return reactor.core.publisher.Mono.empty();
        }
    }
    
    static class CustomPluginRegistry implements PluginRegistry {
        @Override
        public reactor.core.publisher.Mono<Void> registerPlugin(com.catalis.core.plugin.api.Plugin plugin) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> unregisterPlugin(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<com.catalis.core.plugin.api.Plugin> getPlugin(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<com.catalis.core.plugin.model.PluginDescriptor> getPluginDescriptor(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.api.Plugin> getAllPlugins() {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.model.PluginDescriptor> getAllPluginDescriptors() {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.api.Plugin> getPluginsByState(com.catalis.core.plugin.model.PluginState state) {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> startPlugin(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> stopPlugin(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> updatePluginConfiguration(String pluginId, java.util.Map<String, Object> configuration) {
            return reactor.core.publisher.Mono.empty();
        }
    }
    
    static class CustomExtensionRegistry implements ExtensionRegistry {
        @Override
        public <T> reactor.core.publisher.Mono<Void> registerExtensionPoint(String extensionPointId, Class<T> extensionPointClass) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public <T> reactor.core.publisher.Mono<Void> registerExtension(String extensionPointId, T extension, int priority) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public <T> reactor.core.publisher.Mono<Void> unregisterExtension(String extensionPointId, T extension) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public <T> reactor.core.publisher.Flux<T> getExtensions(String extensionPointId) {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public <T> reactor.core.publisher.Mono<T> getHighestPriorityExtension(String extensionPointId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Flux<String> getExtensionPoints() {
            return reactor.core.publisher.Flux.empty();
        }
    }
    
    static class CustomPluginLoader implements PluginLoader {
        @Override
        public reactor.core.publisher.Mono<com.catalis.core.plugin.api.Plugin> loadPlugin(java.nio.file.Path pluginPath) {
            return reactor.core.publisher.Mono.empty();
        }
    }
    
    static class CustomPluginManager implements PluginManager {
        @Override
        public PluginRegistry getPluginRegistry() {
            return null;
        }
        
        @Override
        public ExtensionRegistry getExtensionRegistry() {
            return null;
        }
        
        @Override
        public PluginEventBus getEventBus() {
            return null;
        }
        
        @Override
        public reactor.core.publisher.Mono<com.catalis.core.plugin.model.PluginDescriptor> installPlugin(java.nio.file.Path pluginPath) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> uninstallPlugin(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> startPlugin(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> stopPlugin(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> restartPlugin(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> updatePluginConfiguration(String pluginId, java.util.Map<String, Object> configuration) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.model.PluginDescriptor> getAllPlugins() {
            return reactor.core.publisher.Flux.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<com.catalis.core.plugin.model.PluginDescriptor> getPlugin(String pluginId) {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> initialize() {
            return reactor.core.publisher.Mono.empty();
        }
        
        @Override
        public reactor.core.publisher.Mono<Void> shutdown() {
            return reactor.core.publisher.Mono.empty();
        }
    }

    @Configuration
    static class CustomConfiguration {
        @Bean
        public PluginEventBus pluginEventBus() {
            return new CustomPluginEventBus();
        }
        
        @Bean
        public PluginRegistry pluginRegistry() {
            return new CustomPluginRegistry();
        }
        
        @Bean
        public ExtensionRegistry extensionRegistry() {
            return new CustomExtensionRegistry();
        }
        
        @Bean
        public PluginLoader pluginLoader() {
            return new CustomPluginLoader();
        }
        
        @Bean
        public PluginManager pluginManager() {
            return new CustomPluginManager();
        }
    }
}
