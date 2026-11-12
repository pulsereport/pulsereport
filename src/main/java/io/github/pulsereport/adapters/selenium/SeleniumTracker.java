package io.github.pulsereport.adapters.selenium;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.pulsereport.adapters.CaptureBridge;
import io.github.pulsereport.assertions.AssertionCollector;
import io.github.pulsereport.config.ReporterConfig;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.AssertionResult;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;

/**
 * Records what a Selenium test does in the browser, for whichever PulseReport
 * adapter runs the test: TestNG (through {@link SeleniumAdapter}) or Cucumber
 * (tracked actions become sub-steps of the running Gherkin step).
 *
 * <pre>{@code
 * WebDriver driver = SeleniumTracker.track(new ChromeDriver());
 *
 * driver.get("https://example.com/login");                 // step: Open "https://example.com/login"
 * SeleniumTracker.step("Log in", () -> {                     // one step with the actions as sub-steps
 *     driver.findElement(By.id("user")).sendKeys("demo");
 *     driver.findElement(By.id("password")).sendKeys(pwd);   // typed text is masked
 *     driver.findElement(By.id("login")).click();
 * });
 * SeleniumTracker.waitFor("dashboard", Duration.ofSeconds(10),
 *         ExpectedConditions.visibilityOfElementLocated(By.id("dashboard")));
 * SeleniumTracker.screenshot("Dashboard");
 * }</pre>
 *
 * <p>
 * Recorded automatically: clicks, typing, clearing, submitting, navigation,
 * frame/window/alert switches, {@code Actions} gestures (hover, drag,
 * double/right click, key presses, scrolling), closing windows, page load
 * times (as {@code page.load.time} metrics labelled with the URL), and, when a
 * test or step fails, the lookup or wait that failed, a screenshot, the page
 * URL and title, browser console errors and the page source. The browser,
 * version and platform are read from the driver's capabilities.</p>
 *
 * <p>
 * Settings ({@code pulsereport.properties} or {@code -D}):</p>
 * <pre>
 * pulsereport.screenshots.mode=failed               # all | failed | last | none
 * pulsereport.selenium.stepDetail=full              # full | minimal (no extra browser calls per action)
 * pulsereport.selenium.maskInputs=sensitive         # sensitive | all | none
 * pulsereport.selenium.failureCapture=page,console,source   # or none
 * pulsereport.selenium.pageMetrics=true
 * pulsereport.selenium.trackScripts=false           # record executeScript calls as steps
 * </pre>
 *
 * @since 1.3.0
 */
