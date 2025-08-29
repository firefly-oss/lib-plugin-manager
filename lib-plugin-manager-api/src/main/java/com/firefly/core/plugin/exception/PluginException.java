package com.firefly.core.plugin.exception;

/**
 * Base exception class for all plugin-related exceptions.
 */
public class PluginException extends RuntimeException {

    /**
     * Creates a new PluginException with the specified message.
     *
     * @param message the detail message
     */
    public PluginException(String message) {
        super(message);
    }

    /**
     * Creates a new PluginException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
