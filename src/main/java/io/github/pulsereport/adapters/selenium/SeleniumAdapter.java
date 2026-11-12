package io.github.pulsereport.adapters.selenium;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ITestResult;

import io.github.pulsereport.adapters.CaptureBridge;
import io.github.pulsereport.adapters.testng.TestNGAdapter;
import io.github.pulsereport.config.ReporterConfig;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.TestRun;

/**
 * TestNG listener for Selenium WebDriver tests: records browser actions as
 * report steps, takes screenshots and video, and adds browser evidence to
 * failed tests.
 *
 * <h2>Setup</h2>
 * <pre>{@code
 * @Listeners(SeleniumAdapter.class)
 * public class LoginTest {
 *     private final SeleniumAdapter adapter = new SeleniumAdapter();
 *     private WebDriver driver;
 *
 *     @BeforeMethod
 *     public void setUp() {
 *         driver = adapter.track(new ChromeDriver());   // every click/type/open becomes a step
 *         driver.get("https://example.com/login");      // recorded in the test's "@BeforeMethod" step
 *     }
 *
 *     @Test
 *     public void login() {
 *         adapter.step("Log in", () -> {                // one step, the actions as its sub-steps
 *             driver.findElement(By.id("user")).sendKeys("demo");
 *             driver.findElement(By.id("password")).sendKeys(secret);   // shown as ••••••
 *             driver.findElement(By.id("login")).click();
 *         });
 *         adapter.waitFor("dashboard", Duration.ofSeconds(10),
 *                 ExpectedConditions.visibilityOfElementLocated(By.id("dashboard")));
 *         adapter.screenshot("Dashboard");
 *     }
 *
 *     @AfterMethod(alwaysRun = true)
 *     public void tearDown() {
 *         driver.quit();
 *     }
 * }
 * }</pre>
 *
 * <h2>What is recorded</h2>
 * <ul>
 * <li>Clicks, typing (masked for password, one-time-code, card and other
 * sensitive fields), clearing, submitting, navigation, frame/window/alert
 * switches, {@code Actions} gestures and closed windows, each with its
 * element, locator, URL change and duration</li>
 * <li>Actions in {@code @BeforeMethod}/{@code @AfterMethod} (and class/suite
 * configuration methods), as steps of the test named after the method, with
 * the annotation ("@BeforeMethod") as their keyword</li>
 * <li>Page load time of each navigation, as {@code page.load.time} and
 * {@code page.ttfb} metrics labelled with the URL</li>
 * <li>On failure: the element lookup or wait that failed, a screenshot, the
 * page URL and title, browser console errors and the page source</li>
 * <li>The browser, version and platform (from the driver's capabilities), as
 * run metadata and as a tag on each test</li>
 * <li>Optional video of each test (needs ffmpeg), encoded in the background
 * after the test</li>
 * </ul>
 * <p>
 * Screenshots, logs and videos are written under the report folder
 * ({@code pulsereport.output.directory}), so copy or publish that folder as a
 * whole. The same tracking works without TestNG through
 * {@link SeleniumTracker}, e.g. in Cucumber step definitions.</p>
 *
 * <h2>Settings</h2>
 * <pre>
 * pulsereport.screenshots.mode=failed               # all | failed | last | none
 * pulsereport.video.mode=none                       # always | failed | none (requires ffmpeg)
 * pulsereport.video.fps=2
 * pulsereport.video.ffmpegPath=ffmpeg
 * pulsereport.video.storage=path                    # path | embed
 * pulsereport.selenium.stepDetail=full              # full | minimal
 * pulsereport.selenium.maskInputs=sensitive         # sensitive | all | none
 * pulsereport.selenium.failureCapture=page,console,source
 * pulsereport.selenium.pageMetrics=true
 * pulsereport.selenium.trackScripts=false
 * </pre>
 * <p>
 * Set them in {@code pulsereport.properties} or as {@code -D} system properties.</p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * Tracked drivers, open steps and captures are per thread, and the stores are
 * shared by all adapter instances, so parallel tests and adapter instances
 * created in test code work without extra plumbing.</p>
 *
 * @author Pulse Report Team
 * @since 1.0.0
 * @see SeleniumTracker
 * @see TestNGAdapter
 */
