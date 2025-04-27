package com.catalis.core.plugin.security;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a permission that can be granted to a plugin.
 * Permissions control what operations a plugin is allowed to perform.
 */
public class PluginPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Permission types that can be granted to plugins.
     */
    public enum Type {
        /**
         * Permission to access the file system.
         */
        FILE_SYSTEM,

        /**
         * Permission to access the network.
         */
        NETWORK,

        /**
         * Permission to access system properties.
         */
        SYSTEM_PROPERTIES,

        /**
         * Permission to use reflection.
         */
        REFLECTION,

        /**
         * Permission to create threads.
         */
        THREAD,

        /**
         * Permission to load native libraries.
         */
        NATIVE_CODE,

        /**
         * Permission to execute external commands.
         */
        EXECUTE
    }

    private final Type type;
    private final String target;
    private final String action;

    /**
     * Creates a new PluginPermission with the specified type, target, and action.
     *
     * @param type the permission type
     * @param target the target of the permission (e.g., file path, host name)
     * @param action the action allowed on the target (e.g., read, write)
     */
    public PluginPermission(Type type, String target, String action) {
        this.type = type;
        this.target = target;
        this.action = action;
    }

    /**
     * Creates a new PluginPermission with the specified type and target.
     *
     * @param type the permission type
     * @param target the target of the permission (e.g., file path, host name)
     */
    public PluginPermission(Type type, String target) {
        this(type, target, null);
    }

    /**
     * Creates a new PluginPermission with the specified type.
     *
     * @param type the permission type
     */
    public PluginPermission(Type type) {
        this(type, null, null);
    }

    /**
     * Gets the permission type.
     *
     * @return the permission type
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the target of the permission.
     *
     * @return the target of the permission
     */
    public String getTarget() {
        return target;
    }

    /**
     * Gets the action allowed on the target.
     *
     * @return the action allowed on the target
     */
    public String getAction() {
        return action;
    }

    /**
     * Checks if this permission implies another permission.
     * A permission implies another if it is more general than the other.
     *
     * @param other the other permission to check
     * @return true if this permission implies the other, false otherwise
     */
    public boolean implies(PluginPermission other) {
        if (other == null || !type.equals(other.type)) {
            return false;
        }

        // If this permission has no target, it implies any target
        if (target == null) {
            return true;
        }

        // If this permission has a target, it must match or be a prefix of the other target
        if (other.target == null || !other.target.startsWith(target)) {
            return false;
        }

        // If this permission has no action, it implies any action
        if (action == null) {
            return true;
        }

        // If this permission has an action, the other must have the same action
        return action.equals(other.action);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginPermission that = (PluginPermission) o;
        return type == that.type &&
                Objects.equals(target, that.target) &&
                Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, target, action);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        if (target != null) {
            sb.append(":").append(target);
        }
        if (action != null) {
            sb.append(":").append(action);
        }
        return sb.toString();
    }
}
