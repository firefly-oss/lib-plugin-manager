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


package com.firefly.core.plugin.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

public class PluginClassLoaderTest {

    private static final String TEST_PLUGIN_ID = "test-plugin";
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
        classLoader = new PluginClassLoader(TEST_PLUGIN_ID, urls, getClass().getClassLoader());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (classLoader != null) {
            classLoader.close();
        }
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
    void testAllowPackages() {
        // Add multiple custom packages
        Set<String> packages = new HashSet<>(Arrays.asList("com.custom1", "com.custom2"));
        classLoader.allowPackages(packages);

        // Use reflection to access the private field
        try {
            java.lang.reflect.Field allowedPackagesField = PluginClassLoader.class.getDeclaredField("allowedPackages");
            allowedPackagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Set<String> allowedPackages = (java.util.Set<String>) allowedPackagesField.get(classLoader);

            // Verify the packages were added
            assertTrue(allowedPackages.contains("com.custom1"));
            assertTrue(allowedPackages.contains("com.custom2"));
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
            assertTrue(allowedPackages.contains("com.firefly.core.plugin.api"));
            assertTrue(allowedPackages.contains("com.firefly.core.plugin.annotation"));
            assertTrue(allowedPackages.contains("com.firefly.core.plugin.event"));
            assertTrue(allowedPackages.contains("com.firefly.core.plugin.model"));
            assertTrue(allowedPackages.contains("com.firefly.core.plugin.spi"));
            assertTrue(allowedPackages.contains("java."));
            assertTrue(allowedPackages.contains("javax."));
            assertTrue(allowedPackages.contains("jakarta."));
            assertTrue(allowedPackages.contains("org.springframework."));
            assertTrue(allowedPackages.contains("reactor."));
            assertTrue(allowedPackages.contains("org.slf4j."));
            assertTrue(allowedPackages.contains("ch.qos.logback."));
        } catch (Exception e) {
            fail("Failed to access allowedPackages field: " + e.getMessage());
        }
    }

    @Test
    void testExportPackage() {
        // Export a package
        classLoader.exportPackage("com.example.exported");

        // Verify the package was exported
        Set<String> exportedPackages = classLoader.getExportedPackages();
        assertTrue(exportedPackages.contains("com.example.exported"));
    }

    @Test
    void testGetPluginId() {
        assertEquals(TEST_PLUGIN_ID, classLoader.getPluginId());
    }

    @Test
    void testGetLoadedClasses() {
        // Initially, no classes should be loaded
        assertTrue(classLoader.getLoadedClasses().isEmpty());

        // Load a class
        try {
            classLoader.loadClass("java.lang.String");

            // The class should not be in the loaded classes set because it was loaded by the parent
            assertTrue(classLoader.getLoadedClasses().isEmpty());
        } catch (ClassNotFoundException e) {
            fail("Failed to load class: " + e.getMessage());
        }
    }

    @Test
    void testConstructorWithAllowedPackages() {
        // Create a class loader with additional allowed packages
        Set<String> additionalPackages = new HashSet<>(Arrays.asList("com.custom1", "com.custom2"));
        try (PluginClassLoader loader = new PluginClassLoader("test", urls, getClass().getClassLoader(), additionalPackages, null)) {
            // Use reflection to access the private field
            java.lang.reflect.Field allowedPackagesField = PluginClassLoader.class.getDeclaredField("allowedPackages");
            allowedPackagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Set<String> allowedPackages = (java.util.Set<String>) allowedPackagesField.get(loader);

            // Verify the additional packages were added
            assertTrue(allowedPackages.contains("com.custom1"));
            assertTrue(allowedPackages.contains("com.custom2"));
        } catch (Exception e) {
            fail("Failed to create or access class loader: " + e.getMessage());
        }
    }

    @Test
    void testSecurityExceptionForUnauthorizedClass() {
        // Create a class loader with strict security
        try (PluginClassLoader loader = new PluginClassLoader("test", urls, getClass().getClassLoader(), Collections.emptySet(), null)) {
            // Try to load a class that is not in an allowed package
            assertThrows(SecurityException.class, () -> loader.loadClass("com.unauthorized.SomeClass"));
        } catch (Exception e) {
            fail("Failed to create or access class loader: " + e.getMessage());
        }
    }

    /**
     * Creates a test JAR file with a minimal structure.
     */
    private void createTestJar(File jarFile) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Plugin-Id", TEST_PLUGIN_ID);

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
