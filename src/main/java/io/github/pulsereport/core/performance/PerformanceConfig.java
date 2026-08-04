package io.github.pulsereport.core.performance;

import java.nio.file.Path;

/**
 * Configuration for performance tuning and streaming aggregation.
 * Controls memory usage, batch processing, and temporary storage.
 * 
 * <p>Performance modes:</p>
 * <ul>
 *   <li><b>In-memory mode</b> (streamingMode=false): Fast, suitable for &lt; 10k tests</li>
 *   <li><b>Streaming mode</b> (streamingMode=true): Memory-efficient, handles 100k+ tests</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * PerformanceConfig config = PerformanceConfig.builder()
 *     .streamingMode(true)
 *     .batchSize(1000)
 *     .maxMemoryMB(512)
 *     .tempStorageDir(Paths.get("/tmp/reporter"))
 *     .autoCleanup(true)
 *     .build();
 * </pre>
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class PerformanceConfig {

    /**
     * Default batch size for streaming aggregation.
     */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * Default max memory before flushing (512MB).
     */
    public static final int DEFAULT_MAX_MEMORY_MB = 512;

    /**
     * Default temporary storage directory.
     */
    public static final Path DEFAULT_TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"), "reporter-temp");

    private final boolean streamingMode;
    private final int batchSize;
    private final int maxMemoryMB;
    private final Path tempStorageDir;
    private final boolean autoCleanup;

    private PerformanceConfig(Builder builder) {
        this.streamingMode = builder.streamingMode;
        this.batchSize = builder.batchSize;
        this.maxMemoryMB = builder.maxMemoryMB;
        this.tempStorageDir = builder.tempStorageDir;
        this.autoCleanup = builder.autoCleanup;
    }

    /**
     * Creates a new builder for PerformanceConfig.
     * 
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default configuration (in-memory mode).
     * 
     * @return default configuration
     */
    public static PerformanceConfig defaults() {
        return builder().build();
    }

    /**
     * Creates a streaming configuration optimized for large datasets.
     * 
     * @return streaming configuration
     */
    public static PerformanceConfig forLargeDatasets() {
        return builder()
                .streamingMode(true)
                .batchSize(1000)
                .autoCleanup(true)
                .build();
    }

    /**
     * Whether streaming mode is enabled.
     * 
     * @return true if streaming mode is enabled
     */
    public boolean isStreamingMode() {
        return streamingMode;
    }

    /**
     * Gets the batch size for streaming aggregation.
     * 
     * @return batch size (number of tests per batch)
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Gets the maximum memory threshold before flushing.
     * 
     * @return max memory in MB
     */
    public int getMaxMemoryMB() {
        return maxMemoryMB;
    }

    /**
     * Gets the temporary storage directory path.
     * 
     * @return temp storage directory
     */
    public Path getTempStorageDir() {
        return tempStorageDir;
    }

    /**
     * Whether auto-cleanup of temporary files is enabled.
     * 
     * @return true if auto-cleanup is enabled
     */
    public boolean isAutoCleanup() {
        return autoCleanup;
    }

    @Override
    public String toString() {
        return "PerformanceConfig{" +
                "streamingMode=" + streamingMode +
                ", batchSize=" + batchSize +
                ", maxMemoryMB=" + maxMemoryMB +
                ", tempStorageDir=" + tempStorageDir +
                ", autoCleanup=" + autoCleanup +
                '}';
    }

    /**
     * Builder for PerformanceConfig.
     */
    public static class Builder {
        private boolean streamingMode = false;
        private int batchSize = DEFAULT_BATCH_SIZE;
        private int maxMemoryMB = DEFAULT_MAX_MEMORY_MB;
        private Path tempStorageDir = DEFAULT_TEMP_DIR;
        private boolean autoCleanup = true;

        private Builder() {
        }

        /**
         * Sets whether streaming mode is enabled.
         * 
         * @param streamingMode true to enable streaming mode
         * @return this builder
         */
        public Builder streamingMode(boolean streamingMode) {
            this.streamingMode = streamingMode;
            return this;
        }

        /**
         * Sets the batch size for streaming aggregation.
         * 
         * @param batchSize number of tests per batch (must be &gt; 0)
         * @return this builder
         * @throws IllegalArgumentException if batchSize &lt;= 0
         */
        public Builder batchSize(int batchSize) {
            if (batchSize <= 0) {
                throw new IllegalArgumentException("Batch size must be positive: " + batchSize);
            }
            this.batchSize = batchSize;
            return this;
        }

        /**
         * Sets the maximum memory threshold before flushing.
         * 
         * @param maxMemoryMB max memory in MB (must be &gt; 0)
         * @return this builder
         * @throws IllegalArgumentException if maxMemoryMB &lt;= 0
         */
        public Builder maxMemoryMB(int maxMemoryMB) {
            if (maxMemoryMB <= 0) {
                throw new IllegalArgumentException("Max memory must be positive: " + maxMemoryMB);
            }
            this.maxMemoryMB = maxMemoryMB;
            return this;
        }

        /**
         * Sets the temporary storage directory.
         * 
         * @param tempStorageDir path to temp storage directory
         * @return this builder
         * @throws IllegalArgumentException if path is null
         */
        public Builder tempStorageDir(Path tempStorageDir) {
            if (tempStorageDir == null) {
                throw new IllegalArgumentException("Temp storage directory cannot be null");
            }
            this.tempStorageDir = tempStorageDir;
            return this;
        }

        /**
         * Sets whether auto-cleanup is enabled.
         * 
         * @param autoCleanup true to enable auto-cleanup
         * @return this builder
         */
        public Builder autoCleanup(boolean autoCleanup) {
            this.autoCleanup = autoCleanup;
            return this;
        }

        /**
         * Builds the PerformanceConfig.
         * 
         * @return new PerformanceConfig instance
         */
        public PerformanceConfig build() {
            return new PerformanceConfig(this);
        }
    }
}
