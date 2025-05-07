# Firefly Plugin Manager Documentation

Welcome to the Firefly Plugin Manager documentation. This guide provides a comprehensive, step-by-step approach to understanding and using the plugin system.

## Table of Contents

1. [Introduction](./01-introduction.md)
   - What is the Firefly Plugin Manager?
   - Key Features and Benefits
   - Technical Foundation

2. [Core Concepts](./02-core-concepts.md)
   - Plugins
   - Extension Points
   - Extensions
   - Plugin Lifecycle

3. [Architecture Overview](./03-architecture.md)
   - Plugin Registry
   - Extension Registry
   - Event Bus (In-memory and Kafka)
   - Plugin Loader
   - Utility Classes

4. [Microservice vs Plugin Responsibilities](./04-microservice-plugin-responsibilities.md)
   - What Logic Belongs in Core Microservices
   - What Logic Belongs in Plugins
   - Integration Points
   - Best Practices

5. [Getting Started](./05-getting-started.md)
   - Prerequisites
   - Installation
   - Basic Usage
   - Configuration Options

6. [Creating Extension Points in Microservices](./06-creating-extension-points.md)
   - Designing Effective Extension Points
   - Implementing Extension Points
   - Testing Extension Points
   - Extension Point Best Practices

7. [Developing Plugins](./07-developing-plugins.md)
   - Plugin Structure
   - Implementing Extensions
   - Plugin Configuration
   - Event-Based Communication
   - Packaging Plugins

8. [Plugin Installation Methods](./08-plugin-installation.md)
   - JAR File Installation
   - Git Repository Installation
   - Classpath Auto-detection
   - Installation Configuration

9. [Advanced Features](./09-advanced-features.md)
   - Hot Deployment
   - Health Monitoring
   - Plugin Debugging
   - Security Considerations

10. [Examples](./10-examples.md)
    - Credit Card Payment Plugin
    - Machine Learning Fraud Detector
    - Company Enrichment Information
    - Treezor Integration for Core Banking Accounts

11. [Testing](./11-testing.md)
    - Testing Extension Points
    - Testing Plugins
    - Integration Testing
    - Test Utilities

12. [Troubleshooting](./12-troubleshooting.md)
    - Common Issues
    - Debugging Techniques
    - Logging and Monitoring
    - Getting Help

Each section provides detailed information with code examples, diagrams, and best practices to help you effectively use the Firefly Plugin Manager.
