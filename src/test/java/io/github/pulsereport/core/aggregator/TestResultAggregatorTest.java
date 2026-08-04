package io.github.pulsereport.core.aggregator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestSuite;

/**
 * TDD tests for TestResultAggregator. These tests define the expected behavior
 * before implementation.
 */
public class TestResultAggregatorTest {

    private TestResultAggregator aggregator;

    public static class TestClass1 {
    }

    public static class TestClass2 {
    }

    public static class FailTest {
    }

    public static class SkipTest {
    }

    public static class FlakyTest {
    }

    public static class TimingTest {
    }

    @BeforeEach
    public void setUp() {
        aggregator = new TestResultAggregator();
    }

    /**
     * Test 1: Basic conversion - ITestResult with SUCCESS → TestCase with
     * PASSED status
     */
    @Test
    public void testConvertSuccessfulTestResultToTestCase() {
        ITestResult mockResult = createMockTestResult(
                "testMethod",
                "com.example.TestClass",
                ITestResult.SUCCESS,
                1000L,
                2000L,
                null
        );

        TestCase testCase = aggregator.convertToTestCase(mockResult);

        assertNotNull(testCase, "TestCase should not be null");
        assertEquals("testMethod", testCase.getName());
        assertTrue(testCase.getClassName().contains("TestClass")); // Class name may vary
        assertEquals("testMethod", testCase.getMethodName());
        assertEquals(TestStatus.PASSED, testCase.getStatus());
        assertEquals(1000L, testCase.getDuration());
        assertNotNull(testCase.getStartTime());
        assertNotNull(testCase.getEndTime());
        assertNull(testCase.getErrorMessage());
        assertNull(testCase.getStackTrace());
        assertEquals(0, testCase.getRetryCount());
    }

    /**
     * Test 2: Failure conversion - ITestResult with FAILURE → TestCase with
     * errorMessage and stackTrace
     */
    @Test
    public void testConvertFailedTestResultToTestCase() {
        Throwable throwable = new AssertionError("Expected <true> but was <false>");
        ITestResult mockResult = createMockTestResult(
                "testFailingMethod",
                "com.example.FailTest",
                ITestResult.FAILURE,
                500L,
                1500L,
                throwable
        );

        TestCase testCase = aggregator.convertToTestCase(mockResult);

        assertNotNull(testCase);
        assertEquals("testFailingMethod", testCase.getName());
        assertEquals(TestStatus.FAILED, testCase.getStatus());
        assertEquals("Expected <true> but was <false>", testCase.getErrorMessage());
        assertNotNull(testCase.getStackTrace());
        assertTrue(testCase.getStackTrace().contains("AssertionError"));
        assertEquals(0, testCase.getRetryCount());
    }

    /**
     * Test 3: Skip conversion - ITestResult with SKIP → TestCase with SKIPPED
     * status
     */
    @Test
    public void testConvertSkippedTestResultToTestCase() {
        ITestResult mockResult = createMockTestResult(
                "testSkippedMethod",
                "com.example.SkipTest",
                ITestResult.SKIP,
                0L,
                100L,
                null
        );

        TestCase testCase = aggregator.convertToTestCase(mockResult);

        assertNotNull(testCase);
        assertEquals("testSkippedMethod", testCase.getName());
        assertEquals(TestStatus.SKIPPED, testCase.getStatus());
        assertNull(testCase.getErrorMessage());
        assertEquals(0, testCase.getRetryCount());
    }

    /**
     * Test 4: Retry/Flaky detection - Two ITestResults (fail, pass) → TestCase
     * with FLAKY status
     */
    @Test
    public void testDetectFlakyTestAfterRetry() {
        ITestResult failedResult = createMockTestResult(
                "testFlakyMethod",
                "com.example.FlakyTest",
                ITestResult.FAILURE,
                100L,
                600L,
                new RuntimeException("Flaky failure")
        );

        ITestResult successResult = createMockTestResult(
                "testFlakyMethod",
                "com.example.FlakyTest",
                ITestResult.SUCCESS,
                700L,
                1200L,
                null
        );

        aggregator.recordTestResult(failedResult);
        aggregator.recordTestResult(successResult);

        String actualClassName = FlakyTest.class.getName();
        TestCase testCase = aggregator.getTestCase(actualClassName + ".testFlakyMethod_testFlakyMethod");

        assertNotNull(testCase, "TestCase should not be null for flaky test");
        assertEquals("testFlakyMethod", testCase.getName());
        assertEquals(TestStatus.FLAKY, testCase.getStatus());
        assertEquals(1, testCase.getRetryCount());
        assertNull(testCase.getErrorMessage());
    }

