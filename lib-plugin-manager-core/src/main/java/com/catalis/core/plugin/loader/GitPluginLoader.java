package com.catalis.core.plugin.loader;

import com.catalis.core.plugin.api.Plugin;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Plugin loader that loads plugins from Git repositories.
 */
@Component
public class GitPluginLoader implements PluginLoader {

    private static final Logger logger = LoggerFactory.getLogger(GitPluginLoader.class);
    private final DefaultPluginLoader defaultPluginLoader;
    private final Path tempDirectory;

    /**
     * Creates a new GitPluginLoader.
     *
     * @param defaultPluginLoader the default plugin loader to delegate to after cloning
     */
    public GitPluginLoader(DefaultPluginLoader defaultPluginLoader) {
        this.defaultPluginLoader = defaultPluginLoader;

        // Create a temporary directory for cloning repositories
        try {
            this.tempDirectory = Files.createTempDirectory("plugin-git-repos");
            // Register a shutdown hook to delete the temporary directory when the JVM exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.walk(tempDirectory)
                            .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete children first
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    logger.warn("Failed to delete temporary file: {}", path, e);
                                }
                            });
                } catch (IOException e) {
                    logger.warn("Failed to clean up temporary directory: {}", tempDirectory, e);
                }
            }));
        } catch (IOException e) {
            logger.error("Failed to create temporary directory for Git repositories", e);
            throw new RuntimeException("Failed to initialize GitPluginLoader", e);
        }
    }

    @Override
    public Mono<Plugin> loadPlugin(Path pluginPath) {
        // Delegate to the default plugin loader
        return defaultPluginLoader.loadPlugin(pluginPath);
    }

    @Override
    public Mono<Plugin> loadPluginFromGit(URI repositoryUri, String branch) {
        logger.info("Loading plugin from Git repository: {}, branch: {}", repositoryUri, branch);

        return Mono.fromCallable(() -> {
            // Create a unique directory for this repository
            String repoName = repositoryUri.getPath();
            if (repoName.endsWith(".git")) {
                repoName = repoName.substring(0, repoName.length() - 4);
            }
            int lastSlash = repoName.lastIndexOf('/');
            if (lastSlash >= 0) {
                repoName = repoName.substring(lastSlash + 1);
            }

            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            Path repoDir = tempDirectory.resolve(repoName + "-" + uniqueId);

            // Clone the repository
            logger.info("Cloning repository {} to {}", repositoryUri, repoDir);
            Git git = Git.cloneRepository()
                    .setURI(repositoryUri.toString())
                    .setDirectory(repoDir.toFile())
                    .setBranch(branch != null ? branch : "main") // Default to 'main'
                    .call();

            try {
                // Look for a plugin JAR in the repository
                Path pluginJar = findPluginJar(repoDir);
                if (pluginJar == null) {
                    // Try to build the plugin if no JAR is found
                    pluginJar = buildPlugin(repoDir);
                }

                if (pluginJar == null) {
                    throw new IllegalStateException("No plugin JAR found in repository: " + repositoryUri);
                }

                // Load the plugin using the default loader
                return defaultPluginLoader.loadPlugin(pluginJar).block();
            } finally {
                git.close();
            }
        });
    }

    /**
     * Finds a plugin JAR in the repository.
     *
     * @param repoDir the repository directory
     * @return the path to the plugin JAR, or null if not found
     */
    private Path findPluginJar(Path repoDir) throws IOException {
        // Look for JAR files in the repository
        return Files.walk(repoDir)
                .filter(path -> path.toString().endsWith(".jar"))
                .findFirst()
                .orElse(null);
    }

    /**
     * Builds the plugin from source.
     *
     * @param repoDir the repository directory
     * @return the path to the built plugin JAR, or null if build fails
     */
    private Path buildPlugin(Path repoDir) {
        logger.info("Attempting to build plugin from source in {}", repoDir);

        // Check if it's a Maven project
        if (Files.exists(repoDir.resolve("pom.xml"))) {
            return buildMavenPlugin(repoDir);
        }

        // Check if it's a Gradle project
        if (Files.exists(repoDir.resolve("build.gradle")) || Files.exists(repoDir.resolve("build.gradle.kts"))) {
            return buildGradlePlugin(repoDir);
        }

        logger.warn("No recognized build system found in repository");
        return null;
    }

    /**
     * Builds a Maven plugin.
     *
     * @param repoDir the repository directory
     * @return the path to the built plugin JAR, or null if build fails
     */
    private Path buildMavenPlugin(Path repoDir) {
        try {
            logger.info("Building Maven plugin in {}", repoDir);

            // Execute Maven build
            Process process = new ProcessBuilder("mvn", "clean", "package", "-DskipTests")
                    .directory(repoDir.toFile())
                    .inheritIO()
                    .start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Maven build failed with exit code: {}", exitCode);
                return null;
            }

            // Look for the built JAR in the target directory
            return Files.walk(repoDir)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .filter(path -> path.toString().contains("target"))
                    .filter(path -> !path.toString().contains("-sources.jar"))
                    .filter(path -> !path.toString().contains("-javadoc.jar"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to build Maven plugin", e);
            return null;
        }
    }

    /**
     * Builds a Gradle plugin.
     *
     * @param repoDir the repository directory
     * @return the path to the built plugin JAR, or null if build fails
     */
    private Path buildGradlePlugin(Path repoDir) {
        try {
            logger.info("Building Gradle plugin in {}", repoDir);

            // Execute Gradle build
            String gradleCommand = Files.exists(repoDir.resolve("gradlew")) ? "./gradlew" : "gradle";
            Process process = new ProcessBuilder(gradleCommand, "clean", "build", "-x", "test")
                    .directory(repoDir.toFile())
                    .inheritIO()
                    .start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Gradle build failed with exit code: {}", exitCode);
                return null;
            }

            // Look for the built JAR in the build/libs directory
            return Files.walk(repoDir)
                    .filter(path -> path.toString().endsWith(".jar"))
                    .filter(path -> path.toString().contains("build/libs"))
                    .filter(path -> !path.toString().contains("-sources.jar"))
                    .filter(path -> !path.toString().contains("-javadoc.jar"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to build Gradle plugin", e);
            return null;
        }
    }

    @Override
    public Mono<Plugin> loadPluginFromGit(URI repositoryUri) {
        return loadPluginFromGit(repositoryUri, null);
    }

    @Override
    public Flux<Plugin> loadPluginsFromClasspath(String basePackage) {
        logger.error("Classpath plugin loading is not supported by this loader. Use ClasspathPluginLoader instead.");
        return Flux.error(new UnsupportedOperationException("Classpath plugin loading is not supported by this loader. Use ClasspathPluginLoader instead."));
    }

    @Override
    public Flux<Plugin> loadPluginsFromClasspath() {
        return loadPluginsFromClasspath(null);
    }
}
