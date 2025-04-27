# Firefly Plugin Manager Test Suite

This directory contains the comprehensive test suite for the Firefly Plugin Manager. The tests are designed to verify the functionality, reliability, and correctness of the plugin system.

## Test Structure

The test suite is organized into several categories:

### 1. Unit Tests

Unit tests focus on testing individual components in isolation, using mocks for dependencies.

- **Registry Tests**
  - `DefaultPluginRegistryTest`: Tests the plugin registry functionality including registration, lifecycle management, and configuration.
  - `DefaultExtensionRegistryTest`: Tests the extension registry functionality including extension point registration and extension management.

- **Event Bus Tests**
  - `DefaultPluginEventBusTest`: Tests the in-memory event bus implementation.
  - `KafkaPluginEventBusTest`: Tests the Kafka-based event bus implementation.
  - `PluginEventSerializerTest`: Tests the serialization and deserialization of plugin events.
  - `EventBusConfigurationTest`: Tests the configuration options for selecting between in-memory and Kafka event buses.

- **Plugin Management Tests**
  - `DefaultPluginManagerTest`: Tests the high-level plugin management operations.
  - `DefaultPluginLoaderTest`: Tests the plugin loading functionality.
  - `GitPluginLoaderTest`: Tests loading plugins from Git repositories.
  - `ClasspathPluginLoaderTest`: Tests auto-detection and loading of plugins from the classpath.
  - `CompositePluginLoaderTest`: Tests the composite loader that delegates to specific loaders.

- **Security Tests**
  - `PluginClassLoaderTest`: Tests the plugin class loader isolation and package access control.
  - `PluginPermissionTest`: Tests the permission system for controlling plugin access.
  - `PluginSecurityManagerTest`: Tests the security manager that enforces permissions.
  - `PluginResourceLimiterTest`: Tests the resource limiting functionality for plugins.
  - `PluginSignatureVerifierTest`: Tests the signature verification for plugin JARs.

- **Configuration Tests**
  - `PluginManagerPropertiesTest`: Tests the configuration properties binding.
  - `PluginManagerAutoConfigurationTest`: Tests the auto-configuration of beans.

### 2. Integration Tests

Integration tests verify that different components work together correctly.

- `PluginSystemIntegrationTest`: Tests the end-to-end plugin lifecycle and interactions between components.
- `KafkaIntegrationTest`: Tests the integration with Kafka for distributed event processing.
- `GitRepositoryIntegrationTest`: Tests the end-to-end process of installing plugins from Git repositories.
- `ClasspathPluginIntegrationTest`: Tests the auto-detection and installation of plugins from the classpath.

## Running the Tests

### Running All Tests

To run all tests, use the following Maven command:

```bash
mvn test
```

### Running Specific Test Categories

To run only unit tests (excluding integration tests):

```bash
mvn test -Dtest=*Test
```

To run only integration tests:

```bash
mvn verify -Dit.test=*IntegrationTest
```

### Running Individual Tests

To run a specific test class:

```bash
mvn test -Dtest=DefaultPluginRegistryTest
```

To run a specific test method:

```bash
mvn test -Dtest=DefaultPluginRegistryTest#testRegisterPlugin
```

## Test Environment Configuration

### Kafka Integration Tests

The Kafka integration tests require special configuration:

1. By default, Kafka integration tests are disabled to avoid requiring a Kafka broker during regular testing.
2. To enable Kafka tests, set the environment variable `ENABLE_KAFKA_TESTS=true`.
3. The tests use an embedded Kafka broker for testing, so no external Kafka installation is required.

Example:

```bash
ENABLE_KAFKA_TESTS=true mvn test -Dtest=KafkaIntegrationTest
```

### Git Repository Tests

The Git repository integration tests require special configuration:

1. By default, these tests use mock Git repositories to avoid network dependencies.
2. To test with real Git repositories, set the environment variable `USE_REAL_GIT_REPOS=true`.
3. When using real repositories, you can configure the test repository URL with `TEST_GIT_REPO_URL`.

Example:

```bash
USE_REAL_GIT_REPOS=true TEST_GIT_REPO_URL=https://github.com/example/test-plugin.git mvn test -Dtest=GitRepositoryIntegrationTest
```

## Test Coverage

The test suite aims to provide comprehensive coverage of the Firefly Plugin Manager functionality:

