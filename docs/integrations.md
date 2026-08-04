# Integrations Guide

PulseReport integrations move run evidence beyond the local workspace so teams can keep signal visible in storage, APIs, and team channels. This guide covers the supported publishing flows and the configuration you need for reliable release confidence.

## Overview

PulseReport supports three integration paths:

| Integration | Purpose | Use Case |
| ----------- | ------- | -------- |
| **S3** | Store reports in AWS S3 | Long-term archival, team access |
| **HTTP** | POST reports to any endpoint | Custom dashboards, databases |
| **Slack** | Send notifications to Slack | Real-time alerts, team updates |

---

## AWS S3 Integration

Upload test reports to Amazon S3 for centralized storage and access.

### Prerequisites

- AWS Account
- S3 Bucket created
- AWS credentials (access key + secret key)
- Appropriate IAM permissions

### IAM Permissions Required

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:PutObjectAcl"
      ],
      "Resource": "arn:aws:s3:::your-bucket-name/*"
    }
  ]
}
```

### S3 Configuration

In an explicitly loaded PulseReport configuration file (`reporter.properties`):

```properties
# Enable S3 publishing
reporter.s3.enabled=true

# S3 bucket details
reporter.s3.bucket=my-test-reports
reporter.s3.region=us-east-1
reporter.s3.keyPrefix=builds/${BUILD_ID}/
```

`ReporterConfig` currently reads the S3 enabled flag, bucket, region, and key prefix from the file. AWS credentials and any custom endpoint configuration are handled by the `S3Client` you create in code.

### Environment Variables

For security, use environment variables for credentials:

```bash
# Set in shell
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# Or in CI/CD pipeline
# Jenkins: Use "Credentials" plugin
# GitLab CI: Use "Variables" in project settings
# GitHub Actions: Use "Secrets" in repository settings
```

### S3 Programmatic Usage

```java
import io.github.pulsereport.integrations.s3.S3PublishConfig;
import io.github.pulsereport.integrations.s3.S3Publisher;
import java.io.File;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

S3Client s3Client = S3Client.builder()
  .region(Region.US_EAST_1)
  .credentialsProvider(StaticCredentialsProvider.create(
    AwsBasicCredentials.create(
      System.getenv("AWS_ACCESS_KEY_ID"),
      System.getenv("AWS_SECRET_ACCESS_KEY"))))
  .build();

S3PublishConfig config = S3PublishConfig.builder()
  .bucketName("my-test-reports")
    .keyPrefix("builds/" + buildId + "/")
  .region("us-east-1")
  .metadata(Map.of("buildId", buildId, "branch", "main"))
    .build();

S3Publisher publisher = new S3Publisher(s3Client);

File reportFile = new File("target/pulsereport/test-report.html");
publisher.publish(reportFile, config);

System.out.println("Report published to bucket: " + config.getBucketName());
```

### Key Prefix Patterns

Use dynamic key patterns for organization:

```properties
# By build ID and date
reporter.s3.keyPrefix=builds/${BUILD_ID}/${DATE}/

# By branch and build
reporter.s3.keyPrefix=${GIT_BRANCH}/${BUILD_NUMBER}/

# By date hierarchy
reporter.s3.keyPrefix=${YEAR}/${MONTH}/${DAY}/${BUILD_ID}/

# Variables resolved from environment
```

### S3 Report URLs

`S3Publisher` uploads the object but does not generate a public or pre-signed URL. If your bucket policy allows direct access, you can construct a URL from the bucket, region, key prefix, and file name after publish.

### S3-Compatible Services

PulseReport can also work with S3-compatible services when you provide an `S3Client` configured for that endpoint:

```java
S3Client s3Client = S3Client.builder()
  .region(Region.US_EAST_1)
  // Add endpointOverride(...) and credentials as needed for your provider.
  .build();
```

---

## HTTP Integration

POST test reports to any HTTP endpoint for custom processing.

### HTTP Configuration

In an explicitly loaded PulseReport configuration file (`reporter.properties`):

```properties
# Enable HTTP publishing
reporter.http.enabled=true

# Endpoint URL
reporter.http.url=https://api.example.com/test-reports

# HTTP method
reporter.http.method=POST
# Options: POST, PUT

# Bearer token auth, if used
reporter.http.auth.type=bearer
reporter.http.auth.token=${API_TOKEN}
```

`ReporterConfig` currently reads the HTTP enabled flag, endpoint URL, method, auth type, and bearer token from the file. If you need custom headers or basic authentication, set those on `HttpPublishConfig` in code.

### Authentication

#### Bearer Token

```properties
reporter.http.auth.type=bearer
reporter.http.auth.token=${API_TOKEN}
```

#### Basic Authentication

```properties
reporter.http.auth.type=basic
reporter.http.auth.username=${API_USERNAME}
reporter.http.auth.password=${API_PASSWORD}
```

#### API Key Header

```properties
reporter.http.headers.X-API-Key=${API_KEY}
```

#### Custom Headers

```properties
reporter.http.headers.X-Custom-Header=CustomValue
reporter.http.headers.X-Build-ID=${BUILD_ID}
```

### Request Payload

`HttpPublisher` sends the report bytes you provide. It does not serialize `TestRun` automatically. The request uses:

- `Content-Type: application/octet-stream`
- `Content-Disposition: attachment; filename="..."`

### HTTP Programmatic Usage

```java
import io.github.pulsereport.integrations.PublishException;
import io.github.pulsereport.integrations.http.HttpPublishConfig;
import io.github.pulsereport.integrations.http.HttpPublisher;
import java.io.File;
import java.util.Map;

