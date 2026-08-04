package io.github.pulsereport.integrations.http;

import io.github.pulsereport.integrations.PublishConfig;

import java.util.Collections;
import java.util.Map;

/**
 * Configuration for publishing reports to HTTP endpoints.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public final class HttpPublishConfig extends PublishConfig {
    
    private final String endpoint;
    private final String method;
    private final String bearerToken;
    private final String username;
    private final String password;
    private final Map<String, String> headers;

    private HttpPublishConfig(Builder builder) {
        super(builder.retryAttempts, builder.retryDelayMs);
        this.endpoint = builder.endpoint;
        this.method = builder.method;
        this.bearerToken = builder.bearerToken;
        this.username = builder.username;
        this.password = builder.password;
        this.headers = builder.headers != null ? 
                Collections.unmodifiableMap(builder.headers) : Collections.emptyMap();
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getMethod() {
        return method;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Creates a new builder for HttpPublishConfig.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for HttpPublishConfig.
     */
    public static class Builder {
        private String endpoint;
        private String method = "POST";
        private String bearerToken;
        private String username;
        private String password;
        private Map<String, String> headers;
        private int retryAttempts = 3;
        private long retryDelayMs = 1000;

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder bearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder retryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
            return this;
        }

        public Builder retryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
            return this;
        }

        public HttpPublishConfig build() {
            if (endpoint == null || endpoint.isEmpty()) {
                throw new IllegalArgumentException("Endpoint is required");
            }
            return new HttpPublishConfig(this);
        }
    }
}
