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


package com.firefly.core.plugin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the plugin manager.
 * This class defines the configuration properties that can be set in application.properties or application.yml.
 */
@ConfigurationProperties(prefix = "firefly.plugin-manager")
public class PluginManagerProperties {

    /**
     * Creates a new PluginManagerProperties instance with default values.
     */
    public PluginManagerProperties() {
        // Initialize with default values
    }

    /**
     * The directory where plugins are stored.
     */
    private Path pluginsDirectory = Path.of("plugins");

    /**
     * Whether to automatically start plugins when they are loaded.
     */
    private boolean autoStartPlugins = true;

    /**
     * Whether to scan for plugins on startup.
     */
    private boolean scanOnStartup = true;

    /**
     * List of plugin IDs that should be loaded on startup.
     */
    private List<String> autoLoadPlugins = new ArrayList<>();

    /**
     * List of packages that are allowed to be accessed by plugins.
     */
    private List<String> allowedPackages = new ArrayList<>();

    /**
     * Event bus configuration.
     */
    private EventBusProperties eventBus = new EventBusProperties();

    /**
     * Security configuration.
     */
    private SecurityProperties security = new SecurityProperties();

    /**
     * Resource limits configuration.
     */
    private ResourceProperties resources = new ResourceProperties();

    /**
     * Hot deployment configuration.
     */
    private HotDeploymentProperties hotDeployment = new HotDeploymentProperties();

    /**
     * Health monitoring configuration.
     */
    private HealthProperties health = new HealthProperties();

    /**
     * Git repository configuration.
     */
    private GitProperties git = new GitProperties();

    /**
     * Debugger configuration.
     */
    private DebuggerProperties debugger = new DebuggerProperties();

    /**
     * Configuration properties for the event bus.
     */
    public static class EventBusProperties {

        /**
         * Creates a new EventBusProperties instance with default values.
         */
        public EventBusProperties() {
            // Initialize with default values
        }

        /**
         * The type of event bus to use (in-memory, kafka).
         * Default is "in-memory". Set to "kafka" only if you have Kafka dependencies
         * and a running Kafka broker.
         */
        private String type = "in-memory";

        /**
         * Whether to enable distributed events.
         */
        private boolean distributedEvents = false;

        /**
         * Kafka-specific configuration properties.
         */
        private KafkaProperties kafka = new KafkaProperties();

        /**
         * Gets the event bus type.
         *
         * @return the event bus type
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the event bus type.
         *
         * @param type the event bus type
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Gets whether distributed events are enabled.
         *
         * @return true if distributed events are enabled, false otherwise
         */
        public boolean isDistributedEvents() {
            return distributedEvents;
        }

        /**
         * Sets whether distributed events are enabled.
         *
         * @param distributedEvents true if distributed events are enabled, false otherwise
         */
        public void setDistributedEvents(boolean distributedEvents) {
            this.distributedEvents = distributedEvents;
        }

        /**
         * Gets the Kafka properties.
         *
         * @return the Kafka properties
         */
        public KafkaProperties getKafka() {
            return kafka;
        }

        /**
         * Sets the Kafka properties.
         *
         * @param kafka the Kafka properties
         */
        public void setKafka(KafkaProperties kafka) {
            this.kafka = kafka;
        }
    }

    /**
     * Configuration properties for Kafka.
     * These properties are only used if the event bus type is set to "kafka".
     * Kafka dependencies must be on the classpath and a Kafka broker must be running.
     */
    public static class KafkaProperties {

        /**
         * Creates a new KafkaProperties instance with default values.
         */
        public KafkaProperties() {
            // Initialize with default values
        }

        /**
         * Comma-separated list of Kafka bootstrap servers.
         */
        private String bootstrapServers = "localhost:9092";

        /**
         * Consumer group ID for Kafka consumers.
         */
        private String consumerGroupId = "firefly-plugin-manager";

        /**
         * Default topic for plugin events.
         */
        private String defaultTopic = "firefly-plugin-events";

        /**
         * Whether to auto-create topics if they don't exist.
         */
        private boolean autoCreateTopics = true;

        /**
         * Number of partitions for auto-created topics.
         */
        private int numPartitions = 3;

        /**
         * Replication factor for auto-created topics.
         */
        private short replicationFactor = 1;

        /**
         * Gets the bootstrap servers.
         *
         * @return the bootstrap servers
         */
        public String getBootstrapServers() {
            return bootstrapServers;
        }

        /**
         * Sets the bootstrap servers.
         *
         * @param bootstrapServers the bootstrap servers
         */
        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        /**
         * Gets the consumer group ID.
         *
         * @return the consumer group ID
         */
        public String getConsumerGroupId() {
            return consumerGroupId;
        }

        /**
         * Sets the consumer group ID.
         *
         * @param consumerGroupId the consumer group ID
         */
        public void setConsumerGroupId(String consumerGroupId) {
            this.consumerGroupId = consumerGroupId;
        }

        /**
         * Gets the default topic.
         *
         * @return the default topic
         */
        public String getDefaultTopic() {
            return defaultTopic;
        }

        /**
         * Sets the default topic.
         *
         * @param defaultTopic the default topic
         */
        public void setDefaultTopic(String defaultTopic) {
            this.defaultTopic = defaultTopic;
        }

        /**
         * Gets whether to auto-create topics.
         *
         * @return whether to auto-create topics
         */
        public boolean isAutoCreateTopics() {
            return autoCreateTopics;
        }

        /**
         * Sets whether to auto-create topics.
         *
         * @param autoCreateTopics whether to auto-create topics
         */
        public void setAutoCreateTopics(boolean autoCreateTopics) {
            this.autoCreateTopics = autoCreateTopics;
        }

