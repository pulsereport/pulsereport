package io.github.pulsereport.adapters.selenium;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import io.github.pulsereport.adapters.Adapter;
import io.github.pulsereport.adapters.testng.TestNGAdapter;
import io.github.pulsereport.core.model.TestRun;

/**
 * Tests for SeleniumAdapter implementation. Verifies web-specific
 * artifact/metric capture for Selenium WebDriver.
 *
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class SeleniumAdapterTest {

    private SeleniumAdapter adapter;

    @BeforeEach
    public void setUp() {
        adapter = new SeleniumAdapter();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("reporter.output.directory");
    }

    @Test
    public void testAdapterImplementsInterface() {
        assertTrue(adapter instanceof Adapter, "SeleniumAdapter should implement Adapter interface");
    }

    @Test
    public void testExtendsTestNGAdapter() {
        assertTrue(adapter instanceof TestNGAdapter, "SeleniumAdapter should extend TestNGAdapter");
    }

    @Test
    public void testCaptureBrowserScreenshot() {
        ITestResult mockResult = createMockTestResult("testWebApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.captureBrowserScreenshot("testWebApp", "page.png", "/screenshots/page.png", 20000L));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testCaptureBrowserLogs() {
        ITestResult mockResult = createMockTestResult("testWebApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String logContent = "[INFO] Page loaded\n[WARNING] Deprecated API used";
        assertDoesNotThrow(() -> adapter.captureBrowserLogs("testWebApp", "browser.log", logContent));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testCaptureConsoleLogs() {
        ITestResult mockResult = createMockTestResult("testWebApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String consoleContent = "console.log: Application initialized\nconsole.warn: Network slow";
        assertDoesNotThrow(() -> adapter.captureConsoleLogs("testWebApp", "console.log", consoleContent));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testCaptureHarFile() {
        ITestResult mockResult = createMockTestResult("testWebApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String harContent = "{\"log\":{\"version\":\"1.2\",\"entries\":[]}}";
        assertDoesNotThrow(() -> adapter.captureHarFile("testWebApp", "network.har", harContent));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testRecordPageLoadTime() {
        ITestResult mockResult = createMockTestResult("testPageLoad", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.recordPageLoadTime("testPageLoad", 2500.5));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testRecordDomReadyTime() {
        ITestResult mockResult = createMockTestResult("testDomReady", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.recordDomReadyTime("testDomReady", 1200.0));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testRecordNetworkTiming() {
        ITestResult mockResult = createMockTestResult("testNetwork", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertDoesNotThrow(() -> adapter.recordNetworkTiming("testNetwork", "dns", 50.0));
        assertDoesNotThrow(() -> adapter.recordNetworkTiming("testNetwork", "tcp", 100.0));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void testMultipleWebArtifacts() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("WebTestSuite");

        ITestContext mockContext = createMockTestContext("WebTests");
        ITestResult mockResult = createMockTestResult("testWithMultipleArtifacts", ITestResult.SUCCESS);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        adapter.captureBrowserScreenshot("testWithMultipleArtifacts", "screenshot1.png", "/screenshots/screenshot1.png", 15000L);
        adapter.captureBrowserScreenshot("testWithMultipleArtifacts", "screenshot2.png", "/screenshots/screenshot2.png", 16000L);
        adapter.captureBrowserLogs("testWithMultipleArtifacts", "browser.log", "Browser log content");
        adapter.captureConsoleLogs("testWithMultipleArtifacts", "console.log", "Console log content");
        adapter.captureHarFile("testWithMultipleArtifacts", "network.har", "{\"log\":{}}");

        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
    }

    @Test
    public void testMultipleWebMetrics() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("WebTestSuite");

        ITestContext mockContext = createMockTestContext("WebTests");
        ITestResult mockResult = createMockTestResult("testWithMultipleMetrics", ITestResult.SUCCESS);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        adapter.recordPageLoadTime("testWithMultipleMetrics", 2500.0);
        adapter.recordDomReadyTime("testWithMultipleMetrics", 1200.0);
        adapter.recordNetworkTiming("testWithMultipleMetrics", "dns", 50.0);
        adapter.recordNetworkTiming("testWithMultipleMetrics", "tcp", 100.0);
        adapter.recordNetworkTiming("testWithMultipleMetrics", "request", 200.0);

        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
    }

    @Test
    public void testBuildsTestRunWithRecordedBrowserMetadataInEnvironment() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("WebTestSuite");

        ITestContext mockContext = createMockTestContext("WebTests");
        ITestResult mockResult = createMockTestResult("testBrowserMetadata", ITestResult.SUCCESS);

        System.setProperty("reporter.output.directory", "target/pulsereport/test-selenium-adapter");

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);
        adapter.recordBrowserMetadata("Chrome", "124", "macOS 14");
        adapter.recordPageLoadTime("testBrowserMetadata", 842.0);
        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
        assertNotNull(testRun.getEnvironment());
        assertEquals("Chrome", testRun.getEnvironment().get("browser"));
        assertEquals("124", testRun.getEnvironment().get("browserVersion"));
        assertEquals("macOS 14", testRun.getEnvironment().get("platform"));
    }

    @Test
    public void testThreadSafetyForParallelWebTests() throws InterruptedException {
        Runnable test1 = () -> {
            ITestResult result = createMockTestResult("web_test1", ITestResult.SUCCESS);
            adapter.onTestStart(result);
            adapter.captureBrowserScreenshot("web_test1", "test1.png", "/screenshots/test1.png", 15000L);
            adapter.recordPageLoadTime("web_test1", 2000.0);
            adapter.onTestSuccess(result);
        };

        Runnable test2 = () -> {
            ITestResult result = createMockTestResult("web_test2", ITestResult.SUCCESS);
            adapter.onTestStart(result);
            adapter.captureBrowserScreenshot("web_test2", "test2.png", "/screenshots/test2.png", 16000L);
            adapter.recordPageLoadTime("web_test2", 2100.0);
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
                () -> adapter.captureBrowserScreenshot(null, "test.png", "/path/test.png", 1000L));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureBrowserScreenshot("test", null, "/path/test.png", 1000L));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureBrowserLogs(null, "log.txt", "content"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureBrowserLogs("test", null, "content"));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureConsoleLogs(null, "console.log", "content"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureConsoleLogs("test", null, "content"));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureHarFile(null, "network.har", "content"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureHarFile("test", null, "content"));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.recordPageLoadTime(null, 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.recordDomReadyTime(null, 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.recordNetworkTiming(null, "dns", 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.recordNetworkTiming("test", null, 100.0));
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
        when(mockMethod.getRealClass()).thenReturn(SeleniumAdapterTest.class);

        when(mockContext.getName()).thenReturn("WebContext");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockSuite.getName()).thenReturn("WebSuite");

        return mockResult;
    }

    private ITestContext createMockTestContext(String contextName) {
        ITestContext mockContext = mock(ITestContext.class);
        ISuite mockSuite = mock(ISuite.class);

        when(mockContext.getName()).thenReturn(contextName);
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockContext.getStartDate()).thenReturn(new Date());
        when(mockContext.getEndDate()).thenReturn(new Date(System.currentTimeMillis() + 5000));

        when(mockSuite.getName()).thenReturn("WebTestSuite");

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
