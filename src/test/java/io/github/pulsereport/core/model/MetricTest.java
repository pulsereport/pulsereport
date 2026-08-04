package io.github.pulsereport.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Metric model class.
 */
class MetricTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testBuilderCreatesMetric() {
        Instant now = Instant.ofEpochMilli(1000);
        Metric metric = Metric.builder()
                .name("response_time")
                .value(123.45)
                .unit("ms")
                .timestamp(now)
                .build();

        assertEquals("response_time", metric.getName());
        assertEquals(123.45, metric.getValue(), 0.001);
        assertEquals("ms", metric.getUnit());
        assertEquals(now, metric.getTimestamp());
    }

    @Test
    void testBuilderRequiresName() {
        assertThrows(IllegalArgumentException.class, () ->
                Metric.builder()
                        .value(100.0)
                        .unit("ms")
                        .timestamp(Instant.ofEpochMilli(1000))
                        .build()
        );
    }

    @Test
    void testBuilderRequiresUnit() {
        assertThrows(IllegalArgumentException.class, () ->
                Metric.builder()
                        .name("metric")
                        .value(100.0)
                        .timestamp(Instant.ofEpochMilli(1000))
                        .build()
        );
    }

    @Test
    void testBuilderRequiresTimestamp() {
        assertThrows(IllegalArgumentException.class, () ->
                Metric.builder()
                        .name("metric")
                        .value(100.0)
                        .unit("ms")
                        .build()
        );
    }

    @Test
    void testJsonSerialization() throws Exception {
        Instant now = Instant.parse("2026-02-16T10:00:00Z");
        Metric metric = Metric.builder()
                .name("throughput")
                .value(500.0)
                .unit("requests/sec")
                .timestamp(now)
                .build();

        String json = objectMapper.writeValueAsString(metric);
        assertTrue(json.contains("\"name\":\"throughput\""));
        assertTrue(json.contains("\"value\":500.0"));
        assertTrue(json.contains("\"unit\":\"requests/sec\""));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"name\":\"memory\",\"value\":2048.0,\"unit\":\"MB\",\"timestamp\":\"2026-02-16T10:00:00Z\"}";
        Metric metric = objectMapper.readValue(json, Metric.class);

        assertEquals("memory", metric.getName());
        assertEquals(2048.0, metric.getValue(), 0.001);
        assertEquals("MB", metric.getUnit());
        assertEquals(Instant.parse("2026-02-16T10:00:00Z"), metric.getTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.ofEpochMilli(2000);
        Metric metric1 = Metric.builder()
                .name("cpu")
                .value(75.0)
                .unit("%")
                .timestamp(now)
                .build();

        Metric metric2 = Metric.builder()
                .name("cpu")
                .value(75.0)
                .unit("%")
                .timestamp(now)
                .build();

        assertEquals(metric1, metric2);
        assertEquals(metric1.hashCode(), metric2.hashCode());
    }

    @Test
    void testToString() {
        Metric metric = Metric.builder()
                .name("latency")
                .value(50.0)
                .unit("ms")
                .timestamp(Instant.ofEpochMilli(3000))
                .build();

        String str = metric.toString();
        assertTrue(str.contains("latency"));
        assertTrue(str.contains("50.0"));
        assertTrue(str.contains("ms"));
    }
}
