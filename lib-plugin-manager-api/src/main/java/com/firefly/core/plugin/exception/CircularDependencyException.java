package com.firefly.core.plugin.exception;

/**
 * Exception thrown when a circular dependency is detected between plugins.
 */
public class CircularDependencyException extends PluginException {

    /**
     * Creates a new CircularDependencyException with the specified message.
     *
     * @param message the detail message
     */
    public CircularDependencyException(String message) {
        super(message);
    }

    /**
     * Creates a new CircularDependencyException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public CircularDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
