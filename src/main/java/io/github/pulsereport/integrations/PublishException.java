package io.github.pulsereport.integrations;

import java.io.Serial;

/**
 * Exception thrown when report publishing fails.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class PublishException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new PublishException with the specified detail message.
     * 
     * @param message the detail message
     */
    public PublishException(String message) {
        super(message);
    }

    /**
     * Constructs a new PublishException with the specified detail message and
     * cause.
     * 
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public PublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
