# Document Management Plugin

A plugin for the Firefly Plugin Manager that provides document management capabilities, including document creation.

## Overview

The Document Management Plugin allows applications to create and manage documents through a standardized API. It implements the `DocumentManagementExtensionPoint` extension point and provides a `createDocument` operation as required.

## Features

- Create documents with various types (PDF, DOCX, TXT, XML, JSON, HTML)
- Event-based notifications for document operations
- Configurable document storage location
- Support for document metadata

## Installation

To use this plugin in your project, you need to:

1. Build the plugin:
   ```bash
   cd document-mgmt-plugin
   mvn clean package
   ```

2. Add the plugin JAR to your application's plugins directory:
   ```bash
   cp target/document-mgmt-plugin-1.0.0.jar /path/to/your/app/plugins/
   ```

3. Configure the plugin in your application's `application.properties` or `application.yml`:
   ```properties
   # Document Management Plugin Configuration
   firefly.plugin-manager.plugins.com\.catalis\.document\.document-mgmt-plugin.storageLocation=my-document-storage
   firefly.plugin-manager.plugins.com\.catalis\.document\.document-mgmt-plugin.connectionTimeout=60
   ```

## Usage

### Creating a Document

To create a document using the plugin:

```java
// Get the plugin manager
PluginManager pluginManager = ... // Inject or obtain the plugin manager

// Get all document management extensions
pluginManager.getExtensions("com.catalis.document.document-management", DocumentManagementExtensionPoint.class)
    .flatMap(extension -> {
        // Create a document
        Document document = new Document(
            "Sample Document",
            "This is the content of the document",
            "PDF",
            "application/pdf"
        );
        
        // Add metadata if needed
        document.addMetadata("author", "John Doe");
        document.addMetadata("department", "Finance");
        
        // Create the document using the extension
        return extension.createDocument(document);
    })
    .subscribe(createdDocument -> {
        System.out.println("Document created with ID: " + createdDocument.getId());
    });
```

### Subscribing to Document Events

To subscribe to document creation events:

```java
// Get the plugin event bus
PluginEventBus eventBus = ... // Inject or obtain the event bus

// Subscribe to document created events
eventBus.subscribe(DocumentCreatedEvent.class)
    .subscribe(event -> {
        Document document = event.getDocument();
        System.out.println("Document created: " + document.getName() + 
                           " (ID: " + document.getId() + ")");
    });
```

## Extension Point

This plugin implements the `DocumentManagementExtensionPoint` extension point:

```java
@ExtensionPoint(
        id = "com.catalis.document.document-management",
        description = "Extension point for document management operations",
        allowMultiple = true
)
public interface DocumentManagementExtensionPoint {
    Mono<Document> createDocument(Document document);
    String getImplementationName();
    boolean supportsDocumentType(String documentType);
}
```

## Configuration Properties

| Property | Description | Default Value |
|----------|-------------|---------------|
| `storageLocation` | The location where documents are stored | `default-storage` |
| `connectionTimeout` | Connection timeout in seconds | `30` |

## Events

The plugin publishes the following events:

| Event | Description |
|-------|-------------|
| `DocumentCreatedEvent` | Published when a document is successfully created |

## Supported Document Types

The plugin supports the following document types:
- PDF
- DOCX
- TXT
- XML
- JSON
- HTML

## License

This plugin is part of the Firefly Platform and is subject to the same license terms.