package io.github.pulsereport.adapters;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStep;

/**
 * Interface defining the adapter contract for test framework integrations.
 *
 * <p>
 * Adapters bridge the gap between framework-specific test events and the custom
 * reporter's canonical data model. Each test framework (TestNG, JUnit, etc.)
 * should implement this interface to provide framework-specific mapping
 * logic.</p>
 *
 * <h2>Design Intent</h2>
 * <p>
 * The Adapter interface serves <b>TWO distinct purposes</b>:</p>
 * <ol>
 * <li><b>Framework Integration Layer:</b> Lifecycle methods ({@code onTestStart()},
 *       {@code onTestSuccess()}, etc.) are <b>hooks for framework adapters to
 * implement</b>, NOT methods for end users to call directly. These methods are
 * invoked BY the test framework (TestNG, JUnit, etc.) to notify the adapter of
 * test lifecycle events.</li>
 * <li><b>Test Code API:</b> Methods {@link #addArtifact(String, Artifact)} and
 * {@link #addMetric(String, Metric)} are the <b>public API for test code</b> to
 * attach screenshots, logs, performance metrics, etc. to tests during
 * execution.</li>
 * </ol>
 *
 * <p>
 * <b>IMPORTANT:</b> The Adapter is NOT a standalone test runner. Calling
 * lifecycle methods directly (e.g., {@code adapter.onTestSuccess("testName")})
 * does NOT execute tests or produce a TestRun. Adapters must be registered as
 * framework listeners to properly capture test results.</p>
 *
 * <h2>Adapter Mapping Guidelines</h2>
 * <ul>
 * <li><b>Framework Events → Core Lifecycle:</b> Map framework-specific events
 * (test start, pass, fail, skip) to the adapter lifecycle methods</li>
 * <li><b>Test Context Tracking:</b> Maintain thread-safe tracking of current
 * test context to support parallel execution</li>
 * <li><b>Artifacts &amp; Metrics:</b> Support adding screenshots, logs, HTTP
 * data, and performance metrics to the current test context</li>
 * <li><b>Deterministic Aggregation:</b> Ensure consistent ordering and
 * aggregation of test results for reproducible reports</li>
 * <li><b>Thread Safety:</b> Use concurrent collections for parallel test
 * execution</li>
 * </ul>
 *
 * <h2>Implementation Pattern</h2>
 * <pre>{@code
 * // Adapter must implement BOTH Adapter + framework listener interfaces
 * public class MyFrameworkAdapter implements Adapter, MyFrameworkListener {
 *     private final TestResultAggregator aggregator;
 *     private final Map<String, List<Artifact>> artifactsByTest;
 *     private final Map<String, List<Metric>> metricsByTest;
 *
 *     public MyFrameworkAdapter() {
 *         this.aggregator = new TestResultAggregator();
 *         this.artifactsByTest = new ConcurrentHashMap<>();
 *         this.metricsByTest = new ConcurrentHashMap<>();
 *     }
 *
 *     // Framework listener methods call aggregator to record results
 *     @Override
 *     public void frameworkOnTestSuccess(FrameworkTestResult result) {
 *         aggregator.recordTestResult(result); // Record result
 *         onTestSuccess(result.getTestName());  // Hook for custom logic
 *     }
 *
 *     // Adapter lifecycle hooks - called BY framework, not by users
 *     @Override
 *     public void onTestSuccess(String testName) {
 *         // Custom logic (logging, notifications, etc.)
 *     }
 *
 *     // Artifact/metric API - called BY test code
 *     @Override
 *     public void addArtifact(String testName, Artifact artifact) {
 *         artifactsByTest.computeIfAbsent(testName, k -> new ArrayList<>()).add(artifact);
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // 1. Register adapter as framework listener (in testng.xml, @RunWith, etc.)
 * //    Framework automatically calls onTestStart(), onTestSuccess(), etc.
 *
 * // 2. In test code - access adapter instance and attach data:
 * public class MyTest {
 *     private static TestNGAdapter adapter = new TestNGAdapter();
 *
 *     @Test
 *     public void testApiCall() {
 *         // Perform test logic...
 *         Response response = api.call();
 *
 *         // Attach screenshot artifact to THIS test
 *         Artifact screenshot = Artifact.builder()
 *             .name("api-response-screenshot.png")
 *             .type("screenshot")
 *             .path("/screenshots/api-response.png")
 *             .mimeType("image/png")
 *             .size(12345L)
 *             .timestamp(Instant.now())
 *             .build();
 *         adapter.addArtifact("testApiCall", screenshot);
 *
 *         // Record API response time metric
 *         Metric responseTime = Metric.builder()
 *             .name("api.response.time")
 *             .value(250.5)
 *             .unit("ms")
 *             .timestamp(Instant.now())
 *             .build();
 *         adapter.addMetric("testApiCall", responseTime);
 *     }
 * }
 *
 * // 3. After suite completion, retrieve report:
 * TestRun testRun = adapter.getTestRun();
 * }</pre>
 *
 * @author Pulse Report Team
 * @since 1.0.0
 * @see io.github.pulsereport.core.aggregator.TestResultAggregator
 * @see io.github.pulsereport.core.model.TestRun
 */
