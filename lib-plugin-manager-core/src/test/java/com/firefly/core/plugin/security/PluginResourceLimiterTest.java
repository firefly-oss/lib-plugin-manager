package com.firefly.core.plugin.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PluginResourceLimiterTest {

    private static final String TEST_PLUGIN_ID = "test-plugin";
    private PluginResourceLimiter resourceLimiter;

    @BeforeEach
    void setUp() {
        resourceLimiter = new PluginResourceLimiter(true);
    }

    @AfterEach
    void tearDown() {
        resourceLimiter.shutdown();
    }

    @Test
    void testRegisterAndUnregisterPlugin() {
        // Create resource limits
        PluginResourceLimiter.ResourceLimits limits = new PluginResourceLimiter.ResourceLimits(
                1024 * 1024, // 1MB
                5, // 5 threads
                10, // 10% CPU
                10, // 10 file handles
                5 // 5 network connections
        );

        // Register the plugin
        resourceLimiter.registerPlugin(TEST_PLUGIN_ID, limits);

        // Get the resource usage
        PluginResourceLimiter.ResourceUsage usage = resourceLimiter.getResourceUsage(TEST_PLUGIN_ID);
        assertNotNull(usage);
        assertEquals(0, usage.getMemoryBytes());
        assertEquals(0, usage.getThreadCount());
        assertEquals(0, usage.getCpuTimeNanos());
        assertEquals(0, usage.getCpuPercentage());
        assertEquals(0, usage.getFileHandles());
        assertEquals(0, usage.getNetworkConnections());

        // Get the resource limits
        PluginResourceLimiter.ResourceLimits retrievedLimits = resourceLimiter.getResourceLimits(TEST_PLUGIN_ID);
        assertNotNull(retrievedLimits);
        assertEquals(1024 * 1024, retrievedLimits.getMaxMemoryBytes());
        assertEquals(5, retrievedLimits.getMaxThreads());
        assertEquals(10, retrievedLimits.getMaxCpuPercentage());
        assertEquals(10, retrievedLimits.getMaxFileHandles());
        assertEquals(5, retrievedLimits.getMaxNetworkConnections());

        // Unregister the plugin
        resourceLimiter.unregisterPlugin(TEST_PLUGIN_ID);
        assertNull(resourceLimiter.getResourceUsage(TEST_PLUGIN_ID));
        assertNull(resourceLimiter.getResourceLimits(TEST_PLUGIN_ID));
    }

    @Test
    void testDefaultResourceLimits() {
        // Create default resource limits
        PluginResourceLimiter.ResourceLimits limits = new PluginResourceLimiter.ResourceLimits();

        // Verify default values
        assertEquals(256 * 1024 * 1024, limits.getMaxMemoryBytes()); // 256MB
        assertEquals(10, limits.getMaxThreads());
        assertEquals(25, limits.getMaxCpuPercentage());
        assertEquals(100, limits.getMaxFileHandles());
        assertEquals(20, limits.getMaxNetworkConnections());
    }

    @Test
    void testRegisterAndUnregisterThread() {
        // Create resource limits
        PluginResourceLimiter.ResourceLimits limits = new PluginResourceLimiter.ResourceLimits();

        // Register the plugin
        resourceLimiter.registerPlugin(TEST_PLUGIN_ID, limits);

        // Register a thread
        Thread thread = Thread.currentThread();
        resourceLimiter.registerThread(thread, TEST_PLUGIN_ID);

        // Get the resource usage
        PluginResourceLimiter.ResourceUsage usage = resourceLimiter.getResourceUsage(TEST_PLUGIN_ID);
        assertNotNull(usage);
        assertEquals(1, usage.getThreadCount());

        // Unregister the thread
        resourceLimiter.unregisterThread(thread);

        // Get the resource usage again
        usage = resourceLimiter.getResourceUsage(TEST_PLUGIN_ID);
        assertNotNull(usage);
        assertEquals(0, usage.getThreadCount());

        // Unregister the plugin
        resourceLimiter.unregisterPlugin(TEST_PLUGIN_ID);
    }

    @Test
    void testCanAllocateMemory() {
        // Create resource limits
        PluginResourceLimiter.ResourceLimits limits = new PluginResourceLimiter.ResourceLimits(
                1024 * 1024, // 1MB
                5, // 5 threads
                10, // 10% CPU
                10, // 10 file handles
                5 // 5 network connections
        );

        // Register the plugin
        resourceLimiter.registerPlugin(TEST_PLUGIN_ID, limits);

        // Check if the plugin can allocate memory
        assertTrue(resourceLimiter.canAllocateMemory(TEST_PLUGIN_ID, 512 * 1024)); // 512KB
        assertFalse(resourceLimiter.canAllocateMemory(TEST_PLUGIN_ID, 2 * 1024 * 1024)); // 2MB

        // Unregister the plugin
        resourceLimiter.unregisterPlugin(TEST_PLUGIN_ID);
    }

    @Test
    void testCanCreateThread() {
        // Create resource limits
        PluginResourceLimiter.ResourceLimits limits = new PluginResourceLimiter.ResourceLimits(
                1024 * 1024, // 1MB
                2, // 2 threads
                10, // 10% CPU
                10, // 10 file handles
                5 // 5 network connections
        );

        // Register the plugin
        resourceLimiter.registerPlugin(TEST_PLUGIN_ID, limits);

        // Register threads
        Thread thread1 = new Thread();
        Thread thread2 = new Thread();
        resourceLimiter.registerThread(thread1, TEST_PLUGIN_ID);
        resourceLimiter.registerThread(thread2, TEST_PLUGIN_ID);

        // Check if the plugin can create more threads
        assertFalse(resourceLimiter.canCreateThread(TEST_PLUGIN_ID));

        // Unregister a thread
        resourceLimiter.unregisterThread(thread1);

        // Check again
        assertTrue(resourceLimiter.canCreateThread(TEST_PLUGIN_ID));

        // Unregister the remaining thread
        resourceLimiter.unregisterThread(thread2);

        // Unregister the plugin
        resourceLimiter.unregisterPlugin(TEST_PLUGIN_ID);
    }

    @Test
    void testFileHandles() {
        // Create resource limits
        PluginResourceLimiter.ResourceLimits limits = new PluginResourceLimiter.ResourceLimits(
                1024 * 1024, // 1MB
                5, // 5 threads
                10, // 10% CPU
                2, // 2 file handles
                5 // 5 network connections
        );

        // Register the plugin
        resourceLimiter.registerPlugin(TEST_PLUGIN_ID, limits);

        // Open files
        resourceLimiter.fileOpened(TEST_PLUGIN_ID);
        resourceLimiter.fileOpened(TEST_PLUGIN_ID);

        // Check if the plugin can open more files
        assertFalse(resourceLimiter.canOpenFile(TEST_PLUGIN_ID));

        // Close a file
        resourceLimiter.fileClosed(TEST_PLUGIN_ID);

        // Check again
        assertTrue(resourceLimiter.canOpenFile(TEST_PLUGIN_ID));

        // Close the remaining file
        resourceLimiter.fileClosed(TEST_PLUGIN_ID);

        // Unregister the plugin
        resourceLimiter.unregisterPlugin(TEST_PLUGIN_ID);
    }

    @Test
    void testNetworkConnections() {
        // Create resource limits
        PluginResourceLimiter.ResourceLimits limits = new PluginResourceLimiter.ResourceLimits(
                1024 * 1024, // 1MB
                5, // 5 threads
                10, // 10% CPU
                10, // 10 file handles
                2 // 2 network connections
        );

        // Register the plugin
        resourceLimiter.registerPlugin(TEST_PLUGIN_ID, limits);

        // Open network connections
        resourceLimiter.networkConnectionOpened(TEST_PLUGIN_ID);
        resourceLimiter.networkConnectionOpened(TEST_PLUGIN_ID);

        // Check if the plugin can open more network connections
        assertFalse(resourceLimiter.canOpenNetworkConnection(TEST_PLUGIN_ID));

        // Close a network connection
        resourceLimiter.networkConnectionClosed(TEST_PLUGIN_ID);

        // Check again
        assertTrue(resourceLimiter.canOpenNetworkConnection(TEST_PLUGIN_ID));

        // Close the remaining network connection
        resourceLimiter.networkConnectionClosed(TEST_PLUGIN_ID);

        // Unregister the plugin
        resourceLimiter.unregisterPlugin(TEST_PLUGIN_ID);
    }

    @Test
    void testResourceUsage() {
        // Create a resource usage object
        PluginResourceLimiter.ResourceUsage usage = new PluginResourceLimiter.ResourceUsage();

        // Set values
        usage.setMemoryBytes(1024 * 1024);
        usage.setThreadCount(5);
        usage.setCpuTimeNanos(1000000);
        usage.setCpuPercentage(10);
        usage.setFileHandles(10);
        usage.setNetworkConnections(5);

        // Get values
        assertEquals(1024 * 1024, usage.getMemoryBytes());
        assertEquals(5, usage.getThreadCount());
        assertEquals(1000000, usage.getCpuTimeNanos());
        assertEquals(10, usage.getCpuPercentage());
        assertEquals(10, usage.getFileHandles());
        assertEquals(5, usage.getNetworkConnections());

        // Update CPU percentage
        usage.updateCpuPercentage(2000000, 10000000);
        assertEquals(10, usage.getCpuPercentage());
    }

    @Test
    void testResourceLimiterWithEnforcementDisabled() {
        // Create a resource limiter with enforcement disabled
        PluginResourceLimiter limiter = new PluginResourceLimiter(false);

        // Create resource limits
        PluginResourceLimiter.ResourceLimits limits = new PluginResourceLimiter.ResourceLimits(
                1024, // 1KB (very small)
                1, // 1 thread (very small)
                1, // 1% CPU (very small)
                1, // 1 file handle (very small)
                1 // 1 network connection (very small)
        );

        // Register the plugin
        limiter.registerPlugin(TEST_PLUGIN_ID, limits);

        // These should all return true because enforcement is disabled
        assertTrue(limiter.canAllocateMemory(TEST_PLUGIN_ID, 1024 * 1024)); // 1MB
        assertTrue(limiter.canCreateThread(TEST_PLUGIN_ID));
        assertTrue(limiter.canOpenFile(TEST_PLUGIN_ID));
        assertTrue(limiter.canOpenNetworkConnection(TEST_PLUGIN_ID));

        // Unregister the plugin
        limiter.unregisterPlugin(TEST_PLUGIN_ID);

        // Shut down the limiter
        limiter.shutdown();
    }
}
