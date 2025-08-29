# Plugin Loader Tests

This package contains tests for the plugin loader component of the Firefly Plugin Manager.

## DefaultPluginLoaderTest

Tests the `DefaultPluginLoader` implementation, which is responsible for loading plugins from JAR files.

### Test Cases

- **testValidatePluginMetadata**: Verifies that plugin metadata validation works correctly.
- **testLoadPluginWithInvalidPath**: Verifies that attempting to load a plugin from a non-existent path fails.
- **testLoadPluginWithMockClassLoader**: Verifies that a plugin can be loaded using a mock class loader.

### Key Assertions

- Plugin metadata validation works correctly
- Invalid paths are rejected
- Plugins can be loaded from JAR files

### Test Utilities

The test creates a test JAR file for testing:

```java
/**
 * Creates a test JAR file with a minimal structure.
 */
private void createTestJar(File jarFile) throws IOException {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
    
    try (OutputStream os = new FileOutputStream(jarFile);
         JarOutputStream jos = new JarOutputStream(os, manifest)) {
        
        // Add a dummy class file
        JarEntry entry = new JarEntry("com/example/DummyClass.class");
        jos.putNextEntry(entry);
        jos.write(new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE });
        jos.closeEntry();
        
        // Add a service provider configuration file
        entry = new JarEntry("META-INF/services/com.catalis.core.plugin.api.Plugin");
        jos.putNextEntry(entry);
        jos.write("com.example.MockPlugin".getBytes());
        jos.closeEntry();
    }
}
```

## Example

```java
@Test
void testValidatePluginMetadata() throws Exception {
    // Create valid metadata
    PluginMetadata validMetadata = PluginMetadata.builder()
            .id("test-plugin")
            .name("Test Plugin")
            .version("1.0.0")
            .build();
    
    // Use reflection to access the private method
    java.lang.reflect.Method validateMethod = DefaultPluginLoader.class.getDeclaredMethod(
            "validatePluginMetadata", PluginMetadata.class);
    validateMethod.setAccessible(true);
    
    // Should not throw an exception
    validateMethod.invoke(pluginLoader, validMetadata);
    
    // Create invalid metadata (null ID)
    PluginMetadata invalidMetadata1 = PluginMetadata.builder()
            .id(null)
            .name("Test Plugin")
            .version("1.0.0")
            .build();
    
    // Should throw an exception
    assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
        validateMethod.invoke(pluginLoader, invalidMetadata1);
    });
}
```
