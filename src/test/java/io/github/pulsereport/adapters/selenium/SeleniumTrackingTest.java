package io.github.pulsereport.adapters.selenium;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.IInvokedMethod;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;

/** Drivers wrapped with {@link SeleniumAdapter#track}, and the evidence the adapter writes. */
class SeleniumTrackingTest {

    private static final AtomicInteger TEST_IDS = new AtomicInteger();

    @TempDir
    File reportDir;

    private RecordingAdapter adapter;
    private ITestResult result;
    private FakeBrowser browser;

    /** Exposes what the adapter recorded for the test it last started. */
    private static final class RecordingAdapter extends SeleniumAdapter {
        String key;

        @Override
        public void onTestStart(ITestResult result) {
            super.onTestStart(result);
            key = getCurrentTestKey();
        }

        List<TestStep> steps() {
            return recordedSteps(key);
        }

        List<Artifact> artifacts() {
            return recordedArtifacts(key);
        }

        List<Metric> metrics() {
            return recordedMetrics(key);
        }

        List<String> labels() {
            return recordedLabels(key);
        }

        TestRun enrich(TestRun run) {
            return enrichTestRun(run);
        }
    }

    @BeforeEach
    void setUp() {
        System.setProperty("pulsereport.output.directory", reportDir.getAbsolutePath());
        SeleniumAdapter.resetSettings();
        adapter = new RecordingAdapter();
        result = testResult("trackedTest" + TEST_IDS.incrementAndGet());
        browser = new FakeBrowser();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("pulsereport.output.directory");
        System.clearProperty("pulsereport.screenshots.mode");
        System.clearProperty("pulsereport.selenium.autoSteps");
        System.clearProperty("pulsereport.appium.autoSteps");
        System.clearProperty("pulsereport.selenium.maskInputs");
        System.clearProperty("pulsereport.selenium.stepDetail");
        System.clearProperty("pulsereport.screenshots.storage");
        System.clearProperty("pulsereport.screenshots.directory");
        SeleniumAdapter.setVideoUploader(null);
        SeleniumAdapter.resetSettings();
        // Most tests never quit their fake browser; stop tracking it so later
        // test classes on this thread don't find a tracked driver.
        SeleniumTracker.Tracked leftover = SeleniumTracker.active();
        if (leftover != null) {
            SeleniumTracker.untrack(leftover.raw());
        }
    }

    @Test
    void trackedNavigationBecomesAStepWithPageLoadMetrics() {
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        driver.get("https://example.test/login");
        adapter.onTestSuccess(result);
        driver.quit();

        assertEquals(1, adapter.steps().size());
        TestStep step = adapter.steps().get(0);
        assertEquals("Open \"https://example.test/login\"", step.getName());
        assertTrue(step.getDescription().contains("page load 420ms"), step.getDescription());
        Metric load = adapter.metrics().stream().filter(m -> m.getName().equals("page.load.time")).findFirst().orElseThrow();
        assertEquals(420, load.getValue());
        assertEquals("https://example.test/login", load.getLabel());
    }

    @Test
    void failedQuitStopsTrackingTheDriver() {
        browser.failQuit = true;
        WebDriver driver = adapter.track(browser);
        adapter.onTestStart(result);

        assertThrows(WebDriverException.class, driver::quit);
        adapter.onTestFailure(withFailure(result, new AssertionError("boom")));

        assertEquals(0, browser.screenshots.get(), "no failure screenshot from a driver that was quit");
    }

    @Test
    void listenerHoldsTheDriverOnlyForTheDurationOfACommand() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        SeleniumStepListener listener = new SeleniumStepListener(browser, lock, SeleniumTracker.ADAPTER_NAME);
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
    void elementActionsNameTheElementLocatorAndUrlChange() {
        browser.element(By.id("login"), "button", "Sign in", "submit").navigatesTo = "https://example.test/home";
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        driver.findElement(By.id("login")).click();
        adapter.onTestSuccess(result);

        TestStep step = adapter.steps().get(0);
        assertEquals("Click button \"Sign in\"", step.getName());
        assertTrue(step.getDescription().startsWith("By.id: login"), step.getDescription());
        assertTrue(step.getDescription().contains("→ https://example.test/home"), step.getDescription());
        assertTrue(step.getDescription().contains("page load"), "a click that loads a new page reports its load time");
    }

