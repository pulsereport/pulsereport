package io.github.pulsereport.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestRun model class.
 */
class TestRunTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void builderCreatesMinimalTestRun() {
        Instant start = Instant.ofEpochMilli(1000);
        Instant end = start.plusMillis(60000);
        
        TestRun run = TestRun.builder()
                .id("run-001")
                .name("Full Test Run")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(60000)
                .suites(Arrays.asList())
                .build();

        assertEquals("run-001", run.getId());
        assertEquals("Full Test Run", run.getName());
        assertEquals(TestStatus.PASSED, run.getStatus());
        assertEquals(start, run.getStartTime());
        assertEquals(end, run.getEndTime());
        assertEquals(60000, run.getDuration());
        assertTrue(run.getSuites().isEmpty());
        assertNull(run.getEnvironment());
        assertEquals(0, run.getTotalTests());
        assertEquals(0, run.getPassedTests());
        assertEquals(0, run.getFailedTests());
        assertEquals(0, run.getSkippedTests());
    }

    @Test
    void builderWithAllFields() {
        Instant start = Instant.ofEpochMilli(2000);
        Instant end = start.plusMillis(120000);
        
        TestSuite suite1 = TestSuite.builder()
                .id("suite-001")
                .name("Suite 1")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(start.plusMillis(60000))
                .duration(60000)
                .testCases(Arrays.asList())
                .build();
        
        TestSuite suite2 = TestSuite.builder()
                .id("suite-002")
                .name("Suite 2")
                .status(TestStatus.FAILED)
                .startTime(start.plusMillis(60000))
                .endTime(end)
                .duration(60000)
                .testCases(Arrays.asList())
                .build();

        Map<String, String> env = new HashMap<>();
        env.put("os", "macOS");
        env.put("browser", "Chrome");
        env.put("version", "1.0");

        TestRun run = TestRun.builder()
                .id("run-002")
                .name("Regression Suite")
                .status(TestStatus.FAILED)
                .startTime(start)
                .endTime(end)
                .duration(120000)
                .suites(Arrays.asList(suite1, suite2))
                .environment(env)
                .totalTests(20)
                .passedTests(15)
                .failedTests(5)
                .skippedTests(0)
                .build();

        assertEquals("run-002", run.getId());
        assertEquals("Regression Suite", run.getName());
        assertEquals(TestStatus.FAILED, run.getStatus());
        assertEquals(2, run.getSuites().size());
        assertNotNull(run.getEnvironment());
        assertEquals("macOS", run.getEnvironment().get("os"));
        assertEquals(20, run.getTotalTests());
        assertEquals(15, run.getPassedTests());
        assertEquals(5, run.getFailedTests());
        assertEquals(0, run.getSkippedTests());
    }

    @Test
    void builderRequiresId() {
        assertThrows(IllegalArgumentException.class, () ->
                TestRun.builder()
                        .name("Run")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(2000))
                        .duration(0)
                        .suites(Arrays.asList())
                        .build()
        );
    }

    @Test
    void builderRequiresName() {
        assertThrows(IllegalArgumentException.class, () ->
                TestRun.builder()
                        .id("run-001")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(2000))
                        .duration(0)
                        .suites(Arrays.asList())
                        .build()
        );
    }

    @Test
    void builderRequiresStatus() {
        assertThrows(IllegalArgumentException.class, () ->
                TestRun.builder()
                        .id("run-001")
                        .name("Run")
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(2000))
                        .duration(0)
                        .suites(Arrays.asList())
                        .build()
        );
    }

    @Test
    void builderRequiresStartTime() {
        assertThrows(IllegalArgumentException.class, () ->
                TestRun.builder()
                        .id("run-001")
                        .name("Run")
                        .status(TestStatus.PASSED)
                        .endTime(Instant.ofEpochMilli(2000))
                        .duration(0)
                        .suites(Arrays.asList())
                        .build()
        );
    }

    @Test
    void builderRequiresSuites() {
        assertThrows(IllegalArgumentException.class, () ->
                TestRun.builder()
                        .id("run-001")
                        .name("Run")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(2000))
                        .duration(0)
                        .build()
        );
    }

    @Test
    void endTimeIsOptional() {
        Instant start = Instant.ofEpochMilli(3000);
        
        TestRun run = TestRun.builder()
                .id("run-003")
                .name("In Progress Run")
                .status(TestStatus.PASSED)
                .startTime(start)
                .duration(0)
                .suites(Arrays.asList())
                .build();

        assertNull(run.getEndTime());
    }

    @Test
    void immutabilityOfCollections() {
        TestSuite suite = TestSuite.builder()
                .id("suite-001")
                .name("Suite")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(1000))
                .endTime(Instant.ofEpochMilli(2000))
                .duration(0)
                .testCases(Arrays.asList())
                .build();

        TestRun run = TestRun.builder()
                .id("run-001")
                .name("Run")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(3000))
                .endTime(Instant.ofEpochMilli(4000))
                .duration(0)
                .suites(Arrays.asList(suite))
                .build();

        List<TestSuite> suites = run.getSuites();
        assertThrows(UnsupportedOperationException.class, () ->
                suites.add(TestSuite.builder()
                        .id("suite-002")
                        .name("New Suite")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(5000))
                        .endTime(Instant.ofEpochMilli(6000))
                        .duration(0)
                        .testCases(Arrays.asList())
                        .build())
        );
    }

    @Test
    void immutabilityOfEnvironmentMap() {
        Map<String, String> env = new HashMap<>();
        env.put("key", "value");

        TestRun run = TestRun.builder()
                .id("run-001")
                .name("Run")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(7000))
                .endTime(Instant.ofEpochMilli(8000))
                .duration(0)
                .suites(Arrays.asList())
                .environment(env)
                .build();

        Map<String, String> returnedEnv = run.getEnvironment();
        assertThrows(UnsupportedOperationException.class, () ->
                returnedEnv.put("newKey", "newValue")
        );
    }

    @Test
    void jsonSerialization() throws Exception {
        Instant start = Instant.parse("2026-02-16T10:00:00Z");
        Instant end = Instant.parse("2026-02-16T11:00:00Z");
        
        Map<String, String> env = new HashMap<>();
        env.put("environment", "staging");

        TestRun run = TestRun.builder()
                .id("run-004")
                .name("CI Run")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(3600000)
                .suites(Arrays.asList())
                .environment(env)
                .totalTests(100)
                .passedTests(95)
                .failedTests(3)
                .skippedTests(2)
                .build();

        String json = objectMapper.writeValueAsString(run);
        assertTrue(json.contains("\"id\":\"run-004\""));
        assertTrue(json.contains("\"name\":\"CI Run\""));
        assertTrue(json.contains("\"totalTests\":100"));
        assertTrue(json.contains("\"passedTests\":95"));
        assertTrue(json.contains("\"environment\""));
    }

    @Test
    void jsonDeserialization() throws Exception {
        String json = "{\"id\":\"run-005\",\"name\":\"Nightly Run\",\"status\":\"PASSED\",\"startTime\":\"2026-02-16T10:00:00Z\",\"endTime\":\"2026-02-16T12:00:00Z\",\"duration\":7200000,\"suites\":[],\"totalTests\":50,\"passedTests\":50,\"failedTests\":0,\"skippedTests\":0}";
        TestRun run = objectMapper.readValue(json, TestRun.class);

        assertEquals("run-005", run.getId());
        assertEquals("Nightly Run", run.getName());
        assertEquals(TestStatus.PASSED, run.getStatus());
        assertEquals(50, run.getTotalTests());
        assertEquals(50, run.getPassedTests());
    }

    @Test
    void countersDefaultToZero() {
        TestRun run = TestRun.builder()
                .id("run-006")
                .name("Empty Run")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(9000))
                .endTime(Instant.ofEpochMilli(10000))
                .duration(0)
                .suites(Arrays.asList())
                .build();

        assertEquals(0, run.getTotalTests());
        assertEquals(0, run.getPassedTests());
        assertEquals(0, run.getFailedTests());
        assertEquals(0, run.getSkippedTests());
    }

    @Test
    void equalsAndHashCode() {
        Instant start = Instant.ofEpochMilli(11000);
        Instant end = start.plusMillis(1000);
        
        TestRun run1 = TestRun.builder()
                .id("run-007")
                .name("Run")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(1000)
                .suites(Arrays.asList())
                .build();

        TestRun run2 = TestRun.builder()
                .id("run-007")
                .name("Run")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(1000)
                .suites(Arrays.asList())
                .build();

        assertEquals(run1, run2);
        assertEquals(run1.hashCode(), run2.hashCode());
    }

    @Test
    void toString_containsKeyFields() {
        TestRun run = TestRun.builder()
                .id("run-008")
                .name("Test Run")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(12000))
                .endTime(Instant.ofEpochMilli(12000))
                .duration(0)
                .suites(Arrays.asList())
                .build();

        String str = run.toString();
        assertTrue(str.contains("run-008"));
        assertTrue(str.contains("Test Run"));
        assertTrue(str.contains("PASSED"));
    }
}
