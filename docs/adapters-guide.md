# Adapters Guide

PulseReport adapters connect framework events to one reporting layer so each run produces consistent signal and evidence. Use this guide to choose the right adapter and register it with minimal setup.

## Overview

Adapters bridge the gap between testing frameworks and PulseReport's reporting layer. Each adapter:

- Listens to framework-specific test events
- Translates events into PulseReport's data model
- Captures framework-specific artifacts (screenshots, API responses, etc.)
- Records framework-specific metrics (page load times, response times, etc.)

## Available Adapters

| Adapter | Framework | Use Case | Auto-Capture | Status |
| --------- | ----------- | ---------- | -------------- | -------- |
| `CucumberAdapter` | Cucumber | BDD step tracking | Background steps, scenario outlines, feature-to-suite mapping | Stable |
| `RestAssuredAdapter` | REST-assured | API testing | Request/response, timing | Stable |
| `TestNGAdapter` | TestNG | Basic test reporting | Test results, timing | In development |
| `SeleniumAdapter` | Selenium WebDriver | Web UI testing | Screenshots, page metrics | In development |
| `AppiumAdapter` | Appium | Mobile app testing | Screenshots, app metrics | In development |

> **Note:** Adapters marked *In development* may not behave as expected.

---

## TestNG Adapter

The foundation adapter that integrates with TestNG's listener system.

### Installation

```xml
<dependency>
    <groupId>io.github.pulsereport</groupId>
    <artifactId>pulsereport</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Configuration

#### Option 1: testng.xml (Recommended)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
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

#### Option 2: @Listeners Annotation

```java
import org.testng.annotations.Listeners;
import io.github.pulsereport.adapters.testng.TestNGAdapter;

@Listeners(TestNGAdapter.class)
public class MyTest {
    @Test
    public void testExample() {
        Assert.assertTrue(true);
    }
}
```

#### Option 3: Programmatic

```java
import org.testng.TestNG;
import io.github.pulsereport.adapters.testng.TestNGAdapter;

TestNG testng = new TestNG();
testng.addListener(new TestNGAdapter());
testng.setTestClasses(new Class[]{MyTest.class});
testng.run();
```

### Features

#### Automatic Test Capture

```java
@Test(description = "Test addition functionality")
public void testAddition() {
    int result = add(2, 3);
    assertEquals(result, 5);
}
// Automatically captured: test name, description, result, duration
```

#### Parameterized Tests

```java
@DataProvider(name = "testData")
public Object[][] provideData() {
    return new Object[][] {
        {1, 2, 3},
        {5, 5, 10}
    };
}

@Test(dataProvider = "testData")
public void testWithParameters(int a, int b, int expected) {
    assertEquals(add(a, b), expected);
}
// Each parameter combination is tracked separately
```

#### Groups and Priorities

```java
@Test(groups = {"smoke", "regression"}, priority = 1)
public void criticalTest() {
    // Test logic
}
// Groups and priorities are captured in the report
```

### Adding Artifacts

```java
import io.github.pulsereport.core.model.Artifact;
import java.time.Instant;

public class MyTest {
    private static TestNGAdapter adapter;
    
    @BeforeClass
    public void setup() {
        // Get adapter instance (injected or via static field)
        adapter = getAdapterInstance();
    }
    
    @Test
    public void testWithLog() {
        // Test logic...
        
        // Attach log file
        Artifact logFile = Artifact.builder()
            .name("test.log")
            .type("log")
            .content("Test execution log content...")
            .mimeType("text/plain")
            .timestamp(Instant.now())
            .build();
        
        adapter.addArtifact("testWithLog", logFile);
    }
}
```

### Adding Metrics

```java
import io.github.pulsereport.core.model.Metric;
import java.time.Instant;

@Test
public void testPerformance() {
    long start = System.currentTimeMillis();
    
    // Execute operation
    performOperation();
    
    long duration = System.currentTimeMillis() - start;
    
    // Record metric
    Metric metric = Metric.builder()
        .name("operation.duration")
        .value(duration)
        .unit("ms")
        .timestamp(Instant.now())
        .build();
    
    adapter.addMetric("testPerformance", metric);
}
```

---

## Selenium Adapter

Extends TestNGAdapter with Selenium WebDriver-specific features.

### Installation

Same as TestNGAdapter, plus Selenium dependency:

```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.16.1</version>
</dependency>
```

### Configuration

```xml
<suite name="Selenium Test Suite">
    <listeners>
        <listener class-name="io.github.pulsereport.adapters.selenium.SeleniumAdapter"/>
    </listeners>
    <!-- tests -->