public interface Adapter {

    /**
     * Called when a test suite starts execution.
     *
     * <p>
     * <b>Framework Hook:</b> This method is invoked BY the test framework
     * (TestNG, JUnit, etc.) when a suite begins, NOT called directly by test
     * code. Implementations should initialize suite-level state and tracking.
     * This method may be called multiple times for different suites in the same
     * test run.</p>
     *
     * @param suiteName the name of the test suite starting
     */
    void onSuiteStart(String suiteName);

    /**
     * Called when a test suite finishes execution.
     *
     * <p>
     * <b>Framework Hook:</b> This method is invoked BY the test framework
     * (TestNG, JUnit, etc.) when a suite completes, NOT called directly by test
     * code. Implementations should finalize suite-level aggregation and
     * cleanup. This method may be called multiple times for different suites in
     * the same test run.</p>
     *
     * @param suiteName the name of the test suite finishing
     */
    void onSuiteFinish(String suiteName);

    /**
     * Called when an individual test starts execution.
     *
     * <p>
     * <b>Framework Hook:</b> This method is invoked BY the test framework
     * (TestNG, JUnit, etc.) when a test begins, NOT called directly by test
     * code. Implementations should track the current test context for adding
     * artifacts and metrics. Must be thread-safe for parallel execution.</p>
     *
     * @param testName the name of the test starting
     */
    void onTestStart(String testName);

    /**
     * Called when an individual test passes successfully.
     *
     * <p>
     * <b>Framework Hook:</b> This method is invoked BY the test framework
     * (TestNG, JUnit, etc.) when a test passes, NOT called directly by test
     * code. Implementations should record the test result and attach any
     * collected artifacts/metrics to the test case.</p>
     *
     * <p>
     * <b>Note:</b> In many implementations, actual result recording happens in
     * the framework-specific listener method (e.g.,
     * {@code onTestSuccess(ITestResult)}), while this method serves as a hook
     * for custom logic like logging or notifications.</p>
     *
     * @param testName the name of the test that passed
     */
    void onTestSuccess(String testName);

    /**
     * Called when an individual test fails.
     *
     * <p>
     * <b>Framework Hook:</b> This method is invoked BY the test framework
     * (TestNG, JUnit, etc.) when a test fails, NOT called directly by test
     * code. Implementations should record the test result, including error
     * information, and attach any collected artifacts/metrics to the test
     * case.</p>
     *
     * <p>
     * <b>Note:</b> In many implementations, actual result recording happens in
     * the framework-specific listener method (e.g.,
     * {@code onTestFailure(ITestResult)}), while this method serves as a hook
     * for custom logic like logging or notifications.</p>
     *
     * @param testName the name of the test that failed
     * @param throwable the exception that caused the failure (may be null)
     */
    void onTestFailure(String testName, Throwable throwable);

    /**
     * Called when an individual test is skipped.
     *
     * <p>
     * <b>Framework Hook:</b> This method is invoked BY the test framework
     * (TestNG, JUnit, etc.) when a test is skipped, NOT called directly by test
     * code. Implementations should record the test as skipped. Artifacts and
     * metrics may still be attached to skipped tests.</p>
     *
     * <p>
     * <b>Note:</b> In many implementations, actual result recording happens in
     * the framework-specific listener method (e.g.,
     * {@code onTestSkipped(ITestResult)}), while this method serves as a hook
     * for custom logic like logging or notifications.</p>
     *
     * @param testName the name of the test that was skipped
     */
    void onTestSkip(String testName);

