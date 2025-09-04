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


/**
 * Utility classes for the Firefly Plugin Manager.
 * <p>
 * This package contains utility classes that provide helper methods for common tasks
 * related to plugin management, including:
 * <ul>
 *   <li>Plugin operations - {@link com.firefly.core.plugin.util.PluginUtils}</li>
 *   <li>Version comparison and management - {@link com.firefly.core.plugin.util.VersionUtils}</li>
 *   <li>Class loading operations - {@link com.firefly.core.plugin.util.ClassLoaderUtils}</li>
 *   <li>Resource management - {@link com.firefly.core.plugin.util.ResourceUtils}</li>
 * </ul>
 * <p>
 * These utility classes are designed to be used throughout the plugin manager codebase
 * to reduce code duplication and provide consistent implementations of common functionality.
 * All utility classes in this package are final with private constructors to prevent instantiation,
 * as they only provide static utility methods.
 */
package com.firefly.core.plugin.util;
