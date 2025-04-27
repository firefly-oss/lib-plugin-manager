package com.catalis.core.plugin.hotdeploy;

import com.catalis.core.plugin.api.PluginManager;
import com.catalis.core.plugin.config.PluginManagerProperties;
import com.catalis.core.plugin.model.PluginDescriptor;
import com.catalis.core.plugin.model.PluginState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PluginDirectoryWatcherTest {

    @Mock
    private PluginManager pluginManager;

    private PluginManagerProperties properties;
    private PluginDirectoryWatcher watcher;
    private AutoCloseable mocks;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Set up properties
        properties = new PluginManagerProperties();
        properties.setPluginsDirectory(tempDir);
        properties.getHotDeployment().setEnabled(true);
        properties.getHotDeployment().setPollingIntervalMs(100); // Use a short interval for testing
        properties.setAutoStartPlugins(true);

        // Create the watcher
        watcher = new PluginDirectoryWatcher(pluginManager, properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Stop the watcher
        watcher.stop();

        // Close mocks
        mocks.close();
    }

    @Test
    void testNewPluginDetection() throws Exception {
        // Set up mock behavior
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id("test-plugin")
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.INSTALLED)
                .build();

        when(pluginManager.installPlugin(any(Path.class))).thenReturn(Mono.just(descriptor));
        when(pluginManager.startPlugin("test-plugin")).thenReturn(Mono.empty());

        // Start the watcher
        watcher.start();

        // Create a new plugin JAR file
        createPluginJar("test-plugin.jar");

        // Wait for the watcher to detect the new plugin
        Thread.sleep(500);

        // Verify that the plugin was installed
        verify(pluginManager, timeout(1000)).installPlugin(any(Path.class));

        // Since autoStartPlugins is true by default, verify that the plugin was started
        verify(pluginManager, timeout(1000)).startPlugin("test-plugin");
    }

    @Test
    void testDisabledHotDeployment() throws Exception {
        // Disable hot deployment
        properties.getHotDeployment().setEnabled(false);

        // Create a new watcher with hot deployment disabled
        PluginDirectoryWatcher disabledWatcher = new PluginDirectoryWatcher(pluginManager, properties);

        try {
            // Start the watcher
            disabledWatcher.start();

            // Create a new plugin JAR file
            createPluginJar("test-plugin.jar");

            // Wait for a while
            Thread.sleep(500);

            // Verify that the plugin was not installed
            verify(pluginManager, never()).installPlugin(any(Path.class));
        } finally {
            disabledWatcher.stop();
        }
    }

    @Test
    void testDisabledAutoReload() throws Exception {
        // Disable auto reload
        properties.getHotDeployment().setAutoReload(false);

        // Set up mock behavior
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id("test-plugin")
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.INSTALLED)
                .build();

        when(pluginManager.getAllPlugins()).thenReturn(Flux.just(descriptor));

        // Start the watcher
        watcher.start();

        // Create a new plugin JAR file
        createPluginJar("test-plugin.jar");

        // Wait for a while
        Thread.sleep(500);

        // Verify that the plugin was not installed
        verify(pluginManager, never()).installPlugin(any(Path.class));
    }

    /**
     * Creates a test plugin JAR file.
     *
     * @param fileName the name of the JAR file
     * @return the path to the created JAR file
     * @throws IOException if an I/O error occurs
     */
    private Path createPluginJar(String fileName) throws IOException {
        Path pluginPath = tempDir.resolve(fileName);

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Plugin-Id", "test-plugin");
        manifest.getMainAttributes().putValue("Plugin-Version", "1.0.0");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(pluginPath.toFile()), manifest)) {
            // Add a dummy class file
            JarEntry entry = new JarEntry("com/example/TestPlugin.class");
            jos.putNextEntry(entry);
            jos.write(new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE });
            jos.closeEntry();
        }

        return pluginPath;
    }
}
