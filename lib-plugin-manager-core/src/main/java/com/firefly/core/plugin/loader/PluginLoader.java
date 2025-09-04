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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Path;

/**
 * Interface for loading plugins from external sources.
 */
public interface PluginLoader {

    /**
     * Loads a plugin from a JAR file.
     *
     * @param pluginPath the path to the plugin JAR file
     * @return a Mono that emits the loaded plugin
     */
    Mono<Plugin> loadPlugin(Path pluginPath);

    /**
     * Loads a plugin from a Git repository.
     *
     * @param repositoryUri the URI of the Git repository
     * @param branch the branch to checkout (optional, defaults to main/master)
     * @return a Mono that emits the loaded plugin
     */
    Mono<Plugin> loadPluginFromGit(URI repositoryUri, String branch);

    /**
     * Loads a plugin from a Git repository using the default branch.
     *
     * @param repositoryUri the URI of the Git repository
     * @return a Mono that emits the loaded plugin
     */
    Mono<Plugin> loadPluginFromGit(URI repositoryUri);

    /**
     * Scans the classpath for plugins annotated with @Plugin.
     *
     * @param basePackage the base package to scan (optional)
     * @return a Flux that emits all discovered plugins
     */
    Flux<Plugin> loadPluginsFromClasspath(String basePackage);

    /**
     * Scans the entire classpath for plugins annotated with @Plugin.
     *
     * @return a Flux that emits all discovered plugins
     */
    Flux<Plugin> loadPluginsFromClasspath();
}
