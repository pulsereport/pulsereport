package io.github.pulsereport.adapters.restassured;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.pulsereport.adapters.cucumber.CucumberStepContext;
import io.github.pulsereport.config.ReporterConfig;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * REST-assured adapter for API test automation.
 *
 * <p>
 * This is a standalone adapter that implements {@link Filter} to capture HTTP
 * requests/responses, API payloads, and API performance metrics. It works with
 * any test framework (TestNG, JUnit, Cucumber) without requiring
 * framework-specific dependencies.</p>
 *
 * <h2>Features</h2>
 * <ul>
 * <li>Captures HTTP request details (method, URL, headers, body)</li>
 * <li>Captures HTTP response details (status code, headers, body)</li>
 * <li>Captures JSON and XML payloads</li>
 * <li>Records API performance metrics (response time, request/response
 * size)</li>
 * <li>Thread-safe for parallel API test execution</li>
 * <li>Works with any test framework (TestNG, JUnit, Cucumber)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class ApiTest {
 *     private static RestAssuredAdapter adapter = new RestAssuredAdapter();
 *
 *     @Test
 *     public void testLoginApi() {
 *         // Build request
 *         String requestBody = "{\"username\":\"user\",\"password\":\"pass\"}";
 *         Map<String, String> requestHeaders = new HashMap<>();
 *         requestHeaders.put("Content-Type", "application/json");
 *
 *         // Capture request
 *         adapter.captureHttpRequest("testLoginApi", "POST", "/api/login",
 *                                     requestHeaders, requestBody);
 *         adapter.recordRequestSize("testLoginApi", requestBody.length());
 *
 *         // Make API call
 *         long startTime = System.currentTimeMillis();
 *         Response response = given()
 *             .contentType("application/json")
 *             .body(requestBody)
 *             .when()
 *             .post("/api/login")
 *             .then()
 *             .extract().response();
 *         long responseTime = System.currentTimeMillis() - startTime;
 *
 *         // Capture response
 *         adapter.recordResponseTime("testLoginApi", responseTime);
 *
 *         Map<String, String> responseHeaders = response.getHeaders().asList().stream()
 *             .collect(Collectors.toMap(Header::getName, Header::getValue));
 *         adapter.captureHttpResponse("testLoginApi", response.getStatusCode(),
 *                                      responseHeaders, response.getBody().asString());
 *         adapter.recordResponseSize("testLoginApi", response.getBody().asString().length());
 *
 *         // Capture JSON payloads
 *         adapter.captureJsonPayload("testLoginApi", "request.json", requestBody);
 *         adapter.captureJsonPayload("testLoginApi", "response.json", response.getBody().asString());
 *     }
 * }
 * }</pre>
 *
 * <h2>Integration with REST-assured</h2>
 * <p>
 * This adapter works with REST-assured's Response and RequestSpecification
 * objects. Extract the relevant data from REST-assured and pass it to the
 * adapter methods to capture artifacts and metrics.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This adapter uses concurrent collections and ThreadLocal storage, making it
 * safe to use with parallel API test execution. Each thread maintains its own
 * test context to prevent artifact/metric collisions.</p>
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class RestAssuredAdapter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RestAssuredAdapter.class);

    private static final String HEADERS_PREFIX = "Headers:\n";
    private static final String BODY_PREFIX = "Body:\n";
    private static final String BINARY_SIZE_SUFFIX = " bytes]";
    private static final String MIME_TEXT_PLAIN = "text/plain";

    // Static ThreadLocal to share test name across all adapter instances in the same thread
    // This enables the listener instance and filter instance to coordinate
    private static final ThreadLocal<String> currentTestName = new ThreadLocal<>();

    // Guard against multiple filter instances capturing artifacts for the same HTTP call.
    // When users call RestAssured.filters(new RestAssuredAdapter()) in a @Before hook,
    // filters accumulate — instance N means N captures per call.  This flag ensures only
    // the outermost instance records artifacts.
    private static final ThreadLocal<Boolean> capturing = ThreadLocal.withInitial(() -> false);

    private static final ConcurrentHashMap<String, List<Artifact>> artifactsByTest = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<Metric>> metricsByTest = new ConcurrentHashMap<>();

    private final ReporterConfig config;

    /**
     * Constructs a new RestAssuredAdapter. Configuration is auto-detected via
     * {@link ReporterConfig#autoDetect()} (working-directory
     * {@code reporter.properties}, then
     * {@code src/main/resources/reporter.properties}, then the classpath
     * {@code /reporter.properties}); if nothing is found, a built-in default
     * configuration is used.
     */
    public RestAssuredAdapter() {
        ReporterConfig detected = ReporterConfig.autoDetect();
        if (detected != null) {
            this.config = detected;
            logger.info("RestAssuredAdapter initialized with auto-detected config"
                    + " (reporter.properties from working directory or classpath)");
        } else {
            this.config = createDefaultConfig();
            logger.info("RestAssuredAdapter initialized with default config"
                    + " (no reporter.properties found, using built-in defaults)");
        }
    }

    /**
     * Constructs a new RestAssuredAdapter with custom configuration.
     *
     * @param config the reporter configuration
     */
    public RestAssuredAdapter(ReporterConfig config) {
        this.config = config != null ? config : createDefaultConfig();
        logger.info("RestAssuredAdapter initialized with custom config");
    }

    /**
     * Routes an artifact to the active Cucumber step buffer when a Cucumber
     * step is executing on the current thread; otherwise stores in the internal
     * map.
     *
     * @param testName the name of the test to attach the artifact to
     * @param artifact the artifact to add
     * @throws IllegalArgumentException if artifact is null
     */
    public void addArtifact(String testName, Artifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("Artifact cannot be null");
        }
        List<Artifact> cucumberBuf = CucumberStepContext.currentStepArtifacts.get();
        if (cucumberBuf != null) {
            cucumberBuf.add(artifact);
            logger.debug("Routed artifact '{}' to active Cucumber step", artifact.getName());
            return;
        }
        String key = currentTestName.get();
        if (key == null) {
            key = testName;
        }
        artifactsByTest.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(artifact);
        logger.debug("Added artifact '{}' for test '{}'", artifact.getName(), key);
    }

    /**
     * Adds a metric to a test.
     *
     * @param testName the name of the test to attach the metric to
     * @param metric the metric to add
     * @throws IllegalArgumentException if metric is null
     */
    public void addMetric(String testName, Metric metric) {
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }
        String key = currentTestName.get();
        if (key == null) {
            key = testName;
        }
        metricsByTest.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(metric);
        logger.debug("Added metric '{}' for test '{}'", metric.getName(), key);
    }

    /**
     * Sets the current test name for the calling thread. Framework adapters
     * (TestNG, JUnit, etc.) should call this at test start.
     *
     * @param name the test name/key
     */
    public static void setCurrentTestName(String name) {
        currentTestName.set(name);
    }

    /**
     * Clears the current test name for the calling thread. Framework adapters
     * should call this at test end.
     */
    public static void clearCurrentTestName() {
        currentTestName.remove();
    }

    /**
     * Returns artifacts collected for a specific test.
     *
     * @param testName the test name/key
     * @return unmodifiable list of artifacts, empty if none
     */
    public static List<Artifact> getArtifacts(String testName) {
        List<Artifact> list = artifactsByTest.get(testName);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Returns metrics collected for a specific test.
     *
     * @param testName the test name/key
     * @return unmodifiable list of metrics, empty if none
     */
    public static List<Metric> getMetrics(String testName) {
        List<Metric> list = metricsByTest.get(testName);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    /**
     * Clears all data (artifacts and metrics) for a specific test.
     *
     * @param testName the test name/key
     */
    public static void clearTestData(String testName) {
        artifactsByTest.remove(testName);
        metricsByTest.remove(testName);
    }

    /**
     * Creates a default configuration for backward compatibility.
     */
    private static ReporterConfig createDefaultConfig() {
        return ReporterConfig.builder()
                .maxArtifactContentSize(51200) // 50KB default
                .maskSensitiveData(true)
                .maskHeaderFields("Authorization,X-API-Key,Cookie,Set-Cookie")
                .maskBodyEnabled(true)
                .sensitiveBodyFields("password,secret,token,access_token,refresh_token,"
                        + "id_token,client_secret,api_key,apiKey,authorization")
                .maskTokens(true)
                .build();
    }

    /**
     * Masks sensitive fields in an HTTP body when masking is enabled.
     *
     * <p>
     * Masking is applied only when both {@code maskSensitiveData} and
     * {@code maskBodyEnabled} are true. The configured comma-separated
     * sensitive body fields are parsed the same way as sensitive headers.</p>
     *
     * @param body the body to mask (can be null)
     * @return the masked body, or the original body when masking is disabled
     */
    private String maskBodyIfEnabled(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        if (!config.isMaskSensitiveData() || !config.isMaskBodyEnabled()) {
            return body;
        }
        List<String> sensitiveFields = Arrays.asList(
                config.getSensitiveBodyFields().split(","));
        List<String> xmlFields = Arrays.asList(
                config.getXmlFields().split(","));
        return SensitiveDataMasker.maskBody(body, sensitiveFields, xmlFields,
                config.isMaskXmlEnabled(), config.isMaskTokens());
    }

    /**
     * Masks sensitive headers in a header map when masking is enabled.
     *
     * <p>
     * The map is converted to REST-assured {@link Headers} so the shared
     * {@link SensitiveDataMasker#maskHeaders} logic is reused, then converted
     * back to a map. Returns the original map when masking is disabled.</p>
     *
     * @param headers the header map to mask (can be null)
     * @return a map with sensitive header values redacted, or the original map
     */
    private Map<String, String> maskHeadersIfEnabled(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()
                || !config.isMaskSensitiveData() || !config.isMaskHeadersEnabled()) {
            return headers;
        }
        List<String> sensitiveHeadersList = Arrays.asList(
                config.getMaskHeaderFields().split(","));
        List<io.restassured.http.Header> headerList = new java.util.ArrayList<>();
        headers.forEach((name, value)
                -> headerList.add(new io.restassured.http.Header(name, value)));
        Headers masked = SensitiveDataMasker.maskHeaders(new Headers(headerList), sensitiveHeadersList);
        Map<String, String> result = new java.util.LinkedHashMap<>();
        masked.forEach(header -> result.put(header.getName(), header.getValue()));
        return result;
    }

    /**
     * Filter implementation for automatic HTTP request/response capture. This
     * method is automatically invoked by RestAssured for each HTTP call.
     *
     * @param requestSpec the request specification
     * @param responseSpec the response specification
     * @param ctx the filter context
     * @return the HTTP response
     */
    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
            FilterableResponseSpecification responseSpec,
            FilterContext ctx) {
        // Get current test name from thread local (set by onTestStart for TestNG).
        // For Cucumber runs there is no TestNG listener, so testName is null — but
        // CucumberStepContext.currentStepArtifacts is non-null when a step is active.
        // Use a sentinel name so the capture methods are invoked; addArtifact() will
        // route the artifacts to the active Cucumber step buffer automatically.
        String testName = currentTestName.get();
        boolean inCucumberStep = (testName == null)
                && (CucumberStepContext.currentStepArtifacts.get() != null);
        String effectiveName = testName != null ? testName : (inCucumberStep ? "__cucumber__" : null);

        // Guard: only the first (outermost) filter instance on this thread captures.
        // Subsequent duplicate instances skip capture but still call ctx.next().
        boolean shouldCapture = effectiveName != null && !capturing.get();
        if (shouldCapture) {
            capturing.set(true);
            captureRequestFromSpec(effectiveName, requestSpec);
        }

        Response response;
        try {
            response = ctx.next(requestSpec, responseSpec);
        } finally {
            if (shouldCapture) {
                capturing.set(false);
            }
        }

        if (shouldCapture) {
            captureResponseFromSpec(effectiveName, response);
        }

        return response;
    }

    /**
     * Captures HTTP request details from FilterableRequestSpecification.
     */
    private void captureRequestFromSpec(String testName, FilterableRequestSpecification requestSpec) {
        try {
            String method = requestSpec.getMethod();
            String uri = requestSpec.getURI();
            Headers headers = requestSpec.getHeaders();
            Object body = requestSpec.getBody();

            String bodyString = body != null ? body.toString() : null;

            StringBuilder requestContent = new StringBuilder();
            requestContent.append(method).append(" ").append(uri).append("\n\n");

            Headers processedHeaders = headers;
            if (config.isMaskSensitiveData() && config.isMaskHeadersEnabled()) {
                List<String> sensitiveHeadersList = Arrays.asList(
                        config.getMaskHeaderFields().split(",")
                );
                processedHeaders = SensitiveDataMasker.maskHeaders(headers, sensitiveHeadersList);
            }

            if (processedHeaders != null && processedHeaders.iterator().hasNext()) {
                requestContent.append(HEADERS_PREFIX);
                processedHeaders.forEach(header
                        -> requestContent.append(header.getName()).append(": ")
                                .append(header.getValue()).append("\n"));
                requestContent.append("\n");
            }

            String contentType = headers != null ? headers.getValue("Content-Type") : null;
            boolean isBinary = isBinaryContent(contentType);

            if (bodyString != null && !bodyString.isEmpty()) {
                if (isBinary) {
                    requestContent.append("Body:\n[Binary content - ")
                            .append(contentType)
                            .append(", size: ")
                            .append(bodyString.length())
                            .append(BINARY_SIZE_SUFFIX);
                } else {
                    requestContent.append(BODY_PREFIX).append(maskBodyIfEnabled(bodyString));
                }
            }

            String finalContent = applyContentSizeLimit(requestContent.toString());

            Artifact httpRequest = Artifact.builder()
                    .name("http-request.txt")
                    .type("http-request")
                    .path("/artifacts/http/http-request.txt")
                    .mimeType(MIME_TEXT_PLAIN)
                    .size((long) finalContent.length())
                    .timestamp(Instant.now())
                    .content(finalContent)
                    .build();

            addArtifact(testName, httpRequest);
            logger.debug("Captured HTTP request for test '{}': {} {}", testName, method, uri);
        } catch (Exception e) {
            logger.error("Error capturing HTTP request for test '{}'", testName, e);
        }
    }

    /**
     * Captures HTTP response details from Response.
     */
    private void captureResponseFromSpec(String testName, Response response) {
        try {
            int statusCode = response.getStatusCode();
            Headers headers = response.getHeaders();
            String body = null;

            try {
                body = response.getBody().asString();
            } catch (Exception e) {
                logger.debug("Could not read response body as string", e);
            }

            StringBuilder responseContent = new StringBuilder();
            responseContent.append("Status: ").append(statusCode).append("\n\n");

            Headers processedHeaders = headers;
            if (config.isMaskSensitiveData() && config.isMaskHeadersEnabled()) {
                List<String> sensitiveHeadersList = Arrays.asList(
                        config.getMaskHeaderFields().split(",")
                );
                processedHeaders = SensitiveDataMasker.maskHeaders(headers, sensitiveHeadersList);
            }

            if (processedHeaders != null && processedHeaders.iterator().hasNext()) {
                responseContent.append(HEADERS_PREFIX);
                processedHeaders.forEach(header
                        -> responseContent.append(header.getName()).append(": ")
                                .append(header.getValue()).append("\n"));
                responseContent.append("\n");
            }

            String contentType = headers != null ? headers.getValue("Content-Type") : null;
            boolean isBinary = isBinaryContent(contentType);

            if (body != null && !body.isEmpty()) {
                if (isBinary) {
                    responseContent.append("Body:\n[Binary content - ")
                            .append(contentType)
                            .append(", size: ")
                            .append(body.length())
                            .append(BINARY_SIZE_SUFFIX);
                } else {
                    responseContent.append(BODY_PREFIX).append(maskBodyIfEnabled(body));
                }
            }

            String finalContent = applyContentSizeLimit(responseContent.toString());

            Artifact httpResponse = Artifact.builder()
                    .name("http-response.txt")
                    .type("http-response")
                    .path("/artifacts/http/http-response.txt")
                    .mimeType(MIME_TEXT_PLAIN)
                    .size((long) finalContent.length())
                    .timestamp(Instant.now())
                    .content(finalContent)
                    .build();

            addArtifact(testName, httpResponse);
            logger.debug("Captured HTTP response for test '{}': status {}", testName, statusCode);
        } catch (Exception e) {
            logger.error("Error capturing HTTP response for test '{}'", testName, e);
        }
    }

    /**
     * Checks if the content type indicates binary content.
     */
    private boolean isBinaryContent(String contentType) {
        if (contentType == null) {
            return false;
        }

        String lowerContentType = contentType.toLowerCase();
        return lowerContentType.startsWith("image/")
                || lowerContentType.startsWith("video/")
                || lowerContentType.startsWith("audio/")
                || lowerContentType.contains("application/pdf")
                || lowerContentType.contains("application/octet-stream")
                || lowerContentType.contains("application/zip");
    }

    /**
     * Applies content size limit and adds truncation message if needed.
     */
    private String applyContentSizeLimit(String content) {
        if (content == null) {
            return "";
        }

        int maxSize = config.getMaxArtifactContentSize();
        if (content.length() <= maxSize) {
            return content;
        }

        // Truncate and add message
        String truncated = content.substring(0, maxSize);
        String truncationMessage = "\n\n[Content truncated at " + maxSize
                + " bytes. Total size: " + content.length() + BINARY_SIZE_SUFFIX;
        return truncated + truncationMessage;
    }

    /**
     * Captures HTTP request details and attaches them to the specified test.
     *
     * <p>
     * Use this method to capture the HTTP method, URL, headers, and body of an
     * API request for debugging and reporting purposes.</p>
     *
     * @param testName the name of the test to attach the request to
     * @param method the HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param url the request URL or path
     * @param headers the request headers (can be null if no headers)
     * @param body the request body (can be null for GET requests)
     * @throws IllegalArgumentException if testName, method, or url is null or
     * empty
     */
    public void captureHttpRequest(String testName, String method, String url,
            Map<String, String> headers, String body) {
        validateParameter(testName, "testName");
        validateParameter(method, "method");
        validateParameter(url, "url");

        StringBuilder requestContent = new StringBuilder();
        requestContent.append(method).append(" ").append(url).append("\n\n");

        Map<String, String> processedHeaders = maskHeadersIfEnabled(headers);
        if (processedHeaders != null && !processedHeaders.isEmpty()) {
            requestContent.append(HEADERS_PREFIX);
            processedHeaders.forEach((key, value)
                    -> requestContent.append(key).append(": ").append(value).append("\n"));
            requestContent.append("\n");
        }

        if (body != null && !body.isEmpty()) {
            requestContent.append(BODY_PREFIX).append(maskBodyIfEnabled(body));
        }

        String finalContent = requestContent.toString();

        Artifact httpRequest = Artifact.builder()
                .name("http-request.txt")
                .type("http-request")
                .path("/artifacts/http/http-request.txt")
                .mimeType(MIME_TEXT_PLAIN)
                .size((long) finalContent.length())
                .timestamp(Instant.now())
                .content(finalContent)
                .build();

        addArtifact(testName, httpRequest);
        logger.debug("Captured HTTP request for test '{}': {} {}", testName, method, url);
    }

    /**
     * Captures HTTP response details and attaches them to the specified test.
     *
     * <p>
     * Use this method to capture the HTTP status code, headers, and body of an
     * API response for debugging and reporting purposes.</p>
     *
     * @param testName the name of the test to attach the response to
     * @param statusCode the HTTP status code (200, 404, 500, etc.)
     * @param headers the response headers (can be null if no headers)
     * @param body the response body (can be null for empty responses)
     * @throws IllegalArgumentException if testName is null or empty
     */
    public void captureHttpResponse(String testName, int statusCode,
            Map<String, String> headers, String body) {
        validateParameter(testName, "testName");

        StringBuilder responseContent = new StringBuilder();
        responseContent.append("Status: ").append(statusCode).append("\n\n");

        Map<String, String> processedHeaders = maskHeadersIfEnabled(headers);
        if (processedHeaders != null && !processedHeaders.isEmpty()) {
            responseContent.append(HEADERS_PREFIX);
            processedHeaders.forEach((key, value)
                    -> responseContent.append(key).append(": ").append(value).append("\n"));
            responseContent.append("\n");
        }

        if (body != null && !body.isEmpty()) {
            responseContent.append(BODY_PREFIX).append(maskBodyIfEnabled(body));
        }

        String finalContent = responseContent.toString();

        Artifact httpResponse = Artifact.builder()
                .name("http-response.txt")
                .type("http-response")
                .path("/artifacts/http/http-response.txt")
                .mimeType(MIME_TEXT_PLAIN)
                .size((long) finalContent.length())
                .timestamp(Instant.now())
                .content(finalContent)
                .build();

        addArtifact(testName, httpResponse);
        logger.debug("Captured HTTP response for test '{}': status {}", testName, statusCode);
    }

    /**
     * Captures a JSON payload and attaches it to the specified test.
     *
     * <p>
     * Use this method to capture JSON request or response bodies as separate
     * artifacts for easier viewing and validation.</p>
     *
     * @param testName the name of the test to attach the JSON to
     * @param fileName the name of the JSON file (e.g., "request.json",
     * "response.json")
     * @param jsonContent the JSON content
     * @throws IllegalArgumentException if any parameter is null or if
     * testName/fileName is empty
     */
    public void captureJsonPayload(String testName, String fileName, String jsonContent) {
        validateParameter(testName, "testName");
        validateParameter(fileName, "fileName");
        validateParameter(jsonContent, "jsonContent");

        String finalContent = maskBodyIfEnabled(jsonContent);

        Artifact jsonPayload = Artifact.builder()
                .name(fileName)
                .type("json")
                .path("/artifacts/json/" + fileName)
                .mimeType("application/json")
                .size((long) finalContent.length())
                .timestamp(Instant.now())
                .content(finalContent)
                .build();

        addArtifact(testName, jsonPayload);
        logger.debug("Captured JSON payload '{}' for test '{}'", fileName, testName);
    }

    /**
     * Captures an XML payload and attaches it to the specified test.
     *
     * <p>
     * Use this method to capture XML request or response bodies as separate
     * artifacts for easier viewing and validation.</p>
     *
     * @param testName the name of the test to attach the XML to
     * @param fileName the name of the XML file (e.g., "request.xml",
     * "response.xml")
     * @param xmlContent the XML content
     * @throws IllegalArgumentException if any parameter is null or if
     * testName/fileName is empty
     */
    public void captureXmlPayload(String testName, String fileName, String xmlContent) {
        validateParameter(testName, "testName");
        validateParameter(fileName, "fileName");
        validateParameter(xmlContent, "xmlContent");

        String finalContent = maskBodyIfEnabled(xmlContent);

        Artifact xmlPayload = Artifact.builder()
                .name(fileName)
                .type("xml")
                .path("/artifacts/xml/" + fileName)
                .mimeType("application/xml")
                .size((long) finalContent.length())
                .timestamp(Instant.now())
                .content(finalContent)
                .build();

        addArtifact(testName, xmlPayload);
        logger.debug("Captured XML payload '{}' for test '{}'", fileName, testName);
    }

    /**
     * Records API response time metric and attaches it to the specified test.
     *
     * <p>
     * Use this method to measure and record how long an API call takes to
     * complete (from request start to response received).</p>
     *
     * @param testName the name of the test to attach the metric to
     * @param responseTimeMs the API response time in milliseconds
     * @throws IllegalArgumentException if testName is null or empty
     */
    public void recordResponseTime(String testName, double responseTimeMs) {
        validateParameter(testName, "testName");

        Metric responseTimeMetric = Metric.builder()
                .name("api.response.time")
                .value(responseTimeMs)
                .unit("ms")
                .timestamp(Instant.now())
                .build();

        addMetric(testName, responseTimeMetric);
        logger.debug("Recorded API response time for test '{}': {} ms", testName, responseTimeMs);
    }

    /**
     * Records API request size metric and attaches it to the specified test.
     *
     * <p>
     * Use this method to record the size of the HTTP request body in bytes.</p>
     *
     * @param testName the name of the test to attach the metric to
     * @param requestSizeBytes the request body size in bytes
     * @throws IllegalArgumentException if testName is null or empty
     */
    public void recordRequestSize(String testName, long requestSizeBytes) {
        validateParameter(testName, "testName");

        Metric requestSizeMetric = Metric.builder()
                .name("api.request.size")
                .value((double) requestSizeBytes)
                .unit("bytes")
                .timestamp(Instant.now())
                .build();

        addMetric(testName, requestSizeMetric);
        logger.debug("Recorded API request size for test '{}': {} bytes", testName, requestSizeBytes);
    }

    /**
     * Records API response size metric and attaches it to the specified test.
     *
     * <p>
     * Use this method to record the size of the HTTP response body in
     * bytes.</p>
     *
     * @param testName the name of the test to attach the metric to
     * @param responseSizeBytes the response body size in bytes
     * @throws IllegalArgumentException if testName is null or empty
     */
    public void recordResponseSize(String testName, long responseSizeBytes) {
        validateParameter(testName, "testName");

        Metric responseSizeMetric = Metric.builder()
                .name("api.response.size")
                .value((double) responseSizeBytes)
                .unit("bytes")
                .timestamp(Instant.now())
                .build();

        addMetric(testName, responseSizeMetric);
        logger.debug("Recorded API response size for test '{}': {} bytes", testName, responseSizeBytes);
    }

    /**
     * Validates that a parameter is not null or empty.
     *
     * @param parameter the parameter to validate
     * @param parameterName the name of the parameter (for error messages)
     * @throws IllegalArgumentException if parameter is null or empty
     */
    private void validateParameter(String parameter, String parameterName) {
        if (parameter == null) {
            throw new IllegalArgumentException(parameterName + " cannot be null");
        }
        if (parameter.trim().isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be empty");
        }
    }
}
