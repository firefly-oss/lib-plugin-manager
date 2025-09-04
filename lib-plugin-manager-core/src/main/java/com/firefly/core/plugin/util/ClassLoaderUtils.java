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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility methods for working with class loaders.
 * This class provides helper methods for class loading operations.
 */
public final class ClassLoaderUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderUtils.class);
    
    private ClassLoaderUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Gets the most appropriate class loader for the current context.
     * 
     * @return the most appropriate class loader
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            logger.debug("Cannot access thread context ClassLoader - falling back to system class loader", ex);
        }
        
        if (cl == null) {
            cl = ClassLoaderUtils.class.getClassLoader();
            if (cl == null) {
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    logger.debug("Cannot access system ClassLoader - giving up", ex);
                }
            }
        }
        
        return cl;
    }
    
    /**
     * Loads a class with the given name using the provided class loader.
     * 
     * @param className the name of the class to load
     * @param classLoader the class loader to use
     * @return an Optional containing the loaded class, or empty if the class could not be loaded
     */
    public static Optional<Class<?>> loadClass(String className, ClassLoader classLoader) {
        try {
            return Optional.of(classLoader.loadClass(className));
        } catch (ClassNotFoundException e) {
            logger.debug("Class not found: {}", className, e);
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Error loading class: {}", className, e);
            return Optional.empty();
        }
    }
    
    /**
     * Loads a class with the given name using the default class loader.
     * 
     * @param className the name of the class to load
     * @return an Optional containing the loaded class, or empty if the class could not be loaded
     */
    public static Optional<Class<?>> loadClass(String className) {
        return loadClass(className, getDefaultClassLoader());
    }
    
    /**
     * Creates a new instance of a class.
     * 
     * @param <T> the type of the instance
     * @param clazz the class to instantiate
     * @return an Optional containing the new instance, or empty if the instance could not be created
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> createInstance(Class<?> clazz) {
        try {
            return Optional.of((T) clazz.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            logger.warn("Error creating instance of class: {}", clazz.getName(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Finds all resources with the given name using the provided class loader.
     * 
     * @param resourceName the name of the resource to find
     * @param classLoader the class loader to use
     * @return a list of URLs to the resources
     */
    public static List<URL> findResources(String resourceName, ClassLoader classLoader) {
        List<URL> resources = new ArrayList<>();
        
        try {
            Enumeration<URL> urls = classLoader.getResources(resourceName);
            while (urls.hasMoreElements()) {
                resources.add(urls.nextElement());
            }
        } catch (IOException e) {
            logger.warn("Error finding resources: {}", resourceName, e);
        }
        
        return resources;
    }
    
    /**
     * Finds all resources with the given name using the default class loader.
     * 
     * @param resourceName the name of the resource to find
     * @return a list of URLs to the resources
     */
    public static List<URL> findResources(String resourceName) {
        return findResources(resourceName, getDefaultClassLoader());
    }
    
    /**
     * Finds a single resource with the given name using the provided class loader.
     * 
     * @param resourceName the name of the resource to find
     * @param classLoader the class loader to use
     * @return an Optional containing the URL to the resource, or empty if the resource could not be found
     */
    public static Optional<URL> findResource(String resourceName, ClassLoader classLoader) {
        try {
            URL url = classLoader.getResource(resourceName);
            return Optional.ofNullable(url);
        } catch (Exception e) {
            logger.warn("Error finding resource: {}", resourceName, e);
            return Optional.empty();
        }
    }
    
    /**
     * Finds a single resource with the given name using the default class loader.
     * 
     * @param resourceName the name of the resource to find
     * @return an Optional containing the URL to the resource, or empty if the resource could not be found
     */
    public static Optional<URL> findResource(String resourceName) {
        return findResource(resourceName, getDefaultClassLoader());
    }
    
    /**
     * Filters a list of classes by a predicate.
     * 
     * @param classes the list of classes to filter
     * @param predicate the predicate to apply
     * @return a filtered list of classes
     */
    public static List<Class<?>> filterClasses(List<Class<?>> classes, Predicate<Class<?>> predicate) {
        if (classes == null || predicate == null) {
            return List.of();
        }
        
        return classes.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the parent class loader of a class loader.
     * 
     * @param classLoader the class loader
     * @return an Optional containing the parent class loader, or empty if there is no parent
     */
    public static Optional<ClassLoader> getParentClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(classLoader.getParent());
    }
    
    /**
     * Gets the class loader hierarchy for a class loader.
     * 
     * @param classLoader the class loader
     * @return a list of class loaders in the hierarchy, starting with the provided class loader
     */
    public static List<ClassLoader> getClassLoaderHierarchy(ClassLoader classLoader) {
        List<ClassLoader> hierarchy = new ArrayList<>();
        
        ClassLoader current = classLoader;
        while (current != null) {
            hierarchy.add(current);
            current = current.getParent();
        }
        
        return hierarchy;
    }
}
