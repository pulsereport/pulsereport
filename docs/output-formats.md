# Output Formats

PulseReport can emit human-readable and machine-readable outputs from the same run so teams keep one source of evidence and multiple ways to consume it. Use this guide to tune the formats that matter for local debugging, CI signal, and release confidence.

## Supported Formats

| Format | Use Case | Features |
| -------- | ---------- | ---------- |
| **HTML** | Human-readable reports | Interactive, screenshots, search/filter |
| **JSON** | Machine processing, CI/CD | Complete data, queryable |
| **JUnit XML** | CI/CD integration | Jenkins, TeamCity, Bamboo |

## Configuration

### Enable Output Formats

In the PulseReport configuration file (`reporter.properties`):

```properties
# Formats to generate (comma-separated: html, json, junit)
reporter.output.formats=html,json,junit

# Output directory
reporter.output.directory=target/pulsereport
```

> **Note:** `reporter.output.formats` is honored **only by the PulseReport CLI**. The TestNG and Cucumber adapters always generate both `test-report.html` and `test-report.json`, regardless of this setting.

---

## HTML Reports

Rich, interactive HTML reports for human consumption.

### Features

- **Verdict and run timeline**: The header states the outcome ("3 failed of 46 tests") next to a timeline with one bar per test in execution order; bar height shows duration and color shows status. Click a bar to open that test.
- **Failure first**: The first failed test opens automatically. When nothing failed, the report lists the slowest tests instead.
- **Run metadata**: The top bar shows environment details recorded by adapters (browser, device, platform, and so on) plus start and finish times.
- **Search and filter**: Search by test name, suite, tag, class or error text, and filter by status.
- **Keyboard navigation**: `↑`/`↓` move between tests, `f` toggles failures only, `/` focuses search.
- **Readable failures**: Exception name and message first, with framework stack frames collapsed and a copy button for the full trace.
- **BDD steps**: Cucumber scenarios show Given/When/Then steps with status, data tables, doc strings and per-step attachments.
- **HTTP exchanges**: REST-assured requests and responses side by side, with status, headers and pretty-printed JSON bodies.
- **Screenshots and recordings**: Embedded inline; click a screenshot to enlarge it.
- **Light and dark themes**: Follows the operating system or browser theme, with a toggle in the top bar.
- **Deep links**: The URL hash (`#t=<test id>`) opens a specific test.
- **Responsive and printable**: Works on narrow screens; printing outputs every failure with its full stack trace.

### Generated Files

```text
target/pulsereport/
└── test-report.html          # Self-contained single-file report
```

### Opening HTML Reports

```bash
# macOS
open target/pulsereport/test-report.html

# Linux
xdg-open target/pulsereport/test-report.html

# Windows
start target/pulsereport/test-report.html
```

### Customization

#### Report Title

The title in the top bar and browser tab defaults to the run name. Override it with `reporter.report.title` in `reporter.properties` or `-Dreporter.report.title=...`.

#### Themes

The report follows the operating system or browser theme. Picking the other theme with the top-bar toggle is remembered in `localStorage` until the system theme changes again.

> **Note:** Runtime customization of the HTML report (custom CSS, custom templates) is not yet supported for projects consuming PulseReport as a dependency. The report uses a fixed built-in template. If you are contributing to PulseReport itself, the color tokens (`--surface`, `--raised`, `--ink`, `--accent`, `--pass`, `--fail`, `--skip`, and so on) are defined at the top of `src/main/resources/templates/html-report.ftl`.

### Report Layout

- **Top bar**: Report title, run metadata, theme toggle
- **Verdict band**: Outcome headline, passed/skipped breakdown, run timeline
- **Test list**: Tests grouped by suite or feature, with status filters and search
- **Detail pane**: For the selected test, in order: failure, steps, HTTP exchanges, screenshots, recordings, attachments, then details (class, method, start time, duration, retries, metrics)

### Example

