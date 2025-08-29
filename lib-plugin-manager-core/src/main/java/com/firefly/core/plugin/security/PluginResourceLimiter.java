package com.firefly.core.plugin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Limits the resources that plugins can consume.
 * This class monitors and enforces limits on memory usage, CPU usage, thread count, etc.
 */
public class PluginResourceLimiter {

    private static final Logger logger = LoggerFactory.getLogger(PluginResourceLimiter.class);

    /**
     * Resource limits for a plugin.
     */
    public static class ResourceLimits {
        private final long maxMemoryBytes;
        private final int maxThreads;
        private final int maxCpuPercentage;
        private final int maxFileHandles;
        private final int maxNetworkConnections;

        /**
         * Creates a new ResourceLimits with the specified values.
         *
         * @param maxMemoryBytes the maximum memory in bytes
         * @param maxThreads the maximum number of threads
         * @param maxCpuPercentage the maximum CPU percentage (0-100)
         * @param maxFileHandles the maximum number of open file handles
         * @param maxNetworkConnections the maximum number of network connections
         */
        public ResourceLimits(long maxMemoryBytes, int maxThreads, int maxCpuPercentage,
                             int maxFileHandles, int maxNetworkConnections) {
            this.maxMemoryBytes = maxMemoryBytes;
            this.maxThreads = maxThreads;
            this.maxCpuPercentage = maxCpuPercentage;
            this.maxFileHandles = maxFileHandles;
            this.maxNetworkConnections = maxNetworkConnections;
        }

        /**
         * Creates a new ResourceLimits with default values.
         */
        public ResourceLimits() {
            this(256 * 1024 * 1024, 10, 25, 100, 20);
        }

        /**
         * Gets the maximum memory in bytes.
         *
         * @return the maximum memory in bytes
         */
        public long getMaxMemoryBytes() {
            return maxMemoryBytes;
        }

        /**
         * Gets the maximum number of threads.
         *
         * @return the maximum number of threads
         */
        public int getMaxThreads() {
            return maxThreads;
        }

        /**
         * Gets the maximum CPU percentage (0-100).
         *
         * @return the maximum CPU percentage
         */
        public int getMaxCpuPercentage() {
            return maxCpuPercentage;
        }

        /**
         * Gets the maximum number of open file handles.
         *
         * @return the maximum number of open file handles
         */
        public int getMaxFileHandles() {
            return maxFileHandles;
        }

        /**
         * Gets the maximum number of network connections.
         *
         * @return the maximum number of network connections
         */
        public int getMaxNetworkConnections() {
            return maxNetworkConnections;
        }
    }

    /**
     * Resource usage statistics for a plugin.
     */
    public static class ResourceUsage {

        /**
         * Creates a new ResourceUsage instance with default values.
         */
        public ResourceUsage() {
            // Initialize with default values
        }
        private final AtomicLong memoryBytes = new AtomicLong(0);
        private final AtomicLong threadCount = new AtomicLong(0);
        private final AtomicLong cpuTimeNanos = new AtomicLong(0);
        private final AtomicLong fileHandles = new AtomicLong(0);
        private final AtomicLong networkConnections = new AtomicLong(0);
        private long lastCpuTimeNanos = 0;
        private int cpuPercentage = 0;

        /**
         * Gets the current memory usage in bytes.
         *
         * @return the memory usage in bytes
         */
        public long getMemoryBytes() {
            return memoryBytes.get();
        }

        /**
         * Sets the memory usage in bytes.
         *
         * @param bytes the memory usage in bytes
         */
        public void setMemoryBytes(long bytes) {
            memoryBytes.set(bytes);
        }

        /**
         * Gets the current thread count.
         *
         * @return the thread count
         */
        public long getThreadCount() {
            return threadCount.get();
        }

        /**
         * Sets the thread count.
         *
         * @param count the thread count
         */
        public void setThreadCount(long count) {
            threadCount.set(count);
        }

        /**
         * Gets the CPU time in nanoseconds.
         *
         * @return the CPU time in nanoseconds
         */
        public long getCpuTimeNanos() {
            return cpuTimeNanos.get();
        }

        /**
         * Sets the CPU time in nanoseconds.
         *
         * @param nanos the CPU time in nanoseconds
         */
        public void setCpuTimeNanos(long nanos) {
            cpuTimeNanos.set(nanos);
        }

