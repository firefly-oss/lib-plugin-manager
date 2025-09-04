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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PluginEventSerializerTest {

    @Mock
    private ApplicationContext applicationContext;

    private ObjectMapper objectMapper;
    private PluginEventSerializer serializer;

    // Test event classes
    private static class TestEvent extends PluginEvent {
        private String message;

        // Default constructor for Jackson
        public TestEvent() {
            super("", "TEST_EVENT");
        }

        public TestEvent(String pluginId, String message) {
            super(pluginId, "TEST_EVENT");
            this.message = message;
        }

        public TestEvent(String pluginId, String message, Instant timestamp) {
            super(pluginId, "TEST_EVENT", timestamp);
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private static class AnotherTestEvent extends PluginEvent {
        private int value;

        // Default constructor for Jackson
        public AnotherTestEvent() {
            super("", "ANOTHER_TEST_EVENT");
        }

        public AnotherTestEvent(String pluginId, int value) {
            super(pluginId, "ANOTHER_TEST_EVENT");
            this.value = value;
        }

        public AnotherTestEvent(String pluginId, int value, Instant timestamp) {
            super(pluginId, "ANOTHER_TEST_EVENT", timestamp);
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Register the test event classes with the ObjectMapper
        objectMapper.registerSubtypes(
            TestEvent.class,
            AnotherTestEvent.class,
            PluginLifecycleEvent.class,
            PluginConfigurationEvent.class
        );

        serializer = new PluginEventSerializer(objectMapper, applicationContext);

        // Mock the ApplicationContext to return our test events
        Map<String, PluginEvent> eventBeans = Map.of(
                "testEvent", new TestEvent("test-plugin", "Test message"),
                "anotherTestEvent", new AnotherTestEvent("test-plugin", 42)
        );

        when(applicationContext.getBeansOfType(PluginEvent.class)).thenReturn(eventBeans);

        // Initialize the serializer
        serializer.init();
    }

    @Test
    void testSerializeAndDeserializePluginLifecycleEvent() throws Exception {
        // Create a PluginLifecycleEvent
        PluginLifecycleEvent event = new PluginLifecycleEvent(
                "test-plugin",
                com.firefly.core.plugin.model.PluginState.INITIALIZED,
                com.firefly.core.plugin.model.PluginState.STARTED
        );

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(event);

        // Verify JSON contains expected fields
        assertTrue(json.contains("\"pluginId\":\"test-plugin\""));
        assertTrue(json.contains("\"eventType\":\"LIFECYCLE\""));
        assertTrue(json.contains("\"previousState\":\"INITIALIZED\""));
        assertTrue(json.contains("\"newState\":\"STARTED\""));

        // Deserialize from JSON
        PluginEvent deserializedEvent = objectMapper.readValue(json, PluginEvent.class);

        // Verify deserialized event
        assertTrue(deserializedEvent instanceof PluginLifecycleEvent);
        PluginLifecycleEvent lifecycleEvent = (PluginLifecycleEvent) deserializedEvent;
        assertEquals("test-plugin", lifecycleEvent.getPluginId());
        assertEquals("LIFECYCLE", lifecycleEvent.getEventType());
        assertEquals(com.firefly.core.plugin.model.PluginState.INITIALIZED, lifecycleEvent.getPreviousState());
        assertEquals(com.firefly.core.plugin.model.PluginState.STARTED, lifecycleEvent.getNewState());
    }

    @Test
    void testSerializeAndDeserializePluginConfigurationEvent() throws Exception {
        // Create a PluginConfigurationEvent
        Map<String, Object> prevConfig = Map.of("key1", "value1");
        Map<String, Object> newConfig = Map.of("key1", "value2", "key2", 42);

        PluginConfigurationEvent event = new PluginConfigurationEvent(
                "test-plugin",
                prevConfig,
                newConfig
        );

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(event);

        // Verify JSON contains expected fields
        assertTrue(json.contains("\"pluginId\":\"test-plugin\""), "JSON should contain pluginId");
        assertTrue(json.contains("\"eventType\":\"CONFIGURATION\""), "JSON should contain eventType");

        // Parse the JSON to verify the configuration maps instead of string matching
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("value1", jsonNode.get("previousConfiguration").get("key1").asText());
        assertEquals("value2", jsonNode.get("newConfiguration").get("key1").asText());
        assertEquals(42, jsonNode.get("newConfiguration").get("key2").asInt());

        // Deserialize from JSON
        PluginEvent deserializedEvent = objectMapper.readValue(json, PluginEvent.class);

        // Verify deserialized event
        assertTrue(deserializedEvent instanceof PluginConfigurationEvent);
        PluginConfigurationEvent configEvent = (PluginConfigurationEvent) deserializedEvent;
        assertEquals("test-plugin", configEvent.getPluginId());
        assertEquals("CONFIGURATION", configEvent.getEventType());
        assertEquals(prevConfig, configEvent.getPreviousConfiguration());
        assertEquals(newConfig, configEvent.getNewConfiguration());
    }

    @Test
    void testSerializeAndDeserializeCustomEvent() throws Exception {
        // Register our custom event types
        objectMapper.registerSubtypes(TestEvent.class, AnotherTestEvent.class);

        // Create a custom event
        TestEvent event = new TestEvent("test-plugin", "Hello, world!");

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(event);

        // Verify JSON contains expected fields
        assertTrue(json.contains("\"pluginId\":\"test-plugin\""));
        assertTrue(json.contains("\"eventType\":\"TEST_EVENT\""));
        assertTrue(json.contains("\"message\":\"Hello, world!\""));

        // Deserialize from JSON
        PluginEvent deserializedEvent = objectMapper.readValue(json, PluginEvent.class);

        // Verify deserialized event
        assertTrue(deserializedEvent instanceof TestEvent);
        TestEvent testEvent = (TestEvent) deserializedEvent;
        assertEquals("test-plugin", testEvent.getPluginId());
        assertEquals("TEST_EVENT", testEvent.getEventType());
        assertEquals("Hello, world!", testEvent.getMessage());
    }

    @Test
    void testGetObjectMapper() {
        assertNotNull(serializer.getObjectMapper());
        assertSame(objectMapper, serializer.getObjectMapper());
    }
}
