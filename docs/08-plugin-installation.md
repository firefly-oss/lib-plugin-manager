# Plugin Installation Methods

This guide explains the different methods for installing plugins in the Firefly Plugin Manager, including their advantages, limitations, and configuration options.

## Table of Contents

1. [Overview of Installation Methods](#overview-of-installation-methods)
2. [JAR File Installation](#jar-file-installation)
3. [Git Repository Installation](#git-repository-installation)
4. [Classpath Auto-detection](#classpath-auto-detection)
5. [Installation Configuration](#installation-configuration)
6. [Plugin Lifecycle After Installation](#plugin-lifecycle-after-installation)
7. [Troubleshooting](#troubleshooting)

## Overview of Installation Methods

The Firefly Plugin Manager supports three primary methods for installing plugins:

| Method | Description | Best For | Limitations |
|--------|-------------|----------|-------------|
| JAR File | Install plugins from JAR files in a directory | Production environments, manual installation | Requires file system access |
| Git Repository | Install plugins directly from Git repositories | Development, CI/CD pipelines | Requires Git access, build tools |
| Classpath | Auto-detect plugins in the application classpath | Development, tightly integrated plugins | No isolation, requires application restart |

Each method has its own advantages and use cases, allowing you to choose the approach that best fits your needs.

## JAR File Installation

JAR file installation is the most straightforward method for installing plugins. It involves placing plugin JAR files in a designated directory and loading them into the Plugin Manager.

### How It Works

1. Plugin JAR files are placed in a configured plugins directory
2. The Plugin Manager scans this directory for JAR files
3. Each JAR file is loaded using a separate class loader
4. Plugins are discovered using the Service Provider Interface (SPI) mechanism
5. Discovered plugins are registered with the Plugin Registry

### Installation Steps

#### Programmatic Installation

```java
// Install a plugin from a specific JAR file
Mono<Plugin> installedPlugin = pluginManager.installPlugin(
        Paths.get("/path/to/plugins/my-plugin-1.0.0.jar"));

// Install all plugins from a directory
Flux<Plugin> installedPlugins = pluginManager.installPluginsFromDirectory(
        Paths.get("/path/to/plugins"));
```

#### Configuration-Based Installation

```yaml
# In application.yml
firefly:
  plugin-manager:
    plugins-directory: /path/to/plugins
    auto-install: true
    auto-start: true
```

### Advantages

- **Isolation**: Each plugin runs in its own class loader
- **Versioning**: Easy to manage different versions of plugins
- **Deployment**: Simple to deploy in production environments
- **Hot Deployment**: Plugins can be added without restarting the application
- **Security**: Better control over which plugins are installed

### Limitations

- **File System Access**: Requires access to the file system
- **Manual Management**: Requires manual copying of JAR files
- **Dependency Management**: Dependencies must be included in the JAR or provided by the application

### Best Practices

- Use a dedicated directory for plugins
- Follow a consistent naming convention for plugin JAR files
- Include all necessary dependencies in the plugin JAR
- Use the Maven Shade Plugin to create a fat JAR if needed
- Implement proper error handling for plugin loading failures

## Git Repository Installation

Git repository installation allows you to install plugins directly from Git repositories, enabling seamless integration with CI/CD pipelines and development workflows.

### How It Works

1. The Plugin Manager clones or pulls the Git repository
2. The plugin project is built using Maven or Gradle
3. The resulting JAR file is loaded using a separate class loader
4. The plugin is discovered and registered with the Plugin Registry

### Installation Steps

#### Programmatic Installation

```java
// Install a plugin from a Git repository
Mono<Plugin> installedPlugin = pluginManager.installPluginFromGit(
        "https://github.com/example/my-plugin.git");

// Install a plugin from a specific branch or tag
Mono<Plugin> installedPlugin = pluginManager.installPluginFromGit(
        "https://github.com/example/my-plugin.git",
        "feature/new-feature");

// Install with authentication
GitAuthenticationConfig authConfig = GitAuthenticationConfig.builder()
        .type(GitAuthenticationType.TOKEN)
        .accessToken("your-access-token")
        .build();

Mono<Plugin> installedPlugin = pluginManager.installPluginFromGit(
        "https://github.com/example/my-plugin.git",
        "main",
        authConfig);
```

#### Configuration-Based Installation

```yaml
# In application.yml
firefly:
  plugin-manager:
    git:
      repositories:
        - url: https://github.com/example/my-plugin.git
          branch: main
          authentication-type: token
          access-token: ${GIT_ACCESS_TOKEN}
        - url: https://github.com/example/another-plugin.git
          branch: stable
          authentication-type: basic
          username: ${GIT_USERNAME}
          password: ${GIT_PASSWORD}
      auto-install: true
      auto-start: true
```

### Authentication Options

The Git plugin loader supports several authentication methods:

#### 1. No Authentication (Public Repositories)

```java
GitAuthenticationConfig authConfig = GitAuthenticationConfig.builder()
        .type(GitAuthenticationType.NONE)
        .build();
```

#### 2. Basic Authentication

```java
GitAuthenticationConfig authConfig = GitAuthenticationConfig.builder()
        .type(GitAuthenticationType.BASIC)
        .username("your-username")
        .password("your-password")
        .build();
```

#### 3. Token Authentication (Recommended)

```java
GitAuthenticationConfig authConfig = GitAuthenticationConfig.builder()
        .type(GitAuthenticationType.TOKEN)
        .accessToken("your-access-token")
        .build();
```

#### 4. SSH Authentication

```java
GitAuthenticationConfig authConfig = GitAuthenticationConfig.builder()
        .type(GitAuthenticationType.SSH)
        .privateKeyPath("/path/to/private-key")
        .privateKeyPassphrase("your-passphrase")
        .build();
```

### Advantages

- **CI/CD Integration**: Easy integration with CI/CD pipelines
- **Version Control**: Plugins are version-controlled
- **Automated Updates**: Can automatically update plugins from repositories
- **Build Process**: Can include a build process for plugins
- **Development Workflow**: Streamlined development workflow

### Limitations

- **Build Tools**: Requires Maven or Gradle to be available
- **Network Access**: Requires network access to Git repositories
- **Build Time**: Building plugins adds time to the installation process
- **Security Considerations**: Requires secure handling of Git credentials

### Best Practices

- Use token-based authentication instead of username/password
- Store sensitive credentials securely (environment variables, vault)
- Use specific tags or commits for production environments
- Implement proper error handling for Git operations
- Set appropriate timeouts for Git operations

## Classpath Auto-detection

Classpath auto-detection allows you to discover and load plugins that are already on the application classpath, typically added as dependencies to your application.

### How It Works

1. The Plugin Manager scans the classpath for classes annotated with `@Plugin`
2. Discovered plugin classes are instantiated
3. Plugin instances are registered with the Plugin Registry

### Installation Steps

#### Programmatic Installation

```java
// Scan the entire classpath for plugins
Flux<Plugin> installedPlugins = pluginManager.installPluginsFromClasspath();

// Scan a specific package for plugins
Flux<Plugin> installedPlugins = pluginManager.installPluginsFromClasspath(
        "com.example.plugins");
```

#### Configuration-Based Installation

```yaml
# In application.yml
firefly:
  plugin-manager:
    classpath:
      scan-enabled: true
      base-packages: com.example.plugins,com.another.plugins
      auto-start: true
```

### Advantages

- **Simplicity**: No need for separate installation steps
- **Development**: Easy to use during development
- **Integration**: Tight integration with the application
- **Dependency Management**: Leverages the application's dependency management
- **Testing**: Easier to test plugins during development

### Limitations

- **No Isolation**: Plugins share the application's class loader
- **No Hot Deployment**: Requires application restart to add new plugins
- **Version Conflicts**: Potential for dependency version conflicts
- **Limited Separation**: Less separation between application and plugins

### Best Practices

- Use specific base packages to limit scanning
- Ensure plugin dependencies are compatible with the application
- Use this method primarily for development or tightly integrated plugins
- Consider moving to JAR or Git installation for production
- Document plugin dependencies clearly

## Installation Configuration

The Plugin Manager provides several configuration options for plugin installation:

### Global Configuration

```yaml
# In application.yml
firefly:
  plugin-manager:
    plugins-directory: /path/to/plugins
    temp-directory: /path/to/temp
    auto-install: true
    auto-start: true
    installation-timeout: 60
    start-timeout: 30
```

| Property | Description | Default |
|----------|-------------|---------|
| plugins-directory | Directory where plugin JAR files are stored | ./plugins |
| temp-directory | Directory for temporary files | ./plugins/temp |
| auto-install | Whether to automatically install plugins at startup | false |
| auto-start | Whether to automatically start plugins after installation | false |
| installation-timeout | Timeout in seconds for plugin installation | 60 |
| start-timeout | Timeout in seconds for plugin startup | 30 |

### JAR Installation Configuration

```yaml
firefly:
  plugin-manager:
    jar:
      enabled: true
      plugins-directory: /path/to/plugins
      auto-install: true
      file-pattern: "*.jar"
```

| Property | Description | Default |
|----------|-------------|---------|
| enabled | Whether JAR installation is enabled | true |
| plugins-directory | Directory where plugin JAR files are stored | ./plugins |
| auto-install | Whether to automatically install plugins at startup | false |
| file-pattern | Pattern for matching plugin JAR files | *.jar |

### Git Installation Configuration

```yaml
firefly:
  plugin-manager:
    git:
      enabled: true
      repositories:
        - url: https://github.com/example/my-plugin.git
          branch: main
          authentication-type: token
          access-token: ${GIT_ACCESS_TOKEN}
      clone-directory: /path/to/git-plugins
      auto-install: true
      default-branch: main
      timeout-seconds: 60
      verify-ssl: true
```

| Property | Description | Default |
|----------|-------------|---------|
| enabled | Whether Git installation is enabled | true |
| repositories | List of Git repositories to install plugins from | [] |
| clone-directory | Directory where Git repositories are cloned | ./plugins/git |
| auto-install | Whether to automatically install plugins at startup | false |
| default-branch | Default branch to use if not specified | main |
| timeout-seconds | Timeout in seconds for Git operations | 60 |
| verify-ssl | Whether to verify SSL certificates | true |

### Classpath Installation Configuration

```yaml
firefly:
  plugin-manager:
    classpath:
      enabled: true
      scan-enabled: true
      base-packages: com.example.plugins,com.another.plugins
      auto-start: true
```

| Property | Description | Default |
|----------|-------------|---------|
| enabled | Whether classpath installation is enabled | true |
| scan-enabled | Whether to scan the classpath for plugins | false |
| base-packages | Comma-separated list of packages to scan | [] |
| auto-start | Whether to automatically start plugins after installation | false |

## Plugin Lifecycle After Installation

After a plugin is installed, it goes through several lifecycle stages:

### 1. Installation

The plugin is loaded and registered with the Plugin Registry:

```java
// Install a plugin
Mono<Plugin> installedPlugin = pluginManager.installPlugin(
        Paths.get("/path/to/plugins/my-plugin-1.0.0.jar"));

// The plugin is now in the INSTALLED state
```

### 2. Initialization

The plugin's `initialize()` method is called:

```java
// Initialize a plugin
Mono<Void> initialized = pluginManager.initializePlugin("com.example.my-plugin");

// The plugin is now in the INITIALIZED state
```

### 3. Starting

The plugin's `start()` method is called:

```java
// Start a plugin
Mono<Void> started = pluginManager.startPlugin("com.example.my-plugin");

// The plugin is now in the STARTED state
```

### 4. Stopping

The plugin's `stop()` method is called:

```java
// Stop a plugin
Mono<Void> stopped = pluginManager.stopPlugin("com.example.my-plugin");

// The plugin is now in the STOPPED state
```

### 5. Uninstallation

The plugin's `uninstall()` method is called, and it is removed from the Plugin Registry:

```java
// Uninstall a plugin
Mono<Void> uninstalled = pluginManager.uninstallPlugin("com.example.my-plugin");

// The plugin is now uninstalled
```

### Automatic Lifecycle Management

You can configure the Plugin Manager to automatically initialize and start plugins after installation:

```yaml
# In application.yml
firefly:
  plugin-manager:
    auto-install: true
    auto-start: true
```

With this configuration, plugins will be automatically installed, initialized, and started at application startup.

## Troubleshooting

Here are some common issues and solutions related to plugin installation:

### Plugin Not Found

**Issue**: The Plugin Manager cannot find a plugin JAR file.

**Solutions**:
- Check that the file exists in the configured plugins directory
- Verify that the file has the correct extension (usually .jar)
- Ensure that the file is readable by the application
- Check for file system permissions

### Plugin Loading Failed

**Issue**: The Plugin Manager cannot load a plugin from a JAR file.

**Solutions**:
- Verify that the JAR file contains a valid plugin class
- Check that the plugin class implements the `Plugin` interface
- Ensure that the plugin is registered in `META-INF/services`
- Look for exceptions in the logs related to class loading

### Git Repository Issues

**Issue**: The Plugin Manager cannot clone or pull from a Git repository.

**Solutions**:
- Verify that the repository URL is correct
- Check that the authentication credentials are valid
- Ensure that the specified branch exists
- Check network connectivity to the Git server
- Look for Git-related exceptions in the logs

### Classpath Scanning Issues

**Issue**: The Plugin Manager cannot find plugins on the classpath.

**Solutions**:
- Verify that the plugin classes are annotated with `@Plugin`
- Check that the base packages are correctly configured
- Ensure that the plugin classes are on the classpath
- Look for scanning-related exceptions in the logs

### Dependency Conflicts

**Issue**: Plugin installation fails due to dependency conflicts.

**Solutions**:
- Check for conflicting dependencies between plugins
- Use shaded JARs to avoid conflicts
- Configure the Plugin Manager to use isolated class loaders
- Review the dependency tree for conflicts

### Plugin Initialization Failed

**Issue**: The plugin is installed but fails to initialize.

**Solutions**:
- Check the plugin's `initialize()` method for errors
- Look for exceptions in the logs during initialization
- Verify that all required dependencies are available
- Check that the plugin's configuration is valid

By understanding these installation methods and their configuration options, you can choose the approach that best fits your needs and ensure a smooth plugin installation process.
