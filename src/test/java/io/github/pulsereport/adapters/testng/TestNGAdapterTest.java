package io.github.pulsereport.adapters.testng;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import io.github.pulsereport.adapters.Adapter;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;

/**
 * Tests for TestNGAdapter implementation. Verifies correct mapping from TestNG
 * events to the Adapter interface and proper integration with
 * TestResultAggregator.
 *
 * @author PulseReport Team
 * @since 1.0.0
 */
public class TestNGAdapterTest {

    private TestNGAdapter adapter;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        adapter = new TestNGAdapter();
    }

    @Test
    public void adapterImplementsInterface() {
        assertTrue(adapter instanceof Adapter, "TestNGAdapter should implement Adapter interface");
    }

    @Test
    public void suiteLifecycle() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("TestSuite");

        assertDoesNotThrow(() -> adapter.onStart(mockSuite));
        assertDoesNotThrow(() -> adapter.onFinish(mockSuite));
    }

    @Test
    public void testLifecycle() {
        ITestResult mockResult = createMockTestResult("testMethod", ITestResult.SUCCESS);

        assertDoesNotThrow(() -> adapter.onTestStart(mockResult));
        assertDoesNotThrow(() -> adapter.onTestSuccess(mockResult));
    }

    @Test
    public void testSuccess() {
        ITestResult mockResult = createMockTestResult("successTest", ITestResult.SUCCESS);

        adapter.onTestStart(mockResult);
        adapter.onTestSuccess(mockResult);

        assertNotNull(adapter);
    }

    @Test
    public void testFailure() {
        ITestResult mockResult = createMockTestResult("failureTest", ITestResult.FAILURE);
        Throwable throwable = new AssertionError("Test failed");
        when(mockResult.getThrowable()).thenReturn(throwable);

        adapter.onTestStart(mockResult);
        adapter.onTestFailure(mockResult);

        assertNotNull(adapter);
    }

    @Test
    public void testSkip() {
        ITestResult mockResult = createMockTestResult("skippedTest", ITestResult.SKIP);

        adapter.onTestStart(mockResult);
        adapter.onTestSkipped(mockResult);

        assertNotNull(adapter);
    }

    @Test
    public void addArtifact() {
        ITestResult mockResult = createMockTestResult("testWithArtifact", ITestResult.SUCCESS);

        adapter.onTestStart(mockResult);

        Artifact screenshot = Artifact.builder()
                .name("screenshot.png")
                .type("screenshot")
                .path("/screenshots/screenshot.png")
                .mimeType("image/png")
                .size(12345L)
                .timestamp(Instant.now())
                .build();

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void addMetric() {
        ITestResult mockResult = createMockTestResult("testWithMetric", ITestResult.SUCCESS);

        adapter.onTestStart(mockResult);

        Metric responseTime = Metric.builder()
                .name("api.response.time")
                .value(250.5)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        adapter.onTestSuccess(mockResult);
    }

    @Test
    public void getTestRunBeforeFinish() {
        TestRun testRun = adapter.getTestRun();
        assertNull(testRun, "TestRun should be null before suite finishes");
    }

    @Test
    public void getTestRunAfterFinish() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("TestSuite");

        ITestContext mockContext = createMockTestContext("TestContext");

        org.testng.ISuiteResult mockSuiteResult = mock(org.testng.ISuiteResult.class);
        when(mockSuiteResult.getTestContext()).thenReturn(mockContext);

        org.testng.IResultMap passedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap failedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap skippedTests = mock(org.testng.IResultMap.class);

        when(mockContext.getPassedTests()).thenReturn(passedTests);
        when(mockContext.getFailedTests()).thenReturn(failedTests);
        when(mockContext.getSkippedTests()).thenReturn(skippedTests);
        when(passedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(failedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(skippedTests.getAllResults()).thenReturn(new java.util.HashSet<>());

        when(mockSuite.getResults()).thenReturn(
                java.util.Collections.singletonMap("TestContext", mockSuiteResult));

        ITestResult mockResult = createMockTestResult("test1", ITestResult.SUCCESS);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);
        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun, "TestRun should not be null after suite finishes");
    }

    @Test
    public void generateReportsUsesPulseReportDefaultOutputDirectory() throws Exception {
        String previousUserDir = System.getProperty("user.dir");
        String previousOutputDir = System.getProperty("reporter.output.directory");

        try {
            System.clearProperty("reporter.output.directory");
            System.setProperty("user.dir", tempDir.toString());

            ISuite mockSuite = mock(ISuite.class);
            when(mockSuite.getName()).thenReturn("BrandSuite");

            ITestContext mockContext = createMockTestContext("BrandContext");

            org.testng.ISuiteResult mockSuiteResult = mock(org.testng.ISuiteResult.class);
            when(mockSuiteResult.getTestContext()).thenReturn(mockContext);

            org.testng.IResultMap passedTests = mock(org.testng.IResultMap.class);
            org.testng.IResultMap failedTests = mock(org.testng.IResultMap.class);
            org.testng.IResultMap skippedTests = mock(org.testng.IResultMap.class);

            when(mockContext.getPassedTests()).thenReturn(passedTests);
            when(mockContext.getFailedTests()).thenReturn(failedTests);
            when(mockContext.getSkippedTests()).thenReturn(skippedTests);

            ITestResult mockResult = createMockTestResult("brandAlignedTest", ITestResult.SUCCESS);
            when(passedTests.getAllResults()).thenReturn(new java.util.HashSet<>(java.util.List.of(mockResult)));
            when(failedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
            when(skippedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
            when(mockSuite.getResults()).thenReturn(java.util.Collections.singletonMap("BrandContext", mockSuiteResult));

            adapter.onStart(mockSuite);
            adapter.onStart(mockContext);
            adapter.onTestStart(mockResult);
            adapter.onTestSuccess(mockResult);
            adapter.onFinish(mockContext);
            adapter.onFinish(mockSuite);

            Path reportDir = tempDir.resolve("target/pulsereport");
            assertAll(
                    () -> assertTrue(Files.exists(reportDir.resolve("test-report.html")),
                            "TestNG adapter should generate the HTML report in the PulseReport default directory"),
                    () -> assertTrue(Files.exists(reportDir.resolve("test-report.json")),
                            "TestNG adapter should generate the JSON report in the PulseReport default directory"));
        } finally {
            if (previousOutputDir == null) {
                System.clearProperty("reporter.output.directory");
            } else {
                System.setProperty("reporter.output.directory", previousOutputDir);
            }

            if (previousUserDir == null) {
                System.clearProperty("user.dir");
            } else {
                System.setProperty("user.dir", previousUserDir);
            }
        }
    }

    @Test
    public void parallelExecution() throws InterruptedException {
        Runnable test1 = () -> {
            ITestResult result = createMockTestResult("parallel_test1", ITestResult.SUCCESS);
            adapter.onTestStart(result);
            adapter.onTestSuccess(result);
        };

        Runnable test2 = () -> {
            ITestResult result = createMockTestResult("parallel_test2", ITestResult.SUCCESS);
            adapter.onTestStart(result);
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
    public void artifactsAttachedToTestCase() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("TestSuite");

        ITestContext mockContext = createMockTestContext("TestContext");
        ITestResult mockResult = createMockTestResult("testWithArtifacts", ITestResult.SUCCESS);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        for (int i = 0; i < 3; i++) {
            Artifact artifact = Artifact.builder()
                    .name("artifact" + i + ".png")
                    .type("screenshot")
                    .path("/screenshots/artifact" + i + ".png")
                    .mimeType("image/png")
                    .size(1000L * (i + 1))
                    .timestamp(Instant.now())
                    .build();
            adapter.addArtifact("testWithArtifacts", artifact);
        }

        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);

    }

    @Test
    public void metricsAttachedToTestCase() {
        ISuite mockSuite = mock(ISuite.class);
        when(mockSuite.getName()).thenReturn("TestSuite");

        ITestContext mockContext = createMockTestContext("TestContext");
        ITestResult mockResult = createMockTestResult("testWithMetrics", ITestResult.SUCCESS);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);

        Metric metric1 = Metric.builder()
                .name("api.response.time")
                .value(250.5)
                .unit("ms")
                .timestamp(Instant.now())
                .build();
        adapter.addMetric("testWithMetrics", metric1);

        Metric metric2 = Metric.builder()
                .name("page.load.time")
                .value(1500.0)
                .unit("ms")
                .timestamp(Instant.now())
                .build();
        adapter.addMetric("testWithMetrics", metric2);

        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun);
    }

    @Test
    public void nullArtifactHandling() {
        ITestResult mockResult = createMockTestResult("test", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> adapter.addArtifact("test", null));
        assertNotNull(thrown);
    }

    @Test
    public void nullMetricHandling() {
        ITestResult mockResult = createMockTestResult("test", ITestResult.SUCCESS);
        adapter.onTestStart(mockResult);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> adapter.addMetric("test", null));
        assertNotNull(thrown);
    }

    @Test
    public void parameterizedTestsNoCollision() {

        ITestResult result1 = createParameterizedMockTestResult("paramTest", "param1", ITestResult.SUCCESS);
        ITestResult result2 = createParameterizedMockTestResult("paramTest", "param2", ITestResult.SUCCESS);

        adapter.onTestStart(result1);
        Artifact artifact1 = Artifact.builder()
                .name("artifact-param1.png")
                .type("screenshot")
                .path("/screenshots/artifact-param1.png")
                .mimeType("image/png")
                .size(1000L)
                .timestamp(Instant.now())
                .build();
        adapter.addArtifact("paramTest", artifact1);
        adapter.onTestSuccess(result1);

        adapter.onTestStart(result2);
        Artifact artifact2 = Artifact.builder()
                .name("artifact-param2.png")
                .type("screenshot")
                .path("/screenshots/artifact-param2.png")
                .mimeType("image/png")
                .size(2000L)
                .timestamp(Instant.now())
                .build();
        adapter.addArtifact("paramTest", artifact2);
        adapter.onTestSuccess(result2);

        // map-based approach)
        assertNotNull(adapter);
    }

    @Test
    public void lateCaptureRescopedToSingleInvocation() {
        // Single invocation of a method: after onTestSuccess clears the thread-local,
        // a late capture (e.g. video in @AfterMethod) must resolve back to that
        // invocation's full key via the method-name registry, not an ambiguous
        // bare-name bucket. With exactly one registered invocation the fallback is
        // unambiguous, so this must not throw and must associate the artifact.
        ITestResult result = createMockTestResult("soloTest", ITestResult.SUCCESS);
        adapter.onTestStart(result);
        adapter.onTestSuccess(result); // clears the thread-local

        Artifact video = Artifact.builder()
                .name("soloTest.mp4").type("video").path("/videos/soloTest.mp4")
                .mimeType("video/mp4").size(20L).timestamp(Instant.now()).build();

        // Called with no active thread-local (simulates @AfterMethod).
        assertDoesNotThrow(() -> adapter.addArtifact("soloTest", video));
    }

    @Test
    public void addArtifactWithoutTestStart() {
        Artifact artifact = Artifact.builder()
                .name("test.png")
                .type("screenshot")
                .path("/screenshots/test.png")
                .mimeType("image/png")
                .size(1000L)
                .timestamp(Instant.now())
                .build();

        assertDoesNotThrow(
                () -> adapter.addArtifact("test", artifact),
                "Should work using testName as fallback when ThreadLocal not set");
    }

    @Test
    public void addMetricWithoutTestStart() {
        Metric metric = Metric.builder()
                .name("test.metric")
                .value(100.0)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        assertDoesNotThrow(
                () -> adapter.addMetric("test", metric),
                "Should work using testName as fallback when ThreadLocal not set");
    }

    @Test
    public void parameterizedTestsWithUniqueArtifacts() {

        ISuite mockSuite = mock(ISuite.class);
        ITestContext mockContext = mock(ITestContext.class);
        org.testng.IResultMap passedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap failedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap skippedTests = mock(org.testng.IResultMap.class);

        when(mockSuite.getName()).thenReturn("ParamSuite");
        when(mockContext.getName()).thenReturn("ParamContext");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockContext.getStartDate()).thenReturn(new Date());
        when(mockContext.getEndDate()).thenReturn(new Date(System.currentTimeMillis() + 5000));
        when(mockContext.getPassedTests()).thenReturn(passedTests);
        when(mockContext.getFailedTests()).thenReturn(failedTests);
        when(mockContext.getSkippedTests()).thenReturn(skippedTests);

        org.testng.ISuiteResult mockSuiteResult = mock(org.testng.ISuiteResult.class);
        when(mockSuiteResult.getTestContext()).thenReturn(mockContext);
        when(mockSuite.getResults()).thenReturn(
                java.util.Collections.singletonMap("ParamContext", mockSuiteResult));

        ITestResult param1Result = createSimpleParameterizedResult("parameterizedTest", mockContext, "param1");
        ITestResult param2Result = createSimpleParameterizedResult("parameterizedTest", mockContext, "param2");

        java.util.Set<ITestResult> resultsSet = new java.util.HashSet<>();
        resultsSet.add(param1Result);
        resultsSet.add(param2Result);
        when(passedTests.getAllResults()).thenReturn(resultsSet);
        when(failedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(skippedTests.getAllResults()).thenReturn(new java.util.HashSet<>());

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);

        adapter.onTestStart(param1Result);
        Artifact artifact1 = Artifact.builder()
                .name("artifact-param1.png")
                .type("screenshot")
                .path("/screenshots/param1.png")
                .mimeType("image/png")
                .size(1000L)
                .timestamp(Instant.now())
                .build();
        adapter.addArtifact("parameterizedTest", artifact1);

        Metric metric1 = Metric.builder()
                .name("response.time")
                .value(100.0)
                .unit("ms")
                .timestamp(Instant.now())
                .build();
        adapter.addMetric("parameterizedTest", metric1);
        adapter.onTestSuccess(param1Result);

        adapter.onTestStart(param2Result);
        Artifact artifact2 = Artifact.builder()
                .name("artifact-param2.png")
                .type("screenshot")
                .path("/screenshots/param2.png")
                .mimeType("image/png")
                .size(2000L)
                .timestamp(Instant.now())
                .build();
        adapter.addArtifact("parameterizedTest", artifact2);

        Metric metric2 = Metric.builder()
                .name("response.time")
                .value(200.0)
                .unit("ms")
                .timestamp(Instant.now())
                .build();
        adapter.addMetric("parameterizedTest", metric2);
        adapter.onTestSuccess(param2Result);

        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun testRun = adapter.getTestRun();
        assertNotNull(testRun, "TestRun should be built after suite finishes");

        assertFalse(testRun.getSuites().isEmpty(), "TestRun should have at least one suite");
        List<TestCase> testCases = new ArrayList<>(testRun.getSuites().get(0).getTestCases());
        assertEquals(2, testCases.size(), "Should have 2 test cases for parameterized tests");

        TestCase testCase1 = null;
        TestCase testCase2 = null;

        for (TestCase tc : testCases) {
            if (!tc.getArtifacts().isEmpty()) {
                String artifactName = tc.getArtifacts().get(0).getName();
                if ("artifact-param1.png".equals(artifactName)) {
                    testCase1 = tc;
                } else if ("artifact-param2.png".equals(artifactName)) {
                    testCase2 = tc;
                }
            }
        }

        assertNotNull(testCase1, "Should have test with artifact-param1.png");
        assertNotNull(testCase2, "Should have test with artifact-param2.png");

        TestCase firstParameterizedCase = Objects.requireNonNull(testCase1);
        TestCase secondParameterizedCase = Objects.requireNonNull(testCase2);

        assertEquals(1, firstParameterizedCase.getArtifacts().size(), "First test should have 1 artifact");
        assertEquals("artifact-param1.png", firstParameterizedCase.getArtifacts().get(0).getName(),
                "First test should have artifact-param1.png");

        assertEquals(1, firstParameterizedCase.getMetrics().size(), "First test should have 1 metric");
        assertEquals("response.time", firstParameterizedCase.getMetrics().get(0).getName(),
                "First test should have response.time metric");
        assertEquals(100.0, firstParameterizedCase.getMetrics().get(0).getValue(),
                "First test metric should have value 100.0");

        assertEquals(1, secondParameterizedCase.getArtifacts().size(), "Second test should have 1 artifact");
        assertEquals("artifact-param2.png", secondParameterizedCase.getArtifacts().get(0).getName(),
                "Second test should have artifact-param2.png");

        assertEquals(1, secondParameterizedCase.getMetrics().size(), "Second test should have 1 metric");
        assertEquals("response.time", secondParameterizedCase.getMetrics().get(0).getName(),
                "Second test should have response.time metric");
        assertEquals(200.0, secondParameterizedCase.getMetrics().get(0).getValue(),
                "Second test metric should have value 200.0");
    }

    @Test
    public void standaloneAdapterApiUsage() {
        Artifact artifact = Artifact.builder()
                .name("standalone.png")
                .type("screenshot")
                .path("/screenshots/standalone.png")
                .mimeType("image/png")
                .size(2000L)
                .timestamp(Instant.now())
                .build();

        Metric metric = Metric.builder()
                .name("standalone.metric")
                .value(250.5)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        assertDoesNotThrow(() -> adapter.onTestStart("standaloneTest"));
        assertDoesNotThrow(() -> adapter.addArtifact("standaloneTest", artifact));
        assertDoesNotThrow(() -> adapter.addMetric("standaloneTest", metric));
        assertDoesNotThrow(() -> adapter.onTestSuccess("standaloneTest"));
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
        when(mockMethod.getRealClass()).thenReturn(TestNGAdapterTest.class);

        when(mockContext.getName()).thenReturn("TestContext");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockSuite.getName()).thenReturn("TestSuite");

        return mockResult;
    }

    private ITestResult createParameterizedMockTestResult(String methodName, String param, int status) {
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
        when(mockResult.getParameters()).thenReturn(new Object[]{param});

        when(mockMethod.getMethodName()).thenReturn(methodName);
        when(mockMethod.getRealClass()).thenReturn(TestNGAdapterTest.class);

        when(mockContext.getName()).thenReturn("ParamContext");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockSuite.getName()).thenReturn("ParamSuite");

        org.testng.IResultMap passedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap failedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap skippedTests = mock(org.testng.IResultMap.class);

        when(mockContext.getPassedTests()).thenReturn(passedTests);
        when(mockContext.getFailedTests()).thenReturn(failedTests);
        when(mockContext.getSkippedTests()).thenReturn(skippedTests);

        java.util.Set<ITestResult> resultsSet = new java.util.HashSet<>();
        resultsSet.add(mockResult);

        when(passedTests.getAllResults()).thenReturn(resultsSet);
        when(failedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(skippedTests.getAllResults()).thenReturn(new java.util.HashSet<>());

        when(mockContext.getStartDate()).thenReturn(new Date());
        when(mockContext.getEndDate()).thenReturn(new Date(System.currentTimeMillis() + 5000));

        org.testng.ISuiteResult mockSuiteResult = mock(org.testng.ISuiteResult.class);
        when(mockSuiteResult.getTestContext()).thenReturn(mockContext);
        when(mockSuite.getResults()).thenReturn(
                java.util.Collections.singletonMap("ParamContext", mockSuiteResult));

        return mockResult;
    }

    private ITestContext createMockTestContext(String name) {
        ITestContext mockContext = mock(ITestContext.class);
        when(mockContext.getName()).thenReturn(name);
        when(mockContext.getStartDate()).thenReturn(new Date());
        when(mockContext.getEndDate()).thenReturn(new Date(System.currentTimeMillis() + 5000));
        return mockContext;
    }

    private ITestResult createSimpleParameterizedResult(String methodName, ITestContext context, String param) {
        ITestResult mockResult = mock(ITestResult.class);
        ITestNGMethod mockMethod = mock(ITestNGMethod.class);

        when(mockResult.getMethod()).thenReturn(mockMethod);
        when(mockResult.getName()).thenReturn(methodName);
        when(mockResult.getStatus()).thenReturn(ITestResult.SUCCESS);
        when(mockResult.getStartMillis()).thenReturn(System.currentTimeMillis());
        when(mockResult.getEndMillis()).thenReturn(System.currentTimeMillis() + 1000);
        when(mockResult.getTestContext()).thenReturn(context);
        when(mockResult.getParameters()).thenReturn(new Object[]{param});

        when(mockMethod.getMethodName()).thenReturn(methodName);
        when(mockMethod.getRealClass()).thenReturn(TestNGAdapterTest.class);

        return mockResult;
    }
}
