package com.catalis.core.plugin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the plugin manager.
 */
@Component
@ConfigurationProperties(prefix = "firefly.plugin-manager")
public class PluginManagerProperties {

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
     * Configuration properties for the event bus.
     */
    public static class EventBusProperties {

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
}