    /**
     * Test 5: Multiple attempts all fail → TestCase with FAILED status and
     * retry count
     */
    @Test
    public void testMultipleFailedAttempts() {
        ITestResult firstFailure = createMockTestResult(
                "testAlwaysFails",
                "com.Test",
                ITestResult.FAILURE,
                100L,
                200L,
                new AssertionError("First failure")
        );

        ITestResult secondFailure = createMockTestResult(
                "testAlwaysFails",
                "com.Test",
                ITestResult.FAILURE,
                300L,
                400L,
                new AssertionError("Second failure")
        );

        aggregator.recordTestResult(firstFailure);
        aggregator.recordTestResult(secondFailure);

        String actualClassName = TestClass1.class.getName();
        TestCase testCase = aggregator.getTestCase(actualClassName + ".testAlwaysFails_testAlwaysFails");

        assertNotNull(testCase, "TestCase should not be null for multiple failed attempts");
        assertEquals(TestStatus.FAILED, testCase.getStatus());
        assertEquals(1, testCase.getRetryCount());
        assertEquals("Second failure", testCase.getErrorMessage());
    }

    /**
     * Test 6: Multiple tests in suite → TestSuite with multiple TestCases
     */
    @Test
    public void testConvertMultipleTestsToSuite() {
        ITestContext mockContext = createMockTestContext("TestSuite1");

        ITestResult result1 = createMockTestResult("test1", "com.Test", ITestResult.SUCCESS, 100L, 200L, null);
        ITestResult result2 = createMockTestResult("test2", "com.Test", ITestResult.FAILURE, 300L, 400L, new AssertionError("Failed"));
        ITestResult result3 = createMockTestResult("test3", "com.Test", ITestResult.SKIP, 0L, 0L, null);

        aggregator.recordTestResult(result1);
        aggregator.recordTestResult(result2);
        aggregator.recordTestResult(result3);
        TestSuite suite = aggregator.convertToTestSuite(mockContext, Arrays.asList(result1, result2, result3));

        assertNotNull(suite);
        assertEquals("TestSuite1", suite.getName());
        assertEquals(3, suite.getTotalTests());
        assertEquals(1, suite.getPassedTests());
        assertEquals(1, suite.getFailedTests());
        assertEquals(1, suite.getSkippedTests());
        assertEquals(3, suite.getTestCases().size());
        assertEquals(TestStatus.FAILED, suite.getStatus());
    }

