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


package com.firefly.core.plugin.dependency;

import com.firefly.core.plugin.exception.CircularDependencyException;
import com.firefly.core.plugin.exception.DependencyNotFoundException;
import com.firefly.core.plugin.exception.IncompatibleDependencyException;
import com.firefly.core.plugin.model.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resolves plugin dependencies and determines the correct order for loading and starting plugins.
 */
@Component
public class PluginDependencyResolver {

    private static final Logger logger = LoggerFactory.getLogger(PluginDependencyResolver.class);

    // Pattern to find the first occurrence of a version constraint operator
    private static final Pattern VERSION_CONSTRAINT_PATTERN = Pattern.compile("([<>=]+)");

    /**
     * Resolves dependencies for the given plugins and returns them in the order they should be loaded.
     *
     * @param plugins the plugins to resolve dependencies for
     * @return a list of plugins in the order they should be loaded
     * @throws CircularDependencyException if a circular dependency is detected
     * @throws DependencyNotFoundException if a required dependency cannot be found
     * @throws IncompatibleDependencyException if a dependency is found but its version is incompatible
     */
    public List<PluginDescriptor> resolveDependencies(Collection<PluginDescriptor> plugins) {
        logger.debug("Resolving dependencies for {} plugins", plugins.size());

        // Create a map of plugin IDs to descriptors for quick lookup
        Map<String, PluginDescriptor> pluginMap = plugins.stream()
                .collect(Collectors.toMap(PluginDescriptor::getId, p -> p));

        // Create a set to track visited plugins during traversal
        Set<String> visited = new HashSet<>();

        // Create a list to store the plugins in dependency order
        List<PluginDescriptor> orderedPlugins = new ArrayList<>();

        // Process each plugin
        for (PluginDescriptor plugin : plugins) {
            if (!visited.contains(plugin.getId())) {
                // Use a set to track the current dependency path for cycle detection
                Set<String> currentPath = new HashSet<>();
                resolveDependenciesRecursive(plugin.getId(), pluginMap, visited, currentPath, orderedPlugins);
            }
        }

        logger.debug("Resolved dependency order for {} plugins", orderedPlugins.size());
        return orderedPlugins;
    }

    /**
     * Recursively resolves dependencies for a plugin.
     *
     * @param pluginId the ID of the plugin to resolve dependencies for
     * @param pluginMap a map of plugin IDs to descriptors
     * @param visited a set of plugin IDs that have already been processed
     * @param currentPath the current dependency path for cycle detection
     * @param orderedPlugins the list to add plugins to in dependency order
     * @throws CircularDependencyException if a circular dependency is detected
     * @throws DependencyNotFoundException if a required dependency cannot be found
     * @throws IncompatibleDependencyException if a dependency is found but its version is incompatible
     */
    private void resolveDependenciesRecursive(
            String pluginId,
            Map<String, PluginDescriptor> pluginMap,
            Set<String> visited,
            Set<String> currentPath,
            List<PluginDescriptor> orderedPlugins) {

        // Check for circular dependencies
        if (currentPath.contains(pluginId)) {
            throw new CircularDependencyException("Circular dependency detected: " +
                    currentPath.stream().collect(Collectors.joining(" -> ")) + " -> " + pluginId);
        }

        // Skip if already visited
        if (visited.contains(pluginId)) {
            return;
        }

        // Get the plugin descriptor
        PluginDescriptor plugin = pluginMap.get(pluginId);
        if (plugin == null) {
            throw new DependencyNotFoundException("Plugin not found: " + pluginId);
        }

        // Add to current path for cycle detection
        currentPath.add(pluginId);

        // Process each dependency
        for (String dependencySpec : plugin.metadata().dependencies()) {
            // Check if this is an optional dependency
            boolean optional = dependencySpec.startsWith("?");
            String dependency = optional ? dependencySpec.substring(1) : dependencySpec;

            // Parse the dependency specification to extract ID and version constraints
            DependencySpec spec = parseDependencySpec(dependency);

            // Look up the dependency
            PluginDescriptor dependencyPlugin = pluginMap.get(spec.id());

            // Handle missing dependencies
            if (dependencyPlugin == null) {
                if (optional) {
                    logger.debug("Optional dependency {} not found for plugin {}, skipping", spec.id(), pluginId);
                    continue;
                } else {
                    throw new DependencyNotFoundException(
                            "Required dependency " + spec.id() + " not found for plugin " + pluginId);
                }
            }

            // Check version constraints
            if (spec.versionConstraint() != null && !spec.versionConstraint().isEmpty()) {
                String dependencyVersion = dependencyPlugin.getVersion();
                if (!satisfiesVersionConstraint(dependencyVersion, spec.versionConstraint())) {
                    throw new IncompatibleDependencyException(
                            "Dependency version constraint not satisfied: " +
                            spec.id() + " " + spec.versionConstraint() +
                            " (found: " + dependencyVersion + ")");
                }
            }

            // Recursively resolve dependencies of this dependency
            resolveDependenciesRecursive(spec.id(), pluginMap, visited, currentPath, orderedPlugins);
        }

        // Remove from current path as we're done with this plugin
        currentPath.remove(pluginId);

        // Mark as visited
        visited.add(pluginId);

        // Add to ordered list
        orderedPlugins.add(plugin);
    }

