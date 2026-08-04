# Output Formats

PulseReport can emit human-readable and machine-readable outputs from the same run so teams keep one source of evidence and multiple ways to consume it. Use this guide to tune the formats that matter for local debugging, CI signal, and release confidence.

## Supported Formats

| Format | Use Case | Features |
| -------- | ---------- | ---------- |
| **HTML** | Human-readable reports | Interactive, charts, screenshots |
| **JSON** | Machine processing, CI/CD | Complete data, queryable |
| **JUnit XML** | CI/CD integration | Jenkins, TeamCity, Bamboo |

## Configuration

### Enable Output Formats

In the PulseReport configuration file (`reporter.properties`):

```properties
# Enable multiple formats (comma-separated)
reporter.output.formats=html,json,junit

# Output directory
reporter.output.directory=target/pulsereport

# Reporter metadata
reporter.name=PulseReport
reporter.version=1.0.0
```

### Format-Specific Configuration

```properties
# JSON settings
reporter.json.pretty=true
reporter.json.includeStackTraces=true

# JUnit XML settings
reporter.junit.includeSystemOut=true
reporter.junit.includeSystemErr=true
```

---

## HTML Reports

Rich, interactive HTML reports for human consumption.

### Features

- **Test Summary**: Pass/fail/skip counts with percentages
- **Dark/Light Mode**: Built-in theme toggle with `localStorage` persistence
- **Screenshot Gallery**: Embedded screenshots for Selenium/Appium tests
- **Expandable Details**: Click to expand stack traces and logs
- **Search and Filter**: Find tests by name, status, or tags
- **Responsive Design**: Works on desktop and mobile

### Generated Files

```
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

#### Dark Mode

The HTML report includes a built-in dark/light mode toggle. Users can switch themes via the button in the report header. The preference is persisted in `localStorage`.

#### CSS Token System

The report uses a two-layer CSS custom property system for consistent theming:

**Primitives** — raw color values (e.g., `--color-teal-600`, `--color-gray-100`)

**Semantic tokens** — functional meanings that reference primitives:

```css
:root {
    /* Surfaces */
    --bg: var(--color-gray-100);
    --surface: var(--color-gray-0);
    --border: var(--color-gray-300);
    /* Text */
    --text-primary: var(--color-gray-900);
    --text-secondary: var(--color-gray-600);
    /* Accent */
    --accent: var(--color-teal-600);
    /* Status */
    --green: var(--color-green-600);
    --red: var(--color-red-500);
    --amber: var(--color-amber-400);
    --not-run: var(--color-gray-600);
}
```

> **Note:** Runtime customization of the HTML report (custom CSS, custom templates) is not yet supported for projects consuming PulseReport as a dependency. The report uses a fixed built-in template. If you are contributing to PulseReport itself, modify the tokens in `src/main/resources/templates/html-report.ftl`.

### Report Sections

#### 1. Summary Section

```
Test Execution Summary
━━━━━━━━━━━━━━━━━━━━━
Total Tests:    150
Passed:         142 (94.7%)
Failed:         6   (4.0%)
Skipped:        2   (1.3%)

