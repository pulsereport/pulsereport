# CLI Reference

PulseReport's command-line surface is implemented by `io.github.pulsereport.cli.ReporterCLI`. The current CLI exposes three subcommands:

- `generate`
- `publish`
- `validate`

Examples below use the packaged project JAR name from the current Maven build.

## Build

```bash
mvn clean package
```

The build produces `target/pulsereport-1.0.0.jar`.

## Root Command

```bash
java -jar target/pulsereport-1.0.0.jar [COMMAND] [OPTIONS]
```

### Standard Options

The root command uses picocli standard help and version options:

| Option | Meaning |
| ------ | ------- |
| `-h`, `--help` | Show usage information |
| `-V`, `--version` | Print the resolved PulseReport version |

### Version Output

`--version` prints a single line:

```text
PulseReport 1.0.0
```

If build metadata is unavailable, the fallback value is `PulseReport unknown`.

### Important Limits

- There is no documented `help` subcommand in `ReporterCLI`; use `--help` on the root command or on a subcommand.
- There are no global `--debug` or `--quiet` flags.
- When `--config` is omitted, the CLI auto-detects `reporter.properties` from the project root, `src/main/resources/`, or the classpath. Pass `--config` explicitly to override.

## `generate`

Generate reports from an input results file.

### Generate Usage

```bash
java -jar target/pulsereport-1.0.0.jar generate [OPTIONS]
```

### Generate Options

| Option | Short | Required | Description |
| ------ | ----- | -------- | ----------- |
| `--input` | `-i` | Yes | Input test results file |
| `--output` | `-o` | No | Output directory for generated reports |
| `--format` | `-f` | No | Output formats, comma-separated: `html,json,junit` |
| `--config` | `-c` | No | Explicit configuration file to load |
| `--verbose` | `-v` | No | Print extra progress details |
| `--dry-run` | - | No | Show what would happen without generating reports |

### Generate Behavior Notes

- `--input` must point to an existing file or the command exits with code `2`.
- If `--config` is omitted, the CLI auto-detects `reporter.properties` from the project root, `src/main/resources/`, or the classpath.
- If no config is found and `--config` is omitted, the CLI falls back to a config built from command-line options, with `--output` defaulting to `target/pulsereport`.
- `ReporterConfig.validate()` still requires at least one output format, so in practice you must either:
  - pass `--format`, or
  - provide a config (auto-detected or explicit) with `reporter.output.formats` defined.

### Generate Examples

Generate a single format from CLI flags:

```bash
java -jar target/pulsereport-1.0.0.jar generate \
  --input target/test-results.json \
  --format html \
  --output target/pulsereport
```

Generate using an explicit configuration file:

```bash
java -jar target/pulsereport-1.0.0.jar generate \
  --input target/test-results.json \
  --config config/reporter.properties
```

Dry run with explicit formats:

```bash
java -jar target/pulsereport-1.0.0.jar generate \
  --input target/test-results.json \
  --format html,json \
  --dry-run
```

Expected dry-run output:

```text
DRY RUN - Would generate reports:
  Input: target/test-results.json
  Output: target/pulsereport
  Formats: [html, json]
```

Verbose generation with a config file:

```bash
java -jar target/pulsereport-1.0.0.jar generate \
  --input target/test-results.json \
  --config config/reporter.properties \
  --verbose
```

Representative output:

```text
Loading configuration from: config/reporter.properties
Generating reports...
  Input: target/test-results.json
  Output: target/pulsereport
  Formats: [html, json]
Reports generated successfully to: target/pulsereport
```

## `publish`

Validate an input report and a configuration file, then run the current publish command flow.

### Publish Usage

```bash
java -jar target/pulsereport-1.0.0.jar publish [OPTIONS]
```

### Publish Options

| Option | Short | Required | Description |
| ------ | ----- | -------- | ----------- |
| `--input` | `-i` | Yes | Input report file |
| `--target` | `-t` | Yes | Publishing targets, comma-separated |
| `--config` | `-c` | No | Configuration file (auto-detected if omitted) |
| `--verbose` | `-v` | No | Print extra progress details |
| `--dry-run` | - | No | Show what would happen without publishing |

### Publish Behavior Notes

- `--input` must point to an existing file or the command exits with code `2`.
- If `--config` is omitted, the CLI auto-detects `reporter.properties` from the project root, `src/main/resources/`, or the classpath. If no config is found, the command exits with code `2`.

### Publish Examples

```bash
java -jar target/pulsereport-1.0.0.jar publish \
  --input target/pulsereport/test-report.html \
  --target s3 \
  --config config/reporter.properties
```

Multiple targets:

```bash
java -jar target/pulsereport-1.0.0.jar publish \
  --input target/pulsereport/test-report.html \
  --target s3,http,slack \
  --config config/reporter.properties
```

Dry run:

```bash
java -jar target/pulsereport-1.0.0.jar publish \
  --input target/pulsereport/test-report.html \
  --target s3,http \
  --config config/reporter.properties \
  --dry-run
```

Expected dry-run output:

```text
DRY RUN - Would publish report:
  Input: target/pulsereport/test-report.html
  Targets: [s3, http]
```

Verbose mode:

```bash
java -jar target/pulsereport-1.0.0.jar publish \
  --input target/pulsereport/test-report.html \
  --target s3,slack \
  --config config/reporter.properties \
  --verbose
```

Representative output:

```text
Loading configuration from: config/reporter.properties
Publishing report...
  Input: target/pulsereport/test-report.html
  Targets: [s3, slack]
Report published successfully to: [s3, slack]
```

## `validate`

Validate a configuration file.

### Validate Usage

```bash
java -jar target/pulsereport-1.0.0.jar validate [OPTIONS]
```

### Validate Options

| Option | Short | Required | Description |
| ------ | ----- | -------- | ----------- |
| `--config` | `-c` | Yes | Configuration file to validate |
| `--verbose` | `-v` | No | Print the loaded configuration before validation |

### Validate Behavior Notes

- There is no `validate --input` mode in the current CLI.
- `validate` loads the file with `ReporterConfig.loadFromFile(...)` and then calls `config.validate()`.

### Validate Examples

```bash
java -jar target/pulsereport-1.0.0.jar validate \
  --config config/reporter.properties
```

Success output:

```text
✓ Configuration is valid
```

Verbose validation:

```bash
java -jar target/pulsereport-1.0.0.jar validate \
  --config config/reporter.properties \
  --verbose
```

Representative verbose output:

```text
Validating configuration: config/reporter.properties
Loaded configuration:
ReporterConfig{outputFormats=[html, json], outputDirectory=target/pulsereport, ...}
✓ Configuration is valid
```

Invalid configuration output:

```text
✗ Configuration is invalid:
  output formats must be specified
```

## Exit Codes

| Code | Meaning |
| ---- | ------- |
| `0` | Success |
| `1` | General error while reading files or executing the command |
| `2` | Input or configuration validation error |

## Practical Notes

- The CLI auto-detects `reporter.properties` when `--config` is omitted. Use `--config` to point to a specific file.
- Use the Java integration APIs for concrete S3 and HTTP publishing behavior.
- Keep examples consistent with the current supported output formats: `html`, `json`, and `junit`.