HttpPublishConfig config = HttpPublishConfig.builder()
    .endpoint("https://api.example.com/test-reports")
    .method("POST")
    .bearerToken(System.getenv("API_TOKEN"))
    .headers(Map.of("X-Build-ID", buildId))
    .retryAttempts(3)
    .retryDelayMs(1000)
    .build();

HttpPublisher publisher = new HttpPublisher();

File reportFile = new File("target/pulsereport/test-report.json");
publisher.publish(reportFile, config);

System.out.println("Report posted to: " + config.getEndpoint());
```

### Response Handling

```java
try {
    publisher.publish(reportFile, config);
    System.out.println("Report published successfully");
} catch (PublishException e) {
    System.err.println("Failed to publish: " + e.getMessage());
}
```

### Example Endpoints

#### Webhook.site (Testing)

```properties
# Test HTTP publishing with webhook.site
reporter.http.url=https://webhook.site/your-unique-url
reporter.http.method=POST
```

#### Custom API Server

```python
# Simple Flask server to receive reports
from flask import Flask, request

app = Flask(__name__)

@app.route('/test-reports', methods=['POST'])
def receive_report():
    report = request.json
    print(f"Received report: {report['testRun']['name']}")
    print(f"Total tests: {report['testRun']['summary']['totalTests']}")
    return {"status": "received"}, 200

if __name__ == '__main__':
    app.run(port=8080)
```

---

## Slack Integration

Send test result notifications to Slack channels.

### Slack Prerequisites

- Slack Workspace
- Incoming Webhook URL
- Channel to post to

### Create Slack Webhook

1. Go to <https://api.slack.com/apps>
2. Click "Create New App"
3. Choose "From scratch"
4. Name your app (e.g., "Test Reporter Bot")
5. Select your workspace
6. Navigate to "Incoming Webhooks"
7. Activate Incoming Webhooks
8. Click "Add New Webhook to Workspace"
9. Select channel (e.g., #test-results)
10. Copy the webhook URL

### Slack Configuration

In the PulseReport configuration file (`reporter.properties`):

```properties
# Enable Slack notifications
reporter.slack.enabled=true

# Webhook URL (use environment variable for security)
reporter.slack.webhookUrl=${SLACK_WEBHOOK_URL}

# Channel override (optional - webhook has default channel)
reporter.slack.channel=#test-results

# Bot name and icon
reporter.slack.username=Test Reporter Bot
reporter.slack.iconEmoji=:robot_face:
# Or use icon URL: reporter.slack.iconUrl=https://example.com/icon.png

# Notification settings
reporter.slack.notifyOnSuccess=true
reporter.slack.notifyOnFailure=true
reporter.slack.mentionOnFailure=@qa-team
# Or user: reporter.slack.mentionOnFailure=@john.doe

# Message formatting
reporter.slack.includeStackTrace=false
reporter.slack.maxStackTraceLines=10
```

### Message Format

#### Success Message

```text
✅ Test Run Passed
Suite: My Test Suite
Total: 150 tests
✅ Passed: 142 (94.7%)
⊘ Skipped: 2 (1.3%)
⏱ Duration: 2m 45s

View Report: http://reports.example.com/build-123
```

#### Failure Message

```text
❌ Test Run Failed
Suite: My Test Suite
Total: 150 tests
✅ Passed: 142 (94.7%)
❌ Failed: 6 (4.0%)
⊘ Skipped: 2 (1.3%)
⏱ Duration: 2m 45s

Failed Tests:
• testLoginWithInvalidCredentials
• testDatabaseConnection
• testApiTimeout

@qa-team - Please investigate

View Report: http://reports.example.com/build-123
```

### Slack Programmatic Usage

```java
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.integrations.slack.SlackConfig;
import io.github.pulsereport.integrations.slack.SlackNotifier;

SlackConfig config = SlackConfig.builder()
    .webhookUrl(System.getenv("SLACK_WEBHOOK_URL"))
    .channel("#test-results")
    .mentionOnFailure("@qa-team")
  .reportUrl("https://reports.example.com/build-123/test-report.html")
    .build();

SlackNotifier notifier = new SlackNotifier();

