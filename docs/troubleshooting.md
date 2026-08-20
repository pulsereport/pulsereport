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
    <version>1.0.0</version>
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
    <version>1.0.0</version>
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
2. Reporter disabled in configuration
3. Output directory not writable

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

**Step 2**: Check the PulseReport configuration file (`reporter.properties`):

```properties
reporter.enabled=true
```

**Step 3**: Verify output directory permissions:

```bash
ls -la target/pulsereport
# Should be writable
chmod 755 target/pulsereport
```

**Step 4**: Enable debug logging:

```properties
log.level=DEBUG
```

### Duplicate Test Results

**Problem**: Same test appears multiple times in report.

**Cause**: Adapter registered multiple times or parallel test execution issues.

**Solution**:

**Option 1**: Remove duplicate listener registrations:

```xml
<!-- Only register once -->
<suite name="Test Suite">
    <listeners>
        <listener class-name="io.github.pulsereport.adapters.testng.TestNGAdapter"/>
    </listeners>
</suite>
```

**Option 2**: For parallel tests, ensure thread safety:

```properties
reporter.aggregator.threadSafe=true
```

### Parameterized Tests Not Captured

**Problem**: Data-driven tests not captured correctly.

**Cause**: Parameter capture disabled.

**Solution**:

Enable parameter capture:

```properties
reporter.testng.captureParameters=true
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

**Step 2**: Enable pagination for large reports:

```properties
reporter.html.pagination.enabled=true
reporter.html.pagination.pageSize=50
```

**Step 3**: Check browser console for JavaScript errors (F12).

**Step 4**: Try different browser or disable browser extensions.

### Screenshots Not Appearing

**Problem**: Tests run but screenshots missing from report.

**Cause**: Screenshot capture disabled or failed.

**Solution**:

**For Selenium**:

```properties
reporter.selenium.screenshot.enabled=true
reporter.selenium.screenshot.onSuccess=true
reporter.selenium.screenshot.onFailure=true
```

**For Appium**:

```properties
reporter.appium.screenshot.enabled=true
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

### JSON Schema Validation Fails

**Problem**: `JsonSchemaException: Invalid test run structure`

**Cause**: Test run object missing required fields.

**Solution**:

Validate test run before generating JSON:

```java
try {
    testRun.validate();
} catch (ValidationException e) {
    System.err.println("Invalid test run: " + e.getMessage());
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

Ensure special characters are escaped:

```properties
reporter.junit.escapeSpecialChars=true
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

### S3: Slow Upload

**Problem**: Large reports take minutes to upload.

**Cause**: Single-part upload for large files.

**Solution**:

Enable multipart upload:

```properties
reporter.s3.multipart.enabled=true
reporter.s3.multipart.partSize=5242880
# Part size in bytes (5MB default)
```

Compress artifacts:

```properties
reporter.performance.compressArtifacts=true
reporter.html.screenshots.compress=true
```

### HTTP: Connection Timeout

**Problem**: `SocketTimeoutException` when publishing to HTTP endpoint.

**Cause**: Endpoint slow or unresponsive.

**Solution**:

Increase timeout:

```properties
reporter.http.timeout=60000
# 60 seconds
```

Enable retries:

```properties
reporter.http.retry.enabled=true
reporter.http.retry.maxAttempts=3
reporter.http.retry.backoffMs=2000
```

Test endpoint directly:

```bash
curl -X POST https://api.example.com/reports \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
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

**Step 3**: Verify channel exists and bot has access.

**Step 4**: Check rate limits:

```properties
reporter.slack.rateLimit.enabled=true
reporter.slack.rateLimit.perMinute=60
```

**Step 5**: Enable debug logging:

```properties
log.level=DEBUG
```

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

**Step 1**: Use streaming aggregator:

```properties
reporter.aggregator.type=streaming
reporter.aggregator.streaming.flushInterval=100
```

**Step 2**: Enable memory optimization:

```properties
reporter.performance.optimizeMemory=true
reporter.performance.lazyLoadArtifacts=true
```

**Step 3**: Increase Java heap:

```bash
export MAVEN_OPTS="-Xmx2g"
mvn test
```

**Step 4**: Compress large artifacts:

```properties
reporter.performance.compressArtifacts=true
reporter.html.screenshots.maxWidth=1920
reporter.html.screenshots.maxHeight=1080
```

### Slow Report Generation

**Problem**: Report generation takes several minutes.

**Cause**: Large number of tests or artifacts.

**Solution**:

**Step 1**: Enable parallel processing:

```properties
reporter.performance.parallel.enabled=true
reporter.performance.parallel.threads=4
```

**Step 2**: Limit artifact size:

```properties
reporter.restassured.logging.maxBodySize=10240
reporter.selenium.screenshot.compress=true
```

**Step 3**: Use pagination in HTML:

```properties
reporter.html.pagination.enabled=true
reporter.html.pagination.pageSize=50
```

**Step 4**: Disable charts for very large reports:

```properties
reporter.html.charts.enabled=false
```

### Slow Test Execution

**Problem**: Tests run slower with PulseReport enabled.

**Cause**: Synchronous I/O operations or heavy artifact capture.

**Solution**:

**Step 1**: Disable unnecessary captures:

```properties
reporter.selenium.screenshot.onSuccess=false
# Only capture on failure
reporter.selenium.screenshot.onFailure=true
```

**Step 2**: Use async artifact processing:

```properties
reporter.performance.asyncArtifacts=true
```

**Step 3**: Disable temporary storage:

```properties
reporter.aggregator.streaming.enabled=false
# Use in-memory aggregation for smaller suites
```

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
java -jar target/pulsereport-1.0.0.jar validate \
    --config config/reporter.properties
```

