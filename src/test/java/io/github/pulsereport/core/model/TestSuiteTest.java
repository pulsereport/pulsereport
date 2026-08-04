package io.github.pulsereport.core.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Unit tests for TestSuite model class.
 */
class TestSuiteTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testBuilderCreatesMinimalTestSuite() {
        Instant start = Instant.ofEpochMilli(1000);
        Instant end = start.plusMillis(5000);

        TestSuite suite = TestSuite.builder()
                .id("suite-001")
                .name("Login Suite")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(5000)
                .testCases(Arrays.asList())
                .build();

        assertEquals("suite-001", suite.getId());
        assertEquals("Login Suite", suite.getName());
        assertEquals(TestStatus.PASSED, suite.getStatus());
        assertEquals(start, suite.getStartTime());
        assertEquals(end, suite.getEndTime());
        assertEquals(5000, suite.getDuration());
        assertTrue(suite.getTestCases().isEmpty());
        assertEquals(0, suite.getTotalTests());
        assertEquals(0, suite.getPassedTests());
        assertEquals(0, suite.getFailedTests());
        assertEquals(0, suite.getSkippedTests());
    }

    @Test
    void testBuilderWithTestCases() {
        Instant start = Instant.ofEpochMilli(2000);
        Instant end = start.plusMillis(10000);

        TestCase test1 = TestCase.builder()
                .id("test-001")
                .name("Test 1")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(start.plusMillis(1000))
                .duration(1000)
                .build();

        TestCase test2 = TestCase.builder()
                .id("test-002")
                .name("Test 2")
                .status(TestStatus.FAILED)
                .startTime(start.plusMillis(1000))
                .endTime(start.plusMillis(2000))
                .duration(1000)
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-002")
                .name("API Suite")
                .status(TestStatus.FAILED)
                .startTime(start)
                .endTime(end)
                .duration(10000)
                .testCases(Arrays.asList(test1, test2))
                .totalTests(2)
                .passedTests(1)
                .failedTests(1)
                .skippedTests(0)
                .build();

        assertEquals(2, suite.getTestCases().size());
        assertEquals(2, suite.getTotalTests());
        assertEquals(1, suite.getPassedTests());
        assertEquals(1, suite.getFailedTests());
        assertEquals(0, suite.getSkippedTests());
    }

    @Test
    void testBuilderSupportsOptionalSecondaryText() {
        TestSuite suite = TestSuite.builder()
                .id("suite-secondary")
                .name("Feature Suite")
                .secondaryText("features/login.feature")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(2000))
                .endTime(Instant.ofEpochMilli(3000))
                .duration(1000)
                .testCases(Arrays.asList())
                .build();

        assertEquals("features/login.feature", suite.getSecondaryText());
    }

    @Test
    void testBuilderRequiresId() {
        assertThrows(IllegalArgumentException.class, ()
                -> TestSuite.builder()
                        .name("Suite")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(1000))
                        .duration(0)
                        .testCases(Arrays.asList())
                        .build()
        );
    }

    @Test
    void testBuilderRequiresName() {
        assertThrows(IllegalArgumentException.class, ()
                -> TestSuite.builder()
                        .id("suite-001")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(1000))
                        .duration(0)
                        .testCases(Arrays.asList())
                        .build()
        );
    }

    @Test
    void testBuilderRequiresStatus() {
        assertThrows(IllegalArgumentException.class, ()
                -> TestSuite.builder()
                        .id("suite-001")
                        .name("Suite")
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(1000))
                        .duration(0)
                        .testCases(Arrays.asList())
                        .build()
        );
    }

    @Test
    void testBuilderRequiresTestCases() {
        assertThrows(IllegalArgumentException.class, ()
                -> TestSuite.builder()
                        .id("suite-001")
                        .name("Suite")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(1000))
                        .endTime(Instant.ofEpochMilli(1000))
                        .duration(0)
                        .build()
        );
    }

    @Test
    void testImmutabilityOfCollections() {
        TestCase test = TestCase.builder()
                .id("test-001")
                .name("Test")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(3000))
                .endTime(Instant.ofEpochMilli(3000))
                .duration(0)
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-001")
                .name("Suite")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(4000))
                .endTime(Instant.ofEpochMilli(4000))
                .duration(0)
                .testCases(Arrays.asList(test))
                .build();

        List<TestCase> testCases = suite.getTestCases();
        assertThrows(UnsupportedOperationException.class, ()
                -> testCases.add(TestCase.builder()
                        .id("test-002")
                        .name("New Test")
                        .status(TestStatus.PASSED)
                        .startTime(Instant.ofEpochMilli(5000))
                        .endTime(Instant.ofEpochMilli(5000))
                        .duration(0)
                        .build())
        );
    }

    @Test
    void testJsonSerialization() throws Exception {
        Instant start = Instant.parse("2026-02-16T10:00:00Z");
        Instant end = Instant.parse("2026-02-16T10:10:00Z");

        TestSuite suite = TestSuite.builder()
                .id("suite-003")
                .name("Selenium Suite")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(600000)
                .testCases(Arrays.asList())
                .totalTests(10)
                .passedTests(10)
                .failedTests(0)
                .skippedTests(0)
                .build();

        String json = objectMapper.writeValueAsString(suite);
        assertTrue(json.contains("\"id\":\"suite-003\""));
        assertTrue(json.contains("\"name\":\"Selenium Suite\""));
        assertTrue(json.contains("\"totalTests\":10"));
        assertTrue(json.contains("\"passedTests\":10"));
        assertFalse(json.contains("\"secondaryText\""));
    }

    @Test
    void testJsonSerializationIncludesSecondaryTextWhenPresent() throws Exception {
        TestSuite suite = TestSuite.builder()
                .id("suite-secondary-json")
                .name("Readable Suite")
                .secondaryText("features/readable.feature")
                .status(TestStatus.PASSED)
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:05:00Z"))
                .duration(300000)
                .testCases(Arrays.asList())
                .build();

        String json = objectMapper.writeValueAsString(suite);

        assertTrue(json.contains("\"secondaryText\":\"features/readable.feature\""));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"id\":\"suite-004\",\"name\":\"REST Suite\",\"status\":\"PASSED\",\"startTime\":\"2026-02-16T10:00:00Z\",\"endTime\":\"2026-02-16T10:05:00Z\",\"duration\":300000,\"testCases\":[],\"totalTests\":5,\"passedTests\":5,\"failedTests\":0,\"skippedTests\":0}";
        TestSuite suite = objectMapper.readValue(json, TestSuite.class);

        assertEquals("suite-004", suite.getId());
        assertEquals("REST Suite", suite.getName());
        assertEquals(TestStatus.PASSED, suite.getStatus());
        assertEquals(5, suite.getTotalTests());
        assertEquals(5, suite.getPassedTests());
        assertNull(suite.getSecondaryText());
    }

    @Test
    void testJsonDeserializationReadsSecondaryTextWhenPresent() throws Exception {
        String json = "{\"id\":\"suite-004b\",\"name\":\"REST Suite\",\"secondaryText\":\"features/rest.feature\",\"status\":\"PASSED\",\"startTime\":\"2026-02-16T10:00:00Z\",\"endTime\":\"2026-02-16T10:05:00Z\",\"duration\":300000,\"testCases\":[],\"totalTests\":5,\"passedTests\":5,\"failedTests\":0,\"skippedTests\":0}";

        TestSuite suite = objectMapper.readValue(json, TestSuite.class);

        assertEquals("features/rest.feature", suite.getSecondaryText());
    }

    @Test
    void testCountersDefaultToZero() {
        TestSuite suite = TestSuite.builder()
                .id("suite-005")
                .name("Empty Suite")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(6000))
                .endTime(Instant.ofEpochMilli(6000))
                .duration(0)
                .testCases(Arrays.asList())
                .build();

        assertEquals(0, suite.getTotalTests());
        assertEquals(0, suite.getPassedTests());
        assertEquals(0, suite.getFailedTests());
        assertEquals(0, suite.getSkippedTests());
    }

    @Test
    void testEqualsAndHashCode() {
        Instant start = Instant.ofEpochMilli(7000);
        Instant end = start.plusMillis(1000);

        TestSuite suite1 = TestSuite.builder()
                .id("suite-006")
                .name("Suite")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(1000)
                .testCases(Arrays.asList())
                .build();

        TestSuite suite2 = TestSuite.builder()
                .id("suite-006")
                .name("Suite")
                .status(TestStatus.PASSED)
                .startTime(start)
                .endTime(end)
                .duration(1000)
                .testCases(Arrays.asList())
                .build();

        assertEquals(suite1, suite2);
        assertEquals(suite1.hashCode(), suite2.hashCode());
    }

    @Test
    void testToString() {
        TestSuite suite = TestSuite.builder()
                .id("suite-007")
                .name("Test Suite")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(8000))
                .endTime(Instant.ofEpochMilli(8000))
                .duration(0)
                .testCases(Arrays.asList())
                .build();

        String str = suite.toString();
        assertTrue(str.contains("suite-007"));
        assertTrue(str.contains("Test Suite"));
        assertTrue(str.contains("PASSED"));
    }
}
