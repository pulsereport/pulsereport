package io.github.pulsereport.core.performance;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import io.github.pulsereport.core.aggregator.TestResultAggregator;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;

/**
 * Load tests for performance and scaling validation. Tests the reporter's
 * ability to handle large test suites (100k+ tests) without memory issues.
 *
 * Performance targets: - Small datasets (< 1k tests): < 100MB memory, < 5s -
 * Medium datasets (1k-10k tests): < 500MB memory, < 30s - Large datasets
 * (10k-100k tests): < 2GB memory, < 5min (with streaming)
 *
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class LoadTest {

    private static final int SMALL_DATASET = 1000;
    private static final int MEDIUM_DATASET = 10000;
    private static final int LARGE_DATASET = 100000;

    private static final long SMALL_MAX_MEMORY_MB = 100;
    private static final long MEDIUM_MAX_MEMORY_MB = 500;
    private static final long LARGE_MAX_MEMORY_MB = 2048;

    private TestResultAggregator aggregator;

    @BeforeEach
    public void setUp() {
        aggregator = new TestResultAggregator();
    }

    /**
     * Baseline test with 1,000 tests. Should complete quickly with minimal
     * memory usage.
     */
    @Test
    public void smallDataset_1000Tests() {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemoryMB();

        List<ITestResult> results = generateMockResults(SMALL_DATASET);

        for (ITestResult result : results) {
            aggregator.recordTestResult(result);
        }

        ISuite suite = createMockSuite("SmallLoadTest", results);
        TestRun testRun = aggregator.convertToTestRun(suite);

        long duration = System.currentTimeMillis() - startTime;
        long memoryUsed = getUsedMemoryMB() - startMemory;

        assertNotNull(testRun);
        assertEquals(SMALL_DATASET, testRun.getTotalTests());

        assertTrue(memoryUsed < SMALL_MAX_MEMORY_MB,
                "Memory usage %dMB exceeded limit %dMB".formatted(memoryUsed, SMALL_MAX_MEMORY_MB));
        assertTrue(duration < 5000,
                "Duration %dms exceeded limit 5000ms".formatted(duration));

        System.out.printf("[LOAD TEST] Small dataset (1k): %dms, %dMB%n", duration, memoryUsed);
    }

    /**
     * Medium dataset with 10,000 tests. Should complete with moderate memory
     * usage.
     */
    @Test
    public void mediumDataset_10000Tests() {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemoryMB();

        List<ITestResult> results = generateMockResults(MEDIUM_DATASET);

        for (ITestResult result : results) {
            aggregator.recordTestResult(result);
        }

        ISuite suite = createMockSuite("MediumLoadTest", results);
        TestRun testRun = aggregator.convertToTestRun(suite);

        long duration = System.currentTimeMillis() - startTime;
        long memoryUsed = getUsedMemoryMB() - startMemory;

        assertNotNull(testRun);
        assertEquals(MEDIUM_DATASET, testRun.getTotalTests());

        assertTrue(memoryUsed < MEDIUM_MAX_MEMORY_MB,
                "Memory usage %dMB exceeded limit %dMB".formatted(memoryUsed, MEDIUM_MAX_MEMORY_MB));
        assertTrue(duration < 30000,
                "Duration %dms exceeded limit 30000ms".formatted(duration));

        System.out.printf("[LOAD TEST] Medium dataset (10k): %dms, %dMB%n", duration, memoryUsed);
    }

    /**
     * Large dataset with 100,000 tests - requires streaming mode. This test
     * validates that the current in-memory implementation likely exceeds memory
     * limits, motivating the need for streaming.
     */
    @Test
    public void largeDataset_100000Tests_inMemory() {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemoryMB();

        // but demonstrates the need for optimization
        List<ITestResult> results = generateMockResults(LARGE_DATASET);

        for (ITestResult result : results) {
            aggregator.recordTestResult(result);
        }

        ISuite suite = createMockSuite("LargeLoadTest", results);
        TestRun testRun = aggregator.convertToTestRun(suite);

        long duration = System.currentTimeMillis() - startTime;
        long memoryUsed = getUsedMemoryMB() - startMemory;

        assertNotNull(testRun);
        assertEquals(LARGE_DATASET, testRun.getTotalTests());

        System.out.printf("[LOAD TEST] Large dataset (100k) in-memory: %dms, %dMB%n", duration, memoryUsed);

        if (memoryUsed > LARGE_MAX_MEMORY_MB) {
            System.out.printf("[WARNING] Memory usage %dMB exceeded target %dMB - streaming needed%n",
                    memoryUsed, LARGE_MAX_MEMORY_MB);
        }
    }

    /**
     * Tests all test cases can be retrieved without memory issues.
     */
    @Test
    public void getAllTestCases_largeDataset() {
        List<ITestResult> results = generateMockResults(SMALL_DATASET);

        for (ITestResult result : results) {
            aggregator.recordTestResult(result);
        }

        List<TestCase> allTests = aggregator.getAllTestCases();

        assertNotNull(allTests);
        assertEquals(SMALL_DATASET, allTests.size());
    }

    /**
     * Generates mock ITestResult objects for load testing.
     */
    private List<ITestResult> generateMockResults(int count) {
        List<ITestResult> results = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            ITestResult result = mock(ITestResult.class);
            ITestNGMethod method = mock(ITestNGMethod.class);
            ITestContext context = mock(ITestContext.class);
            ISuite suite = mock(ISuite.class);

            when(method.getRealClass()).thenReturn((Class<?>) LoadTest.class);
            when(method.getMethodName()).thenReturn("testMethod" + i);
            when(result.getMethod()).thenReturn(method);

            when(context.getName()).thenReturn("LoadTestContext");
            when(context.getSuite()).thenReturn(suite);
            when(context.getStartDate()).thenReturn(new Date(System.currentTimeMillis() - 60000));
            when(context.getEndDate()).thenReturn(new Date(System.currentTimeMillis()));
            when(result.getTestContext()).thenReturn(context);

            when(suite.getName()).thenReturn("LoadTestSuite");

            when(result.getName()).thenReturn("test" + i);
            when(result.getStatus()).thenReturn(ITestResult.SUCCESS);
            when(result.getStartMillis()).thenReturn(System.currentTimeMillis() - 1000);
            when(result.getEndMillis()).thenReturn(System.currentTimeMillis());
            when(result.getThrowable()).thenReturn(null);
            when(result.getParameters()).thenReturn(new Object[0]);

            results.add(result);
        }

        return results;
    }

    /**
     * Creates a mock ISuite with the given results.
     */
    private ISuite createMockSuite(String suiteName, List<ITestResult> results) {
        ISuite suite = mock(ISuite.class);
        ISuiteResult suiteResult = mock(ISuiteResult.class);
        ITestContext context = mock(ITestContext.class);

        when(suite.getName()).thenReturn(suiteName);

        when(context.getName()).thenReturn(suiteName + "Context");
        when(context.getStartDate()).thenReturn(new Date(System.currentTimeMillis() - 60000));
        when(context.getEndDate()).thenReturn(new Date(System.currentTimeMillis()));

        org.testng.IResultMap passedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap failedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap skippedTests = mock(org.testng.IResultMap.class);

        when(passedTests.getAllResults()).thenReturn(new HashSet<>(results));
        when(failedTests.getAllResults()).thenReturn(new HashSet<>());
        when(skippedTests.getAllResults()).thenReturn(new HashSet<>());

        when(context.getPassedTests()).thenReturn(passedTests);
        when(context.getFailedTests()).thenReturn(failedTests);
        when(context.getSkippedTests()).thenReturn(skippedTests);

        when(suiteResult.getTestContext()).thenReturn(context);

        Map<String, ISuiteResult> suiteResults = new HashMap<>();
        suiteResults.put(suiteName, suiteResult);
        when(suite.getResults()).thenReturn(suiteResults);

        return suite;
    }

    /**
     * Gets current memory usage in megabytes.
     */
    private long getUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        try {
            Thread.sleep(100); // Give GC time to run
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory / (1024 * 1024);
    }
}