    @Test
    void namedStepsKeepTheirActionsAndNestedStepsAsChildren() {
        browser.element(By.id("user"), "input", "User", "text");
        browser.element(By.id("login"), "button", "Sign in", "submit");
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        adapter.step("Log in", () -> {
            driver.findElement(By.id("user")).sendKeys("demo");
            adapter.step("Submit", () -> driver.findElement(By.id("login")).click());
        });
        adapter.onTestSuccess(result);

        assertEquals(1, adapter.steps().size());
        TestStep login = adapter.steps().get(0);
        assertEquals("Log in", login.getName());
        assertNull(login.getDataTable(), "actions are sub-steps, not a table");
        assertEquals(2, login.getChildren().size());
        assertEquals("Type \"demo\" into input \"User\"", login.getChildren().get(0).getName());
        TestStep submit = login.getChildren().get(1);
        assertEquals("Submit", submit.getName());
        assertEquals("Click button \"Sign in\"", submit.getChildren().get(0).getName());
    }

    @Test
    void failingActionInsideAStepCarriesItsScreenshot() throws Exception {
        browser.element(By.id("login"), "button", "Sign in", "submit").clickFails = true;
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> adapter.step("Log in", () -> driver.findElement(By.id("login")).click()));
        adapter.onTestFailure(withFailure(result, failure));

