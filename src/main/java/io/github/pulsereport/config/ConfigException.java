package io.github.pulsereport.config;

/**
 * Exception thrown when configuration validation fails.
 */
public class ConfigException extends Exception {
    
    /**
     * Creates a new configuration exception with the specified message.
     *
     * @param message the error message
     */
    public ConfigException(String message) {
        super(message);
    }

    /**
     * Creates a new configuration exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
