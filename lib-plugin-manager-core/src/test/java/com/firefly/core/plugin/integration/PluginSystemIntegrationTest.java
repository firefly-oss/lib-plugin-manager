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


package com.firefly.core.plugin.integration;

import com.firefly.core.plugin.DefaultPluginManager;
import com.firefly.core.plugin.api.Plugin;
import com.firefly.core.plugin.api.PluginManager;
import com.firefly.core.plugin.dependency.PluginDependencyResolver;
import com.firefly.core.plugin.event.DefaultPluginEventBus;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.extension.DefaultExtensionRegistry;
import com.firefly.core.plugin.loader.DefaultPluginLoader;
import com.firefly.core.plugin.model.PluginMetadata;
import com.firefly.core.plugin.model.PluginState;
import com.firefly.core.plugin.registry.DefaultPluginRegistry;
import com.firefly.core.plugin.spi.AbstractPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class PluginSystemIntegrationTest {

    private PluginEventBus eventBus;
    private DefaultPluginRegistry pluginRegistry;
    private DefaultExtensionRegistry extensionRegistry;
    private DefaultPluginLoader pluginLoader;
    private PluginDependencyResolver dependencyResolver;
    private PluginManager pluginManager;

    // Test extension point
    public interface TestExtensionPoint {
        String getName();
        Mono<String> performAction(String input);
    }

    // Test plugin implementation
    public static class TestPlugin extends AbstractPlugin {
        private final AtomicBoolean initialized = new AtomicBoolean(false);
        private final AtomicBoolean started = new AtomicBoolean(false);
        private final AtomicBoolean stopped = new AtomicBoolean(false);
        private final TestExtensionImpl extension;

        public TestPlugin() {
            super(PluginMetadata.builder()
                    .id("test-plugin")
                    .name("Test Plugin")
                    .version("1.0.0")
                    .description("Test plugin for integration tests")
                    .author("Test Author")
                    .dependencies(Set.of())
                    .installTime(Instant.now())
                    .build());

            this.extension = new TestExtensionImpl();
        }

        @Override
        public Mono<Void> initialize() {
            return Mono.fromRunnable(() -> initialized.set(true));
        }

        @Override
        public Mono<Void> start() {
            return Mono.fromRunnable(() -> started.set(true));
        }

        @Override
        public Mono<Void> stop() {
            return Mono.fromRunnable(() -> stopped.set(true));
        }

        public boolean isInitialized() {
            return initialized.get();
        }

        public boolean isStarted() {
            return started.get();
        }

        public boolean isStopped() {
            return stopped.get();
        }

        public TestExtensionImpl getExtension() {
            return extension;
        }

        public class TestExtensionImpl implements TestExtensionPoint {
            @Override
            public String getName() {
                return "Test Extension";
            }

            @Override
            public Mono<String> performAction(String input) {
                return Mono.just("Processed: " + input);
            }
        }
    }

    @BeforeEach
    void setUp() {
        eventBus = new DefaultPluginEventBus();
        pluginRegistry = new DefaultPluginRegistry(eventBus);
        extensionRegistry = new DefaultExtensionRegistry();
        pluginLoader = new DefaultPluginLoader();
        dependencyResolver = new PluginDependencyResolver();
        pluginManager = new DefaultPluginManager(pluginRegistry, extensionRegistry, eventBus, pluginLoader, dependencyResolver);
    }

    @Test
    void testPluginLifecycle() {
        // Create a test plugin
        TestPlugin testPlugin = new TestPlugin();

        // Register the plugin
        StepVerifier.create(pluginRegistry.registerPlugin(testPlugin))
                .verifyComplete();

        // Verify the plugin is initialized
        assertTrue(testPlugin.isInitialized());

        // Verify the plugin state
        StepVerifier.create(pluginRegistry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals(PluginState.INITIALIZED, descriptor.state());
                })
                .verifyComplete();

        // Start the plugin
        StepVerifier.create(pluginRegistry.startPlugin("test-plugin"))
                .verifyComplete();

        // Verify the plugin is started
        assertTrue(testPlugin.isStarted());

        // Verify the plugin state
        StepVerifier.create(pluginRegistry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals(PluginState.STARTED, descriptor.state());
                })
                .verifyComplete();

        // Stop the plugin
        StepVerifier.create(pluginRegistry.stopPlugin("test-plugin"))
                .verifyComplete();

        // Verify the plugin is stopped
        assertTrue(testPlugin.isStopped());

        // Verify the plugin state
        StepVerifier.create(pluginRegistry.getPluginDescriptor("test-plugin"))
                .assertNext(descriptor -> {
                    assertEquals(PluginState.STOPPED, descriptor.state());
                })
                .verifyComplete();
    }

    @Test
    void testExtensionRegistryAndUsage() {
        // Create a test plugin
        TestPlugin testPlugin = new TestPlugin();

        // Register the plugin
        StepVerifier.create(pluginRegistry.registerPlugin(testPlugin))
                .verifyComplete();

        // Register the extension point
        StepVerifier.create(extensionRegistry.registerExtensionPoint(
                "test.extension-point", TestExtensionPoint.class))
                .verifyComplete();

        // Register the extension
        StepVerifier.create(extensionRegistry.registerExtension(
                "test.extension-point", testPlugin.getExtension(), 100))
                .verifyComplete();

        // Get the extension
        StepVerifier.create(extensionRegistry.getHighestPriorityExtension("test.extension-point"))
                .assertNext(extension -> {
                    assertTrue(extension instanceof TestExtensionPoint);
                    assertEquals("Test Extension", ((TestExtensionPoint) extension).getName());
                })
                .verifyComplete();

        // Use the extension
        StepVerifier.create(extensionRegistry.<TestExtensionPoint>getHighestPriorityExtension("test.extension-point")
                .flatMap(extension -> extension.performAction("test input")))
                .expectNext("Processed: test input")
                .verifyComplete();
    }

    @Test
    void testPluginManagerOperations() {
        // Create a test plugin
        TestPlugin testPlugin = new TestPlugin();

        // Register the plugin using the plugin manager
        StepVerifier.create(pluginRegistry.registerPlugin(testPlugin))
                .verifyComplete();

        // Start the plugin using the plugin manager
        StepVerifier.create(pluginManager.startPlugin("test-plugin"))
                .verifyComplete();

        // Verify the plugin is started
        assertTrue(testPlugin.isStarted());

        // Stop the plugin using the plugin manager
        StepVerifier.create(pluginManager.stopPlugin("test-plugin"))
                .verifyComplete();

        // Verify the plugin is stopped
        assertTrue(testPlugin.isStopped());

        // Restart the plugin using the plugin manager
        StepVerifier.create(pluginManager.restartPlugin("test-plugin"))
                .verifyComplete();

        // Verify the plugin is started again
        assertTrue(testPlugin.isStarted());
    }

    @Test
    void testEventBusCommunication() {
        // Create a test plugin
        TestPlugin testPlugin = new TestPlugin();

        // Register the plugin
        StepVerifier.create(pluginRegistry.registerPlugin(testPlugin))
                .verifyComplete();

        // Create a custom event
        TestEvent event = new TestEvent("test-plugin", "Hello from test!");

        // Subscribe to events
        AtomicBoolean eventReceived = new AtomicBoolean(false);
        eventBus.subscribe(TestEvent.class)
                .doOnNext(e -> eventReceived.set(true))
                .subscribe();

        // Publish the event
        StepVerifier.create(eventBus.publish(event))
                .verifyComplete();

        // Verify the event was received
        assertTrue(eventReceived.get());
    }

    // Custom event class for testing
    private static class TestEvent extends com.firefly.core.plugin.event.PluginEvent {
        private final String message;

        public TestEvent(String pluginId, String message) {
            super(pluginId, "TEST");
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
