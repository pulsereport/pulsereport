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

#### Basic Authentication and Custom Headers

The configuration file only reads `reporter.http.auth.type` and `reporter.http.auth.token` (bearer token). Basic authentication, API-key headers, and any other custom headers are configured in code on `HttpPublishConfig`:

```java
HttpPublishConfig config = HttpPublishConfig.builder()
    .endpoint("https://api.example.com/test-reports")
    .method("POST")
    .headers(Map.of(
        "Authorization", "Basic " + Base64.getEncoder().encodeToString(
            (user + ":" + password).getBytes(StandardCharsets.UTF_8)),
        "X-API-Key", System.getenv("API_KEY"),
        "X-Build-ID", buildId))
    .build();
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
import json
from flask import Flask, request

app = Flask(__name__)

@app.route('/test-reports', methods=['POST'])
def receive_report():
    # HttpPublisher sends the report file bytes as application/octet-stream
    report = json.loads(request.data)
    # The JSON root is the TestRun itself, with flat summary fields
    print(f"Received report: {report['name']}")
    print(f"Total tests: {report['totalTests']}")
    print(f"Failed tests: {report['failedTests']}")
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

# Mention a user or group when the run fails (optional)
reporter.slack.mentionOnFailure=@qa-team
# Or user: reporter.slack.mentionOnFailure=@john.doe

# Optional report URL attached to the message as a "View Report" button
reporter.slack.reportUrl=https://reports.example.com/build-123/test-report.html
```

`ReporterConfig` currently reads the Slack enabled flag, webhook URL, channel, mention-on-failure value, and report URL from the file. Retry behavior is tuned on `SlackConfig` in code.

### Message Format

`SlackNotifier` posts an attachments-style JSON payload to the webhook. The message text is `Test Run: <run name>` (prefixed with the `mentionOnFailure` value when the run failed), and the attachment color is `good` when the run passed, `danger` when it failed, and `warning` otherwise. The attachment carries six short fields and an optional button:

```json
{
  "channel": "#test-results",
  "text": "Test Run: My Test Suite",
  "attachments": [
    {
      "color": "good",
      "fields": [
        { "title": "Status", "value": "PASSED", "short": true },
        { "title": "Total Tests", "value": "150", "short": true },
        { "title": "Passed", "value": "142", "short": true },
        { "title": "Failed", "value": "0", "short": true },
        { "title": "Skipped", "value": "2", "short": true },
        { "title": "Duration", "value": "2m 45s", "short": true }
      ],
      "actions": [
        {
          "type": "button",
          "text": "View Report",
          "url": "https://reports.example.com/build-123/test-report.html"
        }
      ]
    }
  ]
}
```

Notes on the payload:

- `channel` is only included when a channel is configured.
- The `View Report` button action is only included when `reportUrl` is set.
- The `mentionOnFailure` mention is prepended to the message text only when the run status is `FAILED`.
- The payload contains no emojis, percentages, or failed-test lists.

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

---

## Integration Patterns

### Sequential Publishing

Publish to multiple integrations by calling each publisher explicitly, in whatever order fits your pipeline:

```java
s3Publisher.publish(reportFile, s3Config);       // S3
httpPublisher.publish(reportFile, httpConfig);   // HTTP endpoint
slackNotifier.notify(testRun, slackConfig);      // Slack notification
```

There is no automatic publish pipeline: setting `reporter.s3.enabled`, `reporter.http.enabled`, or `reporter.slack.enabled` in a configuration file does not trigger publishing on its own. Your build helper decides the order and the error handling.

### With Report URL in Slack

`S3Publisher` does not generate a public or pre-signed URL for the uploaded object. Construct the report URL yourself (from bucket, region, key prefix, and file name) and pass it to Slack:

```java
String reportUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/"
        + keyPrefix + reportFile.getName();

SlackConfig slackConfig = SlackConfig.builder()
    .webhookUrl(System.getenv("SLACK_WEBHOOK_URL"))
    .channel("#test-results")
    .reportUrl(reportUrl) // adds a "View Report" button to the message
    .build();
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

**Problem**: HTTP publish times out or the endpoint is flaky.

**Solution**: Test the endpoint directly:

```bash
curl -X POST https://api.example.com/reports \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
```

Then tune retries on `HttpPublishConfig` (retry settings are a code-only API, not file properties):

```java
HttpPublishConfig config = HttpPublishConfig.builder()
    .endpoint("https://api.example.com/test-reports")
    .method("POST")
    .retryAttempts(3)
    .retryDelayMs(1000) // doubles on each retry
    .build();
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

---

## Best Practices

### 1. Use Environment Variables

Never hardcode credentials:

```properties
# Good
reporter.slack.webhookUrl=${SLACK_WEBHOOK_URL}
reporter.http.auth.token=${API_TOKEN}

# Bad
reporter.slack.webhookUrl=https://hooks.slack.com/services/T00/B00/xxxx
```

AWS credentials are not read from the properties file at all — provide them through the standard AWS SDK credential chain (environment variables, system properties, shared credentials file, or IAM roles).

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

Publishing is explicit, so decide per integration whether a failure should fail your build. Wrap each publish call and catch `PublishException`:

```java
try {
    publisher.publish(reportFile, config);
} catch (PublishException e) {
    System.err.println("Publish failed (continuing build): " + e.getMessage());
}
```

### 4. Rate Limiting and Retries

`SlackNotifier` and `HttpPublisher` already retry with exponential backoff. Tune the attempts and initial delay in code:

```java
SlackConfig config = SlackConfig.builder()
    .webhookUrl(System.getenv("SLACK_WEBHOOK_URL"))
    .retryAttempts(3)
    .retryDelayMs(1000)
    .build();
```

---

## Next Steps

- [Configuration Reference](configuration.md) - All configuration options
- [CLI Reference](cli-reference.md) - Publish reports from command line
- [Output Formats](output-formats.md) - Report formats for publishing
