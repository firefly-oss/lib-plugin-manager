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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        "firefly.plugin-manager.event-bus.kafka.replication-factor=3",
        "firefly.plugin-manager.security.enforce-security-checks=true",
        "firefly.plugin-manager.security.allow-file-access=false",
        "firefly.plugin-manager.security.allow-network-access=true",
        "firefly.plugin-manager.security.allowed-hosts=example.com,api.example.org",
        "firefly.plugin-manager.security.allowed-directories=/tmp,/var/log",
        "firefly.plugin-manager.security.require-signature=true",
        "firefly.plugin-manager.security.trusted-certificates-directory=/etc/certs",
        "firefly.plugin-manager.resources.enforce-resource-limits=true",
        "firefly.plugin-manager.hot-deployment.enabled=true",
        "firefly.plugin-manager.hot-deployment.polling-interval-ms=1000",
        "firefly.plugin-manager.hot-deployment.auto-reload=true",
        "firefly.plugin-manager.hot-deployment.watch-for-new-plugins=true",
        "firefly.plugin-manager.hot-deployment.watch-for-plugin-updates=true",
        "firefly.plugin-manager.hot-deployment.watch-for-plugin-deletions=true",
        "firefly.plugin-manager.health.enabled=true",
        "firefly.plugin-manager.health.monitoring-interval-ms=30000",
        "firefly.plugin-manager.health.auto-recovery-enabled=true",
        "firefly.plugin-manager.health.max-recovery-attempts=5",
        "firefly.plugin-manager.health.recovery-delay-ms=10000",
        "firefly.plugin-manager.debugger.enabled=true",
        "firefly.plugin-manager.debugger.port=8000",
        "firefly.plugin-manager.debugger.remote-debugging-enabled=false",
        "firefly.plugin-manager.debugger.breakpoints-enabled=true",
        "firefly.plugin-manager.debugger.variable-inspection-enabled=true",
        "firefly.plugin-manager.debugger.step-execution-enabled=true",
        "firefly.plugin-manager.debugger.log-debug-events=true",
        "firefly.plugin-manager.debugger.max-concurrent-sessions=5",
        "firefly.plugin-manager.debugger.session-timeout-ms=3600000",
        "firefly.plugin-manager.resources.max-memory-mb=512",
        "firefly.plugin-manager.resources.max-cpu-percentage=50",
        "firefly.plugin-manager.resources.max-threads=20",
        "firefly.plugin-manager.resources.max-file-handles=100",
        "firefly.plugin-manager.resources.max-network-connections=30"
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
    void testSecurityProperties() {
        PluginManagerProperties.SecurityProperties security = properties.getSecurity();
        assertTrue(security.isEnforceSecurityChecks());
        assertFalse(security.isAllowFileAccess());
        assertTrue(security.isAllowNetworkAccess());
        assertEquals(List.of("example.com", "api.example.org"), security.getAllowedHosts());
        assertEquals(List.of("/tmp", "/var/log"), security.getAllowedDirectories());
        assertTrue(security.isRequireSignature());
        assertEquals(Path.of("/etc/certs"), security.getTrustedCertificatesDirectory());
    }

    @Test
    void testResourceProperties() {
        PluginManagerProperties.ResourceProperties resources = properties.getResources();
        assertTrue(resources.isEnforceResourceLimits());
        assertEquals(512, resources.getMaxMemoryMb());
        assertEquals(50, resources.getMaxCpuPercentage());
        assertEquals(20, resources.getMaxThreads());
        assertEquals(100, resources.getMaxFileHandles());
        assertEquals(30, resources.getMaxNetworkConnections());
    }

    @Test
    void testHotDeploymentProperties() {
        PluginManagerProperties.HotDeploymentProperties hotDeployment = properties.getHotDeployment();
        assertTrue(hotDeployment.isEnabled());
        assertEquals(1000, hotDeployment.getPollingIntervalMs());
        assertTrue(hotDeployment.isAutoReload());
        assertTrue(hotDeployment.isWatchForNewPlugins());
        assertTrue(hotDeployment.isWatchForPluginUpdates());
        assertTrue(hotDeployment.isWatchForPluginDeletions());
    }

    @Test
    void testHealthProperties() {
        PluginManagerProperties.HealthProperties health = properties.getHealth();
        assertTrue(health.isEnabled());
        assertEquals(30000, health.getMonitoringIntervalMs());
        assertTrue(health.isAutoRecoveryEnabled());
        assertEquals(5, health.getMaxRecoveryAttempts());
        assertEquals(10000, health.getRecoveryDelayMs());
    }

    @Test
    void testDebuggerProperties() {
        PluginManagerProperties.DebuggerProperties debugger = properties.getDebugger();
        assertTrue(debugger.isEnabled());
        assertEquals(8000, debugger.getPort());
        assertFalse(debugger.isRemoteDebuggingEnabled());
        assertTrue(debugger.isBreakpointsEnabled());
        assertTrue(debugger.isVariableInspectionEnabled());
        assertTrue(debugger.isStepExecutionEnabled());
        assertTrue(debugger.isLogDebugEvents());
        assertEquals(5, debugger.getMaxConcurrentSessions());
        assertEquals(3600000, debugger.getSessionTimeoutMs());
        assertNotNull(debugger.getPlugins());
        assertTrue(debugger.getPlugins().isEmpty());
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

        // Set security properties
        PluginManagerProperties.SecurityProperties security = new PluginManagerProperties.SecurityProperties();
        security.setEnforceSecurityChecks(true);
        security.setAllowFileAccess(false);
        security.setAllowNetworkAccess(true);
        security.setAllowedHosts(List.of("test.com"));
        security.setAllowedDirectories(List.of("/test"));
        security.setRequireSignature(true);
        security.setTrustedCertificatesDirectory(Path.of("/test/certs"));
        newProps.setSecurity(security);

        // Verify security properties
        assertTrue(newProps.getSecurity().isEnforceSecurityChecks());
        assertFalse(newProps.getSecurity().isAllowFileAccess());
        assertTrue(newProps.getSecurity().isAllowNetworkAccess());
        assertEquals(List.of("test.com"), newProps.getSecurity().getAllowedHosts());
        assertEquals(List.of("/test"), newProps.getSecurity().getAllowedDirectories());
        assertTrue(newProps.getSecurity().isRequireSignature());
        assertEquals(Path.of("/test/certs"), newProps.getSecurity().getTrustedCertificatesDirectory());

        // Set resource properties
        PluginManagerProperties.ResourceProperties resources = new PluginManagerProperties.ResourceProperties();
        resources.setEnforceResourceLimits(true);
        resources.setMaxMemoryMb(256);
        resources.setMaxCpuPercentage(25);
        resources.setMaxThreads(10);
        resources.setMaxFileHandles(50);
        resources.setMaxNetworkConnections(15);
        newProps.setResources(resources);

        // Verify resource properties
        assertTrue(newProps.getResources().isEnforceResourceLimits());
        assertEquals(256, newProps.getResources().getMaxMemoryMb());
        assertEquals(25, newProps.getResources().getMaxCpuPercentage());
        assertEquals(10, newProps.getResources().getMaxThreads());
        assertEquals(50, newProps.getResources().getMaxFileHandles());
        assertEquals(15, newProps.getResources().getMaxNetworkConnections());

        // Set hot deployment properties
        PluginManagerProperties.HotDeploymentProperties hotDeployment = new PluginManagerProperties.HotDeploymentProperties();
        hotDeployment.setEnabled(true);
        hotDeployment.setPollingIntervalMs(2000);
        hotDeployment.setAutoReload(false);
        hotDeployment.setWatchForNewPlugins(true);
        hotDeployment.setWatchForPluginUpdates(false);
        hotDeployment.setWatchForPluginDeletions(true);
        newProps.setHotDeployment(hotDeployment);

        // Verify hot deployment properties
        assertTrue(newProps.getHotDeployment().isEnabled());
        assertEquals(2000, newProps.getHotDeployment().getPollingIntervalMs());
        assertFalse(newProps.getHotDeployment().isAutoReload());
        assertTrue(newProps.getHotDeployment().isWatchForNewPlugins());
        assertFalse(newProps.getHotDeployment().isWatchForPluginUpdates());
        assertTrue(newProps.getHotDeployment().isWatchForPluginDeletions());

        // Set health properties
        PluginManagerProperties.HealthProperties health = new PluginManagerProperties.HealthProperties();
        health.setEnabled(false);
        health.setMonitoringIntervalMs(15000);
        health.setAutoRecoveryEnabled(false);
        health.setMaxRecoveryAttempts(2);
        health.setRecoveryDelayMs(3000);
        newProps.setHealth(health);

        // Verify health properties
        assertFalse(newProps.getHealth().isEnabled());
        assertEquals(15000, newProps.getHealth().getMonitoringIntervalMs());
        assertFalse(newProps.getHealth().isAutoRecoveryEnabled());
        assertEquals(2, newProps.getHealth().getMaxRecoveryAttempts());
        assertEquals(3000, newProps.getHealth().getRecoveryDelayMs());

        // Set debugger properties
        PluginManagerProperties.DebuggerProperties debugger = new PluginManagerProperties.DebuggerProperties();
        debugger.setEnabled(true);
        debugger.setPort(9000);
        debugger.setRemoteDebuggingEnabled(true);
        debugger.setBreakpointsEnabled(false);
        debugger.setVariableInspectionEnabled(false);
        debugger.setStepExecutionEnabled(false);
        debugger.setLogDebugEvents(false);
        debugger.setMaxConcurrentSessions(10);
        debugger.setSessionTimeoutMs(7200000);

        // Set plugin-specific debugger properties
        PluginManagerProperties.PluginDebuggerProperties pluginDebugger = new PluginManagerProperties.PluginDebuggerProperties();
        pluginDebugger.setEnabled(false);
        pluginDebugger.setBreakpointsEnabled(true);
        pluginDebugger.setVariableInspectionEnabled(true);
        pluginDebugger.setStepExecutionEnabled(true);

        Map<String, PluginManagerProperties.PluginDebuggerProperties> plugins = new HashMap<>();
        plugins.put("test-plugin", pluginDebugger);
        debugger.setPlugins(plugins);

        newProps.setDebugger(debugger);

        // Verify debugger properties
        assertTrue(newProps.getDebugger().isEnabled());
        assertEquals(9000, newProps.getDebugger().getPort());
        assertTrue(newProps.getDebugger().isRemoteDebuggingEnabled());
        assertFalse(newProps.getDebugger().isBreakpointsEnabled());
        assertFalse(newProps.getDebugger().isVariableInspectionEnabled());
        assertFalse(newProps.getDebugger().isStepExecutionEnabled());
        assertFalse(newProps.getDebugger().isLogDebugEvents());
        assertEquals(10, newProps.getDebugger().getMaxConcurrentSessions());
        assertEquals(7200000, newProps.getDebugger().getSessionTimeoutMs());

        // Verify plugin-specific debugger properties
        assertEquals(1, newProps.getDebugger().getPlugins().size());
        PluginManagerProperties.PluginDebuggerProperties testPluginProps = newProps.getDebugger().getPlugins().get("test-plugin");
        assertNotNull(testPluginProps);
        assertFalse(testPluginProps.getEnabled());
        assertTrue(testPluginProps.getBreakpointsEnabled());
        assertTrue(testPluginProps.getVariableInspectionEnabled());
        assertTrue(testPluginProps.getStepExecutionEnabled());
    }

    @Test
    void testPluginSpecificProperties() {
        // Create a new properties instance
        PluginManagerProperties newProps = new PluginManagerProperties();

        // Create plugin-specific security properties
        PluginManagerProperties.PluginSecurityProperties pluginSecurity = new PluginManagerProperties.PluginSecurityProperties();
        pluginSecurity.setAllowFileAccess(true);
        pluginSecurity.setAllowNetworkAccess(false);
        pluginSecurity.setAllowedHosts(List.of("plugin-api.com"));
        pluginSecurity.setAllowedDirectories(List.of("/plugin/data"));

        // Add to security properties
        newProps.getSecurity().getPlugins().put("test-plugin", pluginSecurity);

        // Verify plugin-specific security properties
        PluginManagerProperties.PluginSecurityProperties retrievedProps = newProps.getSecurity().getPlugins().get("test-plugin");
        assertNotNull(retrievedProps);
        assertTrue(retrievedProps.getAllowFileAccess());
        assertFalse(retrievedProps.getAllowNetworkAccess());
        assertEquals(List.of("plugin-api.com"), retrievedProps.getAllowedHosts());
        assertEquals(List.of("/plugin/data"), retrievedProps.getAllowedDirectories());

        // Create plugin-specific resource properties
        PluginManagerProperties.PluginResourceProperties pluginResources = new PluginManagerProperties.PluginResourceProperties();
        pluginResources.setMaxMemoryMb(128);
        pluginResources.setMaxCpuPercentage(10);
        pluginResources.setMaxThreads(5);
        pluginResources.setMaxFileHandles(20);
        pluginResources.setMaxNetworkConnections(5);

        // Add to resource properties
        newProps.getResources().getPlugins().put("test-plugin", pluginResources);

        // Verify plugin-specific resource properties
        PluginManagerProperties.PluginResourceProperties retrievedResources = newProps.getResources().getPlugins().get("test-plugin");
        assertNotNull(retrievedResources);
        assertEquals(Integer.valueOf(128), retrievedResources.getMaxMemoryMb());
        assertEquals(Integer.valueOf(10), retrievedResources.getMaxCpuPercentage());
        assertEquals(Integer.valueOf(5), retrievedResources.getMaxThreads());
        assertEquals(Integer.valueOf(20), retrievedResources.getMaxFileHandles());
        assertEquals(Integer.valueOf(5), retrievedResources.getMaxNetworkConnections());
    }

    @EnableConfigurationProperties(PluginManagerProperties.class)
    static class TestConfiguration {
    }
}