    /**
     * Adds an artifact (attachment) to the current test context.
     *
     * <p>
     * <b>Test Code API:</b> This method is called BY test code during test
     * execution to attach screenshots, logs, videos, HTTP data, etc. to the
     * currently running test.</p>
     *
     * <p>
     * Artifacts include screenshots, logs, videos, HTTP request/response data,
     * and other files generated during test execution. The artifact will be
     * associated with the specified test and included in the final TestRun
     * report.</p>
     *
     * <p>
     * Thread-safe: can be called from different threads for different tests
     * during parallel execution.</p>
     *
     * <p>
     * <b>Common Artifact Types:</b></p>
     * <ul>
     * <li><b>screenshot</b> - Browser screenshots (PNG, JPEG)</li>
     * <li><b>log</b> - Text log files</li>
     * <li><b>video</b> - Screen recordings (MP4, WebM)</li>
     * <li><b>http-request</b> - HTTP request data (JSON, XML)</li>
     * <li><b>http-response</b> - HTTP response data (JSON, XML)</li>
     * <li><b>trace</b> - Execution traces (HAR, performance profiles)</li>
     * </ul>
     *
     * @param testName the name of the test to attach the artifact to
     * @param artifact the artifact to add (must not be null)
     * @throws IllegalArgumentException if artifact is null
     */
    void addArtifact(String testName, Artifact artifact);

    /**
     * Adds a metric to the current test context.
     *
     * <p>
     * <b>Test Code API:</b> This method is called BY test code during test
     * execution to record performance measurements like API response times,
     * page load times, etc.</p>
     *
     * <p>
     * Metrics capture quantitative measurements during test execution such as
     * API response times, page load times, network latency, memory usage, etc.
     * The metric will be associated with the specified test and included in the
     * final TestRun report.</p>
     *
     * <p>
     * Thread-safe: can be called from different threads for different tests
     * during parallel execution.</p>
     *
     * <p>
     * <b>Common Metric Examples:</b></p>
     * <ul>
     * <li><b>api.response.time</b> - API endpoint response time (ms)</li>
     * <li><b>page.load.time</b> - Browser page load time (ms)</li>
     * <li><b>network.latency</b> - Network round-trip time (ms)</li>
     * <li><b>memory.used</b> - Memory consumption (MB)</li>
     * <li><b>db.query.time</b> - Database query execution time (ms)</li>
     * </ul>
     *
     * @param testName the name of the test to attach the metric to
     * @param metric the metric to add (must not be null)
     * @throws IllegalArgumentException if metric is null
     */
    void addMetric(String testName, Metric metric);

    /**
     * Adds a step to the current test context.
     *
     * <p>
     * <b>Test Code API:</b> This method is called BY test code during test
     * execution to record a granular sub-action (a UI interaction, an API call,
     * a verification point, etc.) within the currently running test.</p>
     *
     * <p>
     * Steps provide a structured, ordered breakdown of what a test did,
     * complementing free-form artifacts and metrics. This is the non-BDD
     * counterpart to the steps that the Cucumber adapter captures
     * automatically: TestNG-, Selenium-, and Appium-based tests can use this
     * API to record meaningful steps such as "launch app", "tap login button",
     * or "verify dashboard is displayed".</p>
     *
     * <p>
     * Steps are attached to the specified test and included in the final
     * TestRun report in insertion order.</p>
     *
     * <p>
     * Thread-safe: can be called from different threads for different tests
     * during parallel execution.</p>
     *
     * @param testName the name of the test to attach the step to
     * @param step the step to add (must not be null)
     * @throws IllegalArgumentException if step is null
     */
    void addStep(String testName, TestStep step);

    /**
     * Returns the complete TestRun after all suites have finished.
     *
     * <p>
     * This method should be called after all test execution is complete to
     * retrieve the aggregated test results. Returns null if test execution is
     * still in progress or no tests have been run.</p>
     *
     * <p>
     * The returned TestRun includes:</p>
     * <ul>
     * <li>All test suites with their test cases</li>
     * <li>Test steps (if applicable)</li>
     * <li>All attached artifacts (screenshots, logs, etc.)</li>
     * <li>All recorded metrics (response times, etc.)</li>
     * <li>Aggregate statistics (pass/fail counts, duration, etc.)</li>
     * <li>Retry and flaky test information</li>
     * </ul>
     *
     * @return the complete TestRun, or null if not available
     */
    TestRun getTestRun();
}
