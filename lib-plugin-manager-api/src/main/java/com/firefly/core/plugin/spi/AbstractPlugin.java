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


package com.firefly.core.plugin.spi;

import com.firefly.core.plugin.api.Plugin;
import com.firefly.core.plugin.model.PluginMetadata;
import reactor.core.publisher.Mono;

/**
 * Abstract base class for plugins that provides default implementations of the Plugin interface.
 * Plugin developers can extend this class to create their own plugins.
 */
public abstract class AbstractPlugin implements Plugin {
    
    private final PluginMetadata metadata;
    
    /**
     * Creates a new abstract plugin with the specified metadata.
     * 
     * @param metadata the plugin metadata
     */
    protected AbstractPlugin(PluginMetadata metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public Mono<Void> initialize() {
        // Default implementation does nothing
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> start() {
        // Default implementation does nothing
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> stop() {
        // Default implementation does nothing
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> uninstall() {
        // Default implementation does nothing
        return Mono.empty();
    }
}
