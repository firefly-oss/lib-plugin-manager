package com.firefly.core.plugin.util;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class ClassLoaderUtilsTest {

    @Test
    void getDefaultClassLoader_shouldReturnNonNullClassLoader() {
        ClassLoader classLoader = ClassLoaderUtils.getDefaultClassLoader();
        assertNotNull(classLoader);
    }

    @Test
    void loadClass_shouldLoadExistingClass() {
        Optional<Class<?>> stringClass = ClassLoaderUtils.loadClass("java.lang.String");
        assertTrue(stringClass.isPresent());
        assertEquals(String.class, stringClass.get());
        
        Optional<Class<?>> thisClass = ClassLoaderUtils.loadClass(ClassLoaderUtilsTest.class.getName());
        assertTrue(thisClass.isPresent());
        assertEquals(ClassLoaderUtilsTest.class, thisClass.get());
    }

    @Test
    void loadClass_shouldReturnEmptyForNonExistentClass() {
        Optional<Class<?>> nonExistentClass = ClassLoaderUtils.loadClass("com.example.NonExistentClass");
        assertFalse(nonExistentClass.isPresent());
    }

    @Test
    void loadClass_withClassLoader_shouldLoadExistingClass() {
        ClassLoader classLoader = ClassLoaderUtils.getDefaultClassLoader();
        
        Optional<Class<?>> stringClass = ClassLoaderUtils.loadClass("java.lang.String", classLoader);
        assertTrue(stringClass.isPresent());
        assertEquals(String.class, stringClass.get());
    }

    @Test
    void createInstance_shouldCreateInstanceOfClass() {
        Optional<String> stringInstance = ClassLoaderUtils.createInstance(String.class);
        assertTrue(stringInstance.isPresent());
        assertEquals("", stringInstance.get());
    }

    @Test
    void createInstance_shouldReturnEmptyForClassWithoutDefaultConstructor() {
        // Integer doesn't have a no-arg constructor
        Optional<Integer> integerInstance = ClassLoaderUtils.createInstance(Integer.class);
        assertFalse(integerInstance.isPresent());
    }

    @Test
    void findResources_shouldFindExistingResources() {
        // This resource should exist in the classpath
        List<URL> resources = ClassLoaderUtils.findResources("META-INF/MANIFEST.MF");
        assertFalse(resources.isEmpty());
    }

    @Test
    void findResources_shouldReturnEmptyListForNonExistentResources() {
        List<URL> resources = ClassLoaderUtils.findResources("non/existent/resource.txt");
        assertTrue(resources.isEmpty());
    }

    @Test
    void findResources_withClassLoader_shouldFindExistingResources() {
        ClassLoader classLoader = ClassLoaderUtils.getDefaultClassLoader();
        
        List<URL> resources = ClassLoaderUtils.findResources("META-INF/MANIFEST.MF", classLoader);
        assertFalse(resources.isEmpty());
    }

    @Test
    void findResource_shouldFindExistingResource() {
        Optional<URL> resource = ClassLoaderUtils.findResource("META-INF/MANIFEST.MF");
        assertTrue(resource.isPresent());
    }

    @Test
    void findResource_shouldReturnEmptyForNonExistentResource() {
        Optional<URL> resource = ClassLoaderUtils.findResource("non/existent/resource.txt");
        assertFalse(resource.isPresent());
    }

    @Test
    void findResource_withClassLoader_shouldFindExistingResource() {
        ClassLoader classLoader = ClassLoaderUtils.getDefaultClassLoader();
        
        Optional<URL> resource = ClassLoaderUtils.findResource("META-INF/MANIFEST.MF", classLoader);
        assertTrue(resource.isPresent());
    }

    @Test
    void filterClasses_shouldFilterClassesCorrectly() {
        List<Class<?>> classes = List.of(String.class, Integer.class, Double.class);
        
        Predicate<Class<?>> isNumberClass = clazz -> Number.class.isAssignableFrom(clazz);
        List<Class<?>> numberClasses = ClassLoaderUtils.filterClasses(classes, isNumberClass);
        
        assertEquals(2, numberClasses.size());
        assertTrue(numberClasses.contains(Integer.class));
        assertTrue(numberClasses.contains(Double.class));
        assertFalse(numberClasses.contains(String.class));
    }

    @Test
    void filterClasses_shouldHandleNullInput() {
        List<Class<?>> filteredClasses = ClassLoaderUtils.filterClasses(null, clazz -> true);
        assertTrue(filteredClasses.isEmpty());
        
        List<Class<?>> classes = List.of(String.class, Integer.class);
        filteredClasses = ClassLoaderUtils.filterClasses(classes, null);
        assertTrue(filteredClasses.isEmpty());
    }

    @Test
    void getParentClassLoader_shouldReturnParentClassLoader() {
        // Create a class loader with a known parent
        ClassLoader parent = ClassLoaderUtils.getDefaultClassLoader();
        ClassLoader child = new TestClassLoader(parent);
        
        Optional<ClassLoader> retrievedParent = ClassLoaderUtils.getParentClassLoader(child);
        assertTrue(retrievedParent.isPresent());
        assertEquals(parent, retrievedParent.get());
    }

    @Test
    void getParentClassLoader_shouldReturnEmptyForNullInput() {
        Optional<ClassLoader> parent = ClassLoaderUtils.getParentClassLoader(null);
        assertFalse(parent.isPresent());
    }

    @Test
    void getClassLoaderHierarchy_shouldReturnCorrectHierarchy() {
        // Create a class loader hierarchy
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        ClassLoader level1 = new TestClassLoader(systemClassLoader);
        ClassLoader level2 = new TestClassLoader(level1);
        
        List<ClassLoader> hierarchy = ClassLoaderUtils.getClassLoaderHierarchy(level2);
        
        assertFalse(hierarchy.isEmpty());
        assertEquals(level2, hierarchy.get(0));
        assertEquals(level1, hierarchy.get(1));
        assertEquals(systemClassLoader, hierarchy.get(2));
    }

    @Test
    void getClassLoaderHierarchy_shouldHandleNullInput() {
        List<ClassLoader> hierarchy = ClassLoaderUtils.getClassLoaderHierarchy(null);
        assertTrue(hierarchy.isEmpty());
    }

    // Simple test class loader for testing
    private static class TestClassLoader extends ClassLoader {
        public TestClassLoader(ClassLoader parent) {
            super(parent);
        }
    }
}
