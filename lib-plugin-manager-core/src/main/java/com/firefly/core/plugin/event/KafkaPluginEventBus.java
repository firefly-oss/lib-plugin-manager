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

import com.firefly.core.plugin.config.PluginManagerProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka-based implementation of the PluginEventBus interface.
 * This implementation uses Reactor Kafka for event publishing and subscription.
 *
 * This implementation is optional and requires:
 * 1. Kafka dependencies on the classpath (reactor-kafka, kafka-clients)
 * 2. A running Kafka broker
 * 3. Configuration property firefly.plugin-manager.event-bus.type=kafka
 */
@Component
@ConditionalOnClass(name = {"reactor.kafka.sender.KafkaSender", "org.apache.kafka.clients.producer.KafkaProducer"})
@ConditionalOnProperty(name = "firefly.plugin-manager.event-bus.type", havingValue = "kafka")
public class KafkaPluginEventBus implements PluginEventBus {

    private static final Logger logger = LoggerFactory.getLogger(KafkaPluginEventBus.class);
    private static final String TRANSPORT_TYPE = "kafka";
    private final ObjectMapper objectMapper;
    private final PluginManagerProperties.KafkaProperties properties;
    private final Map<String, Sinks.Many<PluginEvent>> eventSinksByTopic = new ConcurrentHashMap<>();
    private final Map<String, Flux<PluginEvent>> eventFluxesByTopic = new ConcurrentHashMap<>();

    private KafkaSender<String, String> sender;
    private Map<String, KafkaReceiver<String, String>> receivers = new ConcurrentHashMap<>();

    /**
     * Creates a new KafkaPluginEventBus.
     *
     * @param objectMapper the object mapper for serializing/deserializing events
     * @param properties the plugin manager properties
     */
    @Autowired
    public KafkaPluginEventBus(ObjectMapper objectMapper, PluginManagerProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties.getEventBus().getKafka();
    }

    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing Kafka event bus with bootstrap servers: {}", properties.getBootstrapServers());

        // Create Kafka sender
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, "firefly-plugin-manager-producer-" + UUID.randomUUID());

        SenderOptions<String, String> senderOptions = SenderOptions.create(producerProps);
        sender = KafkaSender.create(senderOptions);

        // Subscribe to the default topic
        return subscribeTopic(properties.getDefaultTopic()).then();
    }

    @Override
    public Mono<Void> shutdown() {
        logger.info("Shutting down Kafka event bus");

        return Mono.fromRunnable(() -> {
            // Close the sender
            if (sender != null) {
                sender.close();
            }

            // Close all receivers
            receivers.values().forEach(receiver -> {
                // KafkaReceiver doesn't have a close method, but it's disposable
                // so we can dispose it to clean up resources
                if (receiver instanceof reactor.core.Disposable disposable) {
                    disposable.dispose();
                }
            });
            receivers.clear();

            // Complete all sinks
            eventSinksByTopic.values().forEach(Sinks.Many::tryEmitComplete);
            eventSinksByTopic.clear();
        });
    }

    @Override
    public Mono<Void> publish(PluginEvent event) {
        return publish(properties.getDefaultTopic(), event);
    }

    @Override
    public Mono<Void> publish(String topic, PluginEvent event) {
        logger.debug("Publishing event to Kafka topic {}: {} for plugin: {}",
                topic, event.getEventType(), event.getPluginId());

        return Mono.fromCallable(() -> {
            try {
                // Serialize the event to JSON
                String eventJson = objectMapper.writeValueAsString(event);

                // Create a producer record with the event type as the key
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        topic, event.getEventType(), eventJson);

                // Create a sender record
                return SenderRecord.create(record, event.getEventType());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize event", e);
            }
        }).flatMapMany(senderRecord -> sender.send(Mono.just(senderRecord)))
          .then();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PluginEvent> Flux<T> subscribe(Class<T> eventType) {
        return subscribeTopic(properties.getDefaultTopic())
                .filter(event -> eventType.isInstance(event))
                .map(event -> (T) event);
    }

    @Override
    public Flux<PluginEvent> subscribeToPlugin(String pluginId) {
        return subscribeTopic(properties.getDefaultTopic())
                .filter(event -> event.getPluginId().equals(pluginId));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends PluginEvent> Flux<T> subscribeToPlugin(String pluginId, Class<T> eventType) {
        return subscribeTopic(properties.getDefaultTopic())
                .filter(event -> event.getPluginId().equals(pluginId))
                .filter(event -> eventType.isInstance(event))
                .map(event -> (T) event);
    }

    @Override
    public Flux<PluginEvent> subscribeTopic(String topic) {
        return eventFluxesByTopic.computeIfAbsent(topic, this::createTopicSubscription);
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

    /**
     * Creates a subscription to a Kafka topic.
     *
     * @param topic the topic to subscribe to
     * @return a Flux of events from the topic
     */
    private Flux<PluginEvent> createTopicSubscription(String topic) {
        logger.info("Creating subscription to Kafka topic: {}", topic);

        // Create a sink for this topic
        Sinks.Many<PluginEvent> topicSink = Sinks.many().multicast().onBackpressureBuffer();
        eventSinksByTopic.put(topic, topicSink);

        // Create consumer properties
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getConsumerGroupId() + "-" + topic);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        // Create receiver options
        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(consumerProps)
                .subscription(Collections.singleton(topic))
                .commitInterval(Duration.ofSeconds(1));

        // Create and store the receiver
        KafkaReceiver<String, String> receiver = KafkaReceiver.create(receiverOptions);
        receivers.put(topic, receiver);

        // Start consuming from the topic
        receiver.receive()
                .flatMap(record -> {
                    try {
                        // Deserialize the event from JSON
                        String eventJson = record.value();
                        PluginEvent event = objectMapper.readValue(eventJson, PluginEvent.class);

                        // Emit the event to the sink
                        Sinks.EmitResult result = topicSink.tryEmitNext(event);
                        if (result.isFailure()) {
                            logger.error("Failed to emit event from Kafka topic {}: {}", topic, result);
                        }

                        return Mono.empty();
                    } catch (Exception e) {
                        logger.error("Failed to process event from Kafka topic {}: {}", topic, e.getMessage(), e);
                        return Mono.error(e);
                    }
                })
                .onErrorContinue((error, obj) -> {
                    logger.error("Error in Kafka receiver for topic {}: {}", topic, error.getMessage(), error);
                })
                .subscribe();

        // Return the flux from the sink
        return topicSink.asFlux();
    }
}
