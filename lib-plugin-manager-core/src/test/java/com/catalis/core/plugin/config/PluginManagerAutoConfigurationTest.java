package com.catalis.core.plugin.config;

import com.catalis.core.plugin.DefaultPluginManager;
import com.catalis.core.plugin.api.ExtensionRegistry;
import com.catalis.core.plugin.api.PluginDebugger;
import com.catalis.core.plugin.api.PluginManager;
import com.catalis.core.plugin.api.PluginRegistry;
import com.catalis.core.plugin.dependency.PluginDependencyResolver;
import com.catalis.core.plugin.event.DefaultPluginEventBus;
import com.catalis.core.plugin.event.KafkaPluginEventBus;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.event.PluginEventSerializer;
import com.catalis.core.plugin.extension.DefaultExtensionRegistry;
import com.catalis.core.plugin.health.PluginHealthMonitor;
import com.catalis.core.plugin.hotdeploy.PluginDirectoryWatcher;
import com.catalis.core.plugin.loader.DefaultPluginLoader;
import com.catalis.core.plugin.loader.PluginLoader;
import com.catalis.core.plugin.model.PluginDescriptor;
import com.catalis.core.plugin.registry.DefaultPluginRegistry;
import com.catalis.core.plugin.security.PluginResourceLimiter;
import com.catalis.core.plugin.security.PluginSecurityManager;
import com.catalis.core.plugin.security.PluginSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
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
            assertThat(context).hasSingleBean(PluginDependencyResolver.class);

            // Verify default implementations
            assertThat(context).getBean(PluginEventBus.class).isInstanceOf(DefaultPluginEventBus.class);
            assertThat(context).getBean(PluginRegistry.class).isInstanceOf(DefaultPluginRegistry.class);
            assertThat(context).getBean(ExtensionRegistry.class).isInstanceOf(DefaultExtensionRegistry.class);
            assertThat(context).getBean(PluginLoader.class).isInstanceOf(DefaultPluginLoader.class);
            assertThat(context).getBean(PluginManager.class).isInstanceOf(DefaultPluginManager.class);
            assertThat(context).getBean(PluginDependencyResolver.class).isInstanceOf(PluginDependencyResolver.class);
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
                    assertThat(context).hasSingleBean(PluginSecurityManager.class);
                    assertThat(context).hasSingleBean(PluginResourceLimiter.class);
                    assertThat(context).hasSingleBean(PluginSignatureVerifier.class);
                    assertThat(context).hasSingleBean(PluginDirectoryWatcher.class);
                    assertThat(context).hasSingleBean(PluginHealthMonitor.class);
                    assertThat(context).hasSingleBean(PluginDebugger.class);
                    assertThat(context).hasSingleBean(PluginDependencyResolver.class);

                    // Verify custom implementations
                    assertThat(context).getBean(PluginEventBus.class).isInstanceOf(CustomPluginEventBus.class);
                    assertThat(context).getBean(PluginRegistry.class).isInstanceOf(CustomPluginRegistry.class);
                    assertThat(context).getBean(ExtensionRegistry.class).isInstanceOf(CustomExtensionRegistry.class);
                    assertThat(context).getBean(PluginLoader.class).isInstanceOf(CustomPluginLoader.class);
                    assertThat(context).getBean(PluginManager.class).isInstanceOf(CustomPluginManager.class);
                    assertThat(context).getBean(PluginSecurityManager.class).isInstanceOf(CustomPluginSecurityManager.class);
                    assertThat(context).getBean(PluginResourceLimiter.class).isInstanceOf(CustomPluginResourceLimiter.class);
                    assertThat(context).getBean(PluginSignatureVerifier.class).isInstanceOf(CustomPluginSignatureVerifier.class);
                    assertThat(context).getBean(PluginDirectoryWatcher.class).isInstanceOf(CustomPluginDirectoryWatcher.class);
                    assertThat(context).getBean(PluginHealthMonitor.class).isInstanceOf(CustomPluginHealthMonitor.class);
                    assertThat(context).getBean(PluginDebugger.class).isInstanceOf(CustomPluginDebugger.class);
                    assertThat(context).getBean(PluginDependencyResolver.class).isInstanceOf(CustomPluginDependencyResolver.class);
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

    @Test
    void testSecurityComponentsConfiguration() {
        contextRunner.run(context -> {
            // Verify security components are created
            assertThat(context).hasSingleBean(PluginSecurityManager.class);
            assertThat(context).hasSingleBean(PluginResourceLimiter.class);
            assertThat(context).hasSingleBean(PluginSignatureVerifier.class);
        });
    }

    @Test
    void testSecurityComponentsWithCustomConfiguration() {
        contextRunner
                .withPropertyValues(
                        "firefly.plugin-manager.security.enforce-security-checks=true",
                        "firefly.plugin-manager.security.require-signature=true",
                        "firefly.plugin-manager.resources.enforce-resource-limits=true")
                .run(context -> {
                    // Verify security components are created with custom configuration
                    PluginSecurityManager securityManager = context.getBean(PluginSecurityManager.class);
                    PluginResourceLimiter resourceLimiter = context.getBean(PluginResourceLimiter.class);
                    PluginSignatureVerifier signatureVerifier = context.getBean(PluginSignatureVerifier.class);

                    // We can't directly test the configuration values because they're private
                    // and there are no getters, but we can verify the beans are created
                    assertThat(securityManager).isNotNull();
                    assertThat(resourceLimiter).isNotNull();
                    assertThat(signatureVerifier).isNotNull();
                });
    }

    @Test
    void testHotDeploymentConfiguration() {
        contextRunner
                .withPropertyValues(
                        "firefly.plugin-manager.hot-deployment.enabled=true",
                        "firefly.plugin-manager.hot-deployment.polling-interval-ms=1000",
                        "firefly.plugin-manager.hot-deployment.auto-reload=true")
                .run(context -> {
                    // Verify hot deployment component is created
                    assertThat(context).hasSingleBean(PluginDirectoryWatcher.class);

                    // Get the bean to verify it's not null
                    PluginDirectoryWatcher watcher = context.getBean(PluginDirectoryWatcher.class);
                    assertThat(watcher).isNotNull();
                });
    }

    @Test
    void testHotDeploymentDisabled() {
        contextRunner
                .withPropertyValues(
                        "firefly.plugin-manager.hot-deployment.enabled=false")
                .run(context -> {
                    // Verify hot deployment component is not created when disabled
                    assertThat(context).doesNotHaveBean(PluginDirectoryWatcher.class);
                });
    }

    @Test
    void testHealthMonitorConfiguration() {
        contextRunner
                .withPropertyValues(
                        "firefly.plugin-manager.health.enabled=true",
                        "firefly.plugin-manager.health.monitoring-interval-ms=30000",
                        "firefly.plugin-manager.health.auto-recovery-enabled=true")
                .run(context -> {
                    // Verify health monitor component is created
                    assertThat(context).hasSingleBean(PluginHealthMonitor.class);

                    // Get the bean to verify it's not null
                    PluginHealthMonitor healthMonitor = context.getBean(PluginHealthMonitor.class);
                    assertThat(healthMonitor).isNotNull();
                });
    }

    @Test
    void testHealthMonitorDisabled() {
        contextRunner
                .withPropertyValues(
                        "firefly.plugin-manager.health.enabled=false")
                .run(context -> {
                    // Verify health monitor component is not created when disabled
                    assertThat(context).doesNotHaveBean(PluginHealthMonitor.class);
                });
    }

    @Test
    void testDebuggerConfiguration() {
        contextRunner
                .withPropertyValues(
                        "firefly.plugin-manager.debugger.enabled=true",
                        "firefly.plugin-manager.debugger.port=9000",
                        "firefly.plugin-manager.debugger.remote-debugging-enabled=true")
                .run(context -> {
                    // Verify debugger component is created
                    assertThat(context).hasSingleBean(PluginDebugger.class);

                    // Get the bean to verify it's not null
                    PluginDebugger debugger = context.getBean(PluginDebugger.class);
                    assertThat(debugger).isNotNull();
                });
    }

    @Test
    void testDebuggerDisabled() {
        contextRunner
                .withPropertyValues(
                        "firefly.plugin-manager.debugger.enabled=false")
                .run(context -> {
                    // Verify debugger component is not created when disabled
                    assertThat(context).doesNotHaveBean(PluginDebugger.class);
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

        @Override
        public reactor.core.publisher.Mono<com.catalis.core.plugin.api.Plugin> loadPluginFromGit(java.net.URI repositoryUri, String branch) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<com.catalis.core.plugin.api.Plugin> loadPluginFromGit(java.net.URI repositoryUri) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.api.Plugin> loadPluginsFromClasspath(String basePackage) {
            return reactor.core.publisher.Flux.empty();
        }

        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.api.Plugin> loadPluginsFromClasspath() {
            return reactor.core.publisher.Flux.empty();
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

        @Override
        public reactor.core.publisher.Mono<com.catalis.core.plugin.model.PluginDescriptor> installPluginFromGit(java.net.URI repositoryUri, String branch) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Mono<com.catalis.core.plugin.model.PluginDescriptor> installPluginFromGit(java.net.URI repositoryUri) {
            return reactor.core.publisher.Mono.empty();
        }

        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.model.PluginDescriptor> installPluginsFromClasspath(String basePackage) {
            return reactor.core.publisher.Flux.empty();
        }

        @Override
        public reactor.core.publisher.Flux<com.catalis.core.plugin.model.PluginDescriptor> installPluginsFromClasspath() {
            return reactor.core.publisher.Flux.empty();
        }
    }

    static class CustomPluginSecurityManager extends PluginSecurityManager {
        public CustomPluginSecurityManager() {
            super(false);
        }

        @Override
        public void checkPermission(java.security.Permission perm) {
            // Do nothing for testing
        }
    }

    static class CustomPluginResourceLimiter extends PluginResourceLimiter {
        public CustomPluginResourceLimiter() {
            super(false);
        }
    }

    static class CustomPluginSignatureVerifier extends PluginSignatureVerifier {
        public CustomPluginSignatureVerifier() {
            super(false);
        }
    }

    static class CustomPluginDirectoryWatcher extends PluginDirectoryWatcher {
        public CustomPluginDirectoryWatcher(PluginManager pluginManager, PluginManagerProperties properties) {
            super(pluginManager, properties);
        }
    }

    static class CustomPluginHealthMonitor extends PluginHealthMonitor {
        public CustomPluginHealthMonitor(PluginManager pluginManager, PluginEventBus eventBus, PluginManagerProperties properties) {
            super(pluginManager, eventBus, properties);
        }
    }

    static class CustomPluginDependencyResolver extends PluginDependencyResolver {
        // Custom implementation for testing
    }

    static class CustomPluginDebugger implements PluginDebugger {
        @Override
        public Mono<String> startDebugSession(String pluginId) {
            return Mono.just("test-session");
        }

        @Override
        public Mono<Void> stopDebugSession(String sessionId) {
            return Mono.empty();
        }

        @Override
        public Flux<Map.Entry<String, String>> getActiveSessions() {
            return Flux.empty();
        }

        @Override
        public Mono<String> setBreakpoint(String sessionId, String className, int lineNumber) {
            return Mono.just("test-breakpoint");
        }

        @Override
        public Mono<Void> removeBreakpoint(String sessionId, String breakpointId) {
            return Mono.empty();
        }

        @Override
        public Flux<Map<String, Object>> getBreakpoints(String sessionId) {
            return Flux.empty();
        }

        @Override
        public Mono<Void> continueExecution(String sessionId) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> stepOver(String sessionId) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> stepInto(String sessionId) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> stepOut(String sessionId) {
            return Mono.empty();
        }

        @Override
        public Mono<Object> getVariableValue(String sessionId, String variableName) {
            return Mono.just("test-value");
        }

        @Override
        public Flux<Map.Entry<String, Object>> getLocalVariables(String sessionId) {
            return Flux.empty();
        }

        @Override
        public Mono<Object> evaluateExpression(String sessionId, String expression) {
            return Mono.just("test-result");
        }

        @Override
        public Flux<Map<String, Object>> getStackTrace(String sessionId) {
            return Flux.empty();
        }

        @Override
        public Mono<PluginDescriptor> getPluginInfo(String sessionId) {
            return Mono.empty();
        }

        @Override
        public Mono<Boolean> isDebuggingEnabled(String pluginId) {
            return Mono.just(true);
        }

        @Override
        public Mono<Void> setDebuggingEnabled(String pluginId, boolean enabled) {
            return Mono.empty();
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

        @Bean
        public PluginSecurityManager pluginSecurityManager() {
            return new CustomPluginSecurityManager();
        }

        @Bean
        public PluginResourceLimiter pluginResourceLimiter() {
            return new CustomPluginResourceLimiter();
        }

        @Bean
        public PluginSignatureVerifier pluginSignatureVerifier() {
            return new CustomPluginSignatureVerifier();
        }

        @Bean
        public PluginDirectoryWatcher pluginDirectoryWatcher(PluginManager pluginManager, PluginManagerProperties properties) {
            return new CustomPluginDirectoryWatcher(pluginManager, properties);
        }

        @Bean
        public PluginHealthMonitor pluginHealthMonitor(PluginManager pluginManager, PluginEventBus eventBus, PluginManagerProperties properties) {
            return new CustomPluginHealthMonitor(pluginManager, eventBus, properties);
        }

        @Bean
        public PluginDebugger pluginDebugger() {
            return new CustomPluginDebugger();
        }

        @Bean
        public PluginDependencyResolver pluginDependencyResolver() {
            return new CustomPluginDependencyResolver();
        }
    }
}