    /**
     * Test 7: Nested suites → TestRun with multiple TestSuites
     */
    @Test
    public void testConvertNestedSuitesToTestRun() {
        ISuite mockSuite = createMockSuite("MasterSuite");

        ITestContext context1 = createMockTestContext("Suite1");
        ITestContext context2 = createMockTestContext("Suite2");

        ISuiteResult suiteResult1 = mock(ISuiteResult.class);
        ISuiteResult suiteResult2 = mock(ISuiteResult.class);
        when(suiteResult1.getTestContext()).thenReturn(context1);
        when(suiteResult2.getTestContext()).thenReturn(context2);

        IResultMap passedTests1 = mock(IResultMap.class);
        IResultMap failedTests1 = mock(IResultMap.class);
        IResultMap skippedTests1 = mock(IResultMap.class);
        IResultMap passedTests2 = mock(IResultMap.class);
        IResultMap failedTests2 = mock(IResultMap.class);
        IResultMap skippedTests2 = mock(IResultMap.class);

        when(context1.getPassedTests()).thenReturn(passedTests1);
        when(context1.getFailedTests()).thenReturn(failedTests1);
        when(context1.getSkippedTests()).thenReturn(skippedTests1);
        when(context2.getPassedTests()).thenReturn(passedTests2);
        when(context2.getFailedTests()).thenReturn(failedTests2);
        when(context2.getSkippedTests()).thenReturn(skippedTests2);

        when(passedTests1.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(failedTests1.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(skippedTests1.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(passedTests2.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(failedTests2.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(skippedTests2.getAllResults()).thenReturn(new java.util.HashSet<>());

        Map<String, ISuiteResult> suiteResults = new HashMap<>();
        suiteResults.put("Suite1", suiteResult1);
        suiteResults.put("Suite2", suiteResult2);
        when(mockSuite.getResults()).thenReturn(suiteResults);

        ITestResult result1 = createMockTestResult("test1", "com.Test1", ITestResult.SUCCESS, 100L, 200L, null);
        ITestResult result2 = createMockTestResult("test2", "com.Test2", ITestResult.SUCCESS, 100L, 200L, null);

        aggregator.recordTestResult(result1);
        aggregator.recordTestResult(result2);

        TestRun testRun = aggregator.convertToTestRun(mockSuite);

        assertNotNull(testRun);
        assertEquals("MasterSuite", testRun.getName());
        assertEquals(2, testRun.getSuites().size());
        assertNotNull(testRun.getStartTime());
        assertNotNull(testRun.getEndTime());
    }

    /**
     * Test 8: Parallel execution → deterministic TestRun (thread-safe)
     */
    @Test
    public void testParallelExecutionIsDeterministic() throws InterruptedException {
        int threadCount = 10;
        int testsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * testsPerThread);

        // Act - simulate parallel test execution
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            for (int i = 0; i < testsPerThread; i++) {
                final int testId = i;
                executor.submit(() -> {
                    try {
                        ITestResult result = createMockTestResult(
                                "test_" + threadId + "_" + testId,
                                "com.Thread" + threadId,
                                ITestResult.SUCCESS,
                                100L,
                                200L,
                                null
                        );
                        aggregator.recordTestResult(result);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        List<TestCase> allTests = aggregator.getAllTestCases();
        assertEquals(threadCount * testsPerThread, allTests.size());

        for (int i = 1; i < allTests.size(); i++) {
            String prev = allTests.get(i - 1).getName();
            String curr = allTests.get(i).getName();
            assertTrue(prev.compareTo(curr) <= 0
                    || allTests.get(i - 1).getStartTime().isBefore(allTests.get(i).getStartTime()),
                    "Results should be deterministically ordered");
        }
    }

    /**
     * Test 9: Timing extraction - ITestResult timing → TestCase startTime,
     * endTime, duration
     */
    @Test
    public void testTimingExtractionFromTestResult() {
        long startTimeMs = 1609459200000L; // 2021-01-01 00:00:00 UTC
        long endTimeMs = 1609459201500L;   // 2021-01-01 00:00:01.5 UTC
        long expectedDuration = 1500L;

        ITestResult mockResult = createMockTestResult(
                "testTiming",
                "com.example.TimingTest",
                ITestResult.SUCCESS,
                startTimeMs,
                endTimeMs,
                null
        );

        TestCase testCase = aggregator.convertToTestCase(mockResult);

        assertNotNull(testCase.getStartTime());
        assertNotNull(testCase.getEndTime());
        assertEquals(expectedDuration, testCase.getDuration());
        assertEquals(Instant.ofEpochMilli(startTimeMs), testCase.getStartTime());
        assertEquals(Instant.ofEpochMilli(endTimeMs), testCase.getEndTime());
    }

    /**
     * Test 10: Null safety - handle null throwable gracefully
     */
    @Test
    public void testNullThrowableHandling() {
        ITestResult mockResult = createMockTestResult(
                "testMethod",
                "com.Test",
                ITestResult.SUCCESS,
                100L,
                200L,
                null
        );

        TestCase testCase = aggregator.convertToTestCase(mockResult);

        assertNotNull(testCase);
        assertNull(testCase.getErrorMessage());
        assertNull(testCase.getStackTrace());
    }

    @Test
    public void testPassedExpectedExceptionDoesNotExposeThrowableDetails() {
        ITestResult mockResult = createMockTestResult(
                "testDivisionByZero",
                "com.example.TestClass",
                ITestResult.SUCCESS,
                100L,
                200L,
                new ArithmeticException("Division by zero")
        );

        TestCase testCase = aggregator.convertToTestCase(mockResult);

        assertEquals(TestStatus.PASSED, testCase.getStatus());
        assertNull(testCase.getErrorMessage(), "Passed expected-exception tests should not expose an error message");
        assertNull(testCase.getStackTrace(), "Passed expected-exception tests should not expose a stack trace");
    }

    /**
     * Test 11: Empty suite handling
     */
    @Test
    public void testEmptySuiteConversion() {
        ITestContext mockContext = createMockTestContext("EmptySuite");
        List<ITestResult> emptyResults = new ArrayList<>();

        TestSuite suite = aggregator.convertToTestSuite(mockContext, emptyResults);

        assertNotNull(suite);
        assertEquals("EmptySuite", suite.getName());
        assertEquals(0, suite.getTotalTests());
        assertEquals(0, suite.getTestCases().size());
        assertEquals(TestStatus.PASSED, suite.getStatus()); // Empty suite should be PASSED
    }

    /**
     * Test 12: Suite status calculation - all passed → PASSED
     */
    @Test
    public void testSuiteStatusAllPassed() {
        ITestContext mockContext = createMockTestContext("AllPassedSuite");
        ITestResult result1 = createMockTestResult("test1", "com.Test", ITestResult.SUCCESS, 100L, 200L, null);
        ITestResult result2 = createMockTestResult("test2", "com.Test", ITestResult.SUCCESS, 100L, 200L, null);

        aggregator.recordTestResult(result1);
        aggregator.recordTestResult(result2);
        TestSuite suite = aggregator.convertToTestSuite(mockContext, Arrays.asList(result1, result2));

        assertEquals(TestStatus.PASSED, suite.getStatus());
    }

    /**
     * Test 13: Suite status calculation - any failed → FAILED
     */
    @Test
    public void testSuiteStatusWithFailures() {
        ITestContext mockContext = createMockTestContext("FailedSuite");
        ITestResult result1 = createMockTestResult("test1", "com.Test", ITestResult.SUCCESS, 100L, 200L, null);
        ITestResult result2 = createMockTestResult("test2", "com.Test", ITestResult.FAILURE, 100L, 200L, new AssertionError());

        aggregator.recordTestResult(result1);
        aggregator.recordTestResult(result2);
        TestSuite suite = aggregator.convertToTestSuite(mockContext, Arrays.asList(result1, result2));

        assertEquals(TestStatus.FAILED, suite.getStatus());
    }

    /**
     * Test 14: Suite status calculation - has flaky → FLAKY (if no failures)
     */
    @Test
    public void testSuiteStatusWithFlakyTests() {
        ITestContext mockContext = createMockTestContext("FlakySuite");

        ITestResult failResult = createMockTestResult("flakyTest", "com.Test", ITestResult.FAILURE, 100L, 200L, new AssertionError());
        ITestResult passResult = createMockTestResult("flakyTest", "com.Test", ITestResult.SUCCESS, 300L, 400L, null);
        ITestResult normalPass = createMockTestResult("normalTest", "com.Test", ITestResult.SUCCESS, 100L, 200L, null);

        aggregator.recordTestResult(failResult);
        aggregator.recordTestResult(passResult);
        aggregator.recordTestResult(normalPass);

        TestSuite suite = aggregator.convertToTestSuite(mockContext, Arrays.asList(normalPass, passResult));

        assertEquals(TestStatus.FLAKY, suite.getStatus());
    }

    /**
     * Creates a mock ITestResult with specified parameters.
     */
    private ITestResult createMockTestResult(String testName, String className, int status, long startTime, long endTime, Throwable throwable) {
        ITestResult mockResult = mock(ITestResult.class);
        ITestNGMethod mockMethod = mock(ITestNGMethod.class);

        when(mockResult.getName()).thenReturn(testName);
        when(mockResult.getTestName()).thenReturn(testName);  // Set test name for key generation
        when(mockResult.getStatus()).thenReturn(status);
        when(mockResult.getStartMillis()).thenReturn(startTime);
        when(mockResult.getEndMillis()).thenReturn(endTime);
        when(mockResult.getThrowable()).thenReturn(throwable);
        when(mockResult.getMethod()).thenReturn(mockMethod);
        when(mockResult.getParameters()).thenReturn(new Object[0]);  // Empty parameters by default

        when(mockMethod.getMethodName()).thenReturn(testName);
        when(mockMethod.getRealClass()).thenReturn(getClassForName(className));

        return mockResult;
    }

    /**
     * Gets a class for the given name, using predefined test classes when
     * possible.
     */
    private Class<?> getClassForName(String className) {
        switch (className) {
            case "com.example.TestClass":
                return TestClass1.class;
            case "com.Test":
            case "com.Test1":
                return TestClass1.class;
            case "com.Test2":
                return TestClass2.class;
            case "com.example.FailTest":
                return FailTest.class;
            case "com.example.SkipTest":
                return SkipTest.class;
            case "com.example.FlakyTest":
                return FlakyTest.class;
            case "com.example.TimingTest":
                return TimingTest.class;
            case "com.Thread0":
            case "com.Thread1":
            case "com.Thread2":
            case "com.Thread3":
            case "com.Thread4":
            case "com.Thread5":
            case "com.Thread6":
            case "com.Thread7":
            case "com.Thread8":
            case "com.Thread9":
                return TestClass1.class; // Use same class for thread tests
            default:
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    return TestClass1.class;
                }
        }
    }

    /**
     * Creates a mock ITestContext.
     */
    private ITestContext createMockTestContext(String suiteName) {
        ITestContext mockContext = mock(ITestContext.class);
        XmlTest mockXmlTest = mock(XmlTest.class);

        when(mockContext.getName()).thenReturn(suiteName);
        when(mockContext.getStartDate()).thenReturn(new java.util.Date(1000L));
        when(mockContext.getEndDate()).thenReturn(new java.util.Date(2000L));
        when(mockContext.getCurrentXmlTest()).thenReturn(mockXmlTest);

        return mockContext;
    }

    /**
     * Creates a mock ISuite.
     */
    private ISuite createMockSuite(String suiteName) {
        ISuite mockSuite = mock(ISuite.class);
        XmlSuite mockXmlSuite = mock(XmlSuite.class);

        when(mockSuite.getName()).thenReturn(suiteName);
        when(mockSuite.getXmlSuite()).thenReturn(mockXmlSuite);

        return mockSuite;
    }
}
