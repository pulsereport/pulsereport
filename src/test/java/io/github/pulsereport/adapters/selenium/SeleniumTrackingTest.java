package io.github.pulsereport.adapters.selenium;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.TestStep;

/** Drivers wrapped with {@link SeleniumAdapter#track}, and the evidence the adapter writes. */
class SeleniumTrackingTest {

    @TempDir
    File reportDir;

    private RecordingAdapter adapter;
    private ITestResult result;

    /** Keeps what the adapter reports so tests can inspect it. */
    private static final class RecordingAdapter extends SeleniumAdapter {
        final List<TestStep> steps = new CopyOnWriteArrayList<>();
        final List<Artifact> artifacts = new CopyOnWriteArrayList<>();

        @Override
        public void addStep(String testName, TestStep step) {
            steps.add(step);
            super.addStep(testName, step);
        }

        @Override
        public void addArtifact(String testName, Artifact artifact) {
            artifacts.add(artifact);
            super.addArtifact(testName, artifact);
        }
    }

    @BeforeEach
    void setUp() {
        System.setProperty("reporter.output.directory", reportDir.getAbsolutePath());
        adapter = new RecordingAdapter();
        result = testResult("trackedTest");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("reporter.output.directory");
    }

    @Test
    void trackedNavigationBecomesAStep() {
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(new FakeBrowser());

        driver.get("https://example.test/login");
        adapter.onTestSuccess(result);
        driver.quit();

        assertEquals(1, adapter.steps.size());
        assertEquals("Open \"https://example.test/login\"", adapter.steps.get(0).getName());
    }

    @Test
    void failedQuitStopsTrackingTheDriver() {
        FakeBrowser browser = new FakeBrowser();
        browser.failQuit = true;
        WebDriver driver = adapter.track(browser);
        adapter.onTestStart(result);

        assertThrows(WebDriverException.class, driver::quit);
        adapter.onTestFailure(result);

        assertEquals(0, browser.screenshots.get(), "no failure screenshot from a driver that was quit");
    }

    @Test
    void listenerHoldsTheDriverOnlyForTheDurationOfACommand() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        FakeBrowser browser = new FakeBrowser();
        SeleniumStepListener listener = new SeleniumStepListener(adapter, browser, lock);
        Method getTitle = WebDriver.class.getMethod("getTitle");

        listener.beforeAnyCall(browser, getTitle, null);
        assertTrue(lock.isHeldByCurrentThread());
        listener.afterAnyCall(browser, getTitle, null, "");
        assertFalse(lock.isLocked());

        listener.beforeAnyCall(browser, getTitle, null);
        listener.onError(browser, getTitle, null, new InvocationTargetException(new WebDriverException("x")));
        assertFalse(lock.isLocked(), "released when the command fails");

        // A before-call with no matching after-call must not lock the recorder out for good
        listener.beforeAnyCall(browser, getTitle, null);
        listener.beforeAnyCall(browser, getTitle, null);
        listener.afterAnyCall(browser, getTitle, null, "");
        assertFalse(lock.isLocked());
    }

    @Test
    void logsAreWrittenToTheReportAndShownInline() throws Exception {
        adapter.onTestStart(result);
        adapter.captureBrowserLogs("trackedTest", "browser.log", "[SEVERE] boom");
        adapter.onTestSuccess(result);

        Artifact log = adapter.artifacts.get(0);
        assertEquals("[SEVERE] boom", log.getContent());
        assertTrue(log.getPath().startsWith("artifacts/browser-log/"), log.getPath());
        assertEquals("[SEVERE] boom", Files.readString(new File(reportDir, log.getPath()).toPath()));
    }

    @Test
    void largeHarFilesAreLinkedNotInlined() throws Exception {
        String har = "{\"log\":\"" + "x".repeat(SeleniumAdapter.MAX_INLINE_TEXT_BYTES) + "\"}";

        adapter.onTestStart(result);
        adapter.captureHarFile("trackedTest", "network.har", har);
        adapter.onTestSuccess(result);

        Artifact artifact = adapter.artifacts.get(0);
        assertNull(artifact.getContent());
        assertEquals(har.length(), new File(reportDir, artifact.getPath()).length());
    }

    @Test
    void attachmentNamesCannotLeaveTheArtifactsFolder() {
        adapter.onTestStart(result);
        adapter.captureConsoleLogs("trackedTest", "../../../escape.log", "text");
        adapter.onTestSuccess(result);

        String path = adapter.artifacts.get(0).getPath();
        assertFalse(path.contains("/../"), path);
        assertTrue(new File(reportDir, path).toPath().normalize().startsWith(reportDir.toPath()));
    }

    private static ITestResult testResult(String methodName) {
        ITestResult mockResult = mock(ITestResult.class);
        ITestNGMethod mockMethod = mock(ITestNGMethod.class);
        ITestContext mockContext = mock(ITestContext.class);
        ISuite mockSuite = mock(ISuite.class);

        when(mockResult.getMethod()).thenReturn(mockMethod);
        when(mockResult.getName()).thenReturn(methodName);
        when(mockResult.getStartMillis()).thenReturn(System.currentTimeMillis());
        when(mockResult.getEndMillis()).thenReturn(System.currentTimeMillis() + 1000);
        when(mockResult.getTestContext()).thenReturn(mockContext);
        when(mockMethod.getMethodName()).thenReturn(methodName);
        when(mockMethod.getRealClass()).thenReturn(SeleniumTrackingTest.class);
        when(mockContext.getName()).thenReturn("WebContext");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockSuite.getName()).thenReturn("WebSuite");
        return mockResult;
    }
}
