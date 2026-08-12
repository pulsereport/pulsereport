package io.github.pulsereport.adapters.appium;

import io.github.pulsereport.adapters.testng.TestNGAdapter;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Appium adapter for mobile test automation.
 *
 * <p>This adapter extends {@link TestNGAdapter} to provide Appium-specific
 * functionality for capturing mobile screenshots, app logs, device information,
 * mobile performance metrics, granular steps, failure artifacts, and session
 * metadata.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Captures mobile screenshots during test execution</li>
 *   <li>Captures app logs and crash reports</li>
 *   <li>Captures device information (OS, model, screen resolution)</li>
 *   <li>Records mobile performance metrics (app launch time, screen transitions)</li>
 *   <li>Records granular mobile steps (taps, swipes, verifications) via {@link #recordStep}</li>
 *   <li>Automatic failure capture: screenshot + page source when a test fails,
 *       if the driver is registered in {@link MobileDriverHolder}</li>
 *   <li>Screen recording (video) helpers via {@link #captureVideo}</li>
 *   <li>Crash / ANR detection helpers via {@link #captureCrashReport}</li>
 *   <li>Structured session metadata surfaced in {@code TestRun.environment}
 *       via {@link #recordSessionMetadata}</li>
 *   <li>Device health metrics (battery, memory) via {@link #recordDeviceHealth}</li>
 *   <li>Thread-safe for parallel mobile test execution</li>
 *   <li>Integrates seamlessly with TestNG</li>
 * </ul>
 *
 * <h2>Automatic failure capture</h2>
 * <p>Register the driver per test thread and this adapter will automatically
 * attach a failure screenshot and page-source snapshot when a test fails:</p>
 * <pre>{@code
 * @BeforeMethod
 * public void setUp() {
 *     driver = new AndroidDriver(new URL("http://localhost:4723"), caps);
 *     MobileDriverHolder.set(driver);
 * }
 *
 * @AfterMethod(alwaysRun = true)
 * public void tearDown() {
 *     MobileDriverHolder.remove();
 *     driver.quit();
 * }
 * }</pre>
 *
 * <h2>Manual capture</h2>
 * <p>All capture methods remain available for explicit, on-demand use. The
 * adapter doesn't require the driver to be registered — you can still extract
 * data yourself and pass it to the capture/record methods directly.</p>
 *
 * @author Pulse Report Team
 * @since 1.0.0
 * @see TestNGAdapter
 * @see MobileDriverHolder
 */
public class AppiumAdapter extends TestNGAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AppiumAdapter.class);

    /**
     * Run-level mobile session metadata, merged into TestRun.environment.
     * Static so test code (which may hold a different adapter instance than the
     * TestNG-created listener) records into the same store the listener reads
     * when building the report.
     *
     * <p>Keyed by suite name so concurrent suites do not overwrite each other.
     * Test code records into the bucket for the suite whose context is active on
     * the calling thread; the listener merges the bucket for the suite it is
     * finishing.</p>
     */
    private static final Map<String, Map<String, String>> sessionMetadataBySuite = new ConcurrentHashMap<>();

    /**
     * Tracks the active suite name per thread so name-free metadata recording
     * attributes entries to the correct suite during parallel execution.
     */
    private static final ThreadLocal<String> currentSuiteName = new ThreadLocal<>();

    /**
     * Constructs a new AppiumAdapter.
     */
    public AppiumAdapter() {
        super();
        logger.info("AppiumAdapter initialized");
    }

    // ------------------------------------------------------------------
    // Steps
    // ------------------------------------------------------------------

    /**
     * Records a completed mobile step (e.g. "tap login button") with a
     * measured duration.
     *
     * <p>This is a convenience wrapper around {@link #addStep(String, TestStep)}
     * that builds a {@link TestStep} with status, timing, and an optional
     * description. Use it to log granular mobile interactions so the report
     * shows an ordered breakdown of what the test did.</p>
     *
     * @param testName the name of the test
     * @param stepName the step description (e.g. "tap login button")
     * @param durationMs how long the step took, in milliseconds
     * @throws IllegalArgumentException if testName or stepName is null/empty
     */
    public void recordStep(String testName, String stepName, long durationMs) {
        recordStep(testName, stepName, TestStatus.PASSED, durationMs, null);
    }

    /**
     * Records a completed mobile step with an explicit status and description.
     *
     * @param testName the name of the test
     * @param stepName the step description
     * @param status the step outcome
     * @param durationMs how long the step took, in milliseconds
     * @param description optional extra detail (may be null)
     * @throws IllegalArgumentException if testName or stepName is null/empty
     */
    public void recordStep(String testName, String stepName, TestStatus status,
                           long durationMs, String description) {
        validateParameter(testName, "testName");
        validateParameter(stepName, "stepName");

        Instant end = Instant.now();
        Instant start = end.minusMillis(Math.max(durationMs, 0));

        TestStep step = TestStep.builder()
                .name(stepName)
                .status(status)
                .startTime(start)
                .endTime(end)
                .duration(Math.max(durationMs, 0))
                .description(description)
                .build();

        addStep(testName, step);
        logger.debug("Recorded step '{}' ({}) for test '{}'", stepName, status, testName);
    }

    /**
     * Records a step against the test currently executing on this thread,
     * without needing the test name.
     *
     * <p>This is the convenient entry point for framework utilities (e.g. a
     * logger) that don't know the test name — the current test is resolved
     * from the thread-local context set by the TestNG listener. Because the
     * adapter's stores are static, any instance (including the TestNG-created
     * listener) shares the same data, so you can call it on any adapter
     * instance:</p>
     * <pre>{@code
     * new AppiumAdapter().recordStep("tap login button");
     * }</pre>
     *
     * <p>If called when no test is running on this thread, the step is
     * recorded under a fallback bucket and a warning is logged.</p>
     *
     * @param stepName the step description
     * @param status the step outcome
     * @param durationMs how long the step took, in milliseconds
     * @param description optional extra detail (may be null)
     */
    public void recordStep(String stepName, TestStatus status, long durationMs, String description) {
        String testKey = getCurrentTestKey();
        if (testKey == null) {
            testKey = "unknown-test";
            logger.warn("recordStep called with no active test on this thread; attaching to '{}'", testKey);
        }
        recordStep(testKey, stepName, status, durationMs, description);
    }

    /**
     * Records a passing step against the current test on this thread.
     *
     * @param stepName the step description
     */
    public void recordStep(String stepName) {
        recordStep(stepName, TestStatus.PASSED, 0, null);
    }

    // ------------------------------------------------------------------
    // Artifacts
    // ------------------------------------------------------------------

    /**
     * Captures a mobile screenshot and attaches it to the specified test.
     *
     * <p>Use this method to capture screenshots from Appium drivers during test
     * execution. The screenshot file should already be saved to disk.</p>
     *
     * @param testName the name of the test to attach the screenshot to
     * @param fileName the name of the screenshot file (e.g., "home-screen.png")
     * @param filePath the absolute path to the screenshot file
     * @param fileSize the size of the screenshot file in bytes
     * @throws IllegalArgumentException if any parameter is null or if testName/fileName is empty
     */
    public void captureScreenshot(String testName, String fileName, String filePath, long fileSize) {
        validateParameter(testName, "testName");
        validateParameter(fileName, "fileName");
        validateParameter(filePath, "filePath");

        Artifact screenshot = Artifact.builder()
                .name(fileName)
                .type("screenshot")
                .path(filePath)
                .mimeType("image/png")
                .size(fileSize)
                .timestamp(Instant.now())
                .build();

        addArtifact(testName, screenshot);
        logger.debug("Captured mobile screenshot '{}' for test '{}'", fileName, testName);
    }

    /**
     * Captures app logs and attaches them to the specified test.
     *
     * <p>Use this method to capture application logs, crash reports, or debug
     * information from the mobile app during test execution.</p>
     *
     * @param testName the name of the test to attach the logs to
     * @param logFileName the name of the log file (e.g., "app.log", "crash-report.txt")
     * @param logContent the content of the logs
     * @throws IllegalArgumentException if any parameter is null or if testName/logFileName is empty
     */
    public void captureAppLogs(String testName, String logFileName, String logContent) {
        validateParameter(testName, "testName");
        validateParameter(logFileName, "logFileName");
        validateParameter(logContent, "logContent");

        Artifact appLog = Artifact.builder()
                .name(logFileName)
                .type("log")
                .path("/artifacts/logs/" + logFileName)
                .mimeType("text/plain")
                .size((long) logContent.length())
                .timestamp(Instant.now())
                .build();

        addArtifact(testName, appLog);
        logger.debug("Captured app logs '{}' for test '{}'", logFileName, testName);
    }

    /**
     * Captures device information and attaches it to the specified test.
     *
     * <p>Use this method to record device-specific information such as device model,
     * OS version, screen resolution, or other relevant device capabilities.</p>
     *
     * @param testName the name of the test to attach the device info to
     * @param deviceInfo the device information string (e.g., "iPhone 14 Pro, iOS 17.0, 1170x2532")
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public void captureDeviceInfo(String testName, String deviceInfo) {
        validateParameter(testName, "testName");
        validateParameter(deviceInfo, "deviceInfo");

        Artifact deviceInfoArtifact = Artifact.builder()
                .name("device-info.txt")
                .type("device-info")
                .path("/artifacts/device-info/device-info.txt")
                .mimeType("text/plain")
                .size((long) deviceInfo.length())
                .timestamp(Instant.now())
                .build();

        addArtifact(testName, deviceInfoArtifact);
        logger.debug("Captured device info for test '{}': {}", testName, deviceInfo);
    }

    // ------------------------------------------------------------------
    // Video recording
    // ------------------------------------------------------------------

    /**
     * Attaches a screen recording (video) to the specified test.
     *
     * <p>How the third argument is interpreted depends on the configured video
     * storage mode ({@code reporter.video.storage}):</p>
     * <ul>
     *   <li><b>path</b> (default) — {@code source} is a local/hosted file path;
     *       the report renders an inline {@code <video>} player streaming from it.</li>
     *   <li><b>url</b> — {@code source} is an external URL (e.g. Minio/S3); same
     *       rendering, pointing at the URL.</li>
     *   <li><b>embed</b> — {@code source} is the raw Base64 video; bytes are
     *       embedded in the report (larger file, fully self-contained).</li>
     * </ul>
     *
     * @param testName the name of the test to attach the video to
     * @param fileName the video file name (e.g., "test-recording.mp4")
     * @param source the video source: file path, URL, or Base64 content depending on mode
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public void captureVideo(String testName, String fileName, String source) {
        validateParameter(testName, "testName");
        validateParameter(fileName, "fileName");
        validateParameter(source, "source");

        String mode = videoStorageMode();
        Artifact.Builder builder = Artifact.builder()
                .name(fileName)
                .type("video")
                .mimeType("video/mp4")
                .timestamp(Instant.now());

        if ("embed".equals(mode)) {
            builder.path("/artifacts/videos/" + fileName)
                    .content(source)
                    .size(estimateDecodedSize(source));
        } else {
            // path or url: reference, do not embed
            builder.path(source).size(fileSizeOf(source));
        }

        addArtifact(testName, builder.build());
        logger.debug("Captured screen recording '{}' for test '{}' (mode={})", fileName, testName, mode);
    }

    /**
     * Resolves the configured video storage mode, defaulting to "path".
     */
    private static String videoStorageMode() {
        try {
            io.github.pulsereport.config.ReporterConfig cfg =
                    io.github.pulsereport.config.ReporterConfig.autoDetect();
            if (cfg != null && cfg.getVideoStorage() != null && !cfg.getVideoStorage().isBlank()) {
                return cfg.getVideoStorage();
            }
        } catch (Exception e) {
            logger.debug("Could not load reporter config for video storage mode: {}", e.getMessage());
        }
        return "path";
    }

    private static long fileSizeOf(String path) {
        try {
            return java.nio.file.Files.size(java.nio.file.Paths.get(path));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Estimates the decoded byte size of a Base64 string without decoding it.
     */
    private static long estimateDecodedSize(String base64) {
        long len = base64.length();
        long padding = 0;
        if (len > 0 && base64.charAt(base64.length() - 1) == '=') {
            padding++;
        }
        if (len > 1 && base64.charAt(base64.length() - 2) == '=') {
            padding++;
        }
        return (len * 3 / 4) - padding;
    }

    // ------------------------------------------------------------------
    // Crash / ANR detection
    // ------------------------------------------------------------------

    /**
     * Attaches a crash or ANR report to the specified test.
     *
     * <p>Use this to capture native crash logs (Android logcat crash buffer,
     * iOS crash logs) or Application-Not-Responding traces. These are surfaced
     * as a distinct {@code crash} artifact type so the report can highlight
     * "app crashed" separately from an ordinary assertion failure.</p>
     *
     * @param testName the name of the test to attach the crash report to
     * @param fileName the report file name (e.g., "crash.log", "anr-trace.txt")
     * @param content the crash/ANR content
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public void captureCrashReport(String testName, String fileName, String content) {
        validateParameter(testName, "testName");
        validateParameter(fileName, "fileName");
        validateParameter(content, "content");

        Artifact crash = Artifact.builder()
                .name(fileName)
                .type("crash")
                .path("/artifacts/crashes/" + fileName)
                .mimeType("text/plain")
                .size((long) content.length())
                .timestamp(Instant.now())
                .content(content)
                .build();

        addArtifact(testName, crash);
        logger.debug("Captured crash report '{}' for test '{}'", fileName, testName);
    }

    // ------------------------------------------------------------------
    // Page source
    // ------------------------------------------------------------------

    /**
     * Captures the current page (UI hierarchy) source and attaches it to the
     * specified test.
     *
     * <p>The page source is the XML representation of the current screen's
     * view hierarchy and is invaluable for debugging UI failures. When the
     * driver is registered in {@link MobileDriverHolder}, this is captured
     * automatically on failure — this method is for on-demand capture.</p>
     *
     * @param testName the name of the test to attach the page source to
     * @param pageSource the XML page source from {@code driver.getPageSource()}
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public void capturePageSource(String testName, String pageSource) {
        validateParameter(testName, "testName");
        validateParameter(pageSource, "pageSource");

        Artifact source = Artifact.builder()
                .name("page-source.xml")
                .type("page-source")
                .path("/artifacts/page-source/page-source.xml")
                .mimeType("application/xml")
                .size((long) pageSource.length())
                .timestamp(Instant.now())
                .content(pageSource)
                .build();

        addArtifact(testName, source);
        logger.debug("Captured page source for test '{}'", testName);
    }

    // ------------------------------------------------------------------
    // Session metadata -> TestRun.environment
    // ------------------------------------------------------------------

    /**
     * Records structured mobile session metadata that is surfaced in the final
     * {@code TestRun.environment} block (platform, device, app version, etc.).
     *
     * <p>Call this once the session capabilities are known (typically in
     * {@code @BeforeSuite} or after driver creation) so the report header and
     * CI filters can display/filter on device and app details.</p>
     *
     * @param metadata the session metadata (must not be null)
     */
    public void recordSessionMetadata(MobileSessionMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        String suiteKey = currentSuiteKey();
        sessionMetadataBySuite.computeIfAbsent(suiteKey, k -> new ConcurrentHashMap<>())
                .putAll(metadata.toEnvironmentMap());
        logger.debug("Recorded mobile session metadata for suite '{}': {}", suiteKey, metadata);
    }

    /**
     * Derives the suite bucket for metadata: from the active suite on this
     * thread, else from the suite prefix of the current test key, else a
     * shared default bucket.
     */
    private String currentSuiteKey() {
        String suite = currentSuiteName.get();
        if (suite != null) {
            return suite;
        }
        String testKey = getCurrentTestKey();
        if (testKey != null) {
            int dot = testKey.indexOf('.');
            if (dot > 0) {
                return testKey.substring(0, dot);
            }
        }
        return "default";
    }

    @Override
    public void onSuiteStart(String suiteName) {
        currentSuiteName.set(suiteName);
        sessionMetadataBySuite.remove(suiteName);
        super.onSuiteStart(suiteName);
    }

    /**
     * Merges the metadata recorded for the suite that owns this TestRun. The
     * suite name is taken from the run; when the run has multiple suites, all
     * their metadata buckets are merged (single-suite mobile runs use one).
     */
    @Override
    protected TestRun enrichTestRun(TestRun builtTestRun) {
        if (builtTestRun == null) {
            return builtTestRun;
        }

        Map<String, String> metadata = collectMetadataForRun(builtTestRun);
        String finishingSuite = currentSuiteName.get();
        if (finishingSuite != null) {
            sessionMetadataBySuite.remove(finishingSuite);
            currentSuiteName.remove();
        }
        if (metadata.isEmpty()) {
            return builtTestRun;
        }

        Map<String, String> mergedEnvironment = new LinkedHashMap<>();
        if (builtTestRun.getEnvironment() != null) {
            mergedEnvironment.putAll(builtTestRun.getEnvironment());
        }
        mergedEnvironment.putAll(metadata);

        return TestRun.builder()
                .id(builtTestRun.getId())
                .name(builtTestRun.getName())
                .startTime(builtTestRun.getStartTime())
                .endTime(builtTestRun.getEndTime())
                .duration(builtTestRun.getDuration())
                .status(builtTestRun.getStatus())
                .suites(builtTestRun.getSuites())
                .environment(mergedEnvironment)
                .totalTests(builtTestRun.getTotalTests())
                .passedTests(builtTestRun.getPassedTests())
                .failedTests(builtTestRun.getFailedTests())
                .skippedTests(builtTestRun.getSkippedTests())
                .build();
    }

    /**
     * Collects metadata buckets matching the run's suite names, falling back to
     * all buckets (and the default) so name-free recording still surfaces.
     */
    private Map<String, String> collectMetadataForRun(TestRun builtTestRun) {
        Map<String, String> result = new LinkedHashMap<>();
        for (var suite : builtTestRun.getSuites()) {
            Map<String, String> bucket = sessionMetadataBySuite.get(suite.getName());
            if (bucket != null) {
                result.putAll(bucket);
            }
        }
        // Session metadata is run-level (one device/app per run in practice), so when
        // name-matching finds nothing — e.g. name-free recording before a suite context,
        // or the recorded bucket key not matching the built suite name — merge the rest.
        if (result.isEmpty()) {
            for (Map<String, String> bucket : sessionMetadataBySuite.values()) {
                result.putAll(bucket);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Device health metrics
    // ------------------------------------------------------------------

    /**
     * Records device health metrics (battery level, memory usage) for the
     * specified test.
     *
     * <p>Values are recorded as metrics with well-known names so reports can
     * chart them. Pass {@code -1} for any value that is unavailable.</p>
     *
     * @param testName the name of the test
     * @param batteryPercent battery level 0-100, or -1 if unknown
     * @param usedMemoryMb memory used by the app in MB, or -1 if unknown
     * @throws IllegalArgumentException if testName is null or empty
     */
    public void recordDeviceHealth(String testName, int batteryPercent, double usedMemoryMb) {
        validateParameter(testName, "testName");

        if (batteryPercent > 100) {
            throw new IllegalArgumentException("batteryPercent must be between 0 and 100 (or -1 if unknown), got: " + batteryPercent);
        }
        if (batteryPercent >= 0) {
            addMetric(testName, Metric.builder()
                    .name("device.battery.percent")
                    .value(batteryPercent)
                    .unit("%")
                    .timestamp(Instant.now())
                    .build());
        }
        if (usedMemoryMb >= 0) {
            addMetric(testName, Metric.builder()
                    .name("device.memory.used")
                    .value(usedMemoryMb)
                    .unit("MB")
                    .timestamp(Instant.now())
                    .build());
        }
        logger.debug("Recorded device health for test '{}': battery={}%, mem={}MB",
                testName, batteryPercent, usedMemoryMb);
    }

    // ------------------------------------------------------------------
    // Performance metrics
    // ------------------------------------------------------------------

    /**
     * Records app launch time metric and attaches it to the specified test.
     *
     * @param testName the name of the test to attach the metric to
     * @param launchTimeMs the app launch time in milliseconds
     * @throws IllegalArgumentException if testName is null or empty
     */
    public void recordAppLaunchTime(String testName, double launchTimeMs) {
        validateParameter(testName, "testName");

        Metric launchTimeMetric = Metric.builder()
                .name("app.launch.time")
                .value(launchTimeMs)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        addMetric(testName, launchTimeMetric);
        logger.debug("Recorded app launch time for test '{}': {} ms", testName, launchTimeMs);
    }

    /**
     * Records screen transition time metric and attaches it to the specified test.
     *
     * @param testName the name of the test to attach the metric to
     * @param transitionTimeMs the screen transition time in milliseconds
     * @throws IllegalArgumentException if testName is null or empty
     */
    public void recordScreenTransitionTime(String testName, double transitionTimeMs) {
        validateParameter(testName, "testName");

        Metric transitionTimeMetric = Metric.builder()
                .name("screen.transition.time")
                .value(transitionTimeMs)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        addMetric(testName, transitionTimeMetric);
        logger.debug("Recorded screen transition time for test '{}': {} ms", testName, transitionTimeMs);
    }

    // ------------------------------------------------------------------
    // Automatic failure capture
    // ------------------------------------------------------------------

    /**
     * Called when a test fails. Captures automatic failure artifacts
     * (screenshot + page source) from the registered driver, then delegates to
     * the superclass to record the result.
     *
     * @param result the test result
     */
    @Override
    public void onTestFailure(ITestResult result) {
        captureAutomaticFailureArtifacts(result.getName());
        super.onTestFailure(result);
    }

    /**
     * Captures a screenshot and page source from the driver registered for the
     * current thread, attaching both to the failing test. Failures in capture
     * are logged and swallowed so they never mask the real test failure.
     */
    private void captureAutomaticFailureArtifacts(String testName) {
        MobileDriverHolder.get().ifPresent(driver -> {
            captureFailureScreenshot(testName, driver);
            captureFailurePageSource(testName, driver);
        });
    }

    private void captureFailureScreenshot(String testName, WebDriver driver) {
        try {
            if (driver instanceof TakesScreenshot ts) {
                byte[] png = ts.getScreenshotAs(OutputType.BYTES);
                String base64 = Base64.getEncoder().encodeToString(png);
                Artifact screenshot = Artifact.builder()
                        .name("failure-screenshot.png")
                        .type("screenshot")
                        .path("/artifacts/screenshots/failure-screenshot.png")
                        .mimeType("image/png")
                        .size((long) png.length)
                        .timestamp(Instant.now())
                        .content(base64)
                        .build();
                addArtifact(testName, screenshot);
                logger.debug("Auto-captured failure screenshot for test '{}'", testName);
            }
        } catch (RuntimeException e) {
            logger.warn("Could not auto-capture failure screenshot for '{}': {}", testName, e.getMessage());
        }
    }

    private void captureFailurePageSource(String testName, WebDriver driver) {
        try {
            String source = driver.getPageSource();
            if (source != null && !source.isBlank()) {
                Artifact pageSource = Artifact.builder()
                        .name("failure-page-source.xml")
                        .type("page-source")
                        .path("/artifacts/page-source/failure-page-source.xml")
                        .mimeType("application/xml")
                        .size((long) source.length())
                        .timestamp(Instant.now())
                        .content(source)
                        .build();
                addArtifact(testName, pageSource);
                logger.debug("Auto-captured failure page source for test '{}'", testName);
            }
        } catch (RuntimeException e) {
            logger.warn("Could not auto-capture failure page source for '{}': {}", testName, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Validates that a parameter is not null or empty.
     *
     * @param parameter the parameter to validate
     * @param parameterName the name of the parameter (for error messages)
     * @throws IllegalArgumentException if parameter is null or empty
     */
    private void validateParameter(String parameter, String parameterName) {
        if (parameter == null) {
            throw new IllegalArgumentException(parameterName + " cannot be null");
        }
        if (parameter.trim().isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be empty");
        }
    }
}
