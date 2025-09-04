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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory implementation of the PluginEventBus interface.
 * This implementation uses Project Reactor's Sinks for event publishing and subscription.
 */
@Component
@ConditionalOnProperty(name = "firefly.plugin-manager.event-bus.type", havingValue = "in-memory", matchIfMissing = true)
public class DefaultPluginEventBus implements PluginEventBus {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginEventBus.class);
    private static final String TRANSPORT_TYPE = "in-memory";

    private final Sinks.Many<PluginEvent> eventSink;
    private final Flux<PluginEvent> eventFlux;
    private final Map<String, Sinks.Many<PluginEvent>> topicSinks = new ConcurrentHashMap<>();

    /**
     * Creates a new DefaultPluginEventBus.
     */
    public DefaultPluginEventBus() {
        this.eventSink = Sinks.many().multicast().onBackpressureBuffer();
        this.eventFlux = eventSink.asFlux().cache(100);
    }

    @Override
    public Mono<Void> publish(PluginEvent event) {
        logger.debug("Publishing event: {} for plugin: {}", event.getEventType(), event.getPluginId());

        return Mono.fromRunnable(() -> {
            Sinks.EmitResult result = eventSink.tryEmitNext(event);
            if (result.isFailure()) {
                logger.error("Failed to publish event: {}", result);
            }
        });
    }

    @Override
    public Mono<Void> publish(String topic, PluginEvent event) {
        logger.debug("Publishing event to topic {}: {} for plugin: {}",
                topic, event.getEventType(), event.getPluginId());

        return Mono.fromRunnable(() -> {
            Sinks.Many<PluginEvent> topicSink = topicSinks.computeIfAbsent(topic,
                    t -> Sinks.many().multicast().onBackpressureBuffer());

            Sinks.EmitResult result = topicSink.tryEmitNext(event);
            if (result.isFailure()) {
                logger.error("Failed to publish event to topic {}: {}", topic, result);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PluginEvent> Flux<T> subscribe(Class<T> eventType) {
        return eventFlux
                .filter(event -> eventType.isInstance(event))
                .map(event -> (T) event);
    }

    @Override
    public Flux<PluginEvent> subscribeToPlugin(String pluginId) {
        return eventFlux
                .filter(event -> event.getPluginId().equals(pluginId));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PluginEvent> Flux<T> subscribeToPlugin(String pluginId, Class<T> eventType) {
        return eventFlux
                .filter(event -> event.getPluginId().equals(pluginId))
                .filter(event -> eventType.isInstance(event))
                .map(event -> (T) event);
    }

    @Override
    public Flux<PluginEvent> subscribeTopic(String topic) {
        return Flux.defer(() -> {
            Sinks.Many<PluginEvent> topicSink = topicSinks.computeIfAbsent(topic,
                    t -> Sinks.many().multicast().onBackpressureBuffer());
            return topicSink.asFlux();
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PluginEvent> Flux<T> subscribeTopic(String topic, Class<T> eventType) {
        return subscribeTopic(topic)
                .filter(event -> eventType.isInstance(event))
                .map(event -> (T) event);
    }

    @Override
    public String getTransportType() {
        return TRANSPORT_TYPE;
    }

    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing in-memory event bus");
        return Mono.empty(); // Nothing to initialize for in-memory implementation
    }

    @Override
    public Mono<Void> shutdown() {
        logger.info("Shutting down in-memory event bus");
        return Mono.fromRunnable(() -> {
            // Complete all sinks
            eventSink.tryEmitComplete();
            topicSinks.values().forEach(Sinks.Many::tryEmitComplete);
        });
    }
}