</suite>
```

### Features

#### Automatic Screenshot Capture

Screenshots are automatically captured:

- On test success
- On test failure
- On test skip (if applicable)

```java
@Listeners(SeleniumAdapter.class)
public class WebTest {
    private WebDriver driver;
    
    @BeforeMethod
    public void setup() {
        driver = new ChromeDriver();
    }
    
    @Test
    public void testLogin() {
        driver.get("https://example.com");
        // Test logic...
        // Screenshot automatically captured on completion
    }
    
    @AfterMethod
    public void teardown() {
        driver.quit();
    }
}
```

#### Manual Screenshot Capture

```java
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import io.github.pulsereport.core.model.Artifact;

@Test
public void testWithCheckpoint() {
    driver.get("https://example.com");
    
    // Capture screenshot at checkpoint
    byte[] screenshot = ((TakesScreenshot) driver)
        .getScreenshotAs(OutputType.BYTES);
    
    Artifact screenshotArtifact = Artifact.builder()
        .name("checkpoint-screenshot.png")
        .type("screenshot")
        .content(screenshot)
        .mimeType("image/png")
        .timestamp(Instant.now())
        .build();
    
    adapter.addArtifact("testWithCheckpoint", screenshotArtifact);
    
    // Continue test...
}
```

#### Page Load Metrics

```java
@Test
public void testPageLoad() {
    long start = System.currentTimeMillis();
    driver.get("https://example.com");
    long loadTime = System.currentTimeMillis() - start;
    
    // Metric automatically recorded by SeleniumAdapter
    System.out.println("Page loaded in " + loadTime + "ms");
}
```

#### Browser Information

The adapter automatically captures:

- Browser name and version
- Operating system
- Screen resolution
- User agent

### Configuration Options

In the PulseReport configuration file (`reporter.properties`):

```properties
# Selenium screenshot settings
reporter.selenium.screenshot.enabled=true
reporter.selenium.screenshot.onSuccess=true
reporter.selenium.screenshot.onFailure=true
reporter.selenium.screenshot.format=png

# Page metrics
reporter.selenium.metrics.pageLoad=true
reporter.selenium.metrics.domReady=true
```

### Example

See [Selenium Example](../examples/selenium-example/) for a complete working project.

---

## Appium Adapter

Extends TestNGAdapter with Appium-specific features for mobile testing.

### Installation

```xml
<dependency>
    <groupId>io.appium</groupId>
    <artifactId>java-client</artifactId>
    <version>8.6.0</version>
</dependency>
```

### Configuration

```xml
<suite name="Mobile Test Suite">
    <listeners>
        <listener class-name="io.github.pulsereport.adapters.appium.AppiumAdapter"/>
    </listeners>
    <!-- tests -->
</suite>
```

### Features

#### Automatic Screenshot Capture

```java
@Listeners(AppiumAdapter.class)
public class MobileTest {
    private AppiumDriver driver;
    
    @BeforeMethod
    public void setup() {
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("platformName", "Android");
        caps.setCapability("deviceName", "emulator-5554");
        caps.setCapability("app", "/path/to/app.apk");
        
        driver = new AndroidDriver(
            new URL("http://localhost:4723/wd/hub"), caps);
    }
    
    @Test
    public void testMobileApp() {
        // Test logic...
        // Screenshot automatically captured
    }
    
