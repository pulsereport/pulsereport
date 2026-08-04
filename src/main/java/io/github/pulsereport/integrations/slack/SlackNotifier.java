package io.github.pulsereport.integrations.slack;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.integrations.PublishException;

/**
 * Notifier for sending test result summaries to Slack via webhook. Supports
 * rich formatting with colors and mentions.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class SlackNotifier {

    private static final Logger logger = LoggerFactory.getLogger(SlackNotifier.class);
    private static final long MAX_DELAY_MS = 10000;

    private final HttpClient httpClient;

    /**
     * Constructs a SlackNotifier with the specified HTTP client.
     *
     * @param httpClient the HTTP client
     */
    public SlackNotifier(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Constructs a SlackNotifier with a default HTTP client.
     */
    public SlackNotifier() {
        this(HttpClient.newHttpClient());
    }

    /**
     * Sends a test run notification to Slack.
     *
     * @param testRun the test run to report
     * @param config the Slack configuration
     * @throws PublishException if notification fails after retries
     */
    public void notify(TestRun testRun, SlackConfig config) throws PublishException {
        String message = buildSlackMessage(testRun, config);

        int attempt = 0;
        long delay = config.getRetryDelayMs();
        Exception lastException = null;

        while (attempt < config.getRetryAttempts()) {
            try {
                sendSlackMessage(message, config);
                logger.info("Successfully sent notification to Slack");
                return;
            } catch (IOException | InterruptedException e) {
                lastException = e;
                attempt++;

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new PublishException("Notification interrupted", e);
                }

                if (attempt >= config.getRetryAttempts()) {
                    break;
                }

                logger.warn("Slack notification failed (attempt {}/{}): {}", attempt, config.getRetryAttempts(), e.getMessage());

                try {
                    Thread.sleep(Math.min(delay, MAX_DELAY_MS));
                    delay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new PublishException("Notification interrupted", ie);
                }
            }
        }

        throw new PublishException(
                "Failed to send Slack notification after %d attempts".formatted(config.getRetryAttempts()),
                lastException);
    }

    private String buildSlackMessage(TestRun testRun, SlackConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        if (config.getChannel() != null && !config.getChannel().isEmpty()) {
            sb.append("\"channel\":\"").append(escapeJson(config.getChannel())).append("\",");
        }

        sb.append("\"text\":\"Test Run: ").append(escapeJson(testRun.getName())).append("\",");

        String color = testRun.getStatus() == TestStatus.PASSED ? "good"
                : testRun.getStatus() == TestStatus.FAILED ? "danger" : "warning";

        sb.append("\"attachments\":[{");
        sb.append("\"color\":\"").append(color).append("\",");
        sb.append("\"fields\":[");
        sb.append("{\"title\":\"Status\",\"value\":\"").append(testRun.getStatus()).append("\",\"short\":true},");
        sb.append("{\"title\":\"Total Tests\",\"value\":\"").append(testRun.getTotalTests()).append("\",\"short\":true},");
        sb.append("{\"title\":\"Passed\",\"value\":\"").append(testRun.getPassedTests()).append("\",\"short\":true},");
        sb.append("{\"title\":\"Failed\",\"value\":\"").append(testRun.getFailedTests()).append("\",\"short\":true},");
        sb.append("{\"title\":\"Skipped\",\"value\":\"").append(testRun.getSkippedTests()).append("\",\"short\":true},");
        sb.append("{\"title\":\"Duration\",\"value\":\"").append(formatDuration(testRun.getDuration())).append("\",\"short\":true}");
        sb.append("]");

        if (config.getReportUrl() != null && !config.getReportUrl().isEmpty()) {
            sb.append(",\"actions\":[{\"type\":\"button\",\"text\":\"View Report\",\"url\":\"")
                    .append(escapeJson(config.getReportUrl())).append("\"}]");
        }

        sb.append("}]");

        if (testRun.getStatus() == TestStatus.FAILED
                && config.getMentionOnFailure() != null && !config.getMentionOnFailure().isEmpty()) {
            sb.insert(sb.indexOf("\"text\":\"") + 8, config.getMentionOnFailure() + " ");
        }

        sb.append("}");

        return sb.toString();
    }

    private void sendSlackMessage(String message, SlackConfig config) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getWebhookUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Slack webhook returned status: " + response.statusCode());
        }
    }

    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return "%dh %dm %ds".formatted(hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return "%dm %ds".formatted(minutes, seconds % 60);
        } else {
            return "%ds".formatted(seconds);
        }
    }
}
