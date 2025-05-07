# Testing

This guide explains how to test extension points, plugins, and their integration with the Firefly Plugin Manager.

## Table of Contents

1. [Testing Approach](#testing-approach)
2. [Testing Extension Points](#testing-extension-points)
3. [Testing Plugins](#testing-plugins)
4. [Integration Testing](#integration-testing)
5. [Test Utilities](#test-utilities)
6. [Best Practices](#best-practices)

## Testing Approach

Testing in the Firefly Plugin Manager ecosystem involves several layers:

1. **Unit Testing**: Testing individual components in isolation
2. **Component Testing**: Testing extension points and plugins separately
3. **Integration Testing**: Testing the interaction between plugins and the Plugin Manager
4. **System Testing**: Testing the entire system with plugins installed

Each layer builds on the previous one, providing increasing confidence in the system's correctness.

## Testing Extension Points

Extension points are the contracts that plugins implement, so it's crucial to test them thoroughly.

### 1. Interface Contract Testing

Test that the extension point interface defines a clear, consistent contract:

```java
@Test
void testPaymentProcessorContract() {
    // Verify that the extension point is properly annotated
    ExtensionPoint annotation = PaymentProcessor.class.getAnnotation(ExtensionPoint.class);
    assertNotNull(annotation);
    assertEquals("com.catalis.banking.payment-processor", annotation.id());
    assertTrue(annotation.allowMultiple());
    
    // Verify that the interface methods are well-defined
    Method[] methods = PaymentProcessor.class.getDeclaredMethods();
    assertTrue(Arrays.stream(methods)
            .anyMatch(m -> m.getName().equals("supportsPaymentMethod")));
    assertTrue(Arrays.stream(methods)
            .anyMatch(m -> m.getName().equals("processPayment")));
    assertTrue(Arrays.stream(methods)
            .anyMatch(m -> m.getName().equals("getPriority")));
}
```

### 2. Mock Implementation Testing

Test the extension point with a mock implementation:

```java
@Test
void testPaymentProcessorWithMockImplementation() {
    // Create a mock implementation
    PaymentProcessor mockProcessor = new PaymentProcessor() {
        @Override
        public boolean supportsPaymentMethod(String paymentMethod) {
            return "TEST".equals(paymentMethod);
        }
        
        @Override
        public Mono<String> processPayment(BigDecimal amount, String currency, String reference) {
            return Mono.just("payment-123");
        }
        
        @Override
        public int getPriority() {
            return 100;
        }
    };
    
    // Test the implementation
    assertTrue(mockProcessor.supportsPaymentMethod("TEST"));
    assertFalse(mockProcessor.supportsPaymentMethod("UNKNOWN"));
    
    String paymentId = mockProcessor.processPayment(
            new BigDecimal("100.00"), "USD", "order-456").block();
    assertEquals("payment-123", paymentId);
    
    assertEquals(100, mockProcessor.getPriority());
}
```

### 3. Extension Registry Testing

Test that the extension point can be registered with the Extension Registry:

```java
@Test
void testExtensionPointRegistration() {
    // Create an extension point
    ExtensionPoint extensionPoint = new ExtensionPointImpl(
            "com.catalis.banking.payment-processor",
            "Extension point for payment processing",
            true,
            PaymentProcessor.class);
    
    // Create an extension registry
    ExtensionRegistry registry = new DefaultExtensionRegistry();
    
    // Register the extension point
    registry.registerExtensionPoint(extensionPoint).block();
    
    // Verify that the extension point is registered
    ExtensionPoint registeredPoint = registry.getExtensionPoint(
            "com.catalis.banking.payment-processor").block();
    
    assertNotNull(registeredPoint);
    assertEquals("com.catalis.banking.payment-processor", registeredPoint.getId());
    assertEquals(PaymentProcessor.class, registeredPoint.getExtensionClass());
}
```

## Testing Plugins

Plugins should be tested both in isolation and with the Plugin Manager.

### 1. Plugin Unit Testing

Test the plugin class and its components in isolation:

```java
@Test
void testCreditCardPaymentPlugin() {
    // Create a mock event bus
    PluginEventBus mockEventBus = mock(PluginEventBus.class);
    
    // Create the plugin
    CreditCardPaymentPlugin plugin = new CreditCardPaymentPlugin(mockEventBus);
    
    // Verify plugin metadata
    PluginMetadata metadata = plugin.getMetadata();
    assertEquals("com.example.payment.credit-card", metadata.id());
    assertEquals("Credit Card Payment Processor", metadata.name());
    assertEquals("1.0.0", metadata.version());
    
    // Test plugin initialization
    plugin.initialize().block();
    
    // Test plugin start
    plugin.start().block();
    
    // Test plugin stop
    plugin.stop().block();
    
    // Test plugin uninstall
    plugin.uninstall().block();
}
```

### 2. Extension Implementation Testing

Test the extension implementations provided by the plugin:

```java
@Test
void testCreditCardPaymentProcessor() {
    // Create a mock gateway client
    PaymentGatewayClient mockClient = mock(PaymentGatewayClient.class);
    when(mockClient.processCreditCardPayment(any(), any(), any()))
            .thenReturn(Mono.just("payment-123"));
    
    // Create the processor
    CreditCardPaymentProcessor processor = new CreditCardPaymentProcessor(mockClient);
    
    // Test supportsPaymentMethod
    assertTrue(processor.supportsPaymentMethod("VISA"));
    assertTrue(processor.supportsPaymentMethod("MASTERCARD"));
    assertFalse(processor.supportsPaymentMethod("PAYPAL"));
    
    // Test processPayment
    String paymentId = processor.processPayment(
            new BigDecimal("100.00"), "USD", "order-456").block();
    
    assertEquals("payment-123", paymentId);
    verify(mockClient).processCreditCardPayment(
            new BigDecimal("100.00"), "USD", "order-456");
}
```

### 3. Plugin Configuration Testing

Test that the plugin handles configuration correctly:

```java
@Test
void testPluginConfiguration() {
    // Create a mock event bus
    PluginEventBus mockEventBus = mock(PluginEventBus.class);
    
    // Create the plugin
    CreditCardPaymentPlugin plugin = new CreditCardPaymentPlugin(mockEventBus);
    
    // Update configuration
    Map<String, Object> config = new HashMap<>();
    config.put("gatewayUrl", "https://test-gateway.com");
    config.put("apiKey", "test-api-key");
    config.put("timeout", 60);
    
    plugin.updateConfiguration(config).block();
    
    // Verify that configuration was applied
    // This would typically be done by checking the behavior of the plugin
    // or by exposing the configuration for testing
}
```

### 4. Event Handling Testing

Test that the plugin handles events correctly:

```java
@Test
void testEventHandling() {
    // Create a real event bus for testing
    InMemoryEventBus eventBus = new InMemoryEventBus();
    
    // Create the plugin
    CreditCardPaymentPlugin plugin = new CreditCardPaymentPlugin(eventBus);
    
    // Initialize and start the plugin
    plugin.initialize().block();
    plugin.start().block();
    
    // Create a test consumer to verify events
    AtomicReference<Map<String, Object>> receivedEvent = new AtomicReference<>();
    eventBus.subscribe("payment.processed", event -> {
        receivedEvent.set(event.getData());
    });
    
    // Publish an event that the plugin should handle
    eventBus.publish("payment.request", Map.of(
            "paymentMethod", "VISA",
            "amount", new BigDecimal("100.00"),
            "currency", "USD",
            "reference", "order-789"
    ));
    
    // Wait for the plugin to process the event
    await().atMost(5, TimeUnit.SECONDS).until(() -> receivedEvent.get() != null);
    
    // Verify the event data
    Map<String, Object> eventData = receivedEvent.get();
    assertNotNull(eventData);
    assertEquals("order-789", eventData.get("reference"));
    assertEquals(new BigDecimal("100.00"), eventData.get("amount"));
    assertEquals("USD", eventData.get("currency"));
    assertEquals("VISA", eventData.get("method"));
    assertNotNull(eventData.get("paymentId"));
}
```

## Integration Testing

Integration tests verify that plugins work correctly with the Plugin Manager.

### 1. Plugin Installation Testing

Test that a plugin can be installed and registered:

```java
@Test
void testPluginInstallation() {
    // Create a plugin manager
    PluginManager pluginManager = new DefaultPluginManager();
    
    // Install a plugin from a JAR file
    Plugin plugin = pluginManager.installPlugin(
            Paths.get("/path/to/plugins/credit-card-payment-plugin-1.0.0.jar"))
            .block();
    
    // Verify that the plugin is installed
    assertNotNull(plugin);
    assertEquals("com.example.payment.credit-card", plugin.getMetadata().id());
    
    // Verify that the plugin is in the registry
    Plugin registeredPlugin = pluginManager.getPluginRegistry()
            .getPlugin("com.example.payment.credit-card")
            .block();
    
    assertNotNull(registeredPlugin);
    assertEquals(plugin, registeredPlugin);
}
```

### 2. Plugin Lifecycle Testing

Test the plugin lifecycle with the Plugin Manager:

```java
@Test
void testPluginLifecycle() {
    // Create a plugin manager
    PluginManager pluginManager = new DefaultPluginManager();
    
    // Install a plugin
    pluginManager.installPlugin(
            Paths.get("/path/to/plugins/credit-card-payment-plugin-1.0.0.jar"))
            .block();
    
    // Initialize the plugin
    pluginManager.initializePlugin("com.example.payment.credit-card")
            .block();
    
    // Start the plugin
    pluginManager.startPlugin("com.example.payment.credit-card")
            .block();
    
    // Verify that the plugin is started
    PluginState state = pluginManager.getPluginState("com.example.payment.credit-card")
            .block();
    assertEquals(PluginState.STARTED, state);
    
    // Stop the plugin
    pluginManager.stopPlugin("com.example.payment.credit-card")
            .block();
    
    // Verify that the plugin is stopped
    state = pluginManager.getPluginState("com.example.payment.credit-card")
            .block();
    assertEquals(PluginState.STOPPED, state);
    
    // Uninstall the plugin
    pluginManager.uninstallPlugin("com.example.payment.credit-card")
            .block();
    
    // Verify that the plugin is uninstalled
    Mono<Plugin> pluginMono = pluginManager.getPluginRegistry()
            .getPlugin("com.example.payment.credit-card");
    
    StepVerifier.create(pluginMono)
            .expectError(PluginNotFoundException.class)
            .verify();
}
```

### 3. Extension Usage Testing

Test that extensions can be discovered and used:

```java
@Test
void testExtensionUsage() {
    // Create a plugin manager
    PluginManager pluginManager = new DefaultPluginManager();
    
    // Install and start a plugin
    pluginManager.installPlugin(
            Paths.get("/path/to/plugins/credit-card-payment-plugin-1.0.0.jar"))
            .block();
    
    pluginManager.initializePlugin("com.example.payment.credit-card")
            .block();
    
    pluginManager.startPlugin("com.example.payment.credit-card")
            .block();
    
    // Get extensions for the payment processor extension point
    Flux<Object> extensions = pluginManager.getExtensionRegistry()
            .getExtensions("com.catalis.banking.payment-processor");
    
    // Verify that the extension is available
    List<Object> extensionList = extensions.collectList().block();
    assertNotNull(extensionList);
    assertFalse(extensionList.isEmpty());
    
    // Cast and use the extension
    PaymentProcessor processor = (PaymentProcessor) extensionList.get(0);
    assertTrue(processor.supportsPaymentMethod("VISA"));
    
    // Process a payment
    String paymentId = processor.processPayment(
            new BigDecimal("100.00"), "USD", "order-123")
            .block();
    
    assertNotNull(paymentId);
}
```

## Test Utilities

The Firefly Plugin Manager provides several utilities to help with testing:

### 1. TestPluginManager

A simplified Plugin Manager for testing:

```java
public class TestPluginManager implements PluginManager {
    
    private final PluginRegistry pluginRegistry;
    private final ExtensionRegistry extensionRegistry;
    private final PluginEventBus eventBus;
    
    public TestPluginManager() {
        this.pluginRegistry = new DefaultPluginRegistry();
        this.extensionRegistry = new DefaultExtensionRegistry();
        this.eventBus = new InMemoryEventBus();
    }
    
    // Implementation of PluginManager methods...
    
    // Helper methods for testing
    public void registerTestPlugin(Plugin plugin) {
        pluginRegistry.registerPlugin(plugin).block();
    }
    
    public void registerTestExtensionPoint(ExtensionPoint extensionPoint) {
        extensionRegistry.registerExtensionPoint(extensionPoint).block();
    }
    
    public void registerTestExtension(String extensionPointId, Object extension, int priority) {
        extensionRegistry.registerExtension(
                new ExtensionImpl(extensionPointId, extension, priority))
                .block();
    }
}
```

### 2. TestPlugin

A simple plugin implementation for testing:

```java
public class TestPlugin implements Plugin {
    
    private final PluginMetadata metadata;
    
    public TestPlugin(String id, String name, String version) {
        this.metadata = PluginMetadata.builder()
                .id(id)
                .name(name)
                .version(version)
                .build();
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public Mono<Void> initialize() {
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> start() {
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> stop() {
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> uninstall() {
        return Mono.empty();
    }
}
```

### 3. TestExtensionPoint

A simple extension point implementation for testing:

```java
public class TestExtensionPoint implements ExtensionPoint {
    
    private final String id;
    private final String description;
    private final boolean allowMultiple;
    private final Class<?> extensionClass;
    
    public TestExtensionPoint(String id, Class<?> extensionClass) {
        this.id = id;
        this.description = "Test extension point";
        this.allowMultiple = true;
        this.extensionClass = extensionClass;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean allowMultiple() {
        return allowMultiple;
    }
    
    @Override
    public Class<?> getExtensionClass() {
        return extensionClass;
    }
}
```

## Best Practices

Follow these best practices when testing plugins and extension points:

### 1. Test Isolation

- Test each component in isolation before testing integration
- Use mocks for dependencies to isolate the component under test
- Reset the test environment between tests

### 2. Test Coverage

- Test all lifecycle methods of plugins
- Test all methods of extension implementations
- Test error handling and edge cases
- Test configuration handling

### 3. Integration Testing

- Test with the actual Plugin Manager when possible
- Test the full plugin lifecycle (install, initialize, start, stop, uninstall)
- Test extension discovery and usage

### 4. Performance Testing

- Test plugin startup and shutdown times
- Test extension lookup performance
- Test with multiple plugins installed

### 5. Reactive Testing

- Use StepVerifier for testing reactive streams
- Test both success and error cases
- Test cancellation and backpressure

### 6. Test Automation

- Automate tests as part of the build process
- Use continuous integration to run tests on every change
- Include plugin tests in the overall test suite

By following these testing practices, you can ensure that your plugins and extension points work correctly and reliably with the Firefly Plugin Manager.
