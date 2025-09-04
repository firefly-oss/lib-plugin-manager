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


package com.firefly.core.plugin.util;

import com.firefly.core.plugin.model.PluginDescriptor;
import com.firefly.core.plugin.model.PluginMetadata;
import com.firefly.core.plugin.model.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility methods for plugin operations.
 * This class provides helper methods for common plugin-related tasks.
 */
public final class PluginUtils {

    private static final Logger logger = LoggerFactory.getLogger(PluginUtils.class);

    private PluginUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Extracts the plugin ID from a plugin file name.
     *
     * @param fileName the plugin file name
     * @return the plugin ID, or null if it cannot be extracted
     */
    public static String extractPluginIdFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        // Remove file extension
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileName = fileName.substring(0, extensionIndex);
        }

        // Handle versioned file names (e.g., plugin-id-1.0.0.jar)
        int versionIndex = fileName.lastIndexOf('-');
        if (versionIndex > 0) {
            // Check if the part after the last dash looks like a version
            String potentialVersion = fileName.substring(versionIndex + 1);
            if (VersionUtils.isValidVersion(potentialVersion)) {
                fileName = fileName.substring(0, versionIndex);
            }
        }

        return fileName;
    }

    /**
     * Filters a collection of plugin descriptors by state.
     *
     * @param plugins the collection of plugin descriptors
     * @param state the state to filter by
     * @return a list of plugin descriptors with the specified state
     */
    public static List<PluginDescriptor> filterPluginsByState(Collection<PluginDescriptor> plugins, PluginState state) {
        if (plugins == null || state == null) {
            return List.of();
        }

        return plugins.stream()
                .filter(plugin -> plugin.getState() == state)
                .collect(Collectors.toList());
    }

    /**
     * Finds a plugin descriptor by ID in a collection of plugin descriptors.
     *
     * @param plugins the collection of plugin descriptors
     * @param pluginId the plugin ID to find
     * @return an Optional containing the plugin descriptor if found, or empty if not found
     */
    public static Optional<PluginDescriptor> findPluginById(Collection<PluginDescriptor> plugins, String pluginId) {
        if (plugins == null || pluginId == null || pluginId.isEmpty()) {
            return Optional.empty();
        }

        return plugins.stream()
                .filter(plugin -> pluginId.equals(plugin.getId()))
                .findFirst();
    }

    /**
     * Creates a plugin descriptor from plugin metadata.
     *
     * @param metadata the plugin metadata
     * @return a new plugin descriptor
     */
    public static PluginDescriptor createDescriptorFromMetadata(PluginMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Plugin metadata cannot be null");
        }

        return PluginDescriptor.builder()
                .metadata(metadata)
                .state(PluginState.INSTALLED)
                .build();
    }

    /**
     * Resolves a plugin path relative to a base directory.
     *
     * @param baseDir the base directory
     * @param pluginPath the plugin path, which can be absolute or relative
     * @return the resolved path
     */
    public static Path resolvePluginPath(Path baseDir, String pluginPath) {
        if (pluginPath == null || pluginPath.isEmpty()) {
            throw new IllegalArgumentException("Plugin path cannot be null or empty");
        }

        Path path = Paths.get(pluginPath);
        if (path.isAbsolute()) {
            return path;
        }

        return baseDir.resolve(path);
    }

    /**
     * Formats plugin information for logging or display.
     *
     * @param descriptor the plugin descriptor
     * @return a formatted string with plugin information
     */
    public static String formatPluginInfo(PluginDescriptor descriptor) {
        if (descriptor == null) {
            return "null";
        }

        return String.format("Plugin[id=%s, name=%s, version=%s, state=%s]",
                descriptor.getId(),
                descriptor.getName(),
                descriptor.getVersion(),
                descriptor.getState());
    }

    /**
     * Groups plugins by their state.
     *
     * @param plugins the collection of plugin descriptors
     * @return a map of plugin state to list of plugin descriptors
     */
    public static Map<PluginState, List<PluginDescriptor>> groupPluginsByState(Collection<PluginDescriptor> plugins) {
        if (plugins == null) {
            return Map.of();
        }

        return plugins.stream()
                .collect(Collectors.groupingBy(PluginDescriptor::getState));
    }
}
