package io.github.pulsereport.adapters.selenium;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import io.github.pulsereport.core.model.Metric;
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
        System.clearProperty("pulsereport.output.directory");
    }

    @Test
    public void adapterImplementsInterface() {
        assertTrue(adapter instanceof Adapter, "SeleniumAdapter should implement Adapter interface");
    }

    @Test
    public void extendsTestNGAdapter() {
        assertTrue(adapter instanceof TestNGAdapter, "SeleniumAdapter should extend TestNGAdapter");
    }

    @Test
    public void captureBrowserLogs() {
        ITestResult mockResult = createMockTestResult("testWebApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String logContent = "[INFO] Page loaded\n[WARNING] Deprecated API used";
        assertDoesNotThrow(() -> adapter.captureBrowserLogs("browser.log", logContent));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void captureConsoleLogs() {
        ITestResult mockResult = createMockTestResult("testWebApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String consoleContent = "console.log: Application initialized\nconsole.warn: Network slow";
        assertDoesNotThrow(() -> adapter.captureConsoleLogs("console.log", consoleContent));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void captureHarFile() {
        ITestResult mockResult = createMockTestResult("testWebApp", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        String harContent = "{\"log\":{\"version\":\"1.2\",\"entries\":[]}}";
        assertDoesNotThrow(() -> adapter.captureHarFile("network.har", harContent));

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void screenshotWithoutATrackedDriverIsSkipped() {
        ITestResult mockResult = createMockTestResult("testNoDriver", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        assertNull(adapter.screenshot("page"));
        assertNull(adapter.capturePageSource());

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void multipleWebArtifacts() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("WebTestSuite");

        ITestContext mockContext = createMockTestContext("WebTests");
        ITestResult mockResult = createMockTestResult("testWithMultipleArtifacts", ITestResult.SUCCESS);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        adapter.captureBrowserLogs("browser.log", "Browser log content");
        adapter.captureConsoleLogs("console.log", "Console log content");
        adapter.captureHarFile("network.har", "{\"log\":{}}");

        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
    }

    @Test
    public void multipleWebMetrics() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("WebTestSuite");

        ITestContext mockContext = createMockTestContext("WebTests");
        ITestResult mockResult = createMockTestResult("testWithMultipleMetrics", ITestResult.SUCCESS);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        adapter.addMetric("testWithMultipleMetrics", metric("page.load.time", 2500.0));
        adapter.addMetric("testWithMultipleMetrics", metric("dom.ready.time", 1200.0));
        adapter.addMetric("testWithMultipleMetrics", metric("network.dns.time", 50.0));

        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
    }

    @Test
    public void buildsTestRunWithRecordedBrowserMetadataInEnvironment() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("WebTestSuite");

        ITestContext mockContext = createMockTestContext("WebTests");
        ITestResult mockResult = createMockTestResult("testBrowserMetadata", ITestResult.SUCCESS);

        System.setProperty("pulsereport.output.directory", "target/pulsereport/test-selenium-adapter");

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);
        adapter.recordBrowserMetadata("Chrome", "124", "macOS 14");
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
    public void threadSafetyForParallelWebTests() throws InterruptedException {
        Runnable test1 = () -> {
            ITestResult result = createMockTestResult("web_test1", ITestResult.SUCCESS);
            adapter.onTestStart(result);
            adapter.captureConsoleLogs("console1.log", "one");
            adapter.addMetric("web_test1", metric("page.load.time", 2000.0));
            adapter.onTestSuccess(result);
        };

        Runnable test2 = () -> {
            ITestResult result = createMockTestResult("web_test2", ITestResult.SUCCESS);
            adapter.onTestStart(result);
            adapter.captureConsoleLogs("console2.log", "two");
            adapter.addMetric("web_test2", metric("page.load.time", 2100.0));
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

        assertThrows(IllegalArgumentException.class, () -> adapter.captureBrowserLogs(null, "content"));
        assertThrows(IllegalArgumentException.class, () -> adapter.captureBrowserLogs("log.txt", null));
        assertThrows(IllegalArgumentException.class, () -> adapter.captureConsoleLogs(" ", "content"));
        assertThrows(IllegalArgumentException.class, () -> adapter.captureHarFile(null, "content"));
        assertThrows(IllegalArgumentException.class, () -> adapter.track(null));
        assertThrows(IllegalArgumentException.class, () -> adapter.step("x", (SeleniumAdapter.StepAction) null));

        adapter.onTestSuccess(mockResult);
    }

    private static Metric metric(String name, double value) {
        return Metric.builder().name(name).value(value).unit("ms").timestamp(Instant.now()).build();
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
