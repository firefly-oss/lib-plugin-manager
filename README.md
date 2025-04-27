# Firefly Plugin Manager

A modular plugin system for extending Firefly Core Banking Platform functionality.

## Table of Contents

- [Overview](#overview)
- [Core Concepts](#core-concepts)
  - [Plugin](#plugin)
  - [Extension Point](#extension-point)
  - [Extension](#extension)
  - [Plugin Lifecycle](#plugin-lifecycle)
- [Architecture](#architecture)
  - [Plugin Registry](#plugin-registry)
  - [Extension Registry](#extension-registry)
  - [Event Bus](#event-bus)
  - [Plugin Loader](#plugin-loader)
- [Extension Points in Microservices](#extension-points-in-microservices)
  - [Defining Extension Points](#defining-extension-points)
  - [Microservice-Plugin Relationship](#microservice-plugin-relationship)
  - [Extension Point Design Principles](#extension-point-design-principles)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Basic Usage](#basic-usage)
    - [Plugin Installation Methods](#plugin-installation-methods)
    - [Auto-detection with Annotation-based Plugins](#auto-detection-with-annotation-based-plugins)
- [Developing Plugins](#developing-plugins)
  - [Understanding the Plugin Contract](#understanding-the-plugin-contract)
  - [Step 1: Define an Extension Point](#step-1-define-an-extension-point)
  - [Step 2: Create a Plugin with Extensions](#step-2-create-a-plugin-with-extensions)
  - [Step 3: Plugin Configuration](#step-3-plugin-configuration)
    - [Updating Plugin Configuration](#updating-plugin-configuration)
    - [Configuration Sources](#configuration-sources)
  - [Step 4: Event-Based Communication](#step-4-event-based-communication)
  - [Step 5: Package the Plugin](#step-5-package-the-plugin)
- [Configuration](#configuration)
  - [Event Bus Configuration](#event-bus-configuration)
    - [Choosing an Event Bus Implementation](#choosing-an-event-bus-implementation)
    - [In-Memory Event Bus (Default)](#in-memory-event-bus-default)
    - [Kafka Event Bus (Optional)](#kafka-event-bus-optional)
    - [Event Serialization for Kafka](#event-serialization-for-kafka)
    - [Custom Serialization](#custom-serialization)
    - [Event Bus Architecture](#event-bus-architecture)
    - [Event Bus Best Practices](#event-bus-best-practices)
    - [Switching Between Event Bus Implementations](#switching-between-event-bus-implementations)
    - [Monitoring and Troubleshooting](#monitoring-and-troubleshooting)
- [Security Considerations](#security-considerations)
  - [Class Loading Isolation](#class-loading-isolation)
  - [Permission System](#permission-system)
  - [Signature Verification](#signature-verification)
  - [Resource Limiting](#resource-limiting)
  - [Sandboxing](#sandboxing)
  - [Security Best Practices](#security-best-practices)
- [Advanced Topics](#advanced-topics)
  - [Plugin Dependency Management](#plugin-dependency-management)
  - [Hot Deployment](#hot-deployment)
  - [Plugin Versioning and Compatibility](#plugin-versioning-and-compatibility)
  - [Plugin Health Monitoring](#plugin-health-monitoring)
  - [Plugin Testing Framework](#plugin-testing-framework)
- [Complete Example: Microservice Extension Point and Plugin Implementation](#complete-example-microservice-extension-point-and-plugin-implementation)

## Overview

The Firefly Plugin Manager provides a standardized way to develop and manage plugins to extend core functionality of the Firefly Platform. It is built on Spring Boot 3.2.2, WebFlux, and Java 21, using reactive programming principles. This framework enables financial institutions to extend and customize the Firefly platform without modifying the core codebase, ensuring stability, security, and maintainability.

## Core Concepts

### Plugin

A plugin is a self-contained module that encapsulates specific functionality and can be dynamically loaded, started, stopped, and unloaded at runtime without requiring a system restart. Each plugin:

- Has a unique identity and metadata (ID, name, version, description, author)
- Follows a well-defined lifecycle (installed → initialized → started → stopped → uninstalled)
- Can depend on other plugins, creating a dependency graph
- Can provide one or more extensions to extend the core system's functionality
- Has its own configuration that can be modified at runtime
- Can be isolated from other plugins to prevent interference

Plugins are the primary unit of modularity in the system, allowing for a clean separation of concerns and enabling third-party developers to extend the platform without access to the core codebase.

### Extension Point

An extension point is a contract (typically an interface) that is defined within a core microservice and specifies how that microservice can be extended. Extension points:

- Are defined in core microservices (like core-banking-cards, core-banking-accounts)
- Define a clear API that extensions must implement
- Are identified by a unique ID
- Can specify whether they allow single or multiple implementations
- Provide metadata about their purpose and requirements
- Can be discovered dynamically at runtime

Extension points are the foundation of the plugin system's flexibility, as they define the "slots" where plugins can "plug in" their functionality. They represent the stable API that both the core microservices and plugins can rely on. By defining extension points within the core microservices, the system ensures that extensions are properly integrated with the core functionality while maintaining separation of concerns.

### Extension

An extension is a concrete implementation of an extension point provided by a plugin. Extensions:

- Implement the contract defined by an extension point in a core microservice
- Are associated with a specific plugin
- Can have a priority to determine their order when multiple extensions exist
- Can be discovered and used dynamically at runtime by core microservices
- Can be enabled or disabled independently of their plugin

Extensions are how plugins actually contribute functionality to the core microservices. They represent the "plugs" that fit into the "slots" defined by extension points. When a core microservice needs to use extended functionality, it queries the Plugin Manager for all available implementations of a specific extension point, and then uses those implementations to enhance its capabilities.

### Plugin Lifecycle

Plugins follow a well-defined lifecycle that allows the system to manage them properly:

1. **INSTALLED**: The plugin has been loaded into the system but not yet initialized. Its classes are available, but it's not ready for use.

2. **INITIALIZED**: The plugin has been initialized and its resources have been allocated, but it's not actively providing functionality yet. This state allows for setup tasks that need to happen before the plugin starts.

3. **STARTED**: The plugin is active and its extensions are available for use. It may be running background tasks or providing services.

4. **STOPPED**: The plugin has been temporarily deactivated. Its extensions are no longer available, but it remains initialized and can be quickly restarted.

5. **FAILED**: The plugin encountered an error during initialization, starting, or operation. This state indicates that intervention is needed.

6. **UNINSTALLED**: The plugin has been completely removed from the system. Its classes are unloaded and its resources are released.

Each state transition triggers events that other components can observe and react to, enabling coordinated behavior across the system.

## Architecture

The plugin manager consists of the following main components:

### Plugin Registry

The Plugin Registry is the central repository for all plugins in the system. It:

- Maintains a registry of all installed plugins and their metadata
- Tracks the current state of each plugin (installed, initialized, started, stopped, failed, uninstalled)
- Manages plugin lifecycle transitions (initialization, starting, stopping, uninstallation)
- Resolves plugin dependencies to ensure proper loading order
- Provides APIs to query plugins by ID, state, capability, or other criteria
- Publishes events when plugin states change
- Manages plugin configuration storage and updates

The Plugin Registry is the authoritative source of information about plugins in the system and the primary means of controlling their lifecycle.

### Extension Registry

The Extension Registry manages the relationship between extension points and their implementations. It:

- Maintains a registry of all extension points defined in the system
- Tracks all extensions that implement each extension point
- Manages extension priorities to determine their order
- Provides APIs to query extensions by extension point, plugin, or other criteria
- Handles dynamic registration and unregistration of extensions as plugins are started and stopped
- Supports filtering extensions based on capabilities or other criteria

The Extension Registry is the mechanism that enables the dynamic discovery and use of functionality provided by plugins.

### Event Bus

The Event Bus provides a decoupled communication mechanism between plugins and the core system. It:

- Implements a publish-subscribe pattern using reactive streams (Flux/Mono)
- Allows plugins to publish events without knowing who will consume them
- Enables plugins to subscribe to events without knowing who publishes them
- Supports filtering events by type, source, or content
- Provides both synchronous and asynchronous event handling
- Ensures that event subscribers don't block publishers
- Maintains a history of recent events for late subscribers
- Supports both in-memory and distributed (Kafka) transport mechanisms
- Enables cross-instance communication in clustered environments

The Event Bus is essential for loose coupling between components, enabling a truly modular system where plugins can interact without direct dependencies. The system supports two transport mechanisms:

#### In-Memory Event Bus

The default in-memory implementation provides high-performance event processing within a single JVM instance. It's ideal for standalone deployments and testing environments.

#### Kafka Event Bus (Optional)

The Kafka-based implementation is optional and requires additional dependencies. When enabled, it provides distributed event processing across multiple instances of the application with these benefits:

- Reliable event delivery with persistence
- Horizontal scalability for high-volume event processing
- Cross-instance communication in clustered environments
- Event replay capabilities for recovery scenarios
- Topic-based routing for efficient event distribution

The transport mechanism can be configured through application properties, allowing for easy switching between in-memory and Kafka modes based on deployment requirements.

### Plugin Loader

The Plugin Loader is responsible for loading plugin code into the system. It:

- Loads plugin classes from JAR files or other sources
- Creates isolated class loaders for each plugin to prevent interference
- Validates plugin metadata and dependencies before loading
- Performs security checks on plugin code
- Manages the plugin class loading hierarchy
- Handles plugin unloading and class garbage collection

The Plugin Loader is critical for the dynamic nature of the plugin system and for maintaining security boundaries between plugins.

## Extension Points in Microservices

A key architectural principle of the Firefly Plugin Manager is that extension points are defined within core microservices, while plugins implement these extension points. This section explains this critical relationship and how it enables a flexible, maintainable system.

### Defining Extension Points

Extension points are contracts (interfaces) that are defined within Firefly core microservices. Each microservice identifies specific areas where its functionality can be extended or customized, and defines extension points for these areas.

**Key characteristics of extension points in microservices:**

- Extension points are defined in core microservices (e.g., core-banking-cards, core-banking-accounts)
- They represent stable contracts that plugins must implement
- They are designed to evolve slowly and maintain backward compatibility
- They are typically defined as Java interfaces with clear documentation
- They are annotated with `@ExtensionPoint` to provide metadata

**Example of an extension point defined in a core microservice:**

```java
// Defined in the core-banking-cards microservice
@ExtensionPoint(
        id = "com.catalis.banking.cards.fraud-detector",
        description = "Extension point for card transaction fraud detection",
        allowMultiple = true
)
public interface FraudDetector {
    /**
     * Analyzes a card transaction for potential fraud.
     *
     * @param transaction the transaction details
     * @return a Mono that emits a fraud analysis result
     */
    Mono<FraudAnalysisResult> analyzeTransaction(CardTransaction transaction);

    /**
     * Gets the name of this fraud detection method.
     *
     * @return the method name
     */
    String getDetectionMethodName();

    /**
     * Gets the confidence level of this detector.
     *
     * @return confidence level between 0.0 and 1.0
     */
    double getConfidenceLevel();
}
```

### Microservice-Plugin Relationship

The relationship between microservices and plugins is fundamental to the Firefly architecture:

1. **Core Microservices Define Extension Points**: Each core microservice (like core-banking-cards) defines extension points that represent specific capabilities that can be extended.

2. **Plugins Implement Extension Points**: Plugins provide concrete implementations of these extension points, extending the functionality of core microservices without modifying their code.

3. **Plugin Manager Connects Them**: The Plugin Manager acts as the bridge between microservices and plugins, allowing microservices to discover and use plugin implementations at runtime.

4. **Loose Coupling**: This architecture ensures loose coupling between core functionality and extensions, allowing each to evolve independently.

**Diagram: Microservice-Plugin Relationship**

```mermaid
graph TB
%% ──────────────────  PLATFORMS  ──────────────────
    subgraph fireflyCore["Firefly Core Platform"]
        subgraph core-banking-cards
            ExtPointA["Extension Point A"]:::extensionPoint
            ExtPointB["Extension Point B"]:::extensionPoint
        end

        subgraph core-banking-accounts
            ExtPointC["Extension Point C"]:::extensionPoint
            ExtPointD["Extension Point D"]:::extensionPoint
        end
    end

    subgraph pluginEco["Plugin Ecosystem"]
        subgraph "Plugin 1"
            ExtImplA["Extension A Implementation"]:::extension
            ExtImplC["Extension C Implementation"]:::extension
        end

        subgraph "Plugin 2"
            ExtImplB["Extension B Implementation"]:::extension
            ExtImplD["Extension D Implementation"]:::extension
        end
    end

%% ────────────────────  EDGES  ────────────────────
    ExtImplA --> ExtPointA
    ExtImplB --> ExtPointB
    ExtImplC --> ExtPointC
    ExtImplD --> ExtPointD

%% ───────────────────  CLASSES  ───────────────────
    classDef extensionPoint fill:#f9f,stroke:#333,stroke-width:2px
    classDef extension     fill:#bbf,stroke:#333,stroke-width:1px

%% ────────────────────  STYLES  ───────────────────
    style fireflyCore fill:#e6ffe6,stroke:#333,stroke-width:2px
    style pluginEco  fill:#e6e6ff,stroke:#333,stroke-width:2px

```

The diagram illustrates how:
- Core microservices (like core-banking-cards and core-banking-accounts) define extension points
- Plugins provide implementations of these extension points
- The Plugin Manager (not shown) connects the extension points with their implementations
- This architecture allows for extending functionality without modifying core code

### Extension Point Design Principles

When designing extension points in core microservices, follow these principles:

1. **Stability**: Extension points should be stable contracts that change infrequently, as they form the foundation of the plugin ecosystem.

2. **Specificity**: Define extension points for specific, well-defined capabilities rather than general-purpose hooks.

3. **Versioning**: Design extension points with versioning in mind, allowing for evolution while maintaining backward compatibility.

4. **Documentation**: Thoroughly document the contract, including expected behavior, threading model, and error handling.

5. **Granularity**: Choose the right level of granularity—too fine-grained leads to complexity, too coarse-grained limits flexibility.

6. **Independence**: Extension points should be independent of specific implementation details.

7. **Reactive Design**: Design extension points to work well with reactive programming patterns, using Mono/Flux return types for asynchronous operations.

8. **Error Handling**: Define clear error handling expectations and recovery mechanisms.

9. **Security Boundaries**: Consider security implications and define appropriate permission requirements.

10. **Performance Considerations**: Document performance expectations and constraints for implementations.

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Installation

Add the following dependency to your pom.xml:

```xml
<dependency>
    <groupId>com.catalis</groupId>
    <artifactId>lib-plugin-manager-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

1. **Initialize the Plugin Manager**

```java
@Autowired
private PluginManager pluginManager;

// Initialize the plugin manager
pluginManager.initialize().block();
```

2. **Register an Extension Point**

```java
// Register an extension point
pluginManager.getExtensionRegistry()
        .registerExtensionPoint("com.example.extension-point", MyExtensionPoint.class)
        .block();
```

3. **Install a Plugin**

#### Plugin Installation Methods

The Firefly Plugin Manager supports multiple methods for installing plugins, giving you flexibility in how you deploy and manage your plugin ecosystem:

```java
// Method 1: Install a plugin from a JAR file (traditional method)
// This loads a plugin from a JAR file in the filesystem
PluginDescriptor descriptor = pluginManager.installPlugin(Path.of("plugins/my-plugin.jar")).block();

// Method 2: Install a plugin from a Git repository
// This clones the repository and builds the plugin from source
URI repositoryUri = URI.create("https://github.com/example/my-plugin.git");
PluginDescriptor gitDescriptor = pluginManager.installPluginFromGit(repositoryUri).block();

// Method 3: Install a plugin from a Git repository with a specific branch
// This allows you to target a specific branch, tag, or commit
PluginDescriptor branchDescriptor = pluginManager.installPluginFromGit(
        URI.create("https://github.com/example/my-plugin.git"), "develop").block();

// Method 4: Auto-detect and install plugins from the classpath
// This automatically discovers all plugins in the application's classpath
// Useful when plugins are included as direct dependencies
Flux<PluginDescriptor> descriptors = pluginManager.installPluginsFromClasspath().collectList().block();

// Method 5: Auto-detect and install plugins from a specific package
// This limits auto-detection to a specific package namespace
Flux<PluginDescriptor> packageDescriptors = pluginManager.installPluginsFromClasspath("com.example.plugins").collectList().block();

```

#### Auto-detection with Annotation-based Plugins

For a more seamless integration, you can use annotation-based auto-detection:

```java
// 1. Add plugins as dependencies in your pom.xml
// <dependency>
//     <groupId>com.example</groupId>
//     <artifactId>payment-plugins</artifactId>
//     <version>1.0.0</version>
// </dependency>

// 2. Configure auto-detection in application.properties
// firefly.plugin-manager.scan-on-startup=true
// firefly.plugin-manager.auto-start-plugins=true

// 3. The Plugin Manager will automatically discover and start all plugins
// that have the @Plugin annotation and are in the classpath
```

Choose the installation method that best fits your deployment strategy. For more details on packaging and deploying plugins, see the [Step 5: Package the Plugin](#step-5-package-the-plugin) section.

4. **Start a Plugin**

```java
// Start a plugin
pluginManager.startPlugin("com.example.my-plugin").block();
```

5. **Get Extensions**

```java
// Get all extensions for an extension point
Flux<MyExtensionPoint> extensions = pluginManager.getExtensionRegistry()
        .getExtensions("com.example.extension-point");

// Get the highest priority extension
MyExtensionPoint extension = pluginManager.getExtensionRegistry()
        .getHighestPriorityExtension("com.example.extension-point")
        .block();
```

## Developing Plugins

Developing plugins for the Firefly Plugin Manager involves understanding several key concepts and following a structured approach. This section provides a comprehensive guide to creating effective plugins.

### Understanding the Plugin Contract

At its core, a plugin must implement the `Plugin` interface, which defines the fundamental lifecycle methods:

```java
public interface Plugin {
    // Returns metadata about the plugin
    PluginMetadata getMetadata();

    // Called when the plugin is first loaded
    Mono<Void> initialize();

    // Called when the plugin is started
    Mono<Void> start();

    // Called when the plugin is stopped
    Mono<Void> stop();

    // Called when the plugin is being uninstalled
    Mono<Void> uninstall();
}
```

These methods define the plugin's lifecycle and are called by the Plugin Manager at appropriate times. Each method returns a `Mono<Void>` to support reactive programming patterns, allowing for asynchronous operations during lifecycle transitions.

For convenience, the framework provides an `AbstractPlugin` base class that implements common functionality:

```java
public abstract class AbstractPlugin implements Plugin {
    private final PluginMetadata metadata;

    protected AbstractPlugin(PluginMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }

    // Default implementations of lifecycle methods
    // ...
}
```

### Step 1: Define an Extension Point

Extension points are the foundation of the plugin system. They define the contract that plugins must implement to extend specific functionality. An extension point is typically an interface annotated with `@ExtensionPoint`:

```java
@ExtensionPoint(
        id = "com.catalis.banking.payment-processor",
        description = "Extension point for payment processing services",
        allowMultiple = true
)
public interface PaymentProcessor {
    /**
     * Gets the name of this payment method.
     *
     * @return the payment method name
     */
    String getPaymentMethodName();

    /**
     * Processes a payment transaction.
     *
     * @param amount the payment amount
     * @param currency the payment currency code (ISO 4217)
     * @param reference a unique reference for this transaction
     * @return a Mono that emits the transaction ID when complete
     */
    Mono<String> processPayment(BigDecimal amount, String currency, String reference);

    /**
     * Checks if this processor supports a specific currency.
     *
     * @param currency the currency code to check (ISO 4217)
     * @return true if supported, false otherwise
     */
    boolean supportsCurrency(String currency);

    /**
     * Gets the fee structure for this payment method.
     *
     * @return the fee structure
     */
    FeeStructure getFeeStructure();
}
```

When designing extension points, consider the following best practices:

- **Stability**: Extension points should be stable contracts that change infrequently
- **Cohesion**: Each extension point should have a single, well-defined responsibility
- **Granularity**: Extension points should be fine-grained enough to allow for flexible implementation but not so fine-grained that they become cumbersome
- **Documentation**: Thoroughly document the contract, including expected behavior, threading model, and error handling
- **Versioning**: Consider how the extension point will evolve over time and how backward compatibility will be maintained

The `@ExtensionPoint` annotation provides metadata about the extension point:

- **id**: A unique identifier for the extension point (use reverse-domain naming convention)
- **description**: A human-readable description of the extension point's purpose
- **allowMultiple**: Whether multiple implementations of this extension point can be active simultaneously

### Step 2: Create a Plugin with Extensions

A plugin is a concrete implementation that can provide one or more extensions. Here's a comprehensive example:

```java
@Plugin(
        id = "com.catalis.banking.credit-card-payment",
        name = "Credit Card Payment Processor",
        version = "1.0.0",
        description = "Processes credit card payments with support for major card networks",
        author = "Catalis Financial Services",
        minPlatformVersion = "1.0.0",
        dependencies = {"com.catalis.banking.core-services"}
)
public class CreditCardPaymentPlugin extends AbstractPlugin {

    private static final Logger logger = LoggerFactory.getLogger(CreditCardPaymentPlugin.class);

    private final CreditCardPaymentProcessor paymentProcessor;
    private final PluginEventBus eventBus;
    private final PaymentGatewayClient gatewayClient;
    private Disposable eventSubscription;

    /**
     * Creates a new CreditCardPaymentPlugin.
     *
     * @param eventBus the event bus for inter-plugin communication
     */
    public CreditCardPaymentPlugin(PluginEventBus eventBus) {
        super(PluginMetadata.builder()
                .id("com.catalis.banking.credit-card-payment")
                .name("Credit Card Payment Processor")
                .version("1.0.0")
                .description("Processes credit card payments with support for major card networks")
                .author("Catalis Financial Services")
                .minPlatformVersion("1.0.0")
                .dependencies(Set.of("com.catalis.banking.core-services"))
                .build());

        this.eventBus = eventBus;
        this.gatewayClient = new PaymentGatewayClient();
        this.paymentProcessor = new CreditCardPaymentProcessor();
    }

    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing Credit Card Payment Plugin");

        // Initialize the payment gateway client
        return gatewayClient.initialize()
                .doOnSuccess(v -> logger.info("Payment gateway client initialized successfully"))
                .doOnError(e -> logger.error("Failed to initialize payment gateway client", e));
    }

    @Override
    public Mono<Void> start() {
        logger.info("Starting Credit Card Payment Plugin");

        // Subscribe to relevant events
        eventSubscription = eventBus.subscribe(PaymentRequestEvent.class)
                .filter(event -> event.getPaymentMethod().equals("CREDIT_CARD"))
                .flatMap(event -> paymentProcessor.processPayment(
                        event.getAmount(),
                        event.getCurrency(),
                        event.getReference())
                        .map(transactionId -> new PaymentCompletedEvent(
                                getMetadata().id(),
                                event.getReference(),
                                transactionId,
                                event.getAmount(),
                                event.getCurrency()))
                        .flatMap(eventBus::publish)
                        .onErrorResume(error -> {
                            logger.error("Payment processing error", error);
                            return eventBus.publish(new PaymentFailedEvent(
                                    getMetadata().id(),
                                    event.getReference(),
                                    error.getMessage()));
                        }))
                .subscribe();

        // Connect to the payment gateway
        return gatewayClient.connect()
                .doOnSuccess(v -> logger.info("Connected to payment gateway"))
                .doOnError(e -> logger.error("Failed to connect to payment gateway", e));
    }

    @Override
    public Mono<Void> stop() {
        logger.info("Stopping Credit Card Payment Plugin");

        // Dispose of event subscriptions
        if (eventSubscription != null && !eventSubscription.isDisposed()) {
            eventSubscription.dispose();
        }

        // Disconnect from the payment gateway
        return gatewayClient.disconnect()
                .doOnSuccess(v -> logger.info("Disconnected from payment gateway"))
                .doOnError(e -> logger.error("Error disconnecting from payment gateway", e));
    }

    @Override
    public Mono<Void> uninstall() {
        logger.info("Uninstalling Credit Card Payment Plugin");

        // Clean up any persistent resources
        return Mono.fromRunnable(() -> {
            // Remove any stored data, close connections, etc.
        });
    }

    /**
     * Gets the payment processor extension.
     *
     * @return the payment processor
     */
    public PaymentProcessor getPaymentProcessor() {
        return paymentProcessor;
    }

    /**
     * Implementation of the PaymentProcessor extension point.
     */
    @Extension(
            extensionPointId = "com.catalis.banking.payment-processor",
            priority = 100,
            description = "Credit card payment processor supporting Visa, Mastercard, and Amex"
    )
    public class CreditCardPaymentProcessor implements PaymentProcessor {

        @Override
        public String getPaymentMethodName() {
            return "Credit Card";
        }

        @Override
        public Mono<String> processPayment(BigDecimal amount, String currency, String reference) {
            logger.info("Processing credit card payment: amount={}, currency={}, reference={}",
                    amount, currency, reference);

            // Validate inputs
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return Mono.error(new IllegalArgumentException("Payment amount must be positive"));
            }

            if (!supportsCurrency(currency)) {
                return Mono.error(new UnsupportedOperationException("Currency not supported: " + currency));
            }

            // Process the payment through the gateway
            return gatewayClient.processPayment(amount, currency, reference)
                    .doOnSuccess(transactionId -> {
                        logger.info("Payment processed successfully: transactionId={}", transactionId);
                    })
                    .doOnError(error -> {
                        logger.error("Payment processing failed: {}", error.getMessage());
                    });
        }

        @Override
        public boolean supportsCurrency(String currency) {
            // Support major currencies
            return Set.of("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF").contains(currency);
        }

        @Override
        public FeeStructure getFeeStructure() {
            return new FeeStructure(
                    new BigDecimal("0.029"),  // 2.9% percentage fee
                    new BigDecimal("0.30"),   // $0.30 fixed fee
                    "USD"                     // Fee currency
            );
        }
    }

    /**
     * Mock payment gateway client for demonstration purposes.
     */
    private static class PaymentGatewayClient {

        Mono<Void> initialize() {
            // Simulate initialization
            return Mono.delay(Duration.ofMillis(500)).then();
        }

        Mono<Void> connect() {
            // Simulate connection establishment
            return Mono.delay(Duration.ofMillis(300)).then();
        }

        Mono<Void> disconnect() {
            // Simulate disconnection
            return Mono.delay(Duration.ofMillis(200)).then();
        }

        Mono<String> processPayment(BigDecimal amount, String currency, String reference) {
            // Simulate payment processing with the gateway
            return Mono.delay(Duration.ofMillis(1000))
                    .map(ignored -> UUID.randomUUID().toString());
        }
    }
}
```

When implementing a plugin, consider these best practices:

- **Lifecycle Management**: Properly implement all lifecycle methods, ensuring resources are acquired in `initialize`/`start` and released in `stop`/`uninstall`
- **Error Handling**: Use reactive error handling patterns to gracefully handle failures
- **Resource Management**: Dispose of subscriptions and close connections when the plugin is stopped
- **Thread Safety**: Ensure your plugin is thread-safe, as it may be called from multiple threads
- **Configuration**: Design your plugin to be configurable without requiring code changes
- **Logging**: Use appropriate logging levels and include contextual information
- **Event-Based Communication**: Use the event bus for communication rather than direct method calls

The `@Plugin` annotation provides metadata about the plugin:

- **id**: A unique identifier for the plugin (use reverse-domain naming convention)
- **name**: A human-readable name for the plugin
- **version**: The plugin version (using semantic versioning)
- **description**: A description of the plugin's functionality
- **author**: The author or organization that created the plugin
- **minPlatformVersion**: The minimum required version of the platform
- **maxPlatformVersion**: The maximum compatible version of the platform (optional)
- **dependencies**: IDs of other plugins that this plugin depends on

The `@Extension` annotation marks a class as an implementation of an extension point:

- **extensionPointId**: The ID of the extension point being implemented
- **priority**: The priority of this implementation (higher values indicate higher priority)
- **description**: A description of this specific implementation

### Step 3: Plugin Configuration

Plugins often need configuration that can be modified without changing code. The Firefly Plugin Manager provides a configuration system that allows plugins to access and update their configuration:

```java
// Access configuration in your plugin
@Override
public Mono<Void> initialize() {
    return Mono.fromRunnable(() -> {
        // Get configuration from the plugin manager
        Map<String, Object> config = pluginManager.getPlugin(getMetadata().id())
                .block()
                .configuration();

        // Parse configuration
        String apiKey = (String) config.getOrDefault("apiKey", "");
        String merchantId = (String) config.getOrDefault("merchantId", "");
        int timeoutSeconds = (int) config.getOrDefault("timeoutSeconds", 30);

        // Initialize with configuration
        gatewayClient.initialize(apiKey, merchantId, timeoutSeconds);

        // Subscribe to configuration changes
        eventBus.subscribe(PluginConfigurationEvent.class)
                .filter(event -> event.getPluginId().equals(getMetadata().id()))
                .subscribe(event -> {
                    Map<String, Object> newConfig = event.getNewConfiguration();
                    // Update client with new configuration
                    String newApiKey = (String) newConfig.getOrDefault("apiKey", apiKey);
                    String newMerchantId = (String) newConfig.getOrDefault("merchantId", merchantId);
                    int newTimeoutSeconds = (int) newConfig.getOrDefault("timeoutSeconds", timeoutSeconds);

                    gatewayClient.updateConfiguration(newApiKey, newMerchantId, newTimeoutSeconds);
                });
    });
}
```

#### Updating Plugin Configuration

The Plugin Manager provides APIs to update plugin configuration at runtime:

```java
// Get the current plugin configuration
PluginDescriptor descriptor = pluginManager.getPlugin("com.example.payment-plugin").block();
Map<String, Object> currentConfig = descriptor.configuration();

// Create a new configuration with updated values
Map<String, Object> newConfig = new HashMap<>(currentConfig);
newConfig.put("apiKey", "new-api-key-value");
newConfig.put("timeoutSeconds", 60);

// Update the plugin configuration
pluginManager.updatePluginConfiguration("com.example.payment-plugin", newConfig)
    .doOnSuccess(v -> log.info("Plugin configuration updated successfully"))
    .doOnError(e -> log.error("Failed to update plugin configuration", e))
    .subscribe();
```

#### Configuration Sources

Plugin configuration can come from multiple sources:

1. **Default Values**: Defined in the plugin code
2. **Application Properties**: Defined in `application.properties` or `application.yml`
3. **External Configuration**: Loaded from external sources (database, config server)
4. **Runtime Updates**: Applied through the Plugin Manager API

Example of configuration in application properties:

```properties
# Plugin-specific configuration
firefly.plugin-manager.plugins.com\.example\.payment-plugin.apiKey=production-key-12345
firefly.plugin-manager.plugins.com\.example\.payment-plugin.merchantId=MERCHANT_XYZ
firefly.plugin-manager.plugins.com\.example\.payment-plugin.timeoutSeconds=45
```

Configuration best practices:

- **Default Values**: Always provide sensible default values for configuration properties
- **Validation**: Validate configuration values to ensure they are within acceptable ranges
- **Dynamic Updates**: Design your plugin to handle configuration changes at runtime
- **Documentation**: Document all configuration properties, their purpose, and acceptable values
- **Scoping**: Use namespaced configuration keys to avoid conflicts with other plugins
- **Security**: Avoid storing sensitive information (like API keys) in plain text; use encryption or secure vaults
- **Versioning**: Consider versioning your configuration schema to handle upgrades gracefully

### Step 4: Event-Based Communication

Plugins should communicate with each other and the core system through events rather than direct method calls. This promotes loose coupling and allows for more flexible system architecture:

```java
// Define an event
public class PaymentProcessedEvent extends PluginEvent {
    private final BigDecimal amount;
    private final String currency;
    private final String reference;
    private final String transactionId;

    public PaymentProcessedEvent(String pluginId, String reference, String transactionId,
                                BigDecimal amount, String currency) {
        super(pluginId, "PAYMENT_PROCESSED");
        this.reference = reference;
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
    }

    // Getters
    public String getReference() { return reference; }
    public String getTransactionId() { return transactionId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
}

// Publish an event
eventBus.publish(new PaymentProcessedEvent(
        getMetadata().id(),
        reference,
        transactionId,
        amount,
        currency))
        .subscribe();

// Subscribe to events
eventBus.subscribe(PaymentProcessedEvent.class)
        .filter(event -> event.getCurrency().equals("USD"))
        .subscribe(event -> {
            logger.info("Payment processed: amount={}, currency={}, reference={}, transactionId={}",
                    event.getAmount(),
                    event.getCurrency(),
                    event.getReference(),
                    event.getTransactionId());
        });
```

Event-based communication best practices:

- **Event Design**: Design events to carry all necessary information but avoid excessive data
- **Event Types**: Create specific event types for different kinds of notifications
- **Filtering**: Use filtering to receive only relevant events
- **Error Handling**: Handle errors in event subscribers to prevent them from affecting publishers
- **Backpressure**: Be aware of backpressure in high-volume event scenarios
- **Subscription Management**: Dispose of subscriptions when they are no longer needed

### Step 5: Package the Plugin

To make your plugin available to the Firefly Plugin Manager, you need to package it as a JAR file with the appropriate metadata:

1. **Create a service provider configuration file**:

Create a file at `META-INF/services/com.catalis.core.plugin.api.Plugin` with the fully qualified name of your plugin class:

```
com.catalis.banking.CreditCardPaymentPlugin
```

This file enables the Java ServiceLoader mechanism to discover your plugin.

2. **Include dependencies**:

Include any dependencies that aren't provided by the core system. You can use Maven's shade plugin or similar tools to create a fat JAR with all dependencies.

3. **Build the JAR**:

Use Maven or Gradle to build your plugin JAR:

```xml
<!-- Maven pom.xml snippet -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <version>3.2.0</version>
            <configuration>
                <archive>
                    <manifestEntries>
                        <Plugin-Id>${plugin.id}</Plugin-Id>
                        <Plugin-Version>${project.version}</Plugin-Version>
                        <Plugin-Provider>${project.organization.name}</Plugin-Provider>
                    </manifestEntries>
                </archive>
            </configuration>
        </plugin>
    </plugins>
</build>
```

4. **Deploy the plugin**:

There are multiple ways to deploy and install plugins:

**a. JAR File (Traditional Method)**

Place the JAR file in the plugins directory of the Firefly platform, or use the Plugin Manager API to install it programmatically:

```java
PluginDescriptor descriptor = pluginManager.installPlugin(Path.of("plugins/credit-card-payment-1.0.0.jar")).block();
pluginManager.startPlugin(descriptor.metadata().id()).block();
```

**b. Git Repository**

Deploy your plugin to a Git repository and install it directly from there:

```java
URI repositoryUri = URI.create("https://github.com/firefly-oss/credit-card-payment-plugin.git");
PluginDescriptor descriptor = pluginManager.installPluginFromGit(repositoryUri).block();
pluginManager.startPlugin(descriptor.metadata().id()).block();
```

**c. Classpath Auto-detection**

Include your plugin in the application's classpath and let the plugin manager auto-detect it:

```java
// Auto-detect and install all plugins in the classpath
Flux<PluginDescriptor> descriptors = pluginManager.installPluginsFromClasspath().collectList().block();

// Start all discovered plugins
descriptors.forEach(descriptor -> {
    pluginManager.startPlugin(descriptor.metadata().id()).block();
});
```

Packaging best practices:

- **Minimize Size**: Include only necessary dependencies to keep the plugin size manageable
- **Versioning**: Use semantic versioning for your plugins
- **Documentation**: Include documentation in the JAR (e.g., README, Javadoc)
- **Manifest Attributes**: Add plugin metadata to the JAR manifest
- **Testing**: Test your plugin in isolation before deploying it
- **Signature**: Consider signing your plugin JAR for security
- **Git Repository Structure**: When using Git deployment, ensure your repository has a clear structure with a proper build file (pom.xml or build.gradle)
- **Annotation-based Configuration**: For classpath auto-detection, ensure your plugin class is properly annotated with @Plugin

## Configuration

The plugin manager can be configured using the following properties:

```properties
# Plugin Manager Configuration
firefly.plugin-manager.plugins-directory=plugins
firefly.plugin-manager.auto-start-plugins=true
firefly.plugin-manager.scan-on-startup=true

# Allowed packages for plugins
firefly.plugin-manager.allowed-packages=com.catalis.core.plugin.api,com.catalis.core.plugin.annotation

# Event Bus Configuration
# Use 'in-memory' or 'kafka'
firefly.plugin-manager.event-bus.type=in-memory
firefly.plugin-manager.event-bus.distributed-events=false

# Kafka Configuration (optional, only needed when using kafka event bus)
firefly.plugin-manager.event-bus.kafka.bootstrap-servers=localhost:9092
firefly.plugin-manager.event-bus.kafka.consumer-group-id=firefly-plugin-manager
firefly.plugin-manager.event-bus.kafka.default-topic=firefly-plugin-events
firefly.plugin-manager.event-bus.kafka.auto-create-topics=true
firefly.plugin-manager.event-bus.kafka.num-partitions=3
firefly.plugin-manager.event-bus.kafka.replication-factor=1
```

### Event Bus Configuration

The Event Bus can be configured to use either in-memory or Kafka transport. This section provides a comprehensive guide on configuring and using both event systems.

#### Choosing an Event Bus Implementation

The choice between in-memory and Kafka event bus depends on your deployment scenario:

- **In-Memory Event Bus**: Use for single-instance deployments, development environments, or when all plugins run within the same JVM.
- **Kafka Event Bus**: Use for distributed deployments, production environments with multiple instances, or when you need event persistence and replay capabilities.

#### In-Memory Event Bus (Default)

The in-memory event bus is suitable for standalone deployments and provides high-performance event processing within a single JVM instance.

**Configuration:**

```properties
# application.properties or application.yml
firefly.plugin-manager.event-bus.type=in-memory
```

**Usage Example:**

```java
@Autowired
private PluginEventBus eventBus;

// Publishing events
public Mono<Void> processPayment(PaymentRequest request) {
    return paymentService.process(request)
        .flatMap(result -> {
            // Create and publish an event
            PaymentProcessedEvent event = new PaymentProcessedEvent(
                "com.example.payment-plugin",
                request.getReference(),
                result.getTransactionId(),
                request.getAmount(),
                request.getCurrency()
            );
            return eventBus.publish(event);
        });
}

// Subscribing to events
@PostConstruct
public void initialize() {
    // Subscribe to specific event types
    Disposable subscription = eventBus.subscribe(PaymentProcessedEvent.class)
        .filter(event -> event.getAmount().compareTo(new BigDecimal("1000")) > 0)
        .subscribe(event -> {
            logger.info("Large payment processed: {}", event.getTransactionId());
            // Process the event
        });

    // Store the subscription for later disposal
    this.eventSubscription = subscription;
}

@PreDestroy
public void cleanup() {
    // Always dispose subscriptions when no longer needed
    if (eventSubscription != null && !eventSubscription.isDisposed()) {
        eventSubscription.dispose();
    }
}
```

#### Kafka Event Bus (Optional)

The Kafka event bus enables distributed event processing across multiple instances of the application. It provides reliable event delivery, persistence, and replay capabilities.

**Prerequisites:**

1. A running Kafka broker or cluster
2. Additional dependencies in your project

**Adding Kafka Dependencies:**

To use the Kafka event bus, add the following dependencies to your project:

```xml
<!-- Maven pom.xml -->
<dependency>
    <groupId>io.projectreactor.kafka</groupId>
    <artifactId>reactor-kafka</artifactId>
    <version>1.3.21</version>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>3.6.1</version>
</dependency>
```

Or if using Gradle:

```groovy
// Gradle build.gradle
implementation 'io.projectreactor.kafka:reactor-kafka:1.3.21'
implementation 'org.apache.kafka:kafka-clients:3.6.1'
```

These dependencies are marked as optional in the plugin manager, so they won't be transitively included in your project.

**Configuration:**

```properties
# Basic configuration
firefly.plugin-manager.event-bus.type=kafka
firefly.plugin-manager.event-bus.distributed-events=true
firefly.plugin-manager.event-bus.kafka.bootstrap-servers=kafka1:9092,kafka2:9092,kafka3:9092
firefly.plugin-manager.event-bus.kafka.consumer-group-id=firefly-plugin-manager-prod

# Advanced configuration
firefly.plugin-manager.event-bus.kafka.default-topic=firefly-plugin-events
firefly.plugin-manager.event-bus.kafka.auto-create-topics=true
firefly.plugin-manager.event-bus.kafka.num-partitions=3
firefly.plugin-manager.event-bus.kafka.replication-factor=1
```

In a clustered environment, each instance should use the same consumer group ID to ensure proper event distribution.

**Usage Example:**

The usage pattern is identical to the in-memory event bus, as the implementation details are abstracted away:

```java
@Autowired
private PluginEventBus eventBus;

// Publishing events
public Mono<Void> notifyFraudDetection(Transaction transaction) {
    TransactionEvent event = new TransactionEvent(
        "com.example.transaction-plugin",
        transaction.getId(),
        transaction.getAmount(),
        transaction.getAccountId()
    );
    return eventBus.publish(event);
}

// Subscribing to events
@PostConstruct
public void initialize() {
    // The same code works with both in-memory and Kafka event bus
    this.eventSubscription = eventBus.subscribe(TransactionEvent.class)
        .filter(event -> event.getAmount().compareTo(new BigDecimal("10000")) > 0)
        .subscribe(event -> {
            logger.info("High-value transaction detected: {}", event.getTransactionId());
            fraudDetectionService.analyze(event);
        });
}
```

#### Event Serialization for Kafka

When using the Kafka event bus, events need to be serializable. The Firefly Plugin Manager handles serialization automatically, but you should follow these guidelines for your event classes:

```java
// Serialization-friendly event class
public class PaymentProcessedEvent extends PluginEvent implements Serializable {
    // Use primitive types or standard Java serializable classes
    private final BigDecimal amount;
    private final String currency;
    private final String reference;
    private final String transactionId;
    private final LocalDateTime timestamp;

    // Include a no-args constructor for deserialization
    protected PaymentProcessedEvent() {
        super("", "");
        this.amount = null;
        this.currency = null;
        this.reference = null;
        this.transactionId = null;
        this.timestamp = null;
    }

    public PaymentProcessedEvent(String pluginId, String reference, String transactionId,
                              BigDecimal amount, String currency) {
        super(pluginId, "PAYMENT_PROCESSED");
        this.reference = reference;
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getReference() { return reference; }
    public String getTransactionId() { return transactionId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
```

#### Custom Serialization

For complex events or better performance, you can provide custom serializers and deserializers:

```java
// application.properties
firefly.plugin-manager.event-bus.kafka.serializer=com.example.CustomEventSerializer
firefly.plugin-manager.event-bus.kafka.deserializer=com.example.CustomEventDeserializer

// Custom serializer implementation
public class CustomEventSerializer implements Serializer<PluginEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @Override
    public byte[] serialize(String topic, PluginEvent event) {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (Exception e) {
            throw new SerializationException("Error serializing event", e);
        }
    }
}
```

#### Event Bus Architecture

The Event Bus follows a publish-subscribe pattern that enables loose coupling between components. The following diagram illustrates the relationship between publishers, subscribers, and the event bus:

```mermaid
graph TB
%% ───────────────  PUBLISHERS  ───────────────
    subgraph plugin1["Plugin 1"]
        P1[Publisher]:::publisher
    end

    subgraph plugin2["Plugin 2"]
        P2[Publisher]:::publisher
    end

%% ────────────────  EVENT BUS  ────────────────
    subgraph eventBus["Event Bus"]
        EB1["In-Memory Bus"]:::eventbus
        EB2["Kafka Bus"]:::eventbus
        EB1 -.Alternative.-> EB2
    end

%% ───────────────  SUBSCRIBERS  ───────────────
    subgraph plugin3["Plugin 3"]
        S1[Subscriber]:::subscriber
        S2[Subscriber]:::subscriber
    end

    subgraph coreSvc["Core Microservice"]
        S3[Subscriber]:::subscriber
    end

%% ────────────────  EDGES  ────────────────
    P1 -->|"PaymentEvent"| EB1
    P2 -->|"TransactionEvent"| EB1

    EB1 -->|"PaymentEvent"| S1
    EB1 -->|"PaymentEvent"| S3
    EB1 -->|"TransactionEvent"| S2

%% ───────────────  CLASSES  ───────────────
    classDef publisher  fill:#f9f,stroke:#333,stroke-width:1px
    classDef subscriber fill:#bbf,stroke:#333,stroke-width:1px
    classDef eventbus   fill:#bfb,stroke:#333,stroke-width:2px

%% ────────────────  STYLES  ────────────────
    style plugin1  fill:#f9f9f9,stroke:#333,stroke-width:1px
    style plugin2  fill:#f9f9f9,stroke:#333,stroke-width:1px
    style plugin3  fill:#f9f9f9,stroke:#333,stroke-width:1px
    style eventBus fill:#e6ffe6,stroke:#333,stroke-width:1px
    style coreSvc  fill:#e6e6ff,stroke:#333,stroke-width:1px
```

Key aspects of the event bus architecture:

1. **Publishers**: Components that generate events (plugins, core services)
2. **Event Bus**: Central message broker that routes events (in-memory or Kafka)
3. **Subscribers**: Components that consume events (plugins, core services)
4. **Events**: Typed messages that carry information between components
5. **Filtering**: Subscribers can filter events by type or content

#### Event Bus Best Practices

1. **Event Design:**
   - Create specific event classes for different types of notifications
   - Include all necessary information in the event, but avoid excessive data
   - Make events immutable to prevent modification during propagation
   - Use clear naming conventions for event classes (e.g., `PaymentProcessedEvent`, `UserRegisteredEvent`)

2. **Publishing Events:**
   - Always subscribe to the result of `eventBus.publish()` or use `.subscribe()` to ensure the event is actually sent
   - Include error handling for publish operations
   - Consider using `doOnError` and `doOnSuccess` operators for logging

3. **Subscribing to Events:**
   - Use type-based filtering with `eventBus.subscribe(EventType.class)`
   - Apply additional filtering with the `.filter()` operator
   - Store subscription references and dispose them when no longer needed
   - Handle errors within subscribers to prevent them from affecting other subscribers

4. **Performance Considerations:**
   - With Kafka, be mindful of serialization/deserialization overhead
   - For high-volume events, consider using backpressure strategies
   - Monitor Kafka consumer lag in production environments

5. **Testing:**
   - Use the in-memory event bus for unit and integration tests
   - Create test utilities to verify that events were published correctly

#### Switching Between Event Bus Implementations

The Firefly Plugin Manager allows you to switch between event bus implementations without code changes. Simply update the configuration properties and restart the application. This makes it easy to use the in-memory implementation during development and testing, and switch to Kafka for production deployments.

#### Monitoring and Troubleshooting

**For In-Memory Event Bus:**
- Use logging to track event publishing and subscription
- Monitor memory usage, as events are stored in memory

**For Kafka Event Bus:**
- Use Kafka monitoring tools to track topic metrics
- Monitor consumer lag to ensure events are being processed in a timely manner
- Check Kafka logs for connection issues or configuration problems
- Verify that the Kafka cluster is healthy and accessible

## Security Considerations

Plugin systems introduce potential security risks that must be carefully managed. The Firefly Plugin Manager implements several security mechanisms to protect the core system and ensure safe plugin operation.

### Class Loading Isolation

Each plugin runs in its own isolated class loader, which provides several security benefits:

- **Namespace Isolation**: Prevents plugins from accessing or modifying classes from other plugins
- **Dependency Isolation**: Allows plugins to use different versions of the same library without conflicts
- **Controlled Access**: Restricts which core system classes plugins can access
- **Unloading**: Enables complete unloading of plugin classes when they are no longer needed

The `PluginClassLoader` implements this isolation:

```java
public class PluginClassLoader extends URLClassLoader {
    private final Set<String> allowedPackages;

    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.allowedPackages = new HashSet<>();

        // Allow core plugin API packages
        allowedPackages.add("com.catalis.core.plugin.api");
        // ...
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // Check if the class has already been loaded
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        // Check if the class is from an allowed package
        boolean isAllowed = allowedPackages.stream()
                .anyMatch(name::startsWith);

        if (isAllowed) {
            // Delegate to parent class loader for allowed packages
            try {
                return super.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                // Fall through to try loading from plugin
            }
        }

        // Try to load the class from the plugin
        try {
            Class<?> pluginClass = findClass(name);
            if (resolve) {
                resolveClass(pluginClass);
            }
            return pluginClass;
        } catch (ClassNotFoundException e) {
            // If not found in plugin, delegate to parent
            return super.loadClass(name, resolve);
        }
    }
}
```

### Permission System

The plugin manager implements a fine-grained permission system that controls what operations plugins can perform:

- **File System Access**: Controls which directories plugins can read from or write to
- **Network Access**: Restricts which hosts and ports plugins can connect to
- **System Properties**: Controls which system properties plugins can read or modify
- **Reflection**: Limits reflective access to system classes
- **Thread Creation**: Controls whether and how plugins can create threads
- **Native Code**: Prevents plugins from loading native libraries unless explicitly allowed

Permissions can be configured at the system level and overridden for specific plugins:

```properties
# Global permission settings
firefly.plugin-manager.security.allow-file-access=false
firefly.plugin-manager.security.allow-network-access=true
firefly.plugin-manager.security.allowed-hosts=api.payment-gateway.com,auth.payment-gateway.com

# Plugin-specific permissions
firefly.plugin-manager.security.plugins.com\.catalis\.banking\.credit-card-payment.allow-file-access=true
firefly.plugin-manager.security.plugins.com\.catalis\.banking\.credit-card-payment.allowed-directories=/tmp/payments
```

### Signature Verification

Plugins can be signed with digital certificates to verify their authenticity and integrity:

- **JAR Signing**: Plugins are distributed as signed JAR files
- **Certificate Validation**: The plugin manager verifies the signature against trusted certificates
- **Integrity Checking**: Ensures the plugin code hasn't been tampered with
- **Trust Levels**: Different certificates can confer different levels of trust and permissions

Signature verification is performed during plugin loading:

```java
private void verifyPluginSignature(Path pluginPath) throws SecurityException {
    try {
        JarFile jarFile = new JarFile(pluginPath.toFile(), true);
        Enumeration<JarEntry> entries = jarFile.entries();

        // Reading the entry forces signature verification
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                try (InputStream is = jarFile.getInputStream(entry)) {
                    // Read the entry to verify its signature
                    byte[] buffer = new byte[8192];
                    while (is.read(buffer) != -1) {
                        // Just read, don't need to do anything with the data
                    }
                }
            }
        }

        // Check certificate chain
        Certificate[] certificates = jarFile.getManifest().getMainAttributes().getSigningCertificates();
        if (certificates == null || certificates.length == 0) {
            throw new SecurityException("Plugin is not signed");
        }

        // Verify certificate against trusted certificates
        X509Certificate pluginCert = (X509Certificate) certificates[0];
        verifyCertificate(pluginCert);

    } catch (Exception e) {
        throw new SecurityException("Plugin signature verification failed: " + e.getMessage(), e);
    }
}
```

### Resource Limiting

To prevent plugins from consuming excessive system resources, the plugin manager implements resource limiting:

- **Memory Usage**: Limits the amount of memory a plugin can allocate
- **CPU Usage**: Restricts the CPU time a plugin can consume
- **Thread Count**: Limits the number of threads a plugin can create
- **File Handles**: Restricts the number of open file handles
- **Network Connections**: Limits the number of concurrent network connections

Resource limits can be configured globally or per plugin:

```properties
# Global resource limits
firefly.plugin-manager.resources.max-memory-mb=256
firefly.plugin-manager.resources.max-cpu-percentage=25
firefly.plugin-manager.resources.max-threads=10

# Plugin-specific resource limits
firefly.plugin-manager.resources.plugins.com\.catalis\.banking\.reporting.max-memory-mb=512
```

### Sandboxing

For maximum security, plugins can be run in a sandboxed environment:

- **Process Isolation**: Runs plugins in separate JVM processes
- **Container Isolation**: Uses containerization (e.g., Docker) for stronger isolation
- **Security Manager**: Applies a custom SecurityManager to enforce permissions
- **API Gateway**: Mediates all communication between plugins and the core system

Sandboxing configuration example:

```properties
firefly.plugin-manager.sandbox.enabled=true
firefly.plugin-manager.sandbox.mode=PROCESS  # Options: CLASSLOADER, PROCESS, CONTAINER
firefly.plugin-manager.sandbox.timeout-seconds=30
```

### Security Best Practices

When developing and deploying plugins, follow these security best practices:

1. **Principle of Least Privilege**: Grant plugins only the permissions they absolutely need
2. **Input Validation**: Validate all inputs from plugins before processing them
3. **Output Sanitization**: Sanitize all outputs from plugins before displaying or using them
4. **Secure Communication**: Use secure channels for communication between plugins and the core system
5. **Regular Updates**: Keep the plugin manager and all plugins updated with security patches
6. **Code Review**: Perform security code reviews of plugins before deployment
7. **Vulnerability Scanning**: Regularly scan plugins for known vulnerabilities
8. **Monitoring**: Monitor plugin behavior for anomalies that might indicate security issues
9. **Audit Logging**: Maintain detailed logs of plugin activities for security auditing
10. **Incident Response**: Have a plan for responding to security incidents involving plugins

## Advanced Topics

### Plugin Dependency Management

The Firefly Plugin Manager includes a sophisticated dependency resolution system that ensures plugins are loaded and started in the correct order:

- **Dependency Declaration**: Plugins declare their dependencies using the `dependencies` attribute in the `@Plugin` annotation
- **Dependency Resolution**: The plugin manager builds a dependency graph and topologically sorts it
- **Circular Dependency Detection**: The system detects and reports circular dependencies
- **Optional Dependencies**: Plugins can specify optional dependencies that enhance functionality but aren't required
- **Version Constraints**: Dependencies can include version constraints to ensure compatibility

Example of dependency declaration:

```java
@Plugin(
        id = "com.catalis.banking.fraud-detection",
        // ...
        dependencies = {
            "com.catalis.banking.transaction-history>=2.0.0",
            "com.catalis.banking.customer-profile",
            "?com.catalis.banking.machine-learning"  // Optional dependency
        }
)
```

The dependency resolver ensures that all required plugins are available and compatible:

```java
private void resolveDependencies(String pluginId, Set<String> visited, Set<String> inProgress) {
    if (inProgress.contains(pluginId)) {
        throw new CircularDependencyException("Circular dependency detected: " + inProgress);
    }

    if (visited.contains(pluginId)) {
        return; // Already resolved
    }

    PluginDescriptor descriptor = descriptors.get(pluginId);
    if (descriptor == null) {
        throw new PluginNotFoundException("Plugin not found: " + pluginId);
    }

    inProgress.add(pluginId);

    // Resolve each dependency
    for (String dependencySpec : descriptor.metadata().dependencies()) {
        boolean optional = dependencySpec.startsWith("?");
        String dependency = optional ? dependencySpec.substring(1) : dependencySpec;

        // Parse version constraints
        String dependencyId = dependency;
        String versionConstraint = "";
        if (dependency.contains(">") || dependency.contains("=") || dependency.contains("<")) {
            int constraintIndex = findConstraintIndex(dependency);
            dependencyId = dependency.substring(0, constraintIndex);
            versionConstraint = dependency.substring(constraintIndex);
        }

        PluginDescriptor dependencyDescriptor = descriptors.get(dependencyId);
        if (dependencyDescriptor == null) {
            if (optional) {
                continue; // Skip optional dependency
            } else {
                throw new DependencyNotFoundException("Dependency not found: " + dependencyId);
            }
        }

        // Check version constraint
        if (!versionConstraint.isEmpty()) {
            String dependencyVersion = dependencyDescriptor.metadata().version();
            if (!satisfiesVersionConstraint(dependencyVersion, versionConstraint)) {
                throw new IncompatibleDependencyException(
                        "Dependency version constraint not satisfied: " +
                        dependencyId + " " + versionConstraint +
                        " (found: " + dependencyVersion + ")");
            }
        }

        // Recursively resolve dependencies
        resolveDependencies(dependencyId, visited, inProgress);
    }

    inProgress.remove(pluginId);
    visited.add(pluginId);
}
```

### Hot Deployment

The Firefly Plugin Manager supports hot deployment, allowing plugins to be installed, updated, or uninstalled without restarting the application:

- **Directory Watching**: The system monitors the plugins directory for changes
- **Automatic Loading**: New plugins are automatically detected and loaded
- **Graceful Unloading**: Plugins being removed are gracefully shut down
- **Version Updates**: The system handles upgrading plugins to newer versions
- **State Preservation**: Plugin state can be preserved across updates when appropriate

Example configuration for hot deployment:

```properties
firefly.plugin-manager.hot-deployment.enabled=true
firefly.plugin-manager.hot-deployment.scan-interval-seconds=10
firefly.plugin-manager.hot-deployment.plugins-directory=plugins
firefly.plugin-manager.hot-deployment.backup-directory=plugins/backup
```

Implementation of the directory watcher:

```java
private void startDirectoryWatcher() {
    if (!hotDeploymentEnabled) {
        return;
    }

    try {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path pluginsDir = pluginsDirectory.toAbsolutePath();

        pluginsDir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        Thread watcherThread = new Thread(() -> {
            try {
                while (true) {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path fileName = (Path) event.context();
                        Path fullPath = pluginsDir.resolve(fileName);

                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            if (isPluginFile(fullPath)) {
                                handleNewPlugin(fullPath);
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            handleRemovedPlugin(fileName.toString());
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            if (isPluginFile(fullPath)) {
                                handleModifiedPlugin(fullPath);
                            }
                        }
                    }

                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Error in plugin directory watcher", e);
            }
        });

        watcherThread.setDaemon(true);
        watcherThread.setName("plugin-directory-watcher");
        watcherThread.start();

    } catch (IOException e) {
        logger.error("Failed to start plugin directory watcher", e);
    }
}
```

### Plugin Versioning and Compatibility

The Firefly Plugin Manager includes a sophisticated versioning system to ensure compatibility between plugins and the platform:

- **Semantic Versioning**: Plugins use semantic versioning (MAJOR.MINOR.PATCH)
- **Platform Version Constraints**: Plugins specify minimum and maximum platform versions
- **API Compatibility Checking**: The system checks API compatibility between plugins
- **Deprecation Handling**: Support for gracefully handling deprecated APIs
- **Migration Support**: Tools for migrating plugin data between versions

Version compatibility checking:

```java
private boolean isCompatibleWithPlatform(PluginMetadata metadata) {
    String platformVersion = getPlatformVersion();
    String minPlatformVersion = metadata.minPlatformVersion();
    String maxPlatformVersion = metadata.maxPlatformVersion();

    // Check minimum platform version
    if (!minPlatformVersion.isEmpty() &&
            compareVersions(platformVersion, minPlatformVersion) < 0) {
        logger.error("Plugin {} requires minimum platform version {}, but current version is {}",
                metadata.id(), minPlatformVersion, platformVersion);
        return false;
    }

    // Check maximum platform version
    if (!maxPlatformVersion.isEmpty() &&
            compareVersions(platformVersion, maxPlatformVersion) > 0) {
        logger.error("Plugin {} requires maximum platform version {}, but current version is {}",
                metadata.id(), maxPlatformVersion, platformVersion);
        return false;
    }

    return true;
}

private int compareVersions(String version1, String version2) {
    String[] parts1 = version1.split("\\.");
    String[] parts2 = version2.split("\\.");

    int length = Math.max(parts1.length, parts2.length);
    for (int i = 0; i < length; i++) {
        int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
        int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

        if (v1 < v2) {
            return -1;
        } else if (v1 > v2) {
            return 1;
        }
    }

    return 0;
}
```

### Plugin Health Monitoring

The Firefly Plugin Manager includes a comprehensive health monitoring system:

- **Health Checks**: Plugins can provide health check implementations
- **Metrics Collection**: The system collects performance metrics from plugins
- **Resource Monitoring**: Monitors resource usage (CPU, memory, etc.)
- **Deadlock Detection**: Detects thread deadlocks within plugins
- **Automatic Recovery**: Can automatically restart failed plugins

Example health check implementation:

```java
public class PluginHealthIndicator implements ReactiveHealthIndicator {

    private final PluginManager pluginManager;

    @Autowired
    public PluginHealthIndicator(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Override
    public Mono<Health> health() {
        return pluginManager.getAllPlugins()
                .collectList()
                .map(plugins -> {
                    Health.Builder builder = Health.up();

                    // Count plugins by state
                    Map<PluginState, Long> pluginsByState = plugins.stream()
                            .collect(Collectors.groupingBy(
                                    p -> p.state(),
                                    Collectors.counting()));

                    builder.withDetail("plugins", pluginsByState);

                    // Check for failed plugins
                    long failedCount = pluginsByState.getOrDefault(PluginState.FAILED, 0L);
                    if (failedCount > 0) {
                        builder = Health.down()
                                .withDetail("failedCount", failedCount);

                        // Include details about failed plugins
                        List<Map<String, Object>> failedPlugins = plugins.stream()
                                .filter(p -> p.state() == PluginState.FAILED)
                                .map(p -> Map.of(
                                        "id", p.metadata().id(),
                                        "version", p.metadata().version(),
                                        "error", p.getError().orElse("Unknown error")))
                                .collect(Collectors.toList());

                        builder.withDetail("failedPlugins", failedPlugins);
                    }

                    return builder.build();
                });
    }
}
```

### Plugin Testing Framework

The Firefly Plugin Manager includes a comprehensive testing framework for plugin developers:

- **Mock Plugin Manager**: A mock implementation for unit testing
- **Test Harness**: A harness for integration testing plugins
- **Simulation Environment**: Simulates the runtime environment
- **Test Extensions**: Special extensions for testing purposes
- **Verification Tools**: Tools for verifying plugin behavior

Example of a plugin unit test:

```java
@ExtendWith(MockitoExtension.class)
public class CreditCardPaymentPluginTest {

    @Mock
    private PluginEventBus eventBus;

    @Mock
    private PaymentGatewayClient gatewayClient;

    private CreditCardPaymentPlugin plugin;

    @BeforeEach
    public void setup() {
        plugin = new CreditCardPaymentPlugin(eventBus);
        ReflectionTestUtils.setField(plugin, "gatewayClient", gatewayClient);

        when(gatewayClient.initialize()).thenReturn(Mono.empty());
        when(gatewayClient.connect()).thenReturn(Mono.empty());
        when(gatewayClient.disconnect()).thenReturn(Mono.empty());
    }

    @Test
    public void testProcessPayment_Success() {
        // Arrange
        String reference = "REF123";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "USD";
        String transactionId = "TXN456";

        when(gatewayClient.processPayment(amount, currency, reference))
                .thenReturn(Mono.just(transactionId));

        // Act
        PaymentProcessor processor = plugin.getPaymentProcessor();
        String result = processor.processPayment(amount, currency, reference).block();

        // Assert
        assertEquals(transactionId, result);
        verify(gatewayClient).processPayment(amount, currency, reference);
    }

    @Test
    public void testProcessPayment_UnsupportedCurrency() {
        // Arrange
        String reference = "REF123";
        BigDecimal amount = new BigDecimal("100.00");
        String currency = "XYZ"; // Unsupported currency

        // Act & Assert
        PaymentProcessor processor = plugin.getPaymentProcessor();
        assertThrows(UnsupportedOperationException.class, () -> {
            processor.processPayment(amount, currency, reference).block();
        });

        verify(gatewayClient, never()).processPayment(any(), any(), any());
    }

    @Test
    public void testLifecycle() {
        // Test initialize
        plugin.initialize().block();
        verify(gatewayClient).initialize();

        // Test start
        when(eventBus.subscribe(any())).thenReturn(Flux.empty());
        plugin.start().block();
        verify(gatewayClient).connect();
        verify(eventBus).subscribe(PaymentRequestEvent.class);

        // Test stop
        plugin.stop().block();
        verify(gatewayClient).disconnect();
    }
}
```

Example of an integration test using the test harness:

```java
@SpringBootTest
public class CreditCardPaymentPluginIntegrationTest {

    @Autowired
    private PluginTestHarness testHarness;

    @Test
    public void testPluginLifecycle() throws Exception {
        // Load the plugin
        PluginDescriptor descriptor = testHarness.loadPlugin(Path.of("target/credit-card-payment-1.0.0.jar"));
        assertEquals("com.catalis.banking.credit-card-payment", descriptor.metadata().id());
        assertEquals(PluginState.INSTALLED, descriptor.state());

        // Initialize and start the plugin
        testHarness.initializePlugin(descriptor.metadata().id());
        assertEquals(PluginState.INITIALIZED, testHarness.getPluginState(descriptor.metadata().id()));

        testHarness.startPlugin(descriptor.metadata().id());
        assertEquals(PluginState.STARTED, testHarness.getPluginState(descriptor.metadata().id()));

        // Get the extension
        PaymentProcessor paymentProcessor = testHarness.getExtension(
                "com.catalis.banking.payment-processor",
                PaymentProcessor.class);

        assertNotNull(paymentProcessor);
        assertEquals("Credit Card", paymentProcessor.getPaymentMethodName());

        // Test the extension
        String transactionId = paymentProcessor.processPayment(
                new BigDecimal("100.00"), "USD", "TEST-REF-123").block();

        assertNotNull(transactionId);

        // Stop and uninstall the plugin
        testHarness.stopPlugin(descriptor.metadata().id());
        assertEquals(PluginState.STOPPED, testHarness.getPluginState(descriptor.metadata().id()));

        testHarness.uninstallPlugin(descriptor.metadata().id());
        assertEquals(PluginState.UNINSTALLED, testHarness.getPluginState(descriptor.metadata().id()));
    }
}
```

## Complete Example: Microservice Extension Point and Plugin Implementation

This section provides a comprehensive example showing the complete flow from defining an extension point in a core microservice to implementing it in a plugin and using it at runtime.

### 1. Defining an Extension Point in a Core Microservice

First, let's look at how a core microservice (core-banking-accounts) would define an extension point for account enrichment:

```java
// In the core-banking-accounts microservice
package com.catalis.banking.accounts.api.extension;

import com.catalis.core.plugin.annotation.ExtensionPoint;
import com.catalis.banking.accounts.model.Account;
import reactor.core.publisher.Mono;

/**
 * Extension point for enriching account information with additional data.
 * This allows plugins to add supplementary information to accounts without
 * modifying the core account structure.
 */
@ExtensionPoint(
        id = "com.catalis.banking.accounts.account-enricher",
        description = "Extension point for enriching account information with additional data",
        allowMultiple = true
)
public interface AccountEnricher {
    /**
     * Enriches the given account with additional information.
     *
     * @param account the account to enrich
     * @return a Mono that emits the enriched account
     */
    Mono<Account> enrichAccount(Account account);

    /**
     * Gets the name of this enricher.
     *
     * @return the enricher name
     */
    String getEnricherName();

    /**
     * Gets the priority of this enricher.
     * Higher values indicate higher priority.
     *
     * @return the priority value
     */
    int getPriority();

    /**
     * Checks if this enricher supports the given account type.
     *
     * @param accountType the account type to check
     * @return true if supported, false otherwise
     */
    boolean supportsAccountType(String accountType);
}
```

### 2. Using the Extension Point in the Core Microservice

Next, let's see how the core microservice would use the Plugin Manager to find and use implementations of this extension point:

```java
// In the core-banking-accounts microservice
package com.catalis.banking.accounts.service;

import com.catalis.banking.accounts.api.extension.AccountEnricher;
import com.catalis.banking.accounts.model.Account;
import com.catalis.core.plugin.api.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AccountEnrichmentService {

    private final PluginManager pluginManager;

    @Autowired
    public AccountEnrichmentService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * Enriches an account by applying all available account enrichers that
     * support the account's type, in order of priority.
     *
     * @param account the account to enrich
     * @return a Mono that emits the enriched account
     */
    public Mono<Account> enrichAccount(Account account) {
        // Get all extensions for the AccountEnricher extension point
        return pluginManager.getExtensionRegistry()
                .getExtensions("com.catalis.banking.accounts.account-enricher")
                // Cast to the specific extension point interface
                .cast(AccountEnricher.class)
                // Filter to only include enrichers that support this account type
                .filter(enricher -> enricher.supportsAccountType(account.getType()))
                // Sort by priority (highest first)
                .sort((e1, e2) -> Integer.compare(e2.getPriority(), e1.getPriority()))
                // Apply each enricher in sequence
                .reduce(Mono.just(account), (accountMono, enricher) ->
                        accountMono.flatMap(enricher::enrichAccount))
                // Get the final result
                .flatMap(mono -> mono);
    }

    /**
     * Gets information about all available account enrichers.
     *
     * @return a Flux of enricher information
     */
    public Flux<EnricherInfo> getAvailableEnrichers() {
        return pluginManager.getExtensionRegistry()
                .getExtensions("com.catalis.banking.accounts.account-enricher")
                .cast(AccountEnricher.class)
                .map(enricher -> new EnricherInfo(
                        enricher.getEnricherName(),
                        enricher.getPriority(),
                        pluginManager.getPluginForExtension(enricher)
                                .map(plugin -> plugin.getMetadata().id())
                                .orElse("unknown")));
    }

    /**
     * Simple DTO for enricher information.
     */
    public record EnricherInfo(String name, int priority, String pluginId) {}
}
```

### 3. Implementing the Extension Point in a Plugin

Now, let's see how a plugin would implement this extension point:

```java
// In a separate plugin project
package com.catalis.banking.plugins.companyinfo;

import com.catalis.banking.accounts.api.extension.AccountEnricher;
import com.catalis.banking.accounts.model.Account;
import com.catalis.core.plugin.annotation.Extension;
import com.catalis.core.plugin.annotation.Plugin;
import com.catalis.core.plugin.api.AbstractPlugin;
import com.catalis.core.plugin.api.PluginEventBus;
import com.catalis.core.plugin.api.PluginMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Set;

@Plugin(
        id = "com.catalis.banking.company-information",
        name = "Company Information Enricher",
        version = "1.0.0",
        description = "Enriches business accounts with company information from external sources",
        author = "Catalis Financial Services",
        minPlatformVersion = "1.0.0"
)
public class CompanyInformationPlugin extends AbstractPlugin {

    private static final Logger logger = LoggerFactory.getLogger(CompanyInformationPlugin.class);

    private final CompanyInformationEnricher enricher;
    private final CompanyInfoClient companyInfoClient;
    private final PluginEventBus eventBus;

    public CompanyInformationPlugin(PluginEventBus eventBus) {
        super(PluginMetadata.builder()
                .id("com.catalis.banking.company-information")
                .name("Company Information Enricher")
                .version("1.0.0")
                .description("Enriches business accounts with company information from external sources")
                .author("Catalis Financial Services")
                .minPlatformVersion("1.0.0")
                .build());

        this.eventBus = eventBus;
        this.companyInfoClient = new CompanyInfoClient();
        this.enricher = new CompanyInformationEnricher();
    }

    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing Company Information Plugin");

        // Initialize the company information client
        return companyInfoClient.initialize()
                .doOnSuccess(v -> logger.info("Company information client initialized successfully"))
                .doOnError(e -> logger.error("Failed to initialize company information client", e));
    }

    @Override
    public Mono<Void> start() {
        logger.info("Starting Company Information Plugin");

        // Connect to the company information service
        return companyInfoClient.connect()
                .doOnSuccess(v -> logger.info("Connected to company information service"))
                .doOnError(e -> logger.error("Failed to connect to company information service", e));
    }

    @Override
    public Mono<Void> stop() {
        logger.info("Stopping Company Information Plugin");

        // Disconnect from the company information service
        return companyInfoClient.disconnect()
                .doOnSuccess(v -> logger.info("Disconnected from company information service"))
                .doOnError(e -> logger.error("Error disconnecting from company information service", e));
    }

    /**
     * Implementation of the AccountEnricher extension point.
     */
    @Extension(
            extensionPointId = "com.catalis.banking.accounts.account-enricher",
            priority = 100,
            description = "Enriches business accounts with company information from external sources"
    )
    public class CompanyInformationEnricher implements AccountEnricher {

        @Override
        public Mono<Account> enrichAccount(Account account) {
            // Only enrich business accounts
            if (!supportsAccountType(account.getType())) {
                return Mono.just(account);
            }

            logger.info("Enriching business account {} with company information", account.getId());

            // Get the company ID from the account
            String companyId = account.getMetadata().get("companyId");
            if (companyId == null || companyId.isEmpty()) {
                logger.warn("No company ID found for account {}", account.getId());
                return Mono.just(account);
            }

            // Fetch company information from the external service
            return companyInfoClient.getCompanyInfo(companyId)
                    .map(companyInfo -> {
                        // Create a new account with enriched metadata
                        Account enrichedAccount = new Account(account);
                        enrichedAccount.getMetadata().put("companyName", companyInfo.name());
                        enrichedAccount.getMetadata().put("companySize", companyInfo.size());
                        enrichedAccount.getMetadata().put("industrySector", companyInfo.sector());
                        enrichedAccount.getMetadata().put("yearFounded", String.valueOf(companyInfo.yearFounded()));
                        enrichedAccount.getMetadata().put("creditRating", companyInfo.creditRating());

                        logger.info("Successfully enriched account {} with company information", account.getId());
                        return enrichedAccount;
                    })
                    .onErrorResume(error -> {
                        logger.error("Error enriching account {} with company information: {}",
                                account.getId(), error.getMessage());
                        return Mono.just(account);
                    });
        }

        @Override
        public String getEnricherName() {
            return "Company Information Enricher";
        }

        @Override
        public int getPriority() {
            return 100; // High priority
        }

        @Override
        public boolean supportsAccountType(String accountType) {
            return Set.of("BUSINESS", "CORPORATE", "ENTERPRISE").contains(accountType);
        }
    }

    /**
     * Client for the external company information service.
     */
    private static class CompanyInfoClient {

        Mono<Void> initialize() {
            // Initialize the client
            return Mono.empty();
        }

        Mono<Void> connect() {
            // Connect to the service
            return Mono.empty();
        }

        Mono<Void> disconnect() {
            // Disconnect from the service
            return Mono.empty();
        }

        Mono<CompanyInfo> getCompanyInfo(String companyId) {
            // In a real implementation, this would call an external API
            return Mono.just(new CompanyInfo(
                    "Acme Corporation",
                    "LARGE",
                    "Manufacturing",
                    1985,
                    "A+"
            ));
        }
    }

    /**
     * Record representing company information.
     */
    private record CompanyInfo(String name, String size, String sector, int yearFounded, String creditRating) {}
}
```

### 4. Complete Flow of Execution

Finally, let's trace the complete flow of execution when the system processes a request:

1. **User Request**: A user or system requests account details through an API endpoint in the core-banking-accounts microservice.

2. **Controller Layer**: The AccountController receives the request and delegates to the AccountService.

```java
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}")
    public Mono<AccountResponse> getAccount(@PathVariable String accountId) {
        return accountService.getAccountWithEnrichment(accountId)
                .map(this::mapToResponse);
    }

    private AccountResponse mapToResponse(Account account) {
        // Map account to response DTO
        return new AccountResponse(/* ... */);
    }
}
```

3. **Service Layer**: The AccountService retrieves the account and calls the AccountEnrichmentService to enrich it.

```java
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountEnrichmentService enrichmentService;

    @Autowired
    public AccountService(AccountRepository accountRepository,
                          AccountEnrichmentService enrichmentService) {
        this.accountRepository = accountRepository;
        this.enrichmentService = enrichmentService;
    }

    public Mono<Account> getAccountWithEnrichment(String accountId) {
        return accountRepository.findById(accountId)
                .flatMap(enrichmentService::enrichAccount);
    }
}
```

4. **Extension Point Usage**: The AccountEnrichmentService uses the Plugin Manager to find and apply all AccountEnricher implementations.

5. **Plugin Discovery**: The Plugin Manager queries the Extension Registry for all extensions implementing the AccountEnricher extension point.

6. **Extension Filtering and Sorting**: The extensions are filtered to include only those that support the account type, and sorted by priority.

7. **Extension Execution**: Each extension is applied to the account in sequence, starting with the highest priority.

8. **Plugin Implementation**: The CompanyInformationEnricher in the CompanyInformationPlugin processes the account, calling an external service to fetch company information.

9. **Result Enrichment**: The enricher adds the company information to the account metadata.

10. **Response**: The enriched account is returned to the controller, mapped to a response DTO, and sent back to the client.

This flow demonstrates how the plugin system enables the core microservice to be extended with additional functionality (company information enrichment) without modifying its core code. The extension point provides a clear contract, and the Plugin Manager handles the discovery and execution of extensions, creating a flexible, modular system.
