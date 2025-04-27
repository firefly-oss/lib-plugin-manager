# Company Enrichment Information Plugin Example

This example demonstrates how to create a plugin that implements a company information enrichment extension point using the eInforma Developer API. The Company Enrichment Plugin retrieves and enriches customer information with detailed company data from Spain's largest business database.

## Overview

The Company Enrichment Information Plugin:
- Implements a `CompanyEnricher` extension point
- Connects to the eInforma Developer API to retrieve company information
- Enriches customer records with detailed company data
- Supports both synchronous and asynchronous data enrichment
- Caches results to minimize API calls

## Prerequisites

- Java 21
- Spring Boot 3.2.2 or higher
- Firefly Plugin Manager library
- eInforma Developer API credentials (username and authentication key)

## Step 1: Define the Extension Point

First, define the extension point interface that our plugin will implement:

```java
package com.example.enrichment;

import com.catalis.core.plugin.annotation.ExtensionPoint;
import reactor.core.publisher.Mono;

import java.util.Map;

@ExtensionPoint(
        id = "com.example.enrichment.company",
        description = "Extension point for company information enrichment services",
        allowMultiple = true
)
public interface CompanyEnricher {
    
    /**
     * Gets the name of this company enricher.
     * 
     * @return the enricher name
     */
    String getEnricherName();
    
    /**
     * Gets the country code this enricher supports.
     * 
     * @return the ISO country code
     */
    String getSupportedCountry();
    
    /**
     * Checks if this enricher supports the given company identifier type.
     * 
     * @param identifierType the type of identifier (e.g., "CIF", "NIF", "VAT")
     * @return true if supported, false otherwise
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
     * Higher values indicate higher priority when multiple implementations exist.
     * 
     * @return the priority value
     */
    default int getPriority() {
        return 0;
    }
}
```

## Step 2: Create a Client for the eInforma API

Next, create a client class to interact with the eInforma Developer API:

```java
package com.example.enrichment.einforma;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

/**
 * Client for interacting with the eInforma Developer API.
 */
public class EInformaApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(EInformaApiClient.class);
    private static final String API_BASE_URL = "https://api.einforma.com/v1";
    
    private final WebClient webClient;
    private final String credentials;
    
    /**
     * Creates a new eInforma API client.
     * 
     * @param username the API username
     * @param apiKey the API authentication key
     */
    public EInformaApiClient(String username, String apiKey) {
        this.credentials = Base64.getEncoder().encodeToString((username + ":" + apiKey).getBytes());
        
        this.webClient = WebClient.builder()
                .baseUrl(API_BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    /**
     * Gets company information by CIF (Spanish company tax ID).
     * 
     * @param cif the company CIF
     * @return a Mono that emits the company data
     */
    public Mono<Map<String, Object>> getCompanyByCif(String cif) {
        logger.info("Fetching company information for CIF: {}", cif);
        
        return webClient.get()
                .uri("/empresas/{cif}", cif)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(data -> logger.info("Successfully retrieved company data for CIF: {}", cif))
                .doOnError(error -> logger.error("Error retrieving company data for CIF: {}", cif, error));
    }
    
    /**
     * Searches for companies by name.
     * 
     * @param name the company name to search for
     * @param maxResults the maximum number of results to return
     * @return a Mono that emits the search results
     */
    public Mono<Map<String, Object>> searchCompaniesByName(String name, int maxResults) {
        logger.info("Searching for companies with name: {}", name);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/empresas/buscar")
                        .queryParam("nombre", name)
                        .queryParam("limite", maxResults)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(data -> logger.info("Successfully searched for companies with name: {}", name))
                .doOnError(error -> logger.error("Error searching for companies with name: {}", name, error));
    }
}
```

## Step 3: Implement the Plugin

Now, create the plugin class that implements the extension point:

