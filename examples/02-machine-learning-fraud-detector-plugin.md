# Machine Learning Fraud Detector Plugin Example

This example demonstrates how to create a plugin that implements a fraud detection extension point using machine learning techniques. The ML Fraud Detector Plugin analyzes transactions to identify potentially fraudulent activities.

## Overview

The Machine Learning Fraud Detector Plugin:
- Implements a `FraudDetector` extension point
- Uses a pre-trained machine learning model to detect fraud
- Provides real-time fraud risk scoring for transactions
- Supports both synchronous and asynchronous fraud detection

## Prerequisites

- Java 21
- Spring Boot 3.2.2 or higher
- Firefly Plugin Manager library
- Basic understanding of machine learning concepts

## Step 1: Define the Extension Point

First, define the extension point interface that our plugin will implement:

```java
package com.example.fraud;

import com.firefly.core.plugin.annotation.ExtensionPoint;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@ExtensionPoint(
        id = "com.example.fraud.detector",
        description = "Extension point for fraud detection services",
        allowMultiple = true
)
public interface FraudDetector {
    
    /**
     * Gets the name of this fraud detector.
     * 
     * @return the detector name
     */
    String getDetectorName();
    
    /**
     * Analyzes a transaction for potential fraud.
     * 
     * @param transactionId the transaction ID
     * @param amount the transaction amount
     * @param currency the transaction currency
     * @param customerId the customer ID
     * @param metadata additional transaction metadata
     * @return a Mono that emits a fraud detection result
     */
    Mono<FraudDetectionResult> analyzeTransaction(
            String transactionId,
            BigDecimal amount,
            String currency,
            String customerId,
            Map<String, Object> metadata);
    
    /**
     * Gets the confidence threshold for this detector.
     * Transactions with a fraud score above this threshold are considered fraudulent.
     * 
     * @return the confidence threshold (0.0 to 1.0)
     */
    double getConfidenceThreshold();
    
    /**
     * Represents the result of a fraud detection analysis.
     */
    class FraudDetectionResult {
        private final String transactionId;
        private final boolean fraudulent;
        private final double fraudScore;
        private final String reason;
        
        public FraudDetectionResult(String transactionId, boolean fraudulent, double fraudScore, String reason) {
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
        
        @Override
        public String toString() {
            return "FraudDetectionResult{" +
                    "transactionId='" + transactionId + '\'' +
                    ", fraudulent=" + fraudulent +
                    ", fraudScore=" + fraudScore +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }
}
```

## Step 2: Create a Machine Learning Model Service

Create a service that will handle the machine learning model operations:

