/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.core.plugin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods for resource management.
 * This class provides helper methods for working with files, streams, and other resources.
 */
public final class ResourceUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ResourceUtils.class);
    
    private ResourceUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Reads the content of a file as a string.
     * 
     * @param path the path to the file
     * @return an Optional containing the file content, or empty if the file could not be read
     */
    public static Optional<String> readFileAsString(Path path) {
        try {
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.warn("Error reading file: {}", path, e);
            return Optional.empty();
        }
    }
    
    /**
     * Reads the content of a file as a list of lines.
     * 
     * @param path the path to the file
     * @return an Optional containing the list of lines, or empty if the file could not be read
     */
    public static Optional<List<String>> readFileAsLines(Path path) {
        try {
            return Optional.of(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.warn("Error reading file: {}", path, e);
            return Optional.empty();
        }
    }
    
    /**
     * Writes a string to a file.
     * 
     * @param path the path to the file
     * @param content the content to write
     * @return true if the file was written successfully, false otherwise
     */
    public static boolean writeStringToFile(Path path, String content) {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            logger.warn("Error writing to file: {}", path, e);
            return false;
        }
    }
    
    /**
     * Writes a list of lines to a file.
     * 
     * @param path the path to the file
     * @param lines the lines to write
     * @return true if the file was written successfully, false otherwise
     */
    public static boolean writeLinesToFile(Path path, List<String> lines) {
        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            logger.warn("Error writing to file: {}", path, e);
            return false;
        }
    }
    
    /**
     * Reads a resource from the classpath as a string.
     * 
     * @param resourcePath the path to the resource
     * @param classLoader the class loader to use
     * @return an Optional containing the resource content, or empty if the resource could not be read
     */
    public static Optional<String> readResourceAsString(String resourcePath, ClassLoader classLoader) {
        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return Optional.empty();
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return Optional.of(reader.lines().collect(Collectors.joining(System.lineSeparator())));
            }
        } catch (IOException e) {
            logger.warn("Error reading resource: {}", resourcePath, e);
            return Optional.empty();
        }
    }
    
    /**
     * Reads a resource from the classpath as a string using the default class loader.
     * 
     * @param resourcePath the path to the resource
     * @return an Optional containing the resource content, or empty if the resource could not be read
     */
    public static Optional<String> readResourceAsString(String resourcePath) {
        return readResourceAsString(resourcePath, ClassLoaderUtils.getDefaultClassLoader());
    }
    
    /**
     * Loads properties from a file.
     * 
     * @param path the path to the properties file
     * @return an Optional containing the properties, or empty if the file could not be read
     */
    public static Optional<Properties> loadProperties(Path path) {
        Properties properties = new Properties();
        
        try (InputStream is = Files.newInputStream(path)) {
            properties.load(is);
            return Optional.of(properties);
        } catch (IOException e) {
            logger.warn("Error loading properties from file: {}", path, e);
            return Optional.empty();
        }
    }
    
    /**
     * Loads properties from a resource in the classpath.
     * 
     * @param resourcePath the path to the resource
     * @param classLoader the class loader to use
     * @return an Optional containing the properties, or empty if the resource could not be read
     */
    public static Optional<Properties> loadPropertiesFromResource(String resourcePath, ClassLoader classLoader) {
        Properties properties = new Properties();
        
        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return Optional.empty();
            }
            
            properties.load(is);
            return Optional.of(properties);
        } catch (IOException e) {
            logger.warn("Error loading properties from resource: {}", resourcePath, e);
            return Optional.empty();
        }
    }
    
    /**
     * Loads properties from a resource in the classpath using the default class loader.
     * 
     * @param resourcePath the path to the resource
     * @return an Optional containing the properties, or empty if the resource could not be read
     */
    public static Optional<Properties> loadPropertiesFromResource(String resourcePath) {
        return loadPropertiesFromResource(resourcePath, ClassLoaderUtils.getDefaultClassLoader());
    }
    
    /**
     * Lists all files in a directory that match a predicate.
     * 
     * @param directory the directory to list
     * @param predicate the predicate to apply
     * @return a list of paths to the matching files
     */
    public static List<Path> listFiles(Path directory, java.util.function.Predicate<Path> predicate) {
        if (!Files.isDirectory(directory)) {
            logger.warn("Not a directory: {}", directory);
            return List.of();
        }
        
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(predicate).collect(Collectors.toList());
        } catch (IOException e) {
            logger.warn("Error listing files in directory: {}", directory, e);
            return List.of();
        }
    }
    
    /**
     * Creates a directory if it does not exist.
     * 
     * @param directory the directory to create
     * @return true if the directory was created or already exists, false otherwise
     */
    public static boolean createDirectoryIfNotExists(Path directory) {
        if (Files.exists(directory)) {
            return Files.isDirectory(directory);
        }
        
        try {
            Files.createDirectories(directory);
            return true;
        } catch (IOException e) {
            logger.warn("Error creating directory: {}", directory, e);
            return false;
        }
    }
    
    /**
     * Deletes a file or directory.
     * 
     * @param path the path to delete
     * @return true if the path was deleted successfully, false otherwise
     */
    public static boolean delete(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.list(path)) {
                    for (Path child : paths.collect(Collectors.toList())) {
                        delete(child);
                    }
                }
            }
            
            Files.delete(path);
            return true;
        } catch (IOException e) {
            logger.warn("Error deleting path: {}", path, e);
            return false;
        }
    }
    
    /**
     * Copies a file or directory.
     * 
     * @param source the source path
     * @param target the target path
     * @return true if the path was copied successfully, false otherwise
     */
    public static boolean copy(Path source, Path target) {
        try {
            if (Files.isDirectory(source)) {
                createDirectoryIfNotExists(target);
                
                try (Stream<Path> paths = Files.list(source)) {
                    for (Path child : paths.collect(Collectors.toList())) {
                        String fileName = child.getFileName().toString();
                        Path targetChild = target.resolve(fileName);
                        copy(child, targetChild);
                    }
                }
                
                return true;
            } else {
                Files.copy(source, target);
                return true;
            }
        } catch (IOException e) {
            logger.warn("Error copying path: {} to {}", source, target, e);
            return false;
        }
    }
}
