package io.github.pulsereport.core.model;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an artifact (attachment) associated with a test execution.
 * Examples include screenshots, logs, videos, HTTP request/response data, etc.
 * Immutable value object using Builder pattern.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Artifact {
    
    private final String name;
    private final String type;
    private final String path;
    private final String mimeType;
    private final long size;
    private final Instant timestamp;
    private final String content;

    @JsonCreator
    private Artifact(
            @JsonProperty("name") String name,
            @JsonProperty("type") String type,
            @JsonProperty("path") String path,
            @JsonProperty("mimeType") String mimeType,
            @JsonProperty("size") long size,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("content") String content) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.mimeType = mimeType;
        this.size = size;
        this.timestamp = timestamp;
        this.content = content;
    }

    /**
     * Gets the artifact name.
     * @return the artifact name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the artifact type.
     * @return the type (e.g., "screenshot", "log", "video", "http-request")
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the artifact path or URL.
     * @return the file path or URL
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the MIME type.
     * @return the MIME type (optional)
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets the artifact size in bytes.
     * @return the size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Gets the timestamp when the artifact was created.
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the inline content of the artifact.
     * @return the content (optional, may be null)
     */
    public String getContent() {
        return content;
    }

    /**
     * Creates a new builder for Artifact.
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Compares this Artifact to another object for equality.
     * @param o the object to compare with
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artifact artifact = (Artifact) o;
        return size == artifact.size &&
                Objects.equals(name, artifact.name) &&
                Objects.equals(type, artifact.type) &&
                Objects.equals(path, artifact.path) &&
                Objects.equals(mimeType, artifact.mimeType) &&
                Objects.equals(timestamp, artifact.timestamp) &&
                Objects.equals(content, artifact.content);
    }

    /**
     * Returns a hash code value for this Artifact.
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, type, path, mimeType, size, timestamp, content);
    }

    /**
     * Returns a string representation of this Artifact.
     * @return a string representation
     */
    @Override
    public String toString() {
        return "Artifact{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", path='" + path + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", size=" + size +
                ", timestamp=" + timestamp +
                ", content='" + (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : null) + '\'' +
                '}';
    }

    /**
     * Builder for Artifact instances.
     */
    public static final class Builder {
        private String name;
        private String type;
        private String path;
        private String mimeType;
        private long size;
        private Instant timestamp;
        private String content;

        private Builder() {
        }

        /**
         * Sets the artifact name.
         * @param name the artifact name (required)
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the artifact type.
         * @param type the artifact type (required)
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the artifact path or URL.
         * @param path the file path or URL (required)
         * @return this builder
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the MIME type.
         * @param mimeType the MIME type (optional)
         * @return this builder
         */
        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        /**
         * Sets the artifact size in bytes.
         * @param size the size in bytes (optional)
         * @return this builder
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the timestamp.
         * @param timestamp the timestamp (required)
         * @return this builder
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the inline content.
         * @param content the inline content (optional)
         * @return this builder
         */
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        /**
         * Builds the Artifact instance.
         * @return a new Artifact
         * @throws IllegalArgumentException if required fields are missing
         */
        public Artifact build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name is required");
            }
            if (type == null || type.trim().isEmpty()) {
                throw new IllegalArgumentException("type is required");
            }
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("path is required");
            }
            if (timestamp == null) {
                throw new IllegalArgumentException("timestamp is required");
            }
            return new Artifact(name, type, path, mimeType, size, timestamp, content);
        }
    }
}
