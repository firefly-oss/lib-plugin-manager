# Credit Card Payment Plugin Example

This example demonstrates how to create a plugin that implements a payment processor extension point. The Credit Card Payment Plugin processes payments using credit cards and integrates with a payment gateway.

## Overview

The Credit Card Payment Plugin:
- Implements the `PaymentProcessor` extension point
- Supports major credit card networks (Visa, Mastercard, Amex, Discover)
- Processes payments through a simulated payment gateway
- Validates credit card information before processing

## Prerequisites

- Java 21
- Spring Boot 3.2.2 or higher
- Firefly Plugin Manager library

## Step 1: Define the Extension Point

First, define the extension point interface that our plugin will implement:

```java
package com.example.payment;

import com.firefly.core.plugin.annotation.ExtensionPoint;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@ExtensionPoint(
        id = "com.example.payment.processor",
        description = "Extension point for payment processing services",
        allowMultiple = true
)
public interface PaymentProcessor {
    
    /**
     * Gets the payment method name.
     * 
     * @return the payment method name
     */
    String getPaymentMethodName();
    
    /**
     * Processes a payment.
     * 
     * @param amount the payment amount
     * @param currency the payment currency
     * @param reference the payment reference
     * @return a Mono that emits the payment ID when the payment is processed
     */
    Mono<String> processPayment(BigDecimal amount, String currency, String reference);
    
    /**
     * Checks if this payment processor supports a specific currency.
     * 
     * @param currency the currency to check
     * @return true if the currency is supported, false otherwise
     */
    boolean supportsCurrency(String currency);
}
```

## Step 2: Create a Payment Gateway Client

Create a client to interact with the payment gateway:

```java
package com.example.payment.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

/**
 * Client for interacting with a payment gateway.
 * This is a simplified example that simulates communication with a payment gateway.
 */
public class PaymentGatewayClient {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayClient.class);
    
    /**
     * Processes a credit card payment.
     * 
     * @param cardNumber the credit card number
     * @param expiryDate the expiry date (MM/YY)
     * @param cvv the CVV code
     * @param amount the payment amount
     * @param currency the payment currency
     * @return a Mono that emits the transaction ID
     */
    public Mono<String> processCreditCardPayment(
            String cardNumber, 
            String expiryDate, 
            String cvv, 
            BigDecimal amount, 
            String currency) {
        
        logger.info("Processing credit card payment: amount={}, currency={}", amount, currency);
        
        // Simulate network delay
        return Mono.delay(Duration.ofMillis(500))
                .then(Mono.fromCallable(() -> {
                    // Generate a transaction ID
                    String transactionId = "TX-" + UUID.randomUUID().toString();
                    logger.info("Payment processed successfully: {}", transactionId);
                    return transactionId;
                }));
    }
    
    /**
     * Validates a credit card.
     * 
     * @param cardNumber the credit card number
     * @param expiryDate the expiry date (MM/YY)
     * @param cvv the CVV code
     * @return true if the card is valid, false otherwise
     */
    public boolean validateCreditCard(String cardNumber, String expiryDate, String cvv) {
        // Simple validation logic (in a real implementation, this would be more robust)
        boolean isValid = cardNumber != null && cardNumber.length() >= 13 && cardNumber.length() <= 19
                && expiryDate != null && expiryDate.matches("\\d{2}/\\d{2}")
                && cvv != null && cvv.matches("\\d{3,4}");
        
        logger.info("Credit card validation result: {}", isValid);
        return isValid;
    }
}
```

## Step 3: Implement the Plugin

Now, create the plugin class that implements the extension point:

```java
package com.example.payment.plugin;

import com.firefly.core.plugin.annotation.Extension;
import com.firefly.core.plugin.annotation.Plugin;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.model.PluginMetadata;
import com.firefly.core.plugin.spi.AbstractPlugin;
import com.example.payment.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Plugin that implements the PaymentProcessor extension point for credit card payments.
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
    private final CreditCardPaymentProcessor paymentProcessor;
    
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
        this.paymentProcessor = new CreditCardPaymentProcessor();
    }
    
    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing Credit Card Payment Plugin");
        return Mono.empty();
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
    
    /**
     * Gets the payment processor implementation.
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
            extensionPointId = "com.example.payment.processor",
            priority = 100,
            description = "Credit card payment processor"
    )
    public class CreditCardPaymentProcessor implements PaymentProcessor {
        
        // Default test credit card for demonstration
        private static final String TEST_CARD_NUMBER = "4111111111111111";
        private static final String TEST_EXPIRY_DATE = "12/25";
        private static final String TEST_CVV = "123";
        
        @Override
        public String getPaymentMethodName() {
            return "Credit Card";
        }
        
        @Override
        public Mono<String> processPayment(BigDecimal amount, String currency, String reference) {
            logger.info("Processing credit card payment: amount={}, currency={}, reference={}",
                    amount, currency, reference);
            
            if (!supportsCurrency(currency)) {
                return Mono.error(new UnsupportedOperationException("Currency not supported: " + currency));
            }
            
            // In a real implementation, card details would come from the request
            // Here we're using test values for demonstration
            if (gatewayClient.validateCreditCard(TEST_CARD_NUMBER, TEST_EXPIRY_DATE, TEST_CVV)) {
                return gatewayClient.processCreditCardPayment(
                        TEST_CARD_NUMBER, TEST_EXPIRY_DATE, TEST_CVV, amount, currency);
            } else {
                return Mono.error(new IllegalArgumentException("Invalid credit card details"));
            }
        }
        
        @Override
        public boolean supportsCurrency(String currency) {
            // Support major currencies
            return Set.of("USD", "EUR", "GBP", "JPY", "CAD", "AUD").contains(currency);
        }
    }
}
```

