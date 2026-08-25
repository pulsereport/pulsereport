package io.github.pulsereport.integrations.slack;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.integrations.PublishException;

/**
 * Tests for SlackNotifier.
 */
class SlackNotifierTest {

    private HttpClient mockHttpClient;
    private SlackNotifier notifier;
    private SlackConfig slackConfig;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);

        slackConfig = SlackConfig.builder()
                .webhookUrl("https://hooks.slack.com/services/TEST/WEBHOOK/URL")
                .channel("#test-results")
                .retryAttempts(3)
                .retryDelayMs(100)
                .build();

        notifier = new SlackNotifier(mockHttpClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void notify_success() throws Exception {
        TestRun testRun = TestRun.builder()
                .id("test-run-1")
                .name("Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(10))
                .duration(10000)
                .status(TestStatus.PASSED)
                .suites(Collections.emptyList())
                .totalTests(10)
                .passedTests(10)
                .failedTests(0)
                .skippedTests(0)
                .build();

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        assertDoesNotThrow(() -> notifier.notify(testRun, slackConfig));

        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void notify_withReportUrl() throws Exception {
        TestRun testRun = TestRun.builder()
                .id("test-run-1")
                .name("Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(10))
                .duration(10000)
                .status(TestStatus.PASSED)
                .suites(Collections.emptyList())
                .totalTests(10)
                .passedTests(10)
                .failedTests(0)
                .skippedTests(0)
                .build();

        SlackConfig configWithUrl = SlackConfig.builder()
                .webhookUrl("https://hooks.slack.com/services/TEST/WEBHOOK/URL")
                .channel("#test-results")
                .reportUrl("https://reports.example.com/report.html")
                .build();

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        notifier.notify(testRun, configWithUrl);

        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void notify_withMentions() throws Exception {
        TestRun testRun = TestRun.builder()
                .id("test-run-1")
                .name("Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(10))
                .duration(10000)
                .status(TestStatus.FAILED)
                .suites(Collections.emptyList())
                .totalTests(10)
                .passedTests(8)
                .failedTests(2)
                .skippedTests(0)
                .build();

        SlackConfig configWithMentions = SlackConfig.builder()
                .webhookUrl("https://hooks.slack.com/services/TEST/WEBHOOK/URL")
                .channel("#test-results")
                .mentionOnFailure("@channel")
                .build();

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        notifier.notify(testRun, configWithMentions);

        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void notify_retryOnFailure() throws Exception {
        TestRun testRun = TestRun.builder()
                .id("test-run-1")
                .name("Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(10))
                .duration(10000)
                .status(TestStatus.PASSED)
                .suites(Collections.emptyList())
                .totalTests(10)
                .passedTests(10)
                .failedTests(0)
                .skippedTests(0)
                .build();

        HttpResponse<String> failResponse = mock(HttpResponse.class);
        when(failResponse.statusCode()).thenReturn(500);

        HttpResponse<String> successResponse = mock(HttpResponse.class);
        when(successResponse.statusCode()).thenReturn(200);

        // Fail twice, then succeed
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(failResponse)
                .thenReturn(failResponse)
                .thenReturn(successResponse);

        assertDoesNotThrow(() -> notifier.notify(testRun, slackConfig));

        verify(mockHttpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void notify_failAfterRetries() throws Exception {
        TestRun testRun = TestRun.builder()
                .id("test-run-1")
                .name("Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(10))
                .duration(10000)
                .status(TestStatus.PASSED)
                .suites(Collections.emptyList())
                .totalTests(10)
                .passedTests(10)
                .failedTests(0)
                .skippedTests(0)
                .build();

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        PublishException ex = assertThrows(PublishException.class, () -> notifier.notify(testRun, slackConfig));
        assertNotNull(ex.getMessage());

        verify(mockHttpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void slackConfig() {
        SlackConfig config = SlackConfig.builder()
                .webhookUrl("https://hooks.slack.com/test")
                .channel("#alerts")
                .mentionOnFailure("@here")
                .reportUrl("https://example.com/report")
                .retryAttempts(5)
                .retryDelayMs(2000)
                .build();

        assertEquals("https://hooks.slack.com/test", config.getWebhookUrl());
        assertEquals("#alerts", config.getChannel());
        assertEquals("@here", config.getMentionOnFailure());
        assertEquals("https://example.com/report", config.getReportUrl());
        assertEquals(5, config.getRetryAttempts());
        assertEquals(2000, config.getRetryDelayMs());
    }
}
