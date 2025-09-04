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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for the plugin event bus, which allows publishing and subscribing to plugin events.
 * This interface is transport-agnostic and can be implemented using different messaging systems
 * such as in-memory, Kafka, RabbitMQ, etc.
 */
public interface PluginEventBus {

    /**
     * Publishes an event to the event bus.
     *
     * @param event the event to publish
     * @return a Mono that completes when the event has been published
     */
    Mono<Void> publish(PluginEvent event);

    /**
     * Publishes an event to a specific topic on the event bus.
     *
     * @param topic the topic to publish to
     * @param event the event to publish
     * @return a Mono that completes when the event has been published
     */
    Mono<Void> publish(String topic, PluginEvent event);

    /**
     * Subscribes to all events of a specific type.
     *
     * @param eventType the class of events to subscribe to
     * @param <T> the event type
     * @return a Flux of events of the specified type
     */
    <T extends PluginEvent> Flux<T> subscribe(Class<T> eventType);

    /**
     * Subscribes to all events for a specific plugin.
     *
     * @param pluginId the ID of the plugin
     * @return a Flux of events for the specified plugin
     */
    Flux<PluginEvent> subscribeToPlugin(String pluginId);

    /**
     * Subscribes to events of a specific type for a specific plugin.
     *
     * @param pluginId the ID of the plugin
     * @param eventType the class of events to subscribe to
     * @param <T> the event type
     * @return a Flux of events of the specified type for the specified plugin
     */
    <T extends PluginEvent> Flux<T> subscribeToPlugin(String pluginId, Class<T> eventType);

    /**
     * Subscribes to events on a specific topic.
     *
     * @param topic the topic to subscribe to
     * @return a Flux of events from the specified topic
     */
    Flux<PluginEvent> subscribeTopic(String topic);

    /**
     * Subscribes to events of a specific type on a specific topic.
     *
     * @param topic the topic to subscribe to
     * @param eventType the class of events to subscribe to
     * @param <T> the event type
     * @return a Flux of events of the specified type from the specified topic
     */
    <T extends PluginEvent> Flux<T> subscribeTopic(String topic, Class<T> eventType);

    /**
     * Gets the transport type of this event bus implementation.
     *
     * @return the transport type (e.g., "in-memory", "kafka", "rabbitmq")
     */
    String getTransportType();

    /**
     * Initializes the event bus.
     *
     * @return a Mono that completes when the event bus has been initialized
     */
    Mono<Void> initialize();

    /**
     * Shuts down the event bus.
     *
     * @return a Mono that completes when the event bus has been shut down
     */
    Mono<Void> shutdown();
}
