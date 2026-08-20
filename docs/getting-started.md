# Getting Started with PulseReport

PulseReport turns raw automation output into a clear signal for run health. This guide gets the reporting layer into a TestNG project quickly and keeps the setup aligned with the current PulseReport build.

## Requirements

- Java 17 or later
- Maven 3.6+
- A TestNG-based test suite

## Quick Start (5 Steps)

### Step 1: Add Maven Dependency

Add PulseReport to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.pulsereport</groupId>
    <artifactId>pulsereport</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Step 2: Register the Adapter

For TestNG, add the listener to your `testng.xml`:

```xml
<suite name="My Test Suite">
    <listeners>
        <listener class-name="io.github.pulsereport.adapters.testng.TestNGAdapter"/>
    </listeners>
    <test name="My Tests">
        <classes>
            <class name="com.example.MyTest"/>
        </classes>
    </test>
</suite>
```

Or use the `@Listeners` annotation:

```java
@Listeners(TestNGAdapter.class)
public class MyTest {
    @Test
    public void myFirstTest() {
        Assert.assertTrue(true);
    }
}
```

### Step 3: Run Your Tests

```bash
mvn test
```

That is enough to start collecting the evidence PulseReport uses to report run health.

### Step 4: (Optional) Configure Output Formats

Create the PulseReport configuration file at `src/main/resources/reporter.properties`:

```properties
# Enable output formats
reporter.output.formats=html,json,junit
reporter.output.directory=target/pulsereport
```

All adapters and the CLI auto-detect this file (working directory, `src/main/resources/`, or classpath), so no client-side configuration code is needed. If present, `-Dreporter.output.directory` overrides the file's `reporter.output.directory`; otherwise reports go to the directory in the file, falling back to `target/pulsereport`.

### Step 5: (Optional) View HTML Reports

After running tests, open:

```bash
open target/pulsereport/test-report.html
```

## What You Get

### Automatic Capture

PulseReport automatically captures the signal most teams need first:

- **Test Results**: Pass, fail, skip status for every test
- **Timing**: Execution time for each test and suite
- **Stack Traces**: Full error details for failed tests
- **Test Metadata**: Descriptions, groups, priorities

### Multiple Output Formats

- **HTML**: Rich, interactive reports
- **JSON**: Machine-readable format for CI/CD integration
- **JUnit XML**: Compatible with Jenkins, Bamboo, TeamCity

### Framework Support

Choose the right adapter for your framework:

- **Cucumber**: `CucumberAdapter` — fully supported
- **REST-assured**: `RestAssuredAdapter` (captures API request/response) — fully supported
- **TestNG**: `TestNGAdapter` — in development
- **Selenium**: `SeleniumAdapter` (automatically captures screenshots) — in development
- **Appium**: `AppiumAdapter` (captures mobile app screenshots) — in development

## Your First Test with PulseReport

### Complete Example

```java
package com.example;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import io.github.pulsereport.adapters.testng.TestNGAdapter;

@Listeners(TestNGAdapter.class)
public class MyFirstTest {

    @Test(description = "Verify addition works correctly")
    public void testAddition() {
        int result = 2 + 2;
        Assert.assertEquals(result, 4, "Addition should return 4");
    }

    @Test(description = "Verify string concatenation")
    public void testStringConcat() {
        String result = "Hello" + " " + "World";
        Assert.assertEquals(result, "Hello World");
    }
}
```

### Run It

```bash
mvn test
```

### Expected Output

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.MyFirstTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## Next Steps

### For Web UI Testing

If you're testing web applications with Selenium:

```xml
<listener class-name="io.github.pulsereport.adapters.selenium.SeleniumAdapter"/>
```

See the [Selenium Example](../examples/selenium-example/README.md) for a complete working project.

### For API Testing

If you're testing REST APIs with REST-assured:

```xml
<listener class-name="io.github.pulsereport.adapters.restassured.RestAssuredAdapter"/>
```

See the [API Example](../examples/api-example/README.md) for a complete working project.

### For Mobile Testing

If you're testing mobile apps with Appium:

```xml
<listener class-name="io.github.pulsereport.adapters.appium.AppiumAdapter"/>
```

See the [Adapters Guide](adapters-guide.md#appium-adapter) for details.

## Adding Artifacts and Metrics

### Capture Screenshots

```java
import io.github.pulsereport.core.model.Artifact;
import java.time.Instant;

@Test
public void testWithScreenshot() {
    // Your test logic...
    
    // Capture screenshot
    Artifact screenshot = Artifact.builder()
        .name("screenshot.png")
        .type("screenshot")
        .content(screenshotBytes)
        .mimeType("image/png")
        .timestamp(Instant.now())
        .build();
    
    adapter.addArtifact("testWithScreenshot", screenshot);
}
```

### Record Metrics

```java
import io.github.pulsereport.core.model.Metric;
import java.time.Instant;

@Test
public void testWithMetric() {
    long start = System.currentTimeMillis();
    
    // Your test logic...
    
    long duration = System.currentTimeMillis() - start;
    
    // Record response time
    Metric responseTime = Metric.builder()
        .name("response.time")
        .value(duration)
        .unit("ms")
        .timestamp(Instant.now())
        .build();
    
    adapter.addMetric("testWithMetric", responseTime);
}
```

## Publishing Reports

### To Slack

Configure the PulseReport configuration file (`reporter.properties`):

```properties
reporter.slack.enabled=true
reporter.slack.webhookUrl=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
reporter.slack.channel=#test-results
reporter.slack.mentionOnFailure=@qa-team
```

### To S3

Configure the PulseReport configuration file (`reporter.properties`):

```properties
reporter.s3.enabled=true
reporter.s3.bucket=my-test-reports
reporter.s3.region=us-east-1
reporter.s3.keyPrefix=builds/${BUILD_ID}/
```

### Via HTTP

Configure the PulseReport configuration file (`reporter.properties`):

```properties
reporter.http.enabled=true
reporter.http.url=https://api.example.com/reports
reporter.http.method=POST
reporter.http.auth.type=bearer
reporter.http.auth.token=${API_TOKEN}
```

See the [Integrations Guide](integrations.md) for detailed setup instructions.

## Troubleshooting

### Tests Not Captured

**Problem**: No report generated after test execution.

**Solution**: Ensure the adapter is registered in `testng.xml` or via `@Listeners` annotation.

### Reports Not Generated

**Problem**: Adapter registered but no report files created.

**Solution**: Check the PulseReport configuration file (`reporter.properties`):

```properties
reporter.enabled=true
reporter.output.formats=html,json
reporter.output.directory=target/pulsereport
```

### ClassNotFoundException

**Problem**: `ClassNotFoundException: io.github.pulsereport.adapters.testng.TestNGAdapter`

**Solution**: Ensure the Maven dependency is added correctly and run `mvn clean install`.

## Complete Working Examples

- [TestNG Example](../examples/testng-example/) - Basic TestNG integration
- [Selenium Example](../examples/selenium-example/) - Web UI testing with screenshots
- [API Example](../examples/api-example/) - REST API testing with request/response capture

## Further Reading

- [Adapters Guide](adapters-guide.md) - Detailed adapter documentation
- [Configuration Reference](configuration.md) - All configuration options
- [Output Formats](output-formats.md) - Report format details
- [CLI Reference](cli-reference.md) - Command-line usage
- [Troubleshooting](troubleshooting.md) - Common issues and solutions
