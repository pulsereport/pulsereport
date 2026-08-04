package io.github.pulsereport.integrations;

import java.io.File;

/**
 * Interface for publishing test reports to external systems.
 * Implementations should support retry logic with exponential backoff.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public interface Publisher {
    
    /**
     * Publishes a report file to the configured destination.
     * 
     * @param reportFile the report file to publish
     * @param config the publishing configuration
     * @throws PublishException if publishing fails after retries
     */
    void publish(File reportFile, PublishConfig config) throws PublishException;

    /**
     * Publishes report data as bytes to the configured destination.
     * 
     * @param reportData the report data as bytes
     * @param fileName the name of the report file
     * @param config the publishing configuration
     * @throws PublishException if publishing fails after retries
     */
    void publish(byte[] reportData, String fileName, PublishConfig config) throws PublishException;
}