        TestStep login = adapter.steps().get(0);
        assertEquals(TestStatus.FAILED, login.getStatus());
        TestStep click = login.getChildren().get(0);
        assertEquals(TestStatus.FAILED, click.getStatus());
        Artifact shot = click.getArtifacts().get(0);
        assertNotNull(shot.getContent(), "screenshots are embedded as Base64 by default");
        assertFalse(new File(reportDir, "artifacts/screenshots").exists(), "nothing is written when embedding");
        long screenshots = adapter.steps().stream().mapToLong(SeleniumTrackingTest::screenshotsIn).sum()
                + adapter.artifacts().stream().filter(a -> "screenshot".equals(a.getType())).count();
        assertEquals(1, screenshots, "the failure is photographed once, at the action that failed");
    }

    private static long screenshotsIn(TestStep step) {
        return step.getArtifacts().stream().filter(a -> "screenshot".equals(a.getType())).count()
                + step.getChildren().stream().mapToLong(SeleniumTrackingTest::screenshotsIn).sum();
    }

    @Test
    void screenshotsCanBeWrittenToAChosenFolder() throws Exception {
        File folder = new File(reportDir.getParentFile(), "shots-" + TEST_IDS.incrementAndGet());
        System.setProperty("pulsereport.screenshots.storage", "path");
        System.setProperty("pulsereport.screenshots.directory", folder.getAbsolutePath());
        SeleniumAdapter.resetSettings();
        adapter.onTestStart(result);
        adapter.track(browser);

        Artifact shot = adapter.screenshot("Dashboard");
        adapter.onTestSuccess(result);

        assertNull(shot.getContent());
        File file = new File(reportDir, shot.getPath()).getCanonicalFile();
        assertTrue(file.isFile(), "the report finds the screenshot at " + shot.getPath());
        assertEquals(folder.getCanonicalFile(), file.getParentFile());
        assertTrue(shot.getPath().startsWith("../"), "linked relative to the report: " + shot.getPath());
    }

    @Test
    void videosCanBeAttachedByUrl() {
        adapter.onTestStart(result);
        Instant started = Instant.parse("2026-11-10T10:00:00Z");

        Artifact first = adapter.attachVideo("https://minio.example.test/videos/run-1/login.mp4?X-Amz-Signature=abc");
        Artifact second = adapter.attachVideo("Grid recording", "https://grid.example.test/video/42.webm", started);
        adapter.onTestSuccess(result);

        assertEquals(List.of(first, second), adapter.artifacts());
        assertEquals("login.mp4", first.getName());
        assertEquals("video", first.getType());
        assertEquals("https://minio.example.test/videos/run-1/login.mp4?X-Amz-Signature=abc", first.getPath());
        assertEquals("video/webm", second.getMimeType());
        assertEquals(started, second.getTimestamp());
    }

    @Test
    void recordedVideosGoThroughTheUploader() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(SeleniumVideoRecorder.isFfmpegAvailable("ffmpeg"), "needs ffmpeg");
        System.setProperty("pulsereport.video.mode", "always");
        System.setProperty("pulsereport.video.fps", "5");
        SeleniumAdapter.resetSettings();
        List<File> uploaded = new java.util.concurrent.CopyOnWriteArrayList<>();
        SeleniumAdapter.setVideoUploader((video, testKey) -> {
            assertTrue(video.isFile());
            uploaded.add(video);
            return "https://minio.example.test/videos/" + video.getName();
        });
        org.testng.IClass testClass = mock(org.testng.IClass.class);
        when(testClass.getRealClass()).thenReturn((Class) SeleniumTrackingTest.class);
        when(result.getTestClass()).thenReturn(testClass);
        try {
            adapter.onTestStart(result);
            adapter.track(browser).get("https://example.test/");
            Thread.sleep(700);
            adapter.onTestSuccess(result);

            Artifact video = null;
            for (int i = 0; i < 300 && video == null; i++) {
                video = adapter.artifacts().stream().filter(a -> "video".equals(a.getType())).findFirst().orElse(null);
                Thread.sleep(100);
            }
            assertNotNull(video, "video was encoded and uploaded");
            assertTrue(video.getPath().startsWith("https://minio.example.test/videos/"), video.getPath());
            assertEquals(1, uploaded.size());
            assertFalse(uploaded.get(0).exists(), "the local file is removed after a successful upload");
        } finally {
            System.clearProperty("pulsereport.video.mode");
            System.clearProperty("pulsereport.video.fps");
        }
    }

    @Test
    void failedLookupExplainsTheTestFailure() {
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        NoSuchElementException failure = assertThrows(NoSuchElementException.class,
                () -> driver.findElement(By.id("missing")));
        adapter.onTestFailure(withFailure(result, failure));

        TestStep lookup = adapter.steps().get(adapter.steps().size() - 1);
        assertEquals("Find element By.id: missing", lookup.getName());
        assertEquals(TestStatus.FAILED, lookup.getStatus());
        assertEquals("screenshot", lookup.getArtifacts().get(0).getType());
        assertTrue(adapter.artifacts().stream().anyMatch(a -> "page-info".equals(a.getType())
                && a.getContent().contains("URL: about:blank") && a.getContent().contains("Window: 1280×720")));
        assertTrue(adapter.artifacts().stream().anyMatch(a -> "page-source".equals(a.getType())));
    }

    @Test
    void waitTimeoutIsExplainedByTheLookupItRetried() {
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        TimeoutException failure = assertThrows(TimeoutException.class,
                () -> new WebDriverWait(driver, Duration.ofMillis(50))
                        .until(ExpectedConditions.presenceOfElementLocated(By.id("spinner-done"))));
        adapter.onTestFailure(withFailure(result, failure));

        TestStep wait = adapter.steps().get(adapter.steps().size() - 1);
        assertEquals("Wait for By.id: spinner-done", wait.getName());
        assertTrue(wait.getErrorMessage().contains("waiting for presence"), wait.getErrorMessage());
    }

    @Test
    void aLookupFollowedByASuccessfulActionDoesNotExplainALaterTimeout() {
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        assertThrows(NoSuchElementException.class, () -> driver.findElement(By.id("cookie-banner")));
        driver.get("https://example.test/home");
        TimeoutException failure = assertThrows(TimeoutException.class,
                () -> new WebDriverWait(driver, Duration.ofMillis(50)).until(d -> false));
        adapter.onTestFailure(withFailure(result, failure));

        assertTrue(adapter.steps().stream().noneMatch(s -> s.getName().contains("cookie-banner")),
                adapter.steps().toString());
    }

    @Test
    void everyActionInAStepGetsItsOwnScreenshot() {
        System.setProperty("pulsereport.screenshots.mode", "all");
        SeleniumAdapter.resetSettings();
        browser.element(By.id("first"), "button", "First", "");
        browser.element(By.id("second"), "button", "Second", "");
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        adapter.step("Press both", () -> {
            driver.findElement(By.id("first")).click();
            driver.findElement(By.id("second")).click();
        });
        adapter.onTestSuccess(result);

        TestStep group = adapter.steps().get(0);
        assertEquals(List.of("click-button-first.png", "click-button-second.png"), group.getChildren().stream()
                .flatMap(c -> c.getArtifacts().stream()).map(Artifact::getName).toList());
        assertTrue(group.getArtifacts().isEmpty(), "the actions' screenshots already show the step");
    }

    @Test
    void withAutoStepsOffOnlyManualStepsAreRecordedAndEvidenceStays() {
        System.setProperty("pulsereport.selenium.autoSteps", "false");
        System.setProperty("pulsereport.screenshots.mode", "all");
        SeleniumAdapter.resetSettings();
        browser.element(By.id("login"), "button", "Log in", "");
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        driver.get("https://example.test/login");
        adapter.step("Log in", () -> driver.findElement(By.id("login")).click());
        NoSuchElementException failure = assertThrows(NoSuchElementException.class,
                () -> driver.findElement(By.id("missing")));
        adapter.onTestFailure(withFailure(result, failure));

        assertEquals(1, adapter.steps().size(), adapter.steps().toString());
        TestStep step = adapter.steps().get(0);
        assertEquals("Log in", step.getName());
        assertTrue(step.getChildren().isEmpty(), "actions are not steps of their own");
        assertEquals(List.of("log-in.png"), step.getArtifacts().stream().map(Artifact::getName).toList());
        assertTrue(adapter.metrics().stream().anyMatch(m -> m.getName().equals("page.load.time")));
        assertTrue(adapter.artifacts().stream().anyMatch(a -> "failure.png".equals(a.getName())));
        assertTrue(adapter.artifacts().stream().anyMatch(a -> "page-source".equals(a.getType())));
    }

    @Test
    void appiumAutoStepsAreSwitchedOffApartFromSelenium() {
        System.setProperty("pulsereport.appium.autoSteps", "false");
        SeleniumAdapter.resetSettings();
        browser.element(By.id("login"), "button", "Log in", "");
        adapter.onTestStart(result);
        // As AppiumAdapter.createDriver does: the device's calls go to the listener trackIntercepted returns
        WebDriver device = new EventFiringDecorator<WebDriver>(
                SeleniumTracker.trackIntercepted(browser, "Appium", java.util.Map.of())).decorate(browser);

        device.findElement(By.id("login")).click();
        adapter.step("Log in", () -> { });

        assertEquals(List.of("Log in"), adapter.steps().stream().map(TestStep::getName).toList());
        assertTrue(SeleniumTracker.settings().autoSteps("Selenium"), "Selenium keeps its own setting");
    }

    @Test
    void anInterceptedDriverReportingPulseReportsOwnCallLeavesItsLockAlone() throws Exception {
        adapter.onTestStart(result);
        org.openqa.selenium.support.events.WebDriverListener listener =
                SeleniumTracker.trackIntercepted(browser, "Appium", java.util.Map.of());
        java.util.concurrent.locks.ReentrantLock lock = SeleniumTracker.active().lock();
        java.lang.reflect.Method screenshot = org.openqa.selenium.TakesScreenshot.class
                .getMethod("getScreenshotAs", org.openqa.selenium.OutputType.class);

        // As the video recorder does: hold the lock and take a frame, which the intercepted driver reports
        lock.lock();
        try {
            SeleniumTracker.internal(() -> {
                Object[] args = {org.openqa.selenium.OutputType.BYTES};
                listener.beforeAnyCall(browser, screenshot, args);
                listener.afterAnyCall(browser, screenshot, args, new byte[0]);
                return null;
            });
            assertTrue(lock.isHeldByCurrentThread(), "the recorder still holds its lock");
        } finally {
            lock.unlock();
        }
        assertTrue(adapter.steps().isEmpty(), "PulseReport's own calls are no steps");
    }

    @Test
    void screenshotsAreNamedAfterTheirStep() {
        assertEquals("click-button-log-in.png", SeleniumTracker.shotName("Click Button \"Log in\"", ""));
        assertEquals("type-into-edittext-pin-failure.png",
                SeleniumTracker.shotName("Type \"••••••\" into EditText \"pin\"", "-failure"));
        assertEquals("open-https-www-example-test-a-very-long-path-that-goes-on.png",
                SeleniumTracker.shotName("Open \"https://www.example.test/a/very/long/path/that/goes/on/and/on\"", ""));
        assertEquals("step.png", SeleniumTracker.shotName("••••", ""));
    }

    @Test
    void waitForRecordsAStep() {
        browser.element(By.id("dashboard"), "div", "Dashboard", "");
        adapter.onTestStart(result);
        adapter.track(browser);

        adapter.waitFor("dashboard", Duration.ofSeconds(1),
                ExpectedConditions.presenceOfElementLocated(By.id("dashboard")));
        assertThrows(TimeoutException.class, () -> adapter.waitFor("report", Duration.ofMillis(50),
                ExpectedConditions.presenceOfElementLocated(By.id("report"))));
        adapter.onTestSuccess(result);

        assertEquals("Wait for dashboard", adapter.steps().get(0).getName());
        assertEquals(TestStatus.PASSED, adapter.steps().get(0).getStatus());
        TestStep timedOut = adapter.steps().get(1);
        assertEquals("Wait for report", timedOut.getName());
        assertEquals(TestStatus.FAILED, timedOut.getStatus());
        assertEquals(2, adapter.steps().size(), "the retried lookup adds no step of its own");
    }

    @Test
    void sensitiveInputIsMasked() {
        browser.element(By.id("password"), "input", "Password", "password");
        browser.element(By.id("code"), "input", "", "text").autocomplete = "one-time-code";
        browser.element(By.name("cardNumber"), "input", "", "text").name = "cardNumber";
        browser.element(By.id("user"), "input", "User", "text");
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        driver.findElement(By.id("password")).sendKeys("hunter2");
        driver.findElement(By.id("code")).sendKeys("123456");
        driver.findElement(By.name("cardNumber")).sendKeys("4111111111111111");
        driver.findElement(By.id("user")).sendKeys("demo");
        adapter.onTestSuccess(result);

        List<String> names = adapter.steps().stream().map(TestStep::getName).toList();
        assertEquals("Type \"••••••\" into input \"Password\"", names.get(0));
        assertEquals("Type \"••••••\" into input", names.get(1));
        assertEquals("Type \"••••••\" into input", names.get(2));
        assertEquals("Type \"demo\" into input \"User\"", names.get(3));
        assertTrue(names.stream().noneMatch(n -> n.contains("hunter2") || n.contains("4111")));
    }

    @Test
    void allInputCanBeMasked() {
        System.setProperty("pulsereport.selenium.maskInputs", "all");
        SeleniumAdapter.resetSettings();
        browser.element(By.id("user"), "input", "User", "text");
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        driver.findElement(By.id("user")).sendKeys("demo");
        adapter.onTestSuccess(result);

        assertEquals("Type \"••••••\" into input \"User\"", adapter.steps().get(0).getName());
    }

    @Test
    void secretsInUrlsAreMasked() {
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        driver.get("https://user:pw@example.test/callback?code=abc&state=xyz&access_token=t0k3n");
        adapter.onTestSuccess(result);

        String name = adapter.steps().get(0).getName();
        assertEquals("Open \"https://***@example.test/callback?code=***&state=xyz&access_token=***\"", name);
        assertTrue(adapter.metrics().stream().noneMatch(m -> m.getLabel().contains("t0k3n")));
    }

    @Test
    void minimalDetailNamesElementsByLocatorWithoutExtraBrowserCalls() {
        System.setProperty("pulsereport.selenium.stepDetail", "minimal");
        SeleniumAdapter.resetSettings();
        browser.element(By.id("login"), "button", "Sign in", "submit");
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        driver.findElement(By.id("login")).click();
        adapter.onTestSuccess(result);

        TestStep step = adapter.steps().get(0);
        assertEquals("Click By.id: login", step.getName());
        assertNull(step.getDescription());
    }

    @Test
    void setupActionsBecomeTheTestsSetupStep() {
        IInvokedMethod setUp = configMethod("setUp", true);
        ITestResult configResult = configResult(ITestResult.SUCCESS, null);
        adapter.beforeInvocation(setUp, configResult);
        WebDriver driver = adapter.track(browser);
        driver.get("https://example.test/login");
        adapter.afterInvocation(setUp, configResult);

        adapter.onTestStart(result);
        driver.get("https://example.test/home");
        adapter.onTestSuccess(result);

        TestStep setup = adapter.steps().get(0);
        assertEquals("@BeforeMethod", setup.getKeyword());
        assertEquals("setUp", setup.getName());
        assertEquals("Open \"https://example.test/login\"", setup.getChildren().get(0).getName());
        assertEquals("Open \"https://example.test/home\"", adapter.steps().get(1).getName());
    }

    @Test
    void failedSetupKeepsItsEvidence() {
        browser.element(By.id("login"), "button", "Sign in", "submit").clickFails = true;
        IInvokedMethod setUp = configMethod("logIn", true);
        WebDriver driver = adapter.track(browser);
        RuntimeException failure = new RuntimeException("placeholder");
        ITestResult configResult = configResult(ITestResult.FAILURE, null);
        adapter.beforeInvocation(setUp, configResult);
        try {
            driver.findElement(By.id("login")).click();
        } catch (RuntimeException e) {
            failure = e;
        }
        when(configResult.getThrowable()).thenReturn(failure);
        adapter.afterInvocation(setUp, configResult);

        adapter.onTestStart(result);
        adapter.onTestSkipped(result);

        TestStep setup = adapter.steps().get(0);
        assertEquals(TestStatus.FAILED, setup.getStatus());
        assertEquals("Click button \"Sign in\"", setup.getChildren().get(0).getName());
        assertFalse(setup.getChildren().get(0).getArtifacts().isEmpty(), "the failed click has its screenshot");
        assertTrue(setup.getArtifacts().stream().anyMatch(a -> "page-info".equals(a.getType())));
    }

    @Test
    void teardownActionsAttachToTheTestThatJustFinished() {
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);
        adapter.onTestSuccess(result);

        IInvokedMethod tearDown = configMethod("logOut", false);
        ITestResult configResult = configResult(ITestResult.SUCCESS, null);
        adapter.beforeInvocation(tearDown, configResult);
        driver.get("https://example.test/logout");
        adapter.afterInvocation(tearDown, configResult);

        TestStep teardown = adapter.steps().get(adapter.steps().size() - 1);
        assertEquals("@AfterMethod", teardown.getKeyword());
        assertEquals("Open \"https://example.test/logout\"", teardown.getChildren().get(0).getName());
    }

    @Test
    void logLinesSitAmongTheStepsAndAnErrorFailsItsStepAndTheStepsAroundIt() {
        browser.element(By.id("login"), "button", "Sign in", "submit");
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);

        adapter.log("Opening the login page");
        adapter.step("Log in", () -> {
            adapter.step("Submit", () -> {
                driver.findElement(By.id("login")).click();
                adapter.error("Login service answered 500");
            });
            adapter.warn("Slow response");
        });
        adapter.onTestSuccess(result);

        TestStep info = adapter.steps().get(0);
        assertEquals("Info", info.getKeyword());
        assertEquals("Opening the login page", info.getName());
        assertEquals(TestStatus.PASSED, info.getStatus());
        TestStep login = adapter.steps().get(1);
        assertEquals(TestStatus.FAILED, login.getStatus(), "nothing threw, but an error line fails the steps around it");
        TestStep submit = login.getChildren().get(0);
        assertEquals(TestStatus.FAILED, submit.getStatus());
        assertEquals("Error", submit.getChildren().get(1).getKeyword());
        assertEquals("Login service answered 500", submit.getChildren().get(1).getName());
        assertEquals("Warn", login.getChildren().get(1).getKeyword());
        assertTrue(submit.getArtifacts().stream().anyMatch(a -> "screenshot".equals(a.getType())),
                "the step the error was written in gets a screenshot");
        assertTrue(login.getArtifacts().stream().noneMatch(a -> "screenshot".equals(a.getType())),
                "the steps around it don't take another");
    }

    @Test
    void infoAndWarningLinesLeaveTheStepPassed() {
        adapter.onTestStart(result);

        adapter.step("Check balance", () -> {
            adapter.log("Balance: 120");
            adapter.warn("Balance below 200");
        });
        adapter.onTestSuccess(result);

        TestStep check = adapter.steps().get(0);
        assertEquals(TestStatus.PASSED, check.getStatus());
        assertEquals(List.of("Info", "Warn"), check.getChildren().stream().map(TestStep::getKeyword).toList());
    }

    @Test
    void aStepCanCarryADetailUnderItsName() {
        adapter.onTestStart(result);

        adapter.step("Click Log in", "id: login", () -> { });
        String total = adapter.step("Read total", "id: total", () -> "120");
        adapter.step("Plain", () -> { });
        adapter.onTestSuccess(result);

        assertEquals("120", total);
        assertEquals(Arrays.asList("id: login", "id: total", null),
                adapter.steps().stream().map(TestStep::getDescription).toList());
    }

    @Test
    void anErrorWrittenInTeardownFailsItsStep() {
        adapter.onTestStart(result);
        adapter.onTestSuccess(result);

        IInvokedMethod tearDown = configMethod("cleanUpLoan", false);
        ITestResult configResult = configResult(ITestResult.SUCCESS, null);
        adapter.beforeInvocation(tearDown, configResult);
        adapter.error("Post-test loan cleanup failed: 404");
        adapter.afterInvocation(tearDown, configResult);

        TestStep teardown = adapter.steps().get(adapter.steps().size() - 1);
        assertEquals("@AfterMethod", teardown.getKeyword());
        assertEquals(TestStatus.FAILED, teardown.getStatus());
        assertEquals("Post-test loan cleanup failed: 404", teardown.getChildren().get(0).getName());
    }

    @Test
    void recordingAttachedInTeardownGoesOnTheTest() {
        adapter.onTestStart(result);
        adapter.onTestSuccess(result);

        IInvokedMethod tearDown = configMethod("uploadRecording", false);
        ITestResult configResult = configResult(ITestResult.SUCCESS, null);
        adapter.beforeInvocation(tearDown, configResult);
        adapter.attachVideo("https://minio.example.test/videos/run-1/login.mp4");
        adapter.afterInvocation(tearDown, configResult);

        assertEquals(List.of("video"), adapter.artifacts().stream().map(Artifact::getType).toList());
        assertTrue(adapter.steps().stream().noneMatch(s -> "@AfterMethod".equals(s.getKeyword())),
                "a recording alone makes no teardown step");
    }

    @Test
    void browserIsDetectedFromCapabilities() {
        adapter.onSuiteStart("WebSuite");
        adapter.onTestStart(result);
        adapter.track(browser);
        adapter.onTestSuccess(result);

        assertEquals(List.of("Selenium", "Chrome 131"), adapter.labels());
        TestRun run = adapter.enrich(TestRun.builder().id("r").name("run").startTime(Instant.now())
                .status(TestStatus.PASSED).suites(List.of()).build());
        assertEquals("Chrome", run.getEnvironment().get("browser"));
        assertEquals("131.0.6778.86", run.getEnvironment().get("browserVersion"));
        assertEquals("macOS", run.getEnvironment().get("platform"));
    }

    @Test
    void explicitBrowserMetadataWinsOverDetected() {
        adapter.onSuiteStart("WebSuite");
        adapter.track(browser);
        adapter.recordBrowserMetadata("Chrome", "124", "Windows 11");

        TestRun run = adapter.enrich(TestRun.builder().id("r").name("run").startTime(Instant.now())
                .status(TestStatus.PASSED).suites(List.of()).build());
        assertEquals("124", run.getEnvironment().get("browserVersion"));
        assertEquals("Windows 11", run.getEnvironment().get("platform"));
    }

    @Test
    void lastModeAttachesTheFinalScreenshotToTheLastStep() {
        System.setProperty("pulsereport.screenshots.mode", "last");
        SeleniumAdapter.resetSettings();
        adapter.onTestStart(result);
        WebDriver driver = adapter.track(browser);
        driver.get("https://example.test/a");
        driver.get("https://example.test/b");
        adapter.onTestSuccess(result);

        assertTrue(adapter.steps().get(0).getArtifacts().isEmpty());
        assertEquals("final.png", adapter.steps().get(1).getArtifacts().get(0).getName());
    }

    @Test
    void screenshotAttachesToTheCurrentTest() {
        adapter.onTestStart(result);
        adapter.track(browser);

        Artifact shot = adapter.screenshot("Dashboard");
        adapter.onTestSuccess(result);

        assertNotNull(shot);
        assertEquals("Dashboard.png", shot.getName());
        assertTrue(adapter.artifacts().contains(shot));
    }

    @Test
    void logsAreWrittenToTheReportAndShownInline() throws Exception {
        adapter.onTestStart(result);
        adapter.captureBrowserLogs("browser.log", "[SEVERE] boom");
        adapter.onTestSuccess(result);

        Artifact log = adapter.artifacts().get(0);
        assertEquals("[SEVERE] boom", log.getContent());
        assertTrue(log.getPath().startsWith("artifacts/browser-log/"), log.getPath());
        assertEquals("[SEVERE] boom", Files.readString(new File(reportDir, log.getPath()).toPath()));
    }

    @Test
    void largeHarFilesAreLinkedNotInlined() throws Exception {
        String har = "{\"log\":\"" + "x".repeat(SeleniumAdapter.MAX_INLINE_TEXT_BYTES) + "\"}";

        adapter.onTestStart(result);
        adapter.captureHarFile("network.har", har);
        adapter.onTestSuccess(result);

        Artifact artifact = adapter.artifacts().get(0);
        assertNull(artifact.getContent());
        assertEquals(har.length(), new File(reportDir, artifact.getPath()).length());
    }

    @Test
    void attachmentNamesCannotLeaveTheArtifactsFolder() {
        adapter.onTestStart(result);
        adapter.captureConsoleLogs("../../../escape.log", "text");
        adapter.onTestSuccess(result);

        String path = adapter.artifacts().get(0).getPath();
        assertFalse(path.contains("/../"), path);
        assertTrue(new File(reportDir, path).toPath().normalize().startsWith(reportDir.toPath()));
    }

    private static ITestResult withFailure(ITestResult result, Throwable failure) {
        when(result.getThrowable()).thenReturn(failure);
        return result;
    }

    private static IInvokedMethod configMethod(String name, boolean before) {
        IInvokedMethod invoked = mock(IInvokedMethod.class);
        ITestNGMethod method = mock(ITestNGMethod.class);
        when(invoked.isConfigurationMethod()).thenReturn(true);
        when(invoked.getTestMethod()).thenReturn(method);
        when(method.getMethodName()).thenReturn(name);
        when(method.isBeforeMethodConfiguration()).thenReturn(before);
        when(method.isAfterMethodConfiguration()).thenReturn(!before);
        return invoked;
    }

    private static ITestResult configResult(int status, Throwable failure) {
        ITestResult configResult = mock(ITestResult.class);
        when(configResult.getStatus()).thenReturn(status);
        when(configResult.getThrowable()).thenReturn(failure);
        return configResult;
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
