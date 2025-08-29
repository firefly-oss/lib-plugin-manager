package com.firefly.core.plugin;

import com.firefly.core.plugin.api.ExtensionRegistry;
import com.firefly.core.plugin.api.Plugin;
import com.firefly.core.plugin.api.PluginRegistry;
import com.firefly.core.plugin.dependency.PluginDependencyResolver;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.loader.PluginLoader;
import com.firefly.core.plugin.model.PluginDescriptor;
import com.firefly.core.plugin.model.PluginMetadata;
import com.firefly.core.plugin.model.PluginState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class DefaultPluginManagerTest {

    @Mock
    private PluginRegistry pluginRegistry;

    @Mock
    private ExtensionRegistry extensionRegistry;

    @Mock
    private PluginEventBus eventBus;

    @Mock
    private PluginLoader pluginLoader;

    @Mock
    private PluginDependencyResolver dependencyResolver;

    @Mock
    private Plugin plugin;

    private DefaultPluginManager pluginManager;
    private PluginMetadata metadata;
    private PluginDescriptor descriptor;

    @BeforeEach
    void setUp() {
        pluginManager = new DefaultPluginManager(pluginRegistry, extensionRegistry, eventBus, pluginLoader, dependencyResolver);

        metadata = PluginMetadata.builder()
                .id("test-plugin")
                .name("Test Plugin")
                .version("1.0.0")
                .description("Test plugin for unit tests")
                .author("Test Author")
                .dependencies(Set.of())
                .installTime(Instant.now())
                .build();

        descriptor = PluginDescriptor.builder()
                .metadata(metadata)
                .state(PluginState.INSTALLED)
                .configuration(Map.of())
                .build();

        when(plugin.getMetadata()).thenReturn(metadata);
        when(plugin.initialize()).thenReturn(Mono.empty());
        when(plugin.start()).thenReturn(Mono.empty());
        when(plugin.stop()).thenReturn(Mono.empty());
        when(plugin.uninstall()).thenReturn(Mono.empty());
    }

    @Test
    void testInstallPlugin() {
        // Mock plugin loader
        Path pluginPath = Path.of("plugins/test-plugin.jar");
        when(pluginLoader.loadPlugin(pluginPath)).thenReturn(Mono.just(plugin));

        // Mock plugin registry
        when(pluginRegistry.registerPlugin(plugin)).thenReturn(Mono.empty());
        when(pluginRegistry.getPluginDescriptor("test-plugin")).thenReturn(Mono.just(descriptor));

        // Install the plugin
        StepVerifier.create(pluginManager.installPlugin(pluginPath))
                .expectNext(descriptor)
                .verifyComplete();

        // Verify interactions
        verify(pluginLoader).loadPlugin(pluginPath);
        verify(pluginRegistry).registerPlugin(plugin);
        verify(pluginRegistry).getPluginDescriptor("test-plugin");
    }

    @Test
    void testUninstallPlugin() {
        // Mock plugin registry
        when(pluginRegistry.getPlugin("test-plugin")).thenReturn(Mono.just(plugin));
        when(pluginRegistry.unregisterPlugin("test-plugin")).thenReturn(Mono.empty());

        // Uninstall the plugin
        StepVerifier.create(pluginManager.uninstallPlugin("test-plugin"))
                .verifyComplete();

        // Verify interactions
        verify(pluginRegistry).getPlugin("test-plugin");
        verify(plugin).uninstall();
        verify(pluginRegistry).unregisterPlugin("test-plugin");
    }

    @Test
    void testStartPlugin() {
        // Create a mock plugin descriptor
        PluginDescriptor descriptor = mock(PluginDescriptor.class);
        when(descriptor.getId()).thenReturn("test-plugin");
        when(descriptor.state()).thenReturn(PluginState.INITIALIZED);

        // Mock plugin registry
        when(pluginRegistry.getPluginDescriptor("test-plugin")).thenReturn(Mono.just(descriptor));
        when(pluginRegistry.getAllPluginDescriptors()).thenReturn(Flux.just(descriptor));
        when(pluginRegistry.startPlugin("test-plugin")).thenReturn(Mono.empty());

        // Mock dependency resolver
        when(dependencyResolver.resolveDependencies(anyList())).thenReturn(List.of(descriptor));

        // Start the plugin
        StepVerifier.create(pluginManager.startPlugin("test-plugin"))
                .verifyComplete();

        // Verify interactions
        verify(pluginRegistry).startPlugin("test-plugin");
    }

    @Test
    void testStopPlugin() {
        // Mock plugin registry
        when(pluginRegistry.stopPlugin("test-plugin")).thenReturn(Mono.empty());

        // Stop the plugin
        StepVerifier.create(pluginManager.stopPlugin("test-plugin"))
                .verifyComplete();

        // Verify interactions
        verify(pluginRegistry).stopPlugin("test-plugin");
    }

    @Test
    void testRestartPlugin() {
        // Create a mock plugin descriptor
        PluginDescriptor descriptor = mock(PluginDescriptor.class);
        when(descriptor.getId()).thenReturn("test-plugin");
        when(descriptor.state()).thenReturn(PluginState.INITIALIZED);

        // Mock plugin registry
        when(pluginRegistry.getPluginDescriptor("test-plugin")).thenReturn(Mono.just(descriptor));
        when(pluginRegistry.getAllPluginDescriptors()).thenReturn(Flux.just(descriptor));
        when(pluginRegistry.stopPlugin("test-plugin")).thenReturn(Mono.empty());
        when(pluginRegistry.startPlugin("test-plugin")).thenReturn(Mono.empty());

        // Mock dependency resolver
        when(dependencyResolver.resolveDependencies(anyList())).thenReturn(List.of(descriptor));

        // Restart the plugin
        StepVerifier.create(pluginManager.restartPlugin("test-plugin"))
                .verifyComplete();

        // Verify interactions
        verify(pluginRegistry).stopPlugin("test-plugin");
        verify(pluginRegistry).startPlugin("test-plugin");
    }

    @Test
    void testUpdatePluginConfiguration() {
        // Mock plugin registry
        Map<String, Object> config = Map.of("key", "value");
        when(pluginRegistry.updatePluginConfiguration("test-plugin", config)).thenReturn(Mono.empty());

        // Update configuration
        StepVerifier.create(pluginManager.updatePluginConfiguration("test-plugin", config))
                .verifyComplete();

        // Verify interactions
        verify(pluginRegistry).updatePluginConfiguration("test-plugin", config);
    }

    @Test
    void testGetAllPlugins() {
        // Mock plugin registry
        when(pluginRegistry.getAllPluginDescriptors()).thenReturn(Flux.just(descriptor));

        // Get all plugins
        StepVerifier.create(pluginManager.getAllPlugins())
                .expectNext(descriptor)
                .verifyComplete();

        // Verify interactions
        verify(pluginRegistry).getAllPluginDescriptors();
    }

    @Test
    void testGetPlugin() {
        // Mock plugin registry
        when(pluginRegistry.getPluginDescriptor("test-plugin")).thenReturn(Mono.just(descriptor));

        // Get plugin
        StepVerifier.create(pluginManager.getPlugin("test-plugin"))
                .expectNext(descriptor)
                .verifyComplete();

        // Verify interactions
        verify(pluginRegistry).getPluginDescriptor("test-plugin");
    }

    @Test
    void testInitialize() {
        // Initialize the plugin manager
        StepVerifier.create(pluginManager.initialize())
                .verifyComplete();
    }

    @Test
    void testShutdown() {
        // Mock plugin registry
        when(pluginRegistry.getAllPlugins()).thenReturn(Flux.just(plugin));
        when(pluginRegistry.stopPlugin("test-plugin")).thenReturn(Mono.empty());

        // Shutdown the plugin manager
        StepVerifier.create(pluginManager.shutdown())
                .verifyComplete();

        // Verify interactions
        verify(pluginRegistry).getAllPlugins();
        verify(pluginRegistry).stopPlugin("test-plugin");
    }

    @Test
    void testGetters() {
        assertEquals(pluginRegistry, pluginManager.getPluginRegistry());
        assertEquals(extensionRegistry, pluginManager.getExtensionRegistry());
        assertEquals(eventBus, pluginManager.getEventBus());
    }

    @Test
    void testInstallPluginFromGit() {
        // Mock plugin loader
        URI repositoryUri = URI.create("https://github.com/example/test-plugin.git");
        String branch = "main";
        when(pluginLoader.loadPluginFromGit(repositoryUri, branch)).thenReturn(Mono.just(plugin));

        // Mock plugin registry
        when(pluginRegistry.registerPlugin(plugin)).thenReturn(Mono.empty());
        when(pluginRegistry.getPluginDescriptor("test-plugin")).thenReturn(Mono.just(descriptor));

        // Install the plugin
        StepVerifier.create(pluginManager.installPluginFromGit(repositoryUri, branch))
                .expectNext(descriptor)
                .verifyComplete();

        // Verify interactions
        verify(pluginLoader).loadPluginFromGit(repositoryUri, branch);
        verify(pluginRegistry).registerPlugin(plugin);
        verify(pluginRegistry).getPluginDescriptor("test-plugin");
    }

    @Test
    void testInstallPluginFromGitWithDefaultBranch() {
        // Mock plugin loader
        URI repositoryUri = URI.create("https://github.com/example/test-plugin.git");
        when(pluginLoader.loadPluginFromGit(repositoryUri, null)).thenReturn(Mono.just(plugin));

        // Mock plugin registry
        when(pluginRegistry.registerPlugin(plugin)).thenReturn(Mono.empty());
        when(pluginRegistry.getPluginDescriptor("test-plugin")).thenReturn(Mono.just(descriptor));

        // Install the plugin
        StepVerifier.create(pluginManager.installPluginFromGit(repositoryUri))
                .expectNext(descriptor)
                .verifyComplete();

        // Verify interactions
        verify(pluginLoader).loadPluginFromGit(repositoryUri, null);
        verify(pluginRegistry).registerPlugin(plugin);
        verify(pluginRegistry).getPluginDescriptor("test-plugin");
    }

    @Test
    void testInstallPluginsFromClasspath() {
        // Mock plugin loader
        String basePackage = "com.example";
        when(pluginLoader.loadPluginsFromClasspath(basePackage)).thenReturn(Flux.just(plugin));

        // Mock plugin registry
        when(pluginRegistry.registerPlugin(plugin)).thenReturn(Mono.empty());
        when(pluginRegistry.getPluginDescriptor("test-plugin")).thenReturn(Mono.just(descriptor));

        // Install the plugins
        StepVerifier.create(pluginManager.installPluginsFromClasspath(basePackage))
                .expectNext(descriptor)
                .verifyComplete();

        // Verify interactions
        verify(pluginLoader).loadPluginsFromClasspath(basePackage);
        verify(pluginRegistry).registerPlugin(plugin);
        verify(pluginRegistry).getPluginDescriptor("test-plugin");
    }

    @Test
    void testInstallPluginsFromClasspathWithDefaultPackage() {
        // Mock plugin loader
        when(pluginLoader.loadPluginsFromClasspath(null)).thenReturn(Flux.just(plugin));

        // Mock plugin registry
        when(pluginRegistry.registerPlugin(plugin)).thenReturn(Mono.empty());
        when(pluginRegistry.getPluginDescriptor("test-plugin")).thenReturn(Mono.just(descriptor));

        // Install the plugins
        StepVerifier.create(pluginManager.installPluginsFromClasspath())
                .expectNext(descriptor)
                .verifyComplete();

        // Verify interactions
        verify(pluginLoader).loadPluginsFromClasspath(null);
        verify(pluginRegistry).registerPlugin(plugin);
        verify(pluginRegistry).getPluginDescriptor("test-plugin");
    }
}
