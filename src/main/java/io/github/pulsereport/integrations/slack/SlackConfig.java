package io.github.pulsereport.integrations.slack;

import io.github.pulsereport.integrations.PublishConfig;

/**
 * Configuration for sending notifications to Slack.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public final class SlackConfig extends PublishConfig {
    
    private final String webhookUrl;
    private final String channel;
    private final String mentionOnFailure;
    private final String reportUrl;

    private SlackConfig(Builder builder) {
        super(builder.retryAttempts, builder.retryDelayMs);
        this.webhookUrl = builder.webhookUrl;
        this.channel = builder.channel;
        this.mentionOnFailure = builder.mentionOnFailure;
        this.reportUrl = builder.reportUrl;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getChannel() {
        return channel;
    }

    public String getMentionOnFailure() {
        return mentionOnFailure;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    /**
     * Creates a new builder for SlackConfig.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for SlackConfig.
     */
    public static class Builder {
        private String webhookUrl;
        private String channel;
        private String mentionOnFailure;
        private String reportUrl;
        private int retryAttempts = 3;
        private long retryDelayMs = 1000;

        public Builder webhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder mentionOnFailure(String mentionOnFailure) {
            this.mentionOnFailure = mentionOnFailure;
            return this;
        }

        public Builder reportUrl(String reportUrl) {
            this.reportUrl = reportUrl;
            return this;
        }

        public Builder retryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
            return this;
        }

        public Builder retryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
            return this;
        }

        public SlackConfig build() {
            if (webhookUrl == null || webhookUrl.isEmpty()) {
                throw new IllegalArgumentException("Webhook URL is required");
            }
            return new SlackConfig(this);
        }
    }
}
