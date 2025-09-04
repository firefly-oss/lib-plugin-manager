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


package com.firefly.document.api;

import com.firefly.core.plugin.annotation.ExtensionPoint;
import reactor.core.publisher.Mono;

/**
 * Extension point for document management operations.
 * This extension point allows plugins to implement document creation functionality.
 */
@ExtensionPoint(
        id = "com.firefly.document.document-management",
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