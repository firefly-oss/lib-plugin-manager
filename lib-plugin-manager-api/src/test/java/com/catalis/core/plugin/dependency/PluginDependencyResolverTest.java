package com.catalis.core.plugin.dependency;

import com.catalis.core.plugin.exception.CircularDependencyException;
import com.catalis.core.plugin.exception.DependencyNotFoundException;
import com.catalis.core.plugin.exception.IncompatibleDependencyException;
import com.catalis.core.plugin.model.PluginDescriptor;
import com.catalis.core.plugin.model.PluginMetadata;
import com.catalis.core.plugin.model.PluginState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PluginDependencyResolverTest {

    private PluginDependencyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PluginDependencyResolver();
    }

    @Test
    void testVersionComparison() {
        // Equal versions
        assertEquals(0, resolver.compareVersions("1.0.0", "1.0.0"));

        // Simple comparisons
        assertTrue(resolver.compareVersions("1.0.1", "1.0.0") > 0);
        assertTrue(resolver.compareVersions("1.1.0", "1.0.0") > 0);
        assertTrue(resolver.compareVersions("2.0.0", "1.0.0") > 0);
        assertTrue(resolver.compareVersions("0.9.0", "1.0.0") < 0);

        // Different length versions
        assertTrue(resolver.compareVersions("1.0.0.1", "1.0.0") > 0);
        assertEquals(0, resolver.compareVersions("1.0", "1.0.0"));

        // Pre-release versions - these are handled by extracting the numeric prefix
        // so 1.0.0 and 1.0.0-SNAPSHOT both become 1.0.0 and are equal
        assertEquals(0, resolver.compareVersions("1.0.0", "1.0.0-SNAPSHOT"));
        assertEquals(0, resolver.compareVersions("1.0.0-alpha", "1.0.0-SNAPSHOT"));
    }

    @Test
    void testVersionConstraints() {
        // Equal constraint
        assertTrue(resolver.satisfiesVersionConstraint("1.0.0", "=1.0.0"));
        assertTrue(resolver.satisfiesVersionConstraint("1.0.0", "==1.0.0"));
        assertFalse(resolver.satisfiesVersionConstraint("1.0.1", "=1.0.0"));

        // Greater than constraint
        assertTrue(resolver.satisfiesVersionConstraint("1.0.1", ">1.0.0"));
        assertFalse(resolver.satisfiesVersionConstraint("1.0.0", ">1.0.0"));
        assertFalse(resolver.satisfiesVersionConstraint("0.9.0", ">1.0.0"));

        // Greater than or equal constraint
        assertTrue(resolver.satisfiesVersionConstraint("1.0.1", ">=1.0.0"));
        assertTrue(resolver.satisfiesVersionConstraint("1.0.0", ">=1.0.0"));
        assertFalse(resolver.satisfiesVersionConstraint("0.9.0", ">=1.0.0"));

        // Less than constraint
        assertTrue(resolver.satisfiesVersionConstraint("0.9.0", "<1.0.0"));
        assertFalse(resolver.satisfiesVersionConstraint("1.0.0", "<1.0.0"));
        assertFalse(resolver.satisfiesVersionConstraint("1.0.1", "<1.0.0"));

        // Less than or equal constraint
        assertTrue(resolver.satisfiesVersionConstraint("0.9.0", "<=1.0.0"));
        assertTrue(resolver.satisfiesVersionConstraint("1.0.0", "<=1.0.0"));
        assertFalse(resolver.satisfiesVersionConstraint("1.0.1", "<=1.0.0"));
    }

    @Test
    void testSimpleDependencyResolution() {
        // Create plugins with simple dependencies
        PluginDescriptor plugin1 = createPlugin("plugin1", "1.0.0", Set.of());
        PluginDescriptor plugin2 = createPlugin("plugin2", "1.0.0", Set.of("plugin1"));
        PluginDescriptor plugin3 = createPlugin("plugin3", "1.0.0", Set.of("plugin2"));

        List<PluginDescriptor> plugins = List.of(plugin3, plugin1, plugin2);
        List<PluginDescriptor> orderedPlugins = resolver.resolveDependencies(plugins);

        // Check that the order is correct
        assertEquals(3, orderedPlugins.size());
        assertEquals("plugin1", orderedPlugins.get(0).getId());
        assertEquals("plugin2", orderedPlugins.get(1).getId());
        assertEquals("plugin3", orderedPlugins.get(2).getId());
    }

    @Test
    void testComplexDependencyResolution() {
        // Create plugins with more complex dependencies
        PluginDescriptor plugin1 = createPlugin("plugin1", "1.0.0", Set.of());
        PluginDescriptor plugin2 = createPlugin("plugin2", "1.0.0", Set.of("plugin1"));
        PluginDescriptor plugin3 = createPlugin("plugin3", "1.0.0", Set.of("plugin1"));
        PluginDescriptor plugin4 = createPlugin("plugin4", "1.0.0", Set.of("plugin2", "plugin3"));
        PluginDescriptor plugin5 = createPlugin("plugin5", "1.0.0", Set.of("plugin4"));

        List<PluginDescriptor> plugins = List.of(plugin5, plugin4, plugin3, plugin2, plugin1);
        List<PluginDescriptor> orderedPlugins = resolver.resolveDependencies(plugins);

        // Check that the order is correct
        assertEquals(5, orderedPlugins.size());
        assertEquals("plugin1", orderedPlugins.get(0).getId());

        // plugin2 and plugin3 can be in any order since they both depend only on plugin1
        assertTrue(
            (orderedPlugins.get(1).getId().equals("plugin2") && orderedPlugins.get(2).getId().equals("plugin3")) ||
            (orderedPlugins.get(1).getId().equals("plugin3") && orderedPlugins.get(2).getId().equals("plugin2"))
        );

        assertEquals("plugin4", orderedPlugins.get(3).getId());
        assertEquals("plugin5", orderedPlugins.get(4).getId());
    }

    @Test
    void testCircularDependencyDetection() {
        // Create plugins with circular dependencies
        PluginDescriptor plugin1 = createPlugin("plugin1", "1.0.0", Set.of("plugin3"));
        PluginDescriptor plugin2 = createPlugin("plugin2", "1.0.0", Set.of("plugin1"));
        PluginDescriptor plugin3 = createPlugin("plugin3", "1.0.0", Set.of("plugin2"));

        List<PluginDescriptor> plugins = List.of(plugin1, plugin2, plugin3);

        // This should throw a CircularDependencyException
        assertThrows(CircularDependencyException.class, () -> resolver.resolveDependencies(plugins));
    }

    @Test
    void testMissingDependency() {
        // Create plugins with a missing dependency
        PluginDescriptor plugin1 = createPlugin("plugin1", "1.0.0", Set.of("missing-plugin"));
        PluginDescriptor plugin2 = createPlugin("plugin2", "1.0.0", Set.of("plugin1"));

        List<PluginDescriptor> plugins = List.of(plugin1, plugin2);

        // This should throw a DependencyNotFoundException
        assertThrows(DependencyNotFoundException.class, () -> resolver.resolveDependencies(plugins));
    }

    @Test
    void testOptionalDependency() {
        // Create plugins with an optional dependency
        PluginDescriptor plugin1 = createPlugin("plugin1", "1.0.0", Set.of("?missing-plugin"));
        PluginDescriptor plugin2 = createPlugin("plugin2", "1.0.0", Set.of("plugin1"));

        List<PluginDescriptor> plugins = List.of(plugin1, plugin2);

        // This should not throw an exception
        List<PluginDescriptor> orderedPlugins = resolver.resolveDependencies(plugins);

        // Check that the order is correct
        assertEquals(2, orderedPlugins.size());
        assertEquals("plugin1", orderedPlugins.get(0).getId());
        assertEquals("plugin2", orderedPlugins.get(1).getId());
    }

    @Test
    void testVersionConstraints_Compatible() {
        // Create plugins with version constraints
        PluginDescriptor plugin1 = createPlugin("plugin1", "1.0.0", Set.of());
        PluginDescriptor plugin2 = createPlugin("plugin2", "1.0.0", Set.of("plugin1>=1.0.0"));

        List<PluginDescriptor> plugins = List.of(plugin1, plugin2);

        // This should not throw an exception
        List<PluginDescriptor> orderedPlugins = resolver.resolveDependencies(plugins);

        // Check that the order is correct
        assertEquals(2, orderedPlugins.size());
        assertEquals("plugin1", orderedPlugins.get(0).getId());
        assertEquals("plugin2", orderedPlugins.get(1).getId());
    }

    @Test
    void testVersionConstraints_Incompatible() {
        // Create plugins with incompatible version constraints
        PluginDescriptor plugin1 = createPlugin("plugin1", "1.0.0", Set.of());
        PluginDescriptor plugin2 = createPlugin("plugin2", "1.0.0", Set.of("plugin1>=2.0.0"));

        List<PluginDescriptor> plugins = List.of(plugin1, plugin2);

        // This should throw an IncompatibleDependencyException
        assertThrows(IncompatibleDependencyException.class, () -> resolver.resolveDependencies(plugins));
    }

    /**
     * Helper method to create a plugin descriptor for testing.
     */
    private PluginDescriptor createPlugin(String id, String version, Set<String> dependencies) {
        PluginMetadata metadata = PluginMetadata.builder()
                .id(id)
                .name("Plugin " + id)
                .version(version)
                .description("Test plugin " + id)
                .dependencies(dependencies)
                .installTime(Instant.now())
                .build();

        return new PluginDescriptor(
                metadata,
                PluginState.INSTALLED,
                Collections.emptyMap(),
                "test-classloader",
                "test-location"
        );
    }
}
