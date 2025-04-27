# Event Bus Tests

This package contains tests for the event bus components of the Firefly Plugin Manager.

## DefaultPluginEventBusTest

Tests the in-memory implementation of the `PluginEventBus` interface.

### Test Cases

- **testPublishAndSubscribe**: Verifies that events can be published and received by subscribers.
- **testSubscribeToSpecificType**: Verifies that subscribers can filter events by type.
- **testSubscribeToPlugin**: Verifies that subscribers can filter events by plugin ID.
- **testSubscribeToPluginAndType**: Verifies that subscribers can filter events by both plugin ID and type.
- **testPublishToTopic**: Verifies that events can be published to specific topics.
- **testSubscribeToTopicAndType**: Verifies that subscribers can filter topic events by type.
- **testMultipleSubscribers**: Verifies that multiple subscribers can receive the same event.
- **testInitializeAndShutdown**: Verifies that the event bus can be initialized and shut down correctly.
- **testGetTransportType**: Verifies that the transport type is reported correctly.

### Key Assertions

- Events are delivered to subscribers
- Event filtering works correctly
- Topic-based routing works correctly
- Multiple subscribers receive events
- Shutdown prevents further event delivery

## KafkaPluginEventBusTest

Tests the Kafka-based implementation of the `PluginEventBus` interface.

### Test Cases

- **testPublish**: Verifies that events can be published to Kafka.
- **testPublishToTopic**: Verifies that events can be published to specific Kafka topics.
- **testGetTransportType**: Verifies that the transport type is reported correctly.
- **testInitialize**: Verifies that the Kafka event bus can be initialized correctly.
- **testShutdown**: Verifies that the Kafka event bus can be shut down correctly.
- **testSubscribeToTopic**: Verifies that subscribers can receive events from Kafka topics.

### Key Assertions

- Events are published to Kafka
- Topic-based routing works correctly
- Initialization and shutdown work correctly
- Event serialization and deserialization work correctly

## PluginEventSerializerTest

Tests the serialization and deserialization of plugin events.

### Test Cases

- **testSerializeAndDeserializePluginLifecycleEvent**: Verifies that lifecycle events can be serialized and deserialized.
- **testSerializeAndDeserializePluginConfigurationEvent**: Verifies that configuration events can be serialized and deserialized.
- **testSerializeAndDeserializeCustomEvent**: Verifies that custom event types can be serialized and deserialized.
- **testGetObjectMapper**: Verifies that the object mapper can be retrieved.

### Key Assertions

- Events are correctly serialized to JSON
- Events are correctly deserialized from JSON
- Type information is preserved during serialization/deserialization
- Custom event types are handled correctly

## Example

```java
@Test
void testPublishAndSubscribe() {
    // Create a test event
    TestEvent event = new TestEvent("test-plugin", "Hello, world!");

    // Subscribe to events
    List<TestEvent> receivedEvents = new ArrayList<>();
    eventBus.subscribe(TestEvent.class)
            .doOnNext(receivedEvents::add)
            .subscribe();

    // Publish the event
    StepVerifier.create(eventBus.publish(event))
            .verifyComplete();

    // Verify the event was received
    assertEquals(1, receivedEvents.size());
    assertEquals("Hello, world!", receivedEvents.get(0).getMessage());
    assertEquals("test-plugin", receivedEvents.get(0).getPluginId());
}
```