```java
package com.example.fraud.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Random;

/**
 * Service that handles machine learning model operations for fraud detection.
 * This is a simplified example that simulates a machine learning model.
 */
public class MachineLearningModelService {
    
    private static final Logger logger = LoggerFactory.getLogger(MachineLearningModelService.class);
    private final Random random = new Random();
    
    // Simulated model weights for different factors
    private final double amountWeight = 0.4;
    private final double customerHistoryWeight = 0.3;
    private final double locationWeight = 0.2;
    private final double timeOfDayWeight = 0.1;
    
    /**
     * Initializes the machine learning model.
     * In a real implementation, this would load the model from a file or service.
     * 
     * @return a Mono that completes when the model is initialized
     */
    public Mono<Void> initializeModel() {
        logger.info("Initializing fraud detection machine learning model");
        
        // Simulate model loading time
        return Mono.delay(Duration.ofSeconds(2)).then();
    }
    
    /**
     * Predicts the fraud score for a transaction.
     * 
     * @param amount the transaction amount
     * @param customerId the customer ID
     * @param metadata additional transaction metadata
     * @return a Mono that emits the fraud score (0.0 to 1.0)
     */
    public Mono<Double> predictFraudScore(
            BigDecimal amount,
            String customerId,
            Map<String, Object> metadata) {
        
        logger.info("Predicting fraud score for transaction: customer={}, amount={}",
                customerId, amount);
        
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
            
            // Add some randomness for demonstration
            fraudScore += (random.nextDouble() * 0.1) - 0.05;
            
            // Ensure score is between 0 and 1
            fraudScore = Math.max(0.0, Math.min(1.0, fraudScore));
            
            logger.info("Predicted fraud score: {}", fraudScore);
            return fraudScore;
        });
    }
    
    /**
     * Calculates the fraud factor based on transaction amount.
     * 
     * @param amount the transaction amount
     * @return the amount factor (0.0 to 1.0)
     */
    private double calculateAmountFactor(BigDecimal amount) {
        // Simple logic: higher amounts have higher risk
        double value = amount.doubleValue();
        if (value > 1000) return 0.8;
        if (value > 500) return 0.6;
        if (value > 200) return 0.4;
        if (value > 100) return 0.2;
        return 0.1;
    }
    
    /**
     * Calculates the fraud factor based on customer history.
     * 
     * @param customerId the customer ID
     * @return the customer factor (0.0 to 1.0)
     */
    private double calculateCustomerFactor(String customerId) {
        // In a real implementation, this would check customer history
        // For demonstration, we'll use a hash of the customer ID
        return (Math.abs(customerId.hashCode()) % 100) / 100.0;
    }
    
    /**
     * Calculates the fraud factor based on location.
     * 
     * @param metadata the transaction metadata
     * @return the location factor (0.0 to 1.0)
     */
    private double calculateLocationFactor(Map<String, Object> metadata) {
        // Check if location is in high-risk areas
        String location = (String) metadata.getOrDefault("location", "");
        if (location.isEmpty()) return 0.5;
        
        // Example high-risk locations
        if (location.contains("Anonymous Proxy") || 
            location.contains("Satellite Provider")) {
            return 0.9;
        }
        
        return 0.3;
    }
    
    /**
     * Calculates the fraud factor based on time of day.
     * 
     * @param metadata the transaction metadata
     * @return the time factor (0.0 to 1.0)
     */
    private double calculateTimeFactor(Map<String, Object> metadata) {
        // Check if transaction is during suspicious hours
        Integer hour = (Integer) metadata.getOrDefault("hour", 12);
        
        // Late night transactions are higher risk
        if (hour >= 0 && hour < 5) {
            return 0.7;
        }
        
        return 0.2;
    }
}
```

## Step 3: Implement the Plugin

Now, create the plugin class that implements the extension point:

```java
package com.example.fraud.plugin;

import com.firefly.core.plugin.annotation.Extension;
import com.firefly.core.plugin.annotation.Plugin;
import com.firefly.core.plugin.event.PluginEventBus;
import com.firefly.core.plugin.model.PluginMetadata;
import com.firefly.core.plugin.spi.AbstractPlugin;
import com.example.fraud.FraudDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Plugin that implements the FraudDetector extension point using machine learning.
 */
@Plugin(
        id = "com.example.fraud.ml-detector",
        name = "Machine Learning Fraud Detector",
        version = "1.0.0",
        description = "Detects fraudulent transactions using machine learning algorithms",
        author = "Example Inc."
)
public class MachineLearningFraudDetectorPlugin extends AbstractPlugin {
    
    private static final Logger logger = LoggerFactory.getLogger(MachineLearningFraudDetectorPlugin.class);
    
    private final PluginEventBus eventBus;
    private final MachineLearningModelService modelService;
    private final MLFraudDetector fraudDetector;
    
    /**
     * Creates a new MachineLearningFraudDetectorPlugin.
     *
     * @param eventBus the event bus for inter-plugin communication
     */
    public MachineLearningFraudDetectorPlugin(PluginEventBus eventBus) {
        super(PluginMetadata.builder()
                .id("com.example.fraud.ml-detector")
                .name("Machine Learning Fraud Detector")
                .version("1.0.0")
                .description("Detects fraudulent transactions using machine learning algorithms")
                .author("Example Inc.")
                .minPlatformVersion("1.0.0")
                .dependencies(Set.of())
                .installTime(Instant.now())
                .build());

        this.eventBus = eventBus;
        this.modelService = new MachineLearningModelService();
        this.fraudDetector = new MLFraudDetector();
    }
    
    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing Machine Learning Fraud Detector Plugin");
        return modelService.initializeModel();
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
    
    /**
     * Gets the fraud detector implementation.
     * 
     * @return the fraud detector
     */
    public FraudDetector getFraudDetector() {
        return fraudDetector;
    }
    
    /**
     * Implementation of the FraudDetector extension point using machine learning.
     */
    @Extension(
            extensionPointId = "com.example.fraud.detector",
            priority = 100,
            description = "Machine learning based fraud detector"
    )
    public class MLFraudDetector implements FraudDetector {
        
        private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.7;
        
        @Override
        public String getDetectorName() {
            return "ML Fraud Detector";
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
            return DEFAULT_CONFIDENCE_THRESHOLD;
        }
    }
}
```

