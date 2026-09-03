# PulseReport

[![CI](https://github.com/pulsereport/pulsereport/actions/workflows/ci.yml/badge.svg)](https://github.com/pulsereport/pulsereport/actions/workflows/ci.yml)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/technologies/downloads/#java17)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A unified test reporting library for Java. PulseReport captures results from TestNG, Selenium, Appium, and REST-assured tests and generates HTML, JSON, and JUnit XML reports — with zero configuration required.

## Installation

```xml
<dependency>
    <groupId>io.github.pulsereport</groupId>
    <artifactId>pulsereport</artifactId>
    <version>1.1.1</version>
</dependency>
```

Add the adapter you need (TestNG listener example):

```xml
<suite name="My Suite">
    <listeners>
        <listener class-name="io.github.pulsereport.adapters.testng.TestNGAdapter"/>
    </listeners>
</suite>
```

Run your tests and open the report:

```bash
mvn test
open target/pulsereport/test-report.html
```

## Adapters

| Adapter | Use Case | What it captures | Status |
| --------- | ---------- | ----------------- | ------ |
| `CucumberAdapter` | BDD tests | Feature/scenario/step hierarchy, data tables | Stable |
| `RestAssuredAdapter` | API tests | Request/response bodies, headers (masked by default), status codes | Stable |
| `TestNGAdapter` | Core test execution | Pass/fail/skip, duration, parameters, exceptions | In development |
| `SeleniumAdapter` | Web UI tests | Screenshots on pass/failure, page load metrics | In development |
| `AppiumAdapter` | Mobile tests | Screenshots, device info, session details | In development |

> **Note:** Adapters marked *In development* may not behave as expected. Cucumber and REST-assured integrations are fully tested and production-ready.

Adapters are optional dependencies — add only the framework JARs you actually use (Selenium, Appium, REST-assured, or Cucumber). The REST-assured adapter is standalone and works with any test runner (TestNG, JUnit, Cucumber).

## Output Formats

- **HTML** — Interactive report with charts, screenshots, and filtering
- **JSON** — Machine-readable for custom tooling and dashboards
- **JUnit XML** — Compatible with Jenkins, GitLab CI, GitHub Actions, and TeamCity

Reports are written to `target/pulsereport/` by default.

## Configuration

Create `reporter.properties` in your classpath or configure via environment variables:

```properties
reporter.output.formats=html,json,junit
reporter.output.directory=target/pulsereport
```

See [Configuration Reference](docs/configuration.md) for all options.

## Requirements

- Java 17+
- Maven 3.6+

## Documentation

- [Getting Started](docs/getting-started.md)
- [Adapters Guide](docs/adapters-guide.md)
- [Output Formats](docs/output-formats.md)
- [Configuration](docs/configuration.md)
- [Integrations](docs/integrations.md)
- [CLI Reference](docs/cli-reference.md)
- [Troubleshooting](docs/troubleshooting.md)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for your changes
4. Ensure `mvn test` passes
5. Open a Pull Request

See the [development docs](docs/getting-started.md) for build instructions.

## License

Licensed under the [Apache License 2.0](LICENSE).
