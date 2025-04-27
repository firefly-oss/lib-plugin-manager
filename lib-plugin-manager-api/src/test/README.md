# Firefly Plugin Manager API Test Suite

This directory contains the test suite for the Firefly Plugin Manager API module. The API module defines the interfaces, models, and annotations that form the contract between the plugin system and plugin developers.

## Test Structure

The test suite should be organized into the following categories:

### 1. Interface Contract Tests

Tests that verify the contract defined by the interfaces in the API module:

- **PluginManager Interface Tests**
  - Tests for the contract defined by the `PluginManager` interface
  - Verification of method signatures and expected behavior

- **PluginRegistry Interface Tests**
  - Tests for the contract defined by the `PluginRegistry` interface
  - Verification of plugin lifecycle management contract

- **ExtensionRegistry Interface Tests**
  - Tests for the contract defined by the `ExtensionRegistry` interface
  - Verification of extension point and extension management contract

- **PluginEventBus Interface Tests**
  - Tests for the contract defined by the `PluginEventBus` interface
  - Verification of event publishing and subscription contract

### 2. Model Tests

Tests for the data models defined in the API module:

- **PluginMetadata Tests**
  - Tests for the `PluginMetadata` record
  - Verification of builder pattern and validation

- **PluginDescriptor Tests**
  - Tests for the `PluginDescriptor` record
  - Verification of builder pattern and validation

- **PluginEvent Tests**
  - Tests for the `PluginEvent` class and its subclasses
  - Verification of event serialization and deserialization

### 3. Annotation Tests

Tests for the annotations defined in the API module:

- **@Plugin Annotation Tests**
  - Tests for the `@Plugin` annotation
  - Verification of annotation attributes and defaults

- **@ExtensionPoint Annotation Tests**
  - Tests for the `@ExtensionPoint` annotation
  - Verification of annotation attributes and defaults

- **@Extension Annotation Tests**
  - Tests for the `@Extension` annotation
  - Verification of annotation attributes and defaults

### 4. SPI Tests

Tests for the Service Provider Interface (SPI) classes:

- **AbstractPlugin Tests**
  - Tests for the `AbstractPlugin` class
  - Verification of default implementations

## Test Implementation Guidelines

When implementing tests for the API module, follow these guidelines:

1. **Focus on Contracts**: The API module defines contracts, so tests should focus on verifying that these contracts are well-defined and consistent.

2. **Use Mocks**: Since the API module only defines interfaces, use mocks to test the expected behavior of implementations.

3. **Test Edge Cases**: Test edge cases such as null values, empty collections, and invalid inputs.

4. **Test Serialization**: For models that need to be serialized (e.g., for event distribution), test serialization and deserialization.

5. **Test Validation**: For models with validation rules, test both valid and invalid inputs.

## Running the Tests

### Running All Tests

To run all tests, use the following Maven command:

```bash
mvn test
```

### Running Specific Test Categories

To run only a specific test class:

```bash
mvn test -Dtest=PluginMetadataTest
```

To run a specific test method:

```bash
mvn test -Dtest=PluginMetadataTest#testBuilder
```

## Test Coverage

The test suite should aim to provide comprehensive coverage of the API module:

- **Interface Contracts**: All methods defined in interfaces should be tested for their contract.
- **Model Validation**: All validation rules for models should be tested.
- **Annotation Processing**: The behavior of annotations should be tested.
- **Builder Patterns**: All builder patterns should be tested for correct construction of objects.

## Adding New Tests

When adding new tests:

1. Follow the existing package structure and naming conventions.
2. Document the purpose of each test class and method.
3. Include both positive and negative test cases.
4. Use descriptive test method names that clearly indicate what is being tested.
