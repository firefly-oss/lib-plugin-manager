# Extension Registry Tests

This package contains tests for the extension registry component of the Firefly Plugin Manager.

## DefaultExtensionRegistryTest

Tests the `DefaultExtensionRegistry` implementation, which is responsible for managing extension points and extensions.

### Test Cases

- **testRegisterExtensionPoint**: Verifies that an extension point can be registered.
- **testRegisterExtension**: Verifies that an extension can be registered for an extension point.
- **testRegisterMultipleExtensionsWithPriority**: Verifies that multiple extensions can be registered with different priorities and are ordered correctly.
- **testUnregisterExtension**: Verifies that an extension can be unregistered.
- **testRegisterExtensionWithoutExtensionPoint**: Verifies that attempting to register an extension without first registering its extension point fails.
- **testRegisterInvalidExtension**: Verifies that attempting to register an object that doesn't implement the extension point interface fails.
- **testGetExtensionsForNonExistentExtensionPoint**: Verifies that getting extensions for a non-existent extension point returns an empty list.

### Key Assertions

- Extension points can be registered and queried
- Extensions can be registered for extension points
- Extensions are ordered by priority (highest first)
- Invalid extensions are rejected
- Extension unregistration works correctly

## Test Extension Point and Extensions

The test uses a simple extension point interface and implementations:

```java
// Test extension point interface
private interface TestExtensionPoint {
    String getName();
    int getPriority();
}

// Test extension implementations
private static class TestExtension1 implements TestExtensionPoint {
    @Override
    public String getName() {
        return "Extension 1";
    }

    @Override
    public int getPriority() {
        return 100;
    }
}

private static class TestExtension2 implements TestExtensionPoint {
    @Override
    public String getName() {
        return "Extension 2";
    }

    @Override
    public int getPriority() {
        return 200;
    }
}
```

## Example

```java
@Test
void testRegisterMultipleExtensionsWithPriority() {
    // Register extension point
    StepVerifier.create(registry.registerExtensionPoint("test.extension-point", TestExtensionPoint.class))
            .verifyComplete();
    
    // Register extensions with different priorities
    TestExtensionPoint extension1 = new TestExtension1();
    TestExtensionPoint extension2 = new TestExtension2();
    TestExtensionPoint extension3 = new TestExtension3();
    
    StepVerifier.create(
            registry.registerExtension("test.extension-point", extension1, 100)
                    .then(registry.registerExtension("test.extension-point", extension2, 200))
                    .then(registry.registerExtension("test.extension-point", extension3, 50))
    ).verifyComplete();
    
    // Verify extensions are ordered by priority (highest first)
    StepVerifier.create(registry.getExtensions("test.extension-point").collectList())
            .assertNext(extensions -> {
                assertEquals(3, extensions.size());
                assertEquals("Extension 2", ((TestExtensionPoint) extensions.get(0)).getName());
                assertEquals("Extension 1", ((TestExtensionPoint) extensions.get(1)).getName());
                assertEquals("Extension 3", ((TestExtensionPoint) extensions.get(2)).getName());
            })
            .verifyComplete();
    
    // Verify highest priority extension
    StepVerifier.create(registry.getHighestPriorityExtension("test.extension-point"))
            .assertNext(extension -> {
                assertEquals("Extension 2", ((TestExtensionPoint) extension).getName());
            })
            .verifyComplete();
}
```