    /**
     * Parses a dependency specification string into its components.
     *
     * @param dependencySpec the dependency specification string (e.g., "com.example.plugin>=1.0.0")
     * @return a DependencySpec object containing the parsed components
     */
    private DependencySpec parseDependencySpec(String dependencySpec) {
        Matcher matcher = VERSION_CONSTRAINT_PATTERN.matcher(dependencySpec);
        if (matcher.find()) {
            int constraintIndex = matcher.start();
            String id = dependencySpec.substring(0, constraintIndex);
            String versionConstraint = dependencySpec.substring(constraintIndex);
            return new DependencySpec(id, versionConstraint);
        } else {
            return new DependencySpec(dependencySpec, "");
        }
    }

    /**
     * Checks if a version satisfies a version constraint.
     *
     * @param version the version to check
     * @param constraint the version constraint (e.g., ">=1.0.0")
     * @return true if the version satisfies the constraint, false otherwise
     */
    public boolean satisfiesVersionConstraint(String version, String constraint) {
        if (constraint == null || constraint.isEmpty()) {
            return true;
        }

        // Extract the operator and version from the constraint
        String operator;
        String constraintVersion;

        if (constraint.startsWith(">=")) {
            operator = ">=";
            constraintVersion = constraint.substring(2);
        } else if (constraint.startsWith(">")) {
            operator = ">";
            constraintVersion = constraint.substring(1);
        } else if (constraint.startsWith("<=")) {
            operator = "<=";
            constraintVersion = constraint.substring(2);
        } else if (constraint.startsWith("<")) {
            operator = "<";
            constraintVersion = constraint.substring(1);
        } else if (constraint.startsWith("==")) {
            operator = "==";
            constraintVersion = constraint.substring(2);
        } else if (constraint.startsWith("=")) {
            operator = "=";
            constraintVersion = constraint.substring(1);
        } else {
            // Invalid constraint format
            logger.warn("Invalid version constraint format: {}", constraint);
            return false;
        }

        // Compare the versions
        int comparison = compareVersions(version, constraintVersion);

        switch (operator) {
            case ">":
                return comparison > 0;
            case ">=":
                return comparison >= 0;
            case "<":
                return comparison < 0;
            case "<=":
                return comparison <= 0;
            case "=":
            case "==":
                return comparison == 0;
            default:
                // Should never happen
                logger.warn("Unhandled operator: {}", operator);
                return false;
        }
    }

    /**
     * Compares two version strings.
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return a negative integer, zero, or a positive integer as the first version is less than, equal to, or greater than the second
     */
    public int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (v1 < v2) {
                return -1;
            } else if (v1 > v2) {
                return 1;
            }
        }

        return 0;
    }

    /**
     * Parses a version part string to an integer.
     * Handles non-numeric parts by extracting the numeric prefix.
     *
     * @param part the version part string
     * @return the parsed integer value
     */
    private int parseVersionPart(String part) {
        // Extract numeric prefix (e.g., "1-SNAPSHOT" -> "1")
        StringBuilder numericPart = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (Character.isDigit(c)) {
                numericPart.append(c);
            } else {
                break;
            }
        }

        if (numericPart.length() > 0) {
            try {
                return Integer.parseInt(numericPart.toString());
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse version part: {}", part, e);
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * Record representing a parsed dependency specification.
     *
     * @param id the plugin ID
     * @param versionConstraint the version constraint (e.g., ">=1.0.0")
     */
    private record DependencySpec(String id, String versionConstraint) {}
}
