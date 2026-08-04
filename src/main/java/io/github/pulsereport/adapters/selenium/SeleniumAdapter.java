package io.github.pulsereport.adapters.selenium;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.pulsereport.adapters.testng.TestNGAdapter;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestRun;

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

    private final Map<String, String> runBrowserMetadata = new ConcurrentHashMap<>();

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

    @Override
    protected TestRun enrichTestRun(TestRun builtTestRun) {
        if (builtTestRun == null || runBrowserMetadata.isEmpty()) {
            return builtTestRun;
        }

        Map<String, String> mergedEnvironment = new LinkedHashMap<>();
        if (builtTestRun.getEnvironment() != null) {
            mergedEnvironment.putAll(builtTestRun.getEnvironment());
        }
        mergedEnvironment.putAll(runBrowserMetadata);

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
