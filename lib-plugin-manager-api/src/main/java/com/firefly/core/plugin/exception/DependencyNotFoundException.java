package com.firefly.core.plugin.exception;

/**
 * Exception thrown when a required plugin dependency cannot be found.
 */
public class DependencyNotFoundException extends PluginException {

    /**
     * Creates a new DependencyNotFoundException with the specified message.
     *
     * @param message the detail message
     */
    public DependencyNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new DependencyNotFoundException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public DependencyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
