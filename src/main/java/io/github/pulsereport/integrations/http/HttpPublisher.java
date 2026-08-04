package io.github.pulsereport.integrations.http;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.pulsereport.integrations.PublishConfig;
import io.github.pulsereport.integrations.PublishException;
import io.github.pulsereport.integrations.Publisher;

/**
 * Publisher implementation for HTTP endpoints. Posts test reports to HTTP/HTTPS
 * endpoints with authentication and retry support.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class HttpPublisher implements Publisher {

    private static final Logger logger = LoggerFactory.getLogger(HttpPublisher.class);
    private static final long MAX_DELAY_MS = 10000;

    private final HttpClient httpClient;

    /**
     * Constructs an HttpPublisher with the specified HTTP client.
     *
     * @param httpClient the HTTP client
     */
    public HttpPublisher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Constructs an HttpPublisher with a default HTTP client.
     */
    public HttpPublisher() {
        this(HttpClient.newHttpClient());
    }

    @Override
    public void publish(File reportFile, PublishConfig config) throws PublishException {
        if (!(config instanceof HttpPublishConfig)) {
            throw new PublishException("Config must be HttpPublishConfig");
        }

        if (!reportFile.exists()) {
            throw new PublishException("Report file does not exist: " + reportFile.getAbsolutePath());
        }

        try {
            byte[] reportData = Files.readAllBytes(reportFile.toPath());
            publish(reportData, reportFile.getName(), config);
        } catch (IOException e) {
            throw new PublishException("Failed to read report file: " + reportFile.getAbsolutePath(), e);
        }
    }

    @Override
    public void publish(byte[] reportData, String fileName, PublishConfig config) throws PublishException {
        if (!(config instanceof HttpPublishConfig)) {
            throw new PublishException("Config must be HttpPublishConfig");
        }

        HttpPublishConfig httpConfig = (HttpPublishConfig) config;

        int attempt = 0;
        long delay = httpConfig.getRetryDelayMs();
        Exception lastException = null;

        while (attempt < httpConfig.getRetryAttempts()) {
            try {
                sendHttpRequest(reportData, fileName, httpConfig);
                logger.info("Successfully published report to HTTP endpoint: {}", httpConfig.getEndpoint());
                return;
            } catch (IOException | InterruptedException e) {
                lastException = e;
                attempt++;

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new PublishException("Upload interrupted", e);
                }

                if (attempt >= httpConfig.getRetryAttempts()) {
                    break;
                }

                logger.warn("HTTP upload failed (attempt {}/{}): {}", attempt, httpConfig.getRetryAttempts(), e.getMessage());

                try {
                    Thread.sleep(Math.min(delay, MAX_DELAY_MS));
                    delay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new PublishException("Upload interrupted", ie);
                }
            } catch (PublishException e) {
                // Don't retry on client errors (4xx)
                throw e;
            }
        }

        throw new PublishException(
                "Failed to publish to HTTP endpoint after %d attempts".formatted(httpConfig.getRetryAttempts()),
                lastException);
    }

    private void sendHttpRequest(byte[] data, String fileName, HttpPublishConfig config)
            throws IOException, InterruptedException, PublishException {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(config.getEndpoint()))
                .method(config.getMethod(), HttpRequest.BodyPublishers.ofByteArray(data))
                .header("Content-Type", "application/octet-stream")
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        if (config.getBearerToken() != null && !config.getBearerToken().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + config.getBearerToken());
        } else if (config.getUsername() != null && config.getPassword() != null) {
            String auth = config.getUsername() + ":" + config.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            requestBuilder.header("Authorization", "Basic " + encodedAuth);
        }

        for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();

        if (statusCode >= 200 && statusCode < 300) {
            return; // Success
        } else if (statusCode >= 400 && statusCode < 500) {
            // Client error - don't retry
            throw new PublishException("HTTP client error: " + statusCode + " - " + response.body());
        } else if (statusCode >= 500) {
            // Server error - retry
            throw new IOException("HTTP server error: " + statusCode);
        } else {
            throw new PublishException("Unexpected HTTP status code: " + statusCode);
        }
    }
}
