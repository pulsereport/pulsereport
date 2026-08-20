# Configuration Reference

Use PulseReport configuration to control how the reporting layer captures evidence, writes outputs, and publishes run health signals. This reference keeps the examples aligned with the current PulseReport defaults and naming.

## Configuration Files

### PulseReport configuration file (`reporter.properties`)

PulseReport includes `src/main/resources/reporter.properties` as a sample starter file. **All adapters** (RestAssured, Cucumber, TestNG, Appium) and the CLI auto-detect this file — no client-side configuration code is needed when `reporter.properties` is on the classpath or in the working directory. Auto-detection searches in:

1. `./reporter.properties` (working directory / project root)
2. `./src/main/resources/reporter.properties`
3. The classpath (`/reporter.properties`)

You can also load it explicitly:

1. Pass it to the CLI with `--config /path/to/reporter.properties`
2. Load it in Java with `ReporterConfig.loadFromFile(new File("/path/to/reporter.properties"))`

```properties
# Example reporter.properties
reporter.output.formats=html,json,junit
reporter.output.directory=target/pulsereport
```

### Loading Configuration

`ReporterConfig` supports three loading paths:

1. Build a configuration in code with `ReporterConfig.builder()`
2. Load a specific properties file with `ReporterConfig.loadFromFile(...)`
3. Auto-detect from well-known locations with `ReporterConfig.autoDetect()`

When loading a file, missing keys keep the class defaults. Placeholder values such as `${AWS_ACCESS_KEY_ID}` are interpolated by checking a Java system property with the same name first, then an environment variable with the same name.

### Properties Loaded by `ReporterConfig`

The current `ReporterConfig` implementation actively reads these keys:

- `reporter.output.formats`
- `reporter.output.directory`
- `reporter.s3.enabled`
- `reporter.s3.bucket`
- `reporter.s3.region`
- `reporter.s3.keyPrefix`
- `reporter.http.enabled`
- `reporter.http.url`
- `reporter.http.method`
- `reporter.http.auth.type`
- `reporter.http.auth.token`
- `reporter.slack.enabled`
- `reporter.slack.webhookUrl`
- `reporter.slack.channel`
- `reporter.slack.mentionOnFailure`
- `reporter.slack.reportUrl`
- `reporter.maxArtifactContentSize`
- `reporter.maskSensitiveData`
- `reporter.maskHeaders.enabled`
- `reporter.maskHeaders.fields`
- `reporter.maskBody.enabled`
- `reporter.maskBody.fields`
- `reporter.maskBody.maskTokens`
- `reporter.maskXml.enabled`
- `reporter.maskXml.fields`

