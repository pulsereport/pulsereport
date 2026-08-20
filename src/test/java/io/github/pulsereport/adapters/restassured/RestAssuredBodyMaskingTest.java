package io.github.pulsereport.adapters.restassured;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Tests for request/response body masking in RestAssuredAdapter. Verifies that
 * sensitive body fields are masked on all capture paths (automatic filter and
 * manual capture), that masking respects the configuration gates, and that
 * JSON/XML payload artifacts actually attach their content.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class RestAssuredBodyMaskingTest {

    private RestAssuredAdapter adapter;
    private FilterableRequestSpecification requestSpec;
    private FilterableResponseSpecification responseSpec;
    private FilterContext filterContext;
    private Response response;
    private ResponseBody<?> responseBody;

    private static ReporterConfig maskingConfig() {
        return ReporterConfig.builder()
                .outputFormats(Arrays.asList("json"))
                .outputDirectory(new File("target/custom-reports"))
                .maxArtifactContentSize(10240)
                .maskSensitiveData(true)
                .maskHeaderFields("Authorization,X-API-Key")
                .maskBodyEnabled(true)
                .sensitiveBodyFields("password,token")
                .maskTokens(true)
                .build();
    }

    @BeforeEach
    public void setUp() {
        adapter = new RestAssuredAdapter(maskingConfig());

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
        RestAssuredAdapter.clearTestData("maskReqTest");
        RestAssuredAdapter.clearTestData("maskRespTest");
        RestAssuredAdapter.clearTestData("manualMaskTest");
        RestAssuredAdapter.clearTestData("manualNoMaskTest");
        RestAssuredAdapter.clearTestData("headersOnlyTest");
        RestAssuredAdapter.clearTestData("jsonPayloadTest");
        RestAssuredAdapter.clearTestData("xmlPayloadTest");
    }

    @Test
    public void testCapturedRequestBodyIsMasked() {
        RestAssuredAdapter.setCurrentTestName("maskReqTest");

        when(requestSpec.getMethod()).thenReturn("POST");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/login");
        when(requestSpec.getHeaders()).thenReturn(new Headers(
                new Header("Content-Type", "application/json")
        ));
        when(requestSpec.getBody()).thenReturn("{\"username\":\"admin\",\"password\":\"secret123\"}");

        when(responseBody.asString()).thenReturn("{\"ok\":true}");
        when(response.getStatusCode()).thenReturn(200);
        when(response.getHeaders()).thenReturn(new Headers());

        adapter.filter(requestSpec, responseSpec, filterContext);

        Artifact reqArtifact = findArtifact("maskReqTest", "http-request");
        assertNotNull(reqArtifact);
        assertNotNull(reqArtifact.getContent());
        assertFalse(reqArtifact.getContent().contains("secret123"),
                "Request body password should be masked");
        assertTrue(reqArtifact.getContent().contains("***REDACTED***"),
                "Request body should contain redacted placeholder");
        assertTrue(reqArtifact.getContent().contains("admin"),
                "Non-sensitive fields should remain readable");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void testCapturedResponseBodyIsMasked() {
        RestAssuredAdapter.setCurrentTestName("maskRespTest");

        when(requestSpec.getMethod()).thenReturn("GET");
        when(requestSpec.getURI()).thenReturn("http://api.example.com/token");
        when(requestSpec.getHeaders()).thenReturn(new Headers());
        when(requestSpec.getBody()).thenReturn(null);

        when(responseBody.asString()).thenReturn("{\"success\":true,\"token\":\"tok-abc-123\"}");
        when(response.getStatusCode()).thenReturn(200);
        when(response.getHeaders()).thenReturn(new Headers(
                new Header("Content-Type", "application/json")
        ));

        adapter.filter(requestSpec, responseSpec, filterContext);

        Artifact respArtifact = findArtifact("maskRespTest", "http-response");
        assertNotNull(respArtifact);
        assertNotNull(respArtifact.getContent());
        assertFalse(respArtifact.getContent().contains("tok-abc-123"),
                "Response body token should be masked");
        assertTrue(respArtifact.getContent().contains("***REDACTED***"),
                "Response body should contain redacted placeholder");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void testManualCaptureMasksHeadersAndBody() {
        RestAssuredAdapter.setCurrentTestName("manualMaskTest");

        Map<String, String> reqHeaders = new HashMap<>();
        reqHeaders.put("Authorization", "Bearer manual-secret-token");
        reqHeaders.put("Content-Type", "application/json");

        adapter.captureHttpRequest("manualMaskTest", "POST", "/api/login",
                reqHeaders, "{\"username\":\"admin\",\"password\":\"secret123\"}");

        Map<String, String> respHeaders = new HashMap<>();
        respHeaders.put("X-API-Key", "resp-secret-key");
        respHeaders.put("Content-Type", "application/json");

        adapter.captureHttpResponse("manualMaskTest", 200, respHeaders,
                "{\"success\":true,\"token\":\"tok-xyz-789\"}");

        Artifact reqArtifact = findArtifact("manualMaskTest", "http-request");
        assertNotNull(reqArtifact);
        assertNotNull(reqArtifact.getContent());
        assertFalse(reqArtifact.getContent().contains("manual-secret-token"),
                "Request Authorization header should be masked");
        assertFalse(reqArtifact.getContent().contains("secret123"),
                "Request body password should be masked");
        assertTrue(reqArtifact.getContent().contains("***REDACTED***"));

        Artifact respArtifact = findArtifact("manualMaskTest", "http-response");
        assertNotNull(respArtifact);
        assertNotNull(respArtifact.getContent());
        assertFalse(respArtifact.getContent().contains("resp-secret-key"),
                "Response X-API-Key header should be masked");
        assertFalse(respArtifact.getContent().contains("tok-xyz-789"),
                "Response body token should be masked");
        assertTrue(respArtifact.getContent().contains("***REDACTED***"));

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void testManualCaptureRespectsMaskingDisabled() {
        ReporterConfig noMaskConfig = ReporterConfig.builder()
                .outputFormats(Arrays.asList("json"))
                .outputDirectory(new File("target/custom-reports"))
                .maskSensitiveData(false)
                .maskHeaderFields("Authorization")
                .sensitiveBodyFields("password,token")
                .build();
        RestAssuredAdapter noMaskAdapter = new RestAssuredAdapter(noMaskConfig);

        RestAssuredAdapter.setCurrentTestName("manualNoMaskTest");

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer visible-token");
        headers.put("Content-Type", "application/json");

        noMaskAdapter.captureHttpRequest("manualNoMaskTest", "POST", "/api/login",
                headers, "{\"username\":\"admin\",\"password\":\"secret123\"}");
        noMaskAdapter.captureHttpResponse("manualNoMaskTest", 200, headers,
                "{\"success\":true,\"token\":\"tok-visible\"}");

        Artifact reqArtifact = findArtifact("manualNoMaskTest", "http-request");
        assertNotNull(reqArtifact);
        assertNotNull(reqArtifact.getContent());
        assertTrue(reqArtifact.getContent().contains("Bearer visible-token"),
                "Header should remain raw when masking is disabled");
        assertTrue(reqArtifact.getContent().contains("secret123"),
                "Body should remain raw when masking is disabled");
        assertFalse(reqArtifact.getContent().contains("***REDACTED***"));

        Artifact respArtifact = findArtifact("manualNoMaskTest", "http-response");
        assertNotNull(respArtifact);
        assertNotNull(respArtifact.getContent());
        assertTrue(respArtifact.getContent().contains("tok-visible"),
                "Response body should remain raw when masking is disabled");
        assertFalse(respArtifact.getContent().contains("***REDACTED***"));

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void testMaskBodyDisabledMasksHeadersOnly() {
        ReporterConfig headersOnlyConfig = ReporterConfig.builder()
                .outputFormats(Arrays.asList("json"))
                .outputDirectory(new File("target/custom-reports"))
                .maskSensitiveData(true)
                .maskHeaderFields("Authorization")
                .maskBodyEnabled(false)
                .sensitiveBodyFields("password,token")
                .build();
        RestAssuredAdapter headersOnlyAdapter = new RestAssuredAdapter(headersOnlyConfig);

        RestAssuredAdapter.setCurrentTestName("headersOnlyTest");

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer header-secret");
        headers.put("Content-Type", "application/json");

        headersOnlyAdapter.captureHttpRequest("headersOnlyTest", "POST", "/api/login",
                headers, "{\"username\":\"admin\",\"password\":\"secret123\"}");

        Artifact reqArtifact = findArtifact("headersOnlyTest", "http-request");
        assertNotNull(reqArtifact);
        assertNotNull(reqArtifact.getContent());
        assertFalse(reqArtifact.getContent().contains("header-secret"),
                "Header should still be masked when body masking is disabled");
        assertTrue(reqArtifact.getContent().contains("***REDACTED***"));
        assertTrue(reqArtifact.getContent().contains("secret123"),
                "Body should remain raw when body masking is disabled");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void testCaptureJsonPayloadAttachesContent() {
        RestAssuredAdapter.setCurrentTestName("jsonPayloadTest");

        String jsonPayload = "{\"user\":\"bob\",\"password\":\"hunter2\"}";
        adapter.captureJsonPayload("jsonPayloadTest", "request.json", jsonPayload);

        Artifact artifact = findArtifactByName("jsonPayloadTest", "request.json");
        assertNotNull(artifact);
        assertNotNull(artifact.getContent(), "JSON payload content should be attached");
        assertFalse(artifact.getContent().contains("hunter2"),
                "Sensitive field should be masked in attached content");
        assertTrue(artifact.getContent().contains("***REDACTED***"));

        // With masking disabled the raw payload must be attached verbatim.
        ReporterConfig noMaskConfig = ReporterConfig.builder()
                .outputFormats(Arrays.asList("json"))
                .outputDirectory(new File("target/custom-reports"))
                .maskSensitiveData(false)
                .build();
        RestAssuredAdapter noMaskAdapter = new RestAssuredAdapter(noMaskConfig);
        noMaskAdapter.captureJsonPayload("jsonPayloadTest", "raw.json", jsonPayload);

        Artifact rawArtifact = findArtifactByName("jsonPayloadTest", "raw.json");
        assertNotNull(rawArtifact);
        assertEquals(jsonPayload, rawArtifact.getContent(),
                "Raw JSON payload should be attached verbatim when masking is disabled");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void testCaptureXmlPayloadAttachesContent() {
        RestAssuredAdapter.setCurrentTestName("xmlPayloadTest");

        String xmlPayload = "<?xml version=\"1.0\"?><data><id>1</id><name>Test</name></data>";
        adapter.captureXmlPayload("xmlPayloadTest", "response.xml", xmlPayload);

        Artifact artifact = findArtifactByName("xmlPayloadTest", "response.xml");
        assertNotNull(artifact);
        assertNotNull(artifact.getContent(), "XML payload content should be attached");
        assertEquals(xmlPayload, artifact.getContent(),
                "XML payload without sensitive data should be attached verbatim");
        assertEquals((long) xmlPayload.length(), artifact.getSize(),
                "Artifact size should match the attached content length");

        RestAssuredAdapter.clearCurrentTestName();
    }

    @Test
    public void headersNotMaskedWhenMaskHeadersEnabledFalse() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("json"))
                .outputDirectory(new File("target/custom-reports"))
                .maskSensitiveData(true)
                .maskHeadersEnabled(false)
                .maskBodyEnabled(true)
                .maskHeaderFields("Authorization")
                .sensitiveBodyFields("password")
                .build();
        RestAssuredAdapter headerOffAdapter = new RestAssuredAdapter(config);

        RestAssuredAdapter.setCurrentTestName("headersOffTest");
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer visible-secret");
            headers.put("Content-Type", "application/json");

            headerOffAdapter.captureHttpRequest("headersOffTest", "POST", "/api/login",
                    headers, "{\"password\":\"x\"}");

            Artifact artifact = findArtifact("headersOffTest", "http-request");
            assertNotNull(artifact);
            assertNotNull(artifact.getContent());
            assertTrue(artifact.getContent().contains("Bearer visible-secret"),
                    "Authorization should be visible when maskHeaders.enabled=false");
            assertFalse(artifact.getContent().contains("\"password\":\"x\""),
                    "password should still be masked when body masking is enabled");
            assertTrue(artifact.getContent().contains("***REDACTED***"));
        } finally {
            RestAssuredAdapter.clearCurrentTestName();
            RestAssuredAdapter.clearTestData("headersOffTest");
        }
    }

    @Test
    public void defaultConstructor_loadsAutoDetectedPropertiesFile() throws Exception {
        Path cwdProps = Paths.get("reporter.properties");
        assertFalse(Files.exists(cwdProps),
                "precondition: no working-directory reporter.properties should exist");
        try {
            Files.write(cwdProps, Arrays.asList(
                    "reporter.output.formats=json",
                    "reporter.output.directory=target/custom-reports",
                    "reporter.maskSensitiveData=true",
                    "reporter.maskBody.enabled=true",
                    "reporter.maskBody.fields=customfield"));

            RestAssuredAdapter autoAdapter = new RestAssuredAdapter();

            RestAssuredAdapter.setCurrentTestName("autoDetectTest");
            autoAdapter.captureHttpRequest("autoDetectTest", "POST", "/api/x",
                    new HashMap<>(), "{\"customfield\":\"v1\",\"password\":\"p1\"}");

            Artifact artifact = findArtifact("autoDetectTest", "http-request");
            assertNotNull(artifact);
            assertNotNull(artifact.getContent());
            assertFalse(artifact.getContent().contains("v1"),
                    "custom field from auto-detected reporter.properties should be masked");
            assertTrue(artifact.getContent().contains("p1"),
                    "password is not in the auto-detected field list, so it must remain visible");
        } finally {
            Files.deleteIfExists(cwdProps);
            RestAssuredAdapter.clearCurrentTestName();
            RestAssuredAdapter.clearTestData("autoDetectTest");
        }
    }

    @Test
    public void defaultConstructor_fallsBackToDefaultsWhenNoFile() {
        // DEVIATION: ReporterConfig.autoDetect() also checks the classpath
        // /reporter.properties, which is always present in the test classpath
        // (src/main/resources/reporter.properties), so the literal fallback
        // branch cannot be exercised here. The classpath file's masking
        // defaults match createDefaultConfig(), so the observable behavior
        // asserted below is identical in both cases: with no working-directory
        // properties file, default fields like "password" are masked.
        Path cwdProps = Paths.get("reporter.properties");
        assertFalse(Files.exists(cwdProps),
                "precondition: no working-directory reporter.properties should exist");

        RestAssuredAdapter defaultAdapter = new RestAssuredAdapter();

        RestAssuredAdapter.setCurrentTestName("defaultCtorTest");
        try {
            defaultAdapter.captureHttpRequest("defaultCtorTest", "POST", "/api/x",
                    new HashMap<>(), "{\"password\":\"topsecret\"}");

            Artifact artifact = findArtifact("defaultCtorTest", "http-request");
            assertNotNull(artifact);
            assertNotNull(artifact.getContent());
            assertFalse(artifact.getContent().contains("topsecret"),
                    "default sensitive fields should be masked");
            assertTrue(artifact.getContent().contains("***REDACTED***"));
        } finally {
            RestAssuredAdapter.clearCurrentTestName();
            RestAssuredAdapter.clearTestData("defaultCtorTest");
        }
    }

    private static Artifact findArtifact(String testName, String type) {
        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts(testName);
        return artifacts.stream()
                .filter(a -> type.equals(a.getType()))
                .findFirst().orElse(null);
    }

    private static Artifact findArtifactByName(String testName, String name) {
        List<Artifact> artifacts = RestAssuredAdapter.getArtifacts(testName);
        return artifacts.stream()
                .filter(a -> name.equals(a.getName()))
                .findFirst().orElse(null);
    }
}