        /**
         * Gets the CPU usage percentage (0-100).
         *
         * @return the CPU usage percentage
         */
        public int getCpuPercentage() {
            return cpuPercentage;
        }

        /**
         * Sets the CPU usage percentage (0-100).
         *
         * @param percentage the CPU usage percentage
         */
        public void setCpuPercentage(int percentage) {
            this.cpuPercentage = percentage;
        }

        /**
         * Gets the number of open file handles.
         *
         * @return the number of open file handles
         */
        public long getFileHandles() {
            return fileHandles.get();
        }

        /**
         * Sets the number of open file handles.
         *
         * @param count the number of open file handles
         */
        public void setFileHandles(long count) {
            fileHandles.set(count);
        }

        /**
         * Gets the number of open network connections.
         *
         * @return the number of open network connections
         */
        public long getNetworkConnections() {
            return networkConnections.get();
        }

        /**
         * Sets the number of open network connections.
         *
         * @param count the number of open network connections
         */
        public void setNetworkConnections(long count) {
            networkConnections.set(count);
        }

        /**
         * Updates the CPU usage percentage based on the current CPU time and interval.
         *
         * @param currentCpuTimeNanos the current CPU time in nanoseconds
         * @param intervalNanos the interval in nanoseconds
         */
        public void updateCpuPercentage(long currentCpuTimeNanos, long intervalNanos) {
            if (lastCpuTimeNanos > 0 && intervalNanos > 0) {
                long cpuTimeDiff = currentCpuTimeNanos - lastCpuTimeNanos;
                cpuPercentage = (int) ((cpuTimeDiff * 100) / intervalNanos);
            }
            lastCpuTimeNanos = currentCpuTimeNanos;
        }
    }

    private final Map<String, ResourceLimits> pluginLimits = new ConcurrentHashMap<>();
    private final Map<String, ResourceUsage> pluginUsage = new ConcurrentHashMap<>();
    private final Map<Thread, String> threadToPluginId = new ConcurrentHashMap<>();
    private final ScheduledExecutorService monitoringExecutor;
    private final ThreadMXBean threadMXBean;
    private final MemoryMXBean memoryMXBean;
    private final boolean enforceLimits;

