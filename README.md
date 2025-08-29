# Firefly Plugin Manager

A flexible, reactive plugin system for extending the Firefly Core Banking Platform with custom functionality.

## Overview

The Firefly Plugin Manager is a comprehensive framework that enables modular extension of the Firefly Core Banking Platform through a flexible plugin architecture. Built on modern technologies and reactive programming principles, it allows financial institutions to customize and extend platform functionality without modifying the core codebase.

### Key Features

- **Modular Architecture**: Cleanly separate core functionality from extensions
- **Reactive Design**: Built on Spring WebFlux and Project Reactor for non-blocking operations
- **Dynamic Loading**: Install, update, and uninstall plugins at runtime without system restarts
- **Secure Execution**: Run plugins in isolated environments with configurable security boundaries
- **Event-Driven Communication**: Plugins communicate through a reactive event bus (in-memory or Kafka)
- **Dependency Management**: Automatic resolution of plugin dependencies
- **Multiple Installation Methods**: Install plugins from JAR files, Git repositories, or classpath
- **Health Monitoring**: Track plugin health and resource usage
- **Debugging Support**: Debug plugins at runtime with breakpoints and variable inspection

### Technical Foundation

- **Java 21**: Leverages the latest Java features for improved performance and developer productivity
- **Spring Boot 3.2.2**: Built on the Spring ecosystem for enterprise-grade reliability
- **WebFlux**: Uses reactive programming for efficient resource utilization
- **Multi-Module Maven Structure**: Organized in a modular structure for better maintainability

## Documentation

For detailed documentation, please refer to the following guides:

1. [Introduction](docs/01-introduction.md) - Overview of the Firefly Plugin Manager
2. [Core Concepts](docs/02-core-concepts.md) - Fundamental building blocks of the plugin system
3. [Architecture Overview](docs/03-architecture.md) - Detailed explanation of the system components
4. [Microservice vs Plugin Responsibilities](docs/04-microservice-plugin-responsibilities.md) - Clear guidance on what belongs where
5. [Getting Started](docs/05-getting-started.md) - Prerequisites, installation, and basic usage
6. [Creating Extension Points](docs/06-creating-extension-points.md) - How to design and implement extension points
7. [Developing Plugins](docs/07-developing-plugins.md) - Step-by-step guide to creating plugins
8. [Plugin Installation Methods](docs/08-plugin-installation.md) - Different ways to install plugins
9. [Advanced Features](docs/09-advanced-features.md) - Hot deployment, health monitoring, and more
10. [Examples](docs/10-examples.md) - Real-world examples of plugins
11. [Testing](docs/11-testing.md) - How to test extension points, plugins, and their integration
12. [Troubleshooting](docs/12-troubleshooting.md) - Solutions for common issues

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Installation

Add the following dependency to your pom.xml:

```xml
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-plugin-manager-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

```java
// Initialize the Plugin Manager
@Autowired
private PluginManager pluginManager;

// Register an extension point
pluginManager.getExtensionRegistry()
        .registerExtensionPoint("com.example.extension-point", MyExtensionPoint.class)
        .block();

// Install a plugin
PluginDescriptor descriptor = pluginManager.installPlugin(Path.of("plugins/my-plugin.jar")).block();

// Start a plugin
pluginManager.startPlugin("com.example.my-plugin").block();

// Get extensions
Flux<MyExtensionPoint> extensions = pluginManager.getExtensionRegistry()
        .getExtensions("com.example.extension-point");
```

For more detailed examples and usage instructions, please refer to the [Getting Started](docs/05-getting-started.md) guide.

## Examples

The [Examples](docs/10-examples.md) section provides detailed examples of different types of plugins:

1. **Credit Card Payment Plugin**: Implements a payment processor extension point
2. **Machine Learning Fraud Detector**: Uses ML techniques for fraud detection
3. **Company Enrichment Information**: Integrates with eInforma Developer API
4. **Treezor Integration for Core Banking Accounts**: Connects to Treezor's API for wallet functionality

Each example includes step-by-step instructions, code samples, and explanations of key concepts.

## Implementation Status

### Implemented Features

- Core plugin management (installation, lifecycle, configuration)
- Extension point registry and discovery
- In-memory event bus for plugin communication
- JAR-based plugin loading
- Basic security isolation through class loaders
- Plugin metadata and dependency management

### Planned Features

- Kafka-based distributed event bus
- Git repository plugin installation
- Hot deployment of plugins
- Plugin health monitoring
- Plugin debugging tools
- Enhanced security controls
- Plugin versioning and compatibility checking
- Plugin testing framework

## Contributing

Contributions to the Firefly Plugin Manager are welcome! Please refer to the contributing guidelines for more information.

## License

This project is licensed under the [License Name] - see the LICENSE file for details.
