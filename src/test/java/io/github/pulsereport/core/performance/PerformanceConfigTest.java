package io.github.pulsereport.core.performance;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PerformanceConfig.
 * 
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class PerformanceConfigTest {

    @Test
    public void defaults() {
        PerformanceConfig config = PerformanceConfig.defaults();
        
        assertFalse(config.isStreamingMode());
        assertEquals(PerformanceConfig.DEFAULT_BATCH_SIZE, config.getBatchSize());
        assertEquals(PerformanceConfig.DEFAULT_MAX_MEMORY_MB, config.getMaxMemoryMB());
        assertEquals(PerformanceConfig.DEFAULT_TEMP_DIR, config.getTempStorageDir());
        assertTrue(config.isAutoCleanup());
    }

    @Test
    public void forLargeDatasets() {
        PerformanceConfig config = PerformanceConfig.forLargeDatasets();
        
        assertTrue(config.isStreamingMode());
        assertEquals(1000, config.getBatchSize());
        assertTrue(config.isAutoCleanup());
    }

    @Test
    public void builder() {
        Path customPath = Path.of("/tmp/custom");
        
        PerformanceConfig config = PerformanceConfig.builder()
                .streamingMode(true)
                .batchSize(500)
                .maxMemoryMB(256)
                .tempStorageDir(customPath)
                .autoCleanup(false)
                .build();
        
        assertTrue(config.isStreamingMode());
        assertEquals(500, config.getBatchSize());
        assertEquals(256, config.getMaxMemoryMB());
        assertEquals(customPath, config.getTempStorageDir());
        assertFalse(config.isAutoCleanup());
    }

    @Test
    public void builderInvalidBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            PerformanceConfig.builder()
                    .batchSize(0)
                    .build();
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            PerformanceConfig.builder()
                    .batchSize(-100)
                    .build();
        });
    }

    @Test
    public void builderInvalidMaxMemory() {
        assertThrows(IllegalArgumentException.class, () -> {
            PerformanceConfig.builder()
                    .maxMemoryMB(0)
                    .build();
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            PerformanceConfig.builder()
                    .maxMemoryMB(-512)
                    .build();
        });
    }

    @Test
    public void builderNullTempDir() {
        assertThrows(IllegalArgumentException.class, () -> {
            PerformanceConfig.builder()
                    .tempStorageDir(null)
                    .build();
        });
    }

    @Test
    public void toString_containsKeyFields() {
        PerformanceConfig config = PerformanceConfig.builder()
                .streamingMode(true)
                .batchSize(100)
                .build();
        
        String str = config.toString();
        
        assertTrue(str.contains("streamingMode=true"));
        assertTrue(str.contains("batchSize=100"));
        assertTrue(str.contains("PerformanceConfig"));
    }

    @Test
    public void builderChaining() {
        PerformanceConfig config = PerformanceConfig.builder()
                .streamingMode(true)
                .batchSize(200)
                .maxMemoryMB(1024)
                .autoCleanup(true)
                .build();
        
        assertTrue(config.isStreamingMode());
        assertEquals(200, config.getBatchSize());
        assertEquals(1024, config.getMaxMemoryMB());
        assertTrue(config.isAutoCleanup());
    }

    @Test
    public void defaultBatchSize() {
        PerformanceConfig config = PerformanceConfig.builder()
                .streamingMode(true)
                .build();
        
        assertEquals(PerformanceConfig.DEFAULT_BATCH_SIZE, config.getBatchSize());
    }

    @Test
    public void defaultMaxMemory() {
        PerformanceConfig config = PerformanceConfig.builder()
                .streamingMode(true)
                .build();
        
        assertEquals(PerformanceConfig.DEFAULT_MAX_MEMORY_MB, config.getMaxMemoryMB());
    }
}
