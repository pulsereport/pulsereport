package io.github.pulsereport.adapters.appium;

import io.github.pulsereport.adapters.Adapter;
import io.github.pulsereport.adapters.testng.TestNGAdapter;
import io.github.pulsereport.core.model.TestRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testng.ITestResult;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AppiumAdapter implementation.
 * Verifies mobile-specific artifact/metric capture for Appium framework.
 * 
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class AppiumAdapterTest {

    private AppiumAdapter adapter;

    @BeforeEach
    public void setUp() {
        adapter = new AppiumAdapter();
    }

    @Test
    public void adapterImplementsInterface() {
        assertTrue(adapter instanceof Adapter, "AppiumAdapter should implement Adapter interface");
    }

    @Test
    public void extendsTestNGAdapter() {
        assertTrue(adapter instanceof TestNGAdapter, "AppiumAdapter should extend TestNGAdapter");
    }

    @Test
    public void captureScreenshot() {
        ITestResult mockResult = createMockTestResult("testMobileApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.captureScreenshot("testMobileApp", "screenshot.png", "/screenshots/mobile.png", 15000L));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void captureAppLogs() {
        ITestResult mockResult = createMockTestResult("testMobileApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String logContent = "[INFO] App launched successfully\n[DEBUG] Screen loaded";
        assertDoesNotThrow(() -> adapter.captureAppLogs("testMobileApp", "app.log", logContent));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void captureDeviceInfo() {
        ITestResult mockResult = createMockTestResult("testMobileApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String deviceInfo = "Device: iPhone 14 Pro, OS: iOS 17.0, Screen: 1170x2532";
        assertDoesNotThrow(() -> adapter.captureDeviceInfo("testMobileApp", deviceInfo));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void recordAppLaunchTime() {
        ITestResult mockResult = createMockTestResult("testAppLaunch", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.recordAppLaunchTime("testAppLaunch", 1250.5));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void recordScreenTransitionTime() {
        ITestResult mockResult = createMockTestResult("testNavigation", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.recordScreenTransitionTime("testNavigation", 350.0));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void multipleMobileArtifacts() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("MobileTestSuite");

        ITestContext mockContext = createMockTestContext("MobileTests");
        ITestResult mockResult = createMockTestResult("testWithMultipleArtifacts", ITestResult.SUCCESS);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        // Capture multiple mobile-specific artifacts
        adapter.captureScreenshot("testWithMultipleArtifacts", "before.png", "/screenshots/before.png", 12000L);
        adapter.captureScreenshot("testWithMultipleArtifacts", "after.png", "/screenshots/after.png", 13000L);
        adapter.captureAppLogs("testWithMultipleArtifacts", "app.log", "App log content");
        adapter.captureDeviceInfo("testWithMultipleArtifacts", "iPhone 14 Pro, iOS 17");

        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
    }

    @Test
    public void multipleMobileMetrics() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("MobileTestSuite");

        ITestContext mockContext = createMockTestContext("MobileTests");
        ITestResult mockResult = createMockTestResult("testWithMultipleMetrics", ITestResult.SUCCESS);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        adapter.recordAppLaunchTime("testWithMultipleMetrics", 1500.0);
        adapter.recordScreenTransitionTime("testWithMultipleMetrics", 250.0);
        adapter.recordScreenTransitionTime("testWithMultipleMetrics", 300.0);

        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
    }

    @Test
    public void threadSafetyForParallelMobileTests() throws InterruptedException {
        Runnable test1 = () -> {
            ITestResult result = createMockTestResult("mobile_test1", ITestResult.SUCCESS);
            adapter.onTestStart(result);
            adapter.captureScreenshot("mobile_test1", "test1.png", "/screenshots/test1.png", 10000L);
            adapter.recordAppLaunchTime("mobile_test1", 1000.0);
            adapter.onTestSuccess(result);
        };

        Runnable test2 = () -> {
            ITestResult result = createMockTestResult("mobile_test2", ITestResult.SUCCESS);
            adapter.onTestStart(result);
            adapter.captureScreenshot("mobile_test2", "test2.png", "/screenshots/test2.png", 11000L);
            adapter.recordAppLaunchTime("mobile_test2", 1100.0);
            adapter.onTestSuccess(result);
        };

        Thread t1 = new Thread(test1);
        Thread t2 = new Thread(test2);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertNotNull(adapter);
    }

    @Test
    public void nullParameterHandling() {
        ITestResult mockResult = createMockTestResult("test", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureScreenshot(null, "test.png", "/path/test.png", 1000L));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureScreenshot("test", null, "/path/test.png", 1000L));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureAppLogs(null, "log.txt", "content"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureAppLogs("test", null, "content"));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureDeviceInfo(null, "info"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureDeviceInfo("test", null));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.recordAppLaunchTime(null, 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.recordScreenTransitionTime(null, 100.0));
    }

    // ---------------------------------------------------------------
    // New feature tests
    // ---------------------------------------------------------------

    @Test
    public void recordStepIsCapturedInTestRun() {
        ITestResult mockResult = createMockTestResult("testWithSteps", ITestResult.SUCCESS);
        ISuite mockSuite = createWiredSuite("MobileStepsSuite", "MobileSteps", mockResult);
        ITestContext mockContext = mockResult.getTestContext();

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        adapter.recordStep("testWithSteps", "launch app", 1200);
        adapter.recordStep("testWithSteps", "tap login", 150);
        adapter.recordStep("testWithSteps", "verify dashboard",
                io.github.pulsereport.core.model.TestStatus.PASSED, 80, "dashboard visible");

        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
        var testCase = testRun.getSuites().get(0).getTestCases().stream()
                .filter(tc -> "testWithSteps".equals(tc.getMethodName()))
                .findFirst().orElseThrow();
        assertEquals(3, testCase.getSteps().size(), "Steps should be recorded in order");
        assertEquals("launch app", testCase.getSteps().get(0).getName());
        assertEquals("tap login", testCase.getSteps().get(1).getName());
        assertEquals("verify dashboard", testCase.getSteps().get(2).getName());
        assertEquals("dashboard visible", testCase.getSteps().get(2).getDescription());
    }

    @Test
    public void recordStepNullValidation() {
        ITestResult mockResult = createMockTestResult("stepTest", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);
        assertThrows(IllegalArgumentException.class, () -> adapter.recordStep(null, "step", 1));
        assertThrows(IllegalArgumentException.class, () -> adapter.recordStep("stepTest", null, 1));
    }

    @Test
    public void captureVideo() {
        ITestResult mockResult = createMockTestResult("testVideo", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);
        assertDoesNotThrow(() -> adapter.captureVideo("testVideo", "recording.mp4", "/tmp/recording.mp4"));
        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void captureCrashReport() {
        ITestResult mockResult = createMockTestResult("testCrash", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);
        assertDoesNotThrow(() ->
                adapter.captureCrashReport("testCrash", "crash.log", "FATAL EXCEPTION: main"));
        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void capturePageSource() {
        ITestResult mockResult = createMockTestResult("testPageSource", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);
        assertDoesNotThrow(() ->
                adapter.capturePageSource("testPageSource", "<hierarchy><node/></hierarchy>"));
        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void recordDeviceHealth() {
        ITestResult mockResult = createMockTestResult("testHealth", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);
        assertDoesNotThrow(() -> adapter.recordDeviceHealth("testHealth", 85, 512.0));
        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void recordSessionMetadataSurfacesInEnvironment() {
        ITestResult mockResult = createMockTestResult("testMeta", ITestResult.SUCCESS);
        ISuite mockSuite = createWiredSuite("MetadataSuite", "Metadata", mockResult);
        ITestContext mockContext = mockResult.getTestContext();

        adapter.onStart(mockSuite);
        adapter.recordSessionMetadata(MobileSessionMetadata.builder()
                .platformName("iOS")
                .platformVersion("17.0")
                .deviceName("iPhone 14 Pro")
                .appVersion("3.2.1")
                .appiumServerVersion("2.4.1")
                .build());

        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);
        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
        assertNotNull(testRun.getEnvironment());
        assertEquals("iOS", testRun.getEnvironment().get("mobile.platform"));
        assertEquals("iPhone 14 Pro", testRun.getEnvironment().get("mobile.deviceName"));
        assertEquals("3.2.1", testRun.getEnvironment().get("mobile.appVersion"));
    }

    @Test
    public void automaticFailureCaptureUsesRegisteredDriver() {
        ITestResult mockResult = createMockTestResult("testAutoFail", ITestResult.FAILURE);
        ISuite mockSuite = createWiredSuite("FailureSuite", "Failure", mockResult);
        ITestContext mockContext = mockResult.getTestContext();

        org.openqa.selenium.WebDriver driver = mock(org.openqa.selenium.WebDriver.class,
                withSettings().extraInterfaces(org.openqa.selenium.TakesScreenshot.class));
        when(((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.BYTES))
                .thenReturn(new byte[]{1, 2, 3});
        when(driver.getPageSource()).thenReturn("<hierarchy/>");

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        MobileDriverHolder.set(driver);
        try {
            adapter.onTestFailure(mockResult);
        } finally {
            MobileDriverHolder.remove();
        }
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
        var testCase = testRun.getSuites().get(0).getTestCases().stream()
                .filter(tc -> "testAutoFail".equals(tc.getMethodName()))
                .findFirst().orElseThrow();
        boolean hasScreenshot = testCase.getArtifacts().stream()
                .anyMatch(a -> "failure-screenshot.png".equals(a.getName()));
        boolean hasPageSource = testCase.getArtifacts().stream()
                .anyMatch(a -> "failure-page-source.xml".equals(a.getName()));
        assertTrue(hasScreenshot, "Failure screenshot should be auto-captured");
        assertTrue(hasPageSource, "Failure page source should be auto-captured");
    }

    @Test
    public void failureWithoutDriverDoesNotBreak() {
        ITestResult mockResult = createMockTestResult("testFailNoDriver", ITestResult.FAILURE);
        adapter.onTestStart(mockResult);
        MobileDriverHolder.remove(); // ensure no driver
        assertDoesNotThrow(() -> adapter.onTestFailure(mockResult));
    }

    private ITestResult createMockTestResult(String methodName, int status) {
        ITestResult mockResult = mock(ITestResult.class);
        ITestNGMethod mockMethod = mock(ITestNGMethod.class);
        ITestContext mockContext = mock(ITestContext.class);
        ISuite mockSuite = mock(ISuite.class);

        when(mockResult.getMethod()).thenReturn(mockMethod);
        when(mockResult.getName()).thenReturn(methodName);
        when(mockResult.getStatus()).thenReturn(status);
        when(mockResult.getStartMillis()).thenReturn(System.currentTimeMillis());
        when(mockResult.getEndMillis()).thenReturn(System.currentTimeMillis() + 1000);
        when(mockResult.getTestContext()).thenReturn(mockContext);

        when(mockMethod.getMethodName()).thenReturn(methodName);
        when(mockMethod.getRealClass()).thenReturn(AppiumAdapterTest.class);

        when(mockContext.getName()).thenReturn("MobileContext");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockSuite.getName()).thenReturn("MobileSuite");

        return mockResult;
    }

    private ITestContext createMockTestContext(String contextName) {
        ITestContext mockContext = mock(ITestContext.class);
        ISuite mockSuite = mock(ISuite.class);

        when(mockContext.getName()).thenReturn(contextName);
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockContext.getStartDate()).thenReturn(new Date());
        when(mockContext.getEndDate()).thenReturn(new Date(System.currentTimeMillis() + 5000));

        when(mockSuite.getName()).thenReturn("MobileTestSuite");

        org.testng.IResultMap passedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap failedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap skippedTests = mock(org.testng.IResultMap.class);

        when(mockContext.getPassedTests()).thenReturn(passedTests);
        when(mockContext.getFailedTests()).thenReturn(failedTests);
        when(mockContext.getSkippedTests()).thenReturn(skippedTests);

        when(passedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(failedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(skippedTests.getAllResults()).thenReturn(new java.util.HashSet<>());

        return mockContext;
    }

    /**
     * Builds a fully-wired mock ISuite whose results contain the given test
     * result, so that {@code buildTestRun} produces a real TestCase that
     * assertions can run against.
     */
    private ISuite createWiredSuite(String suiteName, String contextName, ITestResult result) {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn(suiteName);

        ITestContext mockContext = mock(ITestContext.class);
        when(mockContext.getName()).thenReturn(contextName);
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockContext.getStartDate()).thenReturn(new Date());
        when(mockContext.getEndDate()).thenReturn(new Date(System.currentTimeMillis() + 5000));

        org.testng.IResultMap passedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap failedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap skippedTests = mock(org.testng.IResultMap.class);

        java.util.Set<ITestResult> passed = new java.util.HashSet<>();
        java.util.Set<ITestResult> failed = new java.util.HashSet<>();
        java.util.Set<ITestResult> skipped = new java.util.HashSet<>();

        if (result.getStatus() == ITestResult.SUCCESS) {
            passed.add(result);
        } else if (result.getStatus() == ITestResult.FAILURE) {
            failed.add(result);
        } else {
            skipped.add(result);
        }

        when(passedTests.getAllResults()).thenReturn(passed);
        when(failedTests.getAllResults()).thenReturn(failed);
        when(skippedTests.getAllResults()).thenReturn(skipped);
        when(mockContext.getPassedTests()).thenReturn(passedTests);
        when(mockContext.getFailedTests()).thenReturn(failedTests);
        when(mockContext.getSkippedTests()).thenReturn(skippedTests);

        // Make the result's own context point at this wired context.
        when(result.getTestContext()).thenReturn(mockContext);

        org.testng.ISuiteResult suiteResult = mock(org.testng.ISuiteResult.class);
        when(suiteResult.getTestContext()).thenReturn(mockContext);
        Map<String, org.testng.ISuiteResult> results = new java.util.HashMap<>();
        results.put(contextName, suiteResult);
        when(mockSuite.getResults()).thenReturn(results);

        return mockSuite;
    }
}
