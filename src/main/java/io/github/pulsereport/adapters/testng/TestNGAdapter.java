package io.github.pulsereport.adapters.testng;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import io.github.pulsereport.adapters.Adapter;
import io.github.pulsereport.adapters.restassured.RestAssuredAdapter;
import io.github.pulsereport.core.aggregator.TestResultAggregator;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStep;
import io.github.pulsereport.outputs.html.HtmlReportGenerator;
import io.github.pulsereport.outputs.json.JsonReportGenerator;

/**
 * TestNG adapter implementation that bridges TestNG events to the Adapter
 * interface.
 *
 * <p>
 * This reference implementation demonstrates how to integrate a test framework
 * with PulseReport. It implements both the framework-agnostic {@link Adapter}
 * interface and TestNG-specific listener interfaces ({@link ITestListener},
 * {@link ISuiteListener}).</p>
 *
 * <h2>Features</h2>
 * <ul>
 * <li>Maps TestNG lifecycle events to Adapter API</li>
 * <li>Supports adding artifacts (screenshots, logs) to tests</li>
 * <li>Supports adding metrics (response times, etc.) to tests</li>
 * <li>Thread-safe for parallel test execution</li>
 * <li>Integrates with {@link TestResultAggregator} for event aggregation</li>
 * <li>Handles parameterized tests and duplicate method names without
 * collision</li>
 * </ul>
 *
 * <h2>How It Works</h2>
 * <p>
 * TestNGAdapter serves as a <b>bridge between TestNG and PulseReport</b>:</p>
 * <ol>
 * <li>TestNG invokes {@link ITestListener} methods ({@code onTestStart(ITestResult)},
 *       {@code onTestSuccess(ITestResult)}, etc.) during test execution</li>
 * <li>These listener methods delegate to {@link TestResultAggregator} to record
 * results</li>
 * <li>Test code calls {@link #addArtifact(String, Artifact)} and
 * {@link #addMetric(String, Metric)} to attach data to the currently executing
 * test</li>
 * <li>After suite completion, {@link #getTestRun()} returns the aggregated
 * report with all test results, artifacts, and metrics</li>
 * </ol>
 *
 * <p>
 * <b>IMPORTANT:</b> This adapter MUST be registered as a TestNG listener to
 * produce a TestRun. The Adapter interface lifecycle methods
 * ({@code onTestSuccess(String)}, etc.) are hooks called by the ITestListener
 * methods - they do NOT record test results themselves. Only the ITestListener
 * path ({@code onTestSuccess(ITestResult)}) calls the aggregator to build the
 * TestRun.</p>
 *
 * <h2>Usage</h2>
 *
 * <h3>Step 1: Register as TestNG Listener</h3>
 * <p>
 * The adapter MUST be registered so TestNG can invoke the listener methods:</p>
 * <pre>{@code
 * // Option A: In testng.xml
 * <suite name="My Test Suite">
 *   <listeners>
 *     <listener class-name="io.github.pulsereport.adapters.testng.TestNGAdapter"/>
 *   </listeners>
 *   <test name="My Tests">
 *     <classes>
 *       <class name="com.example.MyTest"/>
 *     </classes>
 *   </test>
 * </suite>
 *
 * // Option B: Programmatically
 * TestNG testng = new TestNG();
 * TestNGAdapter adapter = new TestNGAdapter();
 * testng.addListener(adapter);
 * testng.setTestClasses(new Class[]{MyTest.class});
 * testng.run();
 *
 * // Option C: Using @Listeners annotation
 * @Listeners(TestNGAdapter.class)
 * public class MyTest {
 *     // tests...
 * }
 * }</pre>
 *
 * <h3>Step 2: Attach Artifacts/Metrics in Test Code</h3>
 * <p>
 * Test code can access the adapter to attach data during test execution:</p>
 * <pre>{@code
 * public class MyApiTest {
 *     // Get adapter instance (via static field, DI, TestNG ITestContext, etc.)
 *     private static TestNGAdapter adapter = getAdapterInstance();
 *
 *     @Test
 *     public void testLoginApi() {
 *         // Execute test logic
 *         Response response = api.login("user", "pass");
 *
 *         // Attach HTTP request artifact
 *         Artifact request = Artifact.builder()
 *             .name("login-request.json")
 *             .type("http-request")
 *             .content("{\"username\":\"user\"}")
 *             .mimeType("application/json")
 *             .timestamp(Instant.now())
 *             .build();
 *         adapter.addArtifact("testLoginApi", request);
 *
 *         // Record API response time
 *         Metric responseTime = Metric.builder()
 *             .name("api.response.time")
 *             .value(response.getTimeMs())
 *             .unit("ms")
 *             .timestamp(Instant.now())
 *             .build();
 *         adapter.addMetric("testLoginApi", responseTime);
 *
 *         // Test assertions...
 *     }
 * }
 * }</pre>
 *
 * <h3>Step 3: Retrieve TestRun After Execution</h3>
 * <pre>{@code
 * // After TestNG execution completes
 * TestRun testRun = adapter.getTestRun();
 *
 * // testRun contains:
 * // - All test results (pass/fail/skip)
 * // - All attached artifacts
 * // - All recorded metrics
 * // - Aggregate statistics
 * }</pre>
 *
 * <h2>Direct Adapter API (Not Recommended)</h2>
 * <p>
 * <b>WARNING:</b> Calling Adapter lifecycle methods directly does NOT produce a
 * TestRun:</p>
 * <pre>{@code
 * // This does NOT work - no TestRun is produced:
 * TestNGAdapter adapter = new TestNGAdapter();
 * adapter.onTestStart("testName");     // Only sets thread-local context
 * adapter.onTestSuccess("testName");   // Only logs - does NOT record result
 * TestRun run = adapter.getTestRun();  // Returns null - no results recorded!
 *
 * // Only the ITestListener methods record results via the aggregator.
 * // The String-based Adapter methods are hooks, not the recording mechanism.
 * }</pre>
 * <p>
 * The String-based lifecycle methods exist for:</p>
 * <ul>
 * <li>Custom logic hooks (logging, notifications, etc.)</li>
 * <li>Setting thread-local context for artifact/metric association</li>
 * <li>Framework adapter contract compliance</li>
 * </ul>
 * <p>
 * They do NOT replace proper TestNG listener integration.</p>
 *
 * <h2>Thread Safety and Artifact/Metric Association</h2>
 * <p>
 * This adapter uses thread-local storage to track the current test key for each
 * thread. This ensures that artifacts and metrics are correctly associated with
 * their tests even when:</p>
 * <ul>
 * <li>Parameterized tests have the same method name but different
 * parameters</li>
 * <li>Different classes have methods with the same name</li>
 * <li>Tests are retried</li>
 * <li>Tests run in parallel</li>
 * </ul>
 * <p>
 * The test key is set during {@code onTestStart(ITestResult)} (TestNG listener
 * path) or {@code onTestStart(String)} (Adapter API path) and cleared after
 * test completion to prevent memory leaks.</p>
 *
 * <p>
 * <b>TestNG Listener Path:</b> Uses unique keys generated from ITestResult,
 * handling parameterized tests correctly.</p>
 *
 * <p>
 * <b>Adapter API Path:</b> Uses simple test names as keys. Parameterized tests
 * with the same method name will have artifacts/metrics merged together.</p>
 *
 * @author PulseReport Team
 * @since 1.0.0
 * @see Adapter
 * @see TestResultAggregator
 */
