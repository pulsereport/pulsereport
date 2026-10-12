package io.github.pulsereport.adapters.selenium;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;

import io.github.pulsereport.adapters.testng.TestNGAdapter;
import io.github.pulsereport.config.ReporterConfig;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;

/**
 * Selenium WebDriver adapter for web test automation.
 *
 * <p>
 * This adapter extends {@link TestNGAdapter} to provide Selenium-specific
 * functionality for capturing browser screenshots, console logs, network data,
 * and web performance metrics.</p>
 *
 * <h2>Features</h2>
 * <ul>
 * <li>Captures browser screenshots during test execution</li>
 * <li>Captures browser logs and JavaScript console output</li>
 * <li>Captures network traffic via HAR (HTTP Archive) files</li>
 * <li>Records web performance metrics (page load time, DOM ready, network
 * timing)</li>
 * <li>Thread-safe for parallel browser test execution</li>
 * <li>Integrates seamlessly with TestNG</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class WebTest {
 *     private static SeleniumAdapter adapter = new SeleniumAdapter();
 *     private WebDriver driver;
 *
 *     @BeforeMethod
 *     public void setUp() {
 *         driver = new ChromeDriver();
 *     }
 *
 *     @Test
 *     public void testWebApp() {
 *         long startTime = System.currentTimeMillis();
 *
 *         // Load page and record load time
 *         driver.get("https://example.com");
 *         long pageLoadTime = System.currentTimeMillis() - startTime;
 *         adapter.recordPageLoadTime("testWebApp", pageLoadTime);
 *
 *         // Capture screenshot
 *         File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
 *         adapter.captureBrowserScreenshot("testWebApp", "homepage.png",
 *                                           screenshot.getAbsolutePath(), screenshot.length());
 *
 *         // Capture browser logs
 *         LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
 *         StringBuilder logContent = new StringBuilder();
 *         for (LogEntry entry : logs) {
 *             logContent.append(entry.toString()).append("\n");
 *         }
 *         adapter.captureBrowserLogs("testWebApp", "browser.log", logContent.toString());
 *
 *         // Capture console logs
 *         JavascriptExecutor js = (JavascriptExecutor) driver;
 *         List<String> consoleLogs = (List<String>) js.executeScript(
 *             "return window.console.logs || []");
 *         adapter.captureConsoleLogs("testWebApp", "console.log", String.join("\n", consoleLogs));
 *
 *         // Record network timing
 *         Map<String, Long> timing = (Map<String, Long>) js.executeScript(
 *             "return {dns: performance.timing.domainLookupEnd - performance.timing.domainLookupStart}");
 *         adapter.recordNetworkTiming("testWebApp", "dns", timing.get("dns").doubleValue());
 *     }
 * }
 * }</pre>
 *
 * <h2>Steps</h2>
 * <pre>{@code
 * driver = adapter.track(new ChromeDriver());   // every click/type/open becomes a step
 *
 * adapter.step("Log in", () -> {               // groups actions under one named step
 *     driver.findElement(By.id("user")).sendKeys("demo");
 *     driver.findElement(By.id("login")).click();
 * });
 * }</pre>
 * <p>
 * Steps pass unless the action throws; failures record the error, stack trace
 * and a screenshot, then rethrow.</p>
 *
 * <h2>Screenshots and video</h2>
 * <pre>
 * reporter.screenshots.mode=failed   # all | failed | last | none
 * reporter.video.mode=none           # always | failed | none (requires ffmpeg)
 * reporter.video.fps=2
 * reporter.video.ffmpegPath=ffmpeg
 * </pre>
 * <p>
 * Set in {@code reporter.properties} or as {@code -D} system properties.</p>
 *
 * <h2>Integration with Selenium</h2>
 * <p>
 * This adapter works with any WebDriver implementation (ChromeDriver,
 * FirefoxDriver, etc.) and can be used to capture browser-specific artifacts
 * and metrics. The adapter doesn't directly depend on WebDriver instances -
 * instead, test code should extract the relevant data and pass it to the
 * adapter methods.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This adapter inherits thread-safety from {@link TestNGAdapter}, making it
 * safe to use with parallel browser test execution. Each thread maintains its
 * own test context to prevent artifact/metric collisions.</p>
 *
 * @author Pulse Report Team
 * @since 1.0.0
 * @see TestNGAdapter
 */
