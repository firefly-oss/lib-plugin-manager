package com.catalis.core.plugin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security manager for plugins that enforces permissions.
 * This class allows fine-grained control over what operations plugins can perform.
 */
public class PluginSecurityManager {

    private static final Logger logger = LoggerFactory.getLogger(PluginSecurityManager.class);

    private final Map<String, Set<PluginPermission>> pluginPermissions = new ConcurrentHashMap<>();
    private final Map<ClassLoader, String> classLoaderToPluginId = new ConcurrentHashMap<>();
    private final boolean enforcePermissions;

    /**
     * Creates a new PluginSecurityManager.
     *
     * @param enforcePermissions whether to enforce permissions
     */
    public PluginSecurityManager(boolean enforcePermissions) {
        this.enforcePermissions = enforcePermissions;
        logger.info("Plugin security manager initialized with enforcePermissions={}", enforcePermissions);
    }

    /**
     * Registers a plugin with the security manager.
     *
     * @param pluginId the ID of the plugin
     * @param classLoader the class loader of the plugin
     * @param permissions the permissions granted to the plugin
     */
    public void registerPlugin(String pluginId, ClassLoader classLoader, Set<PluginPermission> permissions) {
        if (classLoader instanceof PluginClassLoader) {
            classLoaderToPluginId.put(classLoader, pluginId);
            pluginPermissions.put(pluginId, new HashSet<>(permissions));
            logger.debug("Registered plugin {} with {} permissions", pluginId, permissions.size());
        } else {
            logger.warn("Attempted to register non-plugin class loader for plugin {}", pluginId);
        }
    }

    /**
     * Unregisters a plugin from the security manager.
     *
     * @param pluginId the ID of the plugin
     * @param classLoader the class loader of the plugin
     */
    public void unregisterPlugin(String pluginId, ClassLoader classLoader) {
        classLoaderToPluginId.remove(classLoader);
        pluginPermissions.remove(pluginId);
        logger.debug("Unregistered plugin {}", pluginId);
    }

    /**
     * Adds a permission to a plugin.
     *
     * @param pluginId the ID of the plugin
     * @param permission the permission to add
     */
    public void addPermission(String pluginId, PluginPermission permission) {
        pluginPermissions.computeIfAbsent(pluginId, k -> new HashSet<>()).add(permission);
        logger.debug("Added permission {} to plugin {}", permission, pluginId);
    }

    /**
     * Removes a permission from a plugin.
     *
     * @param pluginId the ID of the plugin
     * @param permission the permission to remove
     */
    public void removePermission(String pluginId, PluginPermission permission) {
        Set<PluginPermission> permissions = pluginPermissions.get(pluginId);
        if (permissions != null) {
            permissions.remove(permission);
            logger.debug("Removed permission {} from plugin {}", permission, pluginId);
        }
    }

    /**
     * Gets the ID of the plugin that is currently executing.
     *
     * @return the plugin ID, or null if the current code is not from a plugin
     */
    private String getCurrentPluginId() {
        // Get the current thread's stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // Create a list of classes from the stack trace
        for (StackTraceElement element : stackTrace) {
            try {
                Class<?> cls = Class.forName(element.getClassName());
                ClassLoader loader = cls.getClassLoader();
                if (loader != null) {
                    String pluginId = classLoaderToPluginId.get(loader);
                    if (pluginId != null) {
                        return pluginId;
                    }
                }
            } catch (ClassNotFoundException e) {
                // Ignore and continue
            }
        }

        return null;
    }

    /**
     * Checks if the current plugin has the specified permission.
     *
     * @param permission the permission to check
     * @return true if the plugin has the permission, false otherwise
     */
    private boolean hasPermission(String pluginId, PluginPermission permission) {
        if (pluginId == null) {
            // Not a plugin, allow the operation
            return true;
        }

        Set<PluginPermission> permissions = pluginPermissions.get(pluginId);
        if (permissions == null) {
            logger.warn("Plugin {} has no registered permissions", pluginId);
            return false;
        }

        // Check if any of the plugin's permissions imply the requested permission
        for (PluginPermission p : permissions) {
            if (p.implies(permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the current plugin has permission to perform the specified operation.
     *
     * @param perm the permission to check
     * @throws SecurityException if the plugin does not have the permission
     */
    public void checkPermission(Permission perm) {
        if (!enforcePermissions) {
            return;
        }

        String pluginId = getCurrentPluginId();
        if (pluginId == null) {
            // Not a plugin, allow the operation
            return;
        }

        // Map Java security permissions to plugin permissions
        PluginPermission pluginPerm = mapToPluginPermission(perm);
        if (pluginPerm == null) {
            // No mapping, allow the operation
            return;
        }

        if (!hasPermission(pluginId, pluginPerm)) {
            logger.warn("Plugin {} attempted unauthorized operation: {}", pluginId, perm);
            throw new SecurityException("Plugin " + pluginId + " does not have permission: " + pluginPerm);
        }
    }

    /**
     * Maps a Java security permission to a plugin permission.
     *
     * @param perm the Java security permission
     * @return the equivalent plugin permission, or null if there is no mapping
     */
    private PluginPermission mapToPluginPermission(Permission perm) {
        if (perm instanceof FilePermission) {
            return new PluginPermission(PluginPermission.Type.FILE_SYSTEM, perm.getName(), perm.getActions());
        } else if (perm instanceof SocketPermission) {
            return new PluginPermission(PluginPermission.Type.NETWORK, perm.getName(), perm.getActions());
        } else if (perm.getName().startsWith("getProperty.") || perm.getName().startsWith("setProperty.")) {
            return new PluginPermission(PluginPermission.Type.SYSTEM_PROPERTIES, perm.getName(), perm.getActions());
        } else if (perm.getName().startsWith("accessDeclaredMembers") || perm.getName().startsWith("suppressAccessChecks")) {
            return new PluginPermission(PluginPermission.Type.REFLECTION, perm.getName(), perm.getActions());
        } else if (perm.getName().equals("modifyThread") || perm.getName().equals("modifyThreadGroup")) {
            return new PluginPermission(PluginPermission.Type.THREAD, perm.getName(), perm.getActions());
        } else if (perm.getName().equals("loadLibrary")) {
            return new PluginPermission(PluginPermission.Type.NATIVE_CODE, perm.getName(), perm.getActions());
        } else if (perm.getName().equals("exec")) {
            return new PluginPermission(PluginPermission.Type.EXECUTE, perm.getName(), perm.getActions());
        }

        return null;
    }
}
