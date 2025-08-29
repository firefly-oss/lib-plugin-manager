package com.firefly.core.plugin.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PluginPermissionTest {

    @Test
    void testConstructors() {
        // Test constructor with type, target, and action
        PluginPermission permission1 = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp", "read");
        assertEquals(PluginPermission.Type.FILE_SYSTEM, permission1.getType());
        assertEquals("/tmp", permission1.getTarget());
        assertEquals("read", permission1.getAction());

        // Test constructor with type and target
        PluginPermission permission2 = new PluginPermission(
                PluginPermission.Type.NETWORK, "localhost");
        assertEquals(PluginPermission.Type.NETWORK, permission2.getType());
        assertEquals("localhost", permission2.getTarget());
        assertNull(permission2.getAction());

        // Test constructor with type only
        PluginPermission permission3 = new PluginPermission(
                PluginPermission.Type.SYSTEM_PROPERTIES);
        assertEquals(PluginPermission.Type.SYSTEM_PROPERTIES, permission3.getType());
        assertNull(permission3.getTarget());
        assertNull(permission3.getAction());
    }

    @Test
    void testImplies() {
        // Test exact match
        PluginPermission permission1 = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp", "read");
        PluginPermission permission2 = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp", "read");
        assertTrue(permission1.implies(permission2));
        assertTrue(permission2.implies(permission1));

        // Test more general permission implies more specific
        PluginPermission generalPermission = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM);
        PluginPermission specificPermission = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp", "read");
        assertTrue(generalPermission.implies(specificPermission));
        assertFalse(specificPermission.implies(generalPermission));

        // Test target prefix
        PluginPermission prefixPermission = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp");
        PluginPermission subPathPermission = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp/subdir", "read");
        assertTrue(prefixPermission.implies(subPathPermission));
        assertFalse(subPathPermission.implies(prefixPermission));

        // Test different types
        PluginPermission filePermission = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp");
        PluginPermission networkPermission = new PluginPermission(
                PluginPermission.Type.NETWORK, "localhost");
        assertFalse(filePermission.implies(networkPermission));
        assertFalse(networkPermission.implies(filePermission));

        // Test null permission
        assertFalse(filePermission.implies(null));
    }

    @Test
    void testEqualsAndHashCode() {
        PluginPermission permission1 = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp", "read");
        PluginPermission permission2 = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp", "read");
        PluginPermission permission3 = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp", "write");

        // Test equals
        assertEquals(permission1, permission2);
        assertNotEquals(permission1, permission3);
        assertNotEquals(permission1, null);
        assertNotEquals(permission1, "not a permission");

        // Test hashCode
        assertEquals(permission1.hashCode(), permission2.hashCode());
        assertNotEquals(permission1.hashCode(), permission3.hashCode());
    }

    @Test
    void testToString() {
        PluginPermission permission1 = new PluginPermission(
                PluginPermission.Type.FILE_SYSTEM, "/tmp", "read");
        assertEquals("FILE_SYSTEM:/tmp:read", permission1.toString());

        PluginPermission permission2 = new PluginPermission(
                PluginPermission.Type.NETWORK);
        assertEquals("NETWORK", permission2.toString());

        PluginPermission permission3 = new PluginPermission(
                PluginPermission.Type.SYSTEM_PROPERTIES, "user.home");
        assertEquals("SYSTEM_PROPERTIES:user.home", permission3.toString());
    }
}
