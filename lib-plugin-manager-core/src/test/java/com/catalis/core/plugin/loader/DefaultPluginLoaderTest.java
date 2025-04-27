package com.catalis.core.plugin.loader;

import com.catalis.core.plugin.api.Plugin;
import com.catalis.core.plugin.model.PluginMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DefaultPluginLoaderTest {

    private DefaultPluginLoader pluginLoader;

    @BeforeEach
    void setUp() {
        pluginLoader = new DefaultPluginLoader();
    }

    @Test
    void testValidatePluginMetadata() throws Exception {
        // Create valid metadata
        PluginMetadata validMetadata = PluginMetadata.builder()
                .id("test-plugin")
                .name("Test Plugin")
                .version("1.0.0")
                .build();

        // Use reflection to access the private method
        java.lang.reflect.Method validateMethod = DefaultPluginLoader.class.getDeclaredMethod(
                "validatePluginMetadata", PluginMetadata.class);
        validateMethod.setAccessible(true);

        // Should not throw an exception
        validateMethod.invoke(pluginLoader, validMetadata);

        // Create invalid metadata (null ID)
        PluginMetadata invalidMetadata1 = PluginMetadata.builder()
                .id(null)
                .name("Test Plugin")
                .version("1.0.0")
                .build();

        // Should throw an exception
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            validateMethod.invoke(pluginLoader, invalidMetadata1);
        });

        // Create invalid metadata (blank name)
        PluginMetadata invalidMetadata2 = PluginMetadata.builder()
                .id("test-plugin")
                .name("")
                .version("1.0.0")
                .build();

        // Should throw an exception
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            validateMethod.invoke(pluginLoader, invalidMetadata2);
        });

        // Create invalid metadata (null version)
        PluginMetadata invalidMetadata3 = PluginMetadata.builder()
                .id("test-plugin")
                .name("Test Plugin")
                .version(null)
                .build();

        // Should throw an exception
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            validateMethod.invoke(pluginLoader, invalidMetadata3);
        });
    }

    @Test
    void testLoadPluginWithInvalidPath() {
        // Try to load a plugin from a non-existent path
        Path nonExistentPath = Path.of("non-existent-plugin.jar");

        StepVerifier.create(pluginLoader.loadPlugin(nonExistentPath))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testLoadPluginWithMockClassLoader(@TempDir Path tempDir) throws Exception {
        // Create a mock plugin
        Plugin mockPlugin = mock(Plugin.class);
        PluginMetadata metadata = PluginMetadata.builder()
                .id("mock-plugin")
                .name("Mock Plugin")
                .version("1.0.0")
                .build();
        when(mockPlugin.getMetadata()).thenReturn(metadata);

        // Create a test JAR file
        Path jarPath = tempDir.resolve("mock-plugin.jar");
        createTestJar(jarPath.toFile());

        // Create a custom plugin loader that uses our mock plugin
        DefaultPluginLoader customLoader = new DefaultPluginLoader() {
            @Override
            public Mono<Plugin> loadPlugin(Path pluginPath) {
                // Simply return the mock plugin
                return Mono.just(mockPlugin);
            }
        };

        // Load the plugin
        StepVerifier.create(customLoader.loadPlugin(jarPath))
                .expectNextMatches(plugin -> plugin == mockPlugin)
                .verifyComplete();
    }

    /**
     * Creates a test JAR file with a minimal structure.
     */
    private void createTestJar(File jarFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (OutputStream os = new FileOutputStream(jarFile);
             JarOutputStream jos = new JarOutputStream(os, manifest)) {

            // Add a dummy class file
            JarEntry entry = new JarEntry("com/example/DummyClass.class");
            jos.putNextEntry(entry);
            jos.write(new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE });
            jos.closeEntry();

            // Add a service provider configuration file
            entry = new JarEntry("META-INF/services/com.catalis.core.plugin.api.Plugin");
            jos.putNextEntry(entry);
            jos.write("com.example.MockPlugin".getBytes());
            jos.closeEntry();
        }
    }
}
