package io.github.pulsereport.core.streaming;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;

import io.github.pulsereport.core.aggregator.TestResultAggregator;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestSuite;
import io.github.pulsereport.core.performance.PerformanceConfig;
import io.github.pulsereport.core.storage.TempStorageManager;

/**
 * Memory-efficient streaming aggregator for large test suites. Processes test
 * results in batches and stores intermediate results to disk, allowing handling
 * of 100k+ tests without memory issues.
 *
 * <p>
 * Differences from TestResultAggregator:</p>
 * <ul>
 * <li>Batches results to disk when batch size is reached</li>
 * <li>Uses temporary storage for intermediate results</li>
 * <li>Streams test cases during final aggregation</li>
 * <li>Lower memory footprint for large datasets</li>
 * </ul>
 *
 * <p>
 * Usage:</p>
 * <pre>
 * PerformanceConfig config = PerformanceConfig.forLargeDatasets();
 * StreamingAggregator aggregator = new StreamingAggregator(config);
 *
 * // Record results
 * for (ITestResult result : results) {
 *     aggregator.recordTestResult(result);
 * }
 *
 * // Build test run (reads from disk)
 * TestRun testRun = aggregator.buildTestRun(suite);
 *
 * // Clean up
 * aggregator.close();
 * </pre>
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class StreamingAggregator implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(StreamingAggregator.class);

    private final PerformanceConfig config;
    private final TempStorageManager storageManager;
    private final TestResultAggregator baseAggregator;

    private final Map<String, List<ITestResult>> currentBatch;
    private int currentBatchSize;
    private int batchCounter;

    /**
     * Creates a new StreamingAggregator.
     *
     * @param config performance configuration
     * @throws IOException if temp storage cannot be initialized
     */
    public StreamingAggregator(PerformanceConfig config) throws IOException {
        this.config = config;
        this.storageManager = new TempStorageManager(config.getTempStorageDir());
        this.baseAggregator = new TestResultAggregator();
        this.currentBatch = new ConcurrentHashMap<>();
        this.currentBatchSize = 0;
        this.batchCounter = 0;

        logger.info("StreamingAggregator initialized with config: {}", config);
    }

    /**
     * Records a test result. Automatically flushes to disk when batch size is
     * reached.
     *
     * @param result the TestNG test result
     */
    public synchronized void recordTestResult(ITestResult result) {
        if (result == null) {
            logger.warn("Received null test result, ignoring");
            return;
        }

        String testKey = baseAggregator.getTestKey(result);
        currentBatch.computeIfAbsent(testKey, k -> Collections.synchronizedList(new ArrayList<>())).add(result);
        currentBatchSize++;

        logger.trace("Recorded test result: {} (batch size: {})", testKey, currentBatchSize);

        if (currentBatchSize >= config.getBatchSize()) {
            try {
                flush();
            } catch (IOException e) {
                logger.error("Failed to flush batch", e);
                throw new RuntimeException("Failed to flush batch", e);
            }
        }
    }

    /**
     * Manually flushes the current batch to disk.
     *
     * @throws IOException if flush fails
     */
    public synchronized void flush() throws IOException {
        if (currentBatch.isEmpty()) {
            logger.debug("Current batch is empty, nothing to flush");
            return;
        }

        List<TestCase> testCases = new ArrayList<>();
        for (String testKey : currentBatch.keySet()) {
            List<ITestResult> results = currentBatch.get(testKey);
            TestCase testCase = convertResultsToTestCase(testKey, results);
            testCases.add(testCase);
        }

        storageManager.writeBatch(testCases, batchCounter);
        logger.info("Flushed batch {} with {} test cases to disk", batchCounter, testCases.size());

        currentBatch.clear();
        currentBatchSize = 0;
        batchCounter++;
    }

    /**
     * Builds a TestRun from the suite, reading batches from disk.
     *
     * @param suite the TestNG suite
     * @return the aggregated TestRun
     * @throws IOException if reading from disk fails
     */
    public synchronized TestRun buildTestRun(ISuite suite) throws IOException {
        if (suite == null) {
            throw new IllegalArgumentException("ISuite cannot be null");
        }

        flush();

        List<TestCase> allTestCases;
        try {
            allTestCases = storageManager.readAllBatches().collect(Collectors.toList());
            logger.info("Read {} test cases from disk storage", allTestCases.size());
        } catch (IOException e) {
            logger.error("Failed to read batches from storage", e);
            throw e;
        }

        // Build test suites
        String runName = suite.getName();
        List<TestSuite> testSuites = new ArrayList<>();

        Map<String, ISuiteResult> suiteResults = suite.getResults();
        if (suiteResults != null && !suiteResults.isEmpty()) {
            for (Map.Entry<String, ISuiteResult> entry : suiteResults.entrySet()) {
                ITestContext context = entry.getValue().getTestContext();

                List<TestCase> contextTestCases = filterTestCasesForContext(allTestCases, context);

                TestSuite testSuite = buildTestSuite(context, contextTestCases);
                testSuites.add(testSuite);
            }
        }

        testSuites.sort(Comparator.comparing(TestSuite::getStartTime)
                .thenComparing(TestSuite::getName));

        int totalTests = testSuites.stream().mapToInt(TestSuite::getTotalTests).sum();
        int passedTests = testSuites.stream().mapToInt(TestSuite::getPassedTests).sum();
        int failedTests = testSuites.stream().mapToInt(TestSuite::getFailedTests).sum();
        int skippedTests = testSuites.stream().mapToInt(TestSuite::getSkippedTests).sum();

        TestStatus overallStatus = determineOverallStatus(testSuites);

        Instant startTime;
        Instant endTime;

        if (!testSuites.isEmpty()) {
            startTime = testSuites.stream()
                    .map(TestSuite::getStartTime)
                    .min(Instant::compareTo)
                    .orElse(Instant.ofEpochMilli(0));

            endTime = testSuites.stream()
                    .map(TestSuite::getEndTime)
                    .max(Instant::compareTo)
                    .orElse(startTime);
        } else {
            startTime = Instant.ofEpochMilli(0);
            endTime = Instant.ofEpochMilli(0);
        }

        long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

        return TestRun.builder()
                .id(UUID.randomUUID().toString())
                .name(runName)
                .startTime(startTime)
                .endTime(endTime)
                .duration(duration)
                .status(overallStatus)
                .suites(testSuites)
                .totalTests(totalTests)
                .passedTests(passedTests)
                .failedTests(failedTests)
                .skippedTests(skippedTests)
                .build();
    }

    /**
     * Closes the aggregator and cleans up resources. If auto-cleanup is
     * enabled, deletes temporary files.
     */
    @Override
    public void close() throws IOException {
        if (config.isAutoCleanup()) {
            logger.info("Auto-cleanup enabled, removing temporary storage");
            storageManager.cleanup();
        } else {
            logger.info("Auto-cleanup disabled, temporary files retained at: {}",
                    config.getTempStorageDir());
        }
    }

    /**
     * Converts a list of ITestResult (including retries) to a single TestCase.
     */
    private TestCase convertResultsToTestCase(String testKey, List<ITestResult> results) {
        List<ITestResult> sortedResults = new ArrayList<>(results);
        sortedResults.sort(Comparator.comparing(ITestResult::getStartMillis));

        ITestResult finalResult = sortedResults.get(sortedResults.size() - 1);

        boolean isFlaky = detectFlaky(sortedResults);
        int retryCount = sortedResults.size() - 1;

        String className = finalResult.getMethod().getRealClass().getName();
        String methodName = finalResult.getMethod().getMethodName();

        Instant startTime = Instant.ofEpochMilli(finalResult.getStartMillis());
        Instant endTime = Instant.ofEpochMilli(finalResult.getEndMillis());
        long duration = finalResult.getEndMillis() - finalResult.getStartMillis();

        TestStatus status;
        if (isFlaky) {
            status = TestStatus.FLAKY;
        } else {
            status = convertStatus(finalResult.getStatus());
        }

        Throwable detailThrowable = getDisplayedThrowable(status, finalResult.getThrowable());
        String errorMessage = extractErrorMessage(detailThrowable);
        String stackTrace = extractStackTrace(detailThrowable);

        return TestCase.builder()
                .id(testKey)
                .name(finalResult.getName())
                .className(className)
                .methodName(methodName)
                .startTime(startTime)
                .endTime(endTime)
                .duration(duration)
                .status(status)
                .errorMessage(errorMessage)
                .stackTrace(stackTrace)
                .retryCount(retryCount)
                .build();
    }

    /**
     * Filters test cases that belong to a specific context.
     */
    private List<TestCase> filterTestCasesForContext(List<TestCase> allTestCases, ITestContext context) {
        return allTestCases;
    }

    /**
     * Builds a TestSuite from a context and test cases.
     */
    private TestSuite buildTestSuite(ITestContext context, List<TestCase> testCases) {
        String suiteName = context.getName();
        Instant startTime = Instant.ofEpochMilli(context.getStartDate().getTime());
        Instant endTime = Instant.ofEpochMilli(context.getEndDate().getTime());
        long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

        testCases.sort(Comparator.comparing(TestCase::getStartTime)
                .thenComparing(TestCase::getClassName)
                .thenComparing(TestCase::getMethodName));

        int totalTests = testCases.size();
        int passedTests = (int) testCases.stream().filter(tc -> tc.getStatus() == TestStatus.PASSED).count();
        int failedTests = (int) testCases.stream().filter(tc -> tc.getStatus() == TestStatus.FAILED).count();
        int skippedTests = (int) testCases.stream().filter(tc -> tc.getStatus() == TestStatus.SKIPPED).count();
        int flakyTests = (int) testCases.stream().filter(tc -> tc.getStatus() == TestStatus.FLAKY).count();

        TestStatus suiteStatus = determineSuiteStatus(passedTests, failedTests, skippedTests, flakyTests, totalTests);

        return TestSuite.builder()
                .id(UUID.randomUUID().toString())
                .name(suiteName)
                .startTime(startTime)
                .endTime(endTime)
                .duration(duration)
                .status(suiteStatus)
                .testCases(testCases)
                .totalTests(totalTests)
                .passedTests(passedTests)
                .failedTests(failedTests)
                .skippedTests(skippedTests)
                .build();
    }

    /**
     * Detects if a test is flaky (earlier failures, final success).
     */
    private boolean detectFlaky(List<ITestResult> sortedResults) {
        if (sortedResults.size() <= 1) {
            return false;
        }

        ITestResult finalResult = sortedResults.get(sortedResults.size() - 1);
        if (finalResult.getStatus() != ITestResult.SUCCESS) {
            return false;
        }

        // Check if any earlier attempts failed
        for (int i = 0; i < sortedResults.size() - 1; i++) {
            if (sortedResults.get(i).getStatus() == ITestResult.FAILURE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Converts TestNG status to TestStatus enum.
     */
    private TestStatus convertStatus(int testNGStatus) {
        switch (testNGStatus) {
            case ITestResult.SUCCESS:
                return TestStatus.PASSED;
            case ITestResult.FAILURE:
                return TestStatus.FAILED;
            case ITestResult.SKIP:
                return TestStatus.SKIPPED;
            default:
                return TestStatus.FAILED;
        }
    }

    /**
     * Extracts error message from throwable.
     */
    private String extractErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return throwable.getMessage();
    }

    /**
     * Extracts stack trace from throwable.
     */
    private String extractStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        if (throwable.getCause() != null) {
            sb.append("Caused by: ").append(extractStackTrace(throwable.getCause()));
        }

        return sb.toString();
    }

    private Throwable getDisplayedThrowable(TestStatus status, Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        if (status == TestStatus.PASSED || status == TestStatus.FLAKY) {
            return null;
        }

        return throwable;
    }

    /**
     * Determines suite status based on test counts.
     */
    private TestStatus determineSuiteStatus(int passed, int failed, int skipped, int flaky, int total) {
        if (total == 0) {
            return TestStatus.SKIPPED;
        }
        if (failed > 0) {
            return TestStatus.FAILED;
        }
        if (flaky > 0) {
            return TestStatus.FLAKY;
        }
        if (passed > 0) {
            return TestStatus.PASSED;
        }
        return TestStatus.SKIPPED;
    }

    /**
     * Determines overall status from suites.
     */
    private TestStatus determineOverallStatus(List<TestSuite> suites) {
        if (suites.isEmpty()) {
            return TestStatus.SKIPPED;
        }

        boolean hasFailures = suites.stream().anyMatch(s -> s.getStatus() == TestStatus.FAILED);
        boolean hasFlaky = suites.stream().anyMatch(s -> s.getStatus() == TestStatus.FLAKY);
        boolean hasPassed = suites.stream().anyMatch(s -> s.getStatus() == TestStatus.PASSED);

        if (hasFailures) {
            return TestStatus.FAILED;
        }
        if (hasFlaky) {
            return TestStatus.FLAKY;
        }
        if (hasPassed) {
            return TestStatus.PASSED;
        }
        return TestStatus.SKIPPED;
    }
}