public class SeleniumAdapter extends TestNGAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumAdapter.class);

    private static final Map<String, String> runBrowserMetadata = Collections.synchronizedMap(new LinkedHashMap<>());

    private static final ThreadLocal<WebDriver> trackedDriver = new ThreadLocal<>();
    private static final ThreadLocal<Deque<StepScope>> openSteps = ThreadLocal.withInitial(ArrayDeque::new);
    private static final List<String> STEP_TABLE_HEADER = List.of("Action", "Details", "Status", "Duration");
    private static final ThreadLocal<TestCapture> testCapture = new ThreadLocal<>();
    private static volatile CaptureSettings captureSettings;

    private static final String MODE_ALL = "all";
    private static final String MODE_FAILED = "failed";
    private static final String MODE_LAST = "last";
    private static final String MODE_NONE = "none";
    private static final String MODE_ALWAYS = "always";

    /** A step body with no return value. */
    @FunctionalInterface
    public interface StepAction {
        void run() throws Exception;
    }

    /** A step body that returns a value (e.g. a located element). */
    @FunctionalInterface
    public interface StepSupplier<T> {
        T get() throws Exception;
    }

    private static final class StepScope {
        final List<List<String>> rows = new ArrayList<>();
        String firstName;
        String firstDetail;

        void add(String name, String detail, Throwable failure, long durationMs) {
            if (rows.isEmpty()) {
                firstName = name;
                firstDetail = detail;
            }
            rows.add(List.of(name, detail == null ? "" : detail,
                    failure == null ? "PASSED" : "FAILED", formatDuration(durationMs)));
        }
    }

    /** Per-test capture state: video recorder and the step held back for a final screenshot. */
    private static final class TestCapture {
        SeleniumVideoRecorder recorder;
        TestStep.Builder heldStep;
        boolean heldStepHasScreenshot;
        boolean failureScreenshotTaken;
    }

    record CaptureSettings(String screenshots, String video, int fps, String ffmpegPath, String videoStorage) {

        static CaptureSettings load() {
            ReporterConfig config = null;
            try {
                config = ReporterConfig.autoDetect();
            } catch (RuntimeException e) {
                logger.warn("PulseReport: could not read reporter.properties; using default capture settings", e);
            }
            if (config == null) {
                config = ReporterConfig.builder().build();
            }
            return new CaptureSettings(
                    option("reporter.screenshots.mode", config.getScreenshotMode(), MODE_FAILED,
                            List.of(MODE_ALL, MODE_FAILED, MODE_LAST, MODE_NONE)),
                    option("reporter.video.mode", config.getVideoMode(), MODE_NONE,
                            List.of(MODE_ALWAYS, MODE_FAILED, MODE_NONE)),
                    fps(System.getProperty("reporter.video.fps"), config.getVideoFps()),
                    System.getProperty("reporter.video.ffmpegPath", config.getFfmpegPath()),
                    config.getVideoStorage());
        }

        private static String option(String key, String configured, String fallback, List<String> allowed) {
            String value = System.getProperty(key, configured == null ? fallback : configured)
                    .trim().toLowerCase(Locale.ROOT);
            if (allowed.contains(value)) {
                return value;
            }
            logger.warn("PulseReport: invalid {}='{}'; expected one of {}. Using '{}'", key, value, allowed, fallback);
            return fallback;
        }

        private static int fps(String override, int configured) {
            int value = configured;
            if (override != null) {
                try {
                    value = Integer.parseInt(override.trim());
                } catch (NumberFormatException e) {
                    logger.warn("PulseReport: invalid reporter.video.fps='{}'; using {}", override, configured);
                }
            }
            return Math.min(30, Math.max(1, value));
        }
    }

    /**
     * Constructs a new SeleniumAdapter.
     */
    public SeleniumAdapter() {
        super();
        logger.info("SeleniumAdapter initialized");
    }

    /**
     * Records run-level browser metadata for the final TestRun environment.
     *
     * <p>
     * Call this once browser capabilities are available so the generated
     * TestRun can expose browser metadata to report renderers.</p>
     *
     * @param browserName the browser name (for example, Chrome)
     * @param browserVersion the browser version (for example, 124)
     * @param platform the execution platform (for example, macOS 14)
     */
    public void recordBrowserMetadata(String browserName, String browserVersion, String platform) {
        putRunBrowserMetadata("browser", browserName);
        putRunBrowserMetadata("browserVersion", browserVersion);
        putRunBrowserMetadata("platform", platform);
        logger.debug("Recorded browser metadata for run: browser='{}', version='{}', platform='{}'",
                browserName,
                browserVersion,
                platform);
    }

    @Override
    public void onSuiteStart(String suiteName) {
        runBrowserMetadata.clear();
        super.onSuiteStart(suiteName);
    }

    /**
     * Captures a browser screenshot and attaches it to the specified test.
     *
     * <p>
     * Use this method to capture screenshots from WebDriver during test
     * execution. The screenshot file should already be saved to disk.</p>
     *
     * @param testName the name of the test to attach the screenshot to
     * @param fileName the name of the screenshot file (e.g., "homepage.png")
     * @param filePath the absolute path to the screenshot file
     * @param fileSize the size of the screenshot file in bytes
     * @throws IllegalArgumentException if any parameter is null or if
     * testName/fileName is empty
     */
    public void captureBrowserScreenshot(String testName, String fileName, String filePath, long fileSize) {
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
        logger.debug("Captured browser screenshot '{}' for test '{}'", fileName, testName);
    }

    /**
     * Captures browser logs and attaches them to the specified test.
     *
     * <p>
     * Use this method to capture browser-level logs (network errors, security
     * warnings, etc.) from the WebDriver logging API.</p>
     *
     * @param testName the name of the test to attach the logs to
     * @param logFileName the name of the log file (e.g., "browser.log")
     * @param logContent the content of the browser logs
     * @throws IllegalArgumentException if any parameter is null or if
     * testName/logFileName is empty
     */
    public void captureBrowserLogs(String testName, String logFileName, String logContent) {
        validateParameter(testName, "testName");
        validateParameter(logFileName, "logFileName");
        validateParameter(logContent, "logContent");

        Artifact browserLog = Artifact.builder()
                .name(logFileName)
                .type("browser-log")
                .path("/artifacts/browser-logs/" + logFileName)
                .mimeType("text/plain")
                .size((long) logContent.length())
                .timestamp(Instant.now())
                .build();

        addArtifact(testName, browserLog);
        logger.debug("Captured browser logs '{}' for test '{}'", logFileName, testName);
    }

    /**
     * Captures JavaScript console logs and attaches them to the specified test.
     *
     * <p>
     * Use this method to capture console.log, console.warn, console.error, etc.
     * output from the browser's JavaScript console.</p>
     *
     * @param testName the name of the test to attach the console logs to
     * @param logFileName the name of the log file (e.g., "console.log")
     * @param logContent the content of the console logs
     * @throws IllegalArgumentException if any parameter is null or if
     * testName/logFileName is empty
     */
    public void captureConsoleLogs(String testName, String logFileName, String logContent) {
        validateParameter(testName, "testName");
        validateParameter(logFileName, "logFileName");
        validateParameter(logContent, "logContent");

        Artifact consoleLog = Artifact.builder()
                .name(logFileName)
                .type("console-log")
                .path("/artifacts/console-logs/" + logFileName)
                .mimeType("text/plain")
                .size((long) logContent.length())
                .timestamp(Instant.now())
                .build();

        addArtifact(testName, consoleLog);
        logger.debug("Captured console logs '{}' for test '{}'", logFileName, testName);
    }

    /**
     * Captures a HAR (HTTP Archive) file and attaches it to the specified test.
     *
     * <p>
     * HAR files contain detailed network traffic information including all HTTP
     * requests/responses, headers, timing data, etc. Use browser extensions or
     * proxy tools (like BrowserMob Proxy) to generate HAR files.</p>
     *
     * @param testName the name of the test to attach the HAR file to
     * @param harFileName the name of the HAR file (e.g., "network.har")
     * @param harContent the content of the HAR file (JSON format)
     * @throws IllegalArgumentException if any parameter is null or if
     * testName/harFileName is empty
     */
    public void captureHarFile(String testName, String harFileName, String harContent) {
        validateParameter(testName, "testName");
        validateParameter(harFileName, "harFileName");
        validateParameter(harContent, "harContent");

        Artifact harFile = Artifact.builder()
                .name(harFileName)
                .type("har")
                .path("/artifacts/har/" + harFileName)
                .mimeType("application/json")
                .size((long) harContent.length())
                .timestamp(Instant.now())
                .build();

        addArtifact(testName, harFile);
        logger.debug("Captured HAR file '{}' for test '{}'", harFileName, testName);
    }

    /**
     * Records page load time metric and attaches it to the specified test.
     *
     * <p>
     * Use this method to measure and record how long it takes for a web page to
     * fully load (including all resources).</p>
     *
     * @param testName the name of the test to attach the metric to
     * @param pageLoadTimeMs the page load time in milliseconds
     * @throws IllegalArgumentException if testName is null or empty
     */
    public void recordPageLoadTime(String testName, double pageLoadTimeMs) {
        validateParameter(testName, "testName");

        Metric pageLoadMetric = Metric.builder()
                .name("page.load.time")
                .value(pageLoadTimeMs)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        addMetric(testName, pageLoadMetric);
        logger.debug("Recorded page load time for test '{}': {} ms", testName, pageLoadTimeMs);
    }

    /**
     * Records DOM ready time metric and attaches it to the specified test.
     *
     * <p>
     * Use this method to measure and record how long it takes for the DOM to
     * become ready (DOMContentLoaded event).</p>
     *
     * @param testName the name of the test to attach the metric to
     * @param domReadyTimeMs the DOM ready time in milliseconds
     * @throws IllegalArgumentException if testName is null or empty
     */
    public void recordDomReadyTime(String testName, double domReadyTimeMs) {
        validateParameter(testName, "testName");

        Metric domReadyMetric = Metric.builder()
                .name("dom.ready.time")
                .value(domReadyTimeMs)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        addMetric(testName, domReadyMetric);
        logger.debug("Recorded DOM ready time for test '{}': {} ms", testName, domReadyTimeMs);
    }

    /**
     * Records network timing metric and attaches it to the specified test.
     *
     * <p>
     * Use this method to record specific network timing metrics such as DNS
     * lookup time, TCP connection time, SSL handshake time, etc. from the
     * Navigation Timing API.</p>
     *
     * @param testName the name of the test to attach the metric to
     * @param timingType the type of timing (e.g., "dns", "tcp", "ssl",
     * "request", "response")
     * @param timingMs the timing value in milliseconds
     * @throws IllegalArgumentException if testName or timingType is null or
     * empty
     */
    public void recordNetworkTiming(String testName, String timingType, double timingMs) {
        validateParameter(testName, "testName");
        validateParameter(timingType, "timingType");

        Metric networkTiming = Metric.builder()
                .name("network." + timingType + ".time")
                .value(timingMs)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        addMetric(testName, networkTiming);
        logger.debug("Recorded network timing '{}' for test '{}': {} ms", timingType, testName, timingMs);
    }

    /**
     * Wraps a driver so every open, click, type, clear and submit is recorded as
     * a step with its element, locator, URL change, page load time and outcome.
     * Use the returned driver in place of the original.
     *
     * @param driver the driver to track
     * @return a decorated driver that records steps
     */
    public WebDriver track(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        trackedDriver.set(driver);
        startVideo();
        return new EventFiringDecorator<WebDriver>(new SeleniumStepListener(this, driver)).decorate(driver);
    }

    /**
     * Runs {@code action} as a step named after the tracked actions it performs.
     *
     * @param action the step body
     */
    public void step(StepAction action) {
        step(null, action);
    }

    /**
     * Runs {@code action} as a named step. Tracked driver actions inside it are
     * listed in the step's table. Exceptions mark the step FAILED and are rethrown.
     *
     * @param message the step name shown in the report
     * @param action the step body
     */
    public void step(String message, StepAction action) {
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        step(message, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Runs {@code action} as a named step and returns its result.
     *
     * @param message the step name shown in the report
     * @param action the step body
     * @param <T> the result type
     * @return the value returned by {@code action}
     */
    public <T> T step(String message, StepSupplier<T> action) {
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        Deque<StepScope> scopes = openSteps.get();
        StepScope scope = new StepScope();
        scopes.push(scope);
        Instant startedAt = Instant.now();
        long start = System.nanoTime();
        Throwable failure = null;
        try {
            return action.get();
        } catch (Exception | AssertionError t) {
            failure = t;
            throw sneakyThrow(t);
        } finally {
            scopes.pop();
            finishStep(message, scope, failure, startedAt, (System.nanoTime() - start) / 1_000_000);
        }
    }

    void untrack(WebDriver driver) {
        if (trackedDriver.get() == driver) {
            trackedDriver.remove();
        }
    }

    /** Captures end-of-test evidence while the browser is still open, for drivers quit mid-test. */
    void beforeQuit(WebDriver driver) {
        TestCapture capture = testCapture.get();
        if (capture == null || trackedDriver.get() != driver) {
            return;
        }
        attachFinalScreenshot(capture, driver);
        if (capture.recorder != null) {
            capture.recorder.stop();
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        super.onTestStart(result);
        testCapture.set(new TestCapture());
        startVideo();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        finishTestCapture(result, false);
        super.onTestSuccess(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        finishTestCapture(result, true);
        super.onTestFailure(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        finishTestCapture(result, false);
        super.onTestSkipped(result);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        finishTestCapture(result, true);
        super.onTestFailedButWithinSuccessPercentage(result);
    }

    private static CaptureSettings settings() {
        CaptureSettings settings = captureSettings;
        if (settings == null) {
            settings = CaptureSettings.load();
            captureSettings = settings;
        }
        return settings;
    }

    private void startVideo() {
        TestCapture capture = testCapture.get();
        WebDriver driver = trackedDriver.get();
        CaptureSettings settings = settings();
        if (capture == null || capture.recorder != null || !(driver instanceof TakesScreenshot)
                || MODE_NONE.equals(settings.video())
                || !SeleniumVideoRecorder.isFfmpegAvailable(settings.ffmpegPath())) {
            return;
        }
        try {
            capture.recorder = new SeleniumVideoRecorder((TakesScreenshot) driver, settings.fps());
        } catch (IOException e) {
            logger.warn("PulseReport: could not start video recording: {}", e.getMessage());
        }
    }

    private void finishTestCapture(ITestResult result, boolean failed) {
        TestCapture capture = testCapture.get();
        testCapture.remove();
        String testKey = getCurrentTestKey();
        if (capture == null || testKey == null) {
            return;
        }
        WebDriver driver = trackedDriver.get();
        try {
            attachFinalScreenshot(capture, driver);
            if (capture.heldStep != null) {
                addStep(testKey, capture.heldStep.build());
                capture.failureScreenshotTaken |= failed && capture.heldStepHasScreenshot;
            }
            if (failed && !capture.failureScreenshotTaken && !MODE_NONE.equals(settings().screenshots())) {
                Artifact screenshot = screenshot(driver, "test-failure.png");
                if (screenshot != null) {
                    addArtifact(testKey, screenshot);
                }
            }
            if (capture.recorder != null) {
                finishVideo(testKey, videoFileName(result, testKey), capture.recorder, failed);
            }
        } catch (RuntimeException e) {
            logger.warn("PulseReport: could not finish screenshot/video capture for '{}'", testKey, e);
        }
    }

    private static void attachFinalScreenshot(TestCapture capture, WebDriver driver) {
        if (capture.heldStep == null || capture.heldStepHasScreenshot) {
            return;
        }
        Artifact screenshot = screenshot(driver, "final.png");
        if (screenshot != null) {
            capture.heldStep.artifacts(List.of(screenshot));
            capture.heldStepHasScreenshot = true;
        }
    }

    private void finishVideo(String testKey, String fileName, SeleniumVideoRecorder recorder, boolean failed) {
        recorder.stop();
        CaptureSettings settings = settings();
        try {
            if (MODE_FAILED.equals(settings.video()) && !failed) {
                return;
            }
            String relativePath = "videos/" + fileName;
            File output = new File(reportDirectory(), relativePath);
            if (!recorder.encode(output, settings.ffmpegPath(), settings.fps())) {
                return;
            }
            Artifact.Builder video = Artifact.builder()
                    .name(fileName)
                    .type("video")
                    .mimeType("video/mp4")
                    .path(relativePath)
                    .size(output.length())
                    .timestamp(Instant.now());
            if ("embed".equals(settings.videoStorage())) {
                video.content(Base64.getEncoder().encodeToString(Files.readAllBytes(output.toPath())));
            }
            addArtifact(testKey, video.build());
        } catch (IOException e) {
            logger.warn("PulseReport: could not attach video for '{}': {}", testKey, e.getMessage());
        } finally {
            recorder.discard();
        }
    }

    /** e.g. LoginTest.testValidLogin-1a2b3c.mp4; the hash keeps parameterized invocations apart. */
    private static String videoFileName(ITestResult result, String testKey) {
        String readable = result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
        return readable.replaceAll("[^A-Za-z0-9._-]", "_") + "-" + Integer.toHexString(testKey.hashCode()) + ".mp4";
    }

    private static File reportDirectory() {
        String dir = ReporterConfig.resolveOutputDirectory("reporter.output.directory", "target/pulsereport");
        File file = new File(dir);
        return file.isAbsolute() ? file : new File(System.getProperty("user.dir", "."), dir);
    }

    /** Formats a duration the same way the HTML report does: 16ms, 1.1s, 31s, 1m 05s. */
    static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        }
        if (ms < 10_000) {
            return String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
        }
        long seconds = Math.round(ms / 1000.0);
        return seconds < 60 ? seconds + "s" : String.format(Locale.ROOT, "%dm %02ds", seconds / 60, seconds % 60);
    }

    void recordAction(String name, String detail, Throwable failure, Instant startedAt, long durationMs,
            WebDriver driver) {
        StepScope scope = openSteps.get().peek();
        if (scope != null) {
            scope.add(name, detail, failure, durationMs);
            return;
        }
        recordStep(name, detail, null, failure, startedAt, durationMs, driver);
    }

    private void finishStep(String message, StepScope scope, Throwable failure, Instant startedAt,
            long durationMs) {
        boolean named = message != null && !message.isBlank();
        String name = named ? message : scope.firstName;
        String description = null;
        List<List<String>> table = null;

        if (name == null) {
            name = "Step";
        } else if (!named && scope.rows.size() == 1) {
            description = scope.firstDetail;
        } else if (!named) {
            name += " (+" + (scope.rows.size() - 1) + " more)";
        }
        if (!scope.rows.isEmpty() && (named || scope.rows.size() > 1)) {
            table = new ArrayList<>();
            table.add(STEP_TABLE_HEADER);
            table.addAll(scope.rows);
        }

        StepScope parent = openSteps.get().peek();
        if (parent != null) {
            parent.add(name, description, failure, durationMs);
            return;
        }
        recordStep(name, description, table, failure, startedAt, durationMs, trackedDriver.get());
    }

    private void recordStep(String name, String description, List<List<String>> table, Throwable failure,
            Instant startedAt, long durationMs, WebDriver driver) {
        String testKey = getCurrentTestKey();
        if (testKey == null) {
            logger.debug("No active test on this thread; step '{}' not recorded", name);
            return;
        }
        TestStep.Builder step = TestStep.builder()
                .name(name)
                .status(failure == null ? TestStatus.PASSED : TestStatus.FAILED)
                .startTime(startedAt)
                .endTime(startedAt.plusMillis(durationMs))
                .duration(durationMs)
                .description(description)
                .dataTable(table);
        String screenshotMode = settings().screenshots();
        Artifact screenshot = null;
        if (failure != null) {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            step.errorMessage(failure.getMessage() != null ? failure.getMessage() : failure.getClass().getName())
                    .stackTrace(trace.toString());
            if (!MODE_NONE.equals(screenshotMode)) {
                screenshot = screenshot(driver, "step-failure.png");
            }
        } else if (MODE_ALL.equals(screenshotMode)) {
            screenshot = screenshot(driver, "step.png");
        }
        if (screenshot != null) {
            step.artifacts(List.of(screenshot));
        }

        TestCapture capture = testCapture.get();
        if (capture != null && failure != null && screenshot != null) {
            capture.failureScreenshotTaken = true;
        }
        if (capture != null && MODE_LAST.equals(screenshotMode)) {
            // Hold the newest step so the end-of-test screenshot can be attached to it
            if (capture.heldStep != null) {
                addStep(testKey, capture.heldStep.build());
            }
            capture.heldStep = step;
            capture.heldStepHasScreenshot = screenshot != null;
            return;
        }
        addStep(testKey, step.build());
    }

    private static Artifact screenshot(WebDriver driver, String name) {
        if (!(driver instanceof TakesScreenshot)) {
            return null;
        }
        try {
            File file = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            return Artifact.builder()
                    .name(name)
                    .type("screenshot")
                    .path(file.getAbsolutePath())
                    .mimeType("image/png")
                    .size(file.length())
                    .timestamp(Instant.now())
                    .build();
        } catch (RuntimeException e) {
            logger.debug("Could not capture screenshot", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }

    @Override
    protected TestRun enrichTestRun(TestRun builtTestRun) {
        if (builtTestRun == null || runBrowserMetadata.isEmpty()) {
            return builtTestRun;
        }

        Map<String, String> mergedEnvironment = new LinkedHashMap<>();
        if (builtTestRun.getEnvironment() != null) {
            mergedEnvironment.putAll(builtTestRun.getEnvironment());
        }
        synchronized (runBrowserMetadata) {
            mergedEnvironment.putAll(runBrowserMetadata);
        }

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

    private void putRunBrowserMetadata(String key, String value) {
        if (value == null) {
            runBrowserMetadata.remove(key);
            return;
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            runBrowserMetadata.remove(key);
            return;
        }

        runBrowserMetadata.put(key, normalizedValue);
    }
}
