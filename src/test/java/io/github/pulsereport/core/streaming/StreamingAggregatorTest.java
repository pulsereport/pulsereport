package io.github.pulsereport.core.streaming;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.performance.PerformanceConfig;

/**
 * Unit tests for StreamingAggregator.
 *
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class StreamingAggregatorTest {

    private StreamingAggregator streamingAggregator;
    private Path tempDir;
    private PerformanceConfig config;

    @BeforeEach
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("streaming-test-");
        config = PerformanceConfig.builder()
                .streamingMode(true)
                .batchSize(100)
                .tempStorageDir(tempDir)
                .autoCleanup(true)
                .build();

        streamingAggregator = new StreamingAggregator(config);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (streamingAggregator != null) {
            streamingAggregator.close();
        }
    }

    @Test
    public void testRecordAndBuildTestRun() throws IOException {
        List<ITestResult> results = generateMockResults(50);

        for (ITestResult result : results) {
            streamingAggregator.recordTestResult(result);
        }

        ISuite suite = createMockSuite("TestSuite", results);
        TestRun testRun = streamingAggregator.buildTestRun(suite);

        assertNotNull(testRun);
        assertEquals(50, testRun.getTotalTests());
        assertEquals("TestSuite", testRun.getName());
    }

    @Test
    public void testAutomaticFlushWhenBatchSizeReached() throws IOException {
        PerformanceConfig smallBatchConfig = PerformanceConfig.builder()
                .streamingMode(true)
                .batchSize(10)
                .tempStorageDir(tempDir)
                .autoCleanup(false)
                .build();

        StreamingAggregator aggregator = new StreamingAggregator(smallBatchConfig);

        List<ITestResult> results = generateMockResults(25);

        for (ITestResult result : results) {
            aggregator.recordTestResult(result);
        }

        // Should have flushed at least 2 batches (0-9, 10-19)
        ISuite suite = createMockSuite("TestSuite", results);
        TestRun testRun = aggregator.buildTestRun(suite);

        assertEquals(25, testRun.getTotalTests());

        aggregator.close();
    }

    @Test
    public void testManualFlush() throws IOException {
        List<ITestResult> results = generateMockResults(5);

        for (ITestResult result : results) {
            streamingAggregator.recordTestResult(result);
        }

        streamingAggregator.flush();

        ISuite suite = createMockSuite("TestSuite", results);
        TestRun testRun = streamingAggregator.buildTestRun(suite);

        assertEquals(5, testRun.getTotalTests());
    }

    @Test
    public void testLargeDataset() throws IOException {
        List<ITestResult> results = generateMockResults(1000);

        for (ITestResult result : results) {
            streamingAggregator.recordTestResult(result);
        }

        ISuite suite = createMockSuite("LargeTestSuite", results);
        TestRun testRun = streamingAggregator.buildTestRun(suite);

        assertNotNull(testRun);
        assertEquals(1000, testRun.getTotalTests());
    }

    @Test
    public void testHandlesRetries() throws IOException {
        ITestResult failedAttempt = createMockResult("test1", ITestResult.FAILURE);
        ITestResult passedAttempt = createMockResult("test1", ITestResult.SUCCESS);

        streamingAggregator.recordTestResult(failedAttempt);
        streamingAggregator.recordTestResult(passedAttempt);

        List<ITestResult> results = Arrays.asList(failedAttempt, passedAttempt);
        ISuite suite = createMockSuite("TestSuite", results);
        TestRun testRun = streamingAggregator.buildTestRun(suite);

        assertEquals(1, testRun.getTotalTests());
    }

    @Test
    public void testPassedExpectedExceptionDoesNotExposeThrowableDetails() throws IOException {
        ITestResult passedExpectedException = createMockResult(
                "testDivisionByZero",
                ITestResult.SUCCESS,
                new ArithmeticException("Division by zero")
        );

        streamingAggregator.recordTestResult(passedExpectedException);

        ISuite suite = createMockSuite("TestSuite", Collections.singletonList(passedExpectedException));
        TestRun testRun = streamingAggregator.buildTestRun(suite);

        assertEquals(1, testRun.getTotalTests());
        assertEquals(1, testRun.getPassedTests());

        TestCase testCase = testRun.getSuites().get(0).getTestCases().get(0);
        assertEquals(TestStatus.PASSED, testCase.getStatus());
        assertNull(testCase.getErrorMessage(), "Passed expected-exception tests should not expose an error message");
        assertNull(testCase.getStackTrace(), "Passed expected-exception tests should not expose a stack trace");
    }

    @Test
    public void testFailedResultRetainsThrowableDetails() throws IOException {
        ITestResult failedResult = createMockResult(
                "testFailure",
                ITestResult.FAILURE,
                new AssertionError("Expected false but was true")
        );

        streamingAggregator.recordTestResult(failedResult);

        ISuite suite = createMockSuite("TestSuite", Collections.singletonList(failedResult));
        TestRun testRun = streamingAggregator.buildTestRun(suite);

        TestCase testCase = testRun.getSuites().get(0).getTestCases().get(0);
        assertEquals(TestStatus.FAILED, testCase.getStatus());
        assertEquals("Expected false but was true", testCase.getErrorMessage());
        assertNotNull(testCase.getStackTrace());
        assertTrue(testCase.getStackTrace().contains("AssertionError"));
    }

    @Test
    public void testAutoCleanup() throws IOException {
        PerformanceConfig autoCleanupConfig = PerformanceConfig.builder()
                .streamingMode(true)
                .batchSize(10)
                .tempStorageDir(tempDir)
                .autoCleanup(true)
                .build();

        StreamingAggregator aggregator = new StreamingAggregator(autoCleanupConfig);

        List<ITestResult> results = generateMockResults(20);

        for (ITestResult result : results) {
            aggregator.recordTestResult(result);
        }

        ISuite suite = createMockSuite("TestSuite", results);
        aggregator.buildTestRun(suite);

        aggregator.close();

        assertFalse(Files.exists(tempDir));
    }

    @Test
    public void testNoAutoCleanup() throws IOException {
        PerformanceConfig noCleanupConfig = PerformanceConfig.builder()
                .streamingMode(true)
                .batchSize(10)
                .tempStorageDir(tempDir)
                .autoCleanup(false)
                .build();

        StreamingAggregator aggregator = new StreamingAggregator(noCleanupConfig);

        List<ITestResult> results = generateMockResults(20);

        for (ITestResult result : results) {
            aggregator.recordTestResult(result);
        }

        ISuite suite = createMockSuite("TestSuite", results);
        aggregator.buildTestRun(suite);

        aggregator.close();

        assertTrue(Files.exists(tempDir));
    }

    @Test
    public void testEmptyTestRun() throws IOException {
        ISuite suite = createMockSuite("EmptySuite", Collections.emptyList());
        TestRun testRun = streamingAggregator.buildTestRun(suite);

        assertNotNull(testRun);
        assertEquals(0, testRun.getTotalTests());
    }

    @Test
    public void testNullResultIgnored() throws IOException {
        streamingAggregator.recordTestResult(null);

        ISuite suite = createMockSuite("TestSuite", Collections.emptyList());
        TestRun testRun = streamingAggregator.buildTestRun(suite);

        assertEquals(0, testRun.getTotalTests());
    }

    @Test
    public void testCalculatesStatisticsCorrectly() throws IOException {
        List<ITestResult> results = new ArrayList<>();

        for (int i = 0; i < 70; i++) {
            results.add(createMockResult("passed" + i, ITestResult.SUCCESS));
        }

        for (int i = 0; i < 20; i++) {
            results.add(createMockResult("failed" + i, ITestResult.FAILURE));
        }

        for (int i = 0; i < 10; i++) {
            results.add(createMockResult("skipped" + i, ITestResult.SKIP));
        }

        for (ITestResult result : results) {
            streamingAggregator.recordTestResult(result);
        }

        ISuite suite = createMockSuite("TestSuite", results);
        TestRun testRun = streamingAggregator.buildTestRun(suite);

        assertEquals(100, testRun.getTotalTests());
        assertEquals(70, testRun.getPassedTests());
        assertEquals(20, testRun.getFailedTests());
        assertEquals(10, testRun.getSkippedTests());
    }

    /**
     * Helper: Generate mock test results
     */
    private List<ITestResult> generateMockResults(int count) {
        List<ITestResult> results = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            results.add(createMockResult("test" + i, ITestResult.SUCCESS));
        }

        return results;
    }

    /**
     * Helper: Create a single mock test result
     */
    private ITestResult createMockResult(String testName, int status) {
        return createMockResult(testName, status,
                status == ITestResult.FAILURE ? new AssertionError("Test failed") : null);
    }

    private ITestResult createMockResult(String testName, int status, Throwable throwable) {
        ITestResult result = mock(ITestResult.class);
        ITestNGMethod method = mock(ITestNGMethod.class);
        ITestContext context = mock(ITestContext.class);
        ISuite suite = mock(ISuite.class);

        when(method.getRealClass()).thenReturn((Class<?>) StreamingAggregatorTest.class);
        when(method.getMethodName()).thenReturn(testName);
        when(result.getMethod()).thenReturn(method);

        when(context.getName()).thenReturn("TestContext");
        when(context.getSuite()).thenReturn(suite);
        when(context.getStartDate()).thenReturn(new Date(System.currentTimeMillis() - 60000));
        when(context.getEndDate()).thenReturn(new Date(System.currentTimeMillis()));
        when(result.getTestContext()).thenReturn(context);

        when(suite.getName()).thenReturn("TestSuite");

        when(result.getName()).thenReturn(testName);
        when(result.getStatus()).thenReturn(status);
        when(result.getStartMillis()).thenReturn(System.currentTimeMillis() - 1000);
        when(result.getEndMillis()).thenReturn(System.currentTimeMillis());
        when(result.getThrowable()).thenReturn(throwable);
        when(result.getParameters()).thenReturn(new Object[0]);

        return result;
    }

    /**
     * Helper: Create mock ISuite
     */
    private ISuite createMockSuite(String suiteName, List<ITestResult> results) {
        ISuite suite = mock(ISuite.class);
        ISuiteResult suiteResult = mock(ISuiteResult.class);
        ITestContext context = mock(ITestContext.class);

        when(suite.getName()).thenReturn(suiteName);

        when(context.getName()).thenReturn(suiteName + "Context");
        when(context.getStartDate()).thenReturn(new Date(System.currentTimeMillis() - 60000));
        when(context.getEndDate()).thenReturn(new Date(System.currentTimeMillis()));

        Set<ITestResult> passed = new HashSet<>();
        Set<ITestResult> failed = new HashSet<>();
        Set<ITestResult> skipped = new HashSet<>();

        for (ITestResult result : results) {
            switch (result.getStatus()) {
                case ITestResult.SUCCESS:
                    passed.add(result);
                    break;
                case ITestResult.FAILURE:
                    failed.add(result);
                    break;
                case ITestResult.SKIP:
                    skipped.add(result);
                    break;
            }
        }

        org.testng.IResultMap passedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap failedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap skippedTests = mock(org.testng.IResultMap.class);

        when(passedTests.getAllResults()).thenReturn(passed);
        when(failedTests.getAllResults()).thenReturn(failed);
        when(skippedTests.getAllResults()).thenReturn(skipped);

        when(context.getPassedTests()).thenReturn(passedTests);
        when(context.getFailedTests()).thenReturn(failedTests);
        when(context.getSkippedTests()).thenReturn(skippedTests);

        when(suiteResult.getTestContext()).thenReturn(context);

        Map<String, ISuiteResult> suiteResults = new HashMap<>();
        suiteResults.put(suiteName, suiteResult);
        when(suite.getResults()).thenReturn(suiteResults);

        return suite;
    }
}
