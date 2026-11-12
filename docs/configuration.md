# Configuration Reference

Use PulseReport configuration to control how the reporting layer captures evidence, writes outputs, and publishes run health signals. This reference keeps the examples aligned with the current PulseReport defaults and naming.

## Configuration Files

### PulseReport configuration file (`pulsereport.properties`)

PulseReport includes `src/main/resources/pulsereport.properties` as a sample starter file. **All adapters** (RestAssured, Cucumber, TestNG, Appium) and the CLI auto-detect this file — no client-side configuration code is needed when `pulsereport.properties` is on the classpath or in the working directory. Auto-detection searches in:

1. `./pulsereport.properties` (working directory / project root)
2. `./src/main/resources/pulsereport.properties`
3. The classpath (`/pulsereport.properties`)

You can also load it explicitly:

1. Pass it to the CLI with `--config /path/to/pulsereport.properties`
2. Load it in Java with `ReporterConfig.loadFromFile(new File("/path/to/pulsereport.properties"))`

```properties
# Example pulsereport.properties
pulsereport.output.formats=html,json,junit
pulsereport.output.directory=target/pulsereport
```

### Loading Configuration

`ReporterConfig` supports three loading paths:

1. Build a configuration in code with `ReporterConfig.builder()`
2. Load a specific properties file with `ReporterConfig.loadFromFile(...)`
3. Auto-detect from well-known locations with `ReporterConfig.autoDetect()`

When loading a file, missing keys keep the class defaults. Placeholder values such as `${AWS_ACCESS_KEY_ID}` are interpolated by checking a Java system property with the same name first, then an environment variable with the same name.

### Properties Loaded by `ReporterConfig`

The current `ReporterConfig` implementation actively reads these keys:

- `pulsereport.output.formats`
- `pulsereport.output.directory`
- `pulsereport.report.title`
- `pulsereport.s3.enabled`
- `pulsereport.s3.bucket`
- `pulsereport.s3.region`
- `pulsereport.s3.keyPrefix`
- `pulsereport.http.enabled`
- `pulsereport.http.url`
- `pulsereport.http.method`
- `pulsereport.http.auth.type`
- `pulsereport.http.auth.token`
- `pulsereport.slack.enabled`
- `pulsereport.slack.webhookUrl`
- `pulsereport.slack.channel`
- `pulsereport.slack.mentionOnFailure`
- `pulsereport.slack.reportUrl`
- `pulsereport.maxArtifactContentSize`
- `pulsereport.video.storage`
- `pulsereport.screenshots.mode`
- `pulsereport.video.mode`
- `pulsereport.video.fps`
- `pulsereport.video.ffmpegPath`
- `pulsereport.maskSensitiveData`
- `pulsereport.maskHeaders.enabled`
- `pulsereport.maskHeaders.fields`
- `pulsereport.maskBody.enabled`
- `pulsereport.maskBody.fields`
- `pulsereport.maskBody.maskTokens`
- `pulsereport.maskXml.enabled`
- `pulsereport.maskXml.fields`
- `pulsereport.assertions.capture`

