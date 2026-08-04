package io.github.pulsereport.outputs.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JsonReportGenerator.
 */
class JsonReportGeneratorTest {

    private JsonReportGenerator generator;
    private ObjectMapper objectMapper;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        generator = new JsonReportGenerator();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testGenerateToFile() throws IOException {
        TestRun testRun = createSampleTestRun();
        File outputFile = new File(tempDir, "test-report.json");

        generator.generate(testRun, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertFalse(content.isEmpty());

        JsonNode jsonNode = objectMapper.readTree(content);
        assertNotNull(jsonNode);
        assertEquals("test-run-1", jsonNode.get("id").asText());
        assertEquals("Sample Test Run", jsonNode.get("name").asText());
    }

    @Test
    void testGenerateToOutputStream() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertFalse(content.isEmpty());

        JsonNode jsonNode = objectMapper.readTree(content);
        assertNotNull(jsonNode);
        assertEquals("test-run-1", jsonNode.get("id").asText());
    }

    @Test
    void testGenerateWithNullTestRunToFile() {
        File outputFile = new File(tempDir, "test-report.json");
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null, outputFile));
    }

    @Test
    void testGenerateWithNullFileToFile() {
        TestRun testRun = createSampleTestRun();
        assertThrows(IllegalArgumentException.class, () -> generator.generate(testRun, (File) null));
    }

    @Test
    void testGenerateWithNullTestRunToStream() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null, outputStream));
    }

    @Test
    void testGenerateWithNullOutputStream() {
        TestRun testRun = createSampleTestRun();
        assertThrows(IllegalArgumentException.class, () -> generator.generate(testRun, (java.io.OutputStream) null));
    }

    @Test
    void testGenerateWithEmptyTestRun() throws IOException {
        TestRun testRun = TestRun.builder()
                .id("empty-run")
                .name("Empty Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .status(TestStatus.PASSED)
                .suites(Collections.emptyList())
                .build();
        File outputFile = new File(tempDir, "empty-report.json");

        generator.generate(testRun, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        JsonNode jsonNode = objectMapper.readTree(content);
        assertEquals("empty-run", jsonNode.get("id").asText());
    }

    @Test
    void testJsonIsPrettyPrinted() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        // Pretty-printed JSON should contain newlines and indentation
        assertTrue(content.contains("\n"));
        assertTrue(content.contains("  ")); // Indentation
    }

    private TestRun createSampleTestRun() {
        TestCase testCase1 = TestCase.builder()
                .id("tc-1")
                .name("Test Case 1")
                .className("com.example.Test1")
                .methodName("testMethod1")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .build();

        TestCase testCase2 = TestCase.builder()
                .id("tc-2")
                .name("Test Case 2")
                .className("com.example.Test1")
                .methodName("testMethod2")
                .startTime(Instant.parse("2026-02-16T10:00:02Z"))
                .endTime(Instant.parse("2026-02-16T10:00:03Z"))
                .duration(1000)
                .status(TestStatus.FAILED)
                .errorMessage("Expected true but was false")
                .stackTrace("java.lang.AssertionError: Expected true but was false\n\tat com.example.Test1.testMethod2(Test1.java:42)")
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-1")
                .name("Test Suite 1")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:03Z"))
                .duration(3000)
                .status(TestStatus.FAILED)
                .testCases(Arrays.asList(testCase1, testCase2))
                .build();

        return TestRun.builder()
                .id("test-run-1")
                .name("Sample Test Run")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:03Z"))
                .duration(3000)
                .status(TestStatus.FAILED)
                .suites(Arrays.asList(suite))
                .build();
    }
}
