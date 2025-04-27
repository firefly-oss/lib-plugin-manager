package com.catalis.core.plugin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = PluginManagerPropertiesTest.TestConfiguration.class)
@TestPropertySource(properties = {
        "firefly.plugin-manager.plugins-directory=custom-plugins",
        "firefly.plugin-manager.auto-start-plugins=false",
        "firefly.plugin-manager.scan-on-startup=false",
        "firefly.plugin-manager.auto-load-plugins=plugin1,plugin2",
        "firefly.plugin-manager.allowed-packages=com.custom.package1,com.custom.package2",
        "firefly.plugin-manager.event-bus.type=kafka",
        "firefly.plugin-manager.event-bus.distributed-events=true",
        "firefly.plugin-manager.event-bus.kafka.bootstrap-servers=kafka1:9092,kafka2:9092",
        "firefly.plugin-manager.event-bus.kafka.consumer-group-id=custom-group",
        "firefly.plugin-manager.event-bus.kafka.default-topic=custom-topic",
        "firefly.plugin-manager.event-bus.kafka.auto-create-topics=false",
        "firefly.plugin-manager.event-bus.kafka.num-partitions=5",
        "firefly.plugin-manager.event-bus.kafka.replication-factor=3"
})
public class PluginManagerPropertiesTest {

    @Autowired
    private PluginManagerProperties properties;

    @Test
    void testBasicProperties() {
        assertEquals(Path.of("custom-plugins"), properties.getPluginsDirectory());
        assertFalse(properties.isAutoStartPlugins());
        assertFalse(properties.isScanOnStartup());
        assertEquals(List.of("plugin1", "plugin2"), properties.getAutoLoadPlugins());
        assertEquals(List.of("com.custom.package1", "com.custom.package2"), properties.getAllowedPackages());
    }

    @Test
    void testEventBusProperties() {
        assertEquals("kafka", properties.getEventBus().getType());
        assertTrue(properties.getEventBus().isDistributedEvents());
    }

    @Test
    void testKafkaProperties() {
        PluginManagerProperties.KafkaProperties kafka = properties.getEventBus().getKafka();
        assertEquals("kafka1:9092,kafka2:9092", kafka.getBootstrapServers());
        assertEquals("custom-group", kafka.getConsumerGroupId());
        assertEquals("custom-topic", kafka.getDefaultTopic());
        assertFalse(kafka.isAutoCreateTopics());
        assertEquals(5, kafka.getNumPartitions());
        assertEquals(3, kafka.getReplicationFactor());
    }

    @Test
    void testSetters() {
        // Create a new properties instance
        PluginManagerProperties newProps = new PluginManagerProperties();
        
        // Set basic properties
        newProps.setPluginsDirectory(Path.of("test-plugins"));
        newProps.setAutoStartPlugins(true);
        newProps.setScanOnStartup(true);
        newProps.setAutoLoadPlugins(List.of("test-plugin"));
        newProps.setAllowedPackages(List.of("com.test"));
        
        // Verify basic properties
        assertEquals(Path.of("test-plugins"), newProps.getPluginsDirectory());
        assertTrue(newProps.isAutoStartPlugins());
        assertTrue(newProps.isScanOnStartup());
        assertEquals(List.of("test-plugin"), newProps.getAutoLoadPlugins());
        assertEquals(List.of("com.test"), newProps.getAllowedPackages());
        
        // Set event bus properties
        PluginManagerProperties.EventBusProperties eventBus = new PluginManagerProperties.EventBusProperties();
        eventBus.setType("in-memory");
        eventBus.setDistributedEvents(false);
        newProps.setEventBus(eventBus);
        
        // Verify event bus properties
        assertEquals("in-memory", newProps.getEventBus().getType());
        assertFalse(newProps.getEventBus().isDistributedEvents());
        
        // Set Kafka properties
        PluginManagerProperties.KafkaProperties kafka = new PluginManagerProperties.KafkaProperties();
        kafka.setBootstrapServers("localhost:9092");
        kafka.setConsumerGroupId("test-group");
        kafka.setDefaultTopic("test-topic");
        kafka.setAutoCreateTopics(true);
        kafka.setNumPartitions(1);
        kafka.setReplicationFactor((short) 1);
        eventBus.setKafka(kafka);
        
        // Verify Kafka properties
        assertEquals("localhost:9092", newProps.getEventBus().getKafka().getBootstrapServers());
        assertEquals("test-group", newProps.getEventBus().getKafka().getConsumerGroupId());
        assertEquals("test-topic", newProps.getEventBus().getKafka().getDefaultTopic());
        assertTrue(newProps.getEventBus().getKafka().isAutoCreateTopics());
        assertEquals(1, newProps.getEventBus().getKafka().getNumPartitions());
        assertEquals(1, newProps.getEventBus().getKafka().getReplicationFactor());
    }

    @EnableConfigurationProperties(PluginManagerProperties.class)
    static class TestConfiguration {
    }
}