        /**
         * Gets the number of partitions for auto-created topics.
         *
         * @return the number of partitions
         */
        public int getNumPartitions() {
            return numPartitions;
        }

        /**
         * Sets the number of partitions for auto-created topics.
         *
         * @param numPartitions the number of partitions
         */
        public void setNumPartitions(int numPartitions) {
            this.numPartitions = numPartitions;
        }

        /**
         * Gets the replication factor for auto-created topics.
         *
         * @return the replication factor
         */
        public short getReplicationFactor() {
            return replicationFactor;
        }

        /**
         * Sets the replication factor for auto-created topics.
         *
         * @param replicationFactor the replication factor
         */
        public void setReplicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
        }
    }

    /**
     * Gets the plugins directory.
     *
     * @return the plugins directory
     */
    public Path getPluginsDirectory() {
        return pluginsDirectory;
    }

    /**
     * Sets the plugins directory.
     *
     * @param pluginsDirectory the plugins directory
     */
    public void setPluginsDirectory(Path pluginsDirectory) {
        this.pluginsDirectory = pluginsDirectory;
    }

    /**
     * Gets whether to automatically start plugins when they are loaded.
     *
     * @return true if plugins should be automatically started, false otherwise
     */
    public boolean isAutoStartPlugins() {
        return autoStartPlugins;
    }

    /**
     * Sets whether to automatically start plugins when they are loaded.
     *
     * @param autoStartPlugins true if plugins should be automatically started, false otherwise
     */
    public void setAutoStartPlugins(boolean autoStartPlugins) {
        this.autoStartPlugins = autoStartPlugins;
    }

    /**
     * Gets whether to scan for plugins on startup.
     *
     * @return true if plugins should be scanned on startup, false otherwise
     */
    public boolean isScanOnStartup() {
        return scanOnStartup;
    }

    /**
     * Sets whether to scan for plugins on startup.
     *
     * @param scanOnStartup true if plugins should be scanned on startup, false otherwise
     */
    public void setScanOnStartup(boolean scanOnStartup) {
        this.scanOnStartup = scanOnStartup;
    }

    /**
     * Gets the list of plugin IDs that should be loaded on startup.
     *
     * @return the list of plugin IDs
     */
    public List<String> getAutoLoadPlugins() {
        return autoLoadPlugins;
    }

    /**
     * Sets the list of plugin IDs that should be loaded on startup.
     *
     * @param autoLoadPlugins the list of plugin IDs
     */
    public void setAutoLoadPlugins(List<String> autoLoadPlugins) {
        this.autoLoadPlugins = autoLoadPlugins;
    }

    /**
     * Gets the list of packages that are allowed to be accessed by plugins.
     *
     * @return the list of allowed packages
     */
    public List<String> getAllowedPackages() {
        return allowedPackages;
    }

    /**
     * Sets the list of packages that are allowed to be accessed by plugins.
     *
     * @param allowedPackages the list of allowed packages
     */
    public void setAllowedPackages(List<String> allowedPackages) {
        this.allowedPackages = allowedPackages;
    }

    /**
     * Gets the event bus properties.
     *
     * @return the event bus properties
     */
    public EventBusProperties getEventBus() {
        return eventBus;
    }

    /**
     * Sets the event bus properties.
     *
     * @param eventBus the event bus properties
     */
    public void setEventBus(EventBusProperties eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Gets the security properties.
     *
     * @return the security properties
     */
    public SecurityProperties getSecurity() {
        return security;
    }

    /**
     * Sets the security properties.
     *
     * @param security the security properties
     */
    public void setSecurity(SecurityProperties security) {
        this.security = security;
    }

    /**
     * Gets the resource properties.
     *
     * @return the resource properties
     */
    public ResourceProperties getResources() {
        return resources;
    }

    /**
     * Sets the resource properties.
     *
     * @param resources the resource properties
     */
    public void setResources(ResourceProperties resources) {
        this.resources = resources;
    }

    /**
     * Gets the hot deployment properties.
     *
     * @return the hot deployment properties
     */
    public HotDeploymentProperties getHotDeployment() {
        return hotDeployment;
    }

    /**
     * Sets the hot deployment properties.
     *
     * @param hotDeployment the hot deployment properties
     */
    public void setHotDeployment(HotDeploymentProperties hotDeployment) {
        this.hotDeployment = hotDeployment;
    }

    /**
     * Gets the health properties.
     *
     * @return the health properties
     */
    public HealthProperties getHealth() {
        return health;
    }

    /**
     * Sets the health properties.
     *
     * @param health the health properties
     */
    public void setHealth(HealthProperties health) {
        this.health = health;
    }

    /**
     * Gets the debugger properties.
     *
     * @return the debugger properties
     */
    public DebuggerProperties getDebugger() {
        return debugger;
    }

    /**
     * Sets the debugger properties.
     *
     * @param debugger the debugger properties
     */
    public void setDebugger(DebuggerProperties debugger) {
        this.debugger = debugger;
    }

    /**
     * Gets the Git properties.
     *
     * @return the Git properties
     */
    public GitProperties getGit() {
        return git;
    }

    /**
     * Sets the Git properties.
     *
     * @param git the Git properties
     */
    public void setGit(GitProperties git) {
        this.git = git;
    }

    /**
     * Configuration properties for security.
     */
    public static class SecurityProperties {

        /**
         * Creates a new SecurityProperties instance with default values.
         */
        public SecurityProperties() {
            // Initialize with default values
        }

        /**
         * Whether to enforce security checks.
         */
        private boolean enforceSecurityChecks = true;

        /**
         * Whether to allow file system access.
         */
        private boolean allowFileAccess = false;

        /**
         * Whether to allow network access.
         */
        private boolean allowNetworkAccess = true;

        /**
         * List of allowed hosts for network access.
         */
        private List<String> allowedHosts = new ArrayList<>();

        /**
         * List of allowed directories for file access.
         */
        private List<String> allowedDirectories = new ArrayList<>();

        /**
         * Whether to require plugins to be signed.
         */
        private boolean requireSignature = false;

        /**
         * Path to the directory containing trusted certificates.
         */
        private Path trustedCertificatesDirectory = Path.of("certificates");

        /**
         * Plugin-specific security settings.
         */
        private Map<String, PluginSecurityProperties> plugins = new HashMap<>();

        /**
         * Gets whether to enforce security checks.
         *
         * @return whether to enforce security checks
         */
        public boolean isEnforceSecurityChecks() {
            return enforceSecurityChecks;
        }

        /**
         * Sets whether to enforce security checks.
         *
         * @param enforceSecurityChecks whether to enforce security checks
         */
        public void setEnforceSecurityChecks(boolean enforceSecurityChecks) {
            this.enforceSecurityChecks = enforceSecurityChecks;
        }

        /**
         * Gets whether to allow file system access.
         *
         * @return whether to allow file system access
         */
        public boolean isAllowFileAccess() {
            return allowFileAccess;
        }

        /**
         * Sets whether to allow file system access.
         *
         * @param allowFileAccess whether to allow file system access
         */
        public void setAllowFileAccess(boolean allowFileAccess) {
            this.allowFileAccess = allowFileAccess;
        }

        /**
         * Gets whether to allow network access.
         *
         * @return whether to allow network access
         */
        public boolean isAllowNetworkAccess() {
            return allowNetworkAccess;
        }

        /**
         * Sets whether to allow network access.
         *
         * @param allowNetworkAccess whether to allow network access
         */
        public void setAllowNetworkAccess(boolean allowNetworkAccess) {
            this.allowNetworkAccess = allowNetworkAccess;
        }

        /**
         * Gets the list of allowed hosts for network access.
         *
         * @return the list of allowed hosts
         */
        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        /**
         * Sets the list of allowed hosts for network access.
         *
         * @param allowedHosts the list of allowed hosts
         */
        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts;
        }

        /**
         * Gets the list of allowed directories for file access.
         *
         * @return the list of allowed directories
         */
        public List<String> getAllowedDirectories() {
            return allowedDirectories;
        }

        /**
         * Sets the list of allowed directories for file access.
         *
         * @param allowedDirectories the list of allowed directories
         */
        public void setAllowedDirectories(List<String> allowedDirectories) {
            this.allowedDirectories = allowedDirectories;
        }

        /**
         * Gets whether to require plugins to be signed.
         *
         * @return whether to require plugins to be signed
         */
        public boolean isRequireSignature() {
            return requireSignature;
        }

        /**
         * Sets whether to require plugins to be signed.
         *
         * @param requireSignature whether to require plugins to be signed
         */
        public void setRequireSignature(boolean requireSignature) {
            this.requireSignature = requireSignature;
        }

        /**
         * Gets the path to the directory containing trusted certificates.
         *
         * @return the path to the trusted certificates directory
         */
        public Path getTrustedCertificatesDirectory() {
            return trustedCertificatesDirectory;
        }

        /**
         * Sets the path to the directory containing trusted certificates.
         *
         * @param trustedCertificatesDirectory the path to the trusted certificates directory
         */
        public void setTrustedCertificatesDirectory(Path trustedCertificatesDirectory) {
            this.trustedCertificatesDirectory = trustedCertificatesDirectory;
        }

        /**
         * Gets the plugin-specific security settings.
         *
         * @return the plugin-specific security settings
         */
        public Map<String, PluginSecurityProperties> getPlugins() {
            return plugins;
        }

        /**
         * Sets the plugin-specific security settings.
         *
         * @param plugins the plugin-specific security settings
         */
        public void setPlugins(Map<String, PluginSecurityProperties> plugins) {
            this.plugins = plugins;
        }
    }

    /**
     * Configuration properties for plugin-specific security settings.
     */
    public static class PluginSecurityProperties {

        /**
         * Creates a new PluginSecurityProperties instance with default values.
         */
        public PluginSecurityProperties() {
            // Initialize with default values
        }

        /**
         * Whether to allow file system access.
         */
        private Boolean allowFileAccess;

        /**
         * Whether to allow network access.
         */
        private Boolean allowNetworkAccess;

        /**
         * List of allowed hosts for network access.
         */
        private List<String> allowedHosts = new ArrayList<>();

        /**
         * List of allowed directories for file access.
         */
        private List<String> allowedDirectories = new ArrayList<>();

        /**
         * Gets whether to allow file system access.
         *
         * @return whether to allow file system access
         */
        public Boolean getAllowFileAccess() {
            return allowFileAccess;
        }

        /**
         * Sets whether to allow file system access.
         *
         * @param allowFileAccess whether to allow file system access
         */
        public void setAllowFileAccess(Boolean allowFileAccess) {
            this.allowFileAccess = allowFileAccess;
        }

        /**
         * Gets whether to allow network access.
         *
         * @return whether to allow network access
         */
        public Boolean getAllowNetworkAccess() {
            return allowNetworkAccess;
        }

        /**
         * Sets whether to allow network access.
         *
         * @param allowNetworkAccess whether to allow network access
         */
        public void setAllowNetworkAccess(Boolean allowNetworkAccess) {
            this.allowNetworkAccess = allowNetworkAccess;
        }

        /**
         * Gets the list of allowed hosts for network access.
         *
         * @return the list of allowed hosts
         */
        public List<String> getAllowedHosts() {
            return allowedHosts;
        }

        /**
         * Sets the list of allowed hosts for network access.
         *
         * @param allowedHosts the list of allowed hosts
         */
        public void setAllowedHosts(List<String> allowedHosts) {
            this.allowedHosts = allowedHosts;
        }

        /**
         * Gets the list of allowed directories for file access.
         *
         * @return the list of allowed directories
         */
        public List<String> getAllowedDirectories() {
            return allowedDirectories;
        }

        /**
         * Sets the list of allowed directories for file access.
         *
         * @param allowedDirectories the list of allowed directories
         */
        public void setAllowedDirectories(List<String> allowedDirectories) {
            this.allowedDirectories = allowedDirectories;
        }
    }

    /**
     * Configuration properties for resource limits.
     */
    public static class ResourceProperties {

        /**
         * Creates a new ResourceProperties instance with default values.
         */
        public ResourceProperties() {
            // Initialize with default values
        }

        /**
         * Whether to enforce resource limits.
         */
        private boolean enforceResourceLimits = true;

        /**
         * Maximum memory usage in megabytes.
         */
        private int maxMemoryMb = 256;

        /**
         * Maximum CPU usage percentage (0-100).
         */
        private int maxCpuPercentage = 25;

        /**
         * Maximum number of threads.
         */
        private int maxThreads = 10;

        /**
         * Maximum number of open file handles.
         */
        private int maxFileHandles = 100;

        /**
         * Maximum number of network connections.
         */
        private int maxNetworkConnections = 20;

        /**
         * Plugin-specific resource limits.
         */
        private Map<String, PluginResourceProperties> plugins = new HashMap<>();

        /**
         * Gets whether to enforce resource limits.
         *
         * @return whether to enforce resource limits
         */
        public boolean isEnforceResourceLimits() {
            return enforceResourceLimits;
        }

        /**
         * Sets whether to enforce resource limits.
         *
         * @param enforceResourceLimits whether to enforce resource limits
         */
        public void setEnforceResourceLimits(boolean enforceResourceLimits) {
            this.enforceResourceLimits = enforceResourceLimits;
        }

        /**
         * Gets the maximum memory usage in megabytes.
         *
         * @return the maximum memory usage in megabytes
         */
        public int getMaxMemoryMb() {
            return maxMemoryMb;
        }

        /**
         * Sets the maximum memory usage in megabytes.
         *
         * @param maxMemoryMb the maximum memory usage in megabytes
         */
        public void setMaxMemoryMb(int maxMemoryMb) {
            this.maxMemoryMb = maxMemoryMb;
        }

        /**
         * Gets the maximum CPU usage percentage.
         *
         * @return the maximum CPU usage percentage
         */
        public int getMaxCpuPercentage() {
            return maxCpuPercentage;
        }

        /**
         * Sets the maximum CPU usage percentage.
         *
         * @param maxCpuPercentage the maximum CPU usage percentage
         */
        public void setMaxCpuPercentage(int maxCpuPercentage) {
            this.maxCpuPercentage = maxCpuPercentage;
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
         * Sets the maximum number of threads.
         *
         * @param maxThreads the maximum number of threads
         */
        public void setMaxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
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
         * Sets the maximum number of open file handles.
         *
         * @param maxFileHandles the maximum number of open file handles
         */
        public void setMaxFileHandles(int maxFileHandles) {
            this.maxFileHandles = maxFileHandles;
        }

        /**
         * Gets the maximum number of network connections.
         *
         * @return the maximum number of network connections
         */
        public int getMaxNetworkConnections() {
            return maxNetworkConnections;
        }

        /**
         * Sets the maximum number of network connections.
         *
         * @param maxNetworkConnections the maximum number of network connections
         */
        public void setMaxNetworkConnections(int maxNetworkConnections) {
            this.maxNetworkConnections = maxNetworkConnections;
        }

        /**
         * Gets the plugin-specific resource limits.
         *
         * @return the plugin-specific resource limits
         */
        public Map<String, PluginResourceProperties> getPlugins() {
            return plugins;
        }

        /**
         * Sets the plugin-specific resource limits.
         *
         * @param plugins the plugin-specific resource limits
         */
        public void setPlugins(Map<String, PluginResourceProperties> plugins) {
            this.plugins = plugins;
        }
    }

    /**
     * Configuration properties for plugin-specific resource limits.
     */
    public static class PluginResourceProperties {

        /**
         * Creates a new PluginResourceProperties instance with default values.
         */
        public PluginResourceProperties() {
            // Initialize with default values
        }

        /**
         * Maximum memory usage in megabytes.
         */
        private Integer maxMemoryMb;

        /**
         * Maximum CPU usage percentage (0-100).
         */
        private Integer maxCpuPercentage;

        /**
         * Maximum number of threads.
         */
        private Integer maxThreads;

        /**
         * Maximum number of open file handles.
         */
        private Integer maxFileHandles;

        /**
         * Maximum number of network connections.
         */
        private Integer maxNetworkConnections;

        /**
         * Gets the maximum memory usage in megabytes.
         *
         * @return the maximum memory usage in megabytes
         */
        public Integer getMaxMemoryMb() {
            return maxMemoryMb;
        }

        /**
         * Sets the maximum memory usage in megabytes.
         *
         * @param maxMemoryMb the maximum memory usage in megabytes
         */
        public void setMaxMemoryMb(Integer maxMemoryMb) {
            this.maxMemoryMb = maxMemoryMb;
        }

        /**
         * Gets the maximum CPU usage percentage.
         *
         * @return the maximum CPU usage percentage
         */
        public Integer getMaxCpuPercentage() {
            return maxCpuPercentage;
        }

        /**
         * Sets the maximum CPU usage percentage.
         *
         * @param maxCpuPercentage the maximum CPU usage percentage
         */
        public void setMaxCpuPercentage(Integer maxCpuPercentage) {
            this.maxCpuPercentage = maxCpuPercentage;
        }

        /**
         * Gets the maximum number of threads.
         *
         * @return the maximum number of threads
         */
        public Integer getMaxThreads() {
            return maxThreads;
        }

        /**
         * Sets the maximum number of threads.
         *
         * @param maxThreads the maximum number of threads
         */
        public void setMaxThreads(Integer maxThreads) {
            this.maxThreads = maxThreads;
        }

        /**
         * Gets the maximum number of open file handles.
         *
         * @return the maximum number of open file handles
         */
        public Integer getMaxFileHandles() {
            return maxFileHandles;
        }

        /**
         * Sets the maximum number of open file handles.
         *
         * @param maxFileHandles the maximum number of open file handles
         */
        public void setMaxFileHandles(Integer maxFileHandles) {
            this.maxFileHandles = maxFileHandles;
        }

        /**
         * Gets the maximum number of network connections.
         *
         * @return the maximum number of network connections
         */
        public Integer getMaxNetworkConnections() {
            return maxNetworkConnections;
        }

        /**
         * Sets the maximum number of network connections.
         *
         * @param maxNetworkConnections the maximum number of network connections
         */
        public void setMaxNetworkConnections(Integer maxNetworkConnections) {
            this.maxNetworkConnections = maxNetworkConnections;
        }
    }

    /**
     * Configuration properties for hot deployment.
     */
    public static class HotDeploymentProperties {

        /**
         * Creates a new HotDeploymentProperties instance with default values.
         */
        public HotDeploymentProperties() {
            // Initialize with default values
        }

        /**
         * Whether to enable hot deployment.
         */
        private boolean enabled = false;

        /**
         * The polling interval in milliseconds for checking for plugin changes.
         */
        private long pollingIntervalMs = 5000;

        /**
         * Whether to automatically reload plugins when they change.
         */
        private boolean autoReload = true;

        /**
         * Whether to watch for new plugins in the plugins directory.
         */
        private boolean watchForNewPlugins = true;

        /**
         * Whether to watch for plugin updates in the plugins directory.
         */
        private boolean watchForPluginUpdates = true;

        /**
         * Whether to watch for plugin deletions in the plugins directory.
         */
        private boolean watchForPluginDeletions = false;

        /**
         * Gets whether hot deployment is enabled.
         *
         * @return whether hot deployment is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether hot deployment is enabled.
         *
         * @param enabled whether hot deployment is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the polling interval in milliseconds.
         *
         * @return the polling interval in milliseconds
         */
        public long getPollingIntervalMs() {
            return pollingIntervalMs;
        }

        /**
         * Sets the polling interval in milliseconds.
         *
         * @param pollingIntervalMs the polling interval in milliseconds
         */
        public void setPollingIntervalMs(long pollingIntervalMs) {
            this.pollingIntervalMs = pollingIntervalMs;
        }

        /**
         * Gets whether to automatically reload plugins when they change.
         *
         * @return whether to automatically reload plugins
         */
        public boolean isAutoReload() {
            return autoReload;
        }

        /**
         * Sets whether to automatically reload plugins when they change.
         *
         * @param autoReload whether to automatically reload plugins
         */
        public void setAutoReload(boolean autoReload) {
            this.autoReload = autoReload;
        }

        /**
         * Gets whether to watch for new plugins.
         *
         * @return whether to watch for new plugins
         */
        public boolean isWatchForNewPlugins() {
            return watchForNewPlugins;
        }

        /**
         * Sets whether to watch for new plugins.
         *
         * @param watchForNewPlugins whether to watch for new plugins
         */
        public void setWatchForNewPlugins(boolean watchForNewPlugins) {
            this.watchForNewPlugins = watchForNewPlugins;
        }

        /**
         * Gets whether to watch for plugin updates.
         *
         * @return whether to watch for plugin updates
         */
        public boolean isWatchForPluginUpdates() {
            return watchForPluginUpdates;
        }

        /**
         * Sets whether to watch for plugin updates.
         *
         * @param watchForPluginUpdates whether to watch for plugin updates
         */
        public void setWatchForPluginUpdates(boolean watchForPluginUpdates) {
            this.watchForPluginUpdates = watchForPluginUpdates;
        }

        /**
         * Gets whether to watch for plugin deletions.
         *
         * @return whether to watch for plugin deletions
         */
        public boolean isWatchForPluginDeletions() {
            return watchForPluginDeletions;
        }

        /**
         * Sets whether to watch for plugin deletions.
         *
         * @param watchForPluginDeletions whether to watch for plugin deletions
         */
        public void setWatchForPluginDeletions(boolean watchForPluginDeletions) {
            this.watchForPluginDeletions = watchForPluginDeletions;
        }
    }

    /**
     * Configuration properties for health monitoring.
     */
    public static class HealthProperties {

        /**
         * Creates a new HealthProperties instance with default values.
         */
        public HealthProperties() {
            // Initialize with default values
        }

        /**
         * Whether to enable health monitoring.
         */
        private boolean enabled = true;

        /**
         * The monitoring interval in milliseconds.
         */
        private long monitoringIntervalMs = 60000; // 1 minute

        /**
         * Whether to enable automatic recovery of failed plugins.
         */
        private boolean autoRecoveryEnabled = true;

        /**
         * The maximum number of recovery attempts before giving up.
         */
        private int maxRecoveryAttempts = 3;

        /**
         * The delay in milliseconds between recovery attempts.
         */
        private long recoveryDelayMs = 5000; // 5 seconds

        /**
         * Plugin-specific health settings.
         */
        private Map<String, PluginHealthProperties> plugins = new HashMap<>();

        /**
         * Gets whether health monitoring is enabled.
         *
         * @return whether health monitoring is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether health monitoring is enabled.
         *
         * @param enabled whether health monitoring is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the monitoring interval in milliseconds.
         *
         * @return the monitoring interval in milliseconds
         */
        public long getMonitoringIntervalMs() {
            return monitoringIntervalMs;
        }

        /**
         * Sets the monitoring interval in milliseconds.
         *
         * @param monitoringIntervalMs the monitoring interval in milliseconds
         */
        public void setMonitoringIntervalMs(long monitoringIntervalMs) {
            this.monitoringIntervalMs = monitoringIntervalMs;
        }

        /**
         * Gets whether automatic recovery is enabled.
         *
         * @return whether automatic recovery is enabled
         */
        public boolean isAutoRecoveryEnabled() {
            return autoRecoveryEnabled;
        }

        /**
         * Sets whether automatic recovery is enabled.
         *
         * @param autoRecoveryEnabled whether automatic recovery is enabled
         */
        public void setAutoRecoveryEnabled(boolean autoRecoveryEnabled) {
            this.autoRecoveryEnabled = autoRecoveryEnabled;
        }

        /**
         * Gets the maximum number of recovery attempts.
         *
         * @return the maximum number of recovery attempts
         */
        public int getMaxRecoveryAttempts() {
            return maxRecoveryAttempts;
        }

        /**
         * Sets the maximum number of recovery attempts.
         *
         * @param maxRecoveryAttempts the maximum number of recovery attempts
         */
        public void setMaxRecoveryAttempts(int maxRecoveryAttempts) {
            this.maxRecoveryAttempts = maxRecoveryAttempts;
        }

        /**
         * Gets the recovery delay in milliseconds.
         *
         * @return the recovery delay in milliseconds
         */
        public long getRecoveryDelayMs() {
            return recoveryDelayMs;
        }

        /**
         * Sets the recovery delay in milliseconds.
         *
         * @param recoveryDelayMs the recovery delay in milliseconds
         */
        public void setRecoveryDelayMs(long recoveryDelayMs) {
            this.recoveryDelayMs = recoveryDelayMs;
        }

        /**
         * Gets the plugin-specific health settings.
         *
         * @return the plugin-specific health settings
         */
        public Map<String, PluginHealthProperties> getPlugins() {
            return plugins;
        }

        /**
         * Sets the plugin-specific health settings.
         *
         * @param plugins the plugin-specific health settings
         */
        public void setPlugins(Map<String, PluginHealthProperties> plugins) {
            this.plugins = plugins;
        }
    }

    /**
     * Configuration properties for plugin-specific health settings.
     */
    public static class PluginHealthProperties {

        /**
         * Creates a new PluginHealthProperties instance with default values.
         */
        public PluginHealthProperties() {
            // Initialize with default values
        }

        /**
         * Whether to enable health monitoring for this plugin.
         */
        private Boolean enabled;

        /**
         * Whether to enable automatic recovery for this plugin.
         */
        private Boolean autoRecoveryEnabled;

        /**
         * The maximum number of recovery attempts for this plugin.
         */
        private Integer maxRecoveryAttempts;

        /**
         * Gets whether health monitoring is enabled for this plugin.
         *
         * @return whether health monitoring is enabled
         */
        public Boolean getEnabled() {
            return enabled;
        }

        /**
         * Sets whether health monitoring is enabled for this plugin.
         *
         * @param enabled whether health monitoring is enabled
         */
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets whether automatic recovery is enabled for this plugin.
         *
         * @return whether automatic recovery is enabled
         */
        public Boolean getAutoRecoveryEnabled() {
            return autoRecoveryEnabled;
        }

        /**
         * Sets whether automatic recovery is enabled for this plugin.
         *
         * @param autoRecoveryEnabled whether automatic recovery is enabled
         */
        public void setAutoRecoveryEnabled(Boolean autoRecoveryEnabled) {
            this.autoRecoveryEnabled = autoRecoveryEnabled;
        }

        /**
         * Gets the maximum number of recovery attempts for this plugin.
         *
         * @return the maximum number of recovery attempts
         */
        public Integer getMaxRecoveryAttempts() {
            return maxRecoveryAttempts;
        }

        /**
         * Sets the maximum number of recovery attempts for this plugin.
         *
         * @param maxRecoveryAttempts the maximum number of recovery attempts
         */
        public void setMaxRecoveryAttempts(Integer maxRecoveryAttempts) {
            this.maxRecoveryAttempts = maxRecoveryAttempts;
        }
    }

    /**
     * Configuration properties for the plugin debugger.
     */
    public static class DebuggerProperties {

        /**
         * Creates a new DebuggerProperties instance with default values.
         */
        public DebuggerProperties() {
            // Initialize with default values
        }

        /**
         * Whether to enable the plugin debugger.
         */
        private boolean enabled = false;

        /**
         * The port to use for the debugger server.
         */
        private int port = 8000;

        /**
         * Whether to enable remote debugging.
         */
        private boolean remoteDebuggingEnabled = false;

        /**
         * Whether to enable breakpoints.
         */
        private boolean breakpointsEnabled = true;

        /**
         * Whether to enable variable inspection.
         */
        private boolean variableInspectionEnabled = true;

        /**
         * Whether to enable step-by-step execution.
         */
        private boolean stepExecutionEnabled = true;

        /**
         * Whether to enable logging of debug events.
         */
        private boolean logDebugEvents = true;

        /**
         * The maximum number of debug sessions that can be active at once.
         */
        private int maxConcurrentSessions = 5;

        /**
         * The timeout in milliseconds for debug sessions.
         */
        private long sessionTimeoutMs = 3600000; // 1 hour

        /**
         * Plugin-specific debugger settings.
         */
        private Map<String, PluginDebuggerProperties> plugins = new HashMap<>();

        /**
         * Gets whether the debugger is enabled.
         *
         * @return whether the debugger is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the debugger is enabled.
         *
         * @param enabled whether the debugger is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the port to use for the debugger server.
         *
         * @return the port
         */
        public int getPort() {
            return port;
        }

        /**
         * Sets the port to use for the debugger server.
         *
         * @param port the port
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * Gets whether remote debugging is enabled.
         *
         * @return whether remote debugging is enabled
         */
        public boolean isRemoteDebuggingEnabled() {
            return remoteDebuggingEnabled;
        }

        /**
         * Sets whether remote debugging is enabled.
         *
         * @param remoteDebuggingEnabled whether remote debugging is enabled
         */
        public void setRemoteDebuggingEnabled(boolean remoteDebuggingEnabled) {
            this.remoteDebuggingEnabled = remoteDebuggingEnabled;
        }

        /**
         * Gets whether breakpoints are enabled.
         *
         * @return whether breakpoints are enabled
         */
        public boolean isBreakpointsEnabled() {
            return breakpointsEnabled;
        }

        /**
         * Sets whether breakpoints are enabled.
         *
         * @param breakpointsEnabled whether breakpoints are enabled
         */
        public void setBreakpointsEnabled(boolean breakpointsEnabled) {
            this.breakpointsEnabled = breakpointsEnabled;
        }

        /**
         * Gets whether variable inspection is enabled.
         *
         * @return whether variable inspection is enabled
         */
        public boolean isVariableInspectionEnabled() {
            return variableInspectionEnabled;
        }

        /**
         * Sets whether variable inspection is enabled.
         *
         * @param variableInspectionEnabled whether variable inspection is enabled
         */
        public void setVariableInspectionEnabled(boolean variableInspectionEnabled) {
            this.variableInspectionEnabled = variableInspectionEnabled;
        }

        /**
         * Gets whether step-by-step execution is enabled.
         *
         * @return whether step-by-step execution is enabled
         */
        public boolean isStepExecutionEnabled() {
            return stepExecutionEnabled;
        }

        /**
         * Sets whether step-by-step execution is enabled.
         *
         * @param stepExecutionEnabled whether step-by-step execution is enabled
         */
        public void setStepExecutionEnabled(boolean stepExecutionEnabled) {
            this.stepExecutionEnabled = stepExecutionEnabled;
        }

        /**
         * Gets whether logging of debug events is enabled.
         *
         * @return whether logging of debug events is enabled
         */
        public boolean isLogDebugEvents() {
            return logDebugEvents;
        }

        /**
         * Sets whether logging of debug events is enabled.
         *
         * @param logDebugEvents whether logging of debug events is enabled
         */
        public void setLogDebugEvents(boolean logDebugEvents) {
            this.logDebugEvents = logDebugEvents;
        }

        /**
         * Gets the maximum number of concurrent debug sessions.
         *
         * @return the maximum number of concurrent debug sessions
         */
        public int getMaxConcurrentSessions() {
            return maxConcurrentSessions;
        }

        /**
         * Sets the maximum number of concurrent debug sessions.
         *
         * @param maxConcurrentSessions the maximum number of concurrent debug sessions
         */
        public void setMaxConcurrentSessions(int maxConcurrentSessions) {
            this.maxConcurrentSessions = maxConcurrentSessions;
        }

        /**
         * Gets the session timeout in milliseconds.
         *
         * @return the session timeout in milliseconds
         */
        public long getSessionTimeoutMs() {
            return sessionTimeoutMs;
        }

        /**
         * Sets the session timeout in milliseconds.
         *
         * @param sessionTimeoutMs the session timeout in milliseconds
         */
        public void setSessionTimeoutMs(long sessionTimeoutMs) {
            this.sessionTimeoutMs = sessionTimeoutMs;
        }

        /**
         * Gets the plugin-specific debugger settings.
         *
         * @return the plugin-specific debugger settings
         */
        public Map<String, PluginDebuggerProperties> getPlugins() {
            return plugins;
        }

        /**
         * Sets the plugin-specific debugger settings.
         *
         * @param plugins the plugin-specific debugger settings
         */
        public void setPlugins(Map<String, PluginDebuggerProperties> plugins) {
            this.plugins = plugins;
        }
    }

    /**
     * Configuration properties for Git repositories.
     */
    public static class GitProperties {

        /**
         * Creates a new GitProperties instance with default values.
         */
        public GitProperties() {
            // Initialize with default values
        }

        /**
         * Authentication type for Git repositories.
         * Possible values: none, basic, ssh, token
         */
        private String authenticationType = "none";

        /**
         * Username for basic authentication.
         */
        private String username;

        /**
         * Password for basic authentication.
         */
        private String password;

        /**
         * Path to the private key file for SSH authentication.
         */
        private Path privateKeyPath;

        /**
         * Passphrase for the private key.
         */
        private String privateKeyPassphrase;

        /**
         * Personal access token for token-based authentication.
         */
        private String accessToken;

        /**
         * Whether to verify SSL certificates.
         */
        private boolean verifySsl = true;

        /**
         * Default branch to use when not specified.
         */
        private String defaultBranch = "main";

        /**
         * Timeout in seconds for Git operations.
         */
        private int timeoutSeconds = 60;

        /**
         * Gets the authentication type.
         *
         * @return the authentication type
         */
        public String getAuthenticationType() {
            return authenticationType;
        }

        /**
         * Sets the authentication type.
         *
         * @param authenticationType the authentication type
         */
        public void setAuthenticationType(String authenticationType) {
            this.authenticationType = authenticationType;
        }

        /**
         * Gets the username.
         *
         * @return the username
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sets the username.
         *
         * @param username the username
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * Gets the password.
         *
         * @return the password
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets the password.
         *
         * @param password the password
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Gets the private key path.
         *
         * @return the private key path
         */
        public Path getPrivateKeyPath() {
            return privateKeyPath;
        }

        /**
         * Sets the private key path.
         *
         * @param privateKeyPath the private key path
         */
        public void setPrivateKeyPath(Path privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        /**
         * Gets the private key passphrase.
         *
         * @return the private key passphrase
         */
        public String getPrivateKeyPassphrase() {
            return privateKeyPassphrase;
        }

        /**
         * Sets the private key passphrase.
         *
         * @param privateKeyPassphrase the private key passphrase
         */
        public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
            this.privateKeyPassphrase = privateKeyPassphrase;
        }

        /**
         * Gets the access token.
         *
         * @return the access token
         */
        public String getAccessToken() {
            return accessToken;
        }

        /**
         * Sets the access token.
         *
         * @param accessToken the access token
         */
        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        /**
         * Gets whether to verify SSL certificates.
         *
         * @return whether to verify SSL certificates
         */
        public boolean isVerifySsl() {
            return verifySsl;
        }

        /**
         * Sets whether to verify SSL certificates.
         *
         * @param verifySsl whether to verify SSL certificates
         */
        public void setVerifySsl(boolean verifySsl) {
            this.verifySsl = verifySsl;
        }

        /**
         * Gets the default branch.
         *
         * @return the default branch
         */
        public String getDefaultBranch() {
            return defaultBranch;
        }

        /**
         * Sets the default branch.
         *
         * @param defaultBranch the default branch
         */
        public void setDefaultBranch(String defaultBranch) {
            this.defaultBranch = defaultBranch;
        }

        /**
         * Gets the timeout in seconds.
         *
         * @return the timeout in seconds
         */
        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        /**
         * Sets the timeout in seconds.
         *
         * @param timeoutSeconds the timeout in seconds
         */
        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    /**
     * Configuration properties for plugin-specific debugger settings.
     */
    public static class PluginDebuggerProperties {

        /**
         * Creates a new PluginDebuggerProperties instance with default values.
         */
        public PluginDebuggerProperties() {
            // Initialize with default values
        }

        /**
         * Whether debugging is enabled for this plugin.
         */
        private Boolean enabled;

        /**
         * Whether breakpoints are enabled for this plugin.
         */
        private Boolean breakpointsEnabled;

        /**
         * Whether variable inspection is enabled for this plugin.
         */
        private Boolean variableInspectionEnabled;

        /**
         * Whether step-by-step execution is enabled for this plugin.
         */
        private Boolean stepExecutionEnabled;

        /**
         * Gets whether debugging is enabled for this plugin.
         *
         * @return whether debugging is enabled
         */
        public Boolean getEnabled() {
            return enabled;
        }

        /**
         * Sets whether debugging is enabled for this plugin.
         *
         * @param enabled whether debugging is enabled
         */
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets whether breakpoints are enabled for this plugin.
         *
         * @return whether breakpoints are enabled
         */
        public Boolean getBreakpointsEnabled() {
            return breakpointsEnabled;
        }

        /**
         * Sets whether breakpoints are enabled for this plugin.
         *
         * @param breakpointsEnabled whether breakpoints are enabled
         */
        public void setBreakpointsEnabled(Boolean breakpointsEnabled) {
            this.breakpointsEnabled = breakpointsEnabled;
        }

        /**
         * Gets whether variable inspection is enabled for this plugin.
         *
         * @return whether variable inspection is enabled
         */
        public Boolean getVariableInspectionEnabled() {
            return variableInspectionEnabled;
        }

        /**
         * Sets whether variable inspection is enabled for this plugin.
         *
         * @param variableInspectionEnabled whether variable inspection is enabled
         */
        public void setVariableInspectionEnabled(Boolean variableInspectionEnabled) {
            this.variableInspectionEnabled = variableInspectionEnabled;
        }

        /**
         * Gets whether step-by-step execution is enabled for this plugin.
         *
         * @return whether step-by-step execution is enabled
         */
        public Boolean getStepExecutionEnabled() {
            return stepExecutionEnabled;
        }

        /**
         * Sets whether step-by-step execution is enabled for this plugin.
         *
         * @param stepExecutionEnabled whether step-by-step execution is enabled
         */
        public void setStepExecutionEnabled(Boolean stepExecutionEnabled) {
            this.stepExecutionEnabled = stepExecutionEnabled;
        }
    }
}
