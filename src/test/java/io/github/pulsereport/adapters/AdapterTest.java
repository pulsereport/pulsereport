package io.github.pulsereport.adapters;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStep;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Adapter interface contract.
 * Verifies that any adapter implementation conforms to expected behavior.
 * 
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class AdapterTest {

    /**
     * Mock implementation of Adapter for testing interface contract.
     */
    private static class MockAdapter implements Adapter {
        private boolean suiteStarted = false;
        private boolean suiteFinished = false;
        private int testStartCount = 0;
        private int artifactCount = 0;
        private int metricCount = 0;
        private int stepCount = 0;
        
        @Override
        public void onSuiteStart(String suiteName) {
            suiteStarted = true;
        }
        
        @Override
        public void onSuiteFinish(String suiteName) {
            suiteFinished = true;
        }
        
        @Override
        public void onTestStart(String testName) {
            testStartCount++;
        }
        
        @Override
        public void onTestSuccess(String testName) {
        }
        
        @Override
        public void onTestFailure(String testName, Throwable throwable) {
        }
        
        @Override
        public void onTestSkip(String testName) {
        }
        
        @Override
        public void addArtifact(String testName, Artifact artifact) {
            if (artifact == null) {
                throw new IllegalArgumentException("Artifact cannot be null");
            }
            artifactCount++;
        }
        
        @Override
        public void addMetric(String testName, Metric metric) {
            if (metric == null) {
                throw new IllegalArgumentException("Metric cannot be null");
            }
            metricCount++;
        }

        @Override
        public void addStep(String testName, TestStep step) {
            if (step == null) {
                throw new IllegalArgumentException("Step cannot be null");
            }
            stepCount++;
        }

        @Override
        public TestRun getTestRun() {
            return null;
        }
    }
    
    @Test
    public void testAdapterLifecycleMethods() {
        MockAdapter adapter = new MockAdapter();
        
        adapter.onSuiteStart("TestSuite");
        assertTrue(adapter.suiteStarted, "Suite should be marked as started");
        
        adapter.onTestStart("test1");
        assertEquals(adapter.testStartCount, 1, "Test start count should be 1");
        
        adapter.onTestStart("test2");
        assertEquals(adapter.testStartCount, 2, "Test start count should be 2");
        
        adapter.onSuiteFinish("TestSuite");
        assertTrue(adapter.suiteFinished, "Suite should be marked as finished");
    }
    
    @Test
    public void testAdapterArtifactSupport() {
        MockAdapter adapter = new MockAdapter();
        
        Artifact screenshot = Artifact.builder()
                .name("screenshot.png")
                .type("screenshot")
                .path("/screenshots/screenshot.png")
                .mimeType("image/png")
                .size(12345L)
                .timestamp(Instant.now())
                .build();
        
        adapter.addArtifact("test1", screenshot);
        assertEquals(adapter.artifactCount, 1, "Artifact count should be 1");
        
        Artifact log = Artifact.builder()
                .name("test.log")
                .type("log")
                .path("/logs/test.log")
                .mimeType("text/plain")
                .size(5432L)
                .timestamp(Instant.now())
                .build();
        
        adapter.addArtifact("test1", log);
        assertEquals(adapter.artifactCount, 2, "Artifact count should be 2");
    }
    
    @Test
    public void testAdapterMetricSupport() {
        MockAdapter adapter = new MockAdapter();
        
        Metric responseTime = Metric.builder()
                .name("api.response.time")
                .value(250.5)
                .unit("ms")
                .timestamp(Instant.now())
                .build();
        
        adapter.addMetric("test1", responseTime);
        assertEquals(adapter.metricCount, 1, "Metric count should be 1");
        
        Metric pageLoad = Metric.builder()
                .name("page.load.time")
                .value(1500.0)
                .unit("ms")
                .timestamp(Instant.now())
                .build();
        
        adapter.addMetric("test1", pageLoad);
        assertEquals(adapter.metricCount, 2, "Metric count should be 2");
    }
    
    @Test
    public void testAdapterNullHandling() {
        MockAdapter adapter = new MockAdapter();
        
        assertThrows(IllegalArgumentException.class, 
            () -> adapter.addArtifact("test1", null));
        assertThrows(IllegalArgumentException.class,
            () -> adapter.addMetric("test1", null));
        
        assertEquals(adapter.artifactCount, 0, "No artifacts should be counted when nulls are rejected");
        assertEquals(adapter.metricCount, 0, "No metrics should be counted when nulls are rejected");
    }
}
