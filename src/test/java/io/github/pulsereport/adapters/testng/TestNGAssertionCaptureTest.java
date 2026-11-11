package io.github.pulsereport.adapters.testng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testng.Assert;
import org.testng.IInvokedMethod;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import io.github.pulsereport.adapters.selenium.SeleniumTracker;
import io.github.pulsereport.assertions.AssertionCapture;
import io.github.pulsereport.core.model.AssertionResult;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;

/** Assertions made in TestNG test methods, configuration methods and tracked sub-steps. */
class TestNGAssertionCaptureTest {

    private static final AtomicInteger TEST_IDS = new AtomicInteger();

    @TempDir
    File reportDir;

    private RecordingAdapter adapter;
    private ITestResult result;
    private IInvokedMethod testMethod;

    /** Exposes what the adapter recorded for the test it last started. */
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

        List<AssertionResult> assertions() {
            return recordedAssertions(key);
        }
    }

    @BeforeAll
    static void installAgent() {
        assertTrue(AssertionCapture.install(), "assertion capture agent should attach in the test JVM");
    }

    @BeforeEach
    void setUp() {
        System.setProperty("reporter.output.directory", reportDir.getAbsolutePath());
        adapter = new RecordingAdapter();
        result = testResult("checkedTest" + TEST_IDS.incrementAndGet());
        testMethod = invoked(false, null, false);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("reporter.output.directory");
    }

    @Test
    void assertionsInTheTestMethodBelongToTheTest() {
        adapter.onTestStart(result);
        adapter.beforeInvocation(testMethod, result);
        Assert.assertEquals(200, 200, "status code");
        try {
            Assert.assertEquals("Ann", "Bob", "user name");
        } catch (AssertionError expected) {
            // the test fails on it
        }
        adapter.afterInvocation(testMethod, result);
        adapter.onTestFailure(result);

        List<AssertionResult> assertions = adapter.assertions();
        assertEquals(List.of("status code", "user name"), assertions.stream().map(AssertionResult::getMessage).toList());
        assertEquals(List.of(TestStatus.PASSED, TestStatus.FAILED),
                assertions.stream().map(AssertionResult::getStatus).toList());
    }

    @Test
    void assertionsInATrackedStepBelongToThatStep() {
        adapter.onTestStart(result);
        adapter.beforeInvocation(testMethod, result);
        SeleniumTracker.step("Open cart", () -> { });
        SeleniumTracker.step("Check totals", () -> Assert.assertTrue(true, "total is shown"));
        adapter.afterInvocation(testMethod, result);
        adapter.onTestSuccess(result);

        assertEquals(List.of(), messages(adapter.steps().get(0)));
        assertEquals(List.of("total is shown"), messages(adapter.steps().get(1)));
        assertTrue(adapter.assertions().isEmpty());
    }

    @Test
    void assertionsGoToTheStepTheyFollow() throws Exception {
        adapter.onTestStart(result);
        adapter.beforeInvocation(testMethod, result);
        Assert.assertTrue(true, "before any step");
        SeleniumTracker.step("Log in", () -> { });
        Assert.assertTrue(true, "dashboard is shown");
        Assert.assertEquals("Ann", "Ann", "greeting");
        SeleniumTracker.step("Open orders", () -> { });
        Assert.assertTrue(true, "orders are listed");
        adapter.afterInvocation(testMethod, result);
        adapter.onTestSuccess(result);

        assertEquals(List.of("dashboard is shown", "greeting"), messages(adapter.steps().get(0)));
        assertEquals(List.of("orders are listed"), messages(adapter.steps().get(1)));
        assertEquals(List.of("before any step"), adapter.assertions().stream().map(AssertionResult::getMessage).toList(),
                "an assertion made before any step stays on the test");
    }

    private static List<String> messages(TestStep step) {
        return step.getAssertions().stream().map(AssertionResult::getMessage).toList();
    }

    @Test
    void assertionsInSetupAndTeardownBecomeTheirSteps() {
        IInvokedMethod setUp = invoked(true, "seedData", true);
        ITestResult setUpResult = configResult();
        adapter.beforeInvocation(setUp, setUpResult);
        Assert.assertTrue(true, "data seeded");
        adapter.afterInvocation(setUp, setUpResult);

        adapter.onTestStart(result);
        adapter.beforeInvocation(testMethod, result);
        adapter.afterInvocation(testMethod, result);
        adapter.onTestSuccess(result);

        IInvokedMethod tearDown = invoked(true, "cleanUp", false);
        ITestResult tearDownResult = configResult();
        adapter.beforeInvocation(tearDown, tearDownResult);
        Assert.assertFalse(false, "data removed");
        adapter.afterInvocation(tearDown, tearDownResult);

        List<TestStep> steps = adapter.steps();
        TestStep setup = steps.get(0);
        assertEquals("@BeforeMethod", setup.getKeyword());
        assertEquals(List.of("data seeded"), setup.getAssertions().stream().map(AssertionResult::getMessage).toList());
        TestStep teardown = steps.get(steps.size() - 1);
        assertEquals("@AfterMethod", teardown.getKeyword());
        assertEquals(List.of("data removed"), teardown.getAssertions().stream().map(AssertionResult::getMessage).toList());
        assertTrue(adapter.assertions().isEmpty(), "setup and teardown checks are not the test's own");
    }

    private static IInvokedMethod invoked(boolean configuration, String name, boolean before) {
        IInvokedMethod invoked = mock(IInvokedMethod.class);
        ITestNGMethod method = mock(ITestNGMethod.class);
        when(invoked.isConfigurationMethod()).thenReturn(configuration);
        when(invoked.isTestMethod()).thenReturn(!configuration);
        when(invoked.getTestMethod()).thenReturn(method);
        if (configuration) {
            when(method.getMethodName()).thenReturn(name);
            when(method.isBeforeMethodConfiguration()).thenReturn(before);
            when(method.isAfterMethodConfiguration()).thenReturn(!before);
        }
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
        when(mockMethod.getRealClass()).thenReturn(TestNGAssertionCaptureTest.class);
        when(mockContext.getName()).thenReturn("CheckContext");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockSuite.getName()).thenReturn("CheckSuite");
        return mockResult;
    }
}
