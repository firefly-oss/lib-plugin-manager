# Utility Classes Tests

This package contains tests for the utility classes in the `com.firefly.core.plugin.util` package.

## VersionUtilsTest

Tests the `VersionUtils` class, which provides utilities for version comparison and management.

### Test Cases

- **isValidVersion**: Verifies that version validation works correctly for valid and invalid versions.
- **compareVersions**: Verifies that version comparison works correctly according to semantic versioning rules.
- **satisfiesConstraint**: Verifies that version constraint checking works correctly.
- **sortVersions**: Verifies that version sorting works correctly.
- **getNextVersion**: Verifies that calculating the next version works correctly.

## PluginUtilsTest

Tests the `PluginUtils` class, which provides utilities for plugin operations.

### Test Cases

- **extractPluginIdFromFileName**: Verifies that plugin IDs can be extracted from file names.
- **filterPluginsByState**: Verifies that plugins can be filtered by state.
- **findPluginById**: Verifies that plugins can be found by ID.
- **createDescriptorFromMetadata**: Verifies that plugin descriptors can be created from metadata.
- **resolvePluginPath**: Verifies that plugin paths can be resolved correctly.
- **formatPluginInfo**: Verifies that plugin information can be formatted correctly.
- **groupPluginsByState**: Verifies that plugins can be grouped by state.

## ClassLoaderUtilsTest

Tests the `ClassLoaderUtils` class, which provides utilities for class loading operations.

### Test Cases

- **getDefaultClassLoader**: Verifies that the default class loader can be retrieved.
- **loadClass**: Verifies that classes can be loaded.
- **createInstance**: Verifies that instances can be created from classes.
- **findResources**: Verifies that resources can be found.
- **filterClasses**: Verifies that classes can be filtered.
- **getClassLoaderHierarchy**: Verifies that the class loader hierarchy can be retrieved.

## ResourceUtilsTest

Tests the `ResourceUtils` class, which provides utilities for resource management.

### Test Cases

- **readFileAsString**: Verifies that files can be read as strings.
- **readFileAsLines**: Verifies that files can be read as lines.
- **writeStringToFile**: Verifies that strings can be written to files.
- **writeLinesToFile**: Verifies that lines can be written to files.
- **readResourceAsString**: Verifies that resources can be read as strings.
- **loadProperties**: Verifies that properties can be loaded from files.
- **loadPropertiesFromResource**: Verifies that properties can be loaded from resources.
- **listFiles**: Verifies that files can be listed.
- **createDirectoryIfNotExists**: Verifies that directories can be created.
- **delete**: Verifies that files and directories can be deleted.
- **copy**: Verifies that files and directories can be copied.
