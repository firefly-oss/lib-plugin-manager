package com.catalis.document.event;

import com.catalis.document.api.Document;

/**
 * Event published when a document is successfully created.
 */
public class DocumentCreatedEvent extends DocumentEvent {
    
    private static final String EVENT_TYPE = "DOCUMENT_CREATED";
    
    /**
     * Creates a new document created event.
     *
     * @param pluginId the ID of the plugin that published the event
     * @param document the document that was created
     */
    public DocumentCreatedEvent(String pluginId, Document document) {
        super(pluginId, EVENT_TYPE, document);
    }
}