    /**
     * Creates a new PluginResourceLimiter.
     *
     * @param enforceLimits whether to enforce resource limits
     */
    public PluginResourceLimiter(boolean enforceLimits) {
        this.enforceLimits = enforceLimits;
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "plugin-resource-monitor");
            t.setDaemon(true);
            return t;
        });

        // Start the monitoring task
        if (enforceLimits) {
            monitoringExecutor.scheduleAtFixedRate(this::monitorResources, 1, 1, TimeUnit.SECONDS);
        }

        logger.info("Plugin resource limiter initialized with enforceLimits={}", enforceLimits);
    }

    /**
     * Registers a plugin with the resource limiter.
     *
     * @param pluginId the ID of the plugin
     * @param limits the resource limits for the plugin
     */
    public void registerPlugin(String pluginId, ResourceLimits limits) {
        pluginLimits.put(pluginId, limits);
        pluginUsage.put(pluginId, new ResourceUsage());
        logger.debug("Registered plugin {} with resource limits: maxMemory={}MB, maxThreads={}, maxCpu={}%",
                pluginId, limits.getMaxMemoryBytes() / (1024 * 1024), limits.getMaxThreads(), limits.getMaxCpuPercentage());
    }

    /**
     * Unregisters a plugin from the resource limiter.
     *
     * @param pluginId the ID of the plugin
     */
    public void unregisterPlugin(String pluginId) {
        pluginLimits.remove(pluginId);
        pluginUsage.remove(pluginId);

        // Remove all threads associated with this plugin
        threadToPluginId.entrySet().removeIf(entry -> entry.getValue().equals(pluginId));

        logger.debug("Unregistered plugin {}", pluginId);
    }

    /**
     * Registers a thread with a plugin.
     *
     * @param thread the thread
     * @param pluginId the ID of the plugin
     */
    public void registerThread(Thread thread, String pluginId) {
        threadToPluginId.put(thread, pluginId);

        // Update thread count
        ResourceUsage usage = pluginUsage.get(pluginId);
        if (usage != null) {
            usage.setThreadCount(usage.getThreadCount() + 1);
        }

        logger.debug("Registered thread {} with plugin {}", thread.getName(), pluginId);
    }

    /**
     * Unregisters a thread from a plugin.
     *
     * @param thread the thread
     */
    public void unregisterThread(Thread thread) {
        String pluginId = threadToPluginId.remove(thread);
        if (pluginId != null) {
            // Update thread count
            ResourceUsage usage = pluginUsage.get(pluginId);
            if (usage != null) {
                usage.setThreadCount(usage.getThreadCount() - 1);
            }

            logger.debug("Unregistered thread {} from plugin {}", thread.getName(), pluginId);
        }
    }

    /**
     * Checks if a plugin can allocate the specified amount of memory.
     *
     * @param pluginId the ID of the plugin
     * @param bytes the number of bytes to allocate
     * @return true if the plugin can allocate the memory, false otherwise
     */
    public boolean canAllocateMemory(String pluginId, long bytes) {
        if (!enforceLimits) {
            return true;
        }

        ResourceLimits limits = pluginLimits.get(pluginId);
        ResourceUsage usage = pluginUsage.get(pluginId);

        if (limits == null || usage == null) {
            logger.warn("Plugin {} not registered with resource limiter", pluginId);
            return false;
        }

        long currentMemory = usage.getMemoryBytes();
        long maxMemory = limits.getMaxMemoryBytes();

        boolean canAllocate = (currentMemory + bytes) <= maxMemory;

        if (!canAllocate) {
            logger.warn("Plugin {} exceeded memory limit: current={}, requested={}, max={}",
                    pluginId, currentMemory, bytes, maxMemory);
        }

        return canAllocate;
    }

    /**
     * Checks if a plugin can create a new thread.
     *
     * @param pluginId the ID of the plugin
     * @return true if the plugin can create a thread, false otherwise
     */
    public boolean canCreateThread(String pluginId) {
        if (!enforceLimits) {
            return true;
        }

        ResourceLimits limits = pluginLimits.get(pluginId);
        ResourceUsage usage = pluginUsage.get(pluginId);

        if (limits == null || usage == null) {
            logger.warn("Plugin {} not registered with resource limiter", pluginId);
            return false;
        }

        long currentThreads = usage.getThreadCount();
        int maxThreads = limits.getMaxThreads();

        boolean canCreate = currentThreads < maxThreads;

        if (!canCreate) {
            logger.warn("Plugin {} exceeded thread limit: current={}, max={}",
                    pluginId, currentThreads, maxThreads);
        }

        return canCreate;
    }

    /**
     * Checks if a plugin can open a file.
     *
     * @param pluginId the ID of the plugin
     * @return true if the plugin can open a file, false otherwise
     */
    public boolean canOpenFile(String pluginId) {
        if (!enforceLimits) {
            return true;
        }

        ResourceLimits limits = pluginLimits.get(pluginId);
        ResourceUsage usage = pluginUsage.get(pluginId);

        if (limits == null || usage == null) {
            logger.warn("Plugin {} not registered with resource limiter", pluginId);
            return false;
        }

        long currentFiles = usage.getFileHandles();
        int maxFiles = limits.getMaxFileHandles();

        boolean canOpen = currentFiles < maxFiles;

        if (!canOpen) {
            logger.warn("Plugin {} exceeded file handle limit: current={}, max={}",
                    pluginId, currentFiles, maxFiles);
        }

        return canOpen;
    }

    /**
     * Checks if a plugin can open a network connection.
     *
     * @param pluginId the ID of the plugin
     * @return true if the plugin can open a network connection, false otherwise
     */
    public boolean canOpenNetworkConnection(String pluginId) {
        if (!enforceLimits) {
            return true;
        }

        ResourceLimits limits = pluginLimits.get(pluginId);
        ResourceUsage usage = pluginUsage.get(pluginId);

        if (limits == null || usage == null) {
            logger.warn("Plugin {} not registered with resource limiter", pluginId);
            return false;
        }

        long currentConnections = usage.getNetworkConnections();
        int maxConnections = limits.getMaxNetworkConnections();

        boolean canOpen = currentConnections < maxConnections;

        if (!canOpen) {
            logger.warn("Plugin {} exceeded network connection limit: current={}, max={}",
                    pluginId, currentConnections, maxConnections);
        }

        return canOpen;
    }

    /**
     * Notifies the resource limiter that a plugin has opened a file.
     *
     * @param pluginId the ID of the plugin
     */
    public void fileOpened(String pluginId) {
        ResourceUsage usage = pluginUsage.get(pluginId);
        if (usage != null) {
            usage.setFileHandles(usage.getFileHandles() + 1);
        }
    }

    /**
     * Notifies the resource limiter that a plugin has closed a file.
     *
     * @param pluginId the ID of the plugin
     */
    public void fileClosed(String pluginId) {
        ResourceUsage usage = pluginUsage.get(pluginId);
        if (usage != null) {
            usage.setFileHandles(usage.getFileHandles() - 1);
        }
    }

    /**
     * Notifies the resource limiter that a plugin has opened a network connection.
     *
     * @param pluginId the ID of the plugin
     */
    public void networkConnectionOpened(String pluginId) {
        ResourceUsage usage = pluginUsage.get(pluginId);
        if (usage != null) {
            usage.setNetworkConnections(usage.getNetworkConnections() + 1);
        }
    }

    /**
     * Notifies the resource limiter that a plugin has closed a network connection.
     *
     * @param pluginId the ID of the plugin
     */
    public void networkConnectionClosed(String pluginId) {
        ResourceUsage usage = pluginUsage.get(pluginId);
        if (usage != null) {
            usage.setNetworkConnections(usage.getNetworkConnections() - 1);
        }
    }

    /**
     * Gets the resource usage for a plugin.
     *
     * @param pluginId the ID of the plugin
     * @return the resource usage, or null if the plugin is not registered
     */
    public ResourceUsage getResourceUsage(String pluginId) {
        return pluginUsage.get(pluginId);
    }

    /**
     * Gets the resource limits for a plugin.
     *
     * @param pluginId the ID of the plugin
     * @return the resource limits, or null if the plugin is not registered
     */
    public ResourceLimits getResourceLimits(String pluginId) {
        return pluginLimits.get(pluginId);
    }

    /**
     * Monitors the resource usage of all plugins.
     */
    private void monitorResources() {
        try {
            long monitoringStartTime = System.nanoTime();

            // Update CPU usage for each plugin
            for (Map.Entry<Thread, String> entry : threadToPluginId.entrySet()) {
                Thread thread = entry.getKey();
                String pluginId = entry.getValue();

                if (!thread.isAlive()) {
                    // Thread is no longer alive, remove it
                    unregisterThread(thread);
                    continue;
                }

                ResourceUsage usage = pluginUsage.get(pluginId);
                if (usage != null) {
                    // Get CPU time for the thread
                    long threadId = thread.getId();
                    long cpuTime = threadMXBean.getThreadCpuTime(threadId);

                    if (cpuTime >= 0) {  // -1 means the thread no longer exists
                        // Update total CPU time for the plugin
                        usage.setCpuTimeNanos(cpuTime);
                    }
                }
            }

            // Calculate CPU percentage and check limits
            long monitoringEndTime = System.nanoTime();
            long monitoringInterval = monitoringEndTime - monitoringStartTime;

            for (Map.Entry<String, ResourceUsage> entry : pluginUsage.entrySet()) {
                String pluginId = entry.getKey();
                ResourceUsage usage = entry.getValue();
                ResourceLimits limits = pluginLimits.get(pluginId);

                if (limits != null) {
                    // Update CPU percentage
                    usage.updateCpuPercentage(usage.getCpuTimeNanos(), monitoringInterval);

                    // Check CPU limit
                    if (usage.getCpuPercentage() > limits.getMaxCpuPercentage()) {
                        logger.warn("Plugin {} exceeded CPU limit: current={}%, max={}%",
                                pluginId, usage.getCpuPercentage(), limits.getMaxCpuPercentage());

                        // TODO: Implement CPU throttling or other mitigation
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error monitoring plugin resources", e);
        }
    }

    /**
     * Shuts down the resource limiter.
     */
    public void shutdown() {
        monitoringExecutor.shutdown();
        try {
            if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitoringExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            monitoringExecutor.shutdownNow();
        }

        pluginLimits.clear();
        pluginUsage.clear();
        threadToPluginId.clear();

        logger.info("Plugin resource limiter shut down");
    }
}
