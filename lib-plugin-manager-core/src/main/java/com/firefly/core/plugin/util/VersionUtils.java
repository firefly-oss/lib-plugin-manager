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

import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for version comparison and management.
 * This class provides helper methods for semantic versioning operations.
 */
public final class VersionUtils {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([\\w.-]+))?(?:\\+([\\w.-]+))?$");
    private static final Pattern VERSION_CONSTRAINT_PATTERN = Pattern.compile("([<>=!~^]+=?)?\\s*(\\d+(?:\\.\\d+){0,2}(?:-[\\w.-]+)?(?:\\+[\\w.-]+)?)");

    private VersionUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Checks if a string is a valid semantic version.
     *
     * @param version the version string to check
     * @return true if the version is valid, false otherwise
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }

        return VERSION_PATTERN.matcher(version).matches();
    }

    /**
     * Compares two version strings according to semantic versioning rules.
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return a negative integer, zero, or a positive integer as the first version is less than, equal to, or greater than the second
     * @throws IllegalArgumentException if either version is invalid
     */
    public static int compareVersions(String version1, String version2) {
        if (!isValidVersion(version1) || !isValidVersion(version2)) {
            throw new IllegalArgumentException("Invalid version format. Expected format: MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]");
        }

        Matcher matcher1 = VERSION_PATTERN.matcher(version1);
        Matcher matcher2 = VERSION_PATTERN.matcher(version2);

        if (!matcher1.matches() || !matcher2.matches()) {
            throw new IllegalArgumentException("Invalid version format");
        }

        // Compare major version
        int major1 = Integer.parseInt(matcher1.group(1));
        int major2 = Integer.parseInt(matcher2.group(1));
        int result = Integer.compare(major1, major2);
        if (result != 0) {
            return result;
        }

        // Compare minor version
        int minor1 = Integer.parseInt(matcher1.group(2));
        int minor2 = Integer.parseInt(matcher2.group(2));
        result = Integer.compare(minor1, minor2);
        if (result != 0) {
            return result;
        }

        // Compare patch version
        int patch1 = Integer.parseInt(matcher1.group(3));
        int patch2 = Integer.parseInt(matcher2.group(3));
        result = Integer.compare(patch1, patch2);
        if (result != 0) {
            return result;
        }

        // Compare pre-release versions
        String preRelease1 = matcher1.group(4);
        String preRelease2 = matcher2.group(4);

        // Pre-release versions have lower precedence than the associated normal version
        if (preRelease1 == null && preRelease2 != null) {
            return 1;
        }
        if (preRelease1 != null && preRelease2 == null) {
            return -1;
        }
        if (preRelease1 != null && preRelease2 != null) {
            return comparePreReleaseVersions(preRelease1, preRelease2);
        }

        // Build metadata does not affect precedence
        return 0;
    }

    /**
     * Compares two pre-release version strings.
     *
     * @param preRelease1 the first pre-release version
     * @param preRelease2 the second pre-release version
     * @return a negative integer, zero, or a positive integer as the first version is less than, equal to, or greater than the second
     */
    private static int comparePreReleaseVersions(String preRelease1, String preRelease2) {
        String[] parts1 = preRelease1.split("\\.");
        String[] parts2 = preRelease2.split("\\.");

        int minLength = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < minLength; i++) {
            String part1 = parts1[i];
            String part2 = parts2[i];

            // Numeric identifiers always have lower precedence than non-numeric identifiers
            boolean isNum1 = isNumeric(part1);
            boolean isNum2 = isNumeric(part2);

            if (isNum1 && !isNum2) {
                return -1;
            }
            if (!isNum1 && isNum2) {
                return 1;
            }

            if (isNum1 && isNum2) {
                // Compare numeric identifiers numerically
                int num1 = Integer.parseInt(part1);
                int num2 = Integer.parseInt(part2);
                int result = Integer.compare(num1, num2);
                if (result != 0) {
                    return result;
                }
            } else {
                // Compare non-numeric identifiers lexically
                int result = part1.compareTo(part2);
                if (result != 0) {
                    return result;
                }
            }
        }

        // A larger set of pre-release fields has a higher precedence than a smaller set
        return Integer.compare(parts1.length, parts2.length);
    }

    /**
     * Checks if a string is numeric.
     *
     * @param str the string to check
     * @return true if the string is numeric, false otherwise
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a version satisfies a version constraint.
     *
     * @param version the version to check
     * @param constraint the version constraint (e.g., "&gt;=1.0.0", "&lt;2.0.0")
     * @return true if the version satisfies the constraint, false otherwise
     */
    public static boolean satisfiesConstraint(String version, String constraint) {
        if (!isValidVersion(version)) {
            throw new IllegalArgumentException("Invalid version format: " + version);
        }

        if (constraint == null || constraint.isEmpty()) {
            return true; // No constraint means any version is acceptable
        }

        Matcher constraintMatcher = VERSION_CONSTRAINT_PATTERN.matcher(constraint);
        if (!constraintMatcher.matches()) {
            throw new IllegalArgumentException("Invalid constraint format: " + constraint);
        }

        String operator = constraintMatcher.group(1);
        String constraintVersion = constraintMatcher.group(2);

        if (operator == null || operator.isEmpty()) {
            operator = "=";
        }

        int comparison = compareVersions(version, constraintVersion);

        return switch (operator) {
            case ">" -> comparison > 0;
            case ">=" -> comparison >= 0;
            case "<" -> comparison < 0;
            case "<=" -> comparison <= 0;
            case "=", "==" -> comparison == 0;
            case "!=" -> comparison != 0;
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    /**
     * Sorts a list of versions according to semantic versioning rules.
     *
     * @param versions the array of versions to sort
     */
    public static void sortVersions(String[] versions) {
        if (versions == null) {
            return;
        }

        Arrays.sort(versions, Comparator.comparing(
                version -> version,
                (v1, v2) -> {
                    try {
                        return compareVersions(v1, v2);
                    } catch (IllegalArgumentException e) {
                        // If versions are invalid, fall back to string comparison
                        return v1.compareTo(v2);
                    }
                }
        ));
    }

    /**
     * Gets the next version based on the current version and the type of update.
     *
     * @param currentVersion the current version
     * @param updateType the type of update (major, minor, patch)
     * @return the next version
     */
    public static String getNextVersion(String currentVersion, VersionUpdateType updateType) {
        if (!isValidVersion(currentVersion)) {
            throw new IllegalArgumentException("Invalid version format: " + currentVersion);
        }

        Matcher matcher = VERSION_PATTERN.matcher(currentVersion);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format");
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));

        return switch (updateType) {
            case MAJOR -> (major + 1) + ".0.0";
            case MINOR -> major + "." + (minor + 1) + ".0";
            case PATCH -> major + "." + minor + "." + (patch + 1);
        };
    }

    /**
     * Enum representing the type of version update.
     */
    public enum VersionUpdateType {
        /**
         * Major version update (e.g., 1.0.0 -> 2.0.0).
         * Indicates incompatible API changes.
         */
        MAJOR,

        /**
         * Minor version update (e.g., 1.0.0 -> 1.1.0).
         * Indicates added functionality in a backward-compatible manner.
         */
        MINOR,

        /**
         * Patch version update (e.g., 1.0.0 -> 1.0.1).
         * Indicates backward-compatible bug fixes.
         */
        PATCH
    }
}
