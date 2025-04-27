package com.catalis.core.plugin.api;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for the extension registry, which manages extension points and their implementations.
 */
public interface ExtensionRegistry {
    
    /**
     * Registers an extension point.
     * 
     * @param extensionPointId the ID of the extension point
     * @param extensionPointClass the class of the extension point
     * @param <T> the type of the extension point
     * @return a Mono that completes when the extension point has been registered
     */
    <T> Mono<Void> registerExtensionPoint(String extensionPointId, Class<T> extensionPointClass);
    
    /**
     * Registers an extension implementation.
     * 
     * @param extensionPointId the ID of the extension point
     * @param extension the extension implementation
     * @param priority the priority of the extension
     * @param <T> the type of the extension point
     * @return a Mono that completes when the extension has been registered
     */
    <T> Mono<Void> registerExtension(String extensionPointId, T extension, int priority);
    
    /**
     * Unregisters an extension implementation.
     * 
     * @param extensionPointId the ID of the extension point
     * @param extension the extension implementation to unregister
     * @param <T> the type of the extension point
     * @return a Mono that completes when the extension has been unregistered
     */
    <T> Mono<Void> unregisterExtension(String extensionPointId, T extension);
    
    /**
     * Gets all extensions for an extension point.
     * 
     * @param extensionPointId the ID of the extension point
     * @param <T> the type of the extension point
     * @return a Flux of all extensions for the extension point
     */
    <T> Flux<T> getExtensions(String extensionPointId);
    
    /**
     * Gets the highest priority extension for an extension point.
     * 
     * @param extensionPointId the ID of the extension point
     * @param <T> the type of the extension point
     * @return a Mono that emits the highest priority extension, or an empty Mono if no extensions exist
     */
    <T> Mono<T> getHighestPriorityExtension(String extensionPointId);
    
    /**
     * Gets all extension points.
     * 
     * @return a Flux of all registered extension point IDs
     */
    Flux<String> getExtensionPoints();
}