- **Plugin Lifecycle**: Tests for installing, initializing, starting, stopping, and uninstalling plugins.
- **Extension Management**: Tests for registering extension points and extensions, and retrieving extensions.
- **Event Communication**: Tests for publishing and subscribing to events, both in-memory and via Kafka. Tests verify that the system works correctly with either event bus implementation and that switching between them is seamless.
- **Configuration**: Tests for configuration properties and their effects on the system.
- **Security**: Tests for class loading isolation, permission system, resource limiting, and signature verification. The security tests verify that plugins can only access resources they have permission for, that resource usage is properly limited, and that plugin signatures are correctly verified.

## Mock Plugins

The test suite includes mock plugin implementations for testing:

- `TestPlugin`: A simple plugin implementation used in integration tests.
- `TestExtensionPoint`: A sample extension point interface.
- `TestExtension`: A sample extension implementation.

These mock implementations allow testing the plugin system without requiring actual plugin JAR files.

## Test Utilities

The test suite includes several utility classes to facilitate testing:

- **Custom Matchers**: For asserting reactive types (Mono/Flux).
- **Test Fixtures**: Reusable test data and configurations.
- **Mock Event Bus**: A simplified event bus implementation for testing.

## Troubleshooting Tests

If you encounter issues with the tests:

1. **Kafka Connection Errors**: These are expected in the Kafka tests when no broker is available. They can be ignored unless you're specifically testing Kafka functionality.

2. **Class Loading Issues**: If you see `ClassNotFoundException` or `NoClassDefFoundError`, ensure that the test classpath includes all required dependencies.

3. **Mockito Errors**: If you see `UnnecessaryStubbingException`, it means there are mock method calls defined that aren't being used in the test. Either remove the unnecessary stubbing or use `@MockitoSettings(strictness = Strictness.LENIENT)`.

4. **Reactive Test Timeouts**: If reactive tests are timing out, check for missing subscriptions or terminal signals in the reactive streams.

5. **Security Manager Issues**: If you see `SecurityException` in tests not related to security, it might be because the security manager is still active from a previous test. Make sure to reset the security manager in the `tearDown` method.

6. **Resource Limiter Issues**: If resource limiter tests are failing, check that the test is properly cleaning up resources (threads, file handles, etc.) after each test.

## Adding New Tests

When adding new tests:

1. Follow the existing package structure and naming conventions.
2. Use the appropriate test category (unit or integration).
3. Mock external dependencies in unit tests.
4. Use `StepVerifier` for testing reactive code.
5. Include both positive and negative test cases.
6. Document any special setup or requirements in the test class JavaDoc.

## Testing Alternative Plugin Installation Methods

### Testing Git Repository Installation

When testing the Git repository plugin installation:

1. Use mock Git repositories for unit tests.
2. Test both HTTPS and SSH authentication methods.
3. Test branch and tag selection.
4. Test error handling for network issues and invalid repositories.

### Testing Classpath Plugin Installation

When testing the classpath plugin installation:

1. Test detection of plugins with the `@Plugin` annotation.
2. Test scanning specific packages vs. the entire classpath.
3. Test handling of duplicate plugins.
4. Test dependency resolution between classpath plugins.

## Testing Security Features

### Testing Class Loading Isolation

When testing the class loading isolation:

1. Test that plugins can only access classes from allowed packages.
2. Test that plugins cannot access classes from other plugins unless explicitly exported.
3. Test that plugins can export packages to other plugins.
4. Test that the class loader properly cleans up resources when closed.
5. Test that the class loader tracks loaded classes for monitoring purposes.

### Testing Permission System

When testing the permission system:

1. Test that plugins can only perform operations they have permission for.
2. Test that permissions can be added and removed dynamically.
3. Test that permissions can be scoped to specific targets (e.g., specific directories or hosts).
4. Test that permission implications work correctly (e.g., a general permission implies more specific ones).
5. Test that the security manager correctly identifies the plugin context when checking permissions.

### Testing Resource Limiting

When testing the resource limiting:

1. Test that plugins cannot exceed their memory allocation limits.
2. Test that plugins cannot create more threads than allowed.
3. Test that plugins cannot open more file handles than allowed.
4. Test that plugins cannot open more network connections than allowed.
5. Test that resource usage is properly tracked and reported.
6. Test that resource limits can be configured globally and per-plugin.

### Testing Signature Verification

When testing the signature verification:

1. Test that signed plugins are correctly verified.
2. Test that unsigned plugins are rejected when signatures are required.
3. Test that plugins signed with untrusted certificates are rejected.
4. Test that trusted certificates can be added and used for verification.
5. Test that the verification process checks all entries in the JAR file.
