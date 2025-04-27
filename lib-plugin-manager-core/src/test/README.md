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

- **Plugin Management Tests**
  - `DefaultPluginManagerTest`: Tests the high-level plugin management operations.
  - `DefaultPluginLoaderTest`: Tests the plugin loading functionality.
  - `PluginClassLoaderTest`: Tests the plugin class loader isolation.

- **Configuration Tests**
  - `PluginManagerPropertiesTest`: Tests the configuration properties binding.
  - `PluginManagerAutoConfigurationTest`: Tests the auto-configuration of beans.

### 2. Integration Tests

Integration tests verify that different components work together correctly.

- `PluginSystemIntegrationTest`: Tests the end-to-end plugin lifecycle and interactions between components.
- `KafkaIntegrationTest`: Tests the integration with Kafka for distributed event processing.

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

## Test Coverage

The test suite aims to provide comprehensive coverage of the Firefly Plugin Manager functionality:

- **Plugin Lifecycle**: Tests for installing, initializing, starting, stopping, and uninstalling plugins.
- **Extension Management**: Tests for registering extension points and extensions, and retrieving extensions.
- **Event Communication**: Tests for publishing and subscribing to events, both in-memory and via Kafka.
- **Configuration**: Tests for configuration properties and their effects on the system.
- **Security**: Tests for class loading isolation and security boundaries.

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

## Adding New Tests

When adding new tests:

1. Follow the existing package structure and naming conventions.
2. Use the appropriate test category (unit or integration).
3. Mock external dependencies in unit tests.
4. Use `StepVerifier` for testing reactive code.
5. Include both positive and negative test cases.
6. Document any special setup or requirements in the test class JavaDoc.