    @AfterMethod
    public void teardown() {
        driver.quit();
    }
}
```

#### Device Information

Automatically captured:

- Platform (iOS/Android)
- Device model
- OS version
- App version
- Screen resolution

#### App Logs

```java
@Test
public void testWithLogs() {
    // Perform actions...
    
    // Capture app logs
    List<LogEntry> logs = driver.manage().logs().get("logcat").getAll();
    
    String logContent = logs.stream()
        .map(LogEntry::toString)
        .collect(Collectors.joining("\n"));
    
    Artifact logArtifact = Artifact.builder()
        .name("app-logs.txt")
        .type("log")
        .content(logContent)
        .mimeType("text/plain")
        .timestamp(Instant.now())
        .build();
    
    adapter.addArtifact("testWithLogs", logArtifact);
}
```

#### App Performance Metrics

```java
@Test
public void testAppLaunch() {
    long start = System.currentTimeMillis();
    
    driver.launchApp();
    
    // Wait for app to be ready
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(d -> d.findElement(By.id("main-screen")).isDisplayed());
    
    long launchTime = System.currentTimeMillis() - start;
    
    Metric launchMetric = Metric.builder()
        .name("app.launch.time")
        .value(launchTime)
        .unit("ms")
        .timestamp(Instant.now())
        .build();
    
    adapter.addMetric("testAppLaunch", launchMetric);
}
```

---

## REST-assured Adapter

A standalone REST-assured filter that captures HTTP request/response data for reporting. It works independently — no TestNG dependency required — making it compatible with TestNG, Cucumber/JUnit, or any other test runner.

> **Note:** Sensitive header masking is enabled by default. The `Authorization`, `X-API-Key`, `Cookie`, and `Set-Cookie` headers are masked as `***REDACTED***` in captured artifacts. See [Configuration](#configuration-options) to customize.

### Installation

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.4.0</version>
</dependency>
```

### Registration

#### Option 1: Global filter (Recommended for Cucumber / JUnit)

Register the adapter as a global REST-assured filter in a `@Before` hook:

```java
import io.github.pulsereport.adapters.restassured.RestAssuredAdapter;
import io.restassured.RestAssured;
import io.cucumber.java.Before;

public class Hooks {
    @Before
    public void setUp() {
        RestAssured.filters(new RestAssuredAdapter());
    }
}
```

#### Option 2: With custom configuration

```java
import io.github.pulsereport.core.ReporterConfig;

ReporterConfig config = ReporterConfig.builder()
    .maskSensitiveData(true)
    .sensitiveHeaders("Authorization,X-API-Key,Cookie,Set-Cookie,X-Custom-Secret")
    .build();

RestAssured.filters(new RestAssuredAdapter(config));
```

#### Option 3: TestNG listener (if using TestNG)

```xml
<suite name="API Test Suite">
    <listeners>
        <listener class-name="io.github.pulsereport.adapters.restassured.RestAssuredAdapter"/>
    </listeners>
    <!-- tests -->
</suite>
```

### Features

#### Automatic Request/Response Capture

```java
public class ApiTest {
    
    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://api.example.com";
        RestAssured.filters(new RestAssuredAdapter());
    }
    
    @Test
    public void testGetUser() {
        given()
            .pathParam("id", 1)
        .when()
            .get("/users/{id}")
        .then()
            .statusCode(200)
            .body("name", notNullValue());
        
        // Request and response automatically captured
    }
}
```

#### Manual Request/Response Logging

```java
@Test
public void testCreateUser() {
    String requestBody = "{ \"name\": \"John\", \"email\": \"john@example.com\" }";
    
    Response response = given()
        .contentType("application/json")
        .body(requestBody)
    .when()
        .post("/users")
    .then()
        .statusCode(201)
    .extract()
        .response();
    
    // Capture request
    Artifact requestArtifact = Artifact.builder()
        .name("create-user-request.json")
        .type("http-request")
        .content(requestBody)
        .mimeType("application/json")
        .timestamp(Instant.now())
        .build();
    
    adapter.addArtifact("testCreateUser", requestArtifact);
    
    // Capture response
    Artifact responseArtifact = Artifact.builder()
        .name("create-user-response.json")
        .type("http-response")
        .content(response.asString())
        .mimeType("application/json")
        .timestamp(Instant.now())
        .build();
    
    adapter.addArtifact("testCreateUser", responseArtifact);
}
```

#### Response Time Metrics

```java
@Test
public void testApiPerformance() {
    Response response = given()
        .when()
            .get("/users")
        .then()
            .statusCode(200)
        .extract()
            .response();
    
    long responseTime = response.getTime();
    
    Metric metric = Metric.builder()
        .name("api.response.time")
        .value(responseTime)
        .unit("ms")
        .timestamp(Instant.now())
        .build();
    
    adapter.addMetric("testApiPerformance", metric);
}
```

