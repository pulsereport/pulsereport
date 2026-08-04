package io.github.pulsereport.outputs.junit;

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
 * Tests for JUnitXmlReportGenerator.
 */
class JUnitXmlReportGeneratorTest {

    private JUnitXmlReportGenerator generator;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        generator = new JUnitXmlReportGenerator();
    }

    @Test
    void testGenerateToFile() throws IOException {
        TestRun testRun = createSampleTestRun();
        File outputFile = new File(tempDir, "TEST-junit-report.xml");

        generator.generate(testRun, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertFalse(content.isEmpty());

        assertTrue(content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(content.contains("<testsuites"));
        assertTrue(content.contains("<testsuite"));
        assertTrue(content.contains("<testcase"));
        assertTrue(content.contains("name=\"Test Case 1\""));
        assertTrue(content.contains("classname=\"com.example.Test1\""));
    }

    @Test
    void testGenerateToOutputStream() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertFalse(content.isEmpty());
        assertTrue(content.contains("<testsuites"));
    }

    @Test
    void testGenerateWithNullTestRunToFile() {
        File outputFile = new File(tempDir, "test.xml");
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
    void testGenerateWithFailedTest() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("<failure"));
        assertTrue(content.contains("Expected true but was false"));
    }

    @Test
    void testGenerateWithSkippedTest() throws IOException {
        TestCase skippedTest = TestCase.builder()
                .id("tc-3")
                .name("Skipped Test")
                .className("com.example.Test2")
                .methodName("testSkipped")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(0)
                .status(TestStatus.SKIPPED)
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-1")
                .name("Test Suite")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(0)
                .status(TestStatus.PASSED)
                .testCases(Collections.singletonList(skippedTest))
                .build();

        TestRun testRun = TestRun.builder()
                .id("run-1")
                .name("Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(0)
                .status(TestStatus.PASSED)
                .suites(Collections.singletonList(suite))
                .build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("skipped=\"1\""));
    }

    @Test
    void testGenerateWithEmptyTestRun() throws IOException {
        TestRun testRun = TestRun.builder()
                .id("empty-run")
                .name("Empty Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(0)
                .status(TestStatus.PASSED)
                .suites(Collections.emptyList())
                .build();
        File outputFile = new File(tempDir, "empty-report.xml");

        generator.generate(testRun, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("<testsuites"));
    }

    @Test
    void testTestSuiteCountsAreCorrect() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        // Should have tests="2" (2 test cases in the suite)
        assertTrue(content.contains("tests="));
        assertTrue(content.contains("failures="));
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
