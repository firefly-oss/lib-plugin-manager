# Developing Plugins

This guide explains how to develop plugins for the Firefly Plugin Manager, from understanding the plugin contract to packaging and deploying your plugin.

## Table of Contents

1. [Plugin Structure](#plugin-structure)
2. [Creating a Plugin Project](#creating-a-plugin-project)
3. [Implementing the Plugin Interface](#implementing-the-plugin-interface)
4. [Creating Extensions](#creating-extensions)
5. [Plugin Configuration](#plugin-configuration)
6. [Event-Based Communication](#event-based-communication)
7. [Packaging and Deployment](#packaging-and-deployment)
8. [Testing Plugins](#testing-plugins)
9. [Best Practices](#best-practices)

## Plugin Structure

A typical plugin consists of several components:

- **Plugin Class**: The main class that implements the `Plugin` interface
- **Extension Classes**: Classes that implement extension points
- **Support Classes**: Helper classes, services, and utilities
- **Resources**: Configuration files, templates, and other resources
- **Dependencies**: Libraries required by the plugin

```
com.example.payment
├── CreditCardPaymentPlugin.java       # Main plugin class
├── extension
│   └── CreditCardPaymentProcessor.java # Extension implementation
├── service
│   └── PaymentGatewayClient.java      # Support service
├── model
│   └── CreditCardPayment.java         # Domain model
└── util
    └── CreditCardValidator.java       # Utility class
```

## Creating a Plugin Project

### 1. Set Up a Maven Project

Create a Maven project with the following structure:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>credit-card-payment-plugin</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Credit Card Payment Plugin</name>
    <description>A plugin for processing credit card payments</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <firefly.plugin.manager.version>1.0.0</firefly.plugin.manager.version>
    </properties>

    <dependencies>
        <!-- Firefly Plugin Manager API -->
        <dependency>
            <groupId>com.catalis.core</groupId>
            <artifactId>lib-plugin-manager-api</artifactId>
            <version>${firefly.plugin.manager.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- Extension Point Interfaces -->
        <dependency>
            <groupId>com.catalis.banking</groupId>
            <artifactId>core-banking-payments</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- Other dependencies -->
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-core</artifactId>
            <version>3.6.0</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <compilerArgs>
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <Plugin-Id>com.example.payment.credit-card</Plugin-Id>
                            <Plugin-Version>1.0.0</Plugin-Version>
                            <Plugin-Provider>Example Inc.</Plugin-Provider>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. Create the Plugin Class

Create the main plugin class that extends `AbstractPlugin`:

```java
package com.example.payment;

import com.catalis.core.plugin.annotation.Plugin;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.spi.AbstractPlugin;
import com.example.payment.service.PaymentGatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

/**
 * Plugin for processing credit card payments.
 */
@Plugin(
    id = "com.example.payment.credit-card",
    name = "Credit Card Payment Processor",
    version = "1.0.0",
    description = "Processes credit card payments with support for major card networks",
    author = "Example Inc."
)
public class CreditCardPaymentPlugin extends AbstractPlugin {
    
    private static final Logger logger = LoggerFactory.getLogger(CreditCardPaymentPlugin.class);
    
    private final PluginEventBus eventBus;
    private final PaymentGatewayClient gatewayClient;
    
    /**
     * Creates a new CreditCardPaymentPlugin.
     *
     * @param eventBus the event bus for inter-plugin communication
     */
    public CreditCardPaymentPlugin(PluginEventBus eventBus) {
        super(PluginMetadata.builder()
                .id("com.example.payment.credit-card")
                .name("Credit Card Payment Processor")
                .version("1.0.0")
                .description("Processes credit card payments with support for major card networks")
                .author("Example Inc.")
                .minPlatformVersion("1.0.0")
                .dependencies(Set.of())
                .installTime(Instant.now())
                .build());
        
        this.eventBus = eventBus;
        this.gatewayClient = new PaymentGatewayClient();
    }
    
    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing Credit Card Payment Plugin");
        return gatewayClient.initialize();
    }
    
    @Override
    public Mono<Void> start() {
        logger.info("Starting Credit Card Payment Plugin");
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> stop() {
        logger.info("Stopping Credit Card Payment Plugin");
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> uninstall() {
        logger.info("Uninstalling Credit Card Payment Plugin");
        return Mono.empty();
    }
}
```

## Implementing the Plugin Interface

The `Plugin` interface defines the lifecycle methods that your plugin must implement:

### 1. initialize()

The `initialize()` method is called when the plugin is first loaded:

```java
@Override
public Mono<Void> initialize() {
    logger.info("Initializing Credit Card Payment Plugin");
    
    // Initialize resources
    return gatewayClient.initialize()
            .doOnSuccess(v -> logger.info("Payment gateway client initialized"))
            .doOnError(e -> logger.error("Error initializing payment gateway client", e));
}
```

Use this method to:
- Initialize resources
- Load configuration
- Set up connections
- Prepare the plugin for use

### 2. start()

The `start()` method is called when the plugin is started:

```java
@Override
public Mono<Void> start() {
    logger.info("Starting Credit Card Payment Plugin");
    
    // Subscribe to events
    eventSubscription = eventBus.subscribe("payment.request", this::handlePaymentRequest);
    
    // Start services
    return gatewayClient.connect()
            .doOnSuccess(v -> logger.info("Payment gateway client connected"))
            .doOnError(e -> logger.error("Error connecting to payment gateway", e));
}
```

Use this method to:
- Activate the plugin
- Register extensions
- Subscribe to events
- Start background tasks

### 3. stop()

The `stop()` method is called when the plugin is stopped:

```java
@Override
public Mono<Void> stop() {
    logger.info("Stopping Credit Card Payment Plugin");
    
    // Unsubscribe from events
    if (eventSubscription != null) {
        eventSubscription.dispose();
    }
    
    // Stop services
    return gatewayClient.disconnect()
            .doOnSuccess(v -> logger.info("Payment gateway client disconnected"))
            .doOnError(e -> logger.error("Error disconnecting from payment gateway", e));
}
```

Use this method to:
- Deactivate the plugin
- Unregister extensions
- Unsubscribe from events
- Stop background tasks

### 4. uninstall()

The `uninstall()` method is called when the plugin is being uninstalled:

```java
@Override
public Mono<Void> uninstall() {
    logger.info("Uninstalling Credit Card Payment Plugin");
    
    // Clean up resources
    return gatewayClient.cleanup()
            .doOnSuccess(v -> logger.info("Payment gateway client cleaned up"))
            .doOnError(e -> logger.error("Error cleaning up payment gateway client", e));
}
```

Use this method to:
- Clean up resources
- Remove configuration
- Delete temporary files
- Prepare for removal

## Creating Extensions

Extensions are the implementations of extension points provided by your plugin:

### 1. Identify the Extension Point

First, identify the extension point you want to implement:

```java
// From the core-banking-payments microservice
@ExtensionPoint(
    id = "com.catalis.banking.payment-processor",
    description = "Extension point for payment processing services",
    allowMultiple = true
)
public interface PaymentProcessor {
    
    boolean supportsPaymentMethod(String paymentMethod);
    
    Mono<String> processPayment(BigDecimal amount, String currency, String reference);
    
    int getPriority();
}
```

### 2. Create the Extension Class

Create a class that implements the extension point:

```java
package com.example.payment.extension;

import com.catalis.banking.payment.PaymentProcessor;
import com.catalis.core.plugin.annotation.Extension;
import com.example.payment.service.PaymentGatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Implementation of the PaymentProcessor extension point for credit card payments.
 */
@Extension(
    extensionPointId = "com.catalis.banking.payment-processor",
    priority = 100,
    description = "Processes credit card payments"
)
public class CreditCardPaymentProcessor implements PaymentProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(CreditCardPaymentProcessor.class);
    
    private static final Set<String> SUPPORTED_METHODS = Set.of(
            "VISA", "MASTERCARD", "AMEX", "DISCOVER");
    
    private final PaymentGatewayClient gatewayClient;
    
    public CreditCardPaymentProcessor(PaymentGatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }
    
    @Override
    public boolean supportsPaymentMethod(String paymentMethod) {
        return SUPPORTED_METHODS.contains(paymentMethod.toUpperCase());
    }
    
    @Override
    public Mono<String> processPayment(BigDecimal amount, String currency, String reference) {
        logger.info("Processing credit card payment: amount={}, currency={}, reference={}",
                amount, currency, reference);
        
        // In a real implementation, card details would come from the request
        // Here we're using test values for demonstration
        return gatewayClient.processCreditCardPayment(amount, currency, reference);
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
}
```

### 3. Register the Extension

Register the extension with the plugin:

```java
@Override
public Mono<Void> initialize() {
    logger.info("Initializing Credit Card Payment Plugin");
    
    // Create the extension
    CreditCardPaymentProcessor paymentProcessor = new CreditCardPaymentProcessor(gatewayClient);
    
    // Register the extension
    return getPluginManager().getExtensionRegistry()
            .registerExtension(new ExtensionImpl(
                    "com.catalis.banking.payment-processor",
                    paymentProcessor,
                    100))
            .then(gatewayClient.initialize());
}
```

## Plugin Configuration

Plugins can have their own configuration that can be modified at runtime:

### 1. Define Configuration Properties

```java
public class CreditCardPaymentPlugin extends AbstractPlugin {
    
    // Configuration properties
    private final Map<String, Object> configuration = new ConcurrentHashMap<>();
    
    public CreditCardPaymentPlugin(PluginEventBus eventBus) {
        // ...
        
        // Default configuration
        configuration.put("gatewayUrl", "https://example.com/payment-gateway");
        configuration.put("apiKey", "default-api-key");
        configuration.put("timeout", 30);
        configuration.put("retryCount", 3);
    }
    
    // ...
}
```

### 2. Implement Configuration Update

```java
@Override
public Mono<Void> updateConfiguration(Map<String, Object> newConfig) {
    logger.info("Updating configuration: {}", newConfig);
    
    // Update configuration
    configuration.putAll(newConfig);
    
    // Apply configuration to components
    gatewayClient.setGatewayUrl((String) configuration.get("gatewayUrl"));
    gatewayClient.setApiKey((String) configuration.get("apiKey"));
    gatewayClient.setTimeout((Integer) configuration.get("timeout"));
    gatewayClient.setRetryCount((Integer) configuration.get("retryCount"));
    
    return Mono.empty();
}
```

### 3. Use Configuration

```java
public class PaymentGatewayClient {
    
    private String gatewayUrl;
    private String apiKey;
    private int timeout;
    private int retryCount;
    
    // Getters and setters...
    
    public Mono<String> processCreditCardPayment(BigDecimal amount, String currency, String reference) {
        // Use configuration properties
        WebClient webClient = WebClient.builder()
                .baseUrl(gatewayUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        
        return webClient.post()
                .uri("/process")
                .bodyValue(Map.of(
                        "amount", amount,
                        "currency", currency,
                        "reference", reference
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeout))
                .retry(retryCount)
                .map(response -> (String) response.get("paymentId"));
    }
}
```

## Event-Based Communication

Plugins can communicate with each other and with the application through events:

### 1. Subscribe to Events

```java
@Override
public Mono<Void> start() {
    logger.info("Starting Credit Card Payment Plugin");
    
    // Subscribe to payment request events
    paymentRequestSubscription = eventBus.subscribe("payment.request", this::handlePaymentRequest);
    
    // Subscribe to configuration update events
    configUpdateSubscription = eventBus.subscribe("config.update.credit-card", this::handleConfigUpdate);
    
    return Mono.empty();
}

private void handlePaymentRequest(Event event) {
    Map<String, Object> data = event.getData();
    String paymentMethod = (String) data.get("paymentMethod");
    
    if (SUPPORTED_METHODS.contains(paymentMethod.toUpperCase())) {
        BigDecimal amount = (BigDecimal) data.get("amount");
        String currency = (String) data.get("currency");
        String reference = (String) data.get("reference");
        
        gatewayClient.processCreditCardPayment(amount, currency, reference)
                .subscribe(paymentId -> {
                    // Publish payment processed event
                    eventBus.publish("payment.processed", Map.of(
                            "paymentId", paymentId,
                            "amount", amount,
                            "currency", currency,
                            "reference", reference,
                            "method", paymentMethod,
                            "processor", "credit-card",
                            "timestamp", Instant.now()
                    ));
                });
    }
}

private void handleConfigUpdate(Event event) {
    Map<String, Object> data = event.getData();
    updateConfiguration(data).subscribe();
}
```

### 2. Publish Events

```java
public Mono<String> processPayment(BigDecimal amount, String currency, String reference) {
    return gatewayClient.processCreditCardPayment(amount, currency, reference)
            .doOnSuccess(paymentId -> {
                // Publish payment processed event
                eventBus.publish("payment.processed", Map.of(
                        "paymentId", paymentId,
                        "amount", amount,
                        "currency", currency,
                        "reference", reference,
                        "processor", "credit-card",
                        "timestamp", Instant.now()
                ));
            });
}
```

### 3. Clean Up Subscriptions

```java
@Override
public Mono<Void> stop() {
    logger.info("Stopping Credit Card Payment Plugin");
    
    // Dispose of event subscriptions
    if (paymentRequestSubscription != null) {
        paymentRequestSubscription.dispose();
    }
    
    if (configUpdateSubscription != null) {
        configUpdateSubscription.dispose();
    }
    
    return Mono.empty();
}
```

## Packaging and Deployment

Packaging your plugin for deployment involves several steps:

### 1. Create a Service Provider Configuration

Create a file at `src/main/resources/META-INF/services/com.catalis.core.plugin.api.Plugin` with the fully qualified name of your plugin class:

```
com.example.payment.CreditCardPaymentPlugin
```

This enables the Service Provider Interface (SPI) mechanism to discover your plugin.

### 2. Build the Plugin JAR

Use Maven to build your plugin:

```bash
mvn clean package
```

This creates a JAR file in the `target` directory.

### 3. Deploy the Plugin

There are several ways to deploy your plugin:

#### Option 1: JAR File Installation

Copy the JAR file to the plugins directory:

```java
// In the application
pluginManager.installPlugin(Paths.get("/path/to/plugins/credit-card-payment-plugin-1.0.0.jar"))
        .block();
```

#### Option 2: Git Repository Installation

Install the plugin from a Git repository:

```java
// In the application
pluginManager.installPluginFromGit("https://github.com/example/credit-card-payment-plugin.git")
        .block();
```

#### Option 3: Classpath Auto-detection

Add the plugin as a dependency and use classpath scanning:

```java
// In the application
pluginManager.installPluginsFromClasspath("com.example.payment")
        .block();
```

## Testing Plugins

Testing your plugin is crucial to ensure it works correctly:

### 1. Unit Testing

Test individual components of your plugin:

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

### 2. Integration Testing

Test the plugin with the Plugin Manager:

```java
@Test
void testPluginLifecycle() {
    // Create a mock event bus
    PluginEventBus mockEventBus = mock(PluginEventBus.class);
    
    // Create the plugin
    CreditCardPaymentPlugin plugin = new CreditCardPaymentPlugin(mockEventBus);
    
    // Test initialize
    plugin.initialize().block();
    
    // Test start
    plugin.start().block();
    
    // Verify event subscription
    verify(mockEventBus).subscribe(eq("payment.request"), any());
    
    // Test stop
    plugin.stop().block();
    
    // Test uninstall
    plugin.uninstall().block();
}
```

### 3. End-to-End Testing

Test the plugin in a real environment:

```java
@SpringBootTest
class CreditCardPaymentPluginE2ETest {
    
    @Autowired
    private PluginManager pluginManager;
    
    @Autowired
    private PaymentService paymentService;
    
    @Test
    void testPaymentProcessing() {
        // Install the plugin
        pluginManager.installPlugin(Paths.get("/path/to/plugins/credit-card-payment-plugin-1.0.0.jar"))
                .block();
        
        // Start the plugin
        pluginManager.startPlugin("com.example.payment.credit-card")
                .block();
        
        // Process a payment
        String paymentId = paymentService.processPayment(
                "VISA", new BigDecimal("100.00"), "USD", "order-456").block();
        
        assertNotNull(paymentId);
        
        // Stop the plugin
        pluginManager.stopPlugin("com.example.payment.credit-card")
                .block();
        
        // Uninstall the plugin
        pluginManager.uninstallPlugin("com.example.payment.credit-card")
                .block();
    }
}
```

## Best Practices

Follow these best practices when developing plugins:

### 1. Plugin Design

- **Single Responsibility**: Each plugin should have a focused purpose
- **Minimal Dependencies**: Minimize dependencies on external libraries
- **Proper Isolation**: Avoid interfering with other plugins
- **Graceful Degradation**: Handle errors and missing dependencies gracefully
- **Resource Management**: Clean up resources when the plugin is stopped

### 2. Extension Implementation

- **Follow the Contract**: Implement all methods defined by the extension point
- **Respect Semantics**: Follow the semantics defined by the extension point
- **Handle Errors**: Properly handle and report errors
- **Be Reactive**: Use reactive programming patterns consistently
- **Document Behavior**: Document how your extension behaves

### 3. Configuration

- **Provide Defaults**: Always provide sensible default configuration
- **Validate Configuration**: Validate configuration values
- **Document Configuration**: Document all configuration properties
- **Support Runtime Updates**: Handle configuration updates at runtime
- **Secure Sensitive Data**: Protect sensitive configuration data

### 4. Event Handling

- **Use Specific Topics**: Use specific, well-named event topics
- **Document Events**: Document events that your plugin publishes or consumes
- **Handle Errors**: Properly handle errors in event handlers
- **Clean Up Subscriptions**: Dispose of subscriptions when the plugin stops
- **Be Selective**: Only subscribe to events that your plugin needs

### 5. Testing

- **Unit Test Components**: Test individual components in isolation
- **Test Extensions**: Test extension implementations
- **Test Lifecycle**: Test plugin lifecycle methods
- **Test Configuration**: Test configuration handling
- **Test Events**: Test event publishing and handling
- **Test Error Handling**: Test how your plugin handles errors

By following these guidelines, you can create robust, maintainable plugins that extend the Firefly Platform in a clean, modular way.
