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


package com.firefly.document;

import com.firefly.core.plugin.annotation.Plugin;
import com.firefly.core.plugin.annotation.Extension;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.model.PluginMetadata;
import com.firefly.core.plugin.spi.AbstractPlugin;
import com.firefly.document.api.Document;
import com.firefly.document.api.DocumentManagementExtensionPoint;
import com.firefly.document.event.DocumentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.Map;

/**
 * Plugin for document management operations.
 * This plugin provides functionality for creating and managing documents.
 */
@Plugin(
        id = "com.firefly.document.document-mgmt-plugin",
        name = "Document Management Plugin",
        version = "1.0.0",
        description = "Provides document management capabilities including document creation",
        author = "Firefly Financial Services"
)
public class DocumentManagementPlugin extends AbstractPlugin {

    private static final Logger logger = LoggerFactory.getLogger(DocumentManagementPlugin.class);

    private final PluginEventBus eventBus;
    private final DocumentStorageClient storageClient;
    private Disposable eventSubscription;

    /**
     * Creates a new DocumentManagementPlugin.
     *
     * @param eventBus the event bus for inter-plugin communication
     */
    public DocumentManagementPlugin(PluginEventBus eventBus) {
        super(PluginMetadata.builder()
                .id("com.firefly.document.document-mgmt-plugin")
                .name("Document Management Plugin")
                .version("1.0.0")
                .description("Provides document management capabilities including document creation")
                .author("Firefly Financial Services")
                .build());

        this.eventBus = eventBus;
        this.storageClient = new DocumentStorageClient();
    }

    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing Document Management Plugin");

        // Get configuration from the plugin manager
        return Mono.fromRunnable(() -> {
            try {
                Map<String, Object> config = pluginManager.getPlugin(getMetadata().id())
                        .block()
                        .configuration();

                // Parse configuration
                String storageLocation = (String) config.getOrDefault("storageLocation", "default-storage");
                int connectionTimeout = (int) config.getOrDefault("connectionTimeout", 30);

                // Initialize the storage client
                storageClient.initialize(storageLocation, connectionTimeout);
                logger.info("Document storage client initialized with location: {}", storageLocation);
            } catch (Exception e) {
                logger.error("Failed to initialize document storage client", e);
                throw e;
            }
        });
    }

    @Override
    public Mono<Void> start() {
        logger.info("Starting Document Management Plugin");

        // Subscribe to relevant events
        eventSubscription = eventBus.subscribe(DocumentCreatedEvent.class)
                .subscribe(event -> {
                    logger.info("Received document created event: documentId={}, documentType={}",
                            event.getDocument().getId(), event.getDocument().getDocumentType());
                });

        // Connect to the document storage
        return storageClient.connect()
                .doOnSuccess(v -> logger.info("Connected to document storage"))
                .doOnError(e -> logger.error("Failed to connect to document storage", e));
    }

    @Override
    public Mono<Void> stop() {
        logger.info("Stopping Document Management Plugin");

        // Dispose of event subscriptions
        if (eventSubscription != null && !eventSubscription.isDisposed()) {
            eventSubscription.dispose();
        }

        // Disconnect from the document storage
        return storageClient.disconnect()
                .doOnSuccess(v -> logger.info("Disconnected from document storage"))
                .doOnError(e -> logger.error("Error disconnecting from document storage", e));
    }

    @Override
    public Mono<Void> uninstall() {
        logger.info("Uninstalling Document Management Plugin");

        // Clean up any persistent resources
        return Mono.fromRunnable(() -> {
            // Remove any stored data, close connections, etc.
            logger.info("Cleaned up resources for Document Management Plugin");
        });
    }

    /**
     * Gets the document manager extension.
     *
     * @return the document manager
     */
    public DocumentManager getDocumentManager() {
        return new DocumentManager();
    }

    /**
     * Implementation of the DocumentManagementExtensionPoint.
     */
    @Extension(
            extensionPointId = "com.firefly.document.document-management",
            priority = 100,
            description = "Standard document management implementation"
    )
    public class DocumentManager implements DocumentManagementExtensionPoint {

        @Override
        public Mono<Document> createDocument(Document document) {
            logger.info("Creating document: name={}, type={}", document.getName(), document.getDocumentType());

            // Validate inputs
            if (document.getName() == null || document.getName().isEmpty()) {
                return Mono.error(new IllegalArgumentException("Document name cannot be empty"));
            }

            if (!supportsDocumentType(document.getDocumentType())) {
                return Mono.error(new UnsupportedOperationException("Document type not supported: " + document.getDocumentType()));
            }

            // Generate an ID if not provided
            if (document.getId() == null) {
                document.setId(UUID.randomUUID().toString());
            }

            // Store the document
            return storageClient.storeDocument(document)
                    .flatMap(storedDoc -> {
                        logger.info("Document created successfully: id={}", storedDoc.getId());

                        // Publish document created event
                        return eventBus.publish(new DocumentCreatedEvent(getMetadata().id(), storedDoc))
                                .thenReturn(storedDoc);
                    })
                    .doOnError(error -> {
                        logger.error("Document creation failed: {}", error.getMessage());
                    });
        }
    }

    /**
     * Mock document storage client for demonstration purposes.
     */
    private static class DocumentStorageClient {

        void initialize(String storageLocation, int connectionTimeout) {
            // Simulate initialization
            logger.info("Initializing document storage client with location: {} and timeout: {}s", 
                    storageLocation, connectionTimeout);
        }

        Mono<Void> connect() {
            // Simulate connection establishment
            return Mono.delay(Duration.ofMillis(300)).then();
        }

        Mono<Void> disconnect() {
            // Simulate disconnection
            return Mono.delay(Duration.ofMillis(200)).then();
        }

        Mono<Document> storeDocument(Document document) {
            // Simulate document storage
            return Mono.delay(Duration.ofMillis(500))
                    .map(ignored -> {
                        document.setUpdatedAt(java.time.LocalDateTime.now());
                        return document;
                    });
        }
    }
}