Duration:       2m 45s
Start Time:     2026-02-16 10:30:00
End Time:       2026-02-16 10:32:45
```

#### 2. Test Details

Each test shows:

- Test name and description
- Status (✅ Pass, ❌ Fail, ⊘ Skip)
- Execution time
- Parameters (for data-driven tests)
- Stack trace (for failures)
- Artifacts (screenshots, logs)
- Metrics (response times, etc.)

#### 3. Artifacts

Screenshots and API call artifacts displayed inline with expand/collapse controls.

### Example

The HTML report is a self-contained single file. All CSS, JavaScript, and assets (including base64-encoded screenshots) are inlined. Open it directly in any browser.

---

## JSON Reports

Machine-readable JSON format for integration and processing.

### Structure

```json
{
  "testRun": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "My Test Suite",
    "startTime": "2026-02-16T10:30:00Z",
    "endTime": "2026-02-16T10:32:45Z",
    "duration": 165000,
    "status": "COMPLETED",
    "suites": [
      {
        "id": "suite-1",
        "name": "Calculator Tests",
        "startTime": "2026-02-16T10:30:00Z",
        "endTime": "2026-02-16T10:31:30Z",
        "duration": 90000,
        "testCases": [
          {
            "id": "test-1",
            "name": "testAddition",
            "description": "Verify addition of two numbers",
            "status": "PASSED",
            "startTime": "2026-02-16T10:30:00Z",
            "endTime": "2026-02-16T10:30:05Z",
            "duration": 5000,
            "steps": [],
            "artifacts": [],
            "metrics": [],
            "errorMessage": null,
            "stackTrace": null
          }
        ]
      }
    ],
    "summary": {
      "totalTests": 150,
      "passedTests": 142,
      "failedTests": 6,
      "skippedTests": 2,
      "successRate": 94.7
    }
  }
}
```

### Generated Files

```
target/pulsereport/
└── test-report.json
```

### Schema

The JSON output structure mirrors the `TestRun` model and its nested objects (`TestSuite`, `TestCase`, `TestStep`, `Artifact`, `Metric`).

### Pretty Printing

```properties
# Enable pretty printing (default: false)
reporter.json.pretty=true
```

Pretty printed output:

```json
{
  "testRun": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "My Test Suite"
  }
}
```

Compact output:

```json
{"testRun":{"id":"550e8400-e29b-41d4-a716-446655440000","name":"My Test Suite"}}
```

### Include/Exclude Data

```properties
# Include full stack traces (can be large)
reporter.json.includeStackTraces=true

# Include artifact content (base64 encoded)
reporter.json.includeArtifacts=true

# Include metric details
reporter.json.includeMetrics=true
```

### Processing JSON Reports

#### JQ (Command Line)

```bash
# Get test summary
jq '.testRun.summary' test-report.json

# Get failed tests
jq '.testRun.suites[].testCases[] | select(.status == "FAILED")' test-report.json

# Get test count
jq '.testRun.summary.totalTests' test-report.json
```

#### Python

```python
import json

with open('test-report.json', 'r') as f:
    report = json.load(f)

summary = report['testRun']['summary']
print(f"Total: {summary['totalTests']}")
print(f"Passed: {summary['passedTests']}")
print(f"Failed: {summary['failedTests']}")
print(f"Success Rate: {summary['successRate']}%")
```

#### JavaScript/Node.js

```javascript
const fs = require('fs');

const report = JSON.parse(fs.readFileSync('test-report.json', 'utf8'));
const summary = report.testRun.summary;

console.log(`Total: ${summary.totalTests}`);
console.log(`Passed: ${summary.passedTests}`);
console.log(`Failed: ${summary.failedTests}`);
console.log(`Success Rate: ${summary.successRate}%`);
```

---

## JUnit XML Reports

Standard JUnit XML format for CI/CD integration.

### Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuites name="My Test Suite" time="165.0" tests="150" failures="6" skipped="2">
    <testsuite name="Calculator Tests" time="90.0" tests="50" failures="2" skipped="1">
        <properties>
            <property name="java.version" value="17.0.2"/>
            <property name="os.name" value="Mac OS X"/>
        </properties>
        
        <testcase name="testAddition" classname="com.example.CalculatorTest" time="5.0">
            <!-- Passed test - no additional elements -->
        </testcase>
        
        <testcase name="testDivision" classname="com.example.CalculatorTest" time="3.2">
            <failure message="Division by zero" type="java.lang.ArithmeticException">
                <![CDATA[
java.lang.ArithmeticException: Division by zero
    at com.example.Calculator.divide(Calculator.java:25)
    at com.example.CalculatorTest.testDivision(CalculatorTest.java:42)
                ]]>
            </failure>
        </testcase>
        
        <testcase name="testSkipped" classname="com.example.CalculatorTest" time="0.0">
            <skipped message="Test disabled"/>
        </testcase>
        
        <system-out><![CDATA[Test execution output]]></system-out>
        <system-err><![CDATA[Error output]]></system-err>
    </testsuite>
</testsuites>
```

### Generated Files

```
target/pulsereport/
└── TEST-junit.xml
```

### CI/CD Integration

#### Jenkins

```groovy
stage('Test') {
    steps {
        sh 'mvn test'
    }
}

stage('Publish Results') {
    steps {
        junit 'target/pulsereport/TEST-junit.xml'
    }
}
```

#### GitLab CI

```yaml
test:
  script:
    - mvn test
  artifacts:
    reports:
      junit: target/pulsereport/TEST-junit.xml
```

