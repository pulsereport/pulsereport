package io.github.pulsereport.integrations.s3;

import io.github.pulsereport.integrations.PublishConfig;

import java.util.Collections;
import java.util.Map;

/**
 * Configuration for publishing reports to AWS S3.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public final class S3PublishConfig extends PublishConfig {
    
    private final String bucketName;
    private final String keyPrefix;
    private final String region;
    private final Map<String, String> metadata;

    private S3PublishConfig(Builder builder) {
        super(builder.retryAttempts, builder.retryDelayMs);
        this.bucketName = builder.bucketName;
        this.keyPrefix = builder.keyPrefix;
        this.region = builder.region;
        this.metadata = builder.metadata != null ? 
                Collections.unmodifiableMap(builder.metadata) : Collections.emptyMap();
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getRegion() {
        return region;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Creates a new builder for S3PublishConfig.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for S3PublishConfig.
     */
    public static class Builder {
        private String bucketName;
        private String keyPrefix = "";
        private String region = "us-east-1";
        private Map<String, String> metadata;
        private int retryAttempts = 3;
        private long retryDelayMs = 1000;

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
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

        public S3PublishConfig build() {
            if (bucketName == null || bucketName.isEmpty()) {
                throw new IllegalArgumentException("Bucket name is required");
            }
            return new S3PublishConfig(this);
        }
    }
}
