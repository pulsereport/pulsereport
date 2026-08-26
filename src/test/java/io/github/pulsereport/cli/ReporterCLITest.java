package io.github.pulsereport.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test suite for ReporterCLI. Tests CLI argument parsing and command execution.
 */
class ReporterCLITest {

    @TempDir
    Path tempDir;

    private File configFile;
    private File inputFile;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @BeforeEach
    void setUp() throws IOException {
        configFile = tempDir.resolve("reporter.properties").toFile();
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html,json");
        props.setProperty("reporter.output.directory", tempDir.toString());
        props.setProperty("reporter.s3.enabled", "false");
        props.setProperty("reporter.http.enabled", "false");
        props.setProperty("reporter.slack.enabled", "false");

        try (FileWriter writer = new FileWriter(configFile)) {
            props.store(writer, "Test config");
        }

        inputFile = tempDir.resolve("results.json").toFile();
        String testRunJson = "{\"id\":\"test-1\",\"name\":\"Test Run\",\"startTime\":\"2024-01-01T00:00:00Z\","
                + "\"endTime\":\"2024-01-01T00:01:00Z\",\"duration\":60000,\"status\":\"PASSED\","
                + "\"suites\":[],\"totalTests\":1,\"passedTests\":1,\"failedTests\":0,\"skippedTests\":0}";
        try (FileWriter fw = new FileWriter(inputFile)) {
            fw.write(testRunJson);
        }

        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @Test
    void mainMethod() {
        assertDoesNotThrow(() -> {
            Class<?> cliClass = Class.forName("io.github.pulsereport.cli.ReporterCLI");
            assertNotNull(cliClass.getMethod("main", String[].class));
        });
    }

    @Test
    void helpCommand() {
        String[] args = {"--help"};

        int exitCode = ReporterCLI.execute(args);

        assertEquals(0, exitCode);
        String output = outputStream.toString();
        assertTrue(output.contains("Usage:") || output.contains("help"));
        assertTrue(output.contains("pulsereport"));
        assertTrue(output.contains("PulseReport"));
    }

    @Test
    void versionCommand() {
        String[] args = {"--version"};

        int exitCode = ReporterCLI.execute(args);

        assertEquals(0, exitCode);
        String output = outputStream.toString();
        assertTrue(output.contains("PulseReport " + readProjectVersion()));
        assertTrue(output.contains("PulseReport"));
    }

    @Test
    void generateCommand_withValidArgs() {
        String[] args = {
            "generate",
            "--input", inputFile.getAbsolutePath(),
            "--output", tempDir.toString(),
            "--format", "html,json"
        };

        int exitCode = ReporterCLI.execute(args);

        assertEquals(0, exitCode);
    }

    @Test
    void generateCommand_missingInputFile() {
        String[] args = {
            "generate",
            "--output", tempDir.toString(),
            "--format", "html"
        };

        int exitCode = ReporterCLI.execute(args);

        assertEquals(2, exitCode); // Validation error
    }

    @Test
    void generateCommand_invalidFormat() {
        String[] args = {
            "generate",
            "--input", inputFile.getAbsolutePath(),
            "--output", tempDir.toString(),
            "--format", "invalid-format"
        };

        int exitCode = ReporterCLI.execute(args);

        assertEquals(2, exitCode); // Validation error
        String output = outputStream.toString() + errorStream.toString();
        assertTrue(output.contains("Invalid") || output.contains("format"));
    }

    @Test
    void publishCommand_withValidArgs() {
        File reportFile = tempDir.resolve("report.html").toFile();
        try {
            reportFile.createNewFile();
        } catch (IOException e) {
            fail("Failed to create test file");
        }

        String[] args = {
            "publish",
            "--input", reportFile.getAbsolutePath(),
            "--target", "s3",
            "--config", configFile.getAbsolutePath()
        };

        // Act - might fail due to missing AWS credentials, but should parse correctly
        int exitCode = ReporterCLI.execute(args);

        // Assert - 0 for success or 1 for runtime error (not parsing error)
        assertTrue(exitCode == 0 || exitCode == 1);
    }

    @Test
    void publishCommand_missingConfig() {
        String[] args = {
            "publish",
            "--input", inputFile.getAbsolutePath(),
            "--target", "s3"
        };

        int exitCode = ReporterCLI.execute(args);

        // Auto-detects reporter.properties from classpath; S3 is disabled so it skips with a warning
        assertTrue(exitCode == 0 || exitCode == 1);
    }

    @Test
    void validateCommand_withValidConfig() {
        String[] args = {
            "validate",
            "--config", configFile.getAbsolutePath()
        };

        int exitCode = ReporterCLI.execute(args);

        assertEquals(0, exitCode);
        String output = outputStream.toString();
        assertTrue(output.contains("valid") || output.contains("success"));
    }

    @Test
    void validateCommand_withInvalidConfig() throws IOException {
        File invalidConfigFile = tempDir.resolve("invalid.properties").toFile();
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", ""); // Invalid: empty formats
        props.setProperty("reporter.s3.enabled", "true");
        props.setProperty("reporter.s3.bucket", ""); // Invalid: empty bucket

        try (FileWriter writer = new FileWriter(invalidConfigFile)) {
            props.store(writer, "Invalid config");
        }

        String[] args = {
            "validate",
            "--config", invalidConfigFile.getAbsolutePath()
        };

        int exitCode = ReporterCLI.execute(args);

        assertEquals(2, exitCode); // Validation error
        String output = outputStream.toString() + errorStream.toString();
        assertTrue(output.contains("invalid") || output.contains("error"));
    }

    @Test
    void verboseMode() {
        String[] args = {
            "validate",
            "--config", configFile.getAbsolutePath(),
            "--verbose"
        };

        int exitCode = ReporterCLI.execute(args);

        assertEquals(0, exitCode);
        String output = outputStream.toString();
        assertFalse(output.isEmpty());
    }

    @Test
    void dryRunMode() {
        String[] args = {
            "generate",
            "--input", inputFile.getAbsolutePath(),
            "--output", tempDir.toString(),
            "--format", "html",
            "--dry-run"
        };

        int exitCode = ReporterCLI.execute(args);

        assertEquals(0, exitCode);
        String output = outputStream.toString() + errorStream.toString();
        assertTrue(output.contains("dry") || output.contains("would") || output.contains("DRY"));
    }

    @Test
    void invalidCommand() {
        String[] args = {"invalid-command"};

        int exitCode = ReporterCLI.execute(args);

        assertEquals(2, exitCode); // Validation/parsing error
    }

    @Test
    void noArguments() {
        String[] args = {};

        int exitCode = ReporterCLI.execute(args);

        assertEquals(0, exitCode); // Should show help
        String output = outputStream.toString();
        assertTrue(output.contains("Usage:") || output.contains("help"));
    }

    @Test
    void generateWithConfigFile() {
        String[] args = {
            "generate",
            "--input", inputFile.getAbsolutePath(),
            "--config", configFile.getAbsolutePath()
        };

        int exitCode = ReporterCLI.execute(args);

        assertEquals(0, exitCode);
    }

    @Test
    void multipleFormats() {
        String[] args = {
            "generate",
            "--input", inputFile.getAbsolutePath(),
            "--output", tempDir.toString(),
            "--format", "html,json,junit"
        };

        int exitCode = ReporterCLI.execute(args);

        assertEquals(0, exitCode);
    }

    @Test
    void generateCommand_malformedJsonInput() throws IOException {
        File badJsonFile = tempDir.resolve("bad-input.json").toFile();
        try (FileWriter fw = new FileWriter(badJsonFile)) {
            fw.write("{ this is not valid json at all }}}");
        }

        String[] args = {
            "generate",
            "--input", badJsonFile.getAbsolutePath(),
            "--output", tempDir.toString(),
            "--format", "json"
        };

        int exitCode = ReporterCLI.execute(args);
        assertEquals(1, exitCode, "Malformed JSON should return error exit code");
    }

    @Test
    void generateCommand_emptyJsonInput() throws IOException {
        File emptyFile = tempDir.resolve("empty.json").toFile();
        try (FileWriter fw = new FileWriter(emptyFile)) {
            fw.write("");
        }

        String[] args = {
            "generate",
            "--input", emptyFile.getAbsolutePath(),
            "--output", tempDir.toString(),
            "--format", "html"
        };

        int exitCode = ReporterCLI.execute(args);
        assertEquals(1, exitCode, "Empty JSON input should return error exit code");
    }

    @Test
    void generateCommand_incompleteJsonInput() throws IOException {
        File incompleteFile = tempDir.resolve("incomplete.json").toFile();
        try (FileWriter fw = new FileWriter(incompleteFile)) {
            fw.write("{\"id\":\"test\",\"name\":\"Incomplete\"");
        }

        String[] args = {
            "generate",
            "--input", incompleteFile.getAbsolutePath(),
            "--output", tempDir.toString(),
            "--format", "json"
        };

        int exitCode = ReporterCLI.execute(args);
        assertEquals(1, exitCode, "Incomplete JSON input should return error exit code");
    }

    private String readProjectVersion() {
        try {
            String pomContents = Files.readString(Path.of("pom.xml"));
            Matcher matcher = Pattern.compile("<version>([^<]+)</version>").matcher(pomContents);
            if (matcher.find()) {
                return matcher.group(1);
            }
            fail("Could not locate project version in pom.xml");
        } catch (IOException e) {
            fail("Could not read pom.xml: " + e.getMessage());
        }
        return "";
    }
}
