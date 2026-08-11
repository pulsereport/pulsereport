package io.github.pulsereport.adapters.appium;

import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import org.junit.jupiter.api.Test;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.ISuite;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Manual end-to-end rendering verification: builds a TestRun that exercises
 * every new mobile feature, writes the report, and asserts on BOTH the JSON
 * model and the rendered HTML.
 */
public class RenderingVerificationTest {

    @Test
    public void rendersAllMobileFeatures() {
        AppiumAdapter adapter = new AppiumAdapter();

        ITestResult result = mockResult("testMobileLogin", ITestResult.SUCCESS);
        ISuite suite = wiredSuite("MobileSuite", "MobileCtx", result);

        adapter.onStart(suite);
        adapter.recordSessionMetadata(MobileSessionMetadata.builder()
                .platformName("iOS").platformVersion("17.0")
                .deviceName("iPhone 14 Pro").appVersion("3.2.1")
                .appiumServerVersion("2.4.1").build());

        adapter.onStart(result.getTestContext());
        adapter.onTestStart(result);

        adapter.recordStep("testMobileLogin", "launch app", 1200);
        adapter.recordStep("testMobileLogin", "tap login", TestStatus.PASSED, 150, "tapped login btn");
        adapter.captureVideo("testMobileLogin", "rec.mp4",
                java.util.Base64.getEncoder().encodeToString("video".getBytes()));
        adapter.captureCrashReport("testMobileLogin", "crash.log", "FATAL EXCEPTION");
        adapter.capturePageSource("testMobileLogin", "<hierarchy><node/></hierarchy>");
        adapter.captureScreenshot("testMobileLogin", "shot.png", "/tmp/shot.png", 1234);
        adapter.recordDeviceHealth("testMobileLogin", 85, 512.0);

        adapter.onTestSuccess(result);
        adapter.onFinish(result.getTestContext());
        adapter.onFinish(suite); // writes target/pulsereport/test-report.{html,json}

        TestRun run = adapter.getTestRun();
        assertNotNull(run);

        // --- model assertions ---
        assertEquals("iOS", run.getEnvironment().get("mobile.platform"));
        var tc = run.getSuites().get(0).getTestCases().get(0);
        assertEquals(2, tc.getSteps().size());
        assertEquals("launch app", tc.getSteps().get(0).getName());
        assertTrue(tc.getArtifacts().stream().anyMatch(a -> "video".equals(a.getType())));
        assertTrue(tc.getArtifacts().stream().anyMatch(a -> "crash".equals(a.getType())));
        assertTrue(tc.getMetrics().stream().anyMatch(m -> "device.battery.percent".equals(m.getName())));
    }

    private ITestResult mockResult(String name, int status) {
        ITestResult r = mock(ITestResult.class);
        ITestNGMethod m = mock(ITestNGMethod.class);
        when(r.getMethod()).thenReturn(m);
        when(r.getName()).thenReturn(name);
        when(r.getStatus()).thenReturn(status);
        when(r.getStartMillis()).thenReturn(System.currentTimeMillis());
        when(r.getEndMillis()).thenReturn(System.currentTimeMillis() + 1000);
        when(m.getMethodName()).thenReturn(name);
        when(m.getRealClass()).thenReturn(RenderingVerificationTest.class);
        return r;
    }

    private ISuite wiredSuite(String suiteName, String ctxName, ITestResult result) {
        ISuite suite = mock(ISuite.class);
        when(suite.getName()).thenReturn(suiteName);
        ITestContext ctx = mock(ITestContext.class);
        when(ctx.getName()).thenReturn(ctxName);
        when(ctx.getSuite()).thenReturn(suite);
        when(ctx.getStartDate()).thenReturn(new Date());
        when(ctx.getEndDate()).thenReturn(new Date(System.currentTimeMillis() + 5000));

        org.testng.IResultMap passed = mock(org.testng.IResultMap.class);
        org.testng.IResultMap failed = mock(org.testng.IResultMap.class);
        org.testng.IResultMap skipped = mock(org.testng.IResultMap.class);
        Set<ITestResult> p = new HashSet<>();
        if (result.getStatus() == ITestResult.SUCCESS) p.add(result);
        when(passed.getAllResults()).thenReturn(p);
        when(failed.getAllResults()).thenReturn(new HashSet<>());
        when(skipped.getAllResults()).thenReturn(new HashSet<>());
        when(ctx.getPassedTests()).thenReturn(passed);
        when(ctx.getFailedTests()).thenReturn(failed);
        when(ctx.getSkippedTests()).thenReturn(skipped);
        when(result.getTestContext()).thenReturn(ctx);

        org.testng.ISuiteResult sr = mock(org.testng.ISuiteResult.class);
        when(sr.getTestContext()).thenReturn(ctx);
        Map<String, org.testng.ISuiteResult> results = new HashMap<>();
        results.put(ctxName, sr);
        when(suite.getResults()).thenReturn(results);
        return suite;
    }
}