## Step 4: Register the Plugin

There are several ways to register the plugin:

### Option 1: Using Service Provider Interface (SPI)

Create a file at `META-INF/services/com.firefly.core.plugin.api.Plugin` with the fully qualified name of your plugin class:

```
com.example.fraud.plugin.MachineLearningFraudDetectorPlugin
```

### Option 2: Programmatic Registration

```java
// Create the plugin instance
PluginEventBus eventBus = pluginManager.getEventBus();
MachineLearningFraudDetectorPlugin plugin = new MachineLearningFraudDetectorPlugin(eventBus);

// Register the plugin
pluginManager.getPluginRegistry().registerPlugin(plugin).block();

// Register the extension
pluginManager.getExtensionRegistry()
        .registerExtension(
                "com.example.fraud.detector",
                plugin.getFraudDetector(),
                100)
        .block();

// Initialize and start the plugin
plugin.initialize().block();
pluginManager.startPlugin(plugin.getMetadata().id()).block();
```

### Option 3: Using Classpath Scanning

If your plugin is annotated with `@Plugin`, you can use classpath scanning:

```java
// Register the extension point first
pluginManager.getExtensionRegistry()
        .registerExtensionPoint(
                "com.example.fraud.detector",
                FraudDetector.class)
        .block();

// Scan for plugins in a specific package
pluginManager.installPluginsFromClasspath("com.example.fraud").block();
```

## Step 5: Using the Plugin

Once the plugin is registered, you can use it through the extension registry:

```java
// Get the fraud detector extension
FraudDetector fraudDetector = pluginManager.getExtensionRegistry()
        .getHighestPriorityExtension("com.example.fraud.detector")
        .block();

if (fraudDetector != null) {
    // Create transaction metadata
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("location", "New York, USA");
    metadata.put("hour", 14); // 2 PM
    metadata.put("device", "mobile");
    metadata.put("ip_address", "192.168.1.1");
    
    // Analyze a transaction
    FraudDetector.FraudDetectionResult result = fraudDetector.analyzeTransaction(
            "TX-12345",
            BigDecimal.valueOf(999.99),
            "USD",
            "CUST-6789",
            metadata)
            .block();
    
    System.out.println("Fraud analysis result: " + result);
    
    if (result.isFraudulent()) {
        System.out.println("WARNING: Potentially fraudulent transaction detected!");
        System.out.println("Fraud score: " + result.getFraudScore());
        System.out.println("Reason: " + result.getReason());
    } else {
        System.out.println("Transaction appears legitimate.");
        System.out.println("Fraud score: " + result.getFraudScore());
    }
} else {
    System.err.println("No fraud detector found");
}
```

## Step 6: Testing the Plugin

Create a test class to verify the plugin's functionality:

