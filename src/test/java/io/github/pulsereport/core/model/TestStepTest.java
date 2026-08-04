package io.github.pulsereport.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestStep model class.
 */
class TestStepTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testBuilderCreatesTestStep() {
        Instant start = Instant.ofEpochMilli(1000);
        Instant end = start.plusMillis(100);
        
        TestStep step = TestStep.builder()
                .name("Login to application")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(100)
                .build();

        assertEquals("Login to application", step.getName());
        assertEquals(TestStatus.PASSED, step.getStatus());
        assertEquals(start, step.getStartTime());
        assertEquals(end, step.getEndTime());
        assertEquals(100, step.getDuration());
        assertNull(step.getDescription());
    }

    @Test
    void testBuilderWithDescription() {
        TestStep step = TestStep.builder()
                .name("Verify response")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(2000))
                .endTime(Instant.ofEpochMilli(2050))
                .duration(50)
                .description("Validate API response status code is 200")
                .build();

        assertEquals("Verify response", step.getName());
        assertEquals("Validate API response status code is 200", step.getDescription());
    }

    @Test
    void testBuilderRequiresName() {
        assertThrows(IllegalArgumentException.class, () ->
                TestStep.builder()
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(1000))
                        .duration(0)
                        .build()
        );
    }

    @Test
    void testBuilderRequiresStatus() {
        assertThrows(IllegalArgumentException.class, () ->
                TestStep.builder()
                        .name("step")
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(1000))
                        .duration(0)
                        .build()
        );
    }

    @Test
    void testJsonSerialization() throws Exception {
        Instant start = Instant.parse("2026-02-16T10:00:00Z");
        Instant end = Instant.parse("2026-02-16T10:00:01Z");
        
        TestStep step = TestStep.builder()
                .name("Navigate to homepage")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(1000)
                .description("Open browser and navigate")
                .build();

        String json = objectMapper.writeValueAsString(step);
        assertTrue(json.contains("\"name\":\"Navigate to homepage\""));
        assertTrue(json.contains("\"status\":\"PASSED\""));
        assertTrue(json.contains("\"duration\":1000"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"name\":\"Click button\",\"status\":\"PASSED\",\"startTime\":\"2026-02-16T10:00:00Z\",\"endTime\":\"2026-02-16T10:00:00.500Z\",\"duration\":500,\"description\":\"Click submit button\"}";
        TestStep step = objectMapper.readValue(json, TestStep.class);

        assertEquals("Click button", step.getName());
        assertEquals(TestStatus.PASSED, step.getStatus());
        assertEquals(500, step.getDuration());
        assertEquals("Click submit button", step.getDescription());
    }

    @Test
    void testFailedStep() {
        TestStep step = TestStep.builder()
                .name("Validate element")
                .status(TestStatus.FAILED)
                .startTime(Instant.ofEpochMilli(3000))
                .endTime(Instant.ofEpochMilli(3200))
                .duration(200)
                .build();

        assertEquals(TestStatus.FAILED, step.getStatus());
    }

    @Test
    void testEqualsAndHashCode() {
        Instant start = Instant.ofEpochMilli(4000);
        Instant end = start.plusMillis(100);
        
        TestStep step1 = TestStep.builder()
                .name("step1")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(100)
                .build();

        TestStep step2 = TestStep.builder()
                .name("step1")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(100)
                .build();

        assertEquals(step1, step2);
        assertEquals(step1.hashCode(), step2.hashCode());
    }

    @Test
    void testToString() {
        TestStep step = TestStep.builder()
                .name("test step")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(5000))
                .endTime(Instant.ofEpochMilli(5000))
                .duration(0)
                .build();

        String str = step.toString();
        assertTrue(str.contains("test step"));
        assertTrue(str.contains("PASSED"));
    }
}
