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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.nio.file.Path;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class CompositePluginLoaderTest {

    @Mock
    private DefaultPluginLoader defaultPluginLoader;

    @Mock
    private GitPluginLoader gitPluginLoader;

    @Mock
    private ClasspathPluginLoader classpathPluginLoader;

    @Mock
    private Plugin mockPlugin;

    private CompositePluginLoader compositePluginLoader;

    @BeforeEach
    void setUp() {
        compositePluginLoader = new CompositePluginLoader(
                defaultPluginLoader, gitPluginLoader, classpathPluginLoader);

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
        StepVerifier.create(compositePluginLoader.loadPlugin(pluginPath))
                .expectNext(mockPlugin)
                .verifyComplete();
    }

    @Test
    void testLoadPluginFromGit() {
        // Setup
        URI repositoryUri = URI.create("https://github.com/example/test-plugin.git");
        String branch = "main";
        when(gitPluginLoader.loadPluginFromGit(repositoryUri, branch)).thenReturn(Mono.just(mockPlugin));

        // Test
        StepVerifier.create(compositePluginLoader.loadPluginFromGit(repositoryUri, branch))
                .expectNext(mockPlugin)
                .verifyComplete();
    }

    @Test
    void testLoadPluginsFromClasspath() {
        // Setup
        String basePackage = "com.example";
        when(classpathPluginLoader.loadPluginsFromClasspath(basePackage)).thenReturn(Flux.just(mockPlugin));

        // Test
        StepVerifier.create(compositePluginLoader.loadPluginsFromClasspath(basePackage))
                .expectNext(mockPlugin)
                .verifyComplete();
    }
}