public class TestNGAdapter implements Adapter, ITestListener, ISuiteListener {

    private static final Logger logger = LoggerFactory.getLogger(TestNGAdapter.class);

    /**
     * Aggregator for converting TestNG events to canonical data model.
     */
    private final TestResultAggregator aggregator;

    // Static so that every adapter instance (the TestNG-created listener AND any
    // instance test code creates, e.g. via a logger) shares the same store and
    // the same per-thread test context. Without this, TestNG's listener instance
    // would build a report missing data recorded through other instances.
    /**
     * Thread-safe map of artifacts by test name. Each test can have multiple
     * artifacts (screenshots, logs, etc.).
     */
    private static final Map<String, List<Artifact>> artifactsByTest = new ConcurrentHashMap<>();

    /**
     * Thread-safe map of metrics by test name. Each test can have multiple
     * metrics (response times, etc.).
     */
    private static final Map<String, List<Metric>> metricsByTest = new ConcurrentHashMap<>();

    /**
     * Thread-safe map of steps by test name. Each test can have multiple
     * steps recorded in execution order.
     */
    private static final Map<String, List<TestStep>> stepsByTest = new ConcurrentHashMap<>();

    /**
     * Thread-local storage for the current test key. Each thread tracks its own
     * test key to avoid collisions with parameterized tests, duplicate method
     * names across classes, and test retries.
     */
    private static final ThreadLocal<String> currentTestKey = new ThreadLocal<>();