public final class SeleniumTracker {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumTracker.class);

    static final String ADAPTER_NAME = "Selenium";
    static final int MAX_INLINE_TEXT_BYTES = 1024 * 1024;

    static final String MODE_ALL = "all";
    static final String MODE_FAILED = "failed";
    static final String MODE_LAST = "last";
    static final String MODE_NONE = "none";

    static final String TYPED_MASK = "••••••";
    static final String URL_MASK = "***";

    private static final int MAX_EVIDENCED = 8;
    private static final int MAX_CAUSE_DEPTH = 20;

    private static final String PAGE_INFO_SCRIPT =
            "return [location.href, document.title, window.innerWidth, window.innerHeight];";

    private static final Set<String> SENSITIVE_AUTOCOMPLETE = Set.of("one-time-code", "cc-number", "cc-csc",
            "cc-exp", "cc-exp-month", "cc-exp-year", "current-password", "new-password");
    private static final List<String> DEFAULT_SENSITIVE_FIELDS = List.of("pass", "password", "passwd", "passcode",
            "pwd", "secret", "token", "otp", "one_time", "pin", "cvv", "cvc", "csc", "ssn", "card_number",
            "card_num", "card_no", "cc_number", "cc_num", "iban", "api_key", "auth");
    private static final Set<String> DEFAULT_SENSITIVE_PARAMS = Set.of("password", "passwd", "pwd", "secret",
            "token", "access_token", "refresh_token", "id_token", "client_secret", "api_key", "apikey", "key",
            "code", "auth", "authorization", "session", "sessionid", "sid", "sig", "signature", "otp");
    private static final Pattern URL_USERINFO = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*://)[^/@?#]+@");
    private static final Pattern URL_PARAM = Pattern.compile("([?&#;])([^=&#;?]+)=([^&#;]*)");

    /** A step body with no return value. */
    @FunctionalInterface
    public interface StepAction {
        /** @throws Exception anything the body throws; it is recorded and rethrown */
        void run() throws Exception;
    }

    /**
     * A step body that returns a value (e.g. a located element).
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface StepSupplier<T> {
        /**
         * @return the step's result
         * @throws Exception anything the body throws; it is recorded and rethrown
         */
        T get() throws Exception;
    }

    /** Callbacks for the adapter that owns the test lifecycle (video, end-of-test screenshots). */
    interface Hooks {
        void onTrack(WebDriver driver);

        void beforeQuit(WebDriver driver);

        /** @return a screenshot taken before the browser went away, or null */
        Artifact fallbackScreenshot();
    }

    /** The browser a driver controls, read from its capabilities. */
    record BrowserInfo(String name, String version, String platform) {
        /** e.g. "Chrome 131" */
        String label() {
            if (version == null || version.isBlank()) {
                return name;
            }
            int dot = version.indexOf('.');
            return name + " " + (dot > 0 ? version.substring(0, dot) : version);
        }
    }

    /** A browser to take screenshots and run scripts on, with the lock that serializes it. */
    record Camera(WebDriver driver, ReentrantLock lock) {
    }

    /** The driver tracked on a thread. */
    record Tracked(WebDriver raw, WebDriver decorated, ReentrantLock lock, BrowserInfo browser) {
        Camera camera() {
            return new Camera(raw, lock);
        }
    }

    record Settings(String screenshots, boolean detailed, String maskInputs, Set<String> failureCapture,
            boolean trackScripts, boolean pageMetrics, Pattern sensitiveField, Set<String> sensitiveParams,
            boolean embedScreenshots, String screenshotDirectory) {

        static Settings load() {
            ReporterConfig config = null;
            try {
                config = ReporterConfig.autoDetect();
            } catch (RuntimeException e) {
                logger.warn("PulseReport: could not read pulsereport.properties; using default Selenium settings", e);
            }
            if (config == null) {
                config = ReporterConfig.builder().build();
            }
            String screenshots = option("pulsereport.screenshots.mode",
                    config.getProperty("pulsereport.screenshots.mode", config.getScreenshotMode()), MODE_FAILED,
                    List.of(MODE_ALL, MODE_FAILED, MODE_LAST, MODE_NONE));
            String detail = option("pulsereport.selenium.stepDetail",
                    config.getProperty("pulsereport.selenium.stepDetail", "full"), "full", List.of("full", "minimal"));
            // pulsereport.maskSensitiveData is the master switch for every kind of masking
            String mask = config.isMaskSensitiveData()
                    ? option("pulsereport.selenium.maskInputs", config.getProperty("pulsereport.selenium.maskInputs", "sensitive"),
                            "sensitive", List.of("sensitive", "all", "none"))
                    : "none";
            Set<String> failureCapture = new LinkedHashSet<>();
            for (String item : config.getProperty("pulsereport.selenium.failureCapture", "page,console,source").split(",")) {
                String value = item.trim().toLowerCase(Locale.ROOT);
                if (List.of("page", "console", "source").contains(value)) {
                    failureCapture.add(value);
                } else if (!value.isEmpty() && !MODE_NONE.equals(value)) {
                    logger.warn("PulseReport: ignoring unknown pulsereport.selenium.failureCapture item '{}'"
                            + " (expected page, console, source or none)", value);
                }
            }
            List<String> configured = config.getSensitiveBodyFields() == null ? List.of()
                    : Arrays.stream(config.getSensitiveBodyFields().split(",")).map(String::trim)
                            .filter(s -> !s.isEmpty()).collect(Collectors.toList());
            Set<String> fields = new LinkedHashSet<>(DEFAULT_SENSITIVE_FIELDS);
            Set<String> params = new LinkedHashSet<>(DEFAULT_SENSITIVE_PARAMS);
            for (String name : configured) {
                fields.add(normalizeName(name));
                params.add(name.toLowerCase(Locale.ROOT));
            }
            Pattern fieldPattern = Pattern.compile("(?:^|_)(?:" + fields.stream().map(Pattern::quote)
                    .collect(Collectors.joining("|")) + ")(?:_|$)");
            String storage = option("pulsereport.screenshots.storage",
                    config.getProperty("pulsereport.screenshots.storage", "embed"), "embed", List.of("embed", "path"));
            return new Settings(screenshots, "full".equals(detail), mask, Collections.unmodifiableSet(failureCapture),
                    Boolean.parseBoolean(config.getProperty("pulsereport.selenium.trackScripts", "false")),
                    Boolean.parseBoolean(config.getProperty("pulsereport.selenium.pageMetrics", "true")),
                    fieldPattern, Collections.unmodifiableSet(params),
                    "embed".equals(storage), config.getProperty("pulsereport.screenshots.directory", null));
        }

        private static String option(String key, String value, String fallback, List<String> allowed) {
            String normalized = value == null ? fallback : value.trim().toLowerCase(Locale.ROOT);
            if (allowed.contains(normalized)) {
                return normalized;
            }
            logger.warn("PulseReport: invalid {}='{}'; expected one of {}. Using '{}'", key, value, allowed, fallback);
            return fallback;
        }
    }

    /** A named step being recorded; tracked actions inside it become its children, its assertions its own. */
    private static final class StepScope {
        final List<TestStep> children = new ArrayList<>();
        final List<Artifact> artifacts = new ArrayList<>();
        /** Collects the step's assertions, nested in the test's or Gherkin step's collector; null when none is open. */
        final Object assertions = AssertionCollector.isActive() ? AssertionCollector.open() : null;
    }

    private record FailedLookup(String locator, Throwable error, Instant startedAt, long durationMs) {
    }

    private static final ThreadLocal<Tracked> tracked = new ThreadLocal<>();
    private static final ThreadLocal<Deque<StepScope>> openSteps = new ThreadLocal<>();
    private static final ThreadLocal<FailedLookup> failedLookup = new ThreadLocal<>();
    // Failures a screenshot was already taken for, so outer steps and the test don't take another
    private static final ThreadLocal<Deque<Throwable>> evidenced = new ThreadLocal<>();
    private static final ThreadLocal<Throwable> lastHandledFailure = new ThreadLocal<>();
    private static final Map<String, BrowserInfo> detectedBrowsers = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final AtomicLong artifactSequence = new AtomicLong();
    private static volatile Settings settings;
    private static volatile Hooks hooks;

    static {
        CaptureBridge.addFailureListener(SeleniumTracker::onFailure);
    }

    private SeleniumTracker() {
    }

    // ------------------------------------------------------------------ public API

    /**
     * Wraps a driver so the actions performed through it are recorded as report
     * steps. Use the returned driver in place of the original. Tracking the
     * driver this method returned again returns it unchanged.
     *
     * @param driver the driver to track
     * @return a decorated driver that records steps
     */
    public static WebDriver track(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        Tracked current = tracked.get();
        if (current != null && (current.decorated() == driver || current.raw() == driver)) {
            return current.decorated();
        }
        ReentrantLock lock = new ReentrantLock();
        BrowserInfo browser = detectBrowser(driver);
        WebDriver decorated = new EventFiringDecorator<WebDriver>(new SeleniumStepListener(driver, lock))
                .decorate(driver);
        Tracked t = new Tracked(driver, decorated, lock, browser);
        tracked.set(t);
        CaptureBridge.Sink sink = CaptureBridge.current();
        if (sink != null) {
            label(sink, t);
        }
        Hooks h = hooks;
        if (h != null) {
            h.onTrack(driver);
        }
        return decorated;
    }

    /**
     * @return the tracked (decorated) driver on this thread, or null when none
     * is tracked or its session has ended
     */
    public static WebDriver driver() {
        Tracked t = active();
        return t == null ? null : t.decorated();
    }

    /**
     * Runs {@code action} as a step named after the tracked actions it performs.
     *
     * @param action the step body
     */
    public static void step(StepAction action) {
        step(null, action);
    }

    /**
     * Runs {@code action} as a named step. Tracked actions inside it become its
     * sub-steps. Exceptions mark the step FAILED, attach a screenshot, and are
     * rethrown.
     *
     * @param message the step name shown in the report
     * @param action the step body
     */
    public static void step(String message, StepAction action) {
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
    public static <T> T step(String message, StepSupplier<T> action) {
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        Deque<StepScope> scopes = openSteps.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            openSteps.set(scopes);
        }
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
            if (scopes.isEmpty()) {
                openSteps.remove();
            }
            try {
                finishStep(message, scope, failure, startedAt, elapsedMs(start));
            } catch (RuntimeException e) {
                logger.debug("PulseReport: could not record step '{}'", message, e);
            }
        }
    }

    /**
     * Waits for {@code condition} on the tracked driver and records the wait as a
     * step. On timeout the step fails with a screenshot and the exception is
     * rethrown.
     *
     * @param description what is awaited, e.g. "dashboard to load"
     * @param timeout how long to wait
     * @param condition e.g. {@code ExpectedConditions.visibilityOfElementLocated(...)}
     * @param <T> the condition's result type
     * @return the condition's result
     * @throws IllegalStateException when no driver is tracked on this thread
     */
    public static <T> T waitFor(String description, Duration timeout, Function<? super WebDriver, T> condition) {
        if (condition == null || timeout == null) {
            throw new IllegalArgumentException("condition and timeout cannot be null");
        }
        Tracked t = active();
        if (t == null) {
            throw new IllegalStateException("No tracked driver on this thread; call track(driver) first");
        }
        String name = "Wait for " + (description == null || description.isBlank() ? "condition" : description);
        Instant startedAt = Instant.now();
        long start = System.nanoTime();
        try {
            T result = new WebDriverWait(t.decorated(), timeout).until(condition);
            recordAction(name, null, null, startedAt, elapsedMs(start), t.camera());
            return result;
        } catch (RuntimeException e) {
            // This step explains the timeout; the wait's last failed lookup needs no step of its own
            consumeFailedLookup(e);
            recordAction(name, "timed out after " + formatDuration(timeout.toMillis()), e, startedAt,
                    elapsedMs(start), t.camera());
            throw e;
        }
    }

    /**
     * Takes a screenshot of the tracked browser and attaches it to the innermost
     * open step, or to the running test.
     *
     * @param name the screenshot name shown in the report (".png" is added)
     * @return the attached screenshot, or null when there is no tracked browser
     * or the screenshot failed
     */
    public static Artifact screenshot(String name) {
        Tracked t = active();
        if (t == null) {
            logger.warn("PulseReport: screenshot '{}' skipped: no tracked driver on this thread", name);
            return null;
        }
        String fileName = name == null || name.isBlank() ? "screenshot" : name.trim();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".png")) {
            fileName += ".png";
        }
        Artifact shot = screenshot(t.camera(), fileName);
        if (shot != null) {
            attach(shot, t);
        }
        return shot;
    }

    /**
     * Attaches a recording stored elsewhere (e.g. a MinIO, S3 or Selenium Grid
     * video URL) to the running test. Call it once per video; the report plays
     * each one.
     *
     * @param name the name shown under the player
     * @param url where the video is served (http(s), or a path relative to the report)
     * @param startedAt when the recording started, so steps get "Play from here"
     * buttons at the right moment; null when unknown (no buttons)
     * @return the attached video, or null when no test is running on this thread
     */
    public static Artifact attachVideo(String name, String url, Instant startedAt) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url cannot be blank");
        }
        String fileName = name == null || name.isBlank() ? url.replaceAll("[?#].*$", "").replaceAll("^.*/", "") : name;
        String lower = url.toLowerCase(Locale.ROOT).replaceAll("[?#].*$", "");
        Artifact video = Artifact.builder()
                .name(fileName.isBlank() ? "recording" : fileName)
                .type("video")
                .mimeType(lower.endsWith(".webm") ? "video/webm" : "video/mp4")
                .path(url.trim())
                // Unknown start: the attach time, normally after the steps, so none claims a moment in it
                .timestamp(startedAt != null ? startedAt : Instant.now())
                .build();
        CaptureBridge.Sink sink = CaptureBridge.current();
        if (sink == null) {
            logger.warn("PulseReport: video '{}' not attached: no test is running on this thread", url);
            return null;
        }
        label(sink, active());
        sink.addArtifact(video);
        return video;
    }

    /**
     * Attaches the tracked browser's current page source to the innermost open
     * step, or to the running test.
     *
     * @return the attached page source, or null when there is no tracked browser
     */
    public static Artifact capturePageSource() {
        Tracked t = active();
        if (t == null) {
            logger.warn("PulseReport: page source skipped: no tracked driver on this thread");
            return null;
        }
        String source = pageSource(t.camera());
        if (source == null) {
            return null;
        }
        Artifact artifact = textArtifact("page-source.html", "page-source", "text/html", source);
        attach(artifact, t);
        return artifact;
    }

    // ------------------------------------------------------------------ adapter integration

    static void setHooks(Hooks newHooks) {
        hooks = newHooks;
    }

    static Settings settings() {
        Settings s = settings;
        if (s == null) {
            s = Settings.load();
            settings = s;
        }
        return s;
    }

    /** Re-reads settings on next use (tests change system properties between cases). */
    static void resetSettings() {
        settings = null;
    }

    /** The tracked driver, or null once its session has ended (including a quit on the unwrapped driver). */
    static Tracked active() {
        Tracked t = tracked.get();
        if (t != null && t.raw() instanceof RemoteWebDriver && ((RemoteWebDriver) t.raw()).getSessionId() == null) {
            tracked.remove();
            return null;
        }
        return t;
    }

    static void untrack(WebDriver raw) {
        Tracked t = tracked.get();
        if (t != null && t.raw() == raw) {
            tracked.remove();
        }
    }

    static void beforeQuit(WebDriver raw) {
        Tracked t = tracked.get();
        Hooks h = hooks;
        if (h != null && t != null && t.raw() == raw) {
            h.beforeQuit(raw);
        }
    }

    /** Clears per-thread failure bookkeeping at the start of a test. */
    static void resetThreadState() {
        failedLookup.remove();
        evidenced.remove();
        lastHandledFailure.remove();
    }

    static Map<String, BrowserInfo> detectedBrowsers() {
        synchronized (detectedBrowsers) {
            return new LinkedHashMap<>(detectedBrowsers);
        }
    }

    static void clearDetectedBrowsers() {
        detectedBrowsers.clear();
    }

    /** Marks the sink's test as a Selenium test and tags it with the browser. */
    static void label(CaptureBridge.Sink sink, Tracked t) {
        sink.markAdapter(ADAPTER_NAME);
        if (t != null && t.browser() != null) {
            sink.addTag(t.browser().label());
        }
    }

    // ------------------------------------------------------------------ recording

    /** Records one tracked browser action (called by the step listener). */
    static void recordAction(String name, String detail, Throwable failure, Instant startedAt, long durationMs,
            Camera camera) {
        TestStep.Builder step = TestStep.builder()
                .name(name)
                .description(detail)
                .status(failure == null ? TestStatus.PASSED : TestStatus.FAILED)
                .startTime(startedAt)
                .endTime(startedAt.plusMillis(durationMs))
                .duration(durationMs);
        List<Artifact> artifacts = new ArrayList<>();
        if (failure != null) {
            withError(step, failure);
            if (screenshotsEnabled()) {
                Artifact shot = screenshot(camera, "step-failure.png");
                if (shot != null) {
                    artifacts.add(shot);
                    markEvidenced(failure);
                }
            }
        }
        StepScope scope = peekScope();
        if (scope != null) {
            scope.children.add(step.artifacts(artifacts).build());
            return;
        }
        emit(step, artifacts, failure == null, camera);
    }

    /** Remembers a lookup that failed, to explain the test failure it may cause. */
    static void rememberFailedLookup(String locator, Throwable error, Instant startedAt, long durationMs) {
        failedLookup.set(new FailedLookup(locator, error, startedAt, durationMs));
    }

    /** Records page load timing for a navigation that loaded a new document. */
    static void recordPageLoad(String url, long loadMs, long ttfbMs) {
        if (!settings().pageMetrics() || loadMs < 0) {
            return;
        }
        CaptureBridge.Sink sink = CaptureBridge.current();
        if (sink == null) {
            return;
        }
        Instant now = Instant.now();
        sink.addMetric(Metric.builder().name("page.load.time").value(loadMs).unit("ms").label(url).timestamp(now).build());
        if (ttfbMs >= 0) {
            sink.addMetric(Metric.builder().name("page.ttfb").value(ttfbMs).unit("ms").label(url).timestamp(now).build());
        }
    }

    /** Attaches an artifact to the innermost open step, else to the running test. */
    static boolean attach(Artifact artifact, Tracked t) {
        StepScope scope = peekScope();
        if (scope != null) {
            scope.artifacts.add(artifact);
            return true;
        }
        CaptureBridge.Sink sink = CaptureBridge.current();
        if (sink == null) {
            logger.debug("PulseReport: no test is running on this thread; '{}' not attached", artifact.getName());
            return false;
        }
        label(sink, t);
        sink.addArtifact(artifact);
        return true;
    }

    private static void finishStep(String message, StepScope scope, Throwable failure, Instant startedAt,
            long durationMs) {
        List<AssertionResult> checks = scope.assertions == null ? List.of() : AssertionCollector.close(scope.assertions);
        Tracked t = active();
        Camera camera = t == null ? null : t.camera();
        if (failure != null) {
            TestStep lookup = failedLookupStep(failure, camera);
            if (lookup != null) {
                scope.children.add(lookup);
            }
        }
        boolean named = message != null && !message.isBlank();
        TestStep only = scope.children.size() == 1 ? scope.children.get(0) : null;
        TestStep.Builder step;
        List<Artifact> artifacts;
        if (!named && only != null && scope.artifacts.isEmpty() && checks.isEmpty()
                && (failure == null || only.getStatus() == TestStatus.FAILED)) {
            // A single unnamed action needs no wrapper step
            step = only.toBuilder();
            artifacts = new ArrayList<>(only.getArtifacts());
        } else {
            String name = named ? message
                    : scope.children.isEmpty() ? "Step"
                    : scope.children.get(0).getName()
                            + (scope.children.size() > 1 ? " (+" + (scope.children.size() - 1) + " more)" : "");
            step = TestStep.builder()
                    .name(name)
                    .status(failure == null ? TestStatus.PASSED : TestStatus.FAILED)
                    .startTime(startedAt)
                    .endTime(startedAt.plusMillis(durationMs))
                    .duration(durationMs)
                    .children(new ArrayList<>(scope.children));
            artifacts = new ArrayList<>(scope.artifacts);
            if (failure != null) {
                withError(step, failure);
            }
            step.assertions(checks);
        }
        if (failure != null && !isEvidenced(failure) && screenshotsEnabled()) {
            Artifact shot = screenshot(camera, "step-failure.png");
            if (shot != null) {
                artifacts.add(shot);
                markEvidenced(failure);
            }
        }
        StepScope parent = peekScope();
        if (parent != null) {
            parent.children.add(step.artifacts(artifacts).build());
            return;
        }
        emit(step, artifacts, failure == null, camera);
    }

    /** Sends a finished top-level step to the running test. */
    private static void emit(TestStep.Builder step, List<Artifact> artifacts, boolean passed, Camera camera) {
        CaptureBridge.Sink sink = CaptureBridge.current();
        if (sink == null) {
            logger.debug("PulseReport: no test is running on this thread; step not recorded");
            return;
        }
        if (passed && MODE_ALL.equals(settings().screenshots()) && artifacts.stream().noneMatch(SeleniumTracker::isImage)) {
            Artifact shot = screenshot(camera, "step.png");
            if (shot != null) {
                artifacts.add(shot);
            }
        }
        label(sink, active());
        sink.addStep(step.artifacts(artifacts).build());
    }

    // ------------------------------------------------------------------ failures

    /** Adds browser evidence to a test or step that failed (registered with {@link CaptureBridge}). */
    static void onFailure(Throwable failure, CaptureBridge.Sink sink) {
        if (inChain(failure, lastHandledFailure.get())) {
            return; // the same failure reported again by an outer runner
        }
        Tracked t = active();
        Camera camera = t == null ? null : t.camera();
        TestStep lookup = failedLookupStep(failure, camera);
        if (t == null && lookup == null) {
            // Not a browser test, or the browser is gone: only a screenshot taken before it went can help
            Hooks h = hooks;
            Artifact shot = h != null && !isEvidenced(failure) ? h.fallbackScreenshot() : null;
            if (shot != null) {
                lastHandledFailure.set(failure);
                markEvidenced(failure);
                label(sink, null);
                sink.addArtifact(shot);
            }
            return;
        }
        lastHandledFailure.set(failure);
        List<Artifact> shots = new ArrayList<>();
        if (lookup == null && !isEvidenced(failure) && screenshotsEnabled()) {
            Artifact shot = screenshot(camera, "failure.png");
            Hooks h = hooks;
            if (shot == null && h != null) {
                shot = h.fallbackScreenshot();
            }
            if (shot != null) {
                shots.add(shot);
                markEvidenced(failure);
            }
        }
        label(sink, t);
        if (lookup != null) {
            sink.addStep(lookup);
        }
        shots.forEach(sink::addArtifact);
        if (t != null) {
            captureFailureContext(t, sink);
        }
    }

    /** The failed lookup behind {@code failure}, as a failed step with a screenshot; null if none caused it. */
    private static TestStep failedLookupStep(Throwable failure, Camera camera) {
        FailedLookup lookup = consumeFailedLookup(failure);
        if (lookup == null) {
            return null;
        }
        Throwable timeout = firstInChain(failure, TimeoutException.class);
        TestStep.Builder step = TestStep.builder()
                .name((timeout != null ? "Wait for " : "Find element ") + lookup.locator())
                .status(TestStatus.FAILED)
                .startTime(lookup.startedAt())
                .endTime(lookup.startedAt().plusMillis(lookup.durationMs()))
                .duration(lookup.durationMs());
        Throwable explained = timeout != null ? timeout : lookup.error();
        step.errorMessage(messageOf(explained));
        List<Artifact> artifacts = new ArrayList<>();
        if (!isEvidenced(failure) && screenshotsEnabled()) {
            Artifact shot = screenshot(camera, "failure.png");
            Hooks h = hooks;
            if (shot == null && h != null) {
                shot = h.fallbackScreenshot();
            }
            if (shot != null) {
                artifacts.add(shot);
                markEvidenced(failure);
            }
        }
        return step.artifacts(artifacts).build();
    }

    private static FailedLookup consumeFailedLookup(Throwable failure) {
        FailedLookup lookup = failedLookup.get();
        if (lookup == null || !inChain(failure, lookup.error())) {
            return null;
        }
        failedLookup.remove();
        return lookup;
    }

    /** Page URL/title, console log and page source for a failure, as configured. */
    private static void captureFailureContext(Tracked t, CaptureBridge.Sink sink) {
        Set<String> wanted = settings().failureCapture();
        if (wanted.isEmpty()) {
            return;
        }
        Camera camera = t.camera();
        List<String> info = new ArrayList<>();
        if (wanted.contains("page")) {
            List<?> page = script(camera, PAGE_INFO_SCRIPT);
            if (page.size() == 4) {
                info.add("URL: " + maskUrl(String.valueOf(page.get(0))));
                info.add("Title: " + page.get(1));
                info.add("Window: " + number(page.get(2)) + "×" + number(page.get(3)));
            }
        }
        Artifact console = null;
        if (wanted.contains("console")) {
            List<LogEntry> entries = browserLog(camera);
            if (!entries.isEmpty()) {
                long errors = entries.stream().filter(e -> "SEVERE".equals(e.getLevel().getName())).count();
                String text = entries.stream()
                        .map(e -> Instant.ofEpochMilli(e.getTimestamp()) + " [" + e.getLevel().getName() + "] "
                                + e.getMessage())
                        .collect(Collectors.joining("\n"));
                console = textArtifact("console.log", "console-log", "text/plain", text);
                if (wanted.contains("page")) {
                    info.add("Console errors: " + errors);
                }
            }
        }
        if (!info.isEmpty()) {
            sink.addArtifact(textArtifact("page-info.txt", "page-info", "text/plain", String.join("\n", info)));
        }
        if (console != null) {
            sink.addArtifact(console);
        }
        if (wanted.contains("source")) {
            String source = pageSource(camera);
            if (source != null && !source.isEmpty()) {
                sink.addArtifact(textArtifact("page-source.html", "page-source", "text/html", source));
            }
        }
    }

    private static List<LogEntry> browserLog(Camera camera) {
        lock(camera);
        try {
            return camera.driver().manage().logs().get(LogType.BROWSER).getAll();
        } catch (RuntimeException | LinkageError e) {
            // Not every browser exposes its console through WebDriver (e.g. Firefox)
            logger.debug("PulseReport: browser console log not available", e);
            return List.of();
        } finally {
            unlock(camera);
        }
    }

    private static String pageSource(Camera camera) {
        lock(camera);
        try {
            return camera.driver().getPageSource();
        } catch (RuntimeException e) {
            logger.debug("PulseReport: page source not available", e);
            return null;
        } finally {
            unlock(camera);
        }
    }

    private static void markEvidenced(Throwable failure) {
        Deque<Throwable> seen = evidenced.get();
        if (seen == null) {
            seen = new ArrayDeque<>();
            evidenced.set(seen);
        }
        seen.push(failure);
        while (seen.size() > MAX_EVIDENCED) {
            seen.removeLast();
        }
    }

    private static boolean isEvidenced(Throwable failure) {
        Deque<Throwable> seen = evidenced.get();
        if (seen == null) {
            return false;
        }
        for (Throwable t : seen) {
            if (inChain(failure, t)) {
                return true;
            }
        }
        return false;
    }

    static boolean inChain(Throwable failure, Throwable target) {
        if (target == null) {
            return false;
        }
        Throwable c = failure;
        for (int depth = 0; c != null && depth < MAX_CAUSE_DEPTH; depth++, c = c.getCause()) {
            if (c == target) {
                return true;
            }
        }
        return false;
    }

    private static Throwable firstInChain(Throwable failure, Class<? extends Throwable> type) {
        Throwable c = failure;
        for (int depth = 0; c != null && depth < MAX_CAUSE_DEPTH; depth++, c = c.getCause()) {
            if (type.isInstance(c)) {
                return c;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ browser + files

    static Artifact screenshot(Camera camera, String name) {
        if (camera == null || !(camera.driver() instanceof TakesScreenshot)) {
            return null;
        }
        byte[] png;
        lock(camera);
        try {
            png = ((TakesScreenshot) camera.driver()).getScreenshotAs(OutputType.BYTES);
        } catch (RuntimeException e) {
            logger.debug("PulseReport: could not capture screenshot", e);
            return null;
        } finally {
            unlock(camera);
        }
        return storeScreenshot(name, png);
    }

    /**
     * Keeps a screenshot as configured: embedded in the report as Base64
     * ({@code pulsereport.screenshots.storage=embed}, the default) or written to
     * {@code pulsereport.screenshots.directory} and shown from there ({@code path}).
     */
    static Artifact storeScreenshot(String name, byte[] png) {
        Settings s = settings();
        String safeName = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!s.embedScreenshots()) {
            File file = new File(directory(s.screenshotDirectory(), "artifacts/screenshots"),
                    String.format(Locale.ROOT, "%05d", artifactSequence.incrementAndGet()) + "-" + safeName);
            try {
                Files.createDirectories(file.getParentFile().toPath());
                Files.write(file.toPath(), png);
                return Artifact.builder().name(name).type("screenshot").mimeType("image/png")
                        .path(reportPath(file)).size((long) png.length).timestamp(Instant.now()).build();
            } catch (IOException e) {
                logger.warn("PulseReport: could not write {}: {}; embedding it instead", file, e.getMessage());
            }
        }
        return Artifact.builder().name(name).type("screenshot").mimeType("image/png").path(safeName)
                .size((long) png.length).timestamp(Instant.now())
                .content(Base64.getEncoder().encodeToString(png)).build();
    }

    /**
     * A configured folder: absolute as given, relative to the working directory
     * otherwise; {@code fallback} under the report folder when not configured.
     */
    static File directory(String configured, String fallback) {
        if (configured == null || configured.isBlank()) {
            return new File(reportDirectory(), fallback);
        }
        File dir = new File(configured.trim());
        return dir.isAbsolute() ? dir : new File(System.getProperty("user.dir", "."), configured.trim());
    }

    /**
     * How the report refers to a file: relative to the report folder, so the
     * report and its folders can be moved together; a file: URI when no relative
     * path exists (e.g. another drive).
     */
    static String reportPath(File file) {
        java.nio.file.Path target = file.toPath().toAbsolutePath().normalize();
        try {
            java.nio.file.Path base = reportDirectory().toPath().toAbsolutePath().normalize();
            return base.relativize(target).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return target.toUri().toString();
        }
    }

    /**
     * Writes text evidence under {@code <report dir>/artifacts/<type>/} and inlines
     * it when small enough, so the report shows it without the file and links it otherwise.
     */
    static Artifact textArtifact(String fileName, String type, String mimeType, String content) {
        return storeFile(type, fileName, type, mimeType, content.getBytes(StandardCharsets.UTF_8), true);
    }

    /**
     * Writes a file into the report folder under a unique name and returns an
     * artifact with a path relative to the report, so the report folder can be
     * moved or published as a whole.
     */
    static Artifact storeFile(String folder, String fileName, String type, String mimeType, byte[] bytes,
            boolean inlineText) {
        // The sequence keeps same-named files apart; the name can't climb out of the folder
        String relativePath = "artifacts/" + folder + "/"
                + String.format(Locale.ROOT, "%05d", artifactSequence.incrementAndGet()) + "-"
                + fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        boolean written = false;
        try {
            File file = new File(reportDirectory(), relativePath);
            Files.createDirectories(file.getParentFile().toPath());
            Files.write(file.toPath(), bytes);
            written = true;
        } catch (IOException e) {
            logger.warn("PulseReport: could not write {}: {}", relativePath, e.getMessage());
        }
        Artifact.Builder artifact = Artifact.builder()
                .name(fileName)
                .type(type)
                .path(relativePath)
                .mimeType(mimeType)
                .size((long) bytes.length)
                .timestamp(Instant.now());
        if (inlineText && bytes.length <= MAX_INLINE_TEXT_BYTES) {
            artifact.content(new String(bytes, StandardCharsets.UTF_8));
        } else if (!written && !inlineText) {
            // Keep the evidence in the report itself when the report folder isn't writable
            artifact.content(Base64.getEncoder().encodeToString(bytes));
        }
        return artifact.build();
    }

    static File reportDirectory() {
        String dir = ReporterConfig.resolveOutputDirectory("pulsereport.output.directory", "target/pulsereport");
        File file = new File(dir);
        return file.isAbsolute() ? file : new File(System.getProperty("user.dir", "."), dir);
    }

    static List<?> script(Camera camera, String script, Object... args) {
        if (camera == null || !(camera.driver() instanceof JavascriptExecutor)) {
            return List.of();
        }
        lock(camera);
        try {
            Object result = ((JavascriptExecutor) camera.driver()).executeScript(script, args);
            return result instanceof List ? (List<?>) result : List.of();
        } catch (RuntimeException e) {
            return List.of();
        } finally {
            unlock(camera);
        }
    }

    private static void lock(Camera camera) {
        if (camera != null && camera.lock() != null) {
            camera.lock().lock();
        }
    }

    private static void unlock(Camera camera) {
        if (camera != null && camera.lock() != null && camera.lock().isHeldByCurrentThread()) {
            camera.lock().unlock();
        }
    }

    private static BrowserInfo detectBrowser(WebDriver driver) {
        if (!(driver instanceof HasCapabilities)) {
            return null;
        }
        try {
            Capabilities caps = ((HasCapabilities) driver).getCapabilities();
            String name = caps.getBrowserName();
            if (name == null || name.isBlank()) {
                return null;
            }
            Platform platform = caps.getPlatformName();
            BrowserInfo info = new BrowserInfo(browserName(name), caps.getBrowserVersion(), platformName(platform));
            detectedBrowsers.putIfAbsent(info.label(), info);
            return info;
        } catch (RuntimeException e) {
            logger.debug("PulseReport: could not read browser capabilities", e);
            return null;
        }
    }

    private static String browserName(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "chrome":
            case "chrome-headless-shell":
                return "Chrome";
            case "msedge":
            case "microsoftedge":
                return "Edge";
            case "firefox":
                return "Firefox";
            case "safari":
                return "Safari";
            default:
                return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }

    private static String platformName(Platform platform) {
        if (platform == null || platform == Platform.ANY) {
            return null;
        }
        Platform family = platform.family() != null ? platform.family() : platform;
        switch (family) {
            case WINDOWS:
                return "Windows";
            case MAC:
                return "macOS";
            case LINUX:
                return "Linux";
            default:
                String lower = family.name().toLowerCase(Locale.ROOT);
                return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
    }

    // ------------------------------------------------------------------ masking

    /**
     * Whether typed text should be hidden: password and one-time-code style
     * fields, fields whose name, id or label looks sensitive, or everything when
     * {@code pulsereport.selenium.maskInputs=all}.
     */
    static boolean isSensitiveField(String type, String autocomplete, String... names) {
        Settings s = settings();
        if ("all".equals(s.maskInputs())) {
            return true;
        }
        if ("none".equals(s.maskInputs())) {
            return false;
        }
        if ("password".equalsIgnoreCase(type)
                || (autocomplete != null && SENSITIVE_AUTOCOMPLETE.contains(autocomplete.trim().toLowerCase(Locale.ROOT)))) {
            return true;
        }
        for (String name : names) {
            if (name != null && !name.isBlank() && s.sensitiveField().matcher(normalizeName(name)).find()) {
                return true;
            }
        }
        return false;
    }

    /** Hides credentials and sensitive query/fragment parameter values in a URL. */
    static String maskUrl(String url) {
        if (url == null || "none".equals(settings().maskInputs())) {
            return url;
        }
        String masked = URL_USERINFO.matcher(url).replaceFirst("$1" + URL_MASK + "@");
        Matcher m = URL_PARAM.matcher(masked);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String replacement = isSensitiveParam(m.group(2))
                    ? m.group(1) + m.group(2) + "=" + URL_MASK
                    : m.group();
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static boolean isSensitiveParam(String rawName) {
        String name;
        try {
            name = URLDecoder.decode(rawName, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            name = rawName.toLowerCase(Locale.ROOT);
        }
        return settings().sensitiveParams().contains(name) || name.contains("token") || name.contains("secret")
                || name.contains("password") || name.contains("passwd") || name.contains("apikey")
                || name.contains("api_key") || name.contains("signature");
    }

    /** "loginPassword" / "login-password" / "Login password" → "login_password". */
    static String normalizeName(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replaceAll("[^A-Za-z0-9]+", "_")
                .toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ helpers

    private static boolean screenshotsEnabled() {
        return !MODE_NONE.equals(settings().screenshots());
    }

    private static boolean isImage(Artifact a) {
        return "screenshot".equals(a.getType()) || (a.getMimeType() != null && a.getMimeType().startsWith("image/"));
    }

    private static StepScope peekScope() {
        Deque<StepScope> scopes = openSteps.get();
        return scopes == null ? null : scopes.peek();
    }

    private static void withError(TestStep.Builder step, Throwable failure) {
        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));
        step.errorMessage(messageOf(failure)).stackTrace(trace.toString());
    }

    static String messageOf(Throwable failure) {
        return failure.getMessage() != null ? failure.getMessage() : failure.getClass().getName();
    }

    private static String number(Object value) {
        return value instanceof Number ? String.valueOf(((Number) value).longValue()) : String.valueOf(value);
    }

    static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
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

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }
}