## Step 4: Register the Plugin

There are several ways to register the plugin:

### Option 1: Using Service Provider Interface (SPI)

Create a file at `META-INF/services/com.firefly.core.plugin.api.Plugin` with the fully qualified name of your plugin class:

```
com.example.payment.plugin.CreditCardPaymentPlugin
```

### Option 2: Programmatic Registration

```java
// Create the plugin instance
PluginEventBus eventBus = pluginManager.getEventBus();
CreditCardPaymentPlugin plugin = new CreditCardPaymentPlugin(eventBus);

// Register the plugin
pluginManager.getPluginRegistry().registerPlugin(plugin).block();

// Register the extension
pluginManager.getExtensionRegistry()
        .registerExtension(
                "com.example.payment.processor",
                plugin.getPaymentProcessor(),
                100)
        .block();

// Start the plugin
pluginManager.startPlugin(plugin.getMetadata().id()).block();
```

### Option 3: Using Classpath Scanning

If your plugin is annotated with `@Plugin`, you can use classpath scanning:

```java
// Scan for plugins in a specific package
pluginManager.installPluginsFromClasspath("com.example.payment").block();

// Or scan the entire classpath
pluginManager.installPluginsFromClasspath().block();
```

## Step 5: Using the Plugin

Once the plugin is registered, you can use it through the extension registry:

```java
// Get the payment processor extension
PaymentProcessor paymentProcessor = pluginManager.getExtensionRegistry()
        .getHighestPriorityExtension("com.example.payment.processor")
        .block();

if (paymentProcessor != null) {
    // Process a payment
    String paymentId = paymentProcessor.processPayment(
            BigDecimal.valueOf(99.99),
            "USD",
            "ORDER-12345")
            .block();
    
    System.out.println("Payment processed with ID: " + paymentId);
} else {
    System.err.println("No payment processor found");
}
```

## Step 6: Testing the Plugin

Create a test class to verify the plugin's functionality:

```java
package com.example.payment.plugin;

import com.firefly.core.plugin.event.DefaultPluginEventBus;
import com.firefly.core.plugin.event.PluginEventBus;
import com.example.payment.PaymentProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CreditCardPaymentPluginTest {
    
    private CreditCardPaymentPlugin plugin;
    private PaymentProcessor paymentProcessor;
    
    @BeforeEach
    void setUp() {
        PluginEventBus eventBus = new DefaultPluginEventBus();
        plugin = new CreditCardPaymentPlugin(eventBus);
        paymentProcessor = plugin.getPaymentProcessor();
    }
    
    @Test
    void testGetPaymentMethodName() {
        assertEquals("Credit Card", paymentProcessor.getPaymentMethodName());
    }
    
    @Test
    void testSupportsCurrency() {
        assertTrue(paymentProcessor.supportsCurrency("USD"));
        assertTrue(paymentProcessor.supportsCurrency("EUR"));
        assertFalse(paymentProcessor.supportsCurrency("XYZ"));
    }
    
    @Test
    void testProcessPayment() {
        StepVerifier.create(paymentProcessor.processPayment(
                        BigDecimal.valueOf(100.00),
                        "USD",
                        "TEST-REF-001"))
                .expectNextMatches(paymentId -> paymentId != null && !paymentId.isEmpty())
                .verifyComplete();
    }
    
    @Test
    void testProcessPaymentWithUnsupportedCurrency() {
        StepVerifier.create(paymentProcessor.processPayment(
                        BigDecimal.valueOf(100.00),
                        "XYZ",
                        "TEST-REF-002"))
                .expectError(UnsupportedOperationException.class)
                .verify();
    }
}
```

## Conclusion

This example demonstrates how to create a Credit Card Payment Plugin that implements the PaymentProcessor extension point. The plugin provides a way to process payments using credit cards through a payment gateway.

Key points:
- The plugin extends `AbstractPlugin` and is annotated with `@Plugin`
- The extension implementation is annotated with `@Extension`
- The plugin uses the event bus for communication
- The plugin can be registered and used through the plugin manager

By following this pattern, you can create your own payment processing plugins that integrate with different payment providers or support different payment methods.
