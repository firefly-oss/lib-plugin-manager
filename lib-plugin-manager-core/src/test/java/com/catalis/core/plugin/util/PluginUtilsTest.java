package com.catalis.core.plugin.util;

import com.catalis.core.plugin.model.PluginDescriptor;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.model.PluginState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PluginUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "plugin-id.jar,plugin-id",
            "plugin-id-1.0.0.jar,plugin-id",
            "plugin-id-1.0.0-SNAPSHOT.jar,plugin-id-1.0.0-SNAPSHOT",
            "plugin.id.jar,plugin.id",
            "plugin-id,plugin-id"
    })
    void extractPluginIdFromFileName_shouldExtractCorrectId(String fileName, String expectedId) {
        assertEquals(expectedId, PluginUtils.extractPluginIdFromFileName(fileName));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void extractPluginIdFromFileName_shouldReturnNullForNullOrEmpty(String fileName) {
        assertNull(PluginUtils.extractPluginIdFromFileName(fileName));
    }

    @Test
    void filterPluginsByState_shouldFilterCorrectly() {
        PluginDescriptor plugin1 = createPluginDescriptor("plugin1", PluginState.STARTED);
        PluginDescriptor plugin2 = createPluginDescriptor("plugin2", PluginState.STOPPED);
        PluginDescriptor plugin3 = createPluginDescriptor("plugin3", PluginState.STARTED);
        
        List<PluginDescriptor> plugins = Arrays.asList(plugin1, plugin2, plugin3);
        
        List<PluginDescriptor> startedPlugins = PluginUtils.filterPluginsByState(plugins, PluginState.STARTED);
        assertEquals(2, startedPlugins.size());
        assertTrue(startedPlugins.contains(plugin1));
        assertTrue(startedPlugins.contains(plugin3));
        
        List<PluginDescriptor> stoppedPlugins = PluginUtils.filterPluginsByState(plugins, PluginState.STOPPED);
        assertEquals(1, stoppedPlugins.size());
        assertTrue(stoppedPlugins.contains(plugin2));
    }

    @Test
    void filterPluginsByState_shouldHandleNullInput() {
        assertTrue(PluginUtils.filterPluginsByState(null, PluginState.STARTED).isEmpty());
        
        List<PluginDescriptor> plugins = Arrays.asList(
                createPluginDescriptor("plugin1", PluginState.STARTED),
                createPluginDescriptor("plugin2", PluginState.STOPPED)
        );
        
        assertTrue(PluginUtils.filterPluginsByState(plugins, null).isEmpty());
    }

    @Test
    void findPluginById_shouldFindCorrectPlugin() {
        PluginDescriptor plugin1 = createPluginDescriptor("plugin1", PluginState.STARTED);
        PluginDescriptor plugin2 = createPluginDescriptor("plugin2", PluginState.STOPPED);
        
        List<PluginDescriptor> plugins = Arrays.asList(plugin1, plugin2);
        
        Optional<PluginDescriptor> found = PluginUtils.findPluginById(plugins, "plugin1");
        assertTrue(found.isPresent());
        assertEquals(plugin1, found.get());
        
        found = PluginUtils.findPluginById(plugins, "plugin3");
        assertFalse(found.isPresent());
    }

    @Test
    void findPluginById_shouldHandleNullInput() {
        assertTrue(PluginUtils.findPluginById(null, "plugin1").isEmpty());
        
        List<PluginDescriptor> plugins = Arrays.asList(
                createPluginDescriptor("plugin1", PluginState.STARTED),
                createPluginDescriptor("plugin2", PluginState.STOPPED)
        );
        
        assertTrue(PluginUtils.findPluginById(plugins, null).isEmpty());
        assertTrue(PluginUtils.findPluginById(plugins, "").isEmpty());
    }

    @Test
    void createDescriptorFromMetadata_shouldCreateCorrectDescriptor() {
        PluginMetadata metadata = PluginMetadata.builder()
                .id("plugin-id")
                .name("Plugin Name")
                .version("1.0.0")
                .description("Plugin Description")
                .provider("Provider")
                .license("License")
                .dependencies(new String[]{"dep1", "dep2"})
                .build();
        
        PluginDescriptor descriptor = PluginUtils.createDescriptorFromMetadata(metadata);
        
        assertEquals("plugin-id", descriptor.getId());
        assertEquals("Plugin Name", descriptor.getName());
        assertEquals("1.0.0", descriptor.getVersion());
        assertEquals("Plugin Description", descriptor.getDescription());
        assertEquals("Provider", descriptor.getProvider());
        assertEquals("License", descriptor.getLicense());
        assertEquals(PluginState.REGISTERED, descriptor.getState());
        assertArrayEquals(new String[]{"dep1", "dep2"}, descriptor.getDependencies());
    }

    @Test
    void createDescriptorFromMetadata_shouldThrowExceptionForNullMetadata() {
        assertThrows(IllegalArgumentException.class, () -> PluginUtils.createDescriptorFromMetadata(null));
    }

    @Test
    void resolvePluginPath_shouldResolveCorrectly() {
        Path baseDir = Paths.get("/base/dir");
        
        // Absolute path
        Path absolutePath = PluginUtils.resolvePluginPath(baseDir, "/absolute/path/plugin.jar");
        assertEquals(Paths.get("/absolute/path/plugin.jar"), absolutePath);
        
        // Relative path
        Path relativePath = PluginUtils.resolvePluginPath(baseDir, "relative/path/plugin.jar");
        assertEquals(Paths.get("/base/dir/relative/path/plugin.jar"), relativePath);
    }

    @Test
    void resolvePluginPath_shouldThrowExceptionForNullOrEmptyPath() {
        Path baseDir = Paths.get("/base/dir");
        
        assertThrows(IllegalArgumentException.class, () -> PluginUtils.resolvePluginPath(baseDir, null));
        assertThrows(IllegalArgumentException.class, () -> PluginUtils.resolvePluginPath(baseDir, ""));
    }

    @Test
    void formatPluginInfo_shouldFormatCorrectly() {
        PluginDescriptor descriptor = createPluginDescriptor("plugin-id", PluginState.STARTED);
        
        String formatted = PluginUtils.formatPluginInfo(descriptor);
        assertEquals("Plugin[id=plugin-id, name=Plugin Name, version=1.0.0, state=STARTED]", formatted);
    }

    @Test
    void formatPluginInfo_shouldHandleNullDescriptor() {
        assertEquals("null", PluginUtils.formatPluginInfo(null));
    }

    @Test
    void groupPluginsByState_shouldGroupCorrectly() {
        PluginDescriptor plugin1 = createPluginDescriptor("plugin1", PluginState.STARTED);
        PluginDescriptor plugin2 = createPluginDescriptor("plugin2", PluginState.STOPPED);
        PluginDescriptor plugin3 = createPluginDescriptor("plugin3", PluginState.STARTED);
        PluginDescriptor plugin4 = createPluginDescriptor("plugin4", PluginState.FAILED);
        
        List<PluginDescriptor> plugins = Arrays.asList(plugin1, plugin2, plugin3, plugin4);
        
        Map<PluginState, List<PluginDescriptor>> grouped = PluginUtils.groupPluginsByState(plugins);
        
        assertEquals(3, grouped.size());
        assertEquals(2, grouped.get(PluginState.STARTED).size());
        assertEquals(1, grouped.get(PluginState.STOPPED).size());
        assertEquals(1, grouped.get(PluginState.FAILED).size());
        
        assertTrue(grouped.get(PluginState.STARTED).contains(plugin1));
        assertTrue(grouped.get(PluginState.STARTED).contains(plugin3));
        assertTrue(grouped.get(PluginState.STOPPED).contains(plugin2));
        assertTrue(grouped.get(PluginState.FAILED).contains(plugin4));
    }

    @Test
    void groupPluginsByState_shouldHandleNullInput() {
        assertTrue(PluginUtils.groupPluginsByState(null).isEmpty());
    }

    private PluginDescriptor createPluginDescriptor(String id, PluginState state) {
        return PluginDescriptor.builder()
                .id(id)
                .name("Plugin Name")
                .version("1.0.0")
                .description("Plugin Description")
                .provider("Provider")
                .license("License")
                .state(state)
                .dependencies(new String[]{"dep1", "dep2"})
                .build();
    }
}
