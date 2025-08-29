package com.firefly.core.plugin.registry;

import com.firefly.core.plugin.api.Plugin;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.event.PluginLifecycleEvent;
import com.firefly.core.plugin.model.PluginDescriptor;
import com.firefly.core.plugin.model.PluginMetadata;
import com.firefly.core.plugin.model.PluginState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class DefaultPluginRegistryTest {

    @Mock
    private PluginEventBus eventBus;

    @Mock
    private Plugin plugin;

    private DefaultPluginRegistry registry;
    private PluginMetadata metadata;

    @BeforeEach
    void setUp() {
        registry = new DefaultPluginRegistry(eventBus);

        metadata = PluginMetadata.builder()
                .id("test-plugin")
                .name("Test Plugin")
                .version("1.0.0")
                .description("Test plugin for unit tests")
                .author("Test Author")
                .dependencies(Set.of())
                .installTime(Instant.now())
                .build();

        when(plugin.getMetadata()).thenReturn(metadata);
        when(plugin.initialize()).thenReturn(Mono.empty());
        when(plugin.start()).thenReturn(Mono.empty());
        when(plugin.stop()).thenReturn(Mono.empty());
        when(eventBus.publish(any())).thenReturn(Mono.empty());
    }

    @Test
    void testRegisterPlugin() {
        // Register the plugin
        StepVerifier.create(registry.registerPlugin(plugin))
                .verifyComplete();

        // Verify the plugin is registered
        StepVerifier.create(registry.getPlugin("test-plugin"))
                .expectNext(plugin)
                .verifyComplete();

        // Verify the plugin descriptor
        StepVerifier.create(registry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals("test-plugin", descriptor.metadata().id());
                    assertEquals(PluginState.INITIALIZED, descriptor.state());
                })
                .verifyComplete();

        // Verify lifecycle event was published
        ArgumentCaptor<PluginLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(PluginLifecycleEvent.class);
        verify(eventBus).publish(eventCaptor.capture());

        PluginLifecycleEvent event = eventCaptor.getValue();
        assertEquals("test-plugin", event.getPluginId());
        assertEquals(PluginState.INSTALLED, event.getPreviousState());
        assertEquals(PluginState.INITIALIZED, event.getNewState());
    }

    @Test
    void testStartPlugin() {
        // Register the plugin
        StepVerifier.create(registry.registerPlugin(plugin))
                .verifyComplete();

        // Start the plugin
        StepVerifier.create(registry.startPlugin("test-plugin"))
                .verifyComplete();

        // Verify the plugin state
        StepVerifier.create(registry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals(PluginState.STARTED, descriptor.state());
                })
                .verifyComplete();

        // Verify start was called
        verify(plugin).start();

        // Verify lifecycle events were published (both for register and start)
        verify(eventBus, times(2)).publish(any(PluginLifecycleEvent.class));
    }

    @Test
    void testStopPlugin() {
        // Register the plugin
        StepVerifier.create(registry.registerPlugin(plugin))
                .verifyComplete();

        // Verify the plugin is registered and initialized
        StepVerifier.create(registry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals(PluginState.INITIALIZED, descriptor.state());
                })
                .verifyComplete();

        // Start the plugin
        StepVerifier.create(registry.startPlugin("test-plugin"))
                .verifyComplete();

        // Verify the plugin is started
        StepVerifier.create(registry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals(PluginState.STARTED, descriptor.state());
                })
                .verifyComplete();

        // Stop the plugin
        StepVerifier.create(registry.stopPlugin("test-plugin"))
                .verifyComplete();

        // Verify the plugin state
        StepVerifier.create(registry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals(PluginState.STOPPED, descriptor.state());
                })
                .verifyComplete();

        // Verify stop was called
        verify(plugin).stop();

        // Verify lifecycle events were published (register, start, and stop)
        verify(eventBus, times(3)).publish(any(PluginLifecycleEvent.class));
    }

    @Test
    void testUpdatePluginConfiguration() {
        // Register the plugin
        StepVerifier.create(registry.registerPlugin(plugin))
                .verifyComplete();

        // Update configuration
        Map<String, Object> config = Map.of("key1", "value1", "key2", 42);
        StepVerifier.create(registry.updatePluginConfiguration("test-plugin", config))
                .verifyComplete();

        // Verify the configuration was updated
        StepVerifier.create(registry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals(config, descriptor.configuration());
                })
                .verifyComplete();
    }

    @Test
    void testGetPluginsByState() {
        // Register two plugins
        PluginMetadata metadata2 = PluginMetadata.builder()
                .id("test-plugin-2")
                .name("Test Plugin 2")
                .version("1.0.0")
                .build();

        Plugin plugin2 = mock(Plugin.class);
        when(plugin2.getMetadata()).thenReturn(metadata2);
        when(plugin2.initialize()).thenReturn(Mono.empty());

        StepVerifier.create(registry.registerPlugin(plugin)
                .then(registry.registerPlugin(plugin2)))
                .verifyComplete();

        // Start only the first plugin
        StepVerifier.create(registry.startPlugin("test-plugin"))
                .verifyComplete();

        // Verify getPluginsByState for STARTED
        StepVerifier.create(registry.getPluginsByState(PluginState.STARTED).collectList())
                .assertNext(plugins -> {
                    assertEquals(1, plugins.size());
                    assertEquals("test-plugin", plugins.get(0).getMetadata().id());
                })
                .verifyComplete();

        // Verify getPluginsByState for INITIALIZED
        StepVerifier.create(registry.getPluginsByState(PluginState.INITIALIZED).collectList())
                .assertNext(plugins -> {
                    assertEquals(1, plugins.size());
                    assertEquals("test-plugin-2", plugins.get(0).getMetadata().id());
                })
                .verifyComplete();
    }

    @Test
    void testStartPluginError() {
        // Register the plugin
        StepVerifier.create(registry.registerPlugin(plugin))
                .verifyComplete();

        // Make start throw an error
        RuntimeException error = new RuntimeException("Start error");
        when(plugin.start()).thenReturn(Mono.error(error));

        // Start the plugin
        StepVerifier.create(registry.startPlugin("test-plugin"))
                .expectErrorMatches(e -> e == error)
                .verify();

        // Verify the plugin state is FAILED
        StepVerifier.create(registry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals(PluginState.FAILED, descriptor.state());
                })
                .verifyComplete();
    }
}
