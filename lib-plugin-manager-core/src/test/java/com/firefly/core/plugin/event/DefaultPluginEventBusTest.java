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


package com.firefly.core.plugin.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultPluginEventBusTest {

    private DefaultPluginEventBus eventBus;

    // Test event classes
    private static class TestEvent extends PluginEvent {
        private final String message;

        public TestEvent(String pluginId, String message) {
            super(pluginId, "TEST");
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class AnotherTestEvent extends PluginEvent {
        private final int value;

        public AnotherTestEvent(String pluginId, int value) {
            super(pluginId, "ANOTHER_TEST");
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @BeforeEach
    void setUp() {
        eventBus = new DefaultPluginEventBus();
    }

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

    @Test
    void testSubscribeToSpecificType() {
        // Create events of different types
        TestEvent event1 = new TestEvent("test-plugin", "Event 1");
        AnotherTestEvent event2 = new AnotherTestEvent("test-plugin", 42);

        // Subscribe to TestEvent only
        List<TestEvent> receivedEvents = new ArrayList<>();
        eventBus.subscribe(TestEvent.class)
                .doOnNext(receivedEvents::add)
                .subscribe();

        // Publish both events
        StepVerifier.create(eventBus.publish(event1)
                .then(eventBus.publish(event2)))
                .verifyComplete();

        // Verify only TestEvent was received
        assertEquals(1, receivedEvents.size());
        assertEquals("Event 1", receivedEvents.get(0).getMessage());
    }

    @Test
    void testSubscribeToPlugin() {
        // Create events from different plugins
        TestEvent event1 = new TestEvent("plugin-1", "From plugin 1");
        TestEvent event2 = new TestEvent("plugin-2", "From plugin 2");

        // Subscribe to events from plugin-1 only
        List<PluginEvent> receivedEvents = new ArrayList<>();
        eventBus.subscribeToPlugin("plugin-1")
                .doOnNext(receivedEvents::add)
                .subscribe();

        // Publish both events
        StepVerifier.create(eventBus.publish(event1)
                .then(eventBus.publish(event2)))
                .verifyComplete();

        // Verify only events from plugin-1 were received
        assertEquals(1, receivedEvents.size());
        assertEquals("From plugin 1", ((TestEvent) receivedEvents.get(0)).getMessage());
    }

    @Test
    void testSubscribeToPluginAndType() {
        // Create events of different types from different plugins
        TestEvent event1 = new TestEvent("plugin-1", "Test from plugin 1");
        AnotherTestEvent event2 = new AnotherTestEvent("plugin-1", 42);
        TestEvent event3 = new TestEvent("plugin-2", "Test from plugin 2");

        // Subscribe to TestEvent from plugin-1 only
        List<TestEvent> receivedEvents = new ArrayList<>();
        eventBus.subscribeToPlugin("plugin-1", TestEvent.class)
                .doOnNext(receivedEvents::add)
                .subscribe();

        // Publish all events
        StepVerifier.create(eventBus.publish(event1)
                .then(eventBus.publish(event2))
                .then(eventBus.publish(event3)))
                .verifyComplete();

        // Verify only TestEvent from plugin-1 was received
        assertEquals(1, receivedEvents.size());
        assertEquals("Test from plugin 1", receivedEvents.get(0).getMessage());
    }

    @Test
    void testPublishToTopic() {
        // Create a test event
        TestEvent event = new TestEvent("test-plugin", "Topic message");

        // Subscribe to the topic
        List<PluginEvent> receivedEvents = new ArrayList<>();
        eventBus.subscribeTopic("test-topic")
                .doOnNext(receivedEvents::add)
                .subscribe();

        // Publish the event to the topic
        StepVerifier.create(eventBus.publish("test-topic", event))
                .verifyComplete();

        // Verify the event was received
        assertEquals(1, receivedEvents.size());
        assertEquals("Topic message", ((TestEvent) receivedEvents.get(0)).getMessage());
    }

    @Test
    void testSubscribeToTopicAndType() {
        // Create events of different types
        TestEvent event1 = new TestEvent("test-plugin", "Test topic event");
        AnotherTestEvent event2 = new AnotherTestEvent("test-plugin", 42);

        // Subscribe to TestEvent on the topic only
        List<TestEvent> receivedEvents = new ArrayList<>();
        eventBus.subscribeTopic("test-topic", TestEvent.class)
                .doOnNext(receivedEvents::add)
                .subscribe();

        // Publish both events to the topic
        StepVerifier.create(eventBus.publish("test-topic", event1)
                .then(eventBus.publish("test-topic", event2)))
                .verifyComplete();

        // Verify only TestEvent was received
        assertEquals(1, receivedEvents.size());
        assertEquals("Test topic event", receivedEvents.get(0).getMessage());
    }

    @Test
    void testMultipleSubscribers() {
        // Create a test event
        TestEvent event = new TestEvent("test-plugin", "Broadcast");

        // Create multiple subscribers
        AtomicInteger subscriber1Count = new AtomicInteger(0);
        AtomicInteger subscriber2Count = new AtomicInteger(0);

        eventBus.subscribe(TestEvent.class)
                .doOnNext(e -> subscriber1Count.incrementAndGet())
                .subscribe();

        eventBus.subscribe(TestEvent.class)
                .doOnNext(e -> subscriber2Count.incrementAndGet())
                .subscribe();

        // Publish the event
        StepVerifier.create(eventBus.publish(event))
                .verifyComplete();

        // Verify both subscribers received the event
        assertEquals(1, subscriber1Count.get());
        assertEquals(1, subscriber2Count.get());
    }

    @Test
    void testInitializeAndShutdown() {
        // Initialize the event bus
        StepVerifier.create(eventBus.initialize())
                .verifyComplete();

        // Create a subscriber
        List<TestEvent> receivedEvents = new ArrayList<>();
        eventBus.subscribe(TestEvent.class)
                .doOnNext(receivedEvents::add)
                .subscribe();

        // Publish an event
        TestEvent event = new TestEvent("test-plugin", "Before shutdown");
        StepVerifier.create(eventBus.publish(event))
                .verifyComplete();

        // Verify the event was received
        assertEquals(1, receivedEvents.size());

        // Shutdown the event bus
        StepVerifier.create(eventBus.shutdown())
                .verifyComplete();

        // Try to publish another event
        TestEvent event2 = new TestEvent("test-plugin", "After shutdown");
        StepVerifier.create(eventBus.publish(event2))
                .verifyComplete();

        // Verify no new events were received (sink is completed)
        assertEquals(1, receivedEvents.size());
    }

    @Test
    void testGetTransportType() {
        assertEquals("in-memory", eventBus.getTransportType());
    }
}
