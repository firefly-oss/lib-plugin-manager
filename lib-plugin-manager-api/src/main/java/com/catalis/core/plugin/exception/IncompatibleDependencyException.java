package com.catalis.core.plugin.exception;

/**
 * Exception thrown when a plugin dependency is found but its version is incompatible
 * with the version constraints specified by the dependent plugin.
 */
public class IncompatibleDependencyException extends PluginException {

    /**
     * Creates a new IncompatibleDependencyException with the specified message.
     *
     * @param message the detail message
     */
    public IncompatibleDependencyException(String message) {
        super(message);
    }

    /**
     * Creates a new IncompatibleDependencyException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public IncompatibleDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
