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


package com.firefly.core.plugin.debug;

import com.firefly.core.plugin.api.Plugin;
import com.firefly.core.plugin.api.PluginManager;
import com.firefly.core.plugin.api.PluginRegistry;
import com.firefly.core.plugin.config.PluginManagerProperties;
import com.firefly.core.plugin.debug.jdi.MockDebuggerVMManager;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.debug.PluginDebugEvent;
import com.firefly.core.plugin.model.PluginDebugEvent.Type;
import com.firefly.core.plugin.model.PluginDescriptor;
import com.firefly.core.plugin.model.PluginMetadata;
import com.firefly.core.plugin.model.PluginState;
import org.mockito.Mockito;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultPluginDebuggerTest {

    @Mock
    private PluginManager pluginManager;

    @Mock
    private PluginRegistry pluginRegistry;

    @Mock
    private PluginEventBus eventBus;

    private PluginManagerProperties properties;
    private DefaultPluginDebugger debugger;
    private AutoCloseable mocks;

    private MockDebuggerVMManager mockVMManager;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Set up properties
        properties = new PluginManagerProperties();
        properties.getDebugger().setEnabled(true);
        properties.getDebugger().setPort(8000);
        properties.getDebugger().setMaxConcurrentSessions(5);

        // Set up mock behavior
        when(pluginManager.getPluginRegistry()).thenReturn(pluginRegistry);
        when(eventBus.publish(any(PluginDebugEvent.class))).thenReturn(Mono.empty());

        // Create the mock VM manager
        mockVMManager = new MockDebuggerVMManager(eventBus);

        // Create the debugger
        debugger = new DefaultPluginDebugger(pluginManager, eventBus, properties);

        // Replace the VM manager with our mock
        setField(debugger, "vmManager", mockVMManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Stop the debugger
        debugger.stop();

        // Close mocks
        mocks.close();
    }

    @Test
    void testStartDebugSession() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        StepVerifier.create(debugger.startDebugSession(pluginId))
                .assertNext(sessionId -> {
                    assertNotNull(sessionId);
                    assertTrue(sessionId.length() > 0);
                })
                .verifyComplete();

        // Verify that a session started event was published
        ArgumentCaptor<PluginDebugEvent> eventCaptor = ArgumentCaptor.forClass(PluginDebugEvent.class);
        verify(eventBus).publish(eventCaptor.capture());

        PluginDebugEvent event = eventCaptor.getValue();
        assertEquals(pluginId, event.getPluginId());
        assertEquals(Type.SESSION_STARTED, event.getType());
        assertEquals("Debug session started for plugin: " + pluginId, event.getMessage());
    }

    @Test
    void testStopDebugSession() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Reset the mock to clear the previous invocation
        reset(eventBus);
        when(eventBus.publish(any(PluginDebugEvent.class))).thenReturn(Mono.empty());

        // Stop the debug session
        StepVerifier.create(debugger.stopDebugSession(sessionId))
                .verifyComplete();

        // Verify that a session ended event was published
        ArgumentCaptor<PluginDebugEvent> eventCaptor = ArgumentCaptor.forClass(PluginDebugEvent.class);
        verify(eventBus).publish(eventCaptor.capture());

        PluginDebugEvent event = eventCaptor.getValue();
        assertEquals(pluginId, event.getPluginId());
        assertEquals(Type.SESSION_ENDED, event.getType());
        assertEquals("Debug session ended: User requested", event.getMessage());
    }

    @Test
    void testGetActiveSessions() {
        // Set up mock behavior
        String pluginId1 = "plugin1";
        String pluginId2 = "plugin2";

        PluginDescriptor descriptor1 = PluginDescriptor.builder()
                .id(pluginId1)
                .name("Plugin 1")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        PluginDescriptor descriptor2 = PluginDescriptor.builder()
                .id(pluginId2)
                .name("Plugin 2")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId1)).thenReturn(Mono.just(descriptor1));
        when(pluginManager.getPlugin(pluginId2)).thenReturn(Mono.just(descriptor2));

        // Start two debug sessions
        String sessionId1 = debugger.startDebugSession(pluginId1).block();
        String sessionId2 = debugger.startDebugSession(pluginId2).block();

        assertNotNull(sessionId1);
        assertNotNull(sessionId2);

        // Get active sessions
        StepVerifier.create(debugger.getActiveSessions().collectList())
                .assertNext(sessions -> {
                    assertEquals(2, sessions.size());

                    // Find session for plugin1
                    Map.Entry<String, String> session1 = sessions.stream()
                            .filter(entry -> entry.getValue().equals(pluginId1))
                            .findFirst()
                            .orElse(null);
                    assertNotNull(session1);
                    assertEquals(sessionId1, session1.getKey());

                    // Find session for plugin2
                    Map.Entry<String, String> session2 = sessions.stream()
                            .filter(entry -> entry.getValue().equals(pluginId2))
                            .findFirst()
                            .orElse(null);
                    assertNotNull(session2);
                    assertEquals(sessionId2, session2.getKey());
                })
                .verifyComplete();
    }

    @Test
    void testSetBreakpoint() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Set a breakpoint
        String className = "com.example.MyClass";
        int lineNumber = 42;

        StepVerifier.create(debugger.setBreakpoint(sessionId, className, lineNumber))
                .assertNext(breakpointId -> {
                    assertNotNull(breakpointId);
                    assertTrue(breakpointId.length() > 0);
                })
                .verifyComplete();

        // Get breakpoints
        StepVerifier.create(debugger.getBreakpoints(sessionId).collectList())
                .assertNext(breakpoints -> {
                    assertEquals(1, breakpoints.size());
                    Map<String, Object> breakpoint = breakpoints.get(0);
                    assertEquals(className, breakpoint.get("className"));
                    assertEquals(lineNumber, breakpoint.get("lineNumber"));
                    assertTrue((Boolean) breakpoint.get("enabled"));
                })
                .verifyComplete();
    }

    @Test
    void testRemoveBreakpoint() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Set a breakpoint
        String className = "com.example.MyClass";
        int lineNumber = 42;
        String breakpointId = debugger.setBreakpoint(sessionId, className, lineNumber).block();
        assertNotNull(breakpointId);

        // Remove the breakpoint
        StepVerifier.create(debugger.removeBreakpoint(sessionId, breakpointId))
                .verifyComplete();

        // Get breakpoints
        StepVerifier.create(debugger.getBreakpoints(sessionId).collectList())
                .assertNext(breakpoints -> {
                    assertEquals(0, breakpoints.size());
                })
                .verifyComplete();
    }

    @Test
    void testGetVariableValue() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Get a variable value
        String variableName = "myVar";

        StepVerifier.create(debugger.getVariableValue(sessionId, variableName))
                .assertNext(value -> {
                    assertEquals("Dummy value for " + variableName, value);
                })
                .verifyComplete();

        // Verify that a variable inspected event was published
        ArgumentCaptor<PluginDebugEvent> eventCaptor = ArgumentCaptor.forClass(PluginDebugEvent.class);
        verify(eventBus, atLeastOnce()).publish(eventCaptor.capture());

        boolean foundEvent = false;
        for (PluginDebugEvent event : eventCaptor.getAllValues()) {
            if (event.getType() == Type.VARIABLE_INSPECTED) {
                foundEvent = true;
                assertEquals(pluginId, event.getPluginId());
                assertEquals(Type.VARIABLE_INSPECTED, event.getType());
                assertEquals("Variable inspected: " + variableName, event.getMessage());
                assertEquals(variableName, event.getDetails().get("variableName"));
                assertEquals("Dummy value for " + variableName, event.getDetails().get("value"));
            }
        }

        assertTrue(foundEvent, "Variable inspected event not found");
    }

    @Test
    void testGetLocalVariables() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Get local variables
        StepVerifier.create(debugger.getLocalVariables(sessionId).collectList())
                .assertNext(variables -> {
                    assertEquals(3, variables.size());

                    // Convert to a map for easier testing
                    Map<String, Object> variableMap = new HashMap<>();
                    for (Map.Entry<String, Object> entry : variables) {
                        variableMap.put(entry.getKey(), entry.getValue());
                    }

                    assertEquals("value1", variableMap.get("var1"));
                    assertEquals(42, variableMap.get("var2"));
                    assertEquals(true, variableMap.get("var3"));
                })
                .verifyComplete();
    }

    @Test
    void testEvaluateExpression() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Evaluate an expression
        String expression = "1 + 2";

        StepVerifier.create(debugger.evaluateExpression(sessionId, expression))
                .assertNext(result -> {
                    assertEquals("Result of " + expression, result);
                })
                .verifyComplete();

        // Verify that an expression evaluated event was published
        ArgumentCaptor<PluginDebugEvent> eventCaptor = ArgumentCaptor.forClass(PluginDebugEvent.class);
        verify(eventBus, atLeastOnce()).publish(eventCaptor.capture());

        boolean foundEvent = false;
        for (PluginDebugEvent event : eventCaptor.getAllValues()) {
            if (event.getType() == Type.EXPRESSION_EVALUATED) {
                foundEvent = true;
                assertEquals(pluginId, event.getPluginId());
                assertEquals(Type.EXPRESSION_EVALUATED, event.getType());
                assertEquals("Expression evaluated: " + expression, event.getMessage());
                assertEquals(expression, event.getDetails().get("expression"));
                assertEquals("Result of " + expression, event.getDetails().get("result"));
            }
        }

        assertTrue(foundEvent, "Expression evaluated event not found");
    }

    @Test
    void testGetStackTrace() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Get stack trace
        StepVerifier.create(debugger.getStackTrace(sessionId).collectList())
                .assertNext(stackTrace -> {
                    assertEquals(2, stackTrace.size());

                    // Check first frame
                    Map<String, Object> frame1 = stackTrace.get(0);
                    assertEquals("com.example.MyClass", frame1.get("className"));
                    assertEquals("myMethod", frame1.get("methodName"));
                    assertEquals("MyClass.java", frame1.get("fileName"));
                    assertEquals(42, frame1.get("lineNumber"));

                    // Check second frame
                    Map<String, Object> frame2 = stackTrace.get(1);
                    assertEquals("com.example.OtherClass", frame2.get("className"));
                    assertEquals("otherMethod", frame2.get("methodName"));
                    assertEquals("OtherClass.java", frame2.get("fileName"));
                    assertEquals(123, frame2.get("lineNumber"));
                })
                .verifyComplete();
    }

    @Test
    void testGetPluginInfo() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Get plugin info
        StepVerifier.create(debugger.getPluginInfo(sessionId))
                .assertNext(info -> {
                    assertEquals(pluginId, info.getId());
                    assertEquals("Test Plugin", info.getName());
                    assertEquals("1.0.0", info.getVersion());
                    assertEquals(PluginState.STARTED, info.getState());
                })
                .verifyComplete();
    }

    @Test
    void testIsDebuggingEnabled() {
        // Test with debugging enabled globally
        properties.getDebugger().setEnabled(true);

        StepVerifier.create(debugger.isDebuggingEnabled("test-plugin"))
                .assertNext(enabled -> {
                    assertTrue(enabled);
                })
                .verifyComplete();

        // Test with debugging disabled globally
        properties.getDebugger().setEnabled(false);

        StepVerifier.create(debugger.isDebuggingEnabled("test-plugin"))
                .assertNext(enabled -> {
                    assertFalse(enabled);
                })
                .verifyComplete();

        // Test with debugging enabled globally but disabled for a specific plugin
        properties.getDebugger().setEnabled(true);
        PluginManagerProperties.PluginDebuggerProperties pluginProps = new PluginManagerProperties.PluginDebuggerProperties();
        pluginProps.setEnabled(false);
        properties.getDebugger().getPlugins().put("test-plugin", pluginProps);

        StepVerifier.create(debugger.isDebuggingEnabled("test-plugin"))
                .assertNext(enabled -> {
                    assertFalse(enabled);
                })
                .verifyComplete();
    }

    @Test
    void testSetDebuggingEnabled() {
        // Set debugging enabled for a plugin
        StepVerifier.create(debugger.setDebuggingEnabled("test-plugin", true))
                .verifyComplete();

        // Verify that debugging is enabled
        StepVerifier.create(debugger.isDebuggingEnabled("test-plugin"))
                .assertNext(enabled -> {
                    assertTrue(enabled);
                })
                .verifyComplete();

        // Set debugging disabled for a plugin
        StepVerifier.create(debugger.setDebuggingEnabled("test-plugin", false))
                .verifyComplete();

        // Verify that debugging is disabled
        StepVerifier.create(debugger.isDebuggingEnabled("test-plugin"))
                .assertNext(enabled -> {
                    assertFalse(enabled);
                })
                .verifyComplete();
    }

    @Test
    void testContinueExecution() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Continue execution
        StepVerifier.create(debugger.continueExecution(sessionId))
                .verifyComplete();
    }

    @Test
    void testStepOver() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Step over
        StepVerifier.create(debugger.stepOver(sessionId))
                .verifyComplete();
    }

    @Test
    void testStepInto() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Step into
        StepVerifier.create(debugger.stepInto(sessionId))
                .verifyComplete();
    }

    @Test
    void testStepOut() {
        // Set up mock behavior
        String pluginId = "test-plugin";
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .id(pluginId)
                .name("Test Plugin")
                .version("1.0.0")
                .state(PluginState.STARTED)
                .build();

        when(pluginManager.getPlugin(pluginId)).thenReturn(Mono.just(descriptor));

        // Start a debug session
        String sessionId = debugger.startDebugSession(pluginId).block();
        assertNotNull(sessionId);

        // Step out
        StepVerifier.create(debugger.stepOut(sessionId))
                .verifyComplete();
    }

    @Test
    void testDebuggerDisabled() {
        // Disable the debugger
        properties.getDebugger().setEnabled(false);

        // Try to start a debug session
        StepVerifier.create(debugger.startDebugSession("test-plugin"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void testMaxConcurrentSessions() {
        // Set up mock behavior
        when(pluginManager.getPlugin(anyString())).thenAnswer(invocation -> {
            String pluginId = invocation.getArgument(0);
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .id(pluginId)
                    .name("Plugin " + pluginId)
                    .version("1.0.0")
                    .state(PluginState.STARTED)
                    .build();
            return Mono.just(descriptor);
        });

        // Set max concurrent sessions to 2
        properties.getDebugger().setMaxConcurrentSessions(2);

        // Start two debug sessions
        String sessionId1 = debugger.startDebugSession("plugin1").block();
        String sessionId2 = debugger.startDebugSession("plugin2").block();

        assertNotNull(sessionId1);
        assertNotNull(sessionId2);

        // Try to start a third session
        StepVerifier.create(debugger.startDebugSession("plugin3"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    /**
     * Sets a private field on an object using reflection.
     *
     * @param target the target object
     * @param fieldName the name of the field
     * @param value the value to set
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    /**
     * Finds a field in a class or its superclasses.
     *
     * @param clazz the class to search
     * @param fieldName the name of the field
     * @return the field
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            throw new RuntimeException("Field not found: " + fieldName, e);
        }
    }
}
