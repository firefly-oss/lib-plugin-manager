# Troubleshooting

This guide provides solutions for common issues you might encounter when working with the Firefly Plugin Manager.

## Table of Contents

1. [Plugin Installation Issues](#plugin-installation-issues)
2. [Plugin Lifecycle Issues](#plugin-lifecycle-issues)
3. [Extension Point Issues](#extension-point-issues)
4. [Extension Issues](#extension-issues)
5. [Event Bus Issues](#event-bus-issues)
6. [Configuration Issues](#configuration-issues)
7. [Classpath Issues](#classpath-issues)
8. [Debugging Techniques](#debugging-techniques)
9. [Getting Help](#getting-help)

## Plugin Installation Issues

### Plugin Not Found

**Problem**: The Plugin Manager cannot find a plugin JAR file.

**Solutions**:
- Verify that the file exists in the specified location
- Check file permissions to ensure the application can read the file
- Ensure the file has the correct extension (usually .jar)
- Check for file system errors or disk space issues

**Example Error**:
```
java.io.FileNotFoundException: /path/to/plugins/my-plugin-1.0.0.jar (No such file or directory)
```

### Invalid Plugin JAR

**Problem**: The JAR file does not contain a valid plugin.

**Solutions**:
- Verify that the JAR contains a class that implements the `Plugin` interface
- Check that the plugin class is registered in `META-INF/services/com.firefly.core.plugin.api.Plugin`
- Ensure the plugin class has a public constructor
- Check for missing dependencies in the JAR

**Example Error**:
```
com.firefly.core.plugin.exception.PluginException: No plugin found in JAR: /path/to/plugins/my-plugin-1.0.0.jar
```

### Plugin Already Installed

**Problem**: Attempting to install a plugin that is already installed.

**Solutions**:
- Check if the plugin is already installed using `pluginManager.getPluginRegistry().getPlugin(pluginId)`
- Uninstall the existing plugin before installing the new one
- Use `updatePlugin` instead of `installPlugin` if you want to update an existing plugin

**Example Error**:
```
com.firefly.core.plugin.exception.PluginException: Plugin already registered: com.example.my-plugin
```

### Git Repository Issues

**Problem**: Cannot install a plugin from a Git repository.

**Solutions**:
- Verify that the repository URL is correct
- Check that the authentication credentials are valid
- Ensure the specified branch exists
- Check network connectivity to the Git server
- Verify that Git is installed and available on the system path

**Example Error**:
```
org.eclipse.jgit.api.errors.TransportException: https://github.com/example/my-plugin.git: not authorized
```

## Plugin Lifecycle Issues

### Initialization Failed

**Problem**: Plugin fails to initialize.

**Solutions**:
- Check the plugin's `initialize()` method for errors
- Verify that all required dependencies are available
- Check for configuration issues
- Look for exceptions in the logs

**Example Error**:
```
com.firefly.core.plugin.exception.PluginLifecycleException: Failed to initialize plugin: com.example.my-plugin
Caused by: java.lang.NullPointerException: Cannot invoke "com.example.service.ExternalService.initialize()" because "this.service" is null
```

### Start Failed

**Problem**: Plugin fails to start.

**Solutions**:
- Check the plugin's `start()` method for errors
- Verify that the plugin was properly initialized
- Check for resource availability (e.g., database connections, file handles)
- Look for exceptions in the logs

**Example Error**:
```
com.firefly.core.plugin.exception.PluginLifecycleException: Failed to start plugin: com.example.my-plugin
Caused by: java.net.ConnectException: Connection refused: connect
```

### Stop Failed

**Problem**: Plugin fails to stop.

**Solutions**:
- Check the plugin's `stop()` method for errors
- Look for hanging threads or unfinished tasks
- Check for resource cleanup issues
- Increase the timeout for stopping plugins

**Example Error**:
```
com.firefly.core.plugin.exception.PluginLifecycleException: Failed to stop plugin: com.example.my-plugin
Caused by: java.util.concurrent.TimeoutException: Timeout waiting for plugin to stop
```

### Uninstall Failed

**Problem**: Plugin fails to uninstall.

**Solutions**:
- Check the plugin's `uninstall()` method for errors
- Ensure the plugin is in the STOPPED state before uninstalling
- Check for resource cleanup issues
- Look for exceptions in the logs

**Example Error**:
```
com.firefly.core.plugin.exception.PluginLifecycleException: Failed to uninstall plugin: com.example.my-plugin
Caused by: java.io.IOException: Cannot delete file: /path/to/plugins/temp/my-plugin-1.0.0.jar
```

## Extension Point Issues

### Extension Point Not Found

**Problem**: Cannot find an extension point.

**Solutions**:
- Verify that the extension point ID is correct
- Check that the extension point is registered with the Extension Registry
- Ensure the extension point is defined in a core microservice
- Look for typos in the extension point ID

**Example Error**:
```
com.firefly.core.plugin.exception.ExtensionException: Extension point not found: com.firefly.banking.payment-processor
```

### Invalid Extension Point

**Problem**: Extension point is not properly defined.

**Solutions**:
- Verify that the extension point is annotated with `@ExtensionPoint`
- Check that the extension point is an interface or abstract class
- Ensure the extension point has a valid ID
- Check for missing or incorrect metadata

**Example Error**:
```
com.firefly.core.plugin.exception.ExtensionException: Invalid extension point: com.firefly.banking.payment-processor
```

### Extension Point Registration Failed

**Problem**: Cannot register an extension point.

**Solutions**:
- Check for duplicate extension point IDs
- Verify that the extension point class is accessible
- Ensure the extension point is properly defined
- Look for exceptions in the logs

**Example Error**:
```
com.firefly.core.plugin.exception.ExtensionException: Failed to register extension point: com.firefly.banking.payment-processor
Caused by: java.lang.IllegalArgumentException: Extension point ID cannot be null or empty
```

## Extension Issues

### Extension Registration Failed

**Problem**: Cannot register an extension.

**Solutions**:
- Verify that the extension point exists
- Check that the extension implements the extension point interface
- Ensure the extension class is accessible
- Look for exceptions in the logs

**Example Error**:
```
com.firefly.core.plugin.exception.ExtensionException: Failed to register extension for extension point: com.firefly.banking.payment-processor
Caused by: java.lang.ClassCastException: class com.example.payment.CreditCardPaymentProcessor cannot be cast to class com.firefly.banking.payment.PaymentProcessor
```

### No Extensions Found

**Problem**: No extensions are found for an extension point.

**Solutions**:
- Verify that plugins implementing the extension point are installed and started
- Check that extensions are properly registered
- Ensure the extension point ID is correct
- Look for filtering issues (e.g., incorrect filter criteria)

**Example Error**:
```
java.util.NoSuchElementException: No value present
```

### Extension Execution Failed

**Problem**: An extension fails during execution.

**Solutions**:
- Check the extension implementation for errors
- Verify that the extension is properly initialized
- Ensure all required dependencies are available
- Look for exceptions in the logs

**Example Error**:
```
java.lang.RuntimeException: Failed to process payment
Caused by: java.net.ConnectException: Connection refused: connect
```

## Event Bus Issues

### Event Publication Failed

**Problem**: Cannot publish an event.

**Solutions**:
- Verify that the event bus is initialized
- Check that the topic is valid
- Ensure the event data is serializable (for Kafka event bus)
- Look for exceptions in the logs

**Example Error**:
```
com.firefly.core.plugin.exception.EventBusException: Failed to publish event to topic: payment.processed
Caused by: org.apache.kafka.common.errors.SerializationException: Can't serialize data
```

### Event Subscription Failed

**Problem**: Cannot subscribe to events.

**Solutions**:
- Verify that the event bus is initialized
- Check that the topic is valid
- Ensure the subscriber is properly implemented
- Look for exceptions in the logs

**Example Error**:
```
com.firefly.core.plugin.exception.EventBusException: Failed to subscribe to topic: payment.processed
```

### Event Handler Exception

**Problem**: An event handler throws an exception.

**Solutions**:
- Check the event handler implementation for errors
- Verify that the event data is in the expected format
- Ensure all required dependencies are available
- Implement error handling in the event handler

**Example Error**:
```
java.lang.RuntimeException: Error handling event: payment.processed
Caused by: java.lang.NullPointerException: Cannot invoke "java.util.Map.get(Object)" because "data" is null
```

## Configuration Issues

### Invalid Configuration

**Problem**: Plugin configuration is invalid.

**Solutions**:
- Verify that the configuration values are of the correct type
- Check for missing required configuration properties
- Ensure configuration values are within valid ranges
- Look for typos in configuration property names

**Example Error**:
```
com.firefly.core.plugin.exception.ConfigurationException: Invalid configuration: timeout must be a positive integer
```

### Configuration Update Failed

**Problem**: Cannot update plugin configuration.

**Solutions**:
- Verify that the plugin is in a state that allows configuration updates
- Check that the configuration values are valid
- Ensure the plugin implements `updateConfiguration` correctly
- Look for exceptions in the logs

**Example Error**:
```
com.firefly.core.plugin.exception.PluginException: Failed to update configuration for plugin: com.example.my-plugin
```

### Missing Configuration

**Problem**: Required configuration is missing.

**Solutions**:
- Provide default values for configuration properties
- Check that all required configuration is provided
- Validate configuration during plugin initialization
- Provide clear error messages for missing configuration

**Example Error**:
```
com.firefly.core.plugin.exception.ConfigurationException: Missing required configuration property: apiKey
```

## Classpath Issues

### Class Not Found

**Problem**: A required class cannot be found.

**Solutions**:
- Verify that the class is included in the plugin JAR
- Check for missing dependencies
- Ensure the class is in the correct package
- Look for class loader issues

**Example Error**:
```
java.lang.ClassNotFoundException: com.example.payment.CreditCardPaymentProcessor
```

### Method Not Found

**Problem**: A required method cannot be found.

**Solutions**:
- Verify that the method exists in the class
- Check for method signature mismatches
- Ensure the method is public
- Look for version compatibility issues

**Example Error**:
```
java.lang.NoSuchMethodException: com.example.payment.CreditCardPaymentProcessor.processPayment(java.math.BigDecimal, java.lang.String, java.lang.String)
```

### Dependency Conflict

**Problem**: Conflicting dependencies between plugins or with the application.

**Solutions**:
- Use shaded JARs to avoid conflicts
- Specify version ranges for dependencies
- Use the plugin's class loader to load dependencies
- Implement proper class loader isolation

**Example Error**:
```
java.lang.LinkageError: Loader constraint violation: when resolving method "org.slf4j.LoggerFactory.getLogger(Ljava/lang/Class;)Lorg/slf4j/Logger;" the class loader (instance of org/springframework/boot/loader/LaunchedURLClassLoader) of the current class, com/example/payment/CreditCardPaymentProcessor, and the class loader (instance of java/net/URLClassLoader) for the method's defining class, org/slf4j/LoggerFactory, have different Class objects for the type org/slf4j/Logger used in the signature
```

## Debugging Techniques

### Enable Debug Logging

Enable debug logging to get more detailed information:

```yaml
# In application.yml
logging:
  level:
    com.firefly.core.plugin: DEBUG
```

### Use the Plugin Debugger

The Plugin Manager includes a Plugin Debugger that provides detailed information about plugins and extensions:

```java
// Get the plugin debugger
PluginDebugger debugger = pluginManager.getPluginDebugger();

// Get detailed information about a plugin
PluginDebugInfo debugInfo = debugger.getPluginDebugInfo("com.example.my-plugin").block();

// Print debug information
System.out.println("Plugin ID: " + debugInfo.getPluginId());
System.out.println("Plugin State: " + debugInfo.getState());
System.out.println("Extensions: " + debugInfo.getExtensions());
System.out.println("Class Loader: " + debugInfo.getClassLoaderInfo());
System.out.println("Resources: " + debugInfo.getResources());
```

### Inspect Class Loaders

Inspect class loaders to diagnose class loading issues:

```java
// Get the plugin
Plugin plugin = pluginManager.getPluginRegistry()
        .getPlugin("com.example.my-plugin")
        .block();

// Get the plugin's class loader
ClassLoader classLoader = plugin.getClass().getClassLoader();

// Print class loader hierarchy
while (classLoader != null) {
    System.out.println("Class Loader: " + classLoader);
    classLoader = classLoader.getParent();
}
```

### Monitor Events

Monitor events to diagnose communication issues:

```java
// Subscribe to all events
pluginManager.getEventBus().subscribe("*", event -> {
    System.out.println("Event: " + event.getTopic());
    System.out.println("Data: " + event.getData());
});
```

### Check Plugin Health

Use the health monitoring feature to check plugin health:

```java
// Get plugin health
PluginHealth health = pluginManager.getPluginHealth("com.example.my-plugin").block();

// Print health information
System.out.println("Status: " + health.getStatus());
System.out.println("Memory Usage: " + health.getMemoryUsage() + " MB");
System.out.println("CPU Usage: " + health.getCpuUsage() + "%");
System.out.println("Thread Count: " + health.getThreadCount());
System.out.println("Error Count: " + health.getErrorCount());
```

## Getting Help

If you're still having issues after trying the solutions above, here are some ways to get help:

### Check Documentation

- Review the [Plugin Manager documentation](../README.md)
- Check the [examples](./10-examples.md) for reference implementations
- Look for similar issues in the troubleshooting guide

### Check Logs

- Review application logs for error messages
- Enable debug logging for more detailed information
- Look for stack traces that indicate the root cause

### Contact Support

- Open an issue in the repository
- Provide detailed information about the issue
- Include relevant logs and configuration
- Describe the steps to reproduce the issue

### Community Resources

- Join the Firefly Platform community forum
- Attend community webinars and workshops
- Participate in the developer Slack channel

By following these troubleshooting steps, you should be able to resolve most issues with the Firefly Plugin Manager. If you encounter a new issue not covered in this guide, please contribute your solution to help others in the future.