public class SeleniumAdapter extends TestNGAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumAdapter.class);

    static final long MAX_EMBEDDED_VIDEO_BYTES = 50L * 1024 * 1024;
    static final long MAX_EMBEDDED_VIDEO_TOTAL_BYTES = 200L * 1024 * 1024;
    static final int MAX_INLINE_TEXT_BYTES = SeleniumTracker.MAX_INLINE_TEXT_BYTES;
    private static final long VIDEO_WAIT_SECONDS = 240;

    private static final String VIDEO_ALWAYS = "always";

    private static final Map<String, String> runBrowserMetadata = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final ThreadLocal<TestCapture> testCapture = new ThreadLocal<>();
    private static final List<PendingVideo> pendingVideos = new CopyOnWriteArrayList<>();
    private static final AtomicLong embedBudget = new AtomicLong(MAX_EMBEDDED_VIDEO_TOTAL_BYTES);
    private static volatile VideoSettings videoSettings;
    private static volatile ExecutorService encoder;
    private static volatile VideoUploader videoUploader;

    static {
        SeleniumTracker.setHooks(new AdapterHooks());
    }

    /** A step body with no return value. */
    @FunctionalInterface
    public interface StepAction extends SeleniumTracker.StepAction {
    }

    /**
     * A step body that returns a value (e.g. a located element).
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface StepSupplier<T> extends SeleniumTracker.StepSupplier<T> {
    }

    /**
     * Uploads a recorded video somewhere the report can play it from (MinIO,
     * S3, a CDN, ...) and returns its URL. Register one with
     * {@link #setVideoUploader(VideoUploader)}.
     */
    @FunctionalInterface
    public interface VideoUploader {
        /**
         * @param video the encoded MP4, deleted after a successful upload
         * @param testKey the test's unique key, e.g. for building an object key
         * @return the URL the report should play the video from; null or blank to keep the local file
         * @throws Exception on failure; the local file is kept and linked instead
         */
        String upload(File video, String testKey) throws Exception;
    }

    /** Per-test capture state: the video recorder and a screenshot taken before a mid-test quit. */
    private static final class TestCapture {
        SeleniumVideoRecorder recorder;
        Artifact screenshotBeforeQuit;
    }

    private record PendingVideo(String testKey, Future<?> encoding) {
    }

    record VideoSettings(String mode, int fps, String ffmpegPath, String storage, String directory) {

        static VideoSettings load() {
            ReporterConfig config = null;
            try {
                config = ReporterConfig.autoDetect();
            } catch (RuntimeException e) {
                logger.warn("PulseReport: could not read pulsereport.properties; using default video settings", e);
            }
            if (config == null) {
                config = ReporterConfig.builder().build();
            }
            String mode = config.getProperty("pulsereport.video.mode", config.getVideoMode());
            mode = mode == null ? SeleniumTracker.MODE_NONE : mode.trim().toLowerCase(Locale.ROOT);
            if (!List.of(VIDEO_ALWAYS, SeleniumTracker.MODE_FAILED, SeleniumTracker.MODE_NONE).contains(mode)) {
                logger.warn("PulseReport: invalid pulsereport.video.mode='{}'; expected always, failed or none."
                        + " Using 'none'", mode);
                mode = SeleniumTracker.MODE_NONE;
            }
            int fps = config.getVideoFps();
            String fpsOverride = System.getProperty("pulsereport.video.fps");
            if (fpsOverride != null) {
                try {
                    fps = Integer.parseInt(fpsOverride.trim());
                } catch (NumberFormatException e) {
                    logger.warn("PulseReport: invalid pulsereport.video.fps='{}'; using {}", fpsOverride, fps);
                }
            }
            String storage = config.getProperty("pulsereport.video.storage", config.getVideoStorage());
            storage = storage == null ? "path" : storage.trim().toLowerCase(Locale.ROOT);
            if (!List.of("path", "embed", "url").contains(storage)) {
                logger.warn("PulseReport: invalid pulsereport.video.storage='{}'; expected path or embed. Using 'path'",
                        storage);
                storage = "path";
            }
            return new VideoSettings(mode, Math.min(30, Math.max(1, fps)),
                    config.getProperty("pulsereport.video.ffmpegPath", config.getFfmpegPath()), storage,
                    config.getProperty("pulsereport.video.directory", null));
        }
    }

    /**
     * Constructs a new SeleniumAdapter. Test code may create its own instances;
     * they share all state with the TestNG-registered listener.
     */
    public SeleniumAdapter() {
        super();
        logger.debug("SeleniumAdapter initialized");
    }

    // ------------------------------------------------------------------ test code API

    /**
     * Wraps a driver so the actions performed through it are recorded as steps
     * with their element, locator, URL change, page load time and outcome. Use
     * the returned driver in place of the original. The browser, version and
     * platform are read from the driver's capabilities.
     *
     * @param driver the driver to track
     * @return a decorated driver that records steps
     */
    public WebDriver track(WebDriver driver) {
        return SeleniumTracker.track(driver);
    }

    /**
     * Runs {@code action} as a step named after the tracked actions it performs.
     *
     * @param action the step body
     */
    public void step(StepAction action) {
        SeleniumTracker.step(action);
    }

    /**
     * Runs {@code action} as a named step. Tracked actions inside it become its
     * sub-steps. Exceptions mark the step FAILED with a screenshot and are
     * rethrown.
     *
     * @param message the step name shown in the report
     * @param action the step body
     */
    public void step(String message, StepAction action) {
        SeleniumTracker.step(message, action);
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
        return SeleniumTracker.step(message, action);
    }

    /**
     * Waits for {@code condition} on the tracked driver and records the wait as
     * a step; a timeout fails the step with a screenshot and is rethrown.
     *
     * @param description what is awaited, e.g. "dashboard to load"
     * @param timeout how long to wait
     * @param condition e.g. {@code ExpectedConditions.visibilityOfElementLocated(...)}
     * @param <T> the condition's result type
     * @return the condition's result
     */
    public <T> T waitFor(String description, Duration timeout, Function<? super WebDriver, T> condition) {
        return SeleniumTracker.waitFor(description, timeout, condition);
    }

    /**
     * Takes a screenshot of the tracked browser and attaches it to the current
     * step or test.
     *
     * @param name the name shown in the report
     * @return the screenshot, or null when no driver is tracked on this thread
     */
    public Artifact screenshot(String name) {
        return SeleniumTracker.screenshot(name);
    }

    /**
     * Sends every video this adapter records to {@code uploader} after it is
     * encoded, and links the URL it returns in the report instead of a local
     * file. Pass null to stop uploading.
     *
     * <pre>{@code
     * SeleniumAdapter.setVideoUploader((video, testKey) -> {
     *     String key = "runs/" + runId + "/" + video.getName();
     *     minio.uploadObject(UploadObjectArgs.builder().bucket("videos").object(key)
     *             .filename(video.getAbsolutePath()).contentType("video/mp4").build());
     *     return "https://minio.example.com/videos/" + key;
     * });
     * }</pre>
     *
     * @param uploader the uploader, or null
     */
    public static void setVideoUploader(VideoUploader uploader) {
        videoUploader = uploader;
    }

    /**
     * Attaches a recording stored elsewhere (e.g. a MinIO, S3 or Selenium Grid
     * video URL) to the running test. Call it once per video.
     *
     * @param url where the video is served
     * @return the attached video, or null when no test is running on this thread
     */
    public Artifact attachVideo(String url) {
        return SeleniumTracker.attachVideo(null, url, null);
    }

    /**
     * Attaches a recording stored elsewhere to the running test.
     *
     * @param name the name shown under the player
     * @param url where the video is served
     * @param startedAt when the recording started, so steps line up with it; null when unknown
     * @return the attached video, or null when no test is running on this thread
     */
    public Artifact attachVideo(String name, String url, Instant startedAt) {
        return SeleniumTracker.attachVideo(name, url, startedAt);
    }

    /**
     * Attaches the tracked browser's page source to the current step or test.
     *
     * @return the page source artifact, or null when no driver is tracked
     */
    public Artifact capturePageSource() {
        return SeleniumTracker.capturePageSource();
    }

    /**
     * Attaches browser-level logs (network errors, security warnings, ...) to
     * the current step or test. The log is written to the report folder and
     * shown inline when 1 MB or smaller.
     *
     * @param logFileName the file name, e.g. "browser.log"
     * @param logContent the log text
     */
    public void captureBrowserLogs(String logFileName, String logContent) {
        attachText(logFileName, logContent, "browser-log", "text/plain");
    }

    /**
     * Attaches JavaScript console output to the current step or test.
     *
     * @param logFileName the file name, e.g. "console.log"
     * @param logContent the console text
     */
    public void captureConsoleLogs(String logFileName, String logContent) {
        attachText(logFileName, logContent, "console-log", "text/plain");
    }

    /**
     * Attaches a HAR (HTTP Archive) network capture to the current step or
     * test; the report lists its requests.
     *
     * @param harFileName the file name, e.g. "network.har"
     * @param harContent the HAR JSON
     */
    public void captureHarFile(String harFileName, String harContent) {
        attachText(harFileName, harContent, "har", "application/json");
    }

    /**
     * Records run-level browser metadata for the report header. Usually not
     * needed: {@link #track} reads it from the driver's capabilities. Values
     * given here win over detected ones.
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
                browserName, browserVersion, platform);
    }

    // ------------------------------------------------------------------ TestNG lifecycle

    @Override
    public void onSuiteStart(String suiteName) {
        runBrowserMetadata.clear();
        SeleniumTracker.clearDetectedBrowsers();
        super.onSuiteStart(suiteName);
    }

    @Override
    public void onTestStart(ITestResult result) {
        super.onTestStart(result);
        SeleniumTracker.resetThreadState();
        // A test whose finish event never arrived must not leave its recorder running
        releaseCapture(testCapture.get());
        testCapture.set(new TestCapture());
        startVideo();
        SeleniumTracker.Tracked t = SeleniumTracker.active();
        CaptureBridge.Sink sink = CaptureBridge.current();
        if (t != null && sink != null) {
            SeleniumTracker.label(sink, t); // a driver created in @BeforeClass carries over
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String testKey = getCurrentTestKey();
        TestCapture capture = testCapture.get();
        if (capture != null && testKey != null && SeleniumTracker.MODE_LAST.equals(SeleniumTracker.settings().screenshots())) {
            attachFinalScreenshot(testKey, capture);
        }
        super.onTestSuccess(result);
        finishTestCapture(capture, result, testKey, false);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String testKey = getCurrentTestKey();
        TestCapture capture = testCapture.get();
        super.onTestFailure(result); // adds the browser evidence while the test is still open
        finishTestCapture(capture, result, testKey, true);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String testKey = getCurrentTestKey();
        TestCapture capture = testCapture.get();
        super.onTestSkipped(result);
        finishTestCapture(capture, result, testKey, false);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        String testKey = getCurrentTestKey();
        TestCapture capture = testCapture.get();
        super.onTestFailedButWithinSuccessPercentage(result);
        finishTestCapture(capture, result, testKey, true);
    }

    /** Waits for this suite's videos to finish encoding, then builds the report. */
    @Override
    public void onFinish(ISuite suite) {
        awaitVideos(suite.getName() + ".");
        super.onFinish(suite);
    }

    @Override
    protected TestRun enrichTestRun(TestRun builtTestRun) {
        Map<String, SeleniumTracker.BrowserInfo> detected = SeleniumTracker.detectedBrowsers();
        if (builtTestRun == null || (runBrowserMetadata.isEmpty() && detected.isEmpty())) {
            return builtTestRun;
        }
        Map<String, String> environment = new LinkedHashMap<>();
        if (builtTestRun.getEnvironment() != null) {
            environment.putAll(builtTestRun.getEnvironment());
        }
        if (detected.size() == 1) {
            SeleniumTracker.BrowserInfo only = detected.values().iterator().next();
            putIfPresent(environment, "browser", only.name());
            putIfPresent(environment, "browserVersion", only.version());
            putIfPresent(environment, "platform", only.platform());
        } else if (detected.size() > 1) {
            environment.put("browser", String.join(", ", detected.keySet()));
            Set<String> platforms = detected.values().stream().map(SeleniumTracker.BrowserInfo::platform)
                    .filter(p -> p != null).collect(Collectors.toCollection(LinkedHashSet::new));
            if (!platforms.isEmpty()) {
                environment.put("platform", String.join(", ", platforms));
            }
        }
        synchronized (runBrowserMetadata) {
            environment.putAll(runBrowserMetadata);
        }
        return builtTestRun.toBuilder().environment(environment).build();
    }

    // ------------------------------------------------------------------ screenshots + video

    private static void attachFinalScreenshot(String testKey, TestCapture capture) {
        SeleniumTracker.Tracked t = SeleniumTracker.active();
        Artifact shot = t != null ? SeleniumTracker.screenshot(t.camera(), "final.png") : capture.screenshotBeforeQuit;
        capture.screenshotBeforeQuit = null;
        if (shot != null && !attachToLastStep(testKey, shot)) {
            addArtifactToKey(testKey, shot);
        }
    }

    private static VideoSettings videoSettings() {
        VideoSettings settings = videoSettings;
        if (settings == null) {
            settings = VideoSettings.load();
            videoSettings = settings;
        }
        return settings;
    }

    /** Re-reads settings on next use (tests change system properties between cases). */
    static void resetSettings() {
        videoSettings = null;
        SeleniumTracker.resetSettings();
    }

    private static void startVideo() {
        TestCapture capture = testCapture.get();
        SeleniumTracker.Tracked t = SeleniumTracker.active();
        VideoSettings settings = videoSettings();
        if (capture == null || capture.recorder != null || t == null || !(t.raw() instanceof TakesScreenshot)
                || SeleniumTracker.MODE_NONE.equals(settings.mode())
                || !SeleniumVideoRecorder.isFfmpegAvailable(settings.ffmpegPath())) {
            return;
        }
        try {
            capture.recorder = new SeleniumVideoRecorder((TakesScreenshot) t.raw(), settings.fps(), t.lock());
        } catch (IOException e) {
            logger.warn("PulseReport: could not start video recording: {}", e.getMessage());
        }
    }

    private void finishTestCapture(TestCapture capture, ITestResult result, String testKey, boolean failed) {
        testCapture.remove();
        if (capture == null || capture.recorder == null) {
            return;
        }
        SeleniumVideoRecorder recorder = capture.recorder;
        recorder.stop();
        if (testKey == null || (SeleniumTracker.MODE_FAILED.equals(videoSettings().mode()) && !failed)) {
            recorder.discard();
            return;
        }
        String fileName = videoFileName(result, testKey);
        try {
            pendingVideos.add(new PendingVideo(testKey, encoder().submit(() -> encodeVideo(testKey, fileName, recorder))));
        } catch (RuntimeException e) {
            logger.warn("PulseReport: could not queue video for '{}': {}", testKey, e.getMessage());
            recorder.discard();
        }
    }

    /** Runs on the encoder pool, so the next test (and the browser's quit) doesn't wait for ffmpeg. */
    private static void encodeVideo(String testKey, String fileName, SeleniumVideoRecorder recorder) {
        VideoSettings settings = videoSettings();
        try {
            File output = new File(SeleniumTracker.directory(settings.directory(), "videos"), fileName);
            if (!recorder.encode(output, settings.ffmpegPath(), settings.fps())) {
                return;
            }
            long start = recorder.videoStartMillis();
            long size = output.length();
            Artifact.Builder video = Artifact.builder()
                    .name(fileName)
                    .type("video")
                    .mimeType("video/mp4")
                    .path(SeleniumTracker.reportPath(output))
                    .size(size)
                    // The first frame's time, so the report can line steps up with the video
                    .timestamp(start > 0 ? Instant.ofEpochMilli(start) : Instant.now());
            String url = upload(output, testKey);
            if (url != null) {
                addArtifactToKey(testKey, video.path(url).build());
                return;
            }
            if ("url".equals(settings.storage())) {
                logger.warn("PulseReport: pulsereport.video.storage=url but no uploader is set "
                        + "(SeleniumAdapter.setVideoUploader); {} is linked from its folder", fileName);
            }
            if ("embed".equals(settings.storage())) {
                if (size > MAX_EMBEDDED_VIDEO_BYTES) {
                    logger.warn("PulseReport: {} is over the {} MB embed limit; the report links the file instead",
                            fileName, MAX_EMBEDDED_VIDEO_BYTES >> 20);
                } else if (!reserveEmbedBudget(size)) {
                    logger.warn("PulseReport: embedded videos reached {} MB in total; the report links {} instead",
                            MAX_EMBEDDED_VIDEO_TOTAL_BYTES >> 20, fileName);
                } else {
                    video.content(Base64.getEncoder().encodeToString(Files.readAllBytes(output.toPath())));
                }
            }
            addArtifactToKey(testKey, video.build());
        } catch (IOException | RuntimeException e) {
            logger.warn("PulseReport: could not attach video for '{}': {}", testKey, e.getMessage());
        } finally {
            recorder.discard();
        }
    }

    /** @return the uploaded video's URL, or null when there is no uploader or the upload failed */
    private static String upload(File video, String testKey) {
        VideoUploader uploader = videoUploader;
        if (uploader == null) {
            return null;
        }
        try {
            String url = uploader.upload(video, testKey);
            if (url == null || url.isBlank()) {
                return null;
            }
            Files.deleteIfExists(video.toPath());
            return url.trim();
        } catch (Exception e) {
            logger.warn("PulseReport: video upload failed for '{}'; linking the local file instead: {}",
                    testKey, e.toString());
            return null;
        }
    }

    private static boolean reserveEmbedBudget(long size) {
        while (true) {
            long left = embedBudget.get();
            if (size > left) {
                return false;
            }
            if (embedBudget.compareAndSet(left, left - size)) {
                return true;
            }
        }
    }

    private static void awaitVideos(String suitePrefix) {
        for (PendingVideo video : pendingVideos) {
            if (!video.testKey().startsWith(suitePrefix)) {
                continue;
            }
            try {
                video.encoding().get(VIDEO_WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | java.util.concurrent.TimeoutException e) {
                logger.warn("PulseReport: video for '{}' was not ready in time: {}", video.testKey(), e.toString());
            } finally {
                pendingVideos.remove(video);
            }
        }
    }

    private static ExecutorService encoder() {
        ExecutorService pool = encoder;
        if (pool == null) {
            synchronized (SeleniumAdapter.class) {
                pool = encoder;
                if (pool == null) {
                    int threads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
                    AtomicInteger n = new AtomicInteger();
                    pool = Executors.newFixedThreadPool(threads, r -> {
                        Thread thread = new Thread(r, "pulsereport-video-encoder-" + n.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    });
                    encoder = pool;
                }
            }
        }
        return pool;
    }

    private static void releaseCapture(TestCapture capture) {
        if (capture != null && capture.recorder != null) {
            capture.recorder.stop();
            capture.recorder.discard();
        }
    }

    /** e.g. LoginTest.testValidLogin-1a2b3c.mp4; the hash keeps parameterized invocations apart. */
    private static String videoFileName(ITestResult result, String testKey) {
        String readable = result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
        return readable.replaceAll("[^A-Za-z0-9._-]", "_") + "-" + Integer.toHexString(testKey.hashCode()) + ".mp4";
    }

    /** Lifecycle callbacks from {@link SeleniumTracker}. */
    private static final class AdapterHooks implements SeleniumTracker.Hooks {

        @Override
        public void onTrack(WebDriver driver) {
            startVideo();
        }

        /** Captures end-of-test evidence while the browser is still open, for drivers quit mid-test. */
        @Override
        public void beforeQuit(WebDriver driver) {
            TestCapture capture = testCapture.get();
            if (capture == null) {
                return; // quit in @AfterMethod: the test already finished
            }
            SeleniumTracker.Tracked t = SeleniumTracker.active();
            // In "last" mode the end-of-test screenshot is due; take it while the browser still exists
            if (t != null && SeleniumTracker.MODE_LAST.equals(SeleniumTracker.settings().screenshots())) {
                capture.screenshotBeforeQuit = SeleniumTracker.screenshot(t.camera(), "final.png");
            }
            if (capture.recorder != null) {
                capture.recorder.stop();
            }
        }

        @Override
        public Artifact fallbackScreenshot() {
            TestCapture capture = testCapture.get();
            if (capture == null) {
                return null;
            }
            Artifact shot = capture.screenshotBeforeQuit;
            capture.screenshotBeforeQuit = null;
            return shot;
        }
    }

    // ------------------------------------------------------------------ helpers

    private void attachText(String fileName, String content, String type, String mimeType) {
        Artifact artifact = validatedText(fileName, content, type, mimeType);
        if (!SeleniumTracker.attach(artifact, SeleniumTracker.active())) {
            logger.warn("PulseReport: '{}' not attached: no test is running on this thread", fileName);
        }
    }

    private Artifact validatedText(String fileName, String content, String type, String mimeType) {
        validateParameter(fileName, "fileName");
        if (content == null) {
            throw new IllegalArgumentException("content cannot be null");
        }
        return SeleniumTracker.textArtifact(fileName, type, mimeType, content);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private void validateParameter(String parameter, String parameterName) {
        if (parameter == null) {
            throw new IllegalArgumentException(parameterName + " cannot be null");
        }
        if (parameter.trim().isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be empty");
        }
    }

    private void putRunBrowserMetadata(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            runBrowserMetadata.remove(key);
            return;
        }
        runBrowserMetadata.put(key, value.trim());
    }
}
