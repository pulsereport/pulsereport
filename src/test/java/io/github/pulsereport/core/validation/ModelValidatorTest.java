package io.github.pulsereport.core.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.pulsereport.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ModelValidator.
 */
class ModelValidatorTest {

    private ModelValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new ModelValidator();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void validateValidTestRun() {
        TestCase testCase = TestCase.builder()
                .id("test-001")
                .name("Test 1")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(1000))
                .build();
        
        TestSuite suite = TestSuite.builder()
                .id("suite-001")
                .name("Suite 1")
                .status(TestStatus.PASSED)
                .testCases(Arrays.asList(testCase))
                .build();
        
        TestRun run = TestRun.builder()
                .id("run-001")
                .name("Valid Run")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(2000))
                .endTime(Instant.ofEpochMilli(3000))
                .duration(1000)
                .suites(Arrays.asList(suite))
                .build();

        List<String> errors = validator.validate(run);
        assertTrue(errors.isEmpty(), "Valid TestRun should have no validation errors");
    }

    @Test
    void validateValidTestSuite() {
        TestCase testCase = TestCase.builder()
                .id("test-001")
                .name("Test 1")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(1000))
                .build();
        
        TestSuite suite = TestSuite.builder()
                .id("suite-001")
                .name("Valid Suite")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(2000))
                .endTime(Instant.ofEpochMilli(3000))
                .duration(1000)
                .testCases(Arrays.asList(testCase))
                .build();

        List<String> errors = validator.validate(suite);
        assertTrue(errors.isEmpty(), "Valid TestSuite should have no validation errors");
    }

    @Test
    void validateValidTestCase() {
        TestCase testCase = TestCase.builder()
                .id("test-001")
                .name("Valid Test")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(1000))
                .endTime(Instant.ofEpochMilli(2000))
                .duration(1000)
                .build();

        List<String> errors = validator.validate(testCase);
        assertTrue(errors.isEmpty(), "Valid TestCase should have no validation errors");
    }

    @Test
    void validateValidTestStep() {
        TestStep step = TestStep.builder()
                .name("Valid Step")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(1000))
                .endTime(Instant.ofEpochMilli(1100))
                .duration(100)
                .build();

        List<String> errors = validator.validate(step);
        assertTrue(errors.isEmpty(), "Valid TestStep should have no validation errors");
    }

    @Test
    void validateValidArtifact() {
        Artifact artifact = Artifact.builder()
                .name("screenshot")
                .type("screenshot")
                .path("/path/to/file")
                .timestamp(Instant.ofEpochMilli(1000))
                .build();

        List<String> errors = validator.validate(artifact);
        assertTrue(errors.isEmpty(), "Valid Artifact should have no validation errors");
    }

    @Test
    void validateValidMetric() {
        Metric metric = Metric.builder()
                .name("response_time")
                .value(100.0)
                .unit("ms")
                .timestamp(Instant.ofEpochMilli(1000))
                .build();

        List<String> errors = validator.validate(metric);
        assertTrue(errors.isEmpty(), "Valid Metric should have no validation errors");
    }

    @Test
    void validateNestedStructures() {
        TestStep step = TestStep.builder()
                .name("Step 1")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(1000))
                .endTime(Instant.ofEpochMilli(1100))
                .duration(100)
                .build();

        TestCase testCase = TestCase.builder()
                .id("test-001")
                .name("Test 1")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(2000))
                .endTime(Instant.ofEpochMilli(3000))
                .duration(1000)
                .steps(Arrays.asList(step))
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-001")
                .name("Suite 1")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(4000))
                .endTime(Instant.ofEpochMilli(9000))
                .duration(5000)
                .testCases(Arrays.asList(testCase))
                .build();

        TestRun run = TestRun.builder()
                .id("run-001")
                .name("Run 1")
                .status(TestStatus.PASSED)
                .startTime(Instant.ofEpochMilli(10000))
                .endTime(Instant.ofEpochMilli(20000))
                .duration(10000)
                .suites(Arrays.asList(suite))
                .build();

        List<String> errors = validator.validate(run);
        assertTrue(errors.isEmpty(), "Valid nested structure should have no validation errors");
    }

    @Test
    void validateNullObject() {
        List<String> errors = validator.validate(null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("null"));
    }

    @Test
    void validateReturnsEmptyListForValidObjects() {
        Metric metric = Metric.builder()
                .name("cpu")
                .value(50.0)
                .unit("%")
                .timestamp(Instant.ofEpochMilli(1000))
                .build();

        List<String> errors = validator.validate(metric);
        assertNotNull(errors);
        assertEquals(0, errors.size());
    }

    @Test
    void validateTestRunWithNullId() throws Exception {
        String json = "{\"name\":\"Test Run\",\"status\":\"PASSED\",\"startTime\":\"2024-01-01T10:00:00Z\",\"suites\":[]}";
        TestRun run = objectMapper.readValue(json, TestRun.class);
        
        List<String> errors = validator.validate(run);
        assertFalse(errors.isEmpty(), "Should have validation errors for null id");
        assertTrue(errors.stream().anyMatch(e -> e.contains("id")), 
                "Should have error about missing id");
    }

    @Test
    void validateTestRunWithEmptyId() throws Exception {
        String json = "{\"id\":\"\",\"name\":\"Test Run\",\"status\":\"PASSED\",\"startTime\":\"2024-01-01T10:00:00Z\",\"suites\":[]}";
        TestRun run = objectMapper.readValue(json, TestRun.class);
        
        List<String> errors = validator.validate(run);
        assertFalse(errors.isEmpty(), "Should have validation errors for empty id");
        assertTrue(errors.stream().anyMatch(e -> e.contains("id")), 
                "Should have error about empty id");
    }

    @Test
    void validateTestRunWithNullName() throws Exception {
        String json = "{\"id\":\"run-001\",\"status\":\"PASSED\",\"startTime\":\"2024-01-01T10:00:00Z\",\"suites\":[]}";
        TestRun run = objectMapper.readValue(json, TestRun.class);
        
        List<String> errors = validator.validate(run);
        assertFalse(errors.isEmpty(), "Should have validation errors for null name");
        assertTrue(errors.stream().anyMatch(e -> e.contains("name")), 
                "Should have error about missing name");
    }

    @Test
    void validateTestRunWithNullStatus() throws Exception {
        String json = "{\"id\":\"run-001\",\"name\":\"Test Run\",\"startTime\":\"2024-01-01T10:00:00Z\",\"suites\":[]}";
        TestRun run = objectMapper.readValue(json, TestRun.class);
        
        List<String> errors = validator.validate(run);
        assertFalse(errors.isEmpty(), "Should have validation errors for null status");
        assertTrue(errors.stream().anyMatch(e -> e.contains("status")), 
                "Should have error about missing status");
    }

    @Test
    void validateTestRunWithNullStartTime() throws Exception {
        String json = "{\"id\":\"run-001\",\"name\":\"Test Run\",\"status\":\"PASSED\",\"suites\":[]}";
        TestRun run = objectMapper.readValue(json, TestRun.class);
        
        List<String> errors = validator.validate(run);
        assertFalse(errors.isEmpty(), "Should have validation errors for null startTime");
        assertTrue(errors.stream().anyMatch(e -> e.contains("startTime")), 
                "Should have error about missing startTime");
    }

    @Test
    void validateTestRunWithInvalidNestedSuite() throws Exception {
        // Create a suite with missing required field via JSON
        String json = "{\"id\":\"run-001\",\"name\":\"Test Run\",\"status\":\"PASSED\"," +
                "\"startTime\":\"2024-01-01T10:00:00Z\"," +
                "\"suites\":[{\"name\":\"Suite 1\",\"status\":\"PASSED\",\"testCases\":[]}]}";
        TestRun run = objectMapper.readValue(json, TestRun.class);
        
        List<String> errors = validator.validate(run);
        assertFalse(errors.isEmpty(), "Should have validation errors for invalid nested suite");
        assertTrue(errors.stream().anyMatch(e -> e.contains("suite") || e.contains("id")), 
                "Should have error about invalid suite");
    }

    @Test
    void validateTestRunWithEmptySuites() throws Exception {
        String json = "{\"id\":\"run-001\",\"name\":\"Test Run\",\"status\":\"PASSED\"," +
                "\"startTime\":\"2024-01-01T10:00:00Z\",\"suites\":[]}";
        TestRun run = objectMapper.readValue(json, TestRun.class);
        
        List<String> errors = validator.validate(run);
        assertFalse(errors.isEmpty(), "Should have validation errors for empty suites list");
        assertTrue(errors.stream().anyMatch(e -> e.contains("suites") && e.contains("empty")), 
                "Should have error about empty suites list");
    }

    @Test
    void validateTestSuiteWithNullId() throws Exception {
        String json = "{\"name\":\"Suite 1\",\"status\":\"PASSED\",\"testCases\":[]}";
        TestSuite suite = objectMapper.readValue(json, TestSuite.class);
        
        List<String> errors = validator.validate(suite);
        assertFalse(errors.isEmpty(), "Should have validation errors for null id");
        assertTrue(errors.stream().anyMatch(e -> e.contains("id")), 
                "Should have error about missing id");
    }

    @Test
    void validateTestSuiteWithEmptyName() throws Exception {
        String json = "{\"id\":\"suite-001\",\"name\":\"\",\"status\":\"PASSED\",\"testCases\":[]}";
        TestSuite suite = objectMapper.readValue(json, TestSuite.class);
        
        List<String> errors = validator.validate(suite);
        assertFalse(errors.isEmpty(), "Should have validation errors for empty name");
        assertTrue(errors.stream().anyMatch(e -> e.contains("name")), 
                "Should have error about empty name");
    }

    @Test
    void validateTestSuiteWithNullStatus() throws Exception {
        String json = "{\"id\":\"suite-001\",\"name\":\"Suite 1\",\"testCases\":[]}";
        TestSuite suite = objectMapper.readValue(json, TestSuite.class);
        
        List<String> errors = validator.validate(suite);
        assertFalse(errors.isEmpty(), "Should have validation errors for null status");
        assertTrue(errors.stream().anyMatch(e -> e.contains("status")), 
                "Should have error about missing status");
    }

    @Test
    void validateTestSuiteWithInvalidNestedTestCase() throws Exception {
        String json = "{\"id\":\"suite-001\",\"name\":\"Suite 1\",\"status\":\"PASSED\"," +
                "\"testCases\":[{\"name\":\"Test 1\",\"status\":\"PASSED\"}]}";
        TestSuite suite = objectMapper.readValue(json, TestSuite.class);
        
        List<String> errors = validator.validate(suite);
        assertFalse(errors.isEmpty(), "Should have validation errors for invalid nested test case");
        assertTrue(errors.stream().anyMatch(e -> e.contains("test") || e.contains("id")), 
                "Should have error about invalid test case");
    }

    @Test
    void validateTestSuiteWithEmptyTestCases() throws Exception {
        String json = "{\"id\":\"suite-001\",\"name\":\"Suite 1\",\"status\":\"PASSED\",\"testCases\":[]}";
        TestSuite suite = objectMapper.readValue(json, TestSuite.class);
        
        List<String> errors = validator.validate(suite);
        assertFalse(errors.isEmpty(), "Should have validation errors for empty testCases list");
        assertTrue(errors.stream().anyMatch(e -> e.contains("testCases") && e.contains("empty")), 
                "Should have error about empty testCases list");
    }

    @Test
    void validateTestCaseWithNullId() throws Exception {
        String json = "{\"name\":\"Test 1\",\"status\":\"PASSED\"}";
        TestCase testCase = objectMapper.readValue(json, TestCase.class);
        
        List<String> errors = validator.validate(testCase);
        assertFalse(errors.isEmpty(), "Should have validation errors for null id");
        assertTrue(errors.stream().anyMatch(e -> e.contains("id")), 
                "Should have error about missing id");
    }

    @Test
    void validateTestCaseWithEmptyName() throws Exception {
        String json = "{\"id\":\"test-001\",\"name\":\"\",\"status\":\"PASSED\"}";
        TestCase testCase = objectMapper.readValue(json, TestCase.class);
        
        List<String> errors = validator.validate(testCase);
        assertFalse(errors.isEmpty(), "Should have validation errors for empty name");
        assertTrue(errors.stream().anyMatch(e -> e.contains("name")), 
                "Should have error about empty name");
    }

    @Test
    void validateTestCaseWithNullStatus() throws Exception {
        String json = "{\"id\":\"test-001\",\"name\":\"Test 1\"}";
        TestCase testCase = objectMapper.readValue(json, TestCase.class);
        
        List<String> errors = validator.validate(testCase);
        assertFalse(errors.isEmpty(), "Should have validation errors for null status");
        assertTrue(errors.stream().anyMatch(e -> e.contains("status")), 
                "Should have error about missing status");
    }

    @Test
    void validateTestCaseWithInvalidNestedStep() throws Exception {
        String json = "{\"id\":\"test-001\",\"name\":\"Test 1\",\"status\":\"PASSED\"," +
                "\"steps\":[{\"status\":\"PASSED\"}]}";
        TestCase testCase = objectMapper.readValue(json, TestCase.class);
        
        List<String> errors = validator.validate(testCase);
        assertFalse(errors.isEmpty(), "Should have validation errors for invalid nested step");
        assertTrue(errors.stream().anyMatch(e -> e.contains("step") || e.contains("name")), 
                "Should have error about invalid step");
    }

    @Test
    void validateTestCaseWithInvalidNestedArtifact() throws Exception {
        String json = "{\"id\":\"test-001\",\"name\":\"Test 1\",\"status\":\"PASSED\"," +
                "\"artifacts\":[{\"name\":\"screenshot\",\"type\":\"image\"}]}";
        TestCase testCase = objectMapper.readValue(json, TestCase.class);
        
        List<String> errors = validator.validate(testCase);
        assertFalse(errors.isEmpty(), "Should have validation errors for invalid nested artifact");
        assertTrue(errors.stream().anyMatch(e -> e.contains("artifact") || e.contains("path") || e.contains("timestamp")), 
                "Should have error about invalid artifact");
    }

    @Test
    void validateTestCaseWithInvalidNestedMetric() throws Exception {
        String json = "{\"id\":\"test-001\",\"name\":\"Test 1\",\"status\":\"PASSED\"," +
                "\"metrics\":[{\"name\":\"cpu\",\"value\":50.0}]}";
        TestCase testCase = objectMapper.readValue(json, TestCase.class);
        
        List<String> errors = validator.validate(testCase);
        assertFalse(errors.isEmpty(), "Should have validation errors for invalid nested metric");
        assertTrue(errors.stream().anyMatch(e -> e.contains("metric") || e.contains("unit") || e.contains("timestamp")), 
                "Should have error about invalid metric");
    }

    @Test
    void validateTestStepWithNullName() throws Exception {
        String json = "{\"status\":\"PASSED\"}";
        TestStep step = objectMapper.readValue(json, TestStep.class);
        
        List<String> errors = validator.validate(step);
        assertFalse(errors.isEmpty(), "Should have validation errors for null name");
        assertTrue(errors.stream().anyMatch(e -> e.contains("name")), 
                "Should have error about missing name");
    }

    @Test
    void validateTestStepWithEmptyName() throws Exception {
        String json = "{\"name\":\"\",\"status\":\"PASSED\"}";
        TestStep step = objectMapper.readValue(json, TestStep.class);
        
        List<String> errors = validator.validate(step);
        assertFalse(errors.isEmpty(), "Should have validation errors for empty name");
        assertTrue(errors.stream().anyMatch(e -> e.contains("name")), 
                "Should have error about empty name");
    }

    @Test
    void validateTestStepWithNullStatus() throws Exception {
        String json = "{\"name\":\"Step 1\"}";
        TestStep step = objectMapper.readValue(json, TestStep.class);
        
        List<String> errors = validator.validate(step);
        assertFalse(errors.isEmpty(), "Should have validation errors for null status");
        assertTrue(errors.stream().anyMatch(e -> e.contains("status")), 
                "Should have error about missing status");
    }

    @Test
    void validateArtifactWithNullName() throws Exception {
        String json = "{\"type\":\"screenshot\",\"path\":\"/path/to/file\",\"timestamp\":\"2024-01-01T10:00:00Z\"}";
        Artifact artifact = objectMapper.readValue(json, Artifact.class);
        
        List<String> errors = validator.validate(artifact);
        assertFalse(errors.isEmpty(), "Should have validation errors for null name");
        assertTrue(errors.stream().anyMatch(e -> e.contains("name")), 
                "Should have error about missing name");
    }

    @Test
    void validateArtifactWithEmptyType() throws Exception {
        String json = "{\"name\":\"screenshot\",\"type\":\"\",\"path\":\"/path/to/file\",\"timestamp\":\"2024-01-01T10:00:00Z\"}";
        Artifact artifact = objectMapper.readValue(json, Artifact.class);
        
        List<String> errors = validator.validate(artifact);
        assertFalse(errors.isEmpty(), "Should have validation errors for empty type");
        assertTrue(errors.stream().anyMatch(e -> e.contains("type")), 
                "Should have error about empty type");
    }

    @Test
    void validateArtifactWithNullPath() throws Exception {
        String json = "{\"name\":\"screenshot\",\"type\":\"image\",\"timestamp\":\"2024-01-01T10:00:00Z\"}";
        Artifact artifact = objectMapper.readValue(json, Artifact.class);
        
        List<String> errors = validator.validate(artifact);
        assertFalse(errors.isEmpty(), "Should have validation errors for null path");
        assertTrue(errors.stream().anyMatch(e -> e.contains("path")), 
                "Should have error about missing path");
    }

    @Test
    void validateArtifactWithNullTimestamp() throws Exception {
        String json = "{\"name\":\"screenshot\",\"type\":\"image\",\"path\":\"/path/to/file\"}";
        Artifact artifact = objectMapper.readValue(json, Artifact.class);
        
        List<String> errors = validator.validate(artifact);
        assertFalse(errors.isEmpty(), "Should have validation errors for null timestamp");
        assertTrue(errors.stream().anyMatch(e -> e.contains("timestamp")), 
                "Should have error about missing timestamp");
    }

    @Test
    void validateMetricWithNullName() throws Exception {
        String json = "{\"value\":100.0,\"unit\":\"ms\",\"timestamp\":\"2024-01-01T10:00:00Z\"}";
        Metric metric = objectMapper.readValue(json, Metric.class);
        
        List<String> errors = validator.validate(metric);
        assertFalse(errors.isEmpty(), "Should have validation errors for null name");
        assertTrue(errors.stream().anyMatch(e -> e.contains("name")), 
                "Should have error about missing name");
    }

    @Test
    void validateMetricWithEmptyUnit() throws Exception {
        String json = "{\"name\":\"cpu\",\"value\":50.0,\"unit\":\"\",\"timestamp\":\"2024-01-01T10:00:00Z\"}";
        Metric metric = objectMapper.readValue(json, Metric.class);
        
        List<String> errors = validator.validate(metric);
        assertFalse(errors.isEmpty(), "Should have validation errors for empty unit");
        assertTrue(errors.stream().anyMatch(e -> e.contains("unit")), 
                "Should have error about empty unit");
    }

    @Test
    void validateMetricWithNullTimestamp() throws Exception {
        String json = "{\"name\":\"cpu\",\"value\":50.0,\"unit\":\"ms\"}";
        Metric metric = objectMapper.readValue(json, Metric.class);
        
        List<String> errors = validator.validate(metric);
        assertFalse(errors.isEmpty(), "Should have validation errors for null timestamp");
        assertTrue(errors.stream().anyMatch(e -> e.contains("timestamp")), 
                "Should have error about missing timestamp");
    }

    @Test
    void validateComplexNestedStructureWithMultipleErrors() throws Exception {
        // TestRun with invalid nested objects at multiple levels
        String json = "{\"id\":\"run-001\",\"name\":\"Test Run\",\"status\":\"PASSED\"," +
                "\"startTime\":\"2024-01-01T10:00:00Z\"," +
                "\"suites\":[" +
                "{\"name\":\"Suite 1\",\"status\":\"PASSED\",\"testCases\":[" +  // Suite missing id
                "{\"name\":\"Test 1\",\"status\":\"PASSED\"}" +  // TestCase missing id
                "]}]}";
        TestRun run = objectMapper.readValue(json, TestRun.class);
        
        List<String> errors = validator.validate(run);
        assertFalse(errors.isEmpty(), "Should have multiple validation errors");
        assertTrue(errors.size() >= 2, "Should have at least 2 errors (suite id and test case id)");
    }
}

