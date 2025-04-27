package com.catalis.core.plugin.event;

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
        private final String message;

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
    }

    private static class AnotherTestEvent extends PluginEvent {
        private final int value;

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
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
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
                com.catalis.core.plugin.model.PluginState.INITIALIZED,
                com.catalis.core.plugin.model.PluginState.STARTED
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
        assertEquals(com.catalis.core.plugin.model.PluginState.INITIALIZED, lifecycleEvent.getPreviousState());
        assertEquals(com.catalis.core.plugin.model.PluginState.STARTED, lifecycleEvent.getNewState());
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
        assertTrue(json.contains("\"pluginId\":\"test-plugin\""));
        assertTrue(json.contains("\"eventType\":\"CONFIGURATION\""));
        assertTrue(json.contains("\"previousConfiguration\":{\"key1\":\"value1\"}"));
        assertTrue(json.contains("\"newConfiguration\":{\"key1\":\"value2\",\"key2\":42}"));
        
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
