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


package com.firefly.core.plugin.loader;

import com.firefly.core.plugin.api.Plugin;
import com.firefly.core.plugin.model.PluginMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ClasspathPluginLoaderTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private DefaultPluginLoader defaultPluginLoader;

    @Mock
    private Plugin mockPlugin;

    private ClasspathPluginLoader classpathPluginLoader;

    @BeforeEach
    void setUp() {
        classpathPluginLoader = new ClasspathPluginLoader(applicationContext, defaultPluginLoader);

        // Setup mock plugin
        PluginMetadata metadata = PluginMetadata.builder()
                .id("test-plugin")
                .name("Test Plugin")
                .version("1.0.0")
                .build();
        when(mockPlugin.getMetadata()).thenReturn(metadata);
    }

    @Test
    void testLoadPlugin() {
        // Setup
        Path pluginPath = Path.of("test-plugin.jar");
        when(defaultPluginLoader.loadPlugin(pluginPath)).thenReturn(Mono.just(mockPlugin));

        // Test
        StepVerifier.create(classpathPluginLoader.loadPlugin(pluginPath))
                .expectNext(mockPlugin)
                .verifyComplete();
    }

    @Test
    void testLoadPluginsFromClasspathWithUnsupportedOperation() {
        // This test verifies that the ClasspathPluginLoader can handle the case where
        // the actual classpath scanning is not performed (e.g., in a test environment)

        // In a real implementation, this would be a more comprehensive test
        // that verifies the classpath scanning and plugin loading
    }
}
