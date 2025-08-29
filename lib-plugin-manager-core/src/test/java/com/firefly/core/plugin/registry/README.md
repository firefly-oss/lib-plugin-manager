# Plugin Registry Tests

This package contains tests for the plugin registry component of the Firefly Plugin Manager.

## DefaultPluginRegistryTest

Tests the `DefaultPluginRegistry` implementation, which is responsible for managing the lifecycle of plugins.

### Test Cases

- **testRegisterPlugin**: Verifies that a plugin can be registered and initialized correctly.
- **testStartPlugin**: Verifies that a registered plugin can be started.
- **testStopPlugin**: Verifies that a started plugin can be stopped.
- **testUpdatePluginConfiguration**: Verifies that a plugin's configuration can be updated.
- **testGetPluginsByState**: Verifies that plugins can be filtered by their state.
- **testStartPluginError**: Verifies that errors during plugin startup are handled correctly.

### Key Assertions

- Plugin state transitions are correctly tracked
- Lifecycle events are published when plugin states change
- Plugin configuration is correctly stored and updated
- Error handling works as expected

## Example

```java
@Test
void testRegisterPlugin() {
    // Register the plugin
    StepVerifier.create(registry.registerPlugin(plugin))
            .verifyComplete();
    
    // Verify the plugin is registered
    StepVerifier.create(registry.getPlugin("test-plugin"))
            .expectNext(plugin)
            .verifyComplete();
    
    // Verify the plugin descriptor
    StepVerifier.create(registry.getPluginDescriptor("test-plugin"))
            .assertNext(descriptor -> {
                assertEquals("test-plugin", descriptor.metadata().id());
                assertEquals(PluginState.INITIALIZED, descriptor.state());
            })
            .verifyComplete();
    
    // Verify lifecycle event was published
    ArgumentCaptor<PluginLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(PluginLifecycleEvent.class);
    verify(eventBus).publish(eventCaptor.capture());
    
    PluginLifecycleEvent event = eventCaptor.getValue();
    assertEquals("test-plugin", event.getPluginId());
    assertEquals(PluginState.INSTALLED, event.getPreviousState());
    assertEquals(PluginState.INITIALIZED, event.getNewState());
}
```