```java
package com.example.fraud.plugin;

import com.firefly.core.plugin.event.DefaultPluginEventBus;
import com.firefly.core.plugin.event.PluginEventBus;
import com.example.fraud.FraudDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MachineLearningFraudDetectorPluginTest {
    
    private MachineLearningFraudDetectorPlugin plugin;
    private FraudDetector fraudDetector;
    
    @BeforeEach
    void setUp() {
        PluginEventBus eventBus = new DefaultPluginEventBus();
        plugin = new MachineLearningFraudDetectorPlugin(eventBus);
        
        // Initialize the plugin
        plugin.initialize().block();
        
        fraudDetector = plugin.getFraudDetector();
    }
    
    @Test
    void testGetDetectorName() {
        assertEquals("ML Fraud Detector", fraudDetector.getDetectorName());
    }
    
    @Test
    void testGetConfidenceThreshold() {
        assertEquals(0.7, fraudDetector.getConfidenceThreshold(), 0.001);
    }
    
    @Test
    void testAnalyzeTransactionLowRisk() {
        // Create low-risk transaction
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", "New York, USA");
        metadata.put("hour", 14); // 2 PM
        metadata.put("device", "mobile");
        
        StepVerifier.create(fraudDetector.analyzeTransaction(
                        "TX-LOW-RISK",
                        BigDecimal.valueOf(50.00),
                        "USD",
                        "CUST-GOOD-HISTORY",
                        metadata))
                .assertNext(result -> {
                    assertFalse(result.isFraudulent());
                    assertTrue(result.getFraudScore() < fraudDetector.getConfidenceThreshold());
                    assertEquals("TX-LOW-RISK", result.getTransactionId());
                    assertNotNull(result.getReason());
                })
                .verifyComplete();
    }
    
    @Test
    void testAnalyzeTransactionHighRisk() {
        // Create high-risk transaction
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("location", "Anonymous Proxy");
        metadata.put("hour", 3); // 3 AM
        metadata.put("device", "unknown");
        
        StepVerifier.create(fraudDetector.analyzeTransaction(
                        "TX-HIGH-RISK",
                        BigDecimal.valueOf(9999.99),
                        "USD",
                        "CUST-NEW",
                        metadata))
                .assertNext(result -> {
                    // Note: Due to randomness in our demo model, this might occasionally fail
                    // In a real implementation with a deterministic model, this would be reliable
                    assertTrue(result.getFraudScore() > 0.5);
                    assertEquals("TX-HIGH-RISK", result.getTransactionId());
                    assertNotNull(result.getReason());
                })
                .verifyComplete();
    }
}
```

## Step 7: Advanced Features - Model Retraining

In a real-world scenario, you might want to periodically retrain your machine learning model. Here's how you could extend the plugin to support this:

```java
// Add to MachineLearningFraudDetectorPlugin class

private Disposable modelUpdateSubscription;

@Override
public Mono<Void> start() {
    logger.info("Starting Machine Learning Fraud Detector Plugin");
    
    // Subscribe to model update events
    modelUpdateSubscription = eventBus.subscribe(ModelUpdateEvent.class)
            .filter(event -> event.getModelType().equals("fraud-detection"))
            .flatMap(event -> {
                logger.info("Received model update event: {}", event);
                return updateModel(event.getModelVersion());
            })
            .subscribe();
    
    return Mono.empty();
}

@Override
public Mono<Void> stop() {
    logger.info("Stopping Machine Learning Fraud Detector Plugin");
    
    // Dispose of the subscription
    if (modelUpdateSubscription != null && !modelUpdateSubscription.isDisposed()) {
        modelUpdateSubscription.dispose();
    }
    
    return Mono.empty();
}

/**
 * Updates the machine learning model.
 * 
 * @param modelVersion the new model version
 * @return a Mono that completes when the model has been updated
 */
private Mono<Void> updateModel(String modelVersion) {
    logger.info("Updating fraud detection model to version: {}", modelVersion);
    
    // In a real implementation, this would download and load the new model
    return modelService.initializeModel();
}

/**
 * Event class for model updates.
 */
public static class ModelUpdateEvent extends PluginEvent {
    private final String modelType;
    private final String modelVersion;
    
    public ModelUpdateEvent(String pluginId, String modelType, String modelVersion) {
        super(pluginId, "MODEL_UPDATE");
        this.modelType = modelType;
        this.modelVersion = modelVersion;
    }
    
    public String getModelType() {
        return modelType;
    }
    
    public String getModelVersion() {
        return modelVersion;
    }
}
```

## Conclusion

This example demonstrates how to create a Machine Learning Fraud Detector Plugin that implements the FraudDetector extension point. The plugin uses a simulated machine learning model to analyze transactions and detect potential fraud.

Key points:
- The plugin extends `AbstractPlugin` and is annotated with `@Plugin`
- The extension implementation is annotated with `@Extension`
- The plugin uses a machine learning model service to make predictions
- The plugin can be registered and used through the plugin manager
- The plugin can be extended to support model updates through the event bus

By following this pattern, you can create your own fraud detection plugins that integrate with different machine learning frameworks or implement different fraud detection algorithms.
