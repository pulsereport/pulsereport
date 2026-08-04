package io.github.pulsereport.adapters.testng;

import io.github.pulsereport.adapters.Adapter;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify TestNGAdapter complies with Adapter interface contract.
 * Specifically tests that the documented API usage examples work as expected.
 * 
 * @author Custom Reporter Team
 * @since Phase 4
 */
public class AdapterContractComplianceTest {

    private Adapter adapter;

    @BeforeEach
    public void setUp() {
        adapter = new TestNGAdapter();
    }

    @Test
    public void testDocumentedApiExample_DirectCall() {
        // adapter.addArtifact("myTest", screenshot);
        
        Artifact screenshot = Artifact.builder()
                .name("failure-screenshot.png")
                .type("screenshot")
                .path("/screenshots/failure-screenshot.png")
                .mimeType("image/png")
                .size(12345L)
                .timestamp(Instant.now())
                .build();
        
        assertDoesNotThrow(() -> adapter.addArtifact("myTest", screenshot));
    }

    @Test
    public void testDocumentedApiExample_WithMetric() {
        // adapter.addMetric("myTest", responseTime);
        
        Metric responseTime = Metric.builder()
                .name("api.response.time")
                .value(250.5)
                .unit("ms")
                .timestamp(Instant.now())
                .build();
        
        assertDoesNotThrow(() -> adapter.addMetric("myTest", responseTime));
    }

    @Test
    public void testDocumentedApiExample_FullLifecycle() {
        Artifact screenshot = Artifact.builder()
                .name("test-screenshot.png")
                .type("screenshot")
                .path("/screenshots/test-screenshot.png")
                .mimeType("image/png")
                .size(10000L)
                .timestamp(Instant.now())
                .build();
        
        Metric responseTime = Metric.builder()
                .name("test.response.time")
                .value(150.0)
                .unit("ms")
                .timestamp(Instant.now())
                .build();
        
        assertDoesNotThrow(() -> {
            adapter.onTestStart("myTest");
            adapter.addArtifact("myTest", screenshot);
            adapter.addMetric("myTest", responseTime);
            adapter.onTestSuccess("myTest");
        });
    }

    @Test
    public void testStandaloneUsageWithProperSetup() {
        
        Artifact artifact = Artifact.builder()
                .name("proper-setup.png")
                .type("screenshot")
                .path("/screenshots/proper-setup.png")
                .mimeType("image/png")
                .size(5000L)
                .timestamp(Instant.now())
                .build();
        
        adapter.onTestStart("properTest");
        assertDoesNotThrow(() -> adapter.addArtifact("properTest", artifact));
    }

    @Test
    public void testFallbackBehaviorWithoutSetup() {
        
        Metric metric = Metric.builder()
                .name("fallback.metric")
                .value(100.0)
                .unit("count")
                .timestamp(Instant.now())
                .build();
        
        assertDoesNotThrow(() -> adapter.addMetric("fallbackTest", metric));
    }
}
