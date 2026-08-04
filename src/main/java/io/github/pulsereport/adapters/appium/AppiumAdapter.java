package io.github.pulsereport.adapters.appium;

import io.github.pulsereport.adapters.testng.TestNGAdapter;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Appium adapter for mobile test automation.
 * 
 * <p>This adapter extends {@link TestNGAdapter} to provide Appium-specific
 * functionality for capturing mobile screenshots, app logs, device information,
 * and mobile performance metrics.</p>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Captures mobile screenshots during test execution</li>
 *   <li>Captures app logs and crash reports</li>
 *   <li>Captures device information (OS, model, screen resolution)</li>
 *   <li>Records mobile performance metrics (app launch time, screen transitions)</li>
 *   <li>Thread-safe for parallel mobile test execution</li>
 *   <li>Integrates seamlessly with TestNG</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MobileTest {
 *     private static AppiumAdapter adapter = new AppiumAdapter();
 *     private AppiumDriver driver;
 *     
 *     @BeforeMethod
 *     public void setUp() {
 *         // Initialize Appium driver
 *         driver = new AndroidDriver(new URL("http://localhost:4723"), capabilities);
 *     }
 *     
 *     @Test
 *     public void testMobileApp() {
 *         long startTime = System.currentTimeMillis();
 *         
 *         // Launch app and record launch time
 *         driver.launchApp();
 *         long launchTime = System.currentTimeMillis() - startTime;
 *         adapter.recordAppLaunchTime("testMobileApp", launchTime);
 *         
 *         // Take screenshot
 *         File screenshot = driver.getScreenshotAs(OutputType.FILE);
 *         adapter.captureScreenshot("testMobileApp", "home-screen.png", 
 *                                    screenshot.getAbsolutePath(), screenshot.length());
 *         
 *         // Navigate and record transition time
 *         startTime = System.currentTimeMillis();
 *         driver.findElement(By.id("menu-button")).click();
 *         long transitionTime = System.currentTimeMillis() - startTime;
 *         adapter.recordScreenTransitionTime("testMobileApp", transitionTime);
 *         
 *         // Capture device info
 *         String deviceInfo = String.format("Device: %s, OS: %s %s",
 *             driver.getCapabilities().getCapability("deviceName"),
 *             driver.getCapabilities().getCapability("platformName"),
 *             driver.getCapabilities().getCapability("platformVersion"));
 *         adapter.captureDeviceInfo("testMobileApp", deviceInfo);
 *         
 *         // Capture app logs
 *         LogEntries logs = driver.manage().logs().get("logcat");
 *         StringBuilder logContent = new StringBuilder();
 *         for (LogEntry entry : logs) {
 *             logContent.append(entry.toString()).append("\n");
 *         }
 *         adapter.captureAppLogs("testMobileApp", "app.log", logContent.toString());
 *     }
 * }
 * }</pre>
 * 
 * <h2>Integration with Appium</h2>
 * <p>This adapter works with any Appium driver (AndroidDriver, IOSDriver, etc.)
 * and can be used to capture platform-specific artifacts and metrics. The adapter
 * doesn't directly depend on Appium driver instances - instead, test code should
 * extract the relevant data and pass it to the adapter methods.</p>
 * 
 * <h2>Thread Safety</h2>
 * <p>This adapter inherits thread-safety from {@link TestNGAdapter}, making it
 * safe to use with parallel mobile test execution. Each thread maintains its own
 * test context to prevent artifact/metric collisions.</p>
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 * @see TestNGAdapter
 */
public class AppiumAdapter extends TestNGAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AppiumAdapter.class);

    /**
     * Constructs a new AppiumAdapter.
     */
    public AppiumAdapter() {
        super();
        logger.info("AppiumAdapter initialized");
    }

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

    /**
     * Records app launch time metric and attaches it to the specified test.
     * 
     * <p>Use this method to measure and record how long it takes for the mobile
     * app to launch and become responsive.</p>
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
     * <p>Use this method to measure and record how long it takes to navigate from
     * one screen to another in the mobile app.</p>
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