```java
package com.example.enrichment.plugin;

import com.catalis.core.plugin.annotation.Extension;
import com.catalis.core.plugin.annotation.Plugin;
import com.catalis.core.plugin.event.PluginEventBus;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.spi.AbstractPlugin;
import com.example.enrichment.CompanyEnricher;
import com.example.enrichment.einforma.EInformaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin that implements the CompanyEnricher extension point using the eInforma Developer API.
 */
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
    private final EInformaCompanyEnricher companyEnricher;
    
    /**
     * Creates a new EInformaCompanyEnrichmentPlugin.
     *
     * @param eventBus the event bus for inter-plugin communication
     */
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
        this.companyEnricher = new EInformaCompanyEnricher();
        
        // Default configuration
        configuration.put("username", "");
        configuration.put("apiKey", "");
        configuration.put("cacheExpirationMinutes", 60);
        configuration.put("maxSearchResults", 10);
    }
    
    @Override
    public Mono<Void> initialize() {
        logger.info("Initializing eInforma Company Enrichment Plugin");
        
        return Mono.fromRunnable(() -> {
            String username = (String) configuration.get("username");
            String apiKey = (String) configuration.get("apiKey");
            
            if (username == null || username.isEmpty() || apiKey == null || apiKey.isEmpty()) {
                logger.warn("eInforma API credentials not configured. Plugin will not be functional until configured.");
            } else {
                apiClient = new EInformaApiClient(username, apiKey);
                logger.info("eInforma API client initialized");
            }
        });
    }
    
    @Override
    public Mono<Void> start() {
        logger.info("Starting eInforma Company Enrichment Plugin");
        
        // Subscribe to configuration update events
        eventBus.subscribe("plugin.config.update")
                .filter(event -> event.getMetadata().get("pluginId").equals(getMetadata().id()))
                .subscribe(event -> {
                    Map<String, Object> newConfig = (Map<String, Object>) event.getData();
                    updateConfiguration(newConfig);
                });
        
        return Mono.empty();
    }
    
    @Override
    public Mono<Void> stop() {
        logger.info("Stopping eInforma Company Enrichment Plugin");
        return Mono.empty();
    }
    
    /**
     * Updates the plugin configuration.
     * 
     * @param newConfig the new configuration
     */
    private void updateConfiguration(Map<String, Object> newConfig) {
        logger.info("Updating eInforma plugin configuration");
        
        if (newConfig.containsKey("username") && newConfig.containsKey("apiKey")) {
            String username = (String) newConfig.get("username");
            String apiKey = (String) newConfig.get("apiKey");
            
            if (username != null && !username.isEmpty() && apiKey != null && !apiKey.isEmpty()) {
                configuration.put("username", username);
                configuration.put("apiKey", apiKey);
                apiClient = new EInformaApiClient(username, apiKey);
                logger.info("eInforma API client reconfigured");
            }
        }
        
        if (newConfig.containsKey("cacheExpirationMinutes")) {
            configuration.put("cacheExpirationMinutes", newConfig.get("cacheExpirationMinutes"));
        }
        
        if (newConfig.containsKey("maxSearchResults")) {
            configuration.put("maxSearchResults", newConfig.get("maxSearchResults"));
        }
    }
    
    /**
     * Gets the company enricher implementation.
     * 
     * @return the company enricher
     */
    public CompanyEnricher getCompanyEnricher() {
        return companyEnricher;
    }
    
    /**
     * Implementation of the CompanyEnricher extension point.
     */
    @Extension(
            extensionPointId = "com.example.enrichment.company",
            priority = 100,
            description = "Company enrichment using eInforma Developer API"
    )
    public class EInformaCompanyEnricher implements CompanyEnricher {
        
        @Override
        public String getEnricherName() {
            return "eInforma";
        }
        
        @Override
        public String getSupportedCountry() {
            return "ES"; // Spain
        }
        
        @Override
        public boolean supportsIdentifierType(String identifierType) {
            return "CIF".equalsIgnoreCase(identifierType) || 
                   "NIF".equalsIgnoreCase(identifierType);
        }
        
        @Override
        public Mono<Map<String, Object>> enrichCompanyData(String identifierType, String identifierValue) {
            if (apiClient == null) {
                return Mono.error(new IllegalStateException("eInforma API client not initialized. Check configuration."));
            }
            
            if (!supportsIdentifierType(identifierType)) {
                return Mono.error(new IllegalArgumentException("Unsupported identifier type: " + identifierType));
            }
            
            // Check cache first
            String cacheKey = identifierType + ":" + identifierValue;
            if (cache.containsKey(cacheKey)) {
                return Mono.just(cache.get(cacheKey));
            }
            
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
            if (apiClient == null) {
                return Mono.error(new IllegalStateException("eInforma API client not initialized. Check configuration."));
            }
            
            int configuredMaxResults = (int) configuration.getOrDefault("maxSearchResults", 10);
            int effectiveMaxResults = Math.min(maxResults, configuredMaxResults);
            
            return apiClient.searchCompaniesByName(companyName, effectiveMaxResults);
        }
        
        @Override
        public int getPriority() {
            return 100;
        }
    }
}
```

## Step 4: Register the Plugin

There are several ways to register the plugin:

### Option 1: Using Service Provider Interface (SPI)

Create a file at `META-INF/services/com.catalis.core.plugin.api.Plugin` with the fully qualified name of your plugin class:

```
com.example.enrichment.plugin.EInformaCompanyEnrichmentPlugin
```

### Option 2: Programmatic Registration