#### Authentication

```java
@Test
public void testWithAuth() {
    given()
        .auth()
            .basic("username", "password")
    .when()
        .get("/secure-endpoint")
    .then()
        .statusCode(200);
    
    // Authentication details captured (credentials masked)
}
```

#### Duration Formatting

Response durations are automatically formatted in human-readable units (e.g., `245ms`, `1.2s`, `2m 15s`) instead of always showing raw milliseconds.

### Configuration Options

In the PulseReport configuration file (`reporter.properties`):

```properties
# REST-assured settings
reporter.restassured.logging.request=true
reporter.restassured.logging.response=true
reporter.restassured.logging.headers=true
reporter.restassured.logging.body=true
reporter.restassured.mask.credentials=true

# Sensitive data masking (enabled by default)
reporter.maskSensitiveData=true
reporter.sensitiveHeaders=Authorization,X-API-Key,Cookie,Set-Cookie
```

### Example

See [API Example](../examples/api-example/) for a complete working project.

---

## Cucumber Adapter

The Cucumber adapter integrates PulseReport with Cucumber JVM for BDD test reporting. It maps Gherkin features, scenarios, and steps to PulseReport's data model.

### Registration

Register the adapter as a Cucumber plugin. Choose one of these methods:

**Option 1: `junit-platform.properties`** (recommended for JUnit Platform runner)

```properties
cucumber.plugin=io.github.pulsereport.adapters.cucumber.CucumberAdapter
```

**Option 2: `@CucumberOptions` annotation**

```java
@CucumberOptions(
    plugin = {"io.github.pulsereport.adapters.cucumber.CucumberAdapter"},
    features = "classpath:features",
    glue = "com.example.steps"
)
public class CucumberRunner { }
```

**Option 3: CLI**

```bash
--plugin io.github.pulsereport.adapters.cucumber.CucumberAdapter
```

### Maven Dependency

```xml
<dependency>
    <groupId>io.github.pulsereport</groupId>
    <artifactId>pulsereport</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Cucumber (bring your own version) -->
<dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.15.0</version>
</dependency>
```

> **Note:** PulseReport declares Cucumber as a `provided` dependency — you must include your own Cucumber version.

### Data Model Mapping

| Gherkin Concept | PulseReport Model | Details |
| ----------------- | ------------------- | --------- |
| Feature | `TestSuite` | Feature name becomes suite name; file path in `secondaryText` |
| Scenario | `TestCase` | `bddType = "scenario"` |
| Scenario Outline | `TestCase` | `bddType = "scenario_outline"` |
| Step | `TestStep` | Includes keyword, name, status, duration |
| Background | `TestCase.backgroundSteps` | Background steps are attached to each scenario |
| Tags | `TestCase.tags` / `TestSuite.tags` | Feature tags propagate to suite; scenario tags to test case |

### Features

- **Automatic step tracking** — All Given/When/Then steps are captured with individual pass/fail status
- **Background step separation** — Background steps appear in a dedicated section per scenario
- **Scenario Outline support** — Each example row generates a separate test case
- **Feature-level grouping** — Tests are grouped by `.feature` file into suites
- **Data tables and doc strings** — Step arguments are captured and displayed in reports
- **Parallel execution support** — Thread-safe via `ThreadLocal` scenario context
- **Artifact attachment** — Attach screenshots or HTTP data to specific steps using `CucumberStepContext`

### Attaching Artifacts to Steps

Use `CucumberStepContext` to attach artifacts (screenshots, API calls) to the current step:

```java
import io.github.pulsereport.adapters.cucumber.CucumberStepContext;
import io.github.pulsereport.core.model.Artifact;

@When("I call the API")
public void iCallTheApi() {
    // ... your step logic ...
    
    Artifact httpRequest = Artifact.builder()
        .name("POST /api/users")
        .type("http-request")
        .content("POST /api/users\nContent-Type: application/json\n\n{\"name\":\"John\"}")
        .mimeType("text/plain")
        .build();
    CucumberStepContext.addArtifact(httpRequest);
}
```