#### GitHub Actions

```yaml
- name: Run Tests
  run: mvn test

- name: Publish Test Results
  uses: EnricoMi/publish-unit-test-result-action@v2
  if: always()
  with:
    files: target/pulsereport/TEST-junit.xml
```

#### Azure Pipelines

```yaml
- task: Maven@3
  inputs:
    goals: 'test'

- task: PublishTestResults@2
  inputs:
    testResultsFormat: 'JUnit'
    testResultsFiles: '**/TEST-*.xml'
```

### Configuration

```properties
# Include system output
reporter.junit.includeSystemOut=true
reporter.junit.includeSystemErr=true

# Include properties
reporter.junit.includeProperties=true

# Flatten suite hierarchy
reporter.junit.flattenSuites=false
```

---

## Custom Output Formats

### Creating a Custom Output Plugin

Implement the `OutputGenerator` interface:

```java
package com.example;

import io.github.pulsereport.outputs.OutputGenerator;
import io.github.pulsereport.core.model.TestRun;

public class CustomOutputGenerator implements OutputGenerator {
    
    @Override
    public String getName() {
        return "custom";
    }
    
    @Override
    public void generate(TestRun testRun, File outputDirectory) {
        // Custom report generation logic
        File reportFile = new File(outputDirectory, "custom-report.txt");
        
        try (PrintWriter writer = new PrintWriter(reportFile)) {
            writer.println("Custom Report");
            writer.println("=============");
            writer.println("Total Tests: " + testRun.getSummary().getTotalTests());
            // ... more custom formatting
        }
    }
}
```

Register the generator in the PulseReport configuration file (`reporter.properties`):

```properties
reporter.output.formats=html,json,custom
# Note: custom output generator registration is reserved for future use.
```

---

## Comparison Table

| Feature | HTML | JSON | JUnit XML |
| --------- | ------ | ------ | ----------- |
| Human Readable | ✅ Excellent | ❌ No | ⚠️ Limited |
| Machine Readable | ❌ No | ✅ Excellent | ✅ Good |
| Screenshots | ✅ Embedded | ✅ Base64 | ❌ No |
| Charts | ✅ Yes | ❌ No | ❌ No |
| CI/CD Integration | ⚠️ Manual | ✅ API | ✅ Native |
| File Size | 🔴 Large | 🟡 Medium | 🟢 Small |
| Search/Filter | ✅ Yes | 🔧 JQ/Script | ❌ No |

---

## Best Practices

### 1. Use Multiple Formats

Generate both HTML (for humans) and JUnit XML (for CI/CD):

```properties
reporter.output.formats=html,junit
```

### 2. Optimize Artifact Size

Large screenshots increase report size:

```properties
# Compress screenshots
reporter.html.screenshots.compress=true
reporter.html.screenshots.maxWidth=1920
reporter.html.screenshots.maxHeight=1080
```

### 3. Archive Reports

Keep historical reports for trend analysis:

```bash
# Archive with timestamp
mv target/pulsereport target/pulsereport-$(date +%Y%m%d-%H%M%S)
```

### 4. Secure Sensitive Data

Mask sensitive information in reports:

```properties
# Mask credentials in REST API tests
reporter.restassured.mask.credentials=true

# Exclude environment variables
reporter.json.excludeEnvVars=PASSWORD,API_KEY,SECRET
```

---

## Troubleshooting

### Large HTML Files

**Problem**: HTML report is too large to open in browser.

**Solution**:

- Compress screenshots
- Limit artifact content
- Use pagination

```properties
reporter.html.pagination.enabled=true
reporter.html.pagination.pageSize=50
```

### Missing Screenshots in HTML

**Problem**: Screenshots not appearing in HTML report.

**Solution**: Ensure screenshots are embedded:

```properties
reporter.html.screenshots.embed=true
# Options: embed, link, none
```

### Invalid JUnit XML

**Problem**: CI/CD system rejects JUnit XML.

**Solution**: Validate against schema:

```bash
xmllint --schema junit-schema.xsd target/pulsereport/TEST-junit.xml
```

---

## Next Steps

- [Configuration Reference](configuration.md) - Detailed configuration options
- [Integrations Guide](integrations.md) - Publish reports to external systems
- [CLI Reference](cli-reference.md) - Generate reports from command line
