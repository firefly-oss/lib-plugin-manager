# Configuration Tests

This package contains tests for the configuration components of the Firefly Plugin Manager.

## PluginManagerPropertiesTest

Tests the binding of configuration properties to the `PluginManagerProperties` class.

### Test Cases

- **testBasicProperties**: Verifies that basic properties are correctly bound.
- **testEventBusProperties**: Verifies that event bus properties are correctly bound.
- **testKafkaProperties**: Verifies that Kafka properties are correctly bound.
- **testSetters**: Verifies that property setters work correctly.

### Key Assertions

- Properties are correctly bound from application.properties
- Nested properties are correctly bound
- Property setters and getters work correctly

### Test Configuration

The test uses the following configuration properties:

```properties
firefly.plugin-manager.plugins-directory=custom-plugins
firefly.plugin-manager.auto-start-plugins=false
firefly.plugin-manager.scan-on-startup=false
firefly.plugin-manager.auto-load-plugins=plugin1,plugin2
firefly.plugin-manager.allowed-packages=com.custom.package1,com.custom.package2
firefly.plugin-manager.event-bus.type=kafka
firefly.plugin-manager.event-bus.distributed-events=true
firefly.plugin-manager.event-bus.kafka.bootstrap-servers=kafka1:9092,kafka2:9092
firefly.plugin-manager.event-bus.kafka.consumer-group-id=custom-group
firefly.plugin-manager.event-bus.kafka.default-topic=custom-topic
firefly.plugin-manager.event-bus.kafka.auto-create-topics=false
firefly.plugin-manager.event-bus.kafka.num-partitions=5
firefly.plugin-manager.event-bus.kafka.replication-factor=3
```

## PluginManagerAutoConfigurationTest

Tests the auto-configuration of beans for the plugin manager.

### Test Cases

- **testDefaultConfiguration**: Verifies that default beans are created with default configuration.
- **testKafkaEventBusConfiguration**: Verifies that the Kafka event bus is created when configured.
- **testCustomBeans**: Verifies that custom beans can override default beans.
- **testObjectMapperConfiguration**: Verifies that the ObjectMapper is correctly configured.

### Key Assertions

- Default beans are created
- Beans are created based on configuration properties
- Custom beans can override default beans
- ObjectMapper is correctly configured for event serialization

### Example

```java
@Test
void testDefaultConfiguration() {
    contextRunner.run(context -> {
        // Verify all beans are created with default configuration
        assertThat(context).hasSingleBean(PluginEventBus.class);
        assertThat(context).hasSingleBean(PluginRegistry.class);
        assertThat(context).hasSingleBean(ExtensionRegistry.class);
        assertThat(context).hasSingleBean(PluginLoader.class);
        assertThat(context).hasSingleBean(PluginManager.class);
        
        // Verify default implementations
        assertThat(context).getBean(PluginEventBus.class).isInstanceOf(DefaultPluginEventBus.class);
        assertThat(context).getBean(PluginRegistry.class).isInstanceOf(DefaultPluginRegistry.class);
        assertThat(context).getBean(ExtensionRegistry.class).isInstanceOf(DefaultExtensionRegistry.class);
        assertThat(context).getBean(PluginLoader.class).isInstanceOf(DefaultPluginLoader.class);
        assertThat(context).getBean(PluginManager.class).isInstanceOf(DefaultPluginManager.class);
    });
}
```
