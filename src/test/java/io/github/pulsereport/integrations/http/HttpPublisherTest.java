package io.github.pulsereport.integrations.http;

import java.io.File;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pulsereport.integrations.PublishException;

/**
 * Tests for HttpPublisher.
 */
class HttpPublisherTest {

    @TempDir
    File tempDir;

    private HttpClient mockHttpClient;
    private HttpPublisher publisher;
    private HttpPublishConfig httpConfig;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);

        httpConfig = HttpPublishConfig.builder()
                .endpoint("https://api.example.com/reports")
                .method("POST")
                .retryAttempts(3)
                .retryDelayMs(100)
                .build();

        publisher = new HttpPublisher(mockHttpClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void publishFile_success() throws Exception {
        File reportFile = new File(tempDir, "report.json");
        Files.writeString(reportFile.toPath(), "{\"test\": \"data\"}");

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertDoesNotThrow(() -> publisher.publish(reportFile, httpConfig));

        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void publishBytes_success() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(201);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertDoesNotThrow(() -> publisher.publish(reportData, fileName, httpConfig));

        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void publish_withHeaders() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Build-Id", "12345");
        headers.put("X-Branch", "main");

        HttpPublishConfig configWithHeaders = HttpPublishConfig.builder()
                .endpoint("https://api.example.com/reports")
                .method("POST")
                .headers(headers)
                .build();

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        publisher.publish(reportData, fileName, configWithHeaders);

        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void publish_withBearerAuth() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        HttpPublishConfig configWithAuth = HttpPublishConfig.builder()
                .endpoint("https://api.example.com/reports")
                .method("POST")
                .bearerToken("test-token-123")
                .build();

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        publisher.publish(reportData, fileName, configWithAuth);

        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void publish_withBasicAuth() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        HttpPublishConfig configWithAuth = HttpPublishConfig.builder()
                .endpoint("https://api.example.com/reports")
                .method("POST")
                .username("user")
                .password("pass")
                .build();

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        publisher.publish(reportData, fileName, configWithAuth);

        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void publish_retryOn5xx() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        HttpResponse<String> failResponse = mock(HttpResponse.class);
        when(failResponse.statusCode()).thenReturn(503);

        HttpResponse<String> successResponse = mock(HttpResponse.class);
        when(successResponse.statusCode()).thenReturn(200);

        // Fail twice, then succeed
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(failResponse)
                .thenReturn(failResponse)
                .thenReturn(successResponse);

        assertDoesNotThrow(() -> publisher.publish(reportData, fileName, httpConfig));

        verify(mockHttpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void publish_failOn4xx() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        PublishException ex4xx = assertThrows(PublishException.class, () -> publisher.publish(reportData, fileName, httpConfig));
        assertNotNull(ex4xx.getMessage());

        // Should not retry on 4xx
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void publish_failAfterRetries() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        PublishException ex5xx = assertThrows(PublishException.class, () -> publisher.publish(reportData, fileName, httpConfig));
        assertNotNull(ex5xx.getMessage());

        verify(mockHttpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void httpPublishConfig() {
        HttpPublishConfig config = HttpPublishConfig.builder()
                .endpoint("https://example.com")
                .method("PUT")
                .bearerToken("token")
                .retryAttempts(5)
                .retryDelayMs(2000)
                .build();

        assertEquals("https://example.com", config.getEndpoint());
        assertEquals("PUT", config.getMethod());
        assertEquals("token", config.getBearerToken());
        assertEquals(5, config.getRetryAttempts());
        assertEquals(2000, config.getRetryDelayMs());
    }
}
