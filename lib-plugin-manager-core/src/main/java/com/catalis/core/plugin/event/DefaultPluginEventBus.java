package com.catalis.core.plugin.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Default implementation of the PluginEventBus interface.
 */
@Component
public class DefaultPluginEventBus implements PluginEventBus {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultPluginEventBus.class);
    
    private final Sinks.Many<PluginEvent> eventSink;
    private final Flux<PluginEvent> eventFlux;
    
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
}