**Step 3**: If you are using the Java API, load the file directly:

```java
ReporterConfig config = ReporterConfig.loadFromFile(new File("config/reporter.properties"));
```

**Step 4**: If you are using the CLI, pass `--config` on commands that support it:

```bash
java -jar target/pulsereport-1.0.0.jar generate \
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

**Step 3**: Use Java system property instead:

```bash
java -Dreporter.slack.webhookUrl="https://..." -jar app.jar
```

### Invalid Property Values

**Problem**: `ConfigException: Invalid value for property X`

**Cause**: Property value doesn't match expected type or format.

**Solution**:

**Step 1**: Validate configuration:

```bash
java -jar target/pulsereport-1.0.0.jar validate --config reporter.properties
```

**Step 2**: Check data types:

```properties
# Boolean: true/false (not yes/no, 1/0)
reporter.s3.enabled=true

# Number: Integer only (no decimals for counts)
reporter.maxArtifactContentSize=51200

# List: Comma-separated (no spaces)
reporter.output.formats=html,json,junit
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

### "Output directory does not exist"

**Cause**: Output directory not created, or no write permissions.

**Solution**:

```bash
mkdir -p target/pulsereport
chmod 755 target/pulsereport
```

Or enable auto-creation:

```properties
reporter.output.createDirectory=true
```

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

### "Failed to serialize TestRun"

**Cause**: TestRun contains non-serializable objects.

**Solution**: Ensure all custom objects in artifacts are serializable or store as strings/bytes.

### "Screenshot capture failed"

**Cause**: WebDriver doesn't support screenshots or browser crashed.

**Solution**:

```java
try {
    if (driver instanceof TakesScreenshot) {
        // Capture screenshot
    }
} catch (Exception e) {
    // Log but don't fail test
    logger.warn("Screenshot capture failed: " + e.getMessage());
}
```

---

## Getting Help

### Enable Debug Logging

```properties
log.level=DEBUG
log.console.enabled=true
log.file.enabled=true
log.file=reporter-debug.log
```

### Check Logs

```bash
# View recent logs
tail -f reporter.log

# Search for errors
grep -i error reporter.log

# Search for specific test
grep "testName" reporter.log
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

Yes, set `reporter.aggregator.threadSafe=true` for parallel tests.

### How do I exclude tests from reporting?

Use TestNG groups:

```java
@Test(groups = "exclude-from-report")
public void testNotInReport() {
    // Not captured
}
```

Configure in adapter:

```properties
reporter.testng.excludeGroups=exclude-from-report
```

### Can I customize the HTML template?

Not yet. The HTML report currently uses a fixed built-in template with a dark/light mode toggle. Runtime customization (custom CSS, custom templates) is planned but not yet available for projects consuming PulseReport as a dependency.

### How do I archive reports by date?

Use date variables in output directory:

```properties
reporter.output.directory=target/pulsereport/${date:yyyyMMdd}
```

Or in S3 key prefix:

```properties
reporter.s3.keyPrefix=reports/${date:yyyy}/${date:MM}/${date:dd}/
```

---

## Still Need Help?

- 📚 [Documentation](../README.md)
- Open an issue on [GitHub](https://github.com/pulsereport/pulsereport/issues)

## Next Steps

- [Getting Started](getting-started.md) - Quick start guide
- [Configuration Reference](configuration.md) - All configuration options
- [Adapters Guide](adapters-guide.md) - Framework-specific documentation
