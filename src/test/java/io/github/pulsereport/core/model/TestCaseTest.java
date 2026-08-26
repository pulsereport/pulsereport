package io.github.pulsereport.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestCase model class.
 */
class TestCaseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void builderCreatesMinimalTestCase() {
        Instant start = Instant.ofEpochMilli(1000);
        Instant end = start.plusMillis(1000);
        
        TestCase testCase = TestCase.builder()
                .id("test-001")
                .name("Test Login")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(1000)
                .build();

        assertEquals("test-001", testCase.getId());
        assertEquals("Test Login", testCase.getName());
        assertEquals(TestStatus.PASSED, testCase.getStatus());
        assertEquals(start, testCase.getStartTime());
        assertEquals(end, testCase.getEndTime());
        assertEquals(1000, testCase.getDuration());
        assertNull(testCase.getClassName());
        assertNull(testCase.getMethodName());
        assertNull(testCase.getErrorMessage());
        assertNull(testCase.getStackTrace());
        assertTrue(testCase.getSteps().isEmpty());
        assertTrue(testCase.getArtifacts().isEmpty());
        assertTrue(testCase.getMetrics().isEmpty());
        assertEquals(0, testCase.getRetryCount());
    }

    @Test
    void builderWithAllFields() {
        Instant start = Instant.ofEpochMilli(2000);
        Instant end = start.plusMillis(2000);
        
        TestStep step = TestStep.builder()
                .name("Step 1")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(2000)
                .build();
        
        Artifact artifact = Artifact.builder()
                .name("screenshot")
                .type("screenshot")
                .path("/img.png")
                .timestamp(start)
                .build();
        
        Metric metric = Metric.builder()
                .name("response_time")
                .value(100.0)
                .unit("ms")
                .timestamp(start)
                .build();

        TestCase testCase = TestCase.builder()
                .id("test-002")
                .name("Test API")
                .className("com.example.ApiTest")
                .methodName("testGetUser")
                .status(TestStatus.FAILED)
                .startTime(start)
                .endTime(end)
                .duration(2000)
                .errorMessage("Expected 200 but got 500")
                .stackTrace("java.lang.AssertionError at line 42")
                .steps(Arrays.asList(step))
                .artifacts(Arrays.asList(artifact))
                .metrics(Arrays.asList(metric))
                .retryCount(2)
                .build();

        assertEquals("test-002", testCase.getId());
        assertEquals("Test API", testCase.getName());
        assertEquals("com.example.ApiTest", testCase.getClassName());
        assertEquals("testGetUser", testCase.getMethodName());
        assertEquals(TestStatus.FAILED, testCase.getStatus());
        assertEquals("Expected 200 but got 500", testCase.getErrorMessage());
        assertEquals("java.lang.AssertionError at line 42", testCase.getStackTrace());
        assertEquals(1, testCase.getSteps().size());
        assertEquals(1, testCase.getArtifacts().size());
        assertEquals(1, testCase.getMetrics().size());
        assertEquals(2, testCase.getRetryCount());
    }

    @Test
    void builderRequiresId() {
        assertThrows(IllegalArgumentException.class, () ->
                TestCase.builder()
                        .name("Test")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(2000))
                        .duration(0)
                        .build()
        );
    }

    @Test
    void builderRequiresName() {
        assertThrows(IllegalArgumentException.class, () ->
                TestCase.builder()
                        .id("test-001")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(2000))
                        .duration(0)
                        .build()
        );
    }

    @Test
    void builderRequiresStatus() {
        assertThrows(IllegalArgumentException.class, () ->
                TestCase.builder()
                        .id("test-001")
                        .name("Test")
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(2000))
                        .duration(0)
                        .build()
        );
    }

    @Test
    void immutabilityOfCollections() {
        TestStep step = TestStep.builder()
                .name("Step")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(3000))
                .endTime(Instant.ofEpochMilli(3000))
                .duration(0)
                .build();

        TestCase testCase = TestCase.builder()
                .id("test-001")
                .name("Test")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(4000))
                .endTime(Instant.ofEpochMilli(4000))
                .duration(0)
                .steps(Arrays.asList(step))
                .build();

        List<TestStep> steps = testCase.getSteps();
        assertThrows(UnsupportedOperationException.class, () -> 
                steps.add(TestStep.builder()
                        .name("New Step")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(5000))
                        .endTime(Instant.ofEpochMilli(5000))
                        .duration(0)
                        .build())
        );
    }

    @Test
    void jsonSerialization() throws Exception {
        Instant start = Instant.parse("2026-02-16T10:00:00Z");
        Instant end = Instant.parse("2026-02-16T10:00:01Z");
        
        TestCase testCase = TestCase.builder()
                .id("test-003")
                .name("Test Selenium")
                .className("com.example.SeleniumTest")
                .methodName("testHomepage")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(1000)
                .build();

        String json = objectMapper.writeValueAsString(testCase);
        assertTrue(json.contains("\"id\":\"test-003\""));
        assertTrue(json.contains("\"name\":\"Test Selenium\""));
        assertTrue(json.contains("\"className\":\"com.example.SeleniumTest\""));
        assertTrue(json.contains("\"status\":\"PASSED\""));
    }

    @Test
    void jsonDeserialization() throws Exception {
        String json = "{\"id\":\"test-004\",\"name\":\"Test REST\",\"className\":\"RestTest\",\"methodName\":\"testGet\",\"status\":\"PASSED\",\"startTime\":\"2026-02-16T10:00:00Z\",\"endTime\":\"2026-02-16T10:00:01Z\",\"duration\":1000,\"retryCount\":0}";
        TestCase testCase = objectMapper.readValue(json, TestCase.class);

        assertEquals("test-004", testCase.getId());
        assertEquals("Test REST", testCase.getName());
        assertEquals("RestTest", testCase.getClassName());
        assertEquals("testGet", testCase.getMethodName());
        assertEquals(TestStatus.PASSED, testCase.getStatus());
        assertEquals(0, testCase.getRetryCount());
    }

    @Test
    void flakyTestWithRetries() {
        TestCase testCase = TestCase.builder()
                .id("test-005")
                .name("Flaky Test")
                .status(TestStatus.FLAKY)
                .startTime(Instant.ofEpochMilli(6000))
                .endTime(Instant.ofEpochMilli(7000))
                .duration(1000)
                .retryCount(3)
                .build();

        assertEquals(TestStatus.FLAKY, testCase.getStatus());
        assertEquals(3, testCase.getRetryCount());
    }

    @Test
    void emptyListsNotNull() {
        TestCase testCase = TestCase.builder()
                .id("test-006")
                .name("Empty Test")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(8000))
                .endTime(Instant.ofEpochMilli(8000))
                .duration(0)
                .build();

        assertNotNull(testCase.getSteps());
        assertNotNull(testCase.getArtifacts());
        assertNotNull(testCase.getMetrics());
        assertTrue(testCase.getSteps().isEmpty());
        assertTrue(testCase.getArtifacts().isEmpty());
        assertTrue(testCase.getMetrics().isEmpty());
    }

    @Test
    void equalsAndHashCode() {
        Instant start = Instant.ofEpochMilli(9000);
        Instant end = start.plusMillis(100);
        
        TestCase testCase1 = TestCase.builder()
                .id("test-007")
                .name("Test")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(100)
                .build();

        TestCase testCase2 = TestCase.builder()
                .id("test-007")
                .name("Test")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(100)
                .build();

        assertEquals(testCase1, testCase2);
        assertEquals(testCase1.hashCode(), testCase2.hashCode());
    }

    @Test
    void toString_containsKeyFields() {
        TestCase testCase = TestCase.builder()
                .id("test-008")
                .name("Test String")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(10000))
                .endTime(Instant.ofEpochMilli(10000))
                .duration(0)
                .build();

        String str = testCase.toString();
        assertTrue(str.contains("test-008"));
        assertTrue(str.contains("Test String"));
        assertTrue(str.contains("PASSED"));
    }
}
