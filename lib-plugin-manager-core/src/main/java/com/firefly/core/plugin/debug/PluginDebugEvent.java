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

import com.firefly.core.plugin.event.PluginEvent;
import com.firefly.core.plugin.model.PluginDebugEvent.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event that is published when a debug event occurs.
 */
public class PluginDebugEvent extends PluginEvent {

    private static final String EVENT_TYPE = "plugin.debug";
    private final String sessionId;
    private final Type type;
    private final String message;
    private final Map<String, Object> details;

    /**
     * Creates a new PluginDebugEvent.
     *
     * @param sessionId the debug session ID
     * @param pluginId the plugin ID
     * @param type the debug event type
     * @param message a message describing the event
     * @param details additional details about the event
     */
    public PluginDebugEvent(String sessionId, String pluginId, Type type, String message, Map<String, Object> details) {
        super(pluginId, EVENT_TYPE);
        this.sessionId = sessionId;
        this.type = type;
        this.message = message;
        this.details = details != null ? new HashMap<>(details) : new HashMap<>();
    }

    /**
     * Gets the debug session ID.
     *
     * @return the debug session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Gets the debug event type.
     *
     * @return the debug event type
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the message describing the event.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the additional details about the event.
     *
     * @return the details
     */
    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    @Override
    public String toString() {
        return "PluginDebugEvent{" +
                "pluginId='" + getPluginId() + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", type=" + type +
                ", message='" + message + '\'' +
                ", timestamp=" + getTimestamp() +
                ", details=" + details +
                '}';
    }
}
