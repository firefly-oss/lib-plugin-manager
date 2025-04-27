# Integration Tests

This package contains integration tests for the Firefly Plugin Manager, which test multiple components working together.

## PluginSystemIntegrationTest

Tests the end-to-end functionality of the plugin system, including plugin lifecycle, extension registration, and event communication.

### Test Cases

- **testPluginLifecycle**: Verifies the complete lifecycle of a plugin (registration, initialization, starting, stopping).
- **testExtensionRegistryAndUsage**: Verifies that extensions can be registered and used.
- **testPluginManagerOperations**: Verifies that the plugin manager can perform operations on plugins.
- **testEventBusCommunication**: Verifies that plugins can communicate through the event bus.

### Key Assertions

- Plugins go through the correct lifecycle states
- Extensions can be registered and retrieved
- Plugin manager operations work correctly
- Events are delivered between components

### Test Plugin Implementation

The test uses a simple plugin implementation:

```java
public static class TestPlugin extends AbstractPlugin {
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final TestExtensionImpl extension;

    public TestPlugin() {
        super(PluginMetadata.builder()
                .id("test-plugin")
                .name("Test Plugin")
                .version("1.0.0")
                .description("Test plugin for integration tests")
                .author("Test Author")
                .dependencies(Set.of())
                .installTime(Instant.now())
                .build());
        
        this.extension = new TestExtensionImpl();
    }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromRunnable(() -> initialized.set(true));
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> started.set(true));
    }

    @Override
    public Mono<Void> stop() {
        return Mono.fromRunnable(() -> stopped.set(true));
    }

    // Extension implementation
    public class TestExtensionImpl implements TestExtensionPoint {
        @Override
        public String getName() {
            return "Test Extension";
        }

        @Override
        public Mono<String> performAction(String input) {
            return Mono.just("Processed: " + input);
        }
    }
}
```

## KafkaIntegrationTest

Tests the integration with Kafka for distributed event processing.

### Test Cases

- **testPublishAndSubscribe**: Verifies that events can be published to Kafka and received by subscribers.
- **testPublishToCustomTopic**: Verifies that events can be published to custom Kafka topics.
- **testDirectKafkaInteraction**: Verifies direct interaction with Kafka using Reactor Kafka.

### Key Assertions

- Events are published to Kafka
- Events are received from Kafka
- Topic-based routing works correctly
- Event serialization and deserialization work correctly

### Test Configuration

The Kafka integration tests use an embedded Kafka broker for testing:

```java
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "ENABLE_KAFKA_TESTS", matches = "true")
@EmbeddedKafka(partitions = 1, topics = {"test-events"})
public class KafkaIntegrationTest {
    // ...
}
```

The tests are disabled by default and can be enabled by setting the environment variable `ENABLE_KAFKA_TESTS=true`.

## Example

```java
@Test
void testPluginLifecycle() {
    // Create a test plugin
    TestPlugin testPlugin = new TestPlugin();
    
    // Register the plugin
    StepVerifier.create(pluginRegistry.registerPlugin(testPlugin))
            .verifyComplete();
    
    // Verify the plugin is initialized
    assertTrue(testPlugin.isInitialized());
    
    // Verify the plugin state
    StepVerifier.create(pluginRegistry.getPluginDescriptor("test-plugin"))
            .assertNext(descriptor -> {
                assertEquals(PluginState.INITIALIZED, descriptor.state());
            })
            .verifyComplete();
    
    // Start the plugin
    StepVerifier.create(pluginRegistry.startPlugin("test-plugin"))
            .verifyComplete();
    
    // Verify the plugin is started
    assertTrue(testPlugin.isStarted());
    
    // Stop the plugin
    StepVerifier.create(pluginRegistry.stopPlugin("test-plugin"))
            .verifyComplete();
    
    // Verify the plugin is stopped
    assertTrue(testPlugin.isStopped());
}
```