    /**
     * Registry mapping bare method names to the full invocation key(s) seen for
     * them. Used to re-scope captures that happen outside the listener window
     * (e.g. in {@code @AfterMethod} after the thread-local key was cleared) so
     * they associate with the correct invocation instead of a globally ambiguous
     * bare-name bucket. Entries are removed when the owning suite finishes.
     */
    private static final Map<String, java.util.Set<String>> keysByMethodName = new ConcurrentHashMap<>();

    /**
     * Complete TestRun after suite finishes.
     */
    private TestRun testRun;

    /**
     * Constructs a new TestNGAdapter.
     */
    public TestNGAdapter() {
        this.aggregator = new TestResultAggregator();
        this.testRun = null;
        logger.info("TestNGAdapter initialized");
    }

    // These methods are HOOKS called by ITestListener methods.
    // They do NOT record test results - that happens in the ITestListener methods via aggregator.
    /**
     * Hook called when a suite starts.
     *
     * <p>
     * <b>Note:</b> This is a hook for custom logic (logging, etc.). Called by
     * {@link #onStart(ISuite)}. Does NOT record suite results.</p>
     */
    @Override
    public void onSuiteStart(String suiteName) {
        logger.info("Suite started: {}", suiteName);
    }

    /**
     * Hook called when a suite finishes.
     *
     * <p>
     * <b>Note:</b> This is a hook for custom logic (logging, etc.). Called by
     * {@link #onFinish(ISuite)}. Does NOT build the TestRun (that happens in
     * {@code onFinish(ISuite)} via the aggregator).</p>
     */
    @Override
    public void onSuiteFinish(String suiteName) {
        logger.info("Suite finished: {}", suiteName);
    }

    /**
     * Hook called when a test starts.
     *
     * <p>
     * Sets thread-local test key for artifact/metric association. Called by
     * {@link #onTestStart(ITestResult)} during normal TestNG execution.</p>
     *
     * <p>
     * <b>Note:</b> If called directly (not via TestNG listener), uses simple
     * testName as key which may cause collisions for parameterized tests.</p>
     */
    @Override
    public void onTestStart(String testName) {
        currentTestKey.set(testName);
        logger.debug("Test started (Adapter API): {}", testName);
    }

    /**
     * Hook called when a test passes.
     *
     * <p>
     * <b>Note:</b> This is a hook for custom logic (logging, notifications,
     * etc.). Called by {@link #onTestSuccess(ITestResult)}. Does NOT record
     * test result (that happens in {@code onTestSuccess(ITestResult)} via the
     * aggregator).</p>
     */
    @Override
    public void onTestSuccess(String testName) {
        logger.info("Test passed: {}", testName);
    }

    /**
     * Hook called when a test fails.
     *
     * <p>
     * <b>Note:</b> This is a hook for custom logic (logging, notifications,
     * etc.). Called by {@link #onTestFailure(ITestResult)}. Does NOT record
     * test result (that happens in {@code onTestFailure(ITestResult)} via the
     * aggregator).</p>
     */
    @Override
    public void onTestFailure(String testName, Throwable throwable) {
        logger.error("Test failed: {}", testName, throwable);
    }

    /**
     * Hook called when a test is skipped.
     *
     * <p>
     * <b>Note:</b> This is a hook for custom logic (logging, notifications,
     * etc.). Called by {@link #onTestSkipped(ITestResult)}. Does NOT record
     * test result (that happens in {@code onTestSkipped(ITestResult)} via the
     * aggregator).</p>
     */
    @Override
    public void onTestSkip(String testName) {
        logger.info("Test skipped: {}", testName);
    }

