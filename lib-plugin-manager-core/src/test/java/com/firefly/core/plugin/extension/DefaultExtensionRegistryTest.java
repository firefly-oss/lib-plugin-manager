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


package com.firefly.core.plugin.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultExtensionRegistryTest {

    private DefaultExtensionRegistry registry;

    // Test extension point interface
    private interface TestExtensionPoint {
        String getName();
        int getPriority();
    }

    // Test extension implementations
    private static class TestExtension1 implements TestExtensionPoint {
        @Override
        public String getName() {
            return "Extension 1";
        }

        @Override
        public int getPriority() {
            return 100;
        }
    }

    private static class TestExtension2 implements TestExtensionPoint {
        @Override
        public String getName() {
            return "Extension 2";
        }

        @Override
        public int getPriority() {
            return 200;
        }
    }

    private static class TestExtension3 implements TestExtensionPoint {
        @Override
        public String getName() {
            return "Extension 3";
        }

        @Override
        public int getPriority() {
            return 50;
        }
    }

    @BeforeEach
    void setUp() {
        registry = new DefaultExtensionRegistry();
    }

    @Test
    void testRegisterExtensionPoint() {
        // Register extension point
        StepVerifier.create(registry.registerExtensionPoint("test.extension-point", TestExtensionPoint.class))
                .verifyComplete();
        
        // Verify extension point is registered
        StepVerifier.create(registry.getExtensionPoints().collectList())
                .assertNext(extensionPoints -> {
                    assertEquals(1, extensionPoints.size());
                    assertEquals("test.extension-point", extensionPoints.get(0));
                })
                .verifyComplete();
    }

    @Test
    void testRegisterExtension() {
        // Register extension point
        StepVerifier.create(registry.registerExtensionPoint("test.extension-point", TestExtensionPoint.class))
                .verifyComplete();
        
        // Register extension
        TestExtensionPoint extension = new TestExtension1();
        StepVerifier.create(registry.registerExtension("test.extension-point", extension, 100))
                .verifyComplete();
        
        // Verify extension is registered
        StepVerifier.create(registry.getExtensions("test.extension-point").collectList())
                .assertNext(extensions -> {
                    assertEquals(1, extensions.size());
                    assertEquals("Extension 1", ((TestExtensionPoint) extensions.get(0)).getName());
                })
                .verifyComplete();
    }

    @Test
    void testRegisterMultipleExtensionsWithPriority() {
        // Register extension point
        StepVerifier.create(registry.registerExtensionPoint("test.extension-point", TestExtensionPoint.class))
                .verifyComplete();
        
        // Register extensions with different priorities
        TestExtensionPoint extension1 = new TestExtension1();
        TestExtensionPoint extension2 = new TestExtension2();
        TestExtensionPoint extension3 = new TestExtension3();
        
        StepVerifier.create(
                registry.registerExtension("test.extension-point", extension1, 100)
                        .then(registry.registerExtension("test.extension-point", extension2, 200))
                        .then(registry.registerExtension("test.extension-point", extension3, 50))
        ).verifyComplete();
        
        // Verify extensions are ordered by priority (highest first)
        StepVerifier.create(registry.getExtensions("test.extension-point").collectList())
                .assertNext(extensions -> {
                    assertEquals(3, extensions.size());
                    assertEquals("Extension 2", ((TestExtensionPoint) extensions.get(0)).getName());
                    assertEquals("Extension 1", ((TestExtensionPoint) extensions.get(1)).getName());
                    assertEquals("Extension 3", ((TestExtensionPoint) extensions.get(2)).getName());
                })
                .verifyComplete();
        
        // Verify highest priority extension
        StepVerifier.create(registry.getHighestPriorityExtension("test.extension-point"))
                .assertNext(extension -> {
                    assertEquals("Extension 2", ((TestExtensionPoint) extension).getName());
                })
                .verifyComplete();
    }

    @Test
    void testUnregisterExtension() {
        // Register extension point and extensions
        TestExtensionPoint extension1 = new TestExtension1();
        TestExtensionPoint extension2 = new TestExtension2();
        
        StepVerifier.create(
                registry.registerExtensionPoint("test.extension-point", TestExtensionPoint.class)
                        .then(registry.registerExtension("test.extension-point", extension1, 100))
                        .then(registry.registerExtension("test.extension-point", extension2, 200))
        ).verifyComplete();
        
        // Unregister one extension
        StepVerifier.create(registry.unregisterExtension("test.extension-point", extension1))
                .verifyComplete();
        
        // Verify only one extension remains
        StepVerifier.create(registry.getExtensions("test.extension-point").collectList())
                .assertNext(extensions -> {
                    assertEquals(1, extensions.size());
                    assertEquals("Extension 2", ((TestExtensionPoint) extensions.get(0)).getName());
                })
                .verifyComplete();
    }

    @Test
    void testRegisterExtensionWithoutExtensionPoint() {
        // Try to register extension without registering extension point first
        TestExtensionPoint extension = new TestExtension1();
        
        StepVerifier.create(registry.registerExtension("test.extension-point", extension, 100))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testRegisterInvalidExtension() {
        // Register extension point
        StepVerifier.create(registry.registerExtensionPoint("test.extension-point", TestExtensionPoint.class))
                .verifyComplete();
        
        // Try to register an object that doesn't implement the extension point
        Object invalidExtension = new Object();
        
        StepVerifier.create(registry.registerExtension("test.extension-point", invalidExtension, 100))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testGetExtensionsForNonExistentExtensionPoint() {
        // Try to get extensions for a non-existent extension point
        StepVerifier.create(registry.getExtensions("non-existent").collectList())
                .assertNext(extensions -> {
                    assertTrue(extensions.isEmpty());
                })
                .verifyComplete();
        
        // Try to get highest priority extension for a non-existent extension point
        StepVerifier.create(registry.getHighestPriorityExtension("non-existent"))
                .verifyComplete();
    }
}
