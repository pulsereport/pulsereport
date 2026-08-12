package io.github.pulsereport.core.aggregator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;
import io.github.pulsereport.core.model.TestSuite;

/**
 * Aggregates TestNG test results into the canonical data model. Handles
 * conversion from TestNG's ITestResult, ITestContext, and ISuite to our
 * TestCase, TestSuite, and TestRun models.
 *
 * Thread-safe for parallel execution. Handles retry logic and flaky test
 * detection.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class TestResultAggregator {

    private static final Logger logger = LoggerFactory.getLogger(TestResultAggregator.class);

    /**
     * Thread-safe map to track test results by test key (className.methodName).
     * Stores all attempts for each test to handle retries.
     */
    private final Map<String, List<ITestResult>> testResultsByKey;

    /**
     * Constructs a new TestResultAggregator.
     */
    public TestResultAggregator() {
        this.testResultsByKey = new ConcurrentHashMap<>();
        logger.debug("TestResultAggregator initialized");
    }

    /**
     * Records a test result. Handles retries by storing multiple results for
     * the same test.
     *
     * @param result the TestNG test result
     */
    public synchronized void recordTestResult(ITestResult result) {
        if (result == null) {
            logger.warn("Received null test result, ignoring");
            return;
        }

        String testKey = getTestKey(result);
        // Use synchronized list for thread-safe concurrent reads/writes
        testResultsByKey.computeIfAbsent(testKey, k -> Collections.synchronizedList(new ArrayList<>())).add(result);
        logger.debug("Recorded test result for: {}", testKey);
    }

    /**
     * Converts a TestNG ITestResult to our TestCase model.
     *
     * @param result the TestNG test result
     * @return the converted TestCase
     */
    public TestCase convertToTestCase(ITestResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ITestResult cannot be null");
        }

        String className = result.getMethod().getRealClass().getName();
        String methodName = result.getMethod().getMethodName();

        Instant startTime = Instant.ofEpochMilli(result.getStartMillis());
        Instant endTime = Instant.ofEpochMilli(result.getEndMillis());
        long duration = result.getEndMillis() - result.getStartMillis();

        TestStatus status = convertStatus(result.getStatus());
        Throwable detailThrowable = getDisplayedThrowable(status, result.getThrowable());
        String errorMessage = extractErrorMessage(detailThrowable);
        String stackTrace = extractStackTrace(detailThrowable);

        return TestCase.builder()
                .id(UUID.randomUUID().toString())
                .name(getTestDisplayName(result))
                .className(className)
                .methodName(methodName)
                .startTime(startTime)
                .endTime(endTime)
                .duration(duration)
                .status(status)
                .errorMessage(errorMessage)
                .stackTrace(stackTrace)
                .retryCount(0)
                .build();
    }

    /**
     * Gets a TestCase by test key, handling retries and flaky detection.
     *
     * @param testKey the test key (className.methodName)
     * @return the aggregated TestCase with retry information
     */
    public TestCase getTestCase(String testKey) {
        List<ITestResult> results = testResultsByKey.get(testKey);
        if (results == null || results.isEmpty()) {
            return null;
        }

        List<ITestResult> sortedResults;
        synchronized (results) {
            sortedResults = new ArrayList<>(results);
        }
        sortedResults.sort(Comparator.comparing(ITestResult::getStartMillis));

        ITestResult finalResult = sortedResults.get(sortedResults.size() - 1);

        // Detect flaky tests: if earlier attempts failed but final succeeded
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
                .id(testKey) // Use test key as ID for stable identification
                .name(getTestDisplayName(finalResult))
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
     * Gets all test cases in deterministic order.
     *
     * @return list of all test cases sorted by name, then class, then method
     */
    public List<TestCase> getAllTestCases() {
        List<TestCase> allTests = new ArrayList<>();

        for (String testKey : testResultsByKey.keySet()) {
            TestCase testCase = getTestCase(testKey);
            if (testCase != null) {
                allTests.add(testCase);
            }
        }

        // tie-breakers
        allTests.sort(Comparator.comparing(TestCase::getName)
                .thenComparing(TestCase::getClassName)
                .thenComparing(TestCase::getMethodName));

        return allTests;
    }

    /**
     * Converts a TestNG ITestContext and its results to our TestSuite model.
     *
     * @param context the TestNG test context
     * @param results the test results for this context
     * @return the converted TestSuite
     */
    public TestSuite convertToTestSuite(ITestContext context, List<ITestResult> results) {
        if (context == null) {
            throw new IllegalArgumentException("ITestContext cannot be null");
        }

        String suiteName = context.getName();
        Instant startTime = Instant.ofEpochMilli(context.getStartDate().getTime());
        Instant endTime = Instant.ofEpochMilli(context.getEndDate().getTime());
        long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

        List<TestCase> testCases = new ArrayList<>();
        Map<String, Boolean> processedTests = new HashMap<>();

        if (results != null) {
            for (ITestResult result : results) {
                String testKey = getTestKey(result);
                if (!processedTests.containsKey(testKey)) {
                    TestCase testCase = getTestCase(testKey);
                    if (testCase != null) {
                        testCases.add(testCase);
                        processedTests.put(testKey, true);
                    }
                }
            }
        }

        testCases.sort(Comparator.comparing(TestCase::getStartTime)
                .thenComparing(TestCase::getClassName)
                .thenComparing(TestCase::getMethodName));

        int totalTests = testCases.size();
        int passedTests = (int) testCases.stream().filter(tc -> tc.getStatus() == TestStatus.PASSED).count();
        int failedTests = (int) testCases.stream().filter(tc -> tc.getStatus() == TestStatus.FAILED).count();
        int skippedTests = (int) testCases.stream().filter(tc -> tc.getStatus() == TestStatus.SKIPPED).count();
        int flakyTests = (int) testCases.stream().filter(tc -> tc.getStatus() == TestStatus.FLAKY).count();

        TestStatus suiteStatus = calculateSuiteStatus(passedTests, failedTests, skippedTests, flakyTests, totalTests);

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
     * Converts a TestNG ISuite to our TestRun model.
     *
     * @param suite the TestNG suite
     * @return the converted TestRun
     */
    public TestRun convertToTestRun(ISuite suite) {
        if (suite == null) {
            throw new IllegalArgumentException("ISuite cannot be null");
        }

        String runName = suite.getName();
        List<TestSuite> testSuites = new ArrayList<>();

        Map<String, ISuiteResult> suiteResults = suite.getResults();
        if (suiteResults != null) {
            for (Map.Entry<String, ISuiteResult> entry : suiteResults.entrySet()) {
                ITestContext context = entry.getValue().getTestContext();

                List<ITestResult> allResults = new ArrayList<>();
                allResults.addAll(context.getPassedTests().getAllResults());
                allResults.addAll(context.getFailedTests().getAllResults());
                allResults.addAll(context.getSkippedTests().getAllResults());

                TestSuite testSuite = convertToTestSuite(context, allResults);
                testSuites.add(testSuite);
            }
        }

        testSuites.sort(Comparator.comparing(TestSuite::getStartTime)
                .thenComparing(TestSuite::getName));

        int totalTests = testSuites.stream().mapToInt(TestSuite::getTotalTests).sum();
        int passedTests = testSuites.stream().mapToInt(TestSuite::getPassedTests).sum();
        int failedTests = testSuites.stream().mapToInt(TestSuite::getFailedTests).sum();
        int skippedTests = testSuites.stream().mapToInt(TestSuite::getSkippedTests).sum();

        TestStatus overallStatus = calculateOverallStatus(testSuites);

        Instant startTime;
        Instant endTime;

        if (!testSuites.isEmpty()) {
            startTime = testSuites.stream()
                    .map(TestSuite::getStartTime)
                    .min(Instant::compareTo)
                    .orElseGet(() -> Instant.ofEpochMilli(suite.getAttribute("startTime") != null
                    ? (Long) suite.getAttribute("startTime")
                    : 0));

            endTime = testSuites.stream()
                    .map(TestSuite::getEndTime)
                    .max(Instant::compareTo)
                    .orElseGet(() -> Instant.ofEpochMilli(suite.getAttribute("endTime") != null
                    ? (Long) suite.getAttribute("endTime")
                    : startTime.toEpochMilli()));
        } else {
            // Empty suite: use epoch 0 for determinism
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
     * Generates a unique key for a test. Includes suite name, context name,
     * className, methodName, parameters, and test name for uniqueness. This
     * ensures parameterized tests and tests in different contexts/suites are
     * tracked separately.
     *
     * @param result the test result to generate a key for
     * @return unique test key string
     */
    public String getTestKey(ITestResult result) {
        ITestNGMethod method = result.getMethod();
        if (method == null || method.getRealClass() == null) {
            // Fallback for incomplete mocks or edge cases
            return result.getTestName() != null ? result.getTestName() : "unknown";
        }

        String className = method.getRealClass().getName();
        String methodName = method.getMethodName();

        String suiteAndContext = "";
        try {
            ITestContext context = result.getTestContext();
            if (context != null) {
                ISuite suite = context.getSuite();
                String suiteName = suite != null ? suite.getName() + "." : "";
                String contextName = context.getName();
                suiteAndContext = suiteName + contextName + ".";
            }
        } catch (Exception e) {
            // Fallback if context/suite not available
            logger.debug("Could not retrieve test context/suite for key generation: {}", e.getMessage());
        }

        Object[] parameters = result.getParameters();
        String paramKey = (parameters != null && parameters.length > 0)
                ? "_" + Arrays.hashCode(parameters)
                : "";

        String testName = result.getTestName() != null ? result.getTestName() : methodName;

        return suiteAndContext + className + "." + methodName + paramKey + "_" + testName;
    }

    /**
     * Returns the display name for a test: uses the @Test description when
     * non-empty, otherwise falls back to the method name.
     */
    private String getTestDisplayName(ITestResult result) {
        String description = result.getMethod().getDescription();
        return (description != null && !description.trim().isEmpty()) ? description.trim() : result.getName();
    }

    /**
     * Converts TestNG status code to our TestStatus enum.
     */
    private TestStatus convertStatus(int testNGStatus) {
        switch (testNGStatus) {
            case ITestResult.SUCCESS:
                return TestStatus.PASSED;
            case ITestResult.FAILURE:
                return TestStatus.FAILED;
            case ITestResult.SKIP:
                return TestStatus.SKIPPED;
            case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
                return TestStatus.FLAKY;
            default:
                logger.warn("Unknown TestNG status: {}, defaulting to FAILED", testNGStatus);
                return TestStatus.FAILED;
        }
    }

    /**
     * Detects if a test is flaky based on retry history. A test is flaky if it
     * failed at least once but ultimately passed.
     */
    private boolean detectFlaky(List<ITestResult> sortedResults) {
        if (sortedResults.size() <= 1) {
            return false;
        }

        ITestResult finalResult = sortedResults.get(sortedResults.size() - 1);
        if (finalResult.getStatus() != ITestResult.SUCCESS) {
            return false;
        }

        for (int i = 0; i < sortedResults.size() - 1; i++) {
            if (sortedResults.get(i).getStatus() == ITestResult.FAILURE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts error message from a throwable.
     */
    private String extractErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        return throwable.getMessage();
    }

    /**
     * Extracts stack trace from a throwable.
     */
    private String extractStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
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
     * Calculates the overall status of a test suite based on its test results.
     */
    private TestStatus calculateSuiteStatus(int passed, int failed, int skipped, int flaky, int total) {
        if (total == 0) {
            return TestStatus.PASSED;
        }

        if (failed > 0) {
            return TestStatus.FAILED;
        }

        if (flaky > 0) {
            return TestStatus.FLAKY;
        }

        if (skipped == total) {
            return TestStatus.SKIPPED;
        }

        return TestStatus.PASSED;
    }

    /**
     * Calculates the overall status of a test run based on its suites.
     */
    private TestStatus calculateOverallStatus(List<TestSuite> suites) {
        if (suites.isEmpty()) {
            return TestStatus.PASSED;
        }

        boolean hasFailure = suites.stream().anyMatch(s -> s.getStatus() == TestStatus.FAILED);
        if (hasFailure) {
            return TestStatus.FAILED;
        }

        boolean hasFlaky = suites.stream().anyMatch(s -> s.getStatus() == TestStatus.FLAKY);
        if (hasFlaky) {
            return TestStatus.FLAKY;
        }

        boolean allSkipped = suites.stream().allMatch(s -> s.getStatus() == TestStatus.SKIPPED);
        if (allSkipped) {
            return TestStatus.SKIPPED;
        }

        return TestStatus.PASSED;
    }

    /**
     * Builds a complete TestRun with artifacts and metrics attached to test
     * cases.
     *
     * <p>
     * This method extends {@link #convertToTestRun(ISuite)} by enriching test
     * cases with artifacts and metrics collected during test execution.
     * Artifacts include screenshots, logs, HTTP data, etc. Metrics include
     * response times, page load times, etc.
     * </p>
     *
     * @param suite the TestNG suite
     * @param artifactsByTest map of test names to their artifacts
     * @param metricsByTest map of test names to their metrics
     * @return the complete TestRun with enriched test cases
     * @throws IllegalArgumentException if suite is null
     */
    public TestRun buildTestRun(ISuite suite,
            Map<String, List<Artifact>> artifactsByTest,
            Map<String, List<Metric>> metricsByTest) {
        return buildTestRun(suite, artifactsByTest, metricsByTest, Collections.emptyMap());
    }

    /**
     * Builds a complete TestRun with artifacts, metrics, and steps attached to
     * test cases.
     *
     * <p>
     * This method extends {@link #convertToTestRun(ISuite)} by enriching test
     * cases with artifacts, metrics, and steps collected during test
     * execution. Artifacts include screenshots, logs, HTTP data, etc. Metrics
     * include response times, page load times, etc. Steps provide an ordered,
     * granular breakdown of sub-actions within a test (useful for TestNG-,
     * Selenium-, and Appium-based tests; BDD steps captured by the Cucumber
     * adapter are preserved as-is).
     * </p>
     *
     * @param suite the TestNG suite
     * @param artifactsByTest map of test names to their artifacts
     * @param metricsByTest map of test names to their metrics
     * @param stepsByTest map of test names to their recorded steps
     * @return the complete TestRun with enriched test cases
     * @throws IllegalArgumentException if suite is null
     */
    public TestRun buildTestRun(ISuite suite,
            Map<String, List<Artifact>> artifactsByTest,
            Map<String, List<Metric>> metricsByTest,
            Map<String, List<TestStep>> stepsByTest) {
        TestRun baseTestRun = convertToTestRun(suite);

        if (baseTestRun == null) {
            return null;
        }

        List<TestSuite> enrichedSuites = new ArrayList<>();
        for (TestSuite suite1 : baseTestRun.getSuites()) {
            List<TestCase> enrichedTestCases = new ArrayList<>();

            for (TestCase testCase : suite1.getTestCases()) {
                enrichedTestCases.add(enrichTestCase(testCase, artifactsByTest, metricsByTest, stepsByTest));
            }

            TestSuite enrichedSuite = TestSuite.builder()
                    .id(suite1.getId())
                    .name(suite1.getName())
                    .startTime(suite1.getStartTime())
                    .endTime(suite1.getEndTime())
                    .duration(suite1.getDuration())
                    .status(suite1.getStatus())
                    .testCases(enrichedTestCases)
                    .totalTests(suite1.getTotalTests())
                    .passedTests(suite1.getPassedTests())
                    .failedTests(suite1.getFailedTests())
                    .skippedTests(suite1.getSkippedTests())
                    .build();

            enrichedSuites.add(enrichedSuite);
        }

        return TestRun.builder()
                .id(baseTestRun.getId())
                .name(baseTestRun.getName())
                .startTime(baseTestRun.getStartTime())
                .endTime(baseTestRun.getEndTime())
                .duration(baseTestRun.getDuration())
                .status(baseTestRun.getStatus())
                .suites(enrichedSuites)
                .totalTests(baseTestRun.getTotalTests())
                .passedTests(baseTestRun.getPassedTests())
                .failedTests(baseTestRun.getFailedTests())
                .skippedTests(baseTestRun.getSkippedTests())
                .build();
    }

    /**
     * Enriches a single test case with its artifacts, metrics, and steps.
     *
     * <p>
     * Falls back to the test method name when no data is stored under the full
     * test key (e.g. data recorded in {@code @AfterMethod} running after
     * {@code onTestSuccess} in TestNG 7.x+). Steps already captured by a
     * framework adapter (e.g. Cucumber BDD steps) are preserved when no
     * additional steps were recorded via the step API.
     * </p>
     */
    private TestCase enrichTestCase(TestCase testCase,
            Map<String, List<Artifact>> artifactsByTest,
            Map<String, List<Metric>> metricsByTest,
            Map<String, List<TestStep>> stepsByTest) {
        String testKey = testCase.getId();

        // Merge data stored under the full test key AND under the bare method name.
        // Some captures land under the full key (thread-local set during the listener
        // callback, e.g. failure screenshots) while others land under the method name
        // (recorded later in @AfterMethod after the thread-local is cleared, e.g. video).
        // Merging — rather than fallback-only-when-empty — ensures both are included.
        List<Artifact> artifacts = new ArrayList<>(artifactsByTest.getOrDefault(testKey, new ArrayList<>()));
        artifacts.addAll(artifactsByTest.getOrDefault(testCase.getMethodName(), new ArrayList<>()));

        List<Metric> metrics = new ArrayList<>(metricsByTest.getOrDefault(testKey, new ArrayList<>()));
        metrics.addAll(metricsByTest.getOrDefault(testCase.getMethodName(), new ArrayList<>()));

        List<TestStep> steps = new ArrayList<>(stepsByTest.getOrDefault(testKey, new ArrayList<>()));
        steps.addAll(stepsByTest.getOrDefault(testCase.getMethodName(), new ArrayList<>()));
        // Preserve steps already captured by framework adapters (e.g. Cucumber BDD steps).
        if (steps.isEmpty()) {
            steps = testCase.getSteps();
        }

        return TestCase.builder()
                .id(testCase.getId())
                .name(testCase.getName())
                .className(testCase.getClassName())
                .methodName(testCase.getMethodName())
                .startTime(testCase.getStartTime())
                .endTime(testCase.getEndTime())
                .duration(testCase.getDuration())
                .status(testCase.getStatus())
                .errorMessage(testCase.getErrorMessage())
                .stackTrace(testCase.getStackTrace())
                .steps(steps)
                .artifacts(artifacts)
                .metrics(metrics)
                .retryCount(testCase.getRetryCount())
                .build();
    }
}
