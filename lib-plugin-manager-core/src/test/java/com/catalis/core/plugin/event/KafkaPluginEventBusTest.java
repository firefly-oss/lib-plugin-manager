package com.catalis.core.plugin.event;

import com.catalis.core.plugin.config.PluginManagerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaPluginEventBusTest {

    @Mock
    private KafkaSender<String, String> kafkaSender;

    @Mock
    private KafkaReceiver<String, String> kafkaReceiver;

    @Mock
    private PluginManagerProperties pluginManagerProperties;

    @Mock
    private PluginManagerProperties.KafkaProperties kafkaProperties;

    private ObjectMapper objectMapper;
    private KafkaPluginEventBus eventBus;

    // Test event class
    private static class TestEvent extends PluginEvent {
        private final String message;

        public TestEvent(String pluginId, String message) {
            super(pluginId, "TEST");
            this.message = message;
        }

        public TestEvent(String pluginId, String message, Instant timestamp) {
            super(pluginId, "TEST", timestamp);
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        
        // Register the TestEvent class with the ObjectMapper
        objectMapper.registerSubtypes(TestEvent.class);
        
        // Configure mocks
        when(pluginManagerProperties.getEventBus()).thenReturn(mock(PluginManagerProperties.EventBusProperties.class));
        when(pluginManagerProperties.getEventBus().getKafka()).thenReturn(kafkaProperties);
        when(kafkaProperties.getBootstrapServers()).thenReturn("localhost:9092");
        when(kafkaProperties.getConsumerGroupId()).thenReturn("test-group");
        when(kafkaProperties.getDefaultTopic()).thenReturn("test-events");
        
        // Create the event bus with mocked dependencies
        eventBus = new KafkaPluginEventBus(objectMapper, pluginManagerProperties);
        
        // Use reflection to set the mocked KafkaSender
        java.lang.reflect.Field senderField = KafkaPluginEventBus.class.getDeclaredField("sender");
        senderField.setAccessible(true);
        senderField.set(eventBus, kafkaSender);
        
        // Mock KafkaSender behavior
        when(kafkaSender.send(any(Mono.class))).thenReturn(Flux.empty());
    }

    @Test
    void testPublish() throws Exception {
        // Create a test event
        TestEvent event = new TestEvent("test-plugin", "Hello Kafka!");
        
        // Publish the event
        StepVerifier.create(eventBus.publish(event))
                .verifyComplete();
        
        // Verify KafkaSender was called
        verify(kafkaSender).send(any(Mono.class));
    }

    @Test
    void testPublishToTopic() throws Exception {
        // Create a test event
        TestEvent event = new TestEvent("test-plugin", "Topic message");
        
        // Publish the event to a specific topic
        StepVerifier.create(eventBus.publish("custom-topic", event))
                .verifyComplete();
        
        // Verify KafkaSender was called
        verify(kafkaSender).send(any(Mono.class));
    }

    @Test
    void testGetTransportType() {
        assertEquals("kafka", eventBus.getTransportType());
    }

    @Test
    void testInitialize() throws Exception {
        // Mock the receivers map
        java.lang.reflect.Field receiversField = KafkaPluginEventBus.class.getDeclaredField("receivers");
        receiversField.setAccessible(true);
        receiversField.set(eventBus, spy(receiversField.get(eventBus)));
        
        // Initialize the event bus
        StepVerifier.create(eventBus.initialize())
                .verifyComplete();
        
        // Verify default topic subscription was attempted
        verify(kafkaProperties).getDefaultTopic();
    }

    @Test
    void testShutdown() throws Exception {
        // Mock the sender and receivers
        when(kafkaSender.close()).thenReturn(Mono.empty());
        
        // Add a mock receiver to the receivers map
        java.util.Map<String, KafkaReceiver<String, String>> receivers = new java.util.HashMap<>();
        receivers.put("test-topic", kafkaReceiver);
        
        java.lang.reflect.Field receiversField = KafkaPluginEventBus.class.getDeclaredField("receivers");
        receiversField.setAccessible(true);
        receiversField.set(eventBus, receivers);
        
        // Shutdown the event bus
        StepVerifier.create(eventBus.shutdown())
                .verifyComplete();
        
        // Verify sender and receiver were closed
        verify(kafkaSender).close();
        verify(kafkaReceiver).close();
    }

    @Test
    void testSubscribeToTopic() throws Exception {
        // Create a mock ReceiverRecord
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>(
                "test-topic", 0, 0, "TEST", 
                objectMapper.writeValueAsString(new TestEvent("test-plugin", "Received message")));
        
        ReceiverRecord<String, String> receiverRecord = new ReceiverRecord<>(consumerRecord, null);
        
        // Mock the KafkaReceiver behavior
        when(kafkaReceiver.receive()).thenReturn(Flux.just(receiverRecord));
        
        // Add the mock receiver to the receivers map
        java.util.Map<String, KafkaReceiver<String, String>> receivers = new java.util.HashMap<>();
        receivers.put("test-topic", kafkaReceiver);
        
        java.lang.reflect.Field receiversField = KafkaPluginEventBus.class.getDeclaredField("receivers");
        receiversField.setAccessible(true);
        receiversField.set(eventBus, receivers);
        
        // Create the event sinks
        java.util.Map<String, reactor.core.publisher.Sinks.Many<PluginEvent>> sinks = new java.util.HashMap<>();
        sinks.put("test-topic", reactor.core.publisher.Sinks.many().multicast().onBackpressureBuffer());
        
        java.lang.reflect.Field sinksField = KafkaPluginEventBus.class.getDeclaredField("eventSinksByTopic");
        sinksField.setAccessible(true);
        sinksField.set(eventBus, sinks);
        
        // Create the event fluxes
        java.util.Map<String, Flux<PluginEvent>> fluxes = new java.util.HashMap<>();
        fluxes.put("test-topic", sinks.get("test-topic").asFlux());
        
        java.lang.reflect.Field fluxesField = KafkaPluginEventBus.class.getDeclaredField("eventFluxesByTopic");
        fluxesField.setAccessible(true);
        fluxesField.set(eventBus, fluxes);
        
        // Subscribe to the topic
        CountDownLatch latch = new CountDownLatch(1);
        List<PluginEvent> receivedEvents = new ArrayList<>();
        
        eventBus.subscribeTopic("test-topic")
                .doOnNext(event -> {
                    receivedEvents.add(event);
                    latch.countDown();
                })
                .subscribe();
        
        // Emit an event to the sink
        sinks.get("test-topic").tryEmitNext(new TestEvent("test-plugin", "Sink message"));
        
        // Wait for the event to be received
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        // Verify the event was received
        assertEquals(1, receivedEvents.size());
        assertEquals("Sink message", ((TestEvent) receivedEvents.get(0)).getMessage());
    }
}
