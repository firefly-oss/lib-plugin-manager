# Creating Extension Points in Microservices

This guide explains how to design and implement effective extension points in your microservices, allowing them to be extended through plugins.

## Table of Contents

1. [Understanding Extension Points](#understanding-extension-points)
2. [Designing Effective Extension Points](#designing-effective-extension-points)
3. [Implementing Extension Points](#implementing-extension-points)
4. [Using Extension Points](#using-extension-points)
5. [Testing Extension Points](#testing-extension-points)
6. [Best Practices](#best-practices)
7. [Real-World Examples](#real-world-examples)

## Understanding Extension Points

Extension points are the foundation of the plugin system. They define the contract between your microservice and the plugins that extend it.

### What is an Extension Point?

An extension point is an interface or abstract class that:

- Defines a clear API that plugins must implement
- Is annotated with `@ExtensionPoint`
- Has a unique ID for discovery
- Specifies whether it allows single or multiple implementations
- Provides documentation about its purpose and usage

### Why Use Extension Points?

Extension points provide several benefits:

- **Modularity**: Separate core functionality from extensions
- **Flexibility**: Allow different implementations without changing core code
- **Stability**: Provide a stable API for plugins to implement
- **Discoverability**: Make extension capabilities explicit and documented
- **Versioning**: Enable evolution of the API over time

## Designing Effective Extension Points

Designing good extension points requires careful consideration of several factors:

### 1. Identify Extension Opportunities

Look for areas in your microservice where:

- Functionality might vary between deployments
- Different algorithms or strategies could be used
- Integration with external systems is needed
- Customization is likely to be required
- Business rules might change frequently

### 2. Define Clear Boundaries

Extension points should have:

- A single, well-defined responsibility
- Clear input and output parameters
- Minimal dependencies on other parts of the system
- A focused, cohesive API

### 3. Consider Granularity

Extension points can be:

- **Fine-grained**: Small, focused extension points for specific functionality
- **Coarse-grained**: Broader extension points that cover larger areas of functionality

The right granularity depends on your needs:

- Fine-grained extension points provide more flexibility but require more management
- Coarse-grained extension points are easier to manage but less flexible

### 4. Plan for Evolution

Extension points should be designed to evolve over time:

- Use interface segregation to keep interfaces focused
- Consider using the builder pattern for complex parameters
- Use version information in extension point IDs if needed
- Document compatibility requirements

## Implementing Extension Points

Implementing extension points in your microservice involves several steps:

### 1. Create the Extension Point Interface

```java
package com.catalis.banking.accounts;

import com.catalis.core.plugin.annotation.ExtensionPoint;
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
     * Enriches an account with additional information.
     * 
     * @param account The account to enrich
     * @return A Mono emitting the enriched account
     */
    Mono<Account> enrichAccount(Account account);
    
    /**
     * Returns the priority of this enricher.
     * Higher priority enrichers are applied first.
     * 
     * @return the priority value
     */
    int getPriority();
    
    /**
     * Returns the name of this enricher.
     * 
     * @return the enricher name
     */
    String getEnricherName();
    
    /**
     * Checks if this enricher supports the given account type.
     * 
     * @param accountType the account type to check
     * @return true if this enricher supports the account type
     */
    boolean supportsAccountType(String accountType);
}
```

### 2. Register the Extension Point

The extension point needs to be registered with the Extension Registry:

```java
@Configuration
public class AccountsConfiguration {
    
    @Bean
    public ExtensionPoint accountEnricherExtensionPoint() {
        return new ExtensionPointImpl(
            "com.catalis.banking.accounts.account-enricher",
            "Extension point for enriching account information with additional data",
            true,
            AccountEnricher.class
        );
    }
    
    @Bean
    public Mono<Void> registerExtensionPoints(ExtensionRegistry extensionRegistry) {
        return extensionRegistry.registerExtensionPoint(accountEnricherExtensionPoint());
    }
}
```

### 3. Document the Extension Point

Provide comprehensive documentation for your extension point:

- **Purpose**: What the extension point is for
- **Contract**: What methods must be implemented
- **Parameters**: What each parameter means
- **Return Values**: What the return values represent
- **Exceptions**: What exceptions might be thrown
- **Threading**: Any threading considerations
- **Examples**: Sample implementations

## Using Extension Points

Once you've defined an extension point, you need to use it in your microservice:

### 1. Inject the Plugin Manager

```java
@Service
public class AccountEnrichmentService {
    
    private final PluginManager pluginManager;
    
    public AccountEnrichmentService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
    
    // Service methods...
}
```

### 2. Get Extensions from the Extension Registry

```java
/**
 * Enriches an account with information from all applicable enrichers.
 * 
 * @param account the account to enrich
 * @return a Mono emitting the enriched account
 */
public Mono<Account> enrichAccount(Account account) {
    return pluginManager.getExtensionRegistry()
            .getExtensions("com.catalis.banking.accounts.account-enricher")
            .cast(AccountEnricher.class)
            .filter(enricher -> enricher.supportsAccountType(account.getType()))
            .sort((e1, e2) -> Integer.compare(e2.getPriority(), e1.getPriority()))
            // Apply each enricher in sequence
            .reduce(Mono.just(account), (accountMono, enricher) ->
                    accountMono.flatMap(enricher::enrichAccount))
            // Get the final result
            .flatMap(mono -> mono);
}
```

### 3. Handle Missing Extensions

Consider what should happen if no extensions are available:

```java
/**
 * Gets the highest priority enricher for a specific account type.
 * 
 * @param accountType the account type
 * @return a Mono emitting the highest priority enricher, or empty if none found
 */
public Mono<AccountEnricher> getHighestPriorityEnricher(String accountType) {
    return pluginManager.getExtensionRegistry()
            .getExtensions("com.catalis.banking.accounts.account-enricher")
            .cast(AccountEnricher.class)
            .filter(enricher -> enricher.supportsAccountType(accountType))
            .sort((e1, e2) -> Integer.compare(e2.getPriority(), e1.getPriority()))
            .next()
            .switchIfEmpty(Mono.defer(() -> {
                logger.warn("No enricher found for account type: {}", accountType);
                return Mono.empty();
            }));
}
```

## Testing Extension Points

Testing extension points is crucial to ensure they work correctly:

### 1. Unit Testing

Test the extension point interface with mock implementations:

```java
@Test
void testAccountEnricherExtensionPoint() {
    // Create a mock implementation
    AccountEnricher mockEnricher = new AccountEnricher() {
        @Override
        public Mono<Account> enrichAccount(Account account) {
            account.getMetadata().put("testKey", "testValue");
            return Mono.just(account);
        }
        
        @Override
        public int getPriority() {
            return 100;
        }
        
        @Override
        public String getEnricherName() {
            return "Test Enricher";
        }
        
        @Override
        public boolean supportsAccountType(String accountType) {
            return "PERSONAL".equals(accountType);
        }
    };
    
    // Create a test account
    Account account = new Account();
    account.setType("PERSONAL");
    account.setMetadata(new HashMap<>());
    
    // Test the enricher
    Account enrichedAccount = mockEnricher.enrichAccount(account).block();
    
    assertNotNull(enrichedAccount);
    assertEquals("testValue", enrichedAccount.getMetadata().get("testKey"));
}
```

### 2. Integration Testing

Test the extension point with the Plugin Manager:

```java
@SpringBootTest
class AccountEnrichmentServiceIntegrationTest {
    
    @Autowired
    private PluginManager pluginManager;
    
    @Autowired
    private AccountEnrichmentService enrichmentService;
    
    @Autowired
    private ExtensionRegistry extensionRegistry;
    
    @Test
    void testEnrichAccount() {
        // Register a test extension point
        ExtensionPoint extensionPoint = new ExtensionPointImpl(
                "com.catalis.banking.accounts.account-enricher",
                "Test extension point",
                true,
                AccountEnricher.class);
        extensionRegistry.registerExtensionPoint(extensionPoint).block();
        
        // Register a test extension
        TestAccountEnricher testEnricher = new TestAccountEnricher();
        extensionRegistry.registerExtension(
                new ExtensionImpl("com.catalis.banking.accounts.account-enricher", testEnricher, 100))
                .block();
        
        // Create a test account
        Account account = new Account();
        account.setType("PERSONAL");
        account.setMetadata(new HashMap<>());
        
        // Test the enrichment service
        Account enrichedAccount = enrichmentService.enrichAccount(account).block();
        
        assertNotNull(enrichedAccount);
        assertEquals("testValue", enrichedAccount.getMetadata().get("testKey"));
    }
    
    static class TestAccountEnricher implements AccountEnricher {
        // Implementation...
    }
}
```

## Best Practices

Follow these best practices when creating extension points:

### 1. Design Principles

- **Single Responsibility**: Each extension point should have a single, clear purpose
- **Interface Segregation**: Keep interfaces focused and cohesive
- **Dependency Inversion**: Depend on abstractions, not concrete implementations
- **Open/Closed**: Design for extension without modification

### 2. Naming Conventions

- Use clear, descriptive names for extension points
- Follow a consistent naming pattern (e.g., `EntityAction`, `EntityProcessor`)
- Use reverse-domain notation for extension point IDs (e.g., `com.catalis.banking.accounts.account-enricher`)

### 3. Documentation

- Document the purpose and usage of each extension point
- Provide examples of how to implement the extension point
- Document any constraints or requirements
- Explain the lifecycle and threading model

### 4. Versioning

- Consider including version information in extension point IDs
- Document compatibility requirements
- Plan for backward compatibility
- Use deprecation annotations when evolving APIs

### 5. Error Handling

- Define clear error handling expectations
- Document exceptions that might be thrown
- Consider providing fallback mechanisms
- Handle missing or failing extensions gracefully

## Real-World Examples

Here are some real-world examples of extension points in the Firefly Platform:

### Example 1: Payment Processor

```java
@ExtensionPoint(
    id = "com.catalis.banking.payments.payment-processor",
    description = "Extension point for payment processing services",
    allowMultiple = true
)
public interface PaymentProcessor {
    
    /**
     * Checks if this processor supports the given payment method.
     * 
     * @param paymentMethod the payment method to check
     * @return true if this processor supports the payment method
     */
    boolean supportsPaymentMethod(String paymentMethod);
    
    /**
     * Processes a payment using this processor.
     * 
     * @param amount the payment amount
     * @param currency the payment currency
     * @param reference the payment reference
     * @param metadata additional payment metadata
     * @return a Mono that emits the payment ID when complete
     */
    Mono<String> processPayment(BigDecimal amount, String currency, 
                               String reference, Map<String, Object> metadata);
    
    /**
     * Gets the priority of this payment processor.
     * Higher priority processors are tried first.
     * 
     * @return the priority value
     */
    int getPriority();
}
```

### Example 2: Fraud Detector

```java
@ExtensionPoint(
    id = "com.catalis.banking.security.fraud-detector",
    description = "Extension point for transaction fraud detection",
    allowMultiple = true
)
public interface FraudDetector {
    
    /**
     * Analyzes a transaction for potential fraud.
     * 
     * @param transactionId the transaction ID
     * @param amount the transaction amount
     * @param currency the transaction currency
     * @param customerId the customer ID
     * @param metadata additional transaction metadata
     * @return a Mono that emits the fraud detection result
     */
    Mono<FraudDetectionResult> analyzeTransaction(
            String transactionId,
            BigDecimal amount,
            String currency,
            String customerId,
            Map<String, Object> metadata);
    
    /**
     * Gets the confidence threshold for fraud detection.
     * Transactions with a fraud score above this threshold are considered fraudulent.
     * 
     * @return the confidence threshold (0.0 to 1.0)
     */
    double getConfidenceThreshold();
    
    /**
     * Gets the priority of this fraud detector.
     * Higher priority detectors are used first.
     * 
     * @return the priority value
     */
    int getPriority();
}
```

### Example 3: Customer Notifier

```java
@ExtensionPoint(
    id = "com.catalis.banking.notifications.customer-notifier",
    description = "Extension point for customer notification services",
    allowMultiple = true
)
public interface CustomerNotifier {
    
    /**
     * Checks if this notifier supports the given notification type and channel.
     * 
     * @param notificationType the type of notification
     * @param channel the notification channel
     * @return true if this notifier supports the combination
     */
    boolean supportsNotification(String notificationType, String channel);
    
    /**
     * Sends a notification to a customer.
     * 
     * @param customerId the customer ID
     * @param notificationType the type of notification
     * @param channel the notification channel
     * @param content the notification content
     * @param metadata additional notification metadata
     * @return a Mono that completes when the notification is sent
     */
    Mono<Void> sendNotification(
            String customerId,
            String notificationType,
            String channel,
            Map<String, Object> content,
            Map<String, Object> metadata);
    
    /**
     * Gets the priority of this notifier.
     * Higher priority notifiers are tried first.
     * 
     * @return the priority value
     */
    int getPriority();
}
```

By following these guidelines, you can create effective extension points that allow your microservices to be extended through plugins, providing flexibility and customization without compromising stability or maintainability.
