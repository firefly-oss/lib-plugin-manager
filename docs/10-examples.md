# Plugin Examples

This section provides practical examples of plugins for the Firefly Plugin Manager, demonstrating how to implement different types of extensions and integrate with various services.

## Table of Contents

1. [Credit Card Payment Plugin](#credit-card-payment-plugin)
2. [Machine Learning Fraud Detector](#machine-learning-fraud-detector)
3. [Company Enrichment Information](#company-enrichment-information)
4. [Treezor Integration for Core Banking Accounts](#treezor-integration-for-core-banking-accounts)

## Credit Card Payment Plugin

This example demonstrates how to create a plugin that implements a payment processor extension point for credit card payments.

### Overview

The Credit Card Payment Plugin:
- Implements the `PaymentProcessor` extension point
- Supports major credit card networks (Visa, Mastercard, Amex, Discover)
- Processes payments through a payment gateway
- Validates credit card information before processing

### Step 1: Define the Extension Point

First, let's look at the extension point defined in the core microservice:

```java
// In the core-banking-payments microservice
package com.catalis.banking.payment;

import com.catalis.core.plugin.annotation.ExtensionPoint;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@ExtensionPoint(
    id = "com.catalis.banking.payment-processor",
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
     * @return a Mono that emits the payment ID when complete
     */
    Mono<String> processPayment(BigDecimal amount, String currency, String reference);

    /**
     * Gets the priority of this payment processor.
     * Higher priority processors are tried first.
     *
     * @return the priority value
     */
    int getPriority();
}
```

### Step 2: Create the Plugin Class

Now, let's create the main plugin class:

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

### Step 3: Implement the Extension

Next, let's implement the `PaymentProcessor` extension point:

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

### Step 4: Create the Payment Gateway Client

Now, let's create the service that interacts with the payment gateway:

```java
package com.example.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

public class PaymentGatewayClient {

    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayClient.class);

    private String gatewayUrl = "https://example.com/payment-gateway";
    private String apiKey = "default-api-key";
    private int timeout = 30;
    private int retryCount = 3;

    public Mono<Void> initialize() {
        logger.info("Initializing payment gateway client: url={}", gatewayUrl);

        // In a real implementation, this would establish a connection to the gateway
        return Mono.delay(Duration.ofMillis(100)).then();
    }

    public Mono<String> processCreditCardPayment(BigDecimal amount, String currency, String reference) {
        logger.info("Sending payment request to gateway: amount={}, currency={}, reference={}",
                amount, currency, reference);

        // In a real implementation, this would call the payment gateway API
        return Mono.delay(Duration.ofMillis(200))
                .map(ignored -> UUID.randomUUID().toString());
    }

    // Getters and setters for configuration properties
    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
```

### Step 5: Register the Extension

Finally, let's modify the plugin class to register the extension:

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

### Step 6: Using the Plugin

Here's how a microservice would use this plugin:

```java
@Service
public class PaymentService {

    private final PluginManager pluginManager;

    public PaymentService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public Mono<String> processPayment(String paymentMethod, BigDecimal amount,
                                      String currency, String reference) {

        return pluginManager.getExtensionRegistry()
                .getExtensions("com.catalis.banking.payment-processor")
                .cast(PaymentProcessor.class)
                .filter(processor -> processor.supportsPaymentMethod(paymentMethod))
                .sort((p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority()))
                .next()
                .switchIfEmpty(Mono.error(new UnsupportedOperationException(
                        "No payment processor found for method: " + paymentMethod)))
                .flatMap(processor -> processor.processPayment(amount, currency, reference));
    }
}
```

### Key Points

- The plugin extends `AbstractPlugin` and is annotated with `@Plugin`
- The extension implementation is annotated with `@Extension`
- The plugin uses a service to interact with an external system
- The extension is registered during plugin initialization
- The plugin can be configured with different gateway settings
- The microservice discovers and uses the extension through the Plugin Manager

## Machine Learning Fraud Detector

This example demonstrates how to create a plugin that implements a fraud detection extension point using machine learning techniques.

### Overview

The Machine Learning Fraud Detector Plugin:
- Implements the `FraudDetector` extension point
- Uses a pre-trained machine learning model to detect fraud
- Provides real-time fraud risk scoring for transactions
- Supports both synchronous and asynchronous fraud detection

### Step 1: Define the Extension Point

First, let's look at the extension point defined in the core microservice:

```java
// In the core-banking-security microservice
package com.catalis.banking.security;

import com.catalis.core.plugin.annotation.ExtensionPoint;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

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

### Step 2: Create the Result Class

Let's create a class to represent the fraud detection result:

```java
package com.catalis.banking.security;

public class FraudDetectionResult {

    private final String transactionId;
    private final boolean fraudulent;
    private final double fraudScore;
    private final String reason;

    public FraudDetectionResult(String transactionId, boolean fraudulent,
                               double fraudScore, String reason) {
        this.transactionId = transactionId;
        this.fraudulent = fraudulent;
        this.fraudScore = fraudScore;
        this.reason = reason;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public boolean isFraudulent() {
        return fraudulent;
    }

    public double getFraudScore() {
        return fraudScore;
    }

    public String getReason() {
        return reason;
    }
}
```

### Step 3: Create the Plugin Class

Now, let's create the main plugin class:

```java
package com.example.fraud;

import com.catalis.core.plugin.annotation.Plugin;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.spi.AbstractPlugin;
import com.example.fraud.service.MachineLearningModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

@Plugin(
    id = "com.example.fraud.ml-detector",
    name = "Machine Learning Fraud Detector",
    version = "1.0.0",
    description = "Detects fraudulent transactions using machine learning",
    author = "Example Inc."
)
public class MachineLearningFraudDetectorPlugin extends AbstractPlugin {

    private static final Logger logger = LoggerFactory.getLogger(MachineLearningFraudDetectorPlugin.class);

    private final PluginEventBus eventBus;
    private final MachineLearningModelService modelService;

    public MachineLearningFraudDetectorPlugin(PluginEventBus eventBus) {
        super(PluginMetadata.builder()
                .id("com.example.fraud.ml-detector")
                .name("Machine Learning Fraud Detector")
                .version("1.0.0")
                .description("Detects fraudulent transactions using machine learning")
                .author("Example Inc.")
                .minPlatformVersion("1.0.0")
                .dependencies(Set.of())
                .installTime(Instant.now())
                .build());

        this.eventBus = eventBus;
        this.modelService = new MachineLearningModelService();
    }

    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing Machine Learning Fraud Detector Plugin");
        return modelService.loadModel();
    }

    @Override
    public Mono<Void> start() {
        logger.info("Starting Machine Learning Fraud Detector Plugin");
        return Mono.empty();
    }

    @Override
    public Mono<Void> stop() {
        logger.info("Stopping Machine Learning Fraud Detector Plugin");
        return Mono.empty();
    }

    @Override
    public Mono<Void> uninstall() {
        logger.info("Uninstalling Machine Learning Fraud Detector Plugin");
        return modelService.unloadModel();
    }
}
```

### Step 4: Implement the Extension

Next, let's implement the `FraudDetector` extension point:

```java
package com.example.fraud.extension;

import com.catalis.banking.security.FraudDetector;
import com.catalis.banking.security.FraudDetectionResult;
import com.catalis.core.plugin.annotation.Extension;
import com.example.fraud.service.MachineLearningModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Extension(
    extensionPointId = "com.catalis.banking.security.fraud-detector",
    priority = 100,
    description = "Detects fraud using machine learning"
)
public class MachineLearningFraudDetector implements FraudDetector {

    private static final Logger logger = LoggerFactory.getLogger(MachineLearningFraudDetector.class);

    private final MachineLearningModelService modelService;
    private final double confidenceThreshold = 0.7;

    public MachineLearningFraudDetector(MachineLearningModelService modelService) {
        this.modelService = modelService;
    }

    @Override
    public Mono<FraudDetectionResult> analyzeTransaction(
            String transactionId,
            BigDecimal amount,
            String currency,
            String customerId,
            Map<String, Object> metadata) {

        logger.info("Analyzing transaction for fraud: id={}, amount={}, currency={}, customer={}",
                transactionId, amount, currency, customerId);

        return modelService.predictFraudScore(amount, customerId, metadata)
                .map(fraudScore -> {
                    boolean isFraudulent = fraudScore >= getConfidenceThreshold();
                    String reason = isFraudulent
                            ? "Transaction flagged as potentially fraudulent by ML model"
                            : "Transaction appears legitimate";

                    return new FraudDetectionResult(transactionId, isFraudulent, fraudScore, reason);
                });
    }

    @Override
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
```

### Step 5: Create the Model Service

Now, let's create the service that interacts with the machine learning model:

```java
package com.example.fraud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

public class MachineLearningModelService {

    private static final Logger logger = LoggerFactory.getLogger(MachineLearningModelService.class);

    // Weights for different factors in the fraud score calculation
    private final double amountWeight = 0.4;
    private final double customerHistoryWeight = 0.3;
    private final double locationWeight = 0.2;
    private final double timeOfDayWeight = 0.1;

    public Mono<Void> loadModel() {
        logger.info("Loading machine learning model");

        // In a real implementation, this would load a pre-trained model
        return Mono.delay(Duration.ofMillis(500)).then();
    }

    public Mono<Void> unloadModel() {
        logger.info("Unloading machine learning model");

        // In a real implementation, this would release model resources
        return Mono.empty();
    }

    public Mono<Double> predictFraudScore(BigDecimal amount, String customerId, Map<String, Object> metadata) {
        logger.info("Predicting fraud score: amount={}, customer={}", amount, customerId);

        return Mono.fromCallable(() -> {
            // Simulate model prediction (in a real implementation, this would use the actual model)

            // Factor 1: Transaction amount (higher amounts have higher risk)
            double amountFactor = calculateAmountFactor(amount);

            // Factor 2: Customer history (based on customer ID)
            double customerFactor = calculateCustomerFactor(customerId);

            // Factor 3: Location (from metadata)
            double locationFactor = calculateLocationFactor(metadata);

            // Factor 4: Time of day (from metadata)
            double timeFactor = calculateTimeFactor(metadata);

            // Combine factors with weights
            double fraudScore = (amountFactor * amountWeight) +
                    (customerFactor * customerHistoryWeight) +
                    (locationFactor * locationWeight) +
                    (timeFactor * timeOfDayWeight);

            return Math.min(1.0, Math.max(0.0, fraudScore));
        });
    }

    private double calculateAmountFactor(BigDecimal amount) {
        // Simple heuristic: higher amounts have higher risk
        double value = amount.doubleValue();
        if (value > 10000) return 0.9;
        if (value > 5000) return 0.7;
        if (value > 1000) return 0.5;
        if (value > 500) return 0.3;
        return 0.1;
    }

    private double calculateCustomerFactor(String customerId) {
        // In a real implementation, this would check customer history
        // For demonstration, we'll use a simple hash-based approach
        return (double) (Math.abs(customerId.hashCode()) % 100) / 100.0;
    }

    private double calculateLocationFactor(Map<String, Object> metadata) {
        // In a real implementation, this would analyze location risk
        // For demonstration, we'll use a simple approach based on metadata
        if (metadata.containsKey("location")) {
            String location = (String) metadata.get("location");
            if (location.contains("high-risk")) return 0.9;
            if (location.contains("medium-risk")) return 0.5;
            return 0.2;
        }
        return 0.3; // Default if no location data
    }

    private double calculateTimeFactor(Map<String, Object> metadata) {
        // In a real implementation, this would analyze time-of-day risk
        // For demonstration, we'll use a simple approach based on metadata
        if (metadata.containsKey("timeOfDay")) {
            int hour = (int) metadata.get("timeOfDay");
            if (hour >= 0 && hour < 6) return 0.8; // Late night/early morning
            if (hour >= 6 && hour < 9) return 0.3; // Morning
            if (hour >= 9 && hour < 17) return 0.2; // Business hours
            if (hour >= 17 && hour < 22) return 0.4; // Evening
            return 0.6; // Late evening
        }
        return 0.4; // Default if no time data
    }
}
```

### Step 6: Register the Extension

Finally, let's modify the plugin class to register the extension:

```java
@Override
public Mono<Void> initialize() {
    logger.info("Initializing Machine Learning Fraud Detector Plugin");

    // Create the extension
    MachineLearningFraudDetector fraudDetector = new MachineLearningFraudDetector(modelService);

    // Register the extension
    return getPluginManager().getExtensionRegistry()
            .registerExtension(new ExtensionImpl(
                    "com.catalis.banking.security.fraud-detector",
                    fraudDetector,
                    100))
            .then(modelService.loadModel());
}
```

### Step 7: Using the Plugin

Here's how a microservice would use this plugin:

```java
@Service
public class TransactionSecurityService {

    private final PluginManager pluginManager;

    public TransactionSecurityService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public Mono<FraudDetectionResult> checkForFraud(String transactionId, BigDecimal amount,
                                                  String currency, String customerId,
                                                  Map<String, Object> metadata) {

        return pluginManager.getExtensionRegistry()
                .getExtensions("com.catalis.banking.security.fraud-detector")
                .cast(FraudDetector.class)
                .sort((d1, d2) -> Integer.compare(d2.getPriority(), d1.getPriority()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalStateException("No fraud detector available")))
                .flatMap(detector -> detector.analyzeTransaction(
                        transactionId, amount, currency, customerId, metadata));
    }
}
```

### Key Points

- The plugin uses a machine learning approach to detect fraud
- The extension calculates a fraud score based on multiple factors
- The plugin demonstrates how to load and use a model
- The extension uses a confidence threshold to determine fraudulent transactions
- The microservice can use the extension to check transactions for fraud

## Company Enrichment Information

This example demonstrates how to create a plugin that implements a company information enrichment extension point using the eInforma Developer API.

### Overview

The Company Enrichment Information Plugin:
- Implements the `CompanyEnricher` extension point
- Connects to the eInforma Developer API to retrieve company information
- Enriches customer records with detailed company data
- Caches results to minimize API calls

### Step 1: Define the Extension Point

First, let's look at the extension point defined in the core microservice:

```java
// In the core-banking-accounts microservice
package com.catalis.banking.accounts.enrichment;

import com.catalis.core.plugin.annotation.ExtensionPoint;
import reactor.core.publisher.Mono;

import java.util.Map;

@ExtensionPoint(
    id = "com.catalis.banking.accounts.company-enricher",
    description = "Extension point for company information enrichment services",
    allowMultiple = true
)
public interface CompanyEnricher {

    /**
     * Checks if this enricher supports the given company identifier type.
     *
     * @param identifierType the type of identifier (e.g., "CIF", "NIF", "VAT")
     * @return true if this enricher supports the identifier type
     */
    boolean supportsIdentifierType(String identifierType);

    /**
     * Enriches company information based on the provided identifier.
     *
     * @param identifierType the type of identifier (e.g., "CIF", "NIF", "VAT")
     * @param identifierValue the value of the identifier
     * @return a Mono that emits the enriched company data as a Map
     */
    Mono<Map<String, Object>> enrichCompanyData(String identifierType, String identifierValue);

    /**
     * Searches for companies by name.
     *
     * @param companyName the company name to search for
     * @param maxResults the maximum number of results to return
     * @return a Mono that emits a list of matching companies
     */
    Mono<Map<String, Object>> searchCompaniesByName(String companyName, int maxResults);

    /**
     * Gets the priority of this enricher.
     * Higher priority enrichers are tried first.
     *
     * @return the priority value
     */
    int getPriority();
}
```

### Step 2: Create the Plugin Class

Now, let's create the main plugin class:

```java
package com.example.enrichment;

import com.catalis.core.plugin.annotation.Plugin;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.spi.AbstractPlugin;
import com.example.enrichment.service.EInformaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
    id = "com.example.enrichment.einforma",
    name = "eInforma Company Enrichment",
    version = "1.0.0",
    description = "Enriches company information using the eInforma Developer API",
    author = "Example Inc."
)
public class EInformaCompanyEnrichmentPlugin extends AbstractPlugin {

    private static final Logger logger = LoggerFactory.getLogger(EInformaCompanyEnrichmentPlugin.class);

    private final PluginEventBus eventBus;
    private final Map<String, Object> configuration;
    private final Map<String, Map<String, Object>> cache;
    private EInformaApiClient apiClient;

    public EInformaCompanyEnrichmentPlugin(PluginEventBus eventBus) {
        super(PluginMetadata.builder()
                .id("com.example.enrichment.einforma")
                .name("eInforma Company Enrichment")
                .version("1.0.0")
                .description("Enriches company information using the eInforma Developer API")
                .author("Example Inc.")
                .minPlatformVersion("1.0.0")
                .dependencies(Set.of())
                .installTime(Instant.now())
                .build());

        this.eventBus = eventBus;
        this.configuration = new ConcurrentHashMap<>();
        this.cache = new ConcurrentHashMap<>();

        // Default configuration
        configuration.put("username", "default-username");
        configuration.put("apiKey", "default-api-key");
        configuration.put("baseUrl", "https://api.einforma.com");
        configuration.put("cacheExpirationMinutes", 60);
        configuration.put("maxSearchResults", 10);
    }

    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing eInforma Company Enrichment Plugin");

        // Create API client with configuration
        apiClient = new EInformaApiClient(
                (String) configuration.get("username"),
                (String) configuration.get("apiKey"),
                (String) configuration.get("baseUrl"));

        // Subscribe to cache expiration events
        eventBus.subscribe("cache.expiration", this::handleCacheExpiration);

        return apiClient.initialize();
    }

    @Override
    public Mono<Void> updateConfiguration(Map<String, Object> newConfig) {
        logger.info("Updating configuration: {}", newConfig);

        // Update configuration
        configuration.putAll(newConfig);

        // Update API client if it exists
        if (apiClient != null) {
            apiClient.setUsername((String) configuration.get("username"));
            apiClient.setApiKey((String) configuration.get("apiKey"));
            apiClient.setBaseUrl((String) configuration.get("baseUrl"));
        }

        return Mono.empty();
    }

    private void handleCacheExpiration(Event event) {
        Map<String, Object> data = event.getData();
        String key = (String) data.get("key");
        Instant expirationTime = (Instant) data.get("expirationTime");

        if (Instant.now().isAfter(expirationTime)) {
            logger.debug("Removing expired cache entry: {}", key);
            cache.remove(key);
        }
    }

    @Override
    public Mono<Void> stop() {
        logger.info("Stopping eInforma Company Enrichment Plugin");

        // Clear cache
        cache.clear();

        return Mono.empty();
    }
}
```

### Step 3: Implement the Extension

Next, let's implement the `CompanyEnricher` extension point:

```java
package com.example.enrichment.extension;

import com.catalis.banking.accounts.enrichment.CompanyEnricher;
import com.catalis.core.plugin.annotation.Extension;
import com.example.enrichment.service.EInformaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Extension(
    extensionPointId = "com.catalis.banking.accounts.company-enricher",
    priority = 100,
    description = "Company enrichment using eInforma Developer API"
)
public class EInformaCompanyEnricherExtension implements CompanyEnricher {

    private static final Logger logger = LoggerFactory.getLogger(EInformaCompanyEnricherExtension.class);

    private static final Set<String> SUPPORTED_IDENTIFIERS = Set.of("CIF", "NIF");

    private final EInformaApiClient apiClient;
    private final Map<String, Map<String, Object>> cache;
    private final Map<String, Object> configuration;
    private final PluginEventBus eventBus;

    public EInformaCompanyEnricherExtension(EInformaApiClient apiClient,
                                          Map<String, Map<String, Object>> cache,
                                          Map<String, Object> configuration,
                                          PluginEventBus eventBus) {
        this.apiClient = apiClient;
        this.cache = cache;
        this.configuration = configuration;
        this.eventBus = eventBus;
    }

    @Override
    public boolean supportsIdentifierType(String identifierType) {
        return SUPPORTED_IDENTIFIERS.contains(identifierType.toUpperCase());
    }

    @Override
    public Mono<Map<String, Object>> enrichCompanyData(String identifierType, String identifierValue) {
        logger.info("Enriching company data: type={}, value={}", identifierType, identifierValue);

        // Check cache first
        String cacheKey = identifierType + "-" + identifierValue;
        Map<String, Object> cachedResult = cache.get(cacheKey);

        if (cachedResult != null) {
            logger.debug("Cache hit for: {}", cacheKey);
            return Mono.just(cachedResult);
        }

        logger.debug("Cache miss for: {}", cacheKey);

        // Call API based on identifier type
        Mono<Map<String, Object>> resultMono;
        if ("CIF".equalsIgnoreCase(identifierType) || "NIF".equalsIgnoreCase(identifierType)) {
            resultMono = apiClient.getCompanyByCif(identifierValue);
        } else {
            return Mono.error(new IllegalArgumentException("Unsupported identifier type: " + identifierType));
        }

        // Cache the result
        return resultMono.doOnSuccess(result -> {
            cache.put(cacheKey, result);

            // Schedule cache cleanup
            int cacheExpirationMinutes = (int) configuration.getOrDefault("cacheExpirationMinutes", 60);
            eventBus.publish("cache.expiration", Map.of(
                    "key", cacheKey,
                    "expirationTime", Instant.now().plus(Duration.ofMinutes(cacheExpirationMinutes))
            ));
        });
    }

    @Override
    public Mono<Map<String, Object>> searchCompaniesByName(String companyName, int maxResults) {
        logger.info("Searching companies by name: {}, maxResults={}", companyName, maxResults);

        // Apply configured limit if smaller than requested
        int configuredMax = (int) configuration.getOrDefault("maxSearchResults", 10);
        int limit = Math.min(maxResults, configuredMax);

        // Check cache first
        String cacheKey = "search-" + companyName + "-" + limit;
        Map<String, Object> cachedResult = cache.get(cacheKey);

        if (cachedResult != null) {
            logger.debug("Cache hit for: {}", cacheKey);
            return Mono.just(cachedResult);
        }

        logger.debug("Cache miss for: {}", cacheKey);

        // Call API
        return apiClient.searchCompaniesByName(companyName, limit)
                .doOnSuccess(result -> {
                    cache.put(cacheKey, result);

                    // Schedule cache cleanup
                    int cacheExpirationMinutes = (int) configuration.getOrDefault("cacheExpirationMinutes", 60);
                    eventBus.publish("cache.expiration", Map.of(
                            "key", cacheKey,
                            "expirationTime", Instant.now().plus(Duration.ofMinutes(cacheExpirationMinutes))
                    ));
                });
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
```

### Step 4: Create the API Client

Now, let's create the service that interacts with the eInforma API:

```java
package com.example.enrichment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class EInformaApiClient {

    private static final Logger logger = LoggerFactory.getLogger(EInformaApiClient.class);

    private String username;
    private String apiKey;
    private String baseUrl;

    public EInformaApiClient(String username, String apiKey, String baseUrl) {
        this.username = username;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public Mono<Void> initialize() {
        logger.info("Initializing eInforma API client: baseUrl={}", baseUrl);

        // In a real implementation, this would validate credentials and set up the client
        return Mono.delay(Duration.ofMillis(200)).then();
    }

    public Mono<Map<String, Object>> getCompanyByCif(String cifNif) {
        logger.info("Getting company by CIF/NIF: {}", cifNif);

        // In a real implementation, this would call the eInforma API
        return Mono.delay(Duration.ofMillis(300))
                .map(ignored -> {
                    // Simulate API response
                    Map<String, Object> company = new HashMap<>();
                    company.put("cif", cifNif);
                    company.put("nombre", "Empresa Ejemplo S.L.");
                    company.put("direccion", "Calle Gran Vía, 123");
                    company.put("localidad", "Madrid");
                    company.put("provincia", "Madrid");
                    company.put("codigoPostal", "28013");
                    company.put("telefono", "+34 91 123 45 67");
                    company.put("email", "info@ejemplo.com");
                    company.put("web", "https://www.ejemplo.com");
                    company.put("actividad", "Desarrollo de software");
                    company.put("cnae", "6201");
                    company.put("fechaConstitucion", "2010-01-15");
                    company.put("capitalSocial", 60000);
                    return company;
                });
    }

    public Mono<Map<String, Object>> searchCompaniesByName(String companyName, int limit) {
        logger.info("Searching companies by name: {}, limit={}", companyName, limit);

        // In a real implementation, this would call the eInforma API
        return Mono.delay(Duration.ofMillis(300))
                .map(ignored -> {
                    // Simulate API response
                    Map<String, Object> result = new HashMap<>();
                    result.put("totalResults", 3);
                    result.put("companies", Map.of(
                            "company1", Map.of(
                                    "cif", "B12345678",
                                    "nombre", companyName + " S.L.",
                                    "localidad", "Madrid"),
                            "company2", Map.of(
                                    "cif", "B87654321",
                                    "nombre", companyName + " Internacional S.A.",
                                    "localidad", "Barcelona"),
                            "company3", Map.of(
                                    "cif", "B55555555",
                                    "nombre", "Grupo " + companyName + " S.L.",
                                    "localidad", "Valencia")
                    ));
                    return result;
                });
    }

    // Getters and setters
    public void setUsername(String username) {
        this.username = username;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
```

### Step 5: Register the Extension

Finally, let's modify the plugin class to register the extension:

```java
@Override
public Mono<Void> initialize() {
    logger.info("Initializing eInforma Company Enrichment Plugin");

    // Create API client with configuration
    apiClient = new EInformaApiClient(
            (String) configuration.get("username"),
            (String) configuration.get("apiKey"),
            (String) configuration.get("baseUrl"));

    // Create the extension
    EInformaCompanyEnricherExtension enricher = new EInformaCompanyEnricherExtension(
            apiClient, cache, configuration, eventBus);

    // Subscribe to cache expiration events
    eventBus.subscribe("cache.expiration", this::handleCacheExpiration);

    // Register the extension
    return getPluginManager().getExtensionRegistry()
            .registerExtension(new ExtensionImpl(
                    "com.catalis.banking.accounts.company-enricher",
                    enricher,
                    100))
            .then(apiClient.initialize());
}
```

### Step 6: Using the Plugin

Here's how a microservice would use this plugin:

```java
@Service
public class CompanyEnrichmentService {

    private final PluginManager pluginManager;

    public CompanyEnrichmentService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public Mono<Map<String, Object>> getCompanyInformation(String identifierType, String identifierValue) {
        return pluginManager.getExtensionRegistry()
                .getExtensions("com.catalis.banking.accounts.company-enricher")
                .cast(CompanyEnricher.class)
                .filter(enricher -> enricher.supportsIdentifierType(identifierType))
                .sort((e1, e2) -> Integer.compare(e2.getPriority(), e1.getPriority()))
                .next()
                .switchIfEmpty(Mono.error(new UnsupportedOperationException(
                        "No company enricher found for identifier type: " + identifierType)))
                .flatMap(enricher -> enricher.enrichCompanyData(identifierType, identifierValue));
    }

    public Mono<Map<String, Object>> searchCompanies(String companyName, int maxResults) {
        return pluginManager.getExtensionRegistry()
                .getExtensions("com.catalis.banking.accounts.company-enricher")
                .cast(CompanyEnricher.class)
                .sort((e1, e2) -> Integer.compare(e2.getPriority(), e1.getPriority()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalStateException("No company enricher available")))
                .flatMap(enricher -> enricher.searchCompaniesByName(companyName, maxResults));
    }
}
```

### Key Points

- The plugin integrates with an external API (eInforma Developer API)
- The extension implements caching to minimize API calls
- The plugin uses configuration for API credentials and settings
- The extension supports different types of company identifiers
- The plugin demonstrates event-based cache expiration

## Treezor Integration for Core Banking Accounts

This example demonstrates how to create a plugin that integrates the Firefly core-banking-accounts microservice with Treezor's API for wallet functionality.

### Overview

The Treezor Integration Plugin:
- Implements the `AccountProvider` extension point
- Connects to Treezor's API for wallet management
- Translates between Firefly account model and Treezor wallet model
- Handles account creation, retrieval, and updates

### Step 1: Define the Extension Point

First, let's look at the extension point defined in the core microservice:

```java
// In the core-banking-accounts microservice
package com.catalis.banking.accounts.provider;

import com.catalis.banking.accounts.model.Account;
import com.catalis.core.plugin.annotation.ExtensionPoint;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@ExtensionPoint(
    id = "com.catalis.banking.accounts.account-provider",
    description = "Extension point for external account providers",
    allowMultiple = true
)
public interface AccountProvider {

    /**
     * Checks if this provider supports the given account type.
     *
     * @param accountType the account type to check
     * @return true if this provider supports the account type
     */
    boolean supportsAccountType(String accountType);

    /**
     * Creates a new account with the external provider.
     *
     * @param account the account details
     * @return a Mono that emits the created account
     */
    Mono<Account> createAccount(Account account);

    /**
     * Retrieves an account from the external provider.
     *
     * @param accountId the account ID
     * @return a Mono that emits the account
     */
    Mono<Account> getAccount(String accountId);

    /**
     * Updates an account with the external provider.
     *
     * @param account the account to update
     * @return a Mono that emits the updated account
     */
    Mono<Account> updateAccount(Account account);

    /**
     * Gets the current balance of an account.
     *
     * @param accountId the account ID
     * @return a Mono that emits the account balance
     */
    Mono<BigDecimal> getAccountBalance(String accountId);

    /**
     * Gets all accounts for a customer.
     *
     * @param customerId the customer ID
     * @return a Flux that emits the customer's accounts
     */
    Flux<Account> getCustomerAccounts(String customerId);

    /**
     * Gets the provider name.
     *
     * @return the provider name
     */
    String getProviderName();

    /**
     * Gets the priority of this provider.
     * Higher priority providers are tried first.
     *
     * @return the priority value
     */
    int getPriority();
}
```

### Step 2: Create the Plugin Class

Now, let's create the main plugin class:

```java
package com.example.treezor;

import com.catalis.core.plugin.annotation.Plugin;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.spi.AbstractPlugin;
import com.example.treezor.service.TreezorApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
    id = "com.example.treezor.account-provider",
    name = "Treezor Account Provider",
    version = "1.0.0",
    description = "Integrates with Treezor API for wallet functionality",
    author = "Example Inc."
)
public class TreezorAccountProviderPlugin extends AbstractPlugin {

    private static final Logger logger = LoggerFactory.getLogger(TreezorAccountProviderPlugin.class);

    private final PluginEventBus eventBus;
    private final Map<String, Object> configuration;
    private TreezorApiClient apiClient;

    public TreezorAccountProviderPlugin(PluginEventBus eventBus) {
        super(PluginMetadata.builder()
                .id("com.example.treezor.account-provider")
                .name("Treezor Account Provider")
                .version("1.0.0")
                .description("Integrates with Treezor API for wallet functionality")
                .author("Example Inc.")
                .minPlatformVersion("1.0.0")
                .dependencies(Set.of())
                .installTime(Instant.now())
                .build());

        this.eventBus = eventBus;
        this.configuration = new ConcurrentHashMap<>();

        // Default configuration
        configuration.put("apiUrl", "https://sandbox.treezor.com/v1/");
        configuration.put("clientId", "default-client-id");
        configuration.put("clientSecret", "default-client-secret");
        configuration.put("timeout", 30);
    }

    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing Treezor Account Provider Plugin");

        // Create API client with configuration
        apiClient = new TreezorApiClient(
                (String) configuration.get("apiUrl"),
                (String) configuration.get("clientId"),
                (String) configuration.get("clientSecret"),
                (Integer) configuration.get("timeout"));

        return apiClient.initialize();
    }

    @Override
    public Mono<Void> updateConfiguration(Map<String, Object> newConfig) {
        logger.info("Updating configuration: {}", newConfig);

        // Update configuration
        configuration.putAll(newConfig);

        // Update API client if it exists
        if (apiClient != null) {
            apiClient.setApiUrl((String) configuration.get("apiUrl"));
            apiClient.setClientId((String) configuration.get("clientId"));
            apiClient.setClientSecret((String) configuration.get("clientSecret"));
            apiClient.setTimeout((Integer) configuration.get("timeout"));
        }

        return Mono.empty();
    }

    @Override
    public Mono<Void> stop() {
        logger.info("Stopping Treezor Account Provider Plugin");
        return apiClient.shutdown();
    }
}
```

### Step 3: Implement the Extension

Next, let's implement the `AccountProvider` extension point:

```java
package com.example.treezor.extension;

import com.catalis.banking.accounts.model.Account;
import com.catalis.banking.accounts.provider.AccountProvider;
import com.catalis.core.plugin.annotation.Extension;
import com.example.treezor.service.TreezorApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Extension(
    extensionPointId = "com.catalis.banking.accounts.account-provider",
    priority = 100,
    description = "Treezor wallet integration for account management"
)
public class TreezorAccountProvider implements AccountProvider {

    private static final Logger logger = LoggerFactory.getLogger(TreezorAccountProvider.class);

    private static final String PROVIDER_NAME = "Treezor";
    private static final String SUPPORTED_ACCOUNT_TYPE = "WALLET";

    private final TreezorApiClient apiClient;

    public TreezorAccountProvider(TreezorApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public boolean supportsAccountType(String accountType) {
        return SUPPORTED_ACCOUNT_TYPE.equalsIgnoreCase(accountType);
    }

    @Override
    public Mono<Account> createAccount(Account account) {
        logger.info("Creating Treezor wallet for account: {}", account.getId());

        // Validate account
        if (!supportsAccountType(account.getType())) {
            return Mono.error(new UnsupportedOperationException(
                    "Unsupported account type: " + account.getType()));
        }

        // Map Firefly account to Treezor wallet
        Map<String, Object> walletRequest = mapAccountToWalletRequest(account);

        // Call Treezor API to create wallet
        return apiClient.createWallet(walletRequest)
                .map(this::mapWalletResponseToAccount);
    }

    @Override
    public Mono<Account> getAccount(String accountId) {
        logger.info("Retrieving Treezor wallet for account: {}", accountId);

        // Call Treezor API to get wallet
        return apiClient.getWallet(accountId)
                .map(this::mapWalletResponseToAccount);
    }

    @Override
    public Mono<Account> updateAccount(Account account) {
        logger.info("Updating Treezor wallet for account: {}", account.getId());

        // Validate account
        if (!supportsAccountType(account.getType())) {
            return Mono.error(new UnsupportedOperationException(
                    "Unsupported account type: " + account.getType()));
        }

        // Map Firefly account to Treezor wallet
        Map<String, Object> walletRequest = mapAccountToWalletRequest(account);

        // Call Treezor API to update wallet
        return apiClient.updateWallet(account.getId(), walletRequest)
                .map(this::mapWalletResponseToAccount);
    }

    @Override
    public Mono<BigDecimal> getAccountBalance(String accountId) {
        logger.info("Retrieving balance for Treezor wallet: {}", accountId);

        // Call Treezor API to get wallet balance
        return apiClient.getWalletBalance(accountId);
    }

    @Override
    public Flux<Account> getCustomerAccounts(String customerId) {
        logger.info("Retrieving Treezor wallets for customer: {}", customerId);

        // Call Treezor API to get customer wallets
        return apiClient.getCustomerWallets(customerId)
                .map(this::mapWalletResponseToAccount);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    private Map<String, Object> mapAccountToWalletRequest(Account account) {
        // In a real implementation, this would map Firefly account fields to Treezor wallet fields
        return Map.of(
                "userId", account.getCustomerId(),
                "walletTypeId", 9, // Standard wallet
                "currency", account.getCurrency(),
                "tag", account.getName(),
                "tariffId", 1,
                "metadata", account.getMetadata()
        );
    }

    private Account mapWalletResponseToAccount(Map<String, Object> walletResponse) {
        // In a real implementation, this would map Treezor wallet fields to Firefly account fields
        Account account = new Account();
        account.setId((String) walletResponse.get("id"));
        account.setCustomerId((String) walletResponse.get("userId"));
        account.setType(SUPPORTED_ACCOUNT_TYPE);
        account.setName((String) walletResponse.get("tag"));
        account.setCurrency((String) walletResponse.get("currency"));
        account.setStatus(mapWalletStatusToAccountStatus((String) walletResponse.get("status")));
        account.setProvider(PROVIDER_NAME);
        account.setProviderId((String) walletResponse.get("id"));
        account.setMetadata((Map<String, Object>) walletResponse.get("metadata"));
        return account;
    }

    private String mapWalletStatusToAccountStatus(String walletStatus) {
        // Map Treezor wallet status to Firefly account status
        switch (walletStatus) {
            case "PENDING":
                return "PENDING";
            case "ACTIVE":
                return "ACTIVE";
            case "SUSPENDED":
                return "SUSPENDED";
            case "CLOSED":
                return "CLOSED";
            default:
                return "UNKNOWN";
        }
    }
}
```

### Step 4: Create the API Client

Now, let's create the service that interacts with the Treezor API:

```java
package com.example.treezor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TreezorApiClient {

    private static final Logger logger = LoggerFactory.getLogger(TreezorApiClient.class);

    private String apiUrl;
    private String clientId;
    private String clientSecret;
    private int timeout;

    public TreezorApiClient(String apiUrl, String clientId, String clientSecret, int timeout) {
        this.apiUrl = apiUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.timeout = timeout;
    }

    public Mono<Void> initialize() {
        logger.info("Initializing Treezor API client: apiUrl={}", apiUrl);

        // In a real implementation, this would set up the HTTP client and authenticate
        return Mono.delay(Duration.ofMillis(300)).then();
    }

    public Mono<Void> shutdown() {
        logger.info("Shutting down Treezor API client");

        // In a real implementation, this would clean up resources
        return Mono.empty();
    }

    public Mono<Map<String, Object>> createWallet(Map<String, Object> walletRequest) {
        logger.info("Creating Treezor wallet: {}", walletRequest);

        // In a real implementation, this would call the Treezor API
        return Mono.delay(Duration.ofMillis(500))
                .map(ignored -> {
                    // Simulate API response
                    Map<String, Object> response = new HashMap<>(walletRequest);
                    response.put("id", UUID.randomUUID().toString());
                    response.put("status", "ACTIVE");
                    response.put("createdDate", System.currentTimeMillis());
                    return response;
                });
    }

    public Mono<Map<String, Object>> getWallet(String walletId) {
        logger.info("Getting Treezor wallet: {}", walletId);

        // In a real implementation, this would call the Treezor API
        return Mono.delay(Duration.ofMillis(300))
                .map(ignored -> {
                    // Simulate API response
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", walletId);
                    response.put("userId", "user-" + walletId.substring(0, 8));
                    response.put("walletTypeId", 9);
                    response.put("currency", "EUR");
                    response.put("tag", "My Wallet");
                    response.put("status", "ACTIVE");
                    response.put("createdDate", System.currentTimeMillis() - 86400000);
                    response.put("metadata", Map.of("purpose", "shopping"));
                    return response;
                });
    }

    public Mono<Map<String, Object>> updateWallet(String walletId, Map<String, Object> walletRequest) {
        logger.info("Updating Treezor wallet: {}, {}", walletId, walletRequest);

        // In a real implementation, this would call the Treezor API
        return Mono.delay(Duration.ofMillis(400))
                .map(ignored -> {
                    // Simulate API response
                    Map<String, Object> response = new HashMap<>(walletRequest);
                    response.put("id", walletId);
                    response.put("status", "ACTIVE");
                    response.put("updatedDate", System.currentTimeMillis());
                    return response;
                });
    }

    public Mono<BigDecimal> getWalletBalance(String walletId) {
        logger.info("Getting balance for Treezor wallet: {}", walletId);

        // In a real implementation, this would call the Treezor API
        return Mono.delay(Duration.ofMillis(300))
                .map(ignored -> new BigDecimal("1250.75"));
    }

    public Flux<Map<String, Object>> getCustomerWallets(String customerId) {
        logger.info("Getting Treezor wallets for customer: {}", customerId);

        // In a real implementation, this would call the Treezor API
        return Flux.range(1, 3)
                .delayElements(Duration.ofMillis(100))
                .map(i -> {
                    // Simulate API response
                    String walletId = UUID.randomUUID().toString();
                    Map<String, Object> wallet = new HashMap<>();
                    wallet.put("id", walletId);
                    wallet.put("userId", customerId);
                    wallet.put("walletTypeId", 9);
                    wallet.put("currency", i == 1 ? "EUR" : (i == 2 ? "USD" : "GBP"));
                    wallet.put("tag", "Wallet " + i);
                    wallet.put("status", "ACTIVE");
                    wallet.put("createdDate", System.currentTimeMillis() - (i * 86400000));
                    wallet.put("metadata", Map.of("purpose", i == 1 ? "shopping" : (i == 2 ? "travel" : "savings")));
                    return wallet;
                });
    }

    // Getters and setters
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
```

### Step 5: Register the Extension

Finally, let's modify the plugin class to register the extension:

```java
@Override
public Mono<Void> initialize() {
    logger.info("Initializing Treezor Account Provider Plugin");

    // Create API client with configuration
    apiClient = new TreezorApiClient(
            (String) configuration.get("apiUrl"),
            (String) configuration.get("clientId"),
            (String) configuration.get("clientSecret"),
            (Integer) configuration.get("timeout"));

    // Create the extension
    TreezorAccountProvider accountProvider = new TreezorAccountProvider(apiClient);

    // Register the extension
    return getPluginManager().getExtensionRegistry()
            .registerExtension(new ExtensionImpl(
                    "com.catalis.banking.accounts.account-provider",
                    accountProvider,
                    100))
            .then(apiClient.initialize());
}
```

### Step 6: Using the Plugin

Here's how a microservice would use this plugin:

```java
@Service
public class AccountService {

    private final PluginManager pluginManager;

    public AccountService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public Mono<Account> createAccount(Account account) {
        return pluginManager.getExtensionRegistry()
                .getExtensions("com.catalis.banking.accounts.account-provider")
                .cast(AccountProvider.class)
                .filter(provider -> provider.supportsAccountType(account.getType()))
                .sort((p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority()))
                .next()
                .switchIfEmpty(Mono.error(new UnsupportedOperationException(
                        "No account provider found for type: " + account.getType())))
                .flatMap(provider -> provider.createAccount(account));
    }

    public Mono<Account> getAccount(String accountId) {
        // First, get the account from the database to determine its type and provider
        return getAccountFromDatabase(accountId)
                .flatMap(account -> {
                    // Then, use the appropriate provider to get the latest account details
                    return pluginManager.getExtensionRegistry()
                            .getExtensions("com.catalis.banking.accounts.account-provider")
                            .cast(AccountProvider.class)
                            .filter(provider -> provider.getProviderName().equals(account.getProvider()))
                            .next()
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Provider not found: " + account.getProvider())))
                            .flatMap(provider -> provider.getAccount(accountId));
                });
    }

    public Mono<BigDecimal> getAccountBalance(String accountId) {
        // First, get the account from the database to determine its provider
        return getAccountFromDatabase(accountId)
                .flatMap(account -> {
                    // Then, use the appropriate provider to get the balance
                    return pluginManager.getExtensionRegistry()
                            .getExtensions("com.catalis.banking.accounts.account-provider")
                            .cast(AccountProvider.class)
                            .filter(provider -> provider.getProviderName().equals(account.getProvider()))
                            .next()
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Provider not found: " + account.getProvider())))
                            .flatMap(provider -> provider.getAccountBalance(accountId));
                });
    }

    private Mono<Account> getAccountFromDatabase(String accountId) {
        // In a real implementation, this would retrieve the account from the database
        // For demonstration, we'll create a dummy account
        Account account = new Account();
        account.setId(accountId);
        account.setType("WALLET");
        account.setProvider("Treezor");
        return Mono.just(account);
    }
}
```

### Key Points

- The plugin integrates with an external API (Treezor) for wallet functionality
- The extension implements the `AccountProvider` interface to provide account operations
- The plugin maps between the Firefly account model and the Treezor wallet model
- The extension supports specific account types (WALLET)
- The microservice can use multiple account providers based on account type
