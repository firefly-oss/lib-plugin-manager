package com.catalis.document.api;

import com.catalis.core.plugin.annotation.ExtensionPoint;
import reactor.core.publisher.Mono;

/**
 * Extension point for document management operations.
 * This extension point allows plugins to implement document creation functionality.
 */
@ExtensionPoint(
        id = "com.catalis.document.document-management",
        description = "Extension point for document management operations",
        allowMultiple = true
)
public interface DocumentManagementExtensionPoint {
    
    /**
     * Creates a new document in the system.
     *
     * @param document the document to create
     * @return a Mono that emits the created document with its ID assigned
     */
    Mono<Document> createDocument(Document document);
    
}