The HTML report is a self-contained single file. All CSS, JavaScript, and assets (including base64-encoded screenshots) are inlined. Open it directly in any browser.

---

## JSON Reports

Machine-readable JSON format for integration and processing.

### Structure

The `TestRun` object is serialized **directly at the document root** — there is no wrapper element. Output is always indented. The example below mirrors a real generated report (from the Appium example):

```json
{
  "id" : "e27c1458-b0e6-4271-bc8e-c173ae4c5f89",
  "name" : "Surefire suite",
  "startTime" : "2026-06-05T13:59:24.844Z",
  "endTime" : "2026-06-05T14:00:58.912Z",
  "duration" : 94068,
  "status" : "FAILED",
  "suites" : [ {
    "id" : "66c6463f-4995-4051-9fd8-6d47ab1c9390",
    "name" : "Surefire test",
    "startTime" : "2026-06-05T13:59:24.844Z",
    "endTime" : "2026-06-05T14:00:58.912Z",
    "duration" : 94068,
    "status" : "FAILED",
    "testCases" : [ {
      "id" : "Surefire suite.Surefire test.io.github.pulsereport.examples.appium.IOSAppTest.testAppLaunch_testAppLaunch",
      "name" : "Verify iOS app launches and product catalog is displayed",
      "className" : "io.github.pulsereport.examples.appium.IOSAppTest",
      "methodName" : "testAppLaunch",
      "startTime" : "2026-06-05T14:00:09.334Z",
      "endTime" : "2026-06-05T14:00:29.989Z",
      "duration" : 20655,
      "status" : "FAILED",
      "errorMessage" : "Expected condition failed: waiting for visibility of element ...",
      "stackTrace" : "org.openqa.selenium.TimeoutException: Expected condition failed: ...",
      "steps" : [ ],
      "artifacts" : [ {
        "name" : "testAppLaunch-failed.png",
        "type" : "screenshot",
        "path" : "/var/folders/.../screenshot2624571246221067343.png",
        "mimeType" : "image/png",
        "size" : 772358,
        "timestamp" : "2026-06-05T14:00:30.224599Z"
      } ],
      "metrics" : [ ],
      "retryCount" : 0,
      "backgroundSteps" : [ ],
      "tags" : [ ]
    } ],
    "totalTests" : 3,
    "passedTests" : 0,
    "failedTests" : 3,
    "skippedTests" : 0,
    "tags" : [ ]
  } ],
  "environment" : {
    "automationName" : "XCuiTest",
    "platformName" : "iOS",
    "udid" : "C6E13B79-9B0E-4DBC-AFA0-C64C11AE9C9E",
    "device" : "iPhone 17 Pro",
    "appName" : "My Demo App.app",
    "platformVersion" : "26.0"
  },
  "totalTests" : 3,
  "passedTests" : 0,
  "failedTests" : 3,
  "skippedTests" : 0
}
```

### Generated Files

```
target/pulsereport/
└── test-report.json
```

### Schema

The JSON output is the `TestRun` model serialized directly at the root, with nested `TestSuite`, `TestCase`, `TestStep`, `Artifact`, and `Metric` objects.

Key facts:

- **Root object** is the test run itself — there is no `testRun` wrapper and no `summary` object. The counters `totalTests`, `passedTests`, `failedTests`, and `skippedTests` are flat fields on both the run and each suite.
- **Status values** are `PASSED`, `FAILED`, `SKIPPED`, or `FLAKY` only.
- **`environment`** is a map of adapter-supplied key/value pairs; its contents vary by adapter.
- **Indentation is always enabled** — there is no configuration to produce compact output.
- Error messages, stack traces, artifact metadata, metrics, steps, tags, and retry counts are included whenever present.

### Processing JSON Reports

#### JQ (Command Line)

```bash
# Get run-level counts
jq '.totalTests, .passedTests, .failedTests, .skippedTests' test-report.json

# Get failed tests
jq '.suites[].testCases[] | select(.status == "FAILED")' test-report.json

# Get overall run status
jq '.status' test-report.json
```

