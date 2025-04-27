package com.catalis.core.plugin.loader;

import com.catalis.core.plugin.api.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.file.Path;

/**
 * A composite plugin loader that delegates to specialized loaders based on the source type.
 */
@Component
@Primary
public class CompositePluginLoader implements PluginLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(CompositePluginLoader.class);
    
    private final DefaultPluginLoader defaultPluginLoader;
    private final GitPluginLoader gitPluginLoader;
    private final ClasspathPluginLoader classpathPluginLoader;
    
    /**
     * Creates a new CompositePluginLoader.
     * 
     * @param defaultPluginLoader the default plugin loader for JAR files
     * @param gitPluginLoader the Git plugin loader
     * @param classpathPluginLoader the classpath plugin loader
     */
    public CompositePluginLoader(
            DefaultPluginLoader defaultPluginLoader,
            GitPluginLoader gitPluginLoader,
            ClasspathPluginLoader classpathPluginLoader) {
        this.defaultPluginLoader = defaultPluginLoader;
        this.gitPluginLoader = gitPluginLoader;
        this.classpathPluginLoader = classpathPluginLoader;
    }
    
    @Override
    public Mono<Plugin> loadPlugin(Path pluginPath) {
        logger.info("Loading plugin from path: {}", pluginPath);
        return defaultPluginLoader.loadPlugin(pluginPath);
    }
    
    @Override
    public Mono<Plugin> loadPluginFromGit(URI repositoryUri, String branch) {
        logger.info("Loading plugin from Git repository: {}, branch: {}", repositoryUri, branch);
        return gitPluginLoader.loadPluginFromGit(repositoryUri, branch);
    }
    
    @Override
    public Flux<Plugin> loadPluginsFromClasspath(String basePackage) {
        logger.info("Loading plugins from classpath, base package: {}", basePackage);
        return classpathPluginLoader.loadPluginsFromClasspath(basePackage);
    }
}