The masking keys are described in detail in [Sensitive Data Masking](#sensitive-data-masking).

`pulsereport.assertions.capture` (default `true`) controls automatic assertion capture in the Cucumber and TestNG adapters. When it's on, every JUnit, TestNG, Hamcrest and AssertJ assertion and every REST Assured response check appears in the report under the step, hook or test that made it, whether it passed or failed. It can also be set as a system property (`-Dpulsereport.assertions.capture=false`), which takes precedence. See [Assertion Capture](adapters-guide.md#assertion-capture) for details.

`pulsereport.video.storage` controls how video evidence (Appium, and Selenium drivers wrapped with `track(...)`) is stored: `path` (default), `embed`, or `url`. For Selenium, `url` means each recording is handed to the uploader registered with `SeleniumAdapter.setVideoUploader(...)` (for example MinIO or S3) and the report plays the returned URL; without an uploader it behaves like `path`. Selenium videos larger than 50 MB, or beyond 200 MB of embedded video in one run, are linked instead of embedded.

Files in `pulsereport.screenshots.directory` and `pulsereport.video.directory` are linked from the report by a path relative to the report's folder, so keep the folders at the same place relative to the report (or move them together). The S3 publisher uploads only linked files that sit inside the report's folder.

For drivers wrapped with `SeleniumAdapter.track(...)` or `SeleniumTracker.track(...)`:

| Key | Values | Default |
| --- | --- | --- |
| `pulsereport.screenshots.mode` | `all` (every top-level step), `failed` (failed steps and failed tests), `last` (last step of each test, plus failures), `none` | `failed` |
| `pulsereport.screenshots.storage` | `embed` (Base64 inside the report, no files), `path` (files in `pulsereport.screenshots.directory`, shown from there) | `embed` |
| `pulsereport.screenshots.directory` | Folder for screenshot files when `storage=path`; absolute, or relative to the working directory | `<output>/artifacts/screenshots` |
| `pulsereport.video.mode` | `always`, `failed` (kept only for failed tests), `none` | `none` |
| `pulsereport.video.directory` | Folder for recorded videos; absolute, or relative to the working directory | `<output>/videos` |
| `pulsereport.video.fps` | Frames captured per second, 1–30 | `2` |
| `pulsereport.video.ffmpegPath` | ffmpeg executable used to encode MP4 | `ffmpeg` |
| `pulsereport.selenium.stepDetail` | `full` (element label, URL change, page load per action; up to two extra browser calls per action), `minimal` (no extra calls; elements named by locator) | `full` |
| `pulsereport.selenium.maskInputs` | `sensitive` (password, one-time-code, card and sensitive-looking fields, plus credentials and sensitive parameters in URLs), `all`, `none`. `pulsereport.maskSensitiveData=false` turns it off | `sensitive` |
| `pulsereport.selenium.failureCapture` | Comma list of `page` (URL, title, window size), `console` (browser console, Chromium-based browsers), `source` (page source), or `none` | `page,console,source` |
| `pulsereport.selenium.pageMetrics` | Record `page.load.time` and `page.ttfb` for each navigation | `true` |
| `pulsereport.selenium.trackScripts` | Record `executeScript` calls as steps | `false` |

Field names in `pulsereport.maskBody.fields` are also treated as sensitive input fields and URL parameters.

Screenshots are embedded in the report by default (`pulsereport.screenshots.storage=embed`); logs and page sources are written to `<output directory>/artifacts/`, and videos to `pulsereport.video.directory`; the report refers to files by relative path. Videos need [ffmpeg](https://ffmpeg.org) with libx264; if ffmpeg is missing, a warning is logged and tests run without video. Every key can also be passed as a `-D` system property.

Size limits: screenshots over 10 MB, or beyond 200 MB of screenshots in one report, are not embedded in the HTML (the report links their file instead). A single test records at most 1 GB of video frames, and encoding is abandoned after 3 minutes. Encoding runs in the background; the report waits up to 4 minutes per video when the suite ends.

Other property examples in this document preserve the current PulseReport naming, but they are not all mapped by `ReporterConfig` yet.

## Core Settings

### Reporter Metadata

```properties
# Reporter name shown in report headers
pulsereport.name=PulseReport

# Reporter version (for tracking)
pulsereport.version=${version}

# Enable/disable reporter globally
pulsereport.enabled=true
# Set to false to disable all reporting
```

### Output Settings

```properties
# Output formats (comma-separated)
pulsereport.output.formats=html,json,junit
# Available: html, json, junit

# Output directory
pulsereport.output.directory=target/pulsereport

# Title shown in the HTML report header and browser tab.
# Defaults to the test run name. Can also be set with -Dpulsereport.report.title=...
pulsereport.report.title=Checkout regression

# Create output directory if missing
pulsereport.output.createDirectory=true

# Overwrite existing reports
pulsereport.output.overwrite=true
```

## Adapter Configuration

### TestNG Adapter

```properties
# Enable TestNG adapter
pulsereport.testng.enabled=true

# Capture test parameters
pulsereport.testng.captureParameters=true

# Capture test groups
pulsereport.testng.captureGroups=true

# Capture configuration methods
pulsereport.testng.captureConfigMethods=false
# @BeforeMethod, @AfterMethod, etc.
```

### Selenium Adapter

```properties
pulsereport.screenshots.mode=failed
pulsereport.video.mode=failed
pulsereport.selenium.stepDetail=full
pulsereport.selenium.maskInputs=sensitive
pulsereport.selenium.failureCapture=page,console,source
pulsereport.selenium.pageMetrics=true
pulsereport.selenium.trackScripts=false
```

See the [Selenium key table](#properties-loaded-by-reporterconfig) above for the values.

### Appium Adapter

```properties
# Enable Appium adapter
pulsereport.appium.enabled=true

# Screenshot settings
pulsereport.appium.screenshot.enabled=true
pulsereport.appium.screenshot.onSuccess=true
pulsereport.appium.screenshot.onFailure=true
pulsereport.appium.screenshot.format=png

# Device info capture
pulsereport.appium.captureDeviceInfo=true

# App logs capture
pulsereport.appium.captureLogs=true
pulsereport.appium.logs.maxLines=1000
```

### REST-assured Adapter

```properties
# Enable REST-assured adapter
pulsereport.restassured.enabled=true

# Request/Response logging
pulsereport.restassured.logging.request=true
pulsereport.restassured.logging.response=true
pulsereport.restassured.logging.headers=true
pulsereport.restassured.logging.body=true
pulsereport.restassured.logging.cookies=false

# Body size limits
pulsereport.maxArtifactContentSize=51200
# In bytes (51200 = 50KB default); caps captured request/response body size

# Sensitive data masking uses the pulsereport.mask* keys documented in
# Sensitive Data Masking below (enabled by default).
pulsereport.maskSensitiveData=true

# Response time tracking
pulsereport.restassured.metrics.responseTime=true
```

## Sensitive Data Masking

The REST-assured adapter masks credentials, tokens, and other sensitive values in captured request/response artifacts before they reach any report. Masking is **enabled by default** and requires no configuration.

### Master Switch

```properties
# Gates ALL masking (headers, bodies, XML, tokens, Selenium input and URLs). Default: true
pulsereport.maskSensitiveData=true
```

When `pulsereport.maskSensitiveData=false`, no masking of any kind is applied, regardless of the per-category toggles below.

### Selenium Input and URL Masking

```properties
# sensitive (default) | all | none
pulsereport.selenium.maskInputs=sensitive
```

For drivers wrapped with `track(...)`:

- `sensitive` hides typed text in password, one-time-code (`autocomplete="one-time-code"`) and
  card fields, and in fields whose name, id or label contains `password`, `token`, `secret`,
  `otp`, `pin`, `cvv`, `ssn`, `card number`, `iban`, `api key`, `auth`, or any name listed in
  `pulsereport.maskBody.fields`. It also hides credentials (`https://user:pass@…`) and the values of
  sensitive URL parameters (`token`, `code`, `access_token`, `session`, `signature`, …) in step
  names, URL changes, metric labels and page details.
- `all` hides every typed text (and keys sent with `Actions` or to alerts), plus the URL masking above.
- `none` shows everything.

Typed text is shown as `••••••`; masked URL parts as `***`.

### Header Masking

```properties
# Whether header masking is applied (default: true)
pulsereport.maskHeaders.enabled=true

# Comma-separated header names to mask
# Default: Authorization,X-API-Key,Cookie,Set-Cookie
pulsereport.maskHeaders.fields=Authorization,X-API-Key,Cookie,Set-Cookie
```

Header name matching is case-insensitive. Masked header values become `***REDACTED***`.

### Body Masking (JSON and form-urlencoded)

```properties
# Whether body masking is applied (default: true)
pulsereport.maskBody.enabled=true

# Comma-separated body field names to mask
# Default: password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization
pulsereport.maskBody.fields=password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization

# Whether Bearer/JWT regex masking passes are applied to ALL body types (default: true)
pulsereport.maskBody.maskTokens=true
```

- **JSON bodies** (body starts with `{` or `[`) are parsed and masked recursively at any depth: any field whose name case-insensitively matches the list has its value replaced with `***REDACTED***`. String, number, boolean, and null values are all replaced with the redacted string.
- **Form-urlencoded bodies** (`key=value&...`) are masked by key, case-insensitively.
- **All body types**: when `pulsereport.maskBody.maskTokens=true`, a Bearer/JWT regex pass additionally redacts tokens (JWTs are anchored on the `eyJ` prefix to avoid false positives like version numbers).

### XML Body Masking

```properties
# Whether XML element masking is applied (default: true)
pulsereport.maskXml.enabled=true

# Comma-separated XML element names to mask.
# When unset, inherits the pulsereport.maskBody.fields list.
pulsereport.maskXml.fields=password,secret,token
```

For XML bodies (body starts with `<`), the text content of any element whose tag name case-insensitively matches the list is replaced with `***REDACTED***`. Namespace prefixes are ignored (e.g. `ns:password` matches `password`). **Attributes are NOT masked.** Parsing is XXE-safe (DOCTYPE declarations are disallowed and external entities are disabled), and malformed XML safely falls back to token-only masking — the masker never throws and never logs the unmasked body.

### Field Lists Are Authoritative

Setting a field list **fully replaces** the built-in defaults — lists are not additive. For example, `pulsereport.maskHeaders.fields=X-Custom-Secret` means only `X-Custom-Secret` is masked; `Authorization` is no longer masked unless you add it to the list.

### Configuration Auto-Detection

Auto-detection applies to **all adapters and the CLI**. When `pulsereport.properties` is in the working directory or on the classpath, no client-side configuration code is needed:

- `new RestAssuredAdapter()` (default constructor) resolves configuration in this order:
  1. `./pulsereport.properties` (working directory)
  2. `./src/main/resources/pulsereport.properties`
  3. `/pulsereport.properties` on the classpath
  4. Built-in defaults (all masking enabled)
- The Cucumber and TestNG adapters auto-detect `pulsereport.properties` from the same locations when resolving the report output directory at the end of the run.
- The Appium adapter auto-detects `pulsereport.properties` (e.g. for `pulsereport.video.storage`) from the same locations.
- The CLI auto-detects `pulsereport.properties` when `--config` is omitted.

An explicit configuration always wins: `new RestAssuredAdapter(config)` and `ReporterConfig.loadFromFile(...)` bypass auto-detection entirely.

### Output Directory Precedence

The Cucumber and TestNG adapters resolve the report output directory with `ReporterConfig.resolveOutputDirectory(...)`, using this precedence (highest first):

1. The `-Dpulsereport.output.directory` system property, if set and non-blank
2. The `pulsereport.output.directory` value from an auto-detected `pulsereport.properties` file
3. The default `target/pulsereport`

This means a `pulsereport.properties` file alone is enough to redirect reports — a system property is only needed to override the file, and resolution never fails: if no file is found, reports go to `target/pulsereport`.

Relative output directories are resolved against the working directory (`user.dir`); under Maven Surefire this is the module directory.

### Complete Example

```properties
# ---- Sensitive data masking ----
# Master switch: gates ALL masking below (default: true)
pulsereport.maskSensitiveData=true

# Header masking (default: true)
pulsereport.maskHeaders.enabled=true
# Authoritative list of header names to mask (case-insensitive);
# values become ***REDACTED***
pulsereport.maskHeaders.fields=Authorization,X-API-Key,Cookie,Set-Cookie

# Body masking for JSON and form-urlencoded bodies (default: true)
pulsereport.maskBody.enabled=true
# Authoritative list of body field names to mask; JSON is masked
# recursively at any depth and values of any type (string, number,
# boolean, null) become ***REDACTED***
pulsereport.maskBody.fields=password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization
# Bearer/JWT regex pass applied to ALL body types (default: true)
pulsereport.maskBody.maskTokens=true

# XML body masking (default: true); element text masked by tag name
# (case-insensitive, namespace prefixes ignored); attributes are NOT
# masked; malformed XML falls back to token-only masking
pulsereport.maskXml.enabled=true
# When unset, inherits pulsereport.maskBody.fields
pulsereport.maskXml.fields=password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization
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

### Migrating from `pulsereport.sensitiveHeaders`

The `pulsereport.sensitiveHeaders` key and the `sensitiveHeaders(...)` / `getSensitiveHeaders()` builder API were **removed** in this release. Configurations using the old key must rename it to `pulsereport.maskHeaders.fields`, and code using the old builder methods must switch to `maskHeaderFields(...)` / `getMaskHeaderFields()`.

## Output Format Configuration

### HTML Output

The HTML report is generated as a self-contained single file with all styles and scripts inlined. Theming (dark/light) is handled via a built-in toggle in the report UI.

> **Note:** Runtime customization of the HTML report (custom CSS, custom templates) is not yet supported. The report uses a fixed built-in template.

### JSON Output

```properties
# Pretty print JSON
pulsereport.json.pretty=true

# Include full stack traces
pulsereport.json.includeStackTraces=true

# Include artifact content
pulsereport.json.includeArtifacts=true
# Artifacts encoded as base64

# Include metric details
pulsereport.json.includeMetrics=true

# JSON schema validation
pulsereport.json.validateSchema=false
pulsereport.json.schemaPath=/path/to/schema.json
```

### JUnit XML Output

```properties
# Include system output
pulsereport.junit.includeSystemOut=true
pulsereport.junit.includeSystemErr=true

# Include properties
pulsereport.junit.includeProperties=true

# Flatten suite hierarchy
pulsereport.junit.flattenSuites=false

# Timestamp format
pulsereport.junit.timestampFormat=ISO8601
# Options: ISO8601, UNIX, CUSTOM

# Custom timestamp format (if CUSTOM)
pulsereport.junit.timestampPattern=yyyy-MM-dd'T'HH:mm:ss.SSSZ
```

## Integration Configuration

### S3 Integration

```properties
# Enable S3 publishing
pulsereport.s3.enabled=false

# Bucket details
pulsereport.s3.bucket=my-test-reports
pulsereport.s3.region=us-east-1
pulsereport.s3.keyPrefix=builds/${BUILD_ID}/
```

`ReporterConfig` currently maps only the S3 enabled flag, bucket, region, and key prefix. Credentials and custom S3 client behavior are configured in code when you build the AWS SDK client.

### HTTP Integration

```properties
# Enable HTTP publishing
pulsereport.http.enabled=false

# Endpoint URL
pulsereport.http.url=https://api.example.com/test-reports

# HTTP method
pulsereport.http.method=POST
# Options: POST, PUT

# Authentication type
pulsereport.http.auth.type=bearer
# Common values in the current API: bearer

# Bearer token (if type=bearer)
pulsereport.http.auth.token=${API_TOKEN}
```

`ReporterConfig` maps the HTTP enabled flag, endpoint URL, method, auth type, and bearer token. When `pulsereport.http.auth.type=bearer`, the CLI passes the token to `HttpPublishConfig` automatically. Custom headers and alternate authentication modes are configured directly on `HttpPublishConfig` in code.

### Slack Integration

```properties
# Enable Slack notifications
pulsereport.slack.enabled=false

# Webhook URL
pulsereport.slack.webhookUrl=${SLACK_WEBHOOK_URL}

# Channel override
pulsereport.slack.channel=#test-results

# Bot customization
pulsereport.slack.username=Test Reporter Bot
pulsereport.slack.iconEmoji=:robot_face:
pulsereport.slack.iconUrl=https://example.com/icon.png

# Notification conditions
pulsereport.slack.notifyOnSuccess=true
pulsereport.slack.notifyOnFailure=true
pulsereport.slack.mentionOnFailure=@qa-team

# URL to include in Slack notification linking to the published report
pulsereport.slack.reportUrl=https://reports.example.com/latest

# Notification thresholds
pulsereport.slack.notifyThreshold.failureRate=0.0
# Only notify if failure rate >= this (%)
pulsereport.slack.notifyThreshold.durationMinutes=0
# Only notify if duration >= this (minutes)

# Message formatting
pulsereport.slack.includeStackTrace=false
pulsereport.slack.maxStackTraceLines=10
pulsereport.slack.includeReportUrl=true

# Custom message template
pulsereport.slack.messageTemplate=/path/to/template.json

# Rate limiting
pulsereport.slack.rateLimit.enabled=true
pulsereport.slack.rateLimit.perMinute=60
```

## Aggregator Configuration

### Standard Aggregator

```properties
# Aggregator type
pulsereport.aggregator.type=standard
# Options: standard, streaming

# Thread safety
pulsereport.aggregator.threadSafe=true
```

### Streaming Aggregator

For large test suites to reduce memory usage:

```properties
# Use streaming aggregator
pulsereport.aggregator.type=streaming

# Flush interval
pulsereport.aggregator.streaming.flushInterval=100
# Flush after every N tests

# Buffer size
pulsereport.aggregator.streaming.bufferSize=1000
# Maximum tests in memory

# Temporary storage
pulsereport.aggregator.streaming.tempDirectory=target/temp
```

## Performance Configuration

### Memory Management

```properties
# Enable memory optimization
pulsereport.performance.optimizeMemory=true

# Artifact compression
pulsereport.performance.compressArtifacts=true

# Lazy loading
pulsereport.performance.lazyLoadArtifacts=true

# Parallel processing
pulsereport.performance.parallel.enabled=true
pulsereport.performance.parallel.threads=4
```

### Caching

```properties
# Enable caching
pulsereport.performance.cache.enabled=true

# Cache size
pulsereport.performance.cache.maxSize=1000
# Number of test results to cache

# Cache TTL
pulsereport.performance.cache.ttlMinutes=60
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
pulsereport.validation.enabled=true

# Strict mode (fail on validation errors)
pulsereport.validation.strict=false

# Validation rules
pulsereport.validation.requireName=true
pulsereport.validation.requireTimestamp=true
pulsereport.validation.requireStatus=true
```

## Environment Variable Substitution

All configuration values support environment variable substitution:

```properties
# Environment variables: ${VAR_NAME}
# Resolves from system properties first, then environment variables.
# If not found, the placeholder is kept as-is.
pulsereport.s3.bucket=${S3_BUCKET}
pulsereport.slack.webhookUrl=${SLACK_WEBHOOK}
pulsereport.name=${REPORT_NAME}
pulsereport.output.directory=${OUTPUT_DIR}
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

File configFile = new File("config/pulsereport.properties");
ReporterConfig config = ReporterConfig.loadFromFile(configFile);
```

### Override Values

```java
ReporterConfig loadedConfig = ReporterConfig.loadFromFile(new File("config/pulsereport.properties"));

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

- `pulsereport.output.formats` must be present and contain only `html`, `json`, or `junit`
- `pulsereport.output.directory` must be set
- If `pulsereport.s3.enabled=true`, `pulsereport.s3.bucket` must be set
- If `pulsereport.http.enabled=true`, `pulsereport.http.url` must be set
- If `pulsereport.slack.enabled=true`, `pulsereport.slack.webhookUrl` must be set
- `pulsereport.video.storage` must be one of `path`, `embed`, or `url` (default `path`); any other value fails validation with `Invalid video storage: …`

## Default Values

```properties
# Defaults in ReporterConfig itself
pulsereport.output.formats=
pulsereport.output.directory=

pulsereport.s3.enabled=false
pulsereport.http.enabled=false
pulsereport.http.method=POST
pulsereport.slack.enabled=false

pulsereport.maxArtifactContentSize=51200
pulsereport.maskSensitiveData=true
pulsereport.maskHeaders.enabled=true
pulsereport.maskHeaders.fields=Authorization,X-API-Key,Cookie,Set-Cookie
pulsereport.maskBody.enabled=true
pulsereport.maskBody.fields=password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization
pulsereport.maskBody.maskTokens=true
pulsereport.maskXml.enabled=true
# pulsereport.maskXml.fields inherits pulsereport.maskBody.fields when unset
```

## Best Practices

### 1. Use Environment Variables for Secrets

```properties
# Good
pulsereport.s3.accessKey=${AWS_ACCESS_KEY_ID}

# Bad
pulsereport.s3.accessKey=AKIAIOSFODNN7EXAMPLE
```

### 2. Enable Appropriate Output Formats

```properties
# For local development
pulsereport.output.formats=html

# For CI/CD
pulsereport.output.formats=html,junit

# For custom processing
pulsereport.output.formats=json
```

### 3. Optimize for Large Test Suites

```properties
pulsereport.aggregator.type=streaming
pulsereport.performance.optimizeMemory=true
pulsereport.html.screenshots.compress=true
```

### 4. Separate Config by Environment

```bash
# Validate a specific environment file
java -jar target/pulsereport-${version}.jar validate \
    --config config/reporter-dev.properties

# Generate with a specific environment file
java -jar target/pulsereport-${version}.jar generate \
    --input target/test-results.json \
    --config config/reporter-prod.properties
```

## Troubleshooting

### Configuration Not Loading

**Problem**: Configuration values not applied.

**Solution**: Ensure `pulsereport.properties` is in the project root, `src/main/resources/`, or on the classpath. For the CLI, pass `--config` explicitly to verify the right file is loaded. Use `--verbose` to confirm auto-detection.

### Invalid Property Values

**Problem**: `ConfigException: Invalid value for property`

**Solution**: Run validation:

```bash
java -jar target/pulsereport-${version}.jar validate --config pulsereport.properties
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