The masking keys are described in detail in [Sensitive Data Masking](#sensitive-data-masking).

Other property examples in this document preserve the current PulseReport naming, but they are not all mapped by `ReporterConfig` yet.

## Core Settings

### Reporter Metadata

```properties
# Reporter name shown in report headers
reporter.name=PulseReport

# Reporter version (for tracking)
reporter.version=1.0.0

# Enable/disable reporter globally
reporter.enabled=true
# Set to false to disable all reporting
```

### Output Settings

```properties
# Output formats (comma-separated)
reporter.output.formats=html,json,junit
# Available: html, json, junit

# Output directory
reporter.output.directory=target/pulsereport

# Create output directory if missing
reporter.output.createDirectory=true

# Overwrite existing reports
reporter.output.overwrite=true
```

## Adapter Configuration

### TestNG Adapter

```properties
# Enable TestNG adapter
reporter.testng.enabled=true

# Capture test parameters
reporter.testng.captureParameters=true

# Capture test groups
reporter.testng.captureGroups=true

# Capture configuration methods
reporter.testng.captureConfigMethods=false
# @BeforeMethod, @AfterMethod, etc.
```

### Selenium Adapter

```properties
# Enable Selenium adapter
reporter.selenium.enabled=true

# Screenshot settings
reporter.selenium.screenshot.enabled=true
reporter.selenium.screenshot.onSuccess=true
reporter.selenium.screenshot.onFailure=true
reporter.selenium.screenshot.onSkip=false
reporter.selenium.screenshot.format=png
# Options: png, jpg

# Screenshot quality (for jpg)
reporter.selenium.screenshot.quality=90
# Range: 1-100

# Screenshot compression
reporter.selenium.screenshot.compress=true
reporter.selenium.screenshot.maxWidth=1920
reporter.selenium.screenshot.maxHeight=1080

# Page metrics
reporter.selenium.metrics.pageLoad=true
reporter.selenium.metrics.domReady=true
reporter.selenium.metrics.firstContentfulPaint=true

# Browser info capture
reporter.selenium.captureBrowserInfo=true
```

### Appium Adapter

```properties
# Enable Appium adapter
reporter.appium.enabled=true

# Screenshot settings
reporter.appium.screenshot.enabled=true
reporter.appium.screenshot.onSuccess=true
reporter.appium.screenshot.onFailure=true
reporter.appium.screenshot.format=png

# Device info capture
reporter.appium.captureDeviceInfo=true

# App logs capture
reporter.appium.captureLogs=true
reporter.appium.logs.maxLines=1000
```

### REST-assured Adapter

```properties
# Enable REST-assured adapter
reporter.restassured.enabled=true

# Request/Response logging
reporter.restassured.logging.request=true
reporter.restassured.logging.response=true
reporter.restassured.logging.headers=true
reporter.restassured.logging.body=true
reporter.restassured.logging.cookies=false

# Body size limits
reporter.restassured.logging.maxBodySize=10240
# In bytes (10KB default)

# Sensitive data masking uses the reporter.mask* keys documented in
# Sensitive Data Masking below (enabled by default).
reporter.maskSensitiveData=true

# Response time tracking
reporter.restassured.metrics.responseTime=true
```

## Sensitive Data Masking

The REST-assured adapter masks credentials, tokens, and other sensitive values in captured request/response artifacts before they reach any report. Masking is **enabled by default** and requires no configuration.

### Master Switch

```properties
# Gates ALL masking (headers, bodies, XML, tokens). Default: true
reporter.maskSensitiveData=true
```

When `reporter.maskSensitiveData=false`, no masking of any kind is applied, regardless of the per-category toggles below.

### Header Masking

```properties
# Whether header masking is applied (default: true)
reporter.maskHeaders.enabled=true

# Comma-separated header names to mask
# Default: Authorization,X-API-Key,Cookie,Set-Cookie
reporter.maskHeaders.fields=Authorization,X-API-Key,Cookie,Set-Cookie
```

Header name matching is case-insensitive. Masked header values become `***REDACTED***`.

### Body Masking (JSON and form-urlencoded)

```properties
# Whether body masking is applied (default: true)
reporter.maskBody.enabled=true

# Comma-separated body field names to mask
# Default: password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization
reporter.maskBody.fields=password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization

# Whether Bearer/JWT regex masking passes are applied to ALL body types (default: true)
reporter.maskBody.maskTokens=true
```

- **JSON bodies** (body starts with `{` or `[`) are parsed and masked recursively at any depth: any field whose name case-insensitively matches the list has its value replaced with `***REDACTED***`. String, number, boolean, and null values are all replaced with the redacted string.
- **Form-urlencoded bodies** (`key=value&...`) are masked by key, case-insensitively.
- **All body types**: when `reporter.maskBody.maskTokens=true`, a Bearer/JWT regex pass additionally redacts tokens (JWTs are anchored on the `eyJ` prefix to avoid false positives like version numbers).

### XML Body Masking

```properties
# Whether XML element masking is applied (default: true)
reporter.maskXml.enabled=true

# Comma-separated XML element names to mask.
# When unset, inherits the reporter.maskBody.fields list.
reporter.maskXml.fields=password,secret,token
```

For XML bodies (body starts with `<`), the text content of any element whose tag name case-insensitively matches the list is replaced with `***REDACTED***`. Namespace prefixes are ignored (e.g. `ns:password` matches `password`). **Attributes are NOT masked.** Parsing is XXE-safe (DOCTYPE declarations are disallowed and external entities are disabled), and malformed XML safely falls back to token-only masking — the masker never throws and never logs the unmasked body.

### Field Lists Are Authoritative

Setting a field list **fully replaces** the built-in defaults — lists are not additive. For example, `reporter.maskHeaders.fields=X-Custom-Secret` means only `X-Custom-Secret` is masked; `Authorization` is no longer masked unless you add it to the list.

### Configuration Auto-Detection

Auto-detection applies to **all adapters and the CLI**. When `reporter.properties` is in the working directory or on the classpath, no client-side configuration code is needed:

- `new RestAssuredAdapter()` (default constructor) resolves configuration in this order:
  1. `./reporter.properties` (working directory)
  2. `./src/main/resources/reporter.properties`
  3. `/reporter.properties` on the classpath
  4. Built-in defaults (all masking enabled)
- The Cucumber and TestNG adapters auto-detect `reporter.properties` from the same locations when resolving the report output directory at the end of the run.
- The Appium adapter auto-detects `reporter.properties` (e.g. for `reporter.video.storage`) from the same locations.
- The CLI auto-detects `reporter.properties` when `--config` is omitted.

An explicit configuration always wins: `new RestAssuredAdapter(config)` and `ReporterConfig.loadFromFile(...)` bypass auto-detection entirely.

### Output Directory Precedence

The Cucumber and TestNG adapters resolve the report output directory with `ReporterConfig.resolveOutputDirectory(...)`, using this precedence (highest first):

1. The `-Dreporter.output.directory` system property, if set and non-blank
2. The `reporter.output.directory` value from an auto-detected `reporter.properties` file
3. The default `target/pulsereport`

This means a `reporter.properties` file alone is enough to redirect reports — a system property is only needed to override the file, and resolution never fails: if no file is found, reports go to `target/pulsereport`.

Relative output directories are resolved against the working directory (`user.dir`); under Maven Surefire this is the module directory.

### Complete Example

```properties
# ---- Sensitive data masking ----
# Master switch: gates ALL masking below (default: true)
reporter.maskSensitiveData=true

# Header masking (default: true)
reporter.maskHeaders.enabled=true
# Authoritative list of header names to mask (case-insensitive);
# values become ***REDACTED***
reporter.maskHeaders.fields=Authorization,X-API-Key,Cookie,Set-Cookie

# Body masking for JSON and form-urlencoded bodies (default: true)
reporter.maskBody.enabled=true
# Authoritative list of body field names to mask; JSON is masked
# recursively at any depth and values of any type (string, number,
# boolean, null) become ***REDACTED***
reporter.maskBody.fields=password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization
# Bearer/JWT regex pass applied to ALL body types (default: true)
reporter.maskBody.maskTokens=true

# XML body masking (default: true); element text masked by tag name
# (case-insensitive, namespace prefixes ignored); attributes are NOT
# masked; malformed XML falls back to token-only masking
reporter.maskXml.enabled=true
# When unset, inherits reporter.maskBody.fields
reporter.maskXml.fields=password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization
```

### Programmatic Masking Configuration

```java
ReporterConfig config = ReporterConfig.builder()
    .maskSensitiveData(true)
    .maskHeadersEnabled(true)
    .maskHeaderFields("Authorization,X-API-Key,Cookie,Set-Cookie,X-Custom-Secret")
    .maskBodyEnabled(true)
    .sensitiveBodyFields("password,secret,token,cvv")
    .maskXmlEnabled(true)
    .maskXmlFields("password,secret") // omit to inherit sensitiveBodyFields
    .maskTokens(true)
    .build();
```

The matching getters are `isMaskSensitiveData()`, `isMaskHeadersEnabled()`, `getMaskHeaderFields()`, `isMaskBodyEnabled()`, `getSensitiveBodyFields()`, `isMaskXmlEnabled()`, `getXmlFields()`, and `isMaskTokens()`.

### Migrating from `reporter.sensitiveHeaders`

The `reporter.sensitiveHeaders` key and the `sensitiveHeaders(...)` / `getSensitiveHeaders()` builder API were **removed** in this release. Configurations using the old key must rename it to `reporter.maskHeaders.fields`, and code using the old builder methods must switch to `maskHeaderFields(...)` / `getMaskHeaderFields()`.

## Output Format Configuration

### HTML Output

The HTML report is generated as a self-contained single file with all styles and scripts inlined. Theming (dark/light) is handled via a built-in toggle in the report UI.

> **Note:** Runtime customization of the HTML report (custom CSS, custom templates) is not yet supported. The report uses a fixed built-in template.

### JSON Output

```properties
# Pretty print JSON
reporter.json.pretty=true

# Include full stack traces
reporter.json.includeStackTraces=true

# Include artifact content
reporter.json.includeArtifacts=true
# Artifacts encoded as base64

# Include metric details
reporter.json.includeMetrics=true

# JSON schema validation
reporter.json.validateSchema=false
reporter.json.schemaPath=/path/to/schema.json
```

### JUnit XML Output

```properties
# Include system output
reporter.junit.includeSystemOut=true
reporter.junit.includeSystemErr=true

# Include properties
reporter.junit.includeProperties=true

# Flatten suite hierarchy
reporter.junit.flattenSuites=false

# Timestamp format
reporter.junit.timestampFormat=ISO8601
# Options: ISO8601, UNIX, CUSTOM

# Custom timestamp format (if CUSTOM)
reporter.junit.timestampPattern=yyyy-MM-dd'T'HH:mm:ss.SSSZ
```

## Integration Configuration

### S3 Integration

```properties
# Enable S3 publishing
reporter.s3.enabled=false

# Bucket details
reporter.s3.bucket=my-test-reports
reporter.s3.region=us-east-1
reporter.s3.keyPrefix=builds/${BUILD_ID}/
```

`ReporterConfig` currently maps only the S3 enabled flag, bucket, region, and key prefix. Credentials and custom S3 client behavior are configured in code when you build the AWS SDK client.

### HTTP Integration

```properties
# Enable HTTP publishing
reporter.http.enabled=false

# Endpoint URL
reporter.http.url=https://api.example.com/test-reports

# HTTP method
reporter.http.method=POST
# Options: POST, PUT

# Authentication type
reporter.http.auth.type=bearer
# Common values in the current API: bearer

# Bearer token (if type=bearer)
reporter.http.auth.token=${API_TOKEN}
```

`ReporterConfig` maps the HTTP enabled flag, endpoint URL, method, auth type, and bearer token. When `reporter.http.auth.type=bearer`, the CLI passes the token to `HttpPublishConfig` automatically. Custom headers and alternate authentication modes are configured directly on `HttpPublishConfig` in code.

### Slack Integration

```properties
# Enable Slack notifications
reporter.slack.enabled=false

# Webhook URL
reporter.slack.webhookUrl=${SLACK_WEBHOOK_URL}

# Channel override
reporter.slack.channel=#test-results

# Bot customization
reporter.slack.username=Test Reporter Bot
reporter.slack.iconEmoji=:robot_face:
reporter.slack.iconUrl=https://example.com/icon.png

# Notification conditions
reporter.slack.notifyOnSuccess=true
reporter.slack.notifyOnFailure=true
reporter.slack.mentionOnFailure=@qa-team

# URL to include in Slack notification linking to the published report
reporter.slack.reportUrl=https://reports.example.com/latest

# Notification thresholds
reporter.slack.notifyThreshold.failureRate=0.0
# Only notify if failure rate >= this (%)
reporter.slack.notifyThreshold.durationMinutes=0
# Only notify if duration >= this (minutes)

# Message formatting
reporter.slack.includeStackTrace=false
reporter.slack.maxStackTraceLines=10
reporter.slack.includeReportUrl=true

# Custom message template
reporter.slack.messageTemplate=/path/to/template.json

# Rate limiting
reporter.slack.rateLimit.enabled=true
reporter.slack.rateLimit.perMinute=60
```

## Aggregator Configuration

### Standard Aggregator

```properties
# Aggregator type
reporter.aggregator.type=standard
# Options: standard, streaming

# Thread safety
reporter.aggregator.threadSafe=true
```

### Streaming Aggregator

For large test suites to reduce memory usage:

```properties
# Use streaming aggregator
reporter.aggregator.type=streaming

# Flush interval
reporter.aggregator.streaming.flushInterval=100
# Flush after every N tests

# Buffer size
reporter.aggregator.streaming.bufferSize=1000
# Maximum tests in memory

# Temporary storage
reporter.aggregator.streaming.tempDirectory=target/temp
```

## Performance Configuration

### Memory Management

```properties
# Enable memory optimization
reporter.performance.optimizeMemory=true

# Artifact compression
reporter.performance.compressArtifacts=true

# Lazy loading
reporter.performance.lazyLoadArtifacts=true

# Parallel processing
reporter.performance.parallel.enabled=true
reporter.performance.parallel.threads=4
```

### Caching

```properties
# Enable caching
reporter.performance.cache.enabled=true

# Cache size
reporter.performance.cache.maxSize=1000
# Number of test results to cache

# Cache TTL
reporter.performance.cache.ttlMinutes=60
```

## Logging Configuration

### SLF4J Settings

```properties
# Log level
log.level=INFO
# Options: TRACE, DEBUG, INFO, WARN, ERROR

# Log file
log.file=reporter.log

# Log pattern
log.pattern=%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n

# Console logging
log.console.enabled=true

# File logging
log.file.enabled=true
log.file.maxSize=10MB
log.file.maxBackupIndex=5
```

## Validation Configuration

```properties
# Enable model validation
reporter.validation.enabled=true

# Strict mode (fail on validation errors)
reporter.validation.strict=false

# Validation rules
reporter.validation.requireName=true
reporter.validation.requireTimestamp=true
reporter.validation.requireStatus=true
```

## Environment Variable Substitution

All configuration values support environment variable substitution:

```properties
# Environment variables: ${VAR_NAME}
# Resolves from system properties first, then environment variables.
# If not found, the placeholder is kept as-is.
reporter.s3.bucket=${S3_BUCKET}
reporter.slack.webhookUrl=${SLACK_WEBHOOK}
reporter.name=${REPORT_NAME}
reporter.output.directory=${OUTPUT_DIR}
```

## Programmatic Configuration

### Using Builder

```java
import io.github.pulsereport.config.ReporterConfig;
import java.io.File;
import java.util.List;

ReporterConfig.S3Config s3Config = new ReporterConfig.S3Config();
s3Config.setEnabled(true);
s3Config.setBucket("my-test-reports");
s3Config.setRegion("us-east-1");
s3Config.setKeyPrefix("builds/local/");

ReporterConfig config = ReporterConfig.builder()
    .outputFormats(List.of("html", "json"))
    .outputDirectory(new File("target/pulsereport"))
    .s3Config(s3Config)
    .maxArtifactContentSize(64 * 1024)
    .maskSensitiveData(true)
    .maskHeadersEnabled(true)
    .maskHeaderFields("Authorization,X-API-Key")
    .maskBodyEnabled(true)
    .maskTokens(true)
    .build();
```

### Loading from File

```java
import io.github.pulsereport.config.ReporterConfig;
import java.io.File;

File configFile = new File("config/reporter.properties");
ReporterConfig config = ReporterConfig.loadFromFile(configFile);
```

### Override Values

```java
ReporterConfig loadedConfig = ReporterConfig.loadFromFile(new File("config/reporter.properties"));

ReporterConfig effectiveConfig = ReporterConfig.builder()
    .outputFormats(loadedConfig.getOutputFormats())
    .outputDirectory(new File("target/pulsereport-ci"))
    .s3Config(loadedConfig.getS3Config())
    .httpConfig(loadedConfig.getHttpConfig())
    .slackConfig(loadedConfig.getSlackConfig())
    .maxArtifactContentSize(loadedConfig.getMaxArtifactContentSize())
    .maskSensitiveData(loadedConfig.isMaskSensitiveData())
    .maskHeadersEnabled(loadedConfig.isMaskHeadersEnabled())
    .maskHeaderFields(loadedConfig.getMaskHeaderFields())
    .maskBodyEnabled(loadedConfig.isMaskBodyEnabled())
    .sensitiveBodyFields(loadedConfig.getSensitiveBodyFields())
    .maskXmlEnabled(loadedConfig.isMaskXmlEnabled())
    .maskXmlFields(loadedConfig.getXmlFields())
    .maskTokens(loadedConfig.isMaskTokens())
    .build();
```

## Configuration Validation

### Validate Configuration

```java
try {
    config.validate();
    System.out.println("Configuration is valid");
} catch (ConfigException e) {
    System.err.println("Invalid configuration: " + e.getMessage());
}
```

### Validation Rules

- `reporter.output.formats` must be present and contain only `html`, `json`, or `junit`
- `reporter.output.directory` must be set
- If `reporter.s3.enabled=true`, `reporter.s3.bucket` must be set
- If `reporter.http.enabled=true`, `reporter.http.url` must be set
- If `reporter.slack.enabled=true`, `reporter.slack.webhookUrl` must be set

## Default Values

```properties
# Defaults in ReporterConfig itself
reporter.output.formats=
reporter.output.directory=

reporter.s3.enabled=false
reporter.http.enabled=false
reporter.http.method=POST
reporter.slack.enabled=false

reporter.maxArtifactContentSize=51200
reporter.maskSensitiveData=true
reporter.maskHeaders.enabled=true
reporter.maskHeaders.fields=Authorization,X-API-Key,Cookie,Set-Cookie
reporter.maskBody.enabled=true
reporter.maskBody.fields=password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization
reporter.maskBody.maskTokens=true
reporter.maskXml.enabled=true
# reporter.maskXml.fields inherits reporter.maskBody.fields when unset
```

## Best Practices

### 1. Use Environment Variables for Secrets

```properties
# Good
reporter.s3.accessKey=${AWS_ACCESS_KEY_ID}

# Bad
reporter.s3.accessKey=AKIAIOSFODNN7EXAMPLE
```

### 2. Enable Appropriate Output Formats

```properties
# For local development
reporter.output.formats=html

# For CI/CD
reporter.output.formats=html,junit

# For custom processing
reporter.output.formats=json
```

### 3. Optimize for Large Test Suites

```properties
reporter.aggregator.type=streaming
reporter.performance.optimizeMemory=true
reporter.html.screenshots.compress=true
```

### 4. Separate Config by Environment

```bash
# Validate a specific environment file
java -jar target/pulsereport-1.0.0.jar validate \
    --config config/reporter-dev.properties

# Generate with a specific environment file
java -jar target/pulsereport-1.0.0.jar generate \
    --input target/test-results.json \
    --config config/reporter-prod.properties
```

## Troubleshooting

### Configuration Not Loading

**Problem**: Configuration values not applied.

**Solution**: Ensure `reporter.properties` is in the project root, `src/main/resources/`, or on the classpath. For the CLI, pass `--config` explicitly to verify the right file is loaded. Use `--verbose` to confirm auto-detection.

### Invalid Property Values

**Problem**: `ConfigException: Invalid value for property`

**Solution**: Run validation:

```bash
java -jar target/pulsereport-1.0.0.jar validate --config reporter.properties
```

### Environment Variables Not Resolved

**Problem**: `${ENV_VAR}` appears literally in configuration.

**Solution**: Ensure variable is exported:

```bash
export ENV_VAR=value
```

## Next Steps

- [CLI Reference](cli-reference.md) - Command-line configuration
- [Adapters Guide](adapters-guide.md) - Adapter-specific configuration
- [Integrations Guide](integrations.md) - Integration configuration details
