package com.catalis.core.plugin.health;

import com.catalis.core.plugin.api.Plugin;
import com.catalis.core.plugin.api.PluginHealthIndicator;
import com.catalis.core.plugin.api.PluginManager;
import com.catalis.core.plugin.api.PluginRegistry;
import com.catalis.core.plugin.config.PluginManagerProperties;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.model.PluginDescriptor;
import com.catalis.core.plugin.model.PluginHealth;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.model.PluginState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PluginHealthMonitorTest {

    @Mock
    private PluginManager pluginManager;

    @Mock
    private PluginRegistry pluginRegistry;

    @Mock
    private PluginEventBus eventBus;

    private PluginManagerProperties properties;
    private PluginHealthMonitor healthMonitor;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Set up properties
        properties = new PluginManagerProperties();
        properties.getHealth().setMonitoringIntervalMs(100); // Use a short interval for testing

        // Set up mock behavior
        when(pluginManager.getPluginRegistry()).thenReturn(pluginRegistry);

        // Create the health monitor
        healthMonitor = new PluginHealthMonitor(pluginManager, eventBus, properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Stop the health monitor
        healthMonitor.stop();

        // Close mocks
        mocks.close();
    }

    @Test
    void testCheckAllPluginHealth() {
        // Create test plugins
        PluginDescriptor descriptor1 = PluginDescriptor.builder()
                .id("plugin1")
                .name("Plugin 1")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        PluginDescriptor descriptor2 = PluginDescriptor.builder()
                .id("plugin2")
                .name("Plugin 2")
                .version("1.0.0")
                .state(PluginState.STOPPED)
                .build();

        // Set up mock behavior
        when(pluginManager.getAllPlugins()).thenReturn(Flux.just(descriptor1, descriptor2));

        PluginMetadata metadata1 = PluginMetadata.builder()
                .id("plugin1")
                .name("Plugin 1")
                .version("1.0.0")
                .build();

        PluginMetadata metadata2 = PluginMetadata.builder()
                .id("plugin2")
                .name("Plugin 2")
                .version("1.0.0")
                .build();

        Plugin plugin1 = mock(Plugin.class);
        Plugin plugin2 = mock(Plugin.class);

        when(plugin1.getMetadata()).thenReturn(metadata1);
        when(plugin2.getMetadata()).thenReturn(metadata2);

        when(pluginRegistry.getPlugin("plugin1")).thenReturn(Mono.just(plugin1));
        when(pluginRegistry.getPlugin("plugin2")).thenReturn(Mono.just(plugin2));

        when(pluginRegistry.getPluginDescriptor("plugin1")).thenReturn(Mono.just(descriptor1));
        when(pluginRegistry.getPluginDescriptor("plugin2")).thenReturn(Mono.just(descriptor2));

        // Test the health check
        StepVerifier.create(healthMonitor.checkAllPluginHealth().collectList())
                .assertNext(healthList -> {
                    assertEquals(2, healthList.size());

                    // Find health for plugin1
                    PluginHealth health1 = healthList.stream()
                            .filter(h -> h.getPluginId().equals("plugin1"))
                            .findFirst()
                            .orElse(null);
                    assertNotNull(health1);
                    assertEquals(PluginHealth.Status.UP, health1.getStatus());

                    // Find health for plugin2
                    PluginHealth health2 = healthList.stream()
                            .filter(h -> h.getPluginId().equals("plugin2"))
                            .findFirst()
                            .orElse(null);
                    assertNotNull(health2);
                    // Plugin2 is in STOPPED state, so it should be DOWN
                    assertEquals(PluginHealth.Status.DOWN, health2.getStatus());
                    assertEquals("Plugin is stopped", health2.getMessage());
                })
                .verifyComplete();
    }

    @Test
    void testCheckPluginHealthWithHealthIndicator() {
        // Create a test plugin that implements PluginHealthIndicator
        String pluginId = "plugin-with-indicator";
        PluginWithHealthIndicator plugin = new PluginWithHealthIndicator(pluginId);

        // Set up mock behavior
        when(pluginRegistry.getPlugin(pluginId)).thenReturn(Mono.just(plugin));

        // Test the health check
        StepVerifier.create(healthMonitor.checkPluginHealth(pluginId))
                .assertNext(health -> {
                    assertEquals(pluginId, health.getPluginId());
                    assertEquals(PluginHealth.Status.UP, health.getStatus());
                    assertEquals("Custom health check", health.getMessage());
                    assertEquals("custom-value", health.getDetails().get("custom-key"));
                })
                .verifyComplete();
    }

    @Test
    void testCheckPluginHealthWithDefaultCheck() {
        // Create a test plugin that doesn't implement PluginHealthIndicator
        String pluginId = "regular-plugin";
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = PluginMetadata.builder()
                .id(pluginId)
                .name("Regular Plugin")
                .version("1.0.0")
                .build();
        when(plugin.getMetadata()).thenReturn(metadata);

        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Regular Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        // Set up mock behavior
        when(pluginRegistry.getPlugin(pluginId)).thenReturn(Mono.just(plugin));
        when(pluginRegistry.getPluginDescriptor(pluginId)).thenReturn(Mono.just(descriptor));

        // Test the health check
        StepVerifier.create(healthMonitor.checkPluginHealth(pluginId))
                .assertNext(health -> {
                    assertEquals(pluginId, health.getPluginId());
                    assertEquals(PluginHealth.Status.UP, health.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void testAutoRecovery() throws Exception {
        // Enable auto recovery
        properties.getHealth().setAutoRecoveryEnabled(true);
        properties.getHealth().setMaxRecoveryAttempts(3);
        properties.getHealth().setMonitoringIntervalMs(1000); // Longer interval to avoid multiple checks

        // Create a test plugin
        String pluginId = "failing-plugin";
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = PluginMetadata.builder()
                .id(pluginId)
                .name("Failing Plugin")
                .version("1.0.0")
                .build();
        when(plugin.getMetadata()).thenReturn(metadata);

        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Failing Plugin")
                .version("1.0.0")
                .state(PluginState.FAILED)
                .build();

        // Set up mock behavior
        when(pluginManager.getAllPlugins()).thenReturn(Flux.just(descriptor));
        when(pluginRegistry.getPlugin(pluginId)).thenReturn(Mono.just(plugin));
        when(pluginRegistry.getPluginDescriptor(pluginId)).thenReturn(Mono.just(descriptor));
        when(pluginManager.restartPlugin(pluginId)).thenReturn(Mono.empty());

        // Create a new health monitor with the updated properties
        PluginHealthMonitor testMonitor = new PluginHealthMonitor(pluginManager, eventBus, properties);

        try {
            // Start the health monitor
            testMonitor.start();

            // Wait for the health check to run
            Thread.sleep(500);

            // Verify that the plugin was restarted exactly once
            verify(pluginManager, timeout(1000).times(1)).restartPlugin(eq(pluginId));

            // Verify that a health event was published
            ArgumentCaptor<PluginHealthEvent> eventCaptor = ArgumentCaptor.forClass(PluginHealthEvent.class);
            verify(eventBus, timeout(1000)).publish(eventCaptor.capture());

            PluginHealthEvent event = eventCaptor.getValue();
            assertEquals(pluginId, event.getPluginId());
            assertEquals(PluginHealth.Status.DOWN, event.getHealth().getStatus());
        } finally {
            // Stop the test monitor
            testMonitor.stop();
        }
    }

    @Test
    void testMaxRecoveryAttempts() throws Exception {
        // Enable auto recovery with a low max attempts
        properties.getHealth().setAutoRecoveryEnabled(true);
        properties.getHealth().setMaxRecoveryAttempts(1);
        properties.getHealth().setMonitoringIntervalMs(1000); // Longer interval to avoid multiple checks

        // Create a test plugin
        String pluginId = "failing-plugin";
        Plugin plugin = mock(Plugin.class);
        PluginMetadata metadata = PluginMetadata.builder()
                .id(pluginId)
                .name("Failing Plugin")
                .version("1.0.0")
                .build();
        when(plugin.getMetadata()).thenReturn(metadata);

        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Failing Plugin")
                .version("1.0.0")
                .state(PluginState.FAILED)
                .build();

        // Set up mock behavior
        when(pluginManager.getAllPlugins()).thenReturn(Flux.just(descriptor));
        when(pluginRegistry.getPlugin(pluginId)).thenReturn(Mono.just(plugin));
        when(pluginRegistry.getPluginDescriptor(pluginId)).thenReturn(Mono.just(descriptor));
        when(pluginManager.restartPlugin(pluginId)).thenReturn(Mono.empty());

        // Create a new PluginManager mock for this test to avoid interference
        PluginManager testPluginManager = mock(PluginManager.class);
        when(testPluginManager.getAllPlugins()).thenReturn(Flux.just(descriptor));
        when(testPluginManager.getPluginRegistry()).thenReturn(pluginRegistry);

        // Create a health monitor with a pre-populated health cache
        PluginHealthMonitor monitor = new PluginHealthMonitor(testPluginManager, eventBus, properties);

        try {
            // Manually add a health entry with recovery attempts
            Map<String, Object> details = new HashMap<>();
            details.put("recoveryAttempts", 1); // Already at the max
            PluginHealth health = new PluginHealth(pluginId, PluginHealth.Status.DOWN, "Plugin failed", details);

            // Use reflection to access the private healthCache field
            java.lang.reflect.Field field = PluginHealthMonitor.class.getDeclaredField("healthCache");
            field.setAccessible(true);
            Map<String, PluginHealth> healthCache = (Map<String, PluginHealth>) field.get(monitor);
            healthCache.put(pluginId, health);

            // Start the health monitor
            monitor.start();

            // Wait for the health check to run
            Thread.sleep(500);

            // Verify that the plugin was NOT restarted (because we've reached max attempts)
            verify(testPluginManager, never()).restartPlugin(eq(pluginId));
        } finally {
            // Stop the monitor
            monitor.stop();
        }
    }

    /**
     * A test plugin that implements PluginHealthIndicator.
     */
    private static class PluginWithHealthIndicator implements Plugin, PluginHealthIndicator {

        private final PluginMetadata metadata;

        public PluginWithHealthIndicator(String pluginId) {
            this.metadata = PluginMetadata.builder()
                    .id(pluginId)
                    .name("Plugin With Health Indicator")
                    .version("1.0.0")
                    .build();
        }

        @Override
        public PluginMetadata getMetadata() {
            return metadata;
        }

        @Override
        public Mono<Void> initialize() {
            return Mono.empty();
        }

        @Override
        public Mono<Void> start() {
            return Mono.empty();
        }

        @Override
        public Mono<Void> stop() {
            return Mono.empty();
        }

        @Override
        public Mono<Void> uninstall() {
            return Mono.empty();
        }

        @Override
        public Mono<PluginHealth> health() {
            Map<String, Object> details = new HashMap<>();
            details.put("custom-key", "custom-value");
            return Mono.just(new PluginHealth(metadata.id(), PluginHealth.Status.UP, "Custom health check", details));
        }
    }
}
