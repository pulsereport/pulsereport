package io.github.pulsereport.adapters.restassured;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.pulsereport.config.ReporterConfig;
import io.github.pulsereport.core.model.Artifact;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Tests for RestAssuredAdapter Filter functionality. Verifies automatic HTTP
 * request/response capture with size limits and masking.
 *
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class RestAssuredFilterTest {

    private RestAssuredAdapter adapter;
    private ReporterConfig config;
    private FilterableRequestSpecification requestSpec;
    private FilterableResponseSpecification responseSpec;
    private FilterContext filterContext;
    private Response response;
    private ResponseBody<?> responseBody;

    @BeforeEach
    public void setUp() {
        config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("json"))
                .outputDirectory(new File("target/custom-reports"))
                .maxArtifactContentSize(1024) // 1KB limit for testing
                .maskSensitiveData(true)
                .maskHeaderFields("Authorization,X-API-Key,Cookie,Set-Cookie")
                .build();

        adapter = new RestAssuredAdapter(config);

        requestSpec = mock(FilterableRequestSpecification.class);
        responseSpec = mock(FilterableResponseSpecification.class);
        filterContext = mock(FilterContext.class);
        response = mock(Response.class);
        responseBody = mock(ResponseBody.class);

        when(filterContext.next(requestSpec, responseSpec)).thenReturn(response);
        when(response.getBody()).thenReturn(responseBody);
    }

    @AfterEach
    public void tearDown() {
        RestAssuredAdapter.clearCurrentTestName();
        RestAssuredAdapter.clearTestData("testApiCall");
        RestAssuredAdapter.clearTestData("testApiCallNoMask");
    }

    @Test
    public void filterCapturesRequest() throws Exception {
        RestAssuredAdapter.setCurrentTestName("testApiCall");

        when(requestSpec.getMethod()).thenReturn("POST");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/users");
        when(requestSpec.getHeaders()).thenReturn(new Headers(
                new Header("Content-Type", "application/json"),
                new Header("User-Agent", "RestAssured")
        ));
        when(requestSpec.getBody()).thenReturn("{\"name\":\"John\",\"email\":\"john@example.com\"}");

        when(responseBody.asString()).thenReturn("{\"id\":123,\"name\":\"John\"}");
        when(response.getStatusCode()).thenReturn(201);
        when(response.getHeaders()).thenReturn(new Headers(
                new Header("Content-Type", "application/json")
        ));

        adapter.filter(requestSpec, responseSpec, filterContext);

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApiCall");
        assertFalse(artifacts.isEmpty(), "Should have captured artifacts");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void filterCapturesResponse() throws Exception {
        RestAssuredAdapter.setCurrentTestName("testApiCall");

        when(requestSpec.getMethod()).thenReturn("GET");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/users/123");
        when(requestSpec.getHeaders()).thenReturn(new Headers());
        when(requestSpec.getBody()).thenReturn(null);

        when(responseBody.asString()).thenReturn("{\"id\":123,\"name\":\"John\",\"email\":\"john@example.com\"}");
        when(response.getStatusCode()).thenReturn(200);
        when(response.getHeaders()).thenReturn(new Headers(
                new Header("Content-Type", "application/json"),
                new Header("Server", "nginx")
        ));

        adapter.filter(requestSpec, responseSpec, filterContext);

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApiCall");
        assertFalse(artifacts.isEmpty(), "Should have captured response artifacts");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void filterStoresContentInArtifacts() throws Exception {
        RestAssuredAdapter.setCurrentTestName("testApiCall");

        String requestBody = "{\"username\":\"admin\",\"password\":\"secret123\"}";
        String respBody = "{\"success\":true,\"token\":\"jwt-token-here\"}";

        when(requestSpec.getMethod()).thenReturn("POST");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/login");
        when(requestSpec.getHeaders()).thenReturn(new Headers(
                new Header("Content-Type", "application/json")
        ));
        when(requestSpec.getBody()).thenReturn(requestBody);

        when(this.responseBody.asString()).thenReturn(respBody);
        when(response.getStatusCode()).thenReturn(200);
        when(response.getHeaders()).thenReturn(new Headers());

        adapter.filter(requestSpec, responseSpec, filterContext);

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApiCall");

        Artifact reqArtifact = artifacts.stream()
                .filter(a -> a.getType().equals("http-request"))
                .findFirst().orElse(null);
        assertNotNull(reqArtifact);
        assertNotNull(reqArtifact.getContent());
        assertTrue(reqArtifact.getContent().contains("POST"));
        assertTrue(reqArtifact.getContent().contains("/login"));
        // Body masking is enabled by default; sensitive fields are redacted.
        assertTrue(reqArtifact.getContent().contains("admin"));
        assertFalse(reqArtifact.getContent().contains("secret123"));
        assertTrue(reqArtifact.getContent().contains("***REDACTED***"));

        Artifact respArtifact = artifacts.stream()
                .filter(a -> a.getType().equals("http-response"))
                .findFirst().orElse(null);
        assertNotNull(respArtifact);
        assertNotNull(respArtifact.getContent());
        assertTrue(respArtifact.getContent().contains("200"));
        // Response token is masked by default body masking.
        assertFalse(respArtifact.getContent().contains("jwt-token-here"));
        assertTrue(respArtifact.getContent().contains("***REDACTED***"));

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void filterTruncatesLargePayloads() throws Exception {
        RestAssuredAdapter.setCurrentTestName("testApiCall");

        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 2048; i++) {
            largeBody.append("x");
        }
        String largeResponseBody = largeBody.toString();

        when(requestSpec.getMethod()).thenReturn("GET");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/data");
        when(requestSpec.getHeaders()).thenReturn(new Headers());
        when(requestSpec.getBody()).thenReturn(null);

        when(responseBody.asString()).thenReturn(largeResponseBody);
        when(response.getStatusCode()).thenReturn(200);
        when(response.getHeaders()).thenReturn(new Headers());

        adapter.filter(requestSpec, responseSpec, filterContext);

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApiCall");
        Artifact respArtifact = artifacts.stream()
                .filter(a -> a.getType().equals("http-response"))
                .findFirst().orElse(null);
        assertNotNull(respArtifact);
        assertNotNull(respArtifact.getContent());
        assertTrue(respArtifact.getContent().contains("[Content truncated at"));
        assertTrue(respArtifact.getContent().contains("bytes. Total size:"));

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void filterMasksSensitiveHeaders() throws Exception {
        RestAssuredAdapter.setCurrentTestName("testApiCall");

        when(requestSpec.getMethod()).thenReturn("GET");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/secure");
        when(requestSpec.getHeaders()).thenReturn(new Headers(
                new Header("Authorization", "Bearer secret-token-12345"),
                new Header("X-API-Key", "my-secret-api-key"),
                new Header("Content-Type", "application/json")
        ));
        when(requestSpec.getBody()).thenReturn(null);

        when(responseBody.asString()).thenReturn("{\"data\":\"secure\"}");
        when(response.getStatusCode()).thenReturn(200);
        when(response.getHeaders()).thenReturn(new Headers(
                new Header("Set-Cookie", "session=xyz123; HttpOnly"),
                new Header("Content-Type", "application/json")
        ));

        adapter.filter(requestSpec, responseSpec, filterContext);

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApiCall");

        Artifact reqArtifact = artifacts.stream()
                .filter(a -> a.getType().equals("http-request"))
                .findFirst().orElse(null);
        assertNotNull(reqArtifact);
        assertNotNull(reqArtifact.getContent());
        assertFalse(reqArtifact.getContent().contains("secret-token-12345"));
        assertFalse(reqArtifact.getContent().contains("my-secret-api-key"));
        assertTrue(reqArtifact.getContent().contains("***REDACTED***"));
        assertTrue(reqArtifact.getContent().contains("application/json"));

        Artifact respArtifact = artifacts.stream()
                .filter(a -> a.getType().equals("http-response"))
                .findFirst().orElse(null);
        assertNotNull(respArtifact);
        assertNotNull(respArtifact.getContent());
        assertFalse(respArtifact.getContent().contains("session=xyz123"));
        assertTrue(respArtifact.getContent().contains("***REDACTED***"));

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void filterHandlesBinaryContent() throws Exception {
        RestAssuredAdapter.setCurrentTestName("testApiCall");

        when(requestSpec.getMethod()).thenReturn("POST");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/upload");
        when(requestSpec.getHeaders()).thenReturn(new Headers(
                new Header("Content-Type", "image/png")
        ));
        when(requestSpec.getBody()).thenReturn("... binary image data ...");

        when(responseBody.asString()).thenReturn("{\"uploaded\":true}");
        when(response.getStatusCode()).thenReturn(201);
        when(response.getHeaders()).thenReturn(new Headers());

        adapter.filter(requestSpec, responseSpec, filterContext);

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApiCall");
        Artifact reqArtifact = artifacts.stream()
                .filter(a -> a.getType().equals("http-request"))
                .findFirst().orElse(null);
        assertNotNull(reqArtifact);
        assertNotNull(reqArtifact.getContent());
        assertTrue(reqArtifact.getContent().contains("image/png")
                || reqArtifact.getContent().contains("[Binary content]"));

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void filterWithMaskingDisabled() throws Exception {
        ReporterConfig configNoMask = ReporterConfig.builder()
                .outputFormats(Arrays.asList("json"))
                .outputDirectory(new File("target/custom-reports"))
                .maxArtifactContentSize(10240)
                .maskSensitiveData(false)
                .maskHeaderFields("Authorization,X-API-Key")
                .build();

        RestAssuredAdapter adapterNoMask = new RestAssuredAdapter(configNoMask);

        RestAssuredAdapter.setCurrentTestName("testApiCallNoMask");

        when(requestSpec.getMethod()).thenReturn("GET");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/data");
        when(requestSpec.getHeaders()).thenReturn(new Headers(
                new Header("Authorization", "Bearer secret-token"),
                new Header("Content-Type", "application/json")
        ));
        when(requestSpec.getBody()).thenReturn(null);

        when(responseBody.asString()).thenReturn("{\"data\":\"test\"}");
        when(response.getStatusCode()).thenReturn(200);
        when(response.getHeaders()).thenReturn(new Headers());

        adapterNoMask.filter(requestSpec, responseSpec, filterContext);

        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApiCallNoMask");
        Artifact reqArtifact = artifacts.stream()
                .filter(a -> a.getType().equals("http-request"))
                .findFirst().orElse(null);
        assertNotNull(reqArtifact);
        assertNotNull(reqArtifact.getContent());
        assertTrue(reqArtifact.getContent().contains("Bearer secret-token"));
        assertFalse(reqArtifact.getContent().contains("***REDACTED***"));

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void filterHandlesNullRequestBody() throws Exception {
        RestAssuredAdapter.setCurrentTestName("testApiCall");

        when(requestSpec.getMethod()).thenReturn("GET");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/users");
        when(requestSpec.getHeaders()).thenReturn(new Headers());
        when(requestSpec.getBody()).thenReturn(null);

        when(responseBody.asString()).thenReturn("{\"users\":[]}");
        when(response.getStatusCode()).thenReturn(200);
        when(response.getHeaders()).thenReturn(new Headers());

        Response result = adapter.filter(requestSpec, responseSpec, filterContext);

        assertNotNull(result);
        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApiCall");
        assertFalse(artifacts.isEmpty());

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void filterHandlesEmptyResponseBody() throws Exception {
        RestAssuredAdapter.setCurrentTestName("testApiCall");

        when(requestSpec.getMethod()).thenReturn("DELETE");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/users/123");
        when(requestSpec.getHeaders()).thenReturn(new Headers());
        when(requestSpec.getBody()).thenReturn(null);

        when(responseBody.asString()).thenReturn("");
        when(response.getStatusCode()).thenReturn(204);
        when(response.getHeaders()).thenReturn(new Headers());

        Response result = adapter.filter(requestSpec, responseSpec, filterContext);

        assertNotNull(result);
        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts("testApiCall");
        assertFalse(artifacts.isEmpty());

        RestAssuredAdapter.clearCurrentTestName();
    }
}
