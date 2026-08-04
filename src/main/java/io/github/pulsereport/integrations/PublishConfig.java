package io.github.pulsereport.integrations;

/**
 * Base configuration class for report publishing.
 * Contains common settings for retry logic and error handling.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public abstract class PublishConfig {
    
    private final int retryAttempts;
    private final long retryDelayMs;

    /**
     * Constructs a PublishConfig with the specified retry settings.
     * 
     * @param retryAttempts maximum number of retry attempts
     * @param retryDelayMs initial delay between retries in milliseconds
     */
    protected PublishConfig(int retryAttempts, long retryDelayMs) {
        this.retryAttempts = retryAttempts > 0 ? retryAttempts : 3;
        this.retryDelayMs = retryDelayMs > 0 ? retryDelayMs : 1000;
    }

    /**
     * Gets the maximum number of retry attempts.
     * 
     * @return retry attempts
     */
    public int getRetryAttempts() {
        return retryAttempts;
    }

    /**
     * Gets the initial retry delay in milliseconds.
     * 
     * @return retry delay in milliseconds
     */
    public long getRetryDelayMs() {
        return retryDelayMs;
    }
}
