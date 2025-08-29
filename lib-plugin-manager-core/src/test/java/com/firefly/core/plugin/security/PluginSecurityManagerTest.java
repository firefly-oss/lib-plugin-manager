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
import java.security.Permission;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

public class PluginSecurityManagerTest {

    private static final String TEST_PLUGIN_ID = "test-plugin";
    private PluginSecurityManager securityManager;
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

        // Create the security manager
        securityManager = new PluginSecurityManager(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Close the class loader
        if (classLoader != null) {
            classLoader.close();
        }
    }

    @Test
    void testRegisterAndUnregisterPlugin() {
        // Create permissions
        Set<PluginPermission> permissions = new HashSet<>();
        permissions.add(new PluginPermission(PluginPermission.Type.FILE_SYSTEM, "/tmp", "read"));

        // Register the plugin
        securityManager.registerPlugin(TEST_PLUGIN_ID, classLoader, permissions);

        // Unregister the plugin
        securityManager.unregisterPlugin(TEST_PLUGIN_ID, classLoader);
    }

    @Test
    void testAddAndRemovePermission() {
        // Register the plugin with no permissions
        securityManager.registerPlugin(TEST_PLUGIN_ID, classLoader, new HashSet<>());

        // Add a permission
        PluginPermission permission = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp", "read");
        securityManager.addPermission(TEST_PLUGIN_ID, permission);

        // Remove the permission
        securityManager.removePermission(TEST_PLUGIN_ID, permission);

        // Unregister the plugin
        securityManager.unregisterPlugin(TEST_PLUGIN_ID, classLoader);
    }

    @Test
    void testCheckPermissionWithEnforcement() {
        // Create a security manager with enforcement enabled
        PluginSecurityManager manager = new PluginSecurityManager(true);

        // Register the plugin with limited permissions
        Set<PluginPermission> permissions = new HashSet<>();
        permissions.add(new PluginPermission(PluginPermission.Type.FILE_SYSTEM, "/tmp", "read"));
        manager.registerPlugin(TEST_PLUGIN_ID, classLoader, permissions);

        // This should not throw an exception because we're not running in the plugin's context
        manager.checkPermission(new java.io.FilePermission("/etc/passwd", "read"));

        // Unregister the plugin
        manager.unregisterPlugin(TEST_PLUGIN_ID, classLoader);
    }

    @Test
    void testCheckPermissionWithoutEnforcement() {
        // Create a security manager with enforcement disabled
        PluginSecurityManager manager = new PluginSecurityManager(false);

        // Register the plugin with no permissions
        manager.registerPlugin(TEST_PLUGIN_ID, classLoader, new HashSet<>());

        // This should not throw an exception because enforcement is disabled
        manager.checkPermission(new java.io.FilePermission("/etc/passwd", "read"));

        // Unregister the plugin
        manager.unregisterPlugin(TEST_PLUGIN_ID, classLoader);
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

    /**
     * A test permission class for testing the security manager.
     */
    private static class TestPermission extends Permission {
        private static final long serialVersionUID = 1L;

        public TestPermission(String name) {
            super(name);
        }

        @Override
        public boolean implies(Permission permission) {
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof TestPermission)) {
                return false;
            }
            return getName().equals(((TestPermission) obj).getName());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public String getActions() {
            return "";
        }
    }
}
