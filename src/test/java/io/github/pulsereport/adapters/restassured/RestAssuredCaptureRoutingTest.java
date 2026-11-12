package io.github.pulsereport.adapters.restassured;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testng.IInvokedMethod;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import io.github.pulsereport.adapters.testng.TestNGAdapter;
import io.github.pulsereport.config.ReporterConfig;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestStep;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/** REST Assured captures reach the running TestNG test, setup or teardown through CaptureBridge. */
class RestAssuredCaptureRoutingTest {

    private static final AtomicInteger TEST_IDS = new AtomicInteger();

    @TempDir
    File reportDir;

    private RecordingAdapter testng;
    private RestAssuredAdapter rest;
    private ITestResult result;

    /** Exposes what the TestNG adapter recorded for the test it last started. */
    private static final class RecordingAdapter extends TestNGAdapter {
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

        List<io.github.pulsereport.core.model.AssertionResult> assertions() {
            return recordedAssertions(key);
        }
    }

    @BeforeEach
    void setUp() {
        System.setProperty("pulsereport.output.directory", reportDir.getAbsolutePath());
        testng = new RecordingAdapter();
        rest = new RestAssuredAdapter(ReporterConfig.builder().outputDirectory(reportDir).build());
        result = testResult("apiTest" + TEST_IDS.incrementAndGet());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("pulsereport.output.directory");
        RestAssuredAdapter.clearCurrentTestName();
        RestAssuredAdapter.clearTestData("standalone");
    }

    @Test
    void requestsInBeforeMethodBecomeStepsOfTheBeforeMethodStep() {
        IInvokedMethod setUp = configMethod("createUser", true);
        ITestResult configResult = configResult();
        testng.beforeInvocation(setUp, configResult);
        call("POST", "https://api.example.test/users", 201);
        testng.afterInvocation(setUp, configResult);

        testng.onTestStart(result);
        call("GET", "https://api.example.test/users/1", 200);
        testng.onTestSuccess(result);

        TestStep setup = testng.steps().get(0);
        assertEquals("@BeforeMethod", setup.getKeyword());
        assertEquals("createUser", setup.getName());
        TestStep post = setup.getChildren().get(0);
        assertEquals("POST /users", post.getName());
        assertEquals(List.of("http-request", "http-response"), types(post.getArtifacts()));
        assertTrue(post.getArtifacts().get(0).getContent().contains("https://api.example.test/users"));

        TestStep get = testng.steps().get(1);
        assertEquals("GET /users/1", get.getName(), "the test's own call is a step of the test");
        assertEquals(List.of("http-request", "http-response"), types(get.getArtifacts()));
        assertTrue(testng.artifacts().isEmpty(), "exchanges live in their call steps");
    }

    @Test
    void checksAfterACallAreListedWithThatCall() {
        assertTrue(io.github.pulsereport.assertions.AssertionCapture.install());
        IInvokedMethod test = mock(IInvokedMethod.class);
        when(test.isTestMethod()).thenReturn(true);
        testng.onTestStart(result);
        testng.beforeInvocation(test, result);
        call("GET", "https://api.example.test/users/1", 200);
        org.testng.Assert.assertEquals(200, 200, "user 1 status");
        call("GET", "https://api.example.test/users/2", 200);
        org.testng.Assert.assertTrue(true, "user 2 is Ervin");
        testng.afterInvocation(test, result);
        testng.onTestSuccess(result);

        List<TestStep> steps = testng.steps();
        assertEquals(List.of("GET /users/1", "GET /users/2"), steps.stream().map(TestStep::getName).toList());
        assertEquals(List.of("user 1 status"), steps.get(0).getAssertions().stream().map(a -> a.getMessage()).toList());
        assertEquals(List.of("user 2 is Ervin"), steps.get(1).getAssertions().stream().map(a -> a.getMessage()).toList());
        assertTrue(testng.assertions().isEmpty(), "nothing left at test level");
    }

    @Test
    void requestsInAfterMethodBecomeStepsOfTheAfterMethodStep() {
        testng.onTestStart(result);
        testng.onTestSuccess(result);

        IInvokedMethod tearDown = configMethod("deleteUser", false);
        ITestResult configResult = configResult();
        testng.beforeInvocation(tearDown, configResult);
        call("DELETE", "https://api.example.test/users/1", 204);
        testng.afterInvocation(tearDown, configResult);

        TestStep teardown = testng.steps().get(testng.steps().size() - 1);
        assertEquals("@AfterMethod", teardown.getKeyword());
        assertEquals("deleteUser", teardown.getName());
        assertEquals("DELETE /users/1", teardown.getChildren().get(0).getName());
        assertEquals(List.of("http-request", "http-response"), types(teardown.getChildren().get(0).getArtifacts()));
    }

    @Test
    void capturesDuringATestAreNotStoredTwice() {
        testng.onTestStart(result);
        call("GET", "https://api.example.test/health", 200);
        rest.recordResponseTime("ignored", 42);
        List<Artifact> storedByName = RestAssuredAdapter.getArtifacts(testng.key);
        testng.onTestSuccess(result);

        assertEquals(2, testng.steps().get(0).getArtifacts().size());
        assertTrue(storedByName.isEmpty(), "went to the test directly, not to the name-keyed store");
        assertEquals(List.of("api.response.time"), testng.metrics().stream().map(Metric::getName).toList());
    }

    @Test
    void withoutARunningTestCapturesAreKeptByName() {
        rest.captureHttpRequest("standalone", "GET", "https://api.example.test/ping", java.util.Map.of(), null);
        rest.recordResponseTime("standalone", 7);

        assertEquals(1, RestAssuredAdapter.getArtifacts("standalone").size());
        assertEquals(1, RestAssuredAdapter.getMetrics("standalone").size());
    }

    private void call(String method, String url, int status) {
        FilterableRequestSpecification request = mock(FilterableRequestSpecification.class);
        FilterableResponseSpecification responseSpec = mock(FilterableResponseSpecification.class);
        FilterContext context = mock(FilterContext.class);
        Response response = mock(Response.class);
        ResponseBody<?> body = mock(ResponseBody.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getURI()).thenReturn(url);
        when(request.getHeaders()).thenReturn(new Headers(new Header("Accept", "application/json")));
        when(context.next(request, responseSpec)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(status);
        when(response.getHeaders()).thenReturn(new Headers(new Header("Content-Type", "application/json")));
        when(response.getBody()).thenAnswer(i -> body);
        when(body.asString()).thenReturn("{\"ok\":true}");
        rest.filter(request, responseSpec, context);
    }

    private static List<String> types(List<Artifact> artifacts) {
        return artifacts.stream().map(Artifact::getType).toList();
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

    private static ITestResult configResult() {
        ITestResult configResult = mock(ITestResult.class);
        when(configResult.getStatus()).thenReturn(ITestResult.SUCCESS);
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
        when(mockMethod.getRealClass()).thenReturn(RestAssuredCaptureRoutingTest.class);
        when(mockContext.getName()).thenReturn("ApiContext");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockSuite.getName()).thenReturn("ApiSuite");
        return mockResult;
    }
}
