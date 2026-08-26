package io.github.pulsereport.adapters.restassured;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.restassured.filter.Filter;

/**
 * Tests for RestAssuredAdapter implementation. Verifies standalone API-specific
 * artifact/metric capture for REST-assured.
 *
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class RestAssuredAdapterTest {

    private RestAssuredAdapter adapter;

    @BeforeEach
    public void setUp() {
        adapter = new RestAssuredAdapter();
    }

    @AfterEach
    public void tearDown() {
        RestAssuredAdapter.clearCurrentTestName();
        RestAssuredAdapter.clearTestData("testApi");
        RestAssuredAdapter.clearTestData("testWithMultipleArtifacts");
        RestAssuredAdapter.clearTestData("testWithMultipleMetrics");
        RestAssuredAdapter.clearTestData("api_test1");
        RestAssuredAdapter.clearTestData("api_test2");
        RestAssuredAdapter.clearTestData("test");
    }

    @Test
    public void adapterImplementsFilter() {
        assertTrue(adapter instanceof Filter, "RestAssuredAdapter should implement Filter interface");
    }

    @Test
    public void captureHttpRequest() {
        RestAssuredAdapter.setCurrentTestName("testApi");

        String requestBody = "{\"username\":\"user\",\"password\":\"pass\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        assertDoesNotThrow(() -> adapter.captureHttpRequest("testApi", "POST", "/api/login",
                headers, requestBody));

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApi");
        assertFalse(artifacts.isEmpty(), "Should have captured HTTP request artifact");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void captureHttpResponse() {
        RestAssuredAdapter.setCurrentTestName("testApi");

        String responseBody = "{\"success\":true,\"token\":\"abc123\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        assertDoesNotThrow(() -> adapter.captureHttpResponse("testApi", 200, headers, responseBody));

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApi");
        assertFalse(artifacts.isEmpty(), "Should have captured HTTP response artifact");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void captureJsonPayload() {
        RestAssuredAdapter.setCurrentTestName("testApi");

        String jsonPayload = "{\"data\":{\"id\":1,\"name\":\"Test\"}}";
        assertDoesNotThrow(() -> adapter.captureJsonPayload("testApi", "response-body.json", jsonPayload));

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApi");
        assertFalse(artifacts.isEmpty(), "Should have captured JSON payload artifact");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void captureXmlPayload() {
        RestAssuredAdapter.setCurrentTestName("testApi");

        String xmlPayload = "<?xml version=\"1.0\"?><data><id>1</id></data>";
        assertDoesNotThrow(() -> adapter.captureXmlPayload("testApi", "response-body.xml", xmlPayload));

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApi");
        assertFalse(artifacts.isEmpty(), "Should have captured XML payload artifact");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void recordResponseTime() {
        RestAssuredAdapter.setCurrentTestName("testApi");

        assertDoesNotThrow(() -> adapter.recordResponseTime("testApi", 350.5));

        List<Metric> metrics = RestAssuredAdapter.getMetrics("testApi");
        assertFalse(metrics.isEmpty(), "Should have recorded response time metric");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void recordRequestSize() {
        RestAssuredAdapter.setCurrentTestName("testApi");

        assertDoesNotThrow(() -> adapter.recordRequestSize("testApi", 1024L));

        List<Metric> metrics = RestAssuredAdapter.getMetrics("testApi");
        assertFalse(metrics.isEmpty(), "Should have recorded request size metric");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void recordResponseSize() {
        RestAssuredAdapter.setCurrentTestName("testApi");

        assertDoesNotThrow(() -> adapter.recordResponseSize("testApi", 2048L));

        List<Metric> metrics = RestAssuredAdapter.getMetrics("testApi");
        assertFalse(metrics.isEmpty(), "Should have recorded response size metric");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void multipleApiArtifacts() {
        RestAssuredAdapter.setCurrentTestName("testWithMultipleArtifacts");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        adapter.captureHttpRequest("testWithMultipleArtifacts", "POST", "/api/users", headers, "{\"name\":\"John\"}");
        adapter.captureHttpResponse("testWithMultipleArtifacts", 201, headers, "{\"id\":123,\"name\":\"John\"}");
        adapter.captureJsonPayload("testWithMultipleArtifacts", "request.json", "{\"name\":\"John\"}");
        adapter.captureJsonPayload("testWithMultipleArtifacts", "response.json", "{\"id\":123}");

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testWithMultipleArtifacts");
        assertEquals(4, artifacts.size(), "Should have 4 artifacts");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void multipleApiMetrics() {
        RestAssuredAdapter.setCurrentTestName("testWithMultipleMetrics");

        adapter.recordResponseTime("testWithMultipleMetrics", 350.5);
        adapter.recordRequestSize("testWithMultipleMetrics", 1024L);
        adapter.recordResponseSize("testWithMultipleMetrics", 2048L);

        List<Metric> metrics = RestAssuredAdapter.getMetrics("testWithMultipleMetrics");
        assertEquals(3, metrics.size(), "Should have 3 metrics");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void threadSafetyForParallelApiTests() throws InterruptedException {
        Runnable test1 = () -> {
            RestAssuredAdapter.setCurrentTestName("api_test1");
            adapter.captureJsonPayload("api_test1", "test1.json", "{\"test\":1}");
            adapter.recordResponseTime("api_test1", 300.0);
            RestAssuredAdapter.clearCurrentTestName();
        };

        Runnable test2 = () -> {
            RestAssuredAdapter.setCurrentTestName("api_test2");
            adapter.captureJsonPayload("api_test2", "test2.json", "{\"test\":2}");
            adapter.recordResponseTime("api_test2", 400.0);
            RestAssuredAdapter.clearCurrentTestName();
        };

        Thread t1 = new Thread(test1);
        Thread t2 = new Thread(test2);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertNotNull(adapter);
        assertFalse(RestAssuredAdapter.getArtifacts("api_test1").isEmpty());
        assertFalse(RestAssuredAdapter.getArtifacts("api_test2").isEmpty());
    }

    @Test
    public void nullParameterHandling() {
        RestAssuredAdapter.setCurrentTestName("test");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureHttpRequest(null, "GET", "/api", headers, "body"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureHttpRequest("test", null, "/api", headers, "body"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureHttpRequest("test", "GET", null, headers, "body"));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureHttpResponse(null, 200, headers, "body"));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureJsonPayload(null, "file.json", "content"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureJsonPayload("test", null, "content"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureXmlPayload(null, "file.xml", "content"));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.captureXmlPayload("test", null, "content"));

        assertThrows(IllegalArgumentException.class,
                () -> adapter.recordResponseTime(null, 100.0));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.recordRequestSize(null, 100L));
        assertThrows(IllegalArgumentException.class,
                () -> adapter.recordResponseSize(null, 100L));

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void setAndClearCurrentTestName() {
        RestAssuredAdapter.setCurrentTestName("myTest");
        adapter.captureJsonPayload("myTest", "test.json", "{\"test\":true}");

        assertFalse(RestAssuredAdapter.getArtifacts("myTest").isEmpty());

        RestAssuredAdapter.clearCurrentTestName();
        RestAssuredAdapter.clearTestData("myTest");
    }

    @Test
    public void clearTestData() {
        RestAssuredAdapter.setCurrentTestName("testApi");
        adapter.captureJsonPayload("testApi", "test.json", "{\"test\":true}");
        adapter.recordResponseTime("testApi", 100.0);

        assertFalse(RestAssuredAdapter.getArtifacts("testApi").isEmpty());
        assertFalse(RestAssuredAdapter.getMetrics("testApi").isEmpty());

        RestAssuredAdapter.clearTestData("testApi");

        assertTrue(RestAssuredAdapter.getArtifacts("testApi").isEmpty());
        assertTrue(RestAssuredAdapter.getMetrics("testApi").isEmpty());

        RestAssuredAdapter.clearCurrentTestName();
    }
}