#### Python

```python
import json

with open('test-report.json', 'r') as f:
    report = json.load(f)

print(f"Total: {report['totalTests']}")
print(f"Passed: {report['passedTests']}")
print(f"Failed: {report['failedTests']}")
print(f"Skipped: {report['skippedTests']}")
```

#### JavaScript/Node.js

```javascript
const fs = require('fs');

const report = JSON.parse(fs.readFileSync('test-report.json', 'utf8'));

console.log(`Total: ${report.totalTests}`);
console.log(`Passed: ${report.passedTests}`);
console.log(`Failed: ${report.failedTests}`);
console.log(`Skipped: ${report.skippedTests}`);
```

---

## JUnit XML Reports

Standard JUnit XML format for CI/CD integration.

> **CLI only:** JUnit XML is generated only by the PulseReport CLI. The TestNG and Cucumber adapters generate only `test-report.html` and `test-report.json` — they never produce JUnit XML. Generate it from an existing JSON report:
>
> ```bash
> pulsereport generate -i target/pulsereport/test-report.json -f junit
> ```

### Structure

The generator emits only `testsuites`, `testsuite`, and `testcase` elements:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites tests="50" failures="2" errors="0" skipped="1" time="90.000">
  <testsuite name="Calculator Tests" tests="50" failures="2" errors="0" skipped="1" time="90.000">
    <testcase name="testAddition" classname="com.example.CalculatorTest" time="5.000" />
    <testcase name="testDivision" classname="com.example.CalculatorTest" time="3.200">
      <failure message="Division by zero">java.lang.ArithmeticException: Division by zero
    at com.example.Calculator.divide(Calculator.java:25)
    at com.example.CalculatorTest.testDivision(CalculatorTest.java:42)</failure>
    </testcase>
    <testcase name="testSkipped" classname="com.example.CalculatorTest" time="0.000">
      <skipped />
    </testcase>
  </testsuite>
</testsuites>
```

Notes on the emitted XML:

- `time` attributes are in seconds (converted from the model's millisecond durations).
- Passed and flaky tests are self-closing `<testcase ... />` elements.
- Skipped tests contain an empty `<skipped />` element (no `message` attribute).
- Failed tests contain a `<failure message="...">` element whose text is the XML-escaped stack trace.
- No `<properties>`, `<system-out>`, or `<system-err>` elements are emitted.

### Generated Files

```
target/pulsereport/
└── test-report.xml          # Generated by the CLI only
```

### CI/CD Integration

#### Jenkins

```groovy
stage('Test') {
    steps {
        sh 'mvn test'
    }
}

stage('Reports') {
    steps {
        sh 'pulsereport generate -i target/pulsereport/test-report.json -f junit'
    }
}

stage('Publish Results') {
    steps {
        junit 'target/pulsereport/test-report.xml'
    }
}
```

#### GitLab CI

```yaml
test:
  script:
    - mvn test
    - pulsereport generate -i target/pulsereport/test-report.json -f junit
  artifacts:
    reports:
      junit: target/pulsereport/test-report.xml
```

#### GitHub Actions

```yaml
- name: Run Tests
  run: mvn test

- name: Generate JUnit XML
  if: always()
  run: pulsereport generate -i target/pulsereport/test-report.json -f junit

- name: Publish Test Results
  uses: EnricoMi/publish-unit-test-result-action@v2
  if: always()
  with:
    files: target/pulsereport/test-report.xml
```

#### Azure Pipelines

```yaml
- task: Maven@3
  inputs:
    goals: 'test'

- script: pulsereport generate -i target/pulsereport/test-report.json -f junit
  displayName: 'Generate JUnit XML'

- task: PublishTestResults@2
  inputs:
    testResultsFormat: 'JUnit'
    testResultsFiles: '**/test-report.xml'
