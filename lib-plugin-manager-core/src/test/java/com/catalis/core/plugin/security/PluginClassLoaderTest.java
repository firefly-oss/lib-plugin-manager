package com.catalis.core.plugin.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

public class PluginClassLoaderTest {

    private PluginClassLoader classLoader;
    private URL[] urls;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        // Create a test JAR file
        File jarFile = tempDir.resolve("test-plugin.jar").toFile();
        createTestJar(jarFile);

        // Create URLs array with the test JAR
        urls = new URL[] { jarFile.toURI().toURL() };

        // Create the plugin class loader
        classLoader = new PluginClassLoader(urls, getClass().getClassLoader());
    }

    @Test
    void testLoadClassFromAllowedPackage() throws Exception {
        // Load a class from an allowed package (java.*)
        Class<?> stringClass = classLoader.loadClass("java.lang.String");
        assertNotNull(stringClass);
        assertEquals(String.class, stringClass);
    }

    @Test
    void testLoadClassFromPlugin() throws Exception {
        // Add a custom class to the allowed packages
        classLoader.allowPackage("com.example");

        // This will fail because the class doesn't actually exist in our test JAR,
        // but it demonstrates the code path
        try {
            classLoader.loadClass("com.example.NonExistentClass");
            fail("Should have thrown ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // Expected exception
            assertTrue(true);
        }
    }

    @Test
    void testAllowPackage() {
        // Add a custom package
        classLoader.allowPackage("com.custom");

        // Use reflection to access the private field
        try {
            java.lang.reflect.Field allowedPackagesField = PluginClassLoader.class.getDeclaredField("allowedPackages");
            allowedPackagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Set<String> allowedPackages = (java.util.Set<String>) allowedPackagesField.get(classLoader);

            // Verify the package was added
            assertTrue(allowedPackages.contains("com.custom"));
        } catch (Exception e) {
            fail("Failed to access allowedPackages field: " + e.getMessage());
        }
    }

    @Test
    void testDefaultAllowedPackages() {
        // Use reflection to access the private field
        try {
            java.lang.reflect.Field allowedPackagesField = PluginClassLoader.class.getDeclaredField("allowedPackages");
            allowedPackagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Set<String> allowedPackages = (java.util.Set<String>) allowedPackagesField.get(classLoader);

            // Verify default allowed packages
            assertTrue(allowedPackages.contains("com.catalis.core.plugin.api"));
            assertTrue(allowedPackages.contains("com.catalis.core.plugin.annotation"));
            assertTrue(allowedPackages.contains("com.catalis.core.plugin.event"));
            assertTrue(allowedPackages.contains("com.catalis.core.plugin.model"));
            assertTrue(allowedPackages.contains("com.catalis.core.plugin.spi"));
            assertTrue(allowedPackages.contains("java."));
            assertTrue(allowedPackages.contains("javax."));
            assertTrue(allowedPackages.contains("org.springframework."));
            assertTrue(allowedPackages.contains("reactor."));
        } catch (Exception e) {
            fail("Failed to access allowedPackages field: " + e.getMessage());
        }
    }

    /**
     * Creates a test JAR file with a minimal structure.
     */
    private void createTestJar(File jarFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");

        try (FileOutputStream fos = new FileOutputStream(jarFile);
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {

            // Add a dummy class file
            JarEntry entry = new JarEntry("com/example/TestClass.class");
            jos.putNextEntry(entry);
            jos.write(new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE });
            jos.closeEntry();
        }
    }
}
