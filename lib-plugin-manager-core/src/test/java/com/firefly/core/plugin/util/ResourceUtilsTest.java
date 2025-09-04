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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ResourceUtilsTest {

    @TempDir
    Path tempDir;
    
    private Path testFile;
    private Path testPropertiesFile;
    private Path testDirectory;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create a test file
        testFile = tempDir.resolve("test-file.txt");
        Files.writeString(testFile, "Line 1\nLine 2\nLine 3", StandardCharsets.UTF_8);
        
        // Create a test properties file
        testPropertiesFile = tempDir.resolve("test.properties");
        String propertiesContent = "key1=value1\nkey2=value2\nkey3=value3";
        Files.writeString(testPropertiesFile, propertiesContent, StandardCharsets.UTF_8);
        
        // Create a test directory with files
        testDirectory = tempDir.resolve("test-dir");
        Files.createDirectory(testDirectory);
        Files.writeString(testDirectory.resolve("file1.txt"), "File 1 content", StandardCharsets.UTF_8);
        Files.writeString(testDirectory.resolve("file2.txt"), "File 2 content", StandardCharsets.UTF_8);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up any additional files created during tests
        Files.deleteIfExists(tempDir.resolve("new-file.txt"));
        Files.deleteIfExists(tempDir.resolve("new-dir"));
    }

    @Test
    void readFileAsString_shouldReadFileContent() {
        Optional<String> content = ResourceUtils.readFileAsString(testFile);
        assertTrue(content.isPresent());
        assertEquals("Line 1\nLine 2\nLine 3", content.get());
    }

    @Test
    void readFileAsString_shouldReturnEmptyForNonExistentFile() {
        Optional<String> content = ResourceUtils.readFileAsString(tempDir.resolve("non-existent.txt"));
        assertFalse(content.isPresent());
    }

    @Test
    void readFileAsLines_shouldReadFileLines() {
        Optional<List<String>> lines = ResourceUtils.readFileAsLines(testFile);
        assertTrue(lines.isPresent());
        assertEquals(3, lines.get().size());
        assertEquals("Line 1", lines.get().get(0));
        assertEquals("Line 2", lines.get().get(1));
        assertEquals("Line 3", lines.get().get(2));
    }

    @Test
    void readFileAsLines_shouldReturnEmptyForNonExistentFile() {
        Optional<List<String>> lines = ResourceUtils.readFileAsLines(tempDir.resolve("non-existent.txt"));
        assertFalse(lines.isPresent());
    }

    @Test
    void writeStringToFile_shouldWriteContentToFile() {
        Path newFile = tempDir.resolve("new-file.txt");
        boolean result = ResourceUtils.writeStringToFile(newFile, "New content");
        
        assertTrue(result);
        assertTrue(Files.exists(newFile));
        
        try {
            String content = Files.readString(newFile, StandardCharsets.UTF_8);
            assertEquals("New content", content);
        } catch (IOException e) {
            fail("Failed to read file: " + e.getMessage());
        }
    }

    @Test
    void writeLinesToFile_shouldWriteLinesToFile() {
        Path newFile = tempDir.resolve("new-file.txt");
        List<String> lines = Arrays.asList("Line A", "Line B", "Line C");
        
        boolean result = ResourceUtils.writeLinesToFile(newFile, lines);
        
        assertTrue(result);
        assertTrue(Files.exists(newFile));
        
        try {
            List<String> readLines = Files.readAllLines(newFile, StandardCharsets.UTF_8);
            assertEquals(3, readLines.size());
            assertEquals("Line A", readLines.get(0));
            assertEquals("Line B", readLines.get(1));
            assertEquals("Line C", readLines.get(2));
        } catch (IOException e) {
            fail("Failed to read file: " + e.getMessage());
        }
    }

    @Test
    void loadProperties_shouldLoadPropertiesFromFile() {
        Optional<Properties> properties = ResourceUtils.loadProperties(testPropertiesFile);
        
        assertTrue(properties.isPresent());
        assertEquals("value1", properties.get().getProperty("key1"));
        assertEquals("value2", properties.get().getProperty("key2"));
        assertEquals("value3", properties.get().getProperty("key3"));
    }

    @Test
    void loadProperties_shouldReturnEmptyForNonExistentFile() {
        Optional<Properties> properties = ResourceUtils.loadProperties(tempDir.resolve("non-existent.properties"));
        assertFalse(properties.isPresent());
    }

    @Test
    void listFiles_shouldListFilesInDirectory() {
        List<Path> files = ResourceUtils.listFiles(testDirectory, path -> true);
        
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(path -> path.getFileName().toString().equals("file1.txt")));
        assertTrue(files.stream().anyMatch(path -> path.getFileName().toString().equals("file2.txt")));
    }

    @Test
    void listFiles_shouldFilterFilesCorrectly() {
        List<Path> files = ResourceUtils.listFiles(testDirectory, path -> path.getFileName().toString().equals("file1.txt"));
        
        assertEquals(1, files.size());
        assertEquals("file1.txt", files.get(0).getFileName().toString());
    }

    @Test
    void listFiles_shouldReturnEmptyListForNonExistentDirectory() {
        List<Path> files = ResourceUtils.listFiles(tempDir.resolve("non-existent-dir"), path -> true);
        assertTrue(files.isEmpty());
    }

    @Test
    void createDirectoryIfNotExists_shouldCreateDirectory() {
        Path newDir = tempDir.resolve("new-dir");
        
        boolean result = ResourceUtils.createDirectoryIfNotExists(newDir);
        
        assertTrue(result);
        assertTrue(Files.isDirectory(newDir));
    }

    @Test
    void createDirectoryIfNotExists_shouldReturnTrueForExistingDirectory() {
        boolean result = ResourceUtils.createDirectoryIfNotExists(testDirectory);
        
        assertTrue(result);
        assertTrue(Files.isDirectory(testDirectory));
    }

    @Test
    void delete_shouldDeleteFile() throws IOException {
        Path fileToDelete = tempDir.resolve("file-to-delete.txt");
        Files.writeString(fileToDelete, "Content to delete", StandardCharsets.UTF_8);
        
        boolean result = ResourceUtils.delete(fileToDelete);
        
        assertTrue(result);
        assertFalse(Files.exists(fileToDelete));
    }

    @Test
    void delete_shouldDeleteDirectoryRecursively() throws IOException {
        Path dirToDelete = tempDir.resolve("dir-to-delete");
        Files.createDirectory(dirToDelete);
        Files.writeString(dirToDelete.resolve("file1.txt"), "Content 1", StandardCharsets.UTF_8);
        Files.writeString(dirToDelete.resolve("file2.txt"), "Content 2", StandardCharsets.UTF_8);
        
        boolean result = ResourceUtils.delete(dirToDelete);
        
        assertTrue(result);
        assertFalse(Files.exists(dirToDelete));
    }

    @Test
    void copy_shouldCopyFile() throws IOException {
        Path destination = tempDir.resolve("copied-file.txt");
        
        boolean result = ResourceUtils.copy(testFile, destination);
        
        assertTrue(result);
        assertTrue(Files.exists(destination));
        assertEquals(
                Files.readString(testFile, StandardCharsets.UTF_8),
                Files.readString(destination, StandardCharsets.UTF_8)
        );
    }

    @Test
    void copy_shouldCopyDirectoryRecursively() throws IOException {
        Path destination = tempDir.resolve("copied-dir");
        
        boolean result = ResourceUtils.copy(testDirectory, destination);
        
        assertTrue(result);
        assertTrue(Files.isDirectory(destination));
        assertTrue(Files.exists(destination.resolve("file1.txt")));
        assertTrue(Files.exists(destination.resolve("file2.txt")));
        assertEquals(
                Files.readString(testDirectory.resolve("file1.txt"), StandardCharsets.UTF_8),
                Files.readString(destination.resolve("file1.txt"), StandardCharsets.UTF_8)
        );
        assertEquals(
                Files.readString(testDirectory.resolve("file2.txt"), StandardCharsets.UTF_8),
                Files.readString(destination.resolve("file2.txt"), StandardCharsets.UTF_8)
        );
    }
}
