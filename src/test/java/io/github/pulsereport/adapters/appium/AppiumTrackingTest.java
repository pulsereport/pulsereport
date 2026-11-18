package io.github.pulsereport.adapters.appium;

import com.sun.net.httpserver.HttpServer;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.github.pulsereport.adapters.selenium.SeleniumTracker;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.CommandPayload;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.Response;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Drivers made with {@link AppiumAdapter#createDriver}, against a fake Appium session. */
class AppiumTrackingTest {

    private static final AtomicInteger TEST_IDS = new AtomicInteger();
    // 1×1 PNG
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @TempDir
    File reportDir;

    private RecordingAdapter adapter;
    private ITestResult result;
    private FakeAppiumServer server;

    /** Exposes what the adapter recorded for the test it last started. */
    private static final class RecordingAdapter extends AppiumAdapter {
        String key;

        @Override
        public void onTestStart(ITestResult result) {
            super.onTestStart(result);
            key = getCurrentTestKey();
        }

        List<TestStep> steps() {
            return recordedSteps(key);
        }

        List<String> labels() {
            return recordedLabels(key);
        }

        Map<String, String> metadata() {
            return recordedMetadata(key);
        }
    }

    /**
     * A device session without a server: element commands arrive through
     * {@link #execute(CommandPayload)}, as they do on a real Appium driver.
     */
    public static class FakeDevice extends RemoteWebDriver {
        final Capabilities caps;
        final Map<String, Map<String, String>> elements = new ConcurrentHashMap<>();
        final Map<String, String> ids = new ConcurrentHashMap<>();
        final List<String> commands = new CopyOnWriteArrayList<>();
        final List<String> typed = new CopyOnWriteArrayList<>();
        volatile boolean clicksFail;

        public FakeDevice(Capabilities caps) {
            this.caps = caps;
            setSessionId("fake-session"); // the tracker treats a driver without one as quit
        }

        void element(By by, Map<String, String> attributes) {
            String id = "el-" + ids.size();
            ids.put(by.toString(), id);
            elements.put(id, attributes);
        }

        @Override
        public Capabilities getCapabilities() {
            return caps;
        }

        @Override
        public WebElement findElement(By by) {
            String id = ids.get(by.toString());
            if (id == null) {
                throw new NoSuchElementException("no such element: " + by);
            }
            RemoteWebElement element = new RemoteWebElement();
            element.setParent(this);
            element.setId(id);
            return element;
        }

        @Override
        public void quit() {
            commands.add(DriverCommand.QUIT);
        }

        @Override
        protected Response execute(CommandPayload payload) {
            String name = payload.getName();
            Map<String, ?> params = payload.getParameters();
            commands.add(name);
            Map<String, String> element = elements.getOrDefault(String.valueOf(params.get("id")), Map.of());
            Object value = null;
            switch (name) {
                case DriverCommand.GET_ELEMENT_TAG_NAME:
                    value = element.get("tag");
                    break;
                case DriverCommand.GET_ELEMENT_ATTRIBUTE:
                    value = element.get(String.valueOf(params.get("name")));
                    break;
                case DriverCommand.CLICK_ELEMENT:
                    if (clicksFail) {
                        throw new ElementClickInterceptedException("element click intercepted");
                    }
                    break;
                case DriverCommand.SEND_KEYS_TO_ELEMENT:
                    typed.add(String.join("", (CharSequence[]) params.get("value")));
                    break;
                case DriverCommand.SCREENSHOT:
                    value = PNG;
                    break;
                default:
                    break;
            }
            Response response = new Response();
            response.setValue(value);
            return response;
        }
    }

    /**
     * An Appium server over HTTP with one session ("s1"), answering what a tap on one
     * element needs, so real {@code AndroidDriver}/{@code IOSDriver} instances can talk to it.
     */
    private static final class FakeAppiumServer {
        final HttpServer http;
        final List<String> requests = new CopyOnWriteArrayList<>();

        FakeAppiumServer(Map<String, Object> sessionCaps) throws IOException {
            http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            http.createContext("/", exchange -> {
                exchange.getRequestBody().readAllBytes();
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                requests.add(method + " " + path);
                String value = "null";
                if ("POST".equals(method) && "/session".equals(path)) {
                    value = "{\"sessionId\":\"s1\",\"capabilities\":" + new Json().toJson(sessionCaps) + "}";
                } else if (path.endsWith("/element")) {
                    value = "{\"element-6066-11e4-a52e-4f735466cecf\":\"el-1\"}";
                } else if (path.endsWith("/attribute/class")) {
                    value = "\"android.widget.Button\"";
                } else if (path.endsWith("/attribute/content-desc")) {
                    value = "\"Log in\"";
                } else if (path.endsWith("/screenshot")) {
                    value = "\"" + PNG + "\"";
                }
                byte[] body = ("{\"value\":" + value + "}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            http.start();
        }

        URL url() throws IOException {
            return new URL("http://127.0.0.1:" + http.getAddress().getPort());
        }

        long newSessions() {
            return requests.stream().filter("POST /session"::equals).count();
        }
    }

    private static MutableCapabilities options(String platform, String automation) {
        return new MutableCapabilities(Map.of("platformName", platform, "appium:automationName", automation));
    }

    @BeforeEach
    void setUp() {
        System.setProperty("pulsereport.output.directory", reportDir.getAbsolutePath());
        adapter = new RecordingAdapter();
        result = testResult("deviceTest" + TEST_IDS.incrementAndGet());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("pulsereport.output.directory");
        if (SeleniumTracker.driver() != null) {
            SeleniumTracker.driver().quit();
        }
        if (server != null) {
            server.http.stop(0);
        }
    }

    @Test
    void createdDriverKeepsItsTypeAndRecordsTapsFromAttributes() {
        adapter.onTestStart(result);
        FakeDevice device = adapter.createDriver(FakeDevice.class, platform("Android"));
        device.element(By.id("login"), Map.of("class", "android.widget.Button", "content-desc", "Log in",
                "resource-id", "com.app:id/login"));

        device.findElement(By.id("login")).click();
        adapter.onTestSuccess(result);

        assertInstanceOf(FakeDevice.class, device);
        TestStep tap = adapter.steps().get(adapter.steps().size() - 1);
        assertEquals("Click Button \"Log in\"", tap.getName());
        assertTrue(tap.getDescription().contains("By.id: login"), tap.getDescription());
        assertFalse(device.commands.contains(DriverCommand.EXECUTE_SCRIPT), "native apps run no JavaScript");
    }

    @Test
    void testsUsingTheDriverAreLabelledAppiumWithTheirDevice() {
        adapter.onTestStart(result);
        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("platformName", "ANDROID");
        caps.setCapability("platformVersion", "16");
        caps.setCapability("deviceName", "emulator-5554");
        caps.setCapability("appium:deviceModel", "sdk_gphone64_arm64");
        caps.setCapability("appPackage", "com.app");
        caps.setCapability("automationName", "UiAutomator2");

        adapter.createDriver(FakeDevice.class, caps);

        assertEquals(List.of("Appium"), adapter.labels());
        assertEquals(Map.of("Device", "emulator-5554 (sdk_gphone64_arm64)", "OS", "Android 16", "App", "com.app",
                "Automation", "UiAutomator2"), adapter.metadata());
        assertEquals(List.of("Device", "OS", "App", "Automation"), List.copyOf(adapter.metadata().keySet()));
    }

    @Test
    void aManualStepWithoutATrackedDriverKeepsTheAppiumLabel() {
        adapter.onTestStart(result);

        adapter.step("Tap Login", () -> { });

        assertEquals(List.of("Appium"), adapter.labels());
        assertEquals("Tap Login", adapter.steps().get(0).getName());
    }

    @Test
    void textTypedIntoAnAndroidPasswordFieldIsMasked() {
        adapter.onTestStart(result);
        FakeDevice device = adapter.createDriver(FakeDevice.class, platform("Android"));
        device.element(By.id("pin"), Map.of("class", "android.widget.EditText", "text", "Code", "password", "true"));

        device.findElement(By.id("pin")).sendKeys("4711");
        adapter.onTestSuccess(result);

        assertEquals(List.of("4711"), device.typed);
        assertEquals("Type \"••••••\" into EditText \"Code\"", adapter.steps().get(adapter.steps().size() - 1).getName());
    }

    @Test
    void textTypedIntoAnIosSecureFieldIsMasked() {
        adapter.onTestStart(result);
        FakeDevice device = adapter.createDriver(FakeDevice.class, platform("iOS"));
        device.element(By.id("pin"), Map.of("tag", "XCUIElementTypeSecureTextField", "label", "Code"));

        device.findElement(By.id("pin")).sendKeys("4711");
        adapter.onTestSuccess(result);

        assertEquals("Type \"••••••\" into SecureTextField \"Code\"",
                adapter.steps().get(adapter.steps().size() - 1).getName());
    }

    @Test
    void failedTapIsAFailedStepWithAScreenshot() {
        adapter.onTestStart(result);
        FakeDevice device = adapter.createDriver(FakeDevice.class, platform("Android"));
        device.element(By.id("buy"), Map.of("class", "android.widget.Button", "text", "Buy"));
        device.clicksFail = true;

        WebElement buy = device.findElement(By.id("buy"));
        assertThrows(ElementClickInterceptedException.class, buy::click);

        TestStep tap = adapter.steps().get(adapter.steps().size() - 1);
        assertEquals("Click Button \"Buy\"", tap.getName());
        assertEquals(TestStatus.FAILED, tap.getStatus());
        assertEquals("screenshot", tap.getArtifacts().get(0).getType());
    }

    @Test
    void failedLookupIsAFailedStepWithAScreenshot() {
        adapter.onTestStart(result);
        FakeDevice device = adapter.createDriver(FakeDevice.class, platform("Android"));

        NoSuchElementException failure = assertThrows(NoSuchElementException.class,
                () -> device.findElement(By.id("missing")));
        when(result.getThrowable()).thenReturn(failure);
        adapter.onTestFailure(result);

        TestStep lookup = adapter.steps().get(adapter.steps().size() - 1);
        assertEquals("Find element By.id: missing", lookup.getName());
        assertEquals(TestStatus.FAILED, lookup.getStatus());
        assertEquals("screenshot", lookup.getArtifacts().get(0).getType());
    }

    @Test
    void quittingStopsTracking() {
        adapter.onTestStart(result);
        FakeDevice device = adapter.createDriver(FakeDevice.class, platform("Android"));

        device.quit();

        assertEquals(null, SeleniumTracker.driver());
    }

    @Test
    void anExistingDriverIsTrackedOnItsOwnSessionAndKeepsItsType() throws IOException {
        server = new FakeAppiumServer(Map.of("platformName", "Android", "automationName", "UiAutomator2",
                "deviceName", "emulator-5554", "platformVersion", "16"));
        AndroidDriver original = new AndroidDriver(server.url(), options("Android", "UiAutomator2"));
        adapter.onTestStart(result);

        AndroidDriver driver = adapter.trackDriver(original);
        driver.findElement(By.id("login")).click();
        adapter.onTestSuccess(result);

        assertNotSame(original, driver);
        assertEquals(original.getSessionId(), driver.getSessionId());
        assertEquals(1, server.newSessions(), "tracking must not start a second session");
        assertEquals("emulator-5554", driver.getCapabilities().getCapability("deviceName"),
                "the attached driver answers with the original session's capabilities");
        assertTrue(server.requests.contains("POST /session/s1/element/el-1/click"), server.requests.toString());
        assertEquals("Click Button \"Log in\"", adapter.steps().get(adapter.steps().size() - 1).getName());
        assertTrue(adapter.labels().contains("Appium"), adapter.labels().toString());
        assertTrue(adapter.metadata().containsValue("emulator-5554"), adapter.metadata().toString());

        driver.quit();
        assertTrue(server.requests.contains("DELETE /session/s1"), server.requests.toString());
        assertNull(SeleniumTracker.driver());
    }

    @Test
    void anExistingIosDriverStaysAnIosDriver() throws IOException {
        server = new FakeAppiumServer(Map.of("platformName", "iOS", "automationName", "XCUITest",
                "deviceName", "iPhone 16"));
        IOSDriver original = new IOSDriver(server.url(), options("iOS", "XCUITest"));

        IOSDriver driver = adapter.trackDriver(original);

        assertInstanceOf(IOSDriver.class, driver);
        assertEquals(original.getSessionId(), driver.getSessionId());
        assertEquals(1, server.newSessions());
        assertEquals("iPhone 16", driver.getCapabilities().getCapability("deviceName"));
    }

    @Test
    void onlyLiveAppiumDriversCanBeTracked() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> adapter.trackDriver(new FakeDevice(platform("Android"))));
        assertThrows(IllegalArgumentException.class, () -> adapter.trackDriver(null));

        server = new FakeAppiumServer(Map.of("platformName", "Android"));
        AndroidDriver original = new AndroidDriver(server.url(), options("Android", "UiAutomator2"));
        original.quit();
        assertThrows(IllegalStateException.class, () -> adapter.trackDriver(original));
    }

    @Test
    void theMostSpecificMatchingConstructorIsUsed() throws NoSuchMethodException {
        assertEquals(Overloads.class.getConstructor(String.class),
                AppiumStepReporter.constructorFor(Overloads.class, new Object[] {"text"}));
        assertEquals(Overloads.class.getConstructor(Object.class),
                AppiumStepReporter.constructorFor(Overloads.class, new Object[] {42}));
        assertThrows(IllegalArgumentException.class,
                () -> AppiumStepReporter.constructorFor(Overloads.class, new Object[] {"a", "b"}));
    }

    public static class Overloads {
        public Overloads(Object value) {
        }

        public Overloads(String value) {
        }
    }

    private static Capabilities platform(String name) {
        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("platformName", name);
        return caps;
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
        when(mockMethod.getRealClass()).thenReturn(AppiumTrackingTest.class);
        when(mockContext.getName()).thenReturn("MobileContext");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockSuite.getName()).thenReturn("MobileSuite");
        return mockResult;
    }
}
