package com.catalis.document.event;

import com.catalis.core.plugin.event.PluginEvent;
import com.catalis.document.api.Document;

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