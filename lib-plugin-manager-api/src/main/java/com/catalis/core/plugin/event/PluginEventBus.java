package com.catalis.core.plugin.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for the plugin event bus, which allows publishing and subscribing to plugin events.
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
}
