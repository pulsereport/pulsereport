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
    public void testAdapterImplementsInterface() {
        assertTrue(adapter instanceof Adapter, "AppiumAdapter should implement Adapter interface");
    }

    @Test
    public void testExtendsTestNGAdapter() {
        assertTrue(adapter instanceof TestNGAdapter, "AppiumAdapter should extend TestNGAdapter");
    }

    @Test
    public void testCaptureScreenshot() {
        ITestResult mockResult = createMockTestResult("testMobileApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.captureScreenshot("testMobileApp", "screenshot.png", "/screenshots/mobile.png", 15000L));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testCaptureAppLogs() {
        ITestResult mockResult = createMockTestResult("testMobileApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String logContent = "[INFO] App launched successfully\n[DEBUG] Screen loaded";
        assertDoesNotThrow(() -> adapter.captureAppLogs("testMobileApp", "app.log", logContent));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testCaptureDeviceInfo() {
        ITestResult mockResult = createMockTestResult("testMobileApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String deviceInfo = "Device: iPhone 14 Pro, OS: iOS 17.0, Screen: 1170x2532";
        assertDoesNotThrow(() -> adapter.captureDeviceInfo("testMobileApp", deviceInfo));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testRecordAppLaunchTime() {
        ITestResult mockResult = createMockTestResult("testAppLaunch", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.recordAppLaunchTime("testAppLaunch", 1250.5));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testRecordScreenTransitionTime() {
        ITestResult mockResult = createMockTestResult("testNavigation", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.recordScreenTransitionTime("testNavigation", 350.0));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testMultipleMobileArtifacts() {
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
    public void testMultipleMobileMetrics() {
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
    public void testThreadSafetyForParallelMobileTests() throws InterruptedException {
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
    public void testNullParameterHandling() {
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
}
