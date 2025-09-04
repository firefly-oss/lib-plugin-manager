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


package com.firefly.document.event;

import com.firefly.core.plugin.event.PluginEvent;
import com.firefly.document.api.Document;

/**
 * Base class for document-related events.
 */
public abstract class DocumentEvent extends PluginEvent {
    
    private final Document document;
    
    /**
     * Creates a new document event.
     *
     * @param pluginId the ID of the plugin that published the event
     * @param eventType the type of the event
     * @param document the document associated with the event
     */
    protected DocumentEvent(String pluginId, String eventType, Document document) {
        super(pluginId, eventType);
        this.document = document;
    }
    
    /**
     * Gets the document associated with this event.
     *
     * @return the document
     */
    public Document getDocument() {
        return document;
    }
}