### Combining with REST-assured Adapter

For API testing with Cucumber, register the REST-assured adapter as a global filter:

```java
import io.github.pulsereport.adapters.restassured.RestAssuredAdapter;
import io.restassured.RestAssured;
import io.cucumber.java.Before;

@Before
public void setUp() {
    RestAssured.filters(new RestAssuredAdapter());
}
```

HTTP request/response artifacts captured by the REST-assured filter are automatically attached to the current BDD step. Sensitive headers (`Authorization`, `X-API-Key`, `Cookie`, `Set-Cookie`) are masked by default.

### Report Output

On test run completion, the adapter generates:

- `target/pulsereport/test-report.html` — Interactive HTML report with BDD step display
- `target/pulsereport/test-report.json` — Machine-readable JSON

### Configuration

The Cucumber adapter uses the default PulseReport output directory (`target/pulsereport/`). To customize, set system properties:

```bash
mvn test -Dpulsereport.output.dir=custom/output/path
```

---

## Adapter Comparison

| Feature | TestNG | Selenium | Appium | REST-assured |
| --------- | -------- | ---------- | -------- | -------------- |
| Test Results | ✅ | ✅ | ✅ | ✅ |
| Timing Metrics | ✅ | ✅ | ✅ | ✅ |
| Screenshots | ❌ | ✅ Auto | ✅ Auto | ❌ |
| Page Metrics | ❌ | ✅ | ✅ | ❌ |
| Request/Response | ❌ | ❌ | ❌ | ✅ Auto |
| Device Info | ❌ | ✅ | ✅ | ❌ |
| Custom Artifacts | ✅ | ✅ | ✅ | ✅ |
| Custom Metrics | ✅ | ✅ | ✅ | ✅ |

---

## Best Practices

### 1. Choose the Right Adapter

- **Unit/Integration Tests**: Use `TestNGAdapter`
- **Web UI Tests**: Use `SeleniumAdapter`
- **Mobile Tests**: Use `AppiumAdapter`
- **API Tests**: Use `RestAssuredAdapter`

### 2. Clean Up Resources

Always close drivers and connections in `@AfterMethod` or `@AfterClass`:

```java
@AfterMethod
public void teardown() {
    if (driver != null) {
        driver.quit();
    }
}
```

### 3. Use Descriptive Test Names

```java
@Test(description = "Verify user can login with valid credentials")
public void testValidLogin() {
    // Clear intent from description
}
```

### 4. Capture Context on Failure

```java
@Test
public void testWithContext() {
    try {
        // Test logic
    } catch (AssertionError e) {
        // Capture additional context on failure
        capturePageSource();
        captureLogs();
        throw e;
    }
}
```

### 5. Limit Artifact Size

Large artifacts can slow down report generation:

```java
// Good: Capture essential data
Artifact screenshot = captureScreenshot();

// Bad: Capture entire video (MB in size)
Artifact video = captureFullVideo(); // Avoid unless necessary
```

---

## Troubleshooting

### Adapter Not Capturing Tests

**Problem**: Tests run but no report is generated.

**Solution**: Verify adapter is registered in `testng.xml` or via `@Listeners`.

### Screenshots Not Captured (Selenium/Appium)

**Problem**: Tests run but screenshots are missing.

**Solution**: Check configuration:

```properties
reporter.selenium.screenshot.enabled=true
reporter.selenium.screenshot.onSuccess=true
reporter.selenium.screenshot.onFailure=true
```

### Request/Response Not Captured (REST-assured)

**Problem**: API tests run but request/response not in report.

**Solution**: Enable logging:

```properties
reporter.restassured.logging.request=true
reporter.restassured.logging.response=true
```

### Memory Issues with Large Suites

**Problem**: OutOfMemoryError when running many tests.

**Solution**: Use streaming aggregator:

```properties
reporter.aggregator.type=streaming
reporter.aggregator.streaming.flushInterval=100
```

---

## Next Steps

- [Configuration Reference](configuration.md) - Customize adapter behavior
- [Output Formats](output-formats.md) - View captured data in reports
- [Integrations](integrations.md) - Publish reports to external systems
- [Complete Examples](../examples/) - Working example projects