TestRun testRun = adapter.getTestRun();
notifier.notify(testRun, config);
```

### Custom Message Templates

Create a custom message template:

```properties
reporter.slack.messageTemplate=/path/to/slack-template.json
```

Template file (`slack-template.json`):

```json
{
  "text": "Test Results for ${PROJECT_NAME}",
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "*Test Run: ${TEST_RUN_NAME}*\nStatus: ${STATUS}"
      }
    },
    {
      "type": "section",
      "fields": [
        {"type": "mrkdwn", "text": "*Total:*\n${TOTAL_TESTS}"},
        {"type": "mrkdwn", "text": "*Passed:*\n${PASSED_TESTS}"},
        {"type": "mrkdwn", "text": "*Failed:*\n${FAILED_TESTS}"},
        {"type": "mrkdwn", "text": "*Duration:*\n${DURATION}"}
      ]
    },
    {
      "type": "actions",
      "elements": [
        {
          "type": "button",
          "text": {"type": "plain_text", "text": "View Report"},
          "url": "${REPORT_URL}"
        }
      ]
    }
  ]
}
```

### Conditional Notifications

Only notify on specific conditions:

```properties
# Only notify on failures
reporter.slack.notifyOnSuccess=false
reporter.slack.notifyOnFailure=true

# Only notify if failure rate > 5%
reporter.slack.notifyThreshold.failureRate=5.0

# Only notify if duration > 10 minutes
reporter.slack.notifyThreshold.durationMinutes=10
```

### Multiple Channels

Send different messages to different channels:

```properties
# Success notifications to general channel
reporter.slack.success.webhookUrl=${SLACK_GENERAL_WEBHOOK}
reporter.slack.success.channel=#general

# Failure notifications to QA team channel
reporter.slack.failure.webhookUrl=${SLACK_QA_WEBHOOK}
reporter.slack.failure.channel=#qa-alerts
reporter.slack.failure.mention=@qa-team
```

---

## Integration Patterns

### Sequential Publishing

Publish to multiple integrations in sequence:

```properties
# Enable all integrations
reporter.s3.enabled=true
reporter.http.enabled=true
reporter.slack.enabled=true

# They execute in this order:
# 1. Generate reports
# 2. Publish to S3
# 3. Publish to HTTP endpoint
# 4. Send Slack notification
```

### With Report URL in Slack

Include S3 URL in Slack notification:

```properties
reporter.s3.enabled=true
reporter.s3.bucket=my-reports
reporter.s3.generatePublicUrl=true

reporter.slack.enabled=true
reporter.slack.includeReportUrl=true
# Slack message will include S3 URL
```

### CI/CD Integration Pattern

```bash
#!/bin/bash
# CI/CD script with PulseReport integrations

# Set environment variables
export AWS_ACCESS_KEY_ID=$CI_AWS_KEY
export AWS_SECRET_ACCESS_KEY=$CI_AWS_SECRET
export SLACK_WEBHOOK_URL=$CI_SLACK_WEBHOOK
export BUILD_ID=$CI_BUILD_ID
export REPORT_URL="https://my-reports.s3.amazonaws.com/builds/${BUILD_ID}/test-report.html"

# Run tests
mvn test

# Publishing is still an explicit step. Creating `reporter.properties` alone does not auto-publish reports.
# Call the S3Publisher, HttpPublisher, or SlackNotifier APIs from your build helper after reports are generated.

# Exit with test status
exit $?
```

---

## Troubleshooting

### S3: Access Denied

**Problem**: `AccessDenied: User does not have permission`

**Solution**: Verify IAM permissions include `s3:PutObject`

### HTTP: Connection Timeout

**Problem**: HTTP publish times out.

**Solution**: Increase timeout:

```properties
reporter.http.timeout=60000
```

### Slack: Invalid Webhook URL

**Problem**: `invalid_payload` or `channel_not_found`

**Solution**: Verify webhook URL and channel name:

```bash
# Test webhook with curl
curl -X POST -H 'Content-type: application/json' \
  --data '{"text":"Test message"}' \
  $SLACK_WEBHOOK_URL
```

### S3: Slow Upload

**Problem**: Large reports take long to upload.

**Solution**: Enable multipart upload:

```properties
reporter.s3.multipart.enabled=true
reporter.s3.multipart.partSize=5242880
```

### HTTP: SSL Certificate Issues

**Problem**: `SSLHandshakeException` or certificate errors

**Solution**: For self-signed certificates (NOT recommended for production):

```properties
reporter.http.ssl.verify=false
```

---

## Best Practices

### 1. Use Environment Variables

Never hardcode credentials:

```properties
# Good
reporter.s3.accessKey=${AWS_ACCESS_KEY_ID}
reporter.slack.webhookUrl=${SLACK_WEBHOOK_URL}

# Bad
reporter.s3.accessKey=AKIAIOSFODNN7EXAMPLE
```

### 2. Organize S3 Structure

Use meaningful key prefixes:

```text
builds/
├── 2026/
│   └── 02/
│       └── 16/
│           └── build-123/
│               ├── test-report.html
│               ├── test-report.json
│               └── screenshots/
```

### 3. Error Handling

Don't fail builds if publishing fails:

```properties
reporter.failOnPublishError=false
```

### 4. Rate Limiting

Respect Slack rate limits:

```properties
reporter.slack.rateLimit.enabled=true
reporter.slack.rateLimit.perMinute=60
```

---

## Next Steps

- [Configuration Reference](configuration.md) - All configuration options
- [CLI Reference](cli-reference.md) - Publish reports from command line
- [Output Formats](output-formats.md) - Report formats for publishing
