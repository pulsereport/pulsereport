# Troubleshooting Guide

Use this guide to isolate setup issues, output failures, and integration problems in PulseReport. The goal is to get back to a reliable run health signal with the smallest possible change.

## Table of Contents

- [Installation Issues](#installation-issues)
- [Adapter Issues](#adapter-issues)
- [Report Generation Issues](#report-generation-issues)
- [Integration Issues](#integration-issues)
- [Performance Issues](#performance-issues)
- [Configuration Issues](#configuration-issues)

---

## Installation Issues

### ClassNotFoundException

**Problem**: `ClassNotFoundException: io.github.pulsereport.adapters.testng.TestNGAdapter`

**Cause**: PulseReport dependency not added to project.

**Solution**:

Add Maven dependency:

```xml
<dependency>
    <groupId>io.github.pulsereport</groupId>
    <artifactId>pulsereport</artifactId>
    <version>${version}</version>
</dependency>
```

Then rebuild:

```bash
mvn clean install
```

### Version Conflicts

**Problem**: `NoSuchMethodError` or `IncompatibleClassChangeError`

**Cause**: Dependency version conflicts (TestNG, Jackson, etc.)

**Solution**:

Check dependency tree:

```bash
mvn dependency:tree | grep -i testng
```

Exclude conflicting versions:

```xml
<dependency>
    <groupId>io.github.pulsereport</groupId>
    <artifactId>pulsereport</artifactId>
    <version>${version}</version>
    <exclusions>
        <exclusion>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

## Adapter Issues

### Tests Not Captured

**Problem**: Tests run but no report generated.

**Possible Causes**:

1. Adapter not registered
2. Output directory not writable
3. Tests were not run through the framework the adapter hooks into (e.g. running TestNG tests directly from an IDE without listeners)

**Solution**:

**Step 1**: Verify adapter registration in `testng.xml`:

```xml
<suite name="Test Suite">
    <listeners>
        <listener class-name="io.github.pulsereport.adapters.testng.TestNGAdapter"/>
    </listeners>
    <!-- tests -->
</suite>
```

**Step 2**: Verify output directory permissions:

```bash
ls -la target/pulsereport
# Should be writable
chmod 755 target/pulsereport
```

**Step 3**: Check the console output at run completion — the adapters log the absolute path of each generated report (`PulseReport: HTML report generated` / `Location: ...`). If those lines are missing, the adapter never ran.

### Duplicate Test Results

**Problem**: Same test appears multiple times in report.

**Cause**: Adapter registered multiple times (e.g. both in `testng.xml` and via `@Listeners`).

**Solution**:

Remove duplicate listener registrations:

```xml
<!-- Only register once -->
<suite name="Test Suite">
    <listeners>
        <listener class-name="io.github.pulsereport.adapters.testng.TestNGAdapter"/>
    </listeners>
</suite>
```

---

## Report Generation Issues

### HTML Report Not Opening

**Problem**: HTML file generated but blank or won't open in browser.

**Possible Causes**:

1. Browser security restrictions
2. Large file size
3. JavaScript errors

**Solution**:

**Step 1**: Check file size:

```bash
ls -lh target/pulsereport/test-report.html
# If > 50MB, it may be too large for browsers
```

**Step 2**: Check browser console for JavaScript errors (F12).

**Step 3**: Try different browser or disable browser extensions.

### Screenshots Not Appearing

**Problem**: Tests run but screenshots missing from report.

**Cause**: Screenshots are not captured automatically. The Selenium adapter only attaches screenshots you capture explicitly in your test code.

**Solution**:

Capture screenshots explicitly via the adapter:

```java
SeleniumAdapter adapter = new SeleniumAdapter(driver);
adapter.captureBrowserScreenshot(); // attaches a screenshot to the current test
```

**Check WebDriver is TakesScreenshot**:

```java
if (driver instanceof TakesScreenshot) {
    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    // Screenshot capture successful
} else {
    // Driver doesn't support screenshots
}
```

### Invalid Test Run Structure

**Problem**: The generated report is missing data, or downstream tooling rejects the output.

**Cause**: Required fields on the model objects were not set.

**Solution**:

Validate the model before generating output with `ModelValidator` (it returns a list of human-readable errors):

```java
import io.github.pulsereport.core.validation.ModelValidator;
import java.util.List;

ModelValidator validator = new ModelValidator();
List<String> errors = validator.validate(testRun);
// e.g. "TestRun.name is required (cannot be null)"
if (!errors.isEmpty()) {
    errors.forEach(System.err::println);
}
```

Ensure required fields are set:

```java
TestRun testRun = TestRun.builder()
    .id(UUID.randomUUID().toString())  // Required
    .name("Test Suite")                 // Required
    .startTime(Instant.now())          // Required
    .status(TestStatus.COMPLETED)      // Required
    .build();
```

### JUnit XML Invalid

**Problem**: CI/CD rejects JUnit XML.

**Cause**: Invalid XML structure or characters.

**Solution**:

Validate XML against schema:

```bash
xmllint --schema junit-schema.xsd target/pulsereport/TEST-junit.xml
```

---

## Integration Issues

### S3: Access Denied

**Problem**: `AccessDeniedException` when uploading to S3.

**Cause**: Insufficient IAM permissions.

**Solution**:

**Step 1**: Verify IAM policy includes:

```json
{
  "Effect": "Allow",
  "Action": [
    "s3:PutObject",
    "s3:PutObjectAcl"
  ],
  "Resource": "arn:aws:s3:::your-bucket/*"
}
```

**Step 2**: Test credentials:

```bash
aws s3 ls s3://your-bucket --profile your-profile
```

**Step 3**: Verify credentials in configuration:

```bash
echo $AWS_ACCESS_KEY_ID
echo $AWS_SECRET_ACCESS_KEY
# Should not be empty
```

### HTTP: Connection Timeout

**Problem**: `SocketTimeoutException` when publishing to HTTP endpoint.

**Cause**: Endpoint slow or unresponsive.

**Solution**:

Test endpoint directly:

```bash
curl -X POST https://api.example.com/reports \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
```

Tune retries on `HttpPublishConfig` (retry settings are a code-only API, not file properties):

```java
HttpPublishConfig config = HttpPublishConfig.builder()
    .endpoint("https://api.example.com/test-reports")
    .method("POST")
    .retryAttempts(3)
    .retryDelayMs(1000) // doubles on each retry
    .build();
```

### Slack: Message Not Sent

**Problem**: No Slack notification received.

**Possible Causes**:

1. Invalid webhook URL
2. Channel doesn't exist
3. Bot not added to channel
4. Rate limiting

**Solution**:

**Step 1**: Test webhook manually:

```bash
curl -X POST -H 'Content-type: application/json' \
  --data '{"text":"Test message"}' \
  $SLACK_WEBHOOK_URL
```

**Step 2**: Check webhook response:

```bash
# Should return: ok
# If error, check response message
```

**Step 3**: Verify channel exists and the webhook points at it. Note that `SlackNotifier` already retries failed sends with exponential backoff; tune `retryAttempts`/`retryDelayMs` on `SlackConfig` in code if needed.

### Slack: Webhook URL Exposed

**Problem**: Webhook URL accidentally committed to version control.

**Immediate Action**:

1. Regenerate webhook URL in Slack
2. Update configuration to use environment variable
3. Remove from git history:

```bash
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch reporter.properties" \
  --prune-empty --tag-name-filter cat -- --all
```

**Prevention**:

Always use environment variables:

```properties
reporter.slack.webhookUrl=${SLACK_WEBHOOK_URL}
```

Add to `.gitignore`:

```text
reporter.properties
*.properties
```

---

## Performance Issues

### OutOfMemoryError

**Problem**: `OutOfMemoryError` when running large test suites.

**Cause**: Too many test results or large artifacts in memory.

**Solution**:

**Step 1**: Increase Java heap:

```bash
export MAVEN_OPTS="-Xmx2g"
mvn test
```

**Step 2**: Reduce what you attach — capture screenshots only on failure and avoid attaching large response bodies or logs to every test.

### Slow Report Generation

**Problem**: Report generation takes several minutes.

**Cause**: Large number of tests or artifacts.

**Solution**:

**Step 1**: Reduce artifact volume — attach large files only for failed tests.

**Step 2**: Split very large suites into separate runs/reports.

**Step 3**: Check the file size of the generated report:

```bash
ls -lh target/pulsereport/
```

### Slow Test Execution

**Problem**: Tests run slower with PulseReport enabled.

**Cause**: Heavy artifact capture (screenshots, logs, response bodies) inside the test flow.

**Solution**:

**Step 1**: Capture expensive artifacts only on failure:

```java
try {
    // test logic
} catch (AssertionError | RuntimeException e) {
    adapter.captureBrowserScreenshot(); // capture only when failing
    throw e;
}
```

**Step 2**: Avoid capturing screenshots or logs for every passing step.

---

## Configuration Issues

### Configuration Not Loaded

**Problem**: Configuration values not applied.

**Cause**: The configuration file was never loaded explicitly, or the path passed to the CLI or Java API is wrong.

**Solution**:

**Step 1**: Verify the file you intend to load actually exists:

```bash
ls -la config/reporter.properties
```

**Step 2**: Validate that same file explicitly:

```bash
java -jar target/pulsereport-${version}.jar validate \
    --config config/reporter.properties
```

**Step 3**: If you are using the Java API, load the file directly:

```java
ReporterConfig config = ReporterConfig.loadFromFile(new File("config/reporter.properties"));
```

**Step 4**: If you are using the CLI, pass `--config` on commands that support it:

```bash
java -jar target/pulsereport-${version}.jar generate \
    --input target/test-results.json \
    --config config/reporter.properties
```

### Environment Variables Not Resolved

**Problem**: `${ENV_VAR}` appears literally in logs/config.

**Cause**: Variable not exported or shell doesn't support substitution.

**Solution**:

**Step 1**: Export variable:

```bash
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."
```

**Step 2**: Verify variable is set:

```bash
echo $SLACK_WEBHOOK_URL
# Should show value, not empty
```

**Step 3**: Use a Java system property instead — `${VAR}` placeholders fall back to system properties before environment variables:

```bash
java -DSLACK_WEBHOOK_URL="https://..." -jar app.jar
```

### Invalid Configuration Values

**Problem**: `ConfigException` thrown during configuration validation, with messages such as:

- `Invalid output format: <format>. Valid formats are: html, json, junit`
- `S3 bucket must be specified when S3 is enabled`
- `HTTP URL must be specified when HTTP publishing is enabled`
- `Slack webhook URL must be specified when Slack is enabled`
- `Invalid video storage: <value>. Valid values are: path, embed, url`

**Cause**: `ReporterConfig.validate()` rejected the loaded configuration.

**Solution**:

**Step 1**: Validate configuration:

```bash
java -jar target/pulsereport-${version}.jar validate --config reporter.properties
```

**Step 2**: Check data types:

```properties
# Boolean: true/false (not yes/no, 1/0)
reporter.s3.enabled=true

# Number: Integer only (no decimals for counts)
reporter.maxArtifactContentSize=51200

# List: Comma-separated (no spaces)
reporter.output.formats=html,json,junit

# Enum-like: path | embed | url
reporter.video.storage=path
```

**Step 3**: Check URL formats:

```properties
# Must include protocol
reporter.http.url=https://api.example.com/reports
# NOT: api.example.com/reports
```

---

## Common Error Messages

### "TestRun is null"

**Cause**: Adapter not registered or tests not run through TestNG.

**Solution**: Register adapter in `testng.xml` or via `@Listeners`.

### Report written to `target/pulsereport` instead of my configured directory

**Cause**: The Cucumber and TestNG adapters resolve the output directory in this precedence order (highest first):

1. `-Dreporter.output.directory` system property
2. `reporter.output.directory` from an auto-detected `reporter.properties`
3. Default `target/pulsereport`

So either your `reporter.properties` was not auto-detected (it must be at `./reporter.properties`, `./src/main/resources/reporter.properties`, or `/reporter.properties` on the classpath), or a `-Dreporter.output.directory` system property is overriding the file.

**Solution**:

- Check the adapter logs at startup/run completion — the Cucumber and TestNG adapters log the absolute path of each generated report (`PulseReport: HTML report generated / Location: ...`).
- Under Maven Surefire the working directory is the module directory, so `./reporter.properties` means the module root, not necessarily the repo root. Putting the file in `src/main/resources/` (classpath) is the most reliable location.
- Check whether a `-Dreporter.output.directory` system property is set (e.g. in the Maven command line or `surefire` `<systemPropertyVariables>`); it overrides the file value.

---

## Getting Help

### Enable Debug Logging

PulseReport logs through SLF4J, so logging is controlled by whichever SLF4J backend is on your classpath (Logback, Log4j2, JUL, etc.). For example, with Logback, add a `logback.xml` to your test classpath:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <logger name="io.github.pulsereport" level="DEBUG"/>
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

At DEBUG level the adapters log the absolute path of each generated report (`PulseReport: HTML report generated` / `Location: ...`).

### Check Logs

PulseReport logs go to the console (or wherever your SLF4J backend writes). Under Maven Surefire, check the test output and per-class reports:

```bash
# Search for errors in test output
mvn test > test-output.txt 2>&1
grep -i error test-output.txt

# Search Surefire reports
grep -i error target/surefire-reports/*.txt

# Search for a specific test
grep "testName" test-output.txt
```

### Collect Diagnostic Information

```bash
# Java version
java -version

# Maven version
mvn -version

# Dependency tree
mvn dependency:tree > dependencies.txt

# Test output
mvn test > test-output.txt 2>&1

# Configuration
cat src/main/resources/reporter.properties
```

### Report Issues

When reporting issues, include:

1. PulseReport version
2. Java version
3. Test framework version (TestNG, Selenium, etc.)
4. Error message and stack trace
5. Minimal reproducible example
6. Configuration file (remove sensitive data)

---

## Frequently Asked Questions

### Can I use PulseReport with JUnit?

Today, PulseReport is designed around TestNG adapters. JUnit support is not part of the current implementation.

### Does it work with parallel test execution?

There is no thread-safety configuration property to tune. Register the adapter exactly once (in `testng.xml` *or* via `@Listeners`, not both) and let the framework run tests in parallel; if you see duplicate results, check for double registration first.

### How do I exclude tests from reporting?

PulseReport has no exclusion configuration. Use standard TestNG mechanisms — for example, group exclusion in `testng.xml` — to control which tests run (and therefore which tests are reported):

```xml
<groups>
    <run>
        <exclude name="slow"/>
    </run>
</groups>
```

### Can I customize the HTML template?

Not yet. The HTML report currently uses a fixed built-in template with a dark/light mode toggle. Runtime customization (custom CSS, custom templates) is planned but not yet available for projects consuming PulseReport as a dependency.

### How do I archive reports by date or build?

Use environment variable interpolation (only `${VAR}` placeholders are supported; there is no `${date:...}` syntax):

```properties
reporter.output.directory=target/pulsereport/${BUILD_ID}
```

Or in the S3 key prefix:

```properties
reporter.s3.keyPrefix=reports/${GIT_BRANCH}/${BUILD_NUMBER}/
```

---

## Still Need Help?

- 📚 [Documentation](../README.md)
- Open an issue on [GitHub](https://github.com/pulsereport/pulsereport/issues)

## Next Steps

- [Getting Started](getting-started.md) - Quick start guide
- [Configuration Reference](configuration.md) - All configuration options
- [Adapters Guide](adapters-guide.md) - Framework-specific documentation
