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


package com.firefly.core.plugin.debug.jdi;

import com.firefly.core.plugin.event.PluginEventBus;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A mock implementation of the DebuggerVMManager for testing.
 */
public class MockDebuggerVMManager extends DebuggerVMManager {

    private final Map<String, MockDebuggerConnection> connections = new ConcurrentHashMap<>();

    public MockDebuggerVMManager(PluginEventBus eventBus) {
        super(eventBus);
    }

    @Override
    public Mono<Void> connectToVM(String sessionId, String pluginId, int port) {
        return Mono.defer(() -> {
            MockDebuggerConnection connection = new MockDebuggerConnection(sessionId, pluginId, port);
            connections.put(sessionId, connection);
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> disconnectFromVM(String sessionId) {
        return Mono.defer(() -> {
            connections.remove(sessionId);
            return Mono.empty();
        });
    }

    @Override
    public DebuggerConnection getConnection(String sessionId) {
        return connections.get(sessionId);
    }

    @Override
    public boolean hasConnection(String sessionId) {
        return connections.containsKey(sessionId);
    }

    @Override
    public Mono<Void> disconnectAll() {
        return Mono.defer(() -> {
            connections.clear();
            return Mono.empty();
        });
    }

    /**
     * A mock implementation of the DebuggerConnection for testing.
     */
    public static class MockDebuggerConnection extends DebuggerConnection {

        private final Map<String, String> breakpoints = new HashMap<>();
        private boolean suspended = false;

        public MockDebuggerConnection(String sessionId, String pluginId, int port) {
            super(sessionId, pluginId, port);
        }

        @Override
        public Mono<String> setBreakpoint(String className, int lineNumber) {
            return Mono.defer(() -> {
                String breakpointId = "bp-" + className + "-" + lineNumber;
                breakpoints.put(breakpointId, className + ":" + lineNumber);
                return Mono.just(breakpointId);
            });
        }

        @Override
        public Mono<Void> removeBreakpoint(String breakpointId) {
            return Mono.defer(() -> {
                breakpoints.remove(breakpointId);
                return Mono.empty();
            });
        }

        @Override
        public Map<String, Map<String, Object>> getBreakpoints() {
            Map<String, Map<String, Object>> result = new HashMap<>();
            for (Map.Entry<String, String> entry : breakpoints.entrySet()) {
                String breakpointId = entry.getKey();
                String[] parts = entry.getValue().split(":");
                String className = parts[0];
                int lineNumber = Integer.parseInt(parts[1]);

                Map<String, Object> breakpointInfo = new HashMap<>();
                breakpointInfo.put("id", breakpointId);
                breakpointInfo.put("className", className);
                breakpointInfo.put("lineNumber", lineNumber);
                breakpointInfo.put("enabled", true);
                breakpointInfo.put("methodName", "testMethod");

                result.put(breakpointId, breakpointInfo);
            }
            return result;
        }

        @Override
        public Mono<Void> continueExecution() {
            return Mono.defer(() -> {
                suspended = false;
                return Mono.empty();
            });
        }

        @Override
        public Mono<Void> stepOver() {
            return Mono.defer(() -> {
                suspended = true;
                return Mono.empty();
            });
        }

        @Override
        public Mono<Void> stepInto() {
            return Mono.defer(() -> {
                suspended = true;
                return Mono.empty();
            });
        }

        @Override
        public Mono<Void> stepOut() {
            return Mono.defer(() -> {
                suspended = true;
                return Mono.empty();
            });
        }

        @Override
        public Object getVariableValue(String variableName) {
            return "Dummy value for " + variableName;
        }

        @Override
        public Map<String, Object> getLocalVariables() {
            Map<String, Object> variables = new HashMap<>();
            variables.put("var1", "value1");
            variables.put("var2", 42);
            variables.put("var3", true);
            return variables;
        }

        @Override
        public Object evaluateExpression(String expression) {
            return "Result of " + expression;
        }

        @Override
        public java.util.List<Map<String, Object>> getStackTrace() {
            java.util.List<Map<String, Object>> stackTrace = new java.util.ArrayList<>();

            Map<String, Object> frame1 = new HashMap<>();
            frame1.put("className", "com.example.MyClass");
            frame1.put("methodName", "myMethod");
            frame1.put("fileName", "MyClass.java");
            frame1.put("lineNumber", 42);

            Map<String, Object> frame2 = new HashMap<>();
            frame2.put("className", "com.example.OtherClass");
            frame2.put("methodName", "otherMethod");
            frame2.put("fileName", "OtherClass.java");
            frame2.put("lineNumber", 123);

            stackTrace.add(frame1);
            stackTrace.add(frame2);

            return stackTrace;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public boolean isSuspended() {
            return suspended;
        }
    }
}