    /**
     * Adds an artifact to a test.
     *
     * <p>
     * <b>Test Code API:</b> Call this from your test code to attach
     * screenshots, logs, HTTP requests/responses, etc. to the currently
     * executing test.</p>
     *
     * <p>
     * Uses thread-local test key set by {@link #onTestStart(ITestResult)} for
     * correct association, even with parameterized tests. Falls back to
     * testName if called outside of TestNG listener context (may cause
     * collisions).</p>
     *
     * @param testName the name of the test
     * @param artifact the artifact to add
     * @throws IllegalArgumentException if artifact is null
     */
    @Override
    public void addArtifact(String testName, Artifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("Artifact cannot be null");
        }

        // Prefer ThreadLocal (set by TestNG listener or Adapter API)
        String testKey = currentTestKey.get();
        if (testKey == null) {
            // Fallback: resolve the scoped key from the method-name registry so
            // late captures (e.g. video in @AfterMethod) attach to the correct
            // invocation instead of a globally ambiguous bare-name bucket.
            testKey = resolveFallbackKey(testName);
        }

        artifactsByTest.computeIfAbsent(testKey, k -> new CopyOnWriteArrayList<>()).add(artifact);
        logger.debug("Added artifact '{}' to test '{}' (key: '{}')", artifact.getName(), testName, testKey);
    }

    /**
     * Adds a metric to a test.
     *
     * <p>
     * <b>Test Code API:</b> Call this from your test code to record performance
     * measurements like API response times, page load times, memory usage,
     * etc.</p>
     *
     * <p>
     * Uses thread-local test key set by {@link #onTestStart(ITestResult)} for
     * correct association, even with parameterized tests. Falls back to
     * testName if called outside of TestNG listener context (may cause
     * collisions).</p>
     *
     * @param testName the name of the test
     * @param metric the metric to add
     * @throws IllegalArgumentException if metric is null
     */
    @Override
    public void addMetric(String testName, Metric metric) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }

        String testKey = currentTestKey.get();
        if (testKey == null) {
            testKey = resolveFallbackKey(testName);
        }

        metricsByTest.computeIfAbsent(testKey, k -> new CopyOnWriteArrayList<>()).add(metric);
        logger.debug("Added metric '{}' to test '{}' (key: '{}')", metric.getName(), testName, testKey);
    }

    /**
     * Adds a step to a test.
     *
     * <p>
     * <b>Test Code API:</b> Call this from your test code to record a granular
     * sub-action (a UI interaction, an API call, a verification point, etc.)
     * within the currently executing test. Steps are rendered in the report in
     * insertion order, giving a structured breakdown of the test's behavior.
     * </p>
     *
     * <p>
     * Uses thread-local test key set by {@link #onTestStart(ITestResult)} for
     * correct association, even with parameterized tests. Falls back to
     * testName if called outside of TestNG listener context (may cause
     * collisions).</p>
     *
     * @param testName the name of the test
     * @param step the step to add
     * @throws IllegalArgumentException if step is null
     */
    @Override
    public void addStep(String testName, TestStep step) {
        if (step == null) {
            throw new IllegalArgumentException("Step cannot be null");
        }
        if (step.getStatus() == null) {
            throw new IllegalArgumentException(
                    "Step status cannot be null (the report renderer requires a status): " + step.getName());
        }

        String testKey = currentTestKey.get();
        if (testKey == null) {
            testKey = resolveFallbackKey(testName);
        }

        stepsByTest.computeIfAbsent(testKey, k -> new CopyOnWriteArrayList<>()).add(step);
        logger.debug("Added step '{}' to test '{}' (key: '{}')", step.getName(), testName, testKey);
    }

    @Override
    public TestRun getTestRun() {
        return testRun;
    }

    /**
     * Returns the thread-local test key for the currently executing test on
     * this thread, or null if no test is in progress. Exposed so subclasses
     * (e.g. AppiumAdapter) can offer name-free capture methods that resolve
     * the current test automatically.
     *
     * @return the current test key, or null
     */
    protected String getCurrentTestKey() {
        return currentTestKey.get();
    }

    /**
     * Called when a suite starts.
     *
     * @param suite the TestNG suite
     */
    @Override
    public void onStart(ISuite suite) {
        onSuiteStart(suite.getName());
    }

    /**
     * Called when a suite finishes. Aggregates all test results and builds the
     * final TestRun. Automatically generates HTML and JSON reports.
     *
     * @param suite the TestNG suite
     */
    @Override
    public void onFinish(ISuite suite) {
        onSuiteFinish(suite.getName());

        testRun = enrichTestRun(aggregator.buildTestRun(suite, artifactsByTest, metricsByTest, stepsByTest));
        logger.info("TestRun built for suite: {}", suite.getName());

        // The shared stores are static (shared across all adapter instances), so a
        // blanket clear() would delete artifacts/metrics/steps belonging to other
        // suites that are still running in parallel. Remove only this suite's entries.
        // Test keys are prefixed with "<suiteName>." (see TestResultAggregator.getTestKey).
        String suitePrefix = suite.getName() + ".";
        artifactsByTest.keySet().removeIf(k -> k.startsWith(suitePrefix));
        metricsByTest.keySet().removeIf(k -> k.startsWith(suitePrefix));
        stepsByTest.keySet().removeIf(k -> k.startsWith(suitePrefix));
        keysByMethodName.values().forEach(set -> set.removeIf(k -> k.startsWith(suitePrefix)));
        keysByMethodName.entrySet().removeIf(e -> e.getValue().isEmpty());
        logger.debug("Cleared artifact, metric, and step entries for suite: {}", suite.getName());

        generateReports();
    }

    /**
     * Registers the full invocation key under its bare method name so late
     * captures (outside the listener window) can be re-scoped to the correct
     * invocation.
     */
    private static void registerMethodKey(ITestResult result, String testKey) {
        String methodName = result.getMethod() != null ? result.getMethod().getMethodName() : null;
        if (methodName != null && testKey != null) {
            keysByMethodName.computeIfAbsent(methodName, k -> ConcurrentHashMap.newKeySet()).add(testKey);
        }
    }

    /**
     * Resolves the scoped fallback key for a capture that happened outside the
     * listener window (no thread-local). When the method name maps to exactly
     * one known invocation key, that key is used so the capture attaches to the
     * correct parameterized invocation. Otherwise the bare name is used as a
     * last resort and a warning is logged.
     */
    private static String resolveFallbackKey(String testName) {
        java.util.Set<String> keys = keysByMethodName.get(testName);
        if (keys != null && keys.size() == 1) {
            String scopedKey = keys.iterator().next();
            logger.debug("Resolved fallback key for '{}' to scoped invocation key '{}'", testName, scopedKey);
            return scopedKey;
        }
        logger.warn("Adding data without an active test context; using bare name '{}'. "
                + "May be ambiguous for parameterized tests.", testName);
        return testName;
    }

    /**
     * Allows subclasses to enrich the final TestRun before reports are
     * generated.
     *
     * @param builtTestRun the TestRun built from aggregated framework results
     * @return the TestRun that should be stored and reported
     */
    protected TestRun enrichTestRun(TestRun builtTestRun) {
        return builtTestRun;
    }

    /**
     * Generates HTML and JSON reports automatically. Reports are saved to the
     * PulseReport output directory.
     */
    private void generateReports() {
        if (testRun == null) {
            logger.warn("Cannot generate reports: TestRun is null");
            return;
        }

        try {
            String outputDir = System.getProperty("reporter.output.directory", "target/pulsereport");
            File reportDir = resolveOutputDirectory(outputDir);
            reportDir.mkdirs();

            File htmlReport = new File(reportDir, "test-report.html");
            HtmlReportGenerator htmlGenerator = new HtmlReportGenerator();
            htmlGenerator.generate(testRun, htmlReport);
            logger.info("""
                
                ========================================
                PulseReport: HTML report generated
                Location: {}
                Open with: open {}
                ========================================\
                """,
                    htmlReport.getAbsolutePath(),
                    htmlReport.getAbsolutePath());

            File jsonReport = new File(reportDir, "test-report.json");
            JsonReportGenerator jsonGenerator = new JsonReportGenerator();
            jsonGenerator.generate(testRun, jsonReport);
            logger.info("PulseReport: JSON report generated at {}", jsonReport.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Failed to generate reports", e);
        }
    }

    private File resolveOutputDirectory(String outputDir) {
        File configuredOutputDir = new File(outputDir);
        if (configuredOutputDir.isAbsolute()) {
            return configuredOutputDir;
        }

        String workingDirectory = System.getProperty("user.dir", ".");
        return new File(workingDirectory, outputDir);
    }

    /**
     * Called when a test context starts.
     *
     * @param context the test context
     */
    @Override
    public void onStart(ITestContext context) {
        logger.info("Test context started: {}", context.getName());
    }

    /**
     * Called when a test context finishes.
     *
     * @param context the test context
     */
    @Override
    public void onFinish(ITestContext context) {
        logger.info("Test context finished: {}", context.getName());
    }

    /**
     * Called when a test starts. Sets the thread-local test key for the current
     * thread to enable artifact/metric association without name collisions.
     *
     * @param result the test result
     */
    @Override
    public void onTestStart(ITestResult result) {
        String testKey = aggregator.getTestKey(result);
        currentTestKey.set(testKey);
        setRestAssuredTestContext(testKey);
        registerMethodKey(result, testKey);

        logger.info("Test started (TestNG listener): {}", result.getName());
    }

    /**
     * Called when a test succeeds. Records the test result and attaches any
     * artifacts/metrics. Cleans up the thread-local test key.
     *
     * @param result the test result
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        aggregator.recordTestResult(result);
        mergeRestAssuredDataForTest(aggregator.getTestKey(result));
        clearRestAssuredTestContext();
        logger.info("Test passed (TestNG listener): {}", result.getName());
        currentTestKey.remove();
    }

    /**
     * Called when a test fails. Records the test result and attaches any
     * artifacts/metrics. Cleans up the thread-local test key.
     *
     * @param result the test result
     */
    @Override
    public void onTestFailure(ITestResult result) {
        aggregator.recordTestResult(result);
        mergeRestAssuredDataForTest(aggregator.getTestKey(result));
        clearRestAssuredTestContext();
        logger.error("Test failed (TestNG listener): {}", result.getName(), result.getThrowable());
        currentTestKey.remove();
    }

    /**
     * Called when a test is skipped. Records the test result and attaches any
     * artifacts/metrics. Cleans up the thread-local test key.
     *
     * @param result the test result
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        aggregator.recordTestResult(result);
        mergeRestAssuredDataForTest(aggregator.getTestKey(result));
        clearRestAssuredTestContext();
        logger.info("Test skipped (TestNG listener): {}", result.getName());
        currentTestKey.remove();
    }

    /**
     * Called when a test fails but is within success percentage. Cleans up the
     * thread-local test key.
     *
     * @param result the test result
     */
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        aggregator.recordTestResult(result);
        mergeRestAssuredDataForTest(aggregator.getTestKey(result));
        clearRestAssuredTestContext();
        logger.info("Test failed but within success percentage: {}", result.getName());
        currentTestKey.remove();
    }

    private void setRestAssuredTestContext(String testKey) {
        try {
            RestAssuredAdapter.setCurrentTestName(testKey);
        } catch (NoClassDefFoundError ignored) {
        }
    }

    private void clearRestAssuredTestContext() {
        try {
            RestAssuredAdapter.clearCurrentTestName();
        } catch (NoClassDefFoundError ignored) {
        }
    }

    private void mergeRestAssuredDataForTest(String testKey) {
        try {
            List<Artifact> restArtifacts = RestAssuredAdapter.getArtifacts(testKey);
            if (!restArtifacts.isEmpty()) {
                artifactsByTest.computeIfAbsent(testKey, k -> new CopyOnWriteArrayList<>())
                        .addAll(restArtifacts);
            }
            List<Metric> restMetrics = RestAssuredAdapter.getMetrics(testKey);
            if (!restMetrics.isEmpty()) {
                metricsByTest.computeIfAbsent(testKey, k -> new CopyOnWriteArrayList<>())
                        .addAll(restMetrics);
            }
            RestAssuredAdapter.clearTestData(testKey);
        } catch (NoClassDefFoundError ignored) {
        }
    }
}