```java
// Create the plugin instance
PluginEventBus eventBus = pluginManager.getEventBus();
EInformaCompanyEnrichmentPlugin plugin = new EInformaCompanyEnrichmentPlugin(eventBus);

// Register the plugin
pluginManager.getPluginRegistry().registerPlugin(plugin).block();

// Register the extension
pluginManager.getExtensionRegistry()
        .registerExtension(
                "com.example.enrichment.company",
                plugin.getCompanyEnricher(),
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
                "com.example.enrichment.company",
                CompanyEnricher.class)
        .block();

// Scan for plugins in a specific package
pluginManager.installPluginsFromClasspath("com.example.enrichment").block();
```

## Step 5: Configure the Plugin

The plugin requires configuration to connect to the eInforma Developer API:

```java
// Configure the plugin
Map<String, Object> config = new HashMap<>();
config.put("username", "your-einforma-username");
config.put("apiKey", "your-einforma-api-key");
config.put("cacheExpirationMinutes", 60);
config.put("maxSearchResults", 10);

pluginManager.updatePluginConfiguration("com.example.enrichment.einforma", config).block();
```

## Step 6: Using the Plugin

Once the plugin is registered and configured, you can use it through the extension registry:

```java
// Get the company enricher extension
CompanyEnricher enricher = pluginManager.getExtensionRegistry()
        .getHighestPriorityExtension("com.example.enrichment.company")
        .block();

// Enrich company data using CIF
enricher.enrichCompanyData("CIF", "B12345678")
        .subscribe(companyData -> {
            System.out.println("Company Name: " + companyData.get("nombre"));
            System.out.println("Address: " + companyData.get("direccion"));
            System.out.println("City: " + companyData.get("localidad"));
            System.out.println("Province: " + companyData.get("provincia"));
            System.out.println("Postal Code: " + companyData.get("codigoPostal"));
            System.out.println("Phone: " + companyData.get("telefono"));
            System.out.println("Email: " + companyData.get("email"));
            System.out.println("Website: " + companyData.get("web"));
            System.out.println("Activity: " + companyData.get("actividad"));
            System.out.println("CNAE: " + companyData.get("cnae"));
            System.out.println("Legal Form: " + companyData.get("formaJuridica"));
            System.out.println("Foundation Date: " + companyData.get("fechaConstitucion"));
            System.out.println("Annual Revenue: " + companyData.get("facturacion"));
            System.out.println("Employees: " + companyData.get("empleados"));
        });

// Search for companies by name
enricher.searchCompaniesByName("Telefonica", 5)
        .subscribe(searchResults -> {
            List<Map<String, Object>> companies = (List<Map<String, Object>>) searchResults.get("empresas");
            companies.forEach(company -> {
                System.out.println("CIF: " + company.get("cif"));
                System.out.println("Name: " + company.get("nombre"));
                System.out.println("City: " + company.get("localidad"));
                System.out.println("Province: " + company.get("provincia"));
                System.out.println("-----------------------");
            });
        });
```

## Step 7: Advanced Features - Event-Based Updates

You can extend the plugin to publish events when company data is updated or when significant changes are detected:

```java
// Add to EInformaCompanyEnricher class

private void publishCompanyUpdateEvent(String cif, Map<String, Object> companyData) {
    Map<String, Object> eventData = new HashMap<>();
    eventData.put("cif", cif);
    eventData.put("companyData", companyData);
    eventData.put("timestamp", Instant.now());
    
    eventBus.publish("company.data.updated", eventData);
}
```

Other plugins can subscribe to these events to react to company data changes:

```java
// In another plugin
eventBus.subscribe("company.data.updated")
        .subscribe(event -> {
            Map<String, Object> eventData = (Map<String, Object>) event.getData();
            String cif = (String) eventData.get("cif");
            Map<String, Object> companyData = (Map<String, Object>) eventData.get("companyData");
            
            // Process the updated company data
            logger.info("Received company data update for CIF: {}", cif);
            // Update local records, trigger workflows, etc.
        });
```

## Conclusion

This example demonstrates how to create a Company Enrichment Plugin that implements the CompanyEnricher extension point using the eInforma Developer API. The plugin provides a way to enrich customer information with detailed company data from Spain's largest business database.

Key points:
- The plugin extends `AbstractPlugin` and is annotated with `@Plugin`
- The extension implementation is annotated with `@Extension`
- The plugin uses the WebClient to interact with the eInforma API
- The plugin implements caching to minimize API calls
- The plugin can be configured with API credentials
- The plugin uses the event bus for communication
- The plugin can be registered and used through the plugin manager

By following this pattern, you can create your own company enrichment plugins that integrate with different data providers or support different countries.