```

---

## Custom Output Formats

### Creating a Custom Output Plugin

Implement the `OutputGenerator` interface. It defines two methods — one writing to a single output **file**, and one writing to an `OutputStream`:

```java
package com.example;

import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.outputs.OutputGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class CustomOutputGenerator implements OutputGenerator {

    @Override
    public void generate(TestRun testRun, File outputFile) throws IOException {
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        try (OutputStream out = new FileOutputStream(outputFile)) {
            generate(testRun, out);
        }
    }

    @Override
    public void generate(TestRun testRun, OutputStream outputStream) throws IOException {
        PrintWriter writer = new PrintWriter(outputStream);
        writer.println("Custom Report");
        writer.println("=============");
        writer.println("Total Tests: " + testRun.getTotalTests());
        writer.println("Passed:      " + testRun.getPassedTests());
        writer.println("Failed:      " + testRun.getFailedTests());
        writer.println("Skipped:     " + testRun.getSkippedTests());
        writer.flush();
    }
}
```

There is no configuration-based registration mechanism — the CLI's supported formats (`html`, `json`, `junit`) are hardcoded. Use a custom generator programmatically instead:

```java
TestRun testRun = ...; // e.g. deserialize test-report.json with Jackson
new CustomOutputGenerator().generate(testRun, new File("target/pulsereport/custom-report.txt"));
```

---

## Comparison Table

| Feature | HTML | JSON | JUnit XML |
| --------- | ------ | ------ | ----------- |
| Human Readable | ✅ Excellent | ❌ No | ⚠️ Limited |
| Machine Readable | ❌ No | ✅ Excellent | ✅ Good |
| Screenshots | ✅ Embedded | ✅ Metadata | ❌ No |
| CI/CD Integration | ⚠️ Manual | ✅ API | ✅ Native |
| File Size | 🔴 Large | 🟡 Medium | 🟢 Small |
| Search/Filter | ✅ Yes | 🔧 JQ/Script | ❌ No |

---

## Best Practices

### 1. Use Multiple Formats

The TestNG and Cucumber adapters always generate both HTML (for humans) and JSON (for automation). Add JUnit XML (for CI/CD) by regenerating with the CLI:

```bash
pulsereport generate -i target/pulsereport/test-report.json -f html,json,junit
```

### 2. Optimize Artifact Size

Large screenshots and log attachments increase report size. Capture screenshots at a reasonable resolution in your tests, and attach large logs only when they are needed for debugging.

### 3. Archive Reports

Keep historical reports for trend analysis:

```bash
# Archive with timestamp
mv target/pulsereport target/pulsereport-$(date +%Y%m%d-%H%M%S)
```

### 4. Secure Sensitive Data

Mask sensitive information in reports:

```properties
# Masking is enabled by default; these are the master switch and
# per-category controls for REST API tests
reporter.maskSensitiveData=true
reporter.maskHeaders.fields=Authorization,X-API-Key,Cookie,Set-Cookie
reporter.maskBody.fields=password,secret,token
```

---

## Troubleshooting

### Large HTML Files

**Problem**: HTML report is too large to open in browser.

**Solution**:

- Reduce the size and number of attached artifacts (screenshots, logs, videos).
- Split very large test runs into smaller suites.

### Missing Screenshots in HTML

**Problem**: Screenshots not appearing in HTML report.

**Solution**: Screenshots appear only when tests attach them as artifacts (for example, by capturing a screenshot in a failure handler or via the Selenium/Appium adapter helpers). Verify the artifacts are present in `test-report.json` under each test case's `artifacts` array.

### Invalid JUnit XML

**Problem**: CI/CD system rejects JUnit XML.

**Solution**: Validate the CLI-generated file:

```bash
xmllint --noout target/pulsereport/test-report.xml
```

---

## Next Steps

- [Configuration Reference](configuration.md) - Detailed configuration options
- [Integrations Guide](integrations.md) - Publish reports to external systems
- [CLI Reference](cli-reference.md) - Generate reports from command line
