package io.github.pulsereport.adapters.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.core.cli.Main;

/**
 * Runs a real Cucumber suite with {@link CucumberAdapter} as a plugin and checks
 * that unwrapped assertions in step definitions land on their steps in the report.
 */
class CucumberAssertionCaptureTest {

    @TempDir
    Path outputDir;

    @Test
    void assertionsInStepDefinitionsAppearUnderTheirSteps() throws Exception {
        String previousOutputDir = System.getProperty("reporter.output.directory");
        System.setProperty("reporter.output.directory", outputDir.toString());
        System.setProperty("cucumber.publish.quiet", "true");
        byte exitStatus;
        try {
            exitStatus = Main.run(new String[] {
                "--plugin", CucumberAdapter.class.getName(),
                "--glue", "io.github.pulsereport.adapters.cucumber.glue",
                "--monochrome",
                "classpath:assertion-capture"
            }, Thread.currentThread().getContextClassLoader());
        } finally {
            if (previousOutputDir == null) {
                System.clearProperty("reporter.output.directory");
            } else {
                System.setProperty("reporter.output.directory", previousOutputDir);
            }
        }
        assertEquals(1, exitStatus, "the second scenario fails on purpose");

        JsonNode report = new ObjectMapper().readTree(Files.readString(outputDir.resolve("test-report.json")));
        List<JsonNode> cases = new ArrayList<>();
        report.path("suites").get(0).path("testCases").forEach(cases::add);
        JsonNode passing = cases.stream()
                .filter(c -> c.path("name").asText().contains("Passing")).findFirst().orElseThrow();
        JsonNode failing = cases.stream()
                .filter(c -> c.path("name").asText().contains("failing")).findFirst().orElseThrow();

        JsonNode passingSteps = passing.path("steps");
        assertTrue(passingSteps.get(0).path("assertions").isMissingNode(),
                "the Given step asserts nothing; hook assertions must not leak into it");

        JsonNode status = passingSteps.get(1).path("assertions");
        assertEquals(1, status.size());
        assertEquals("JUnit 5", status.get(0).path("framework").asText());
        assertEquals("Login API status code", status.get(0).path("message").asText());
        assertEquals("200", status.get(0).path("expected").asText());
        assertEquals("PASSED", status.get(0).path("status").asText());

        JsonNode name = passingSteps.get(2).path("assertions");
        assertEquals(2, name.size());
        assertEquals("TestNG", name.get(0).path("framework").asText());
        assertEquals("\"alice\"", name.get(0).path("actual").asText());
        assertEquals("Hamcrest", name.get(1).path("framework").asText());
        assertEquals("a value greater than <0>", name.get(1).path("expected").asText());

        JsonNode failed = failing.path("steps").get(1).path("assertions");
        assertEquals(1, failed.size());
        assertEquals("FAILED", failed.get(0).path("status").asText());
        assertEquals("200", failed.get(0).path("expected").asText());
        assertEquals("404", failed.get(0).path("actual").asText());
        assertEquals(failing.path("steps").get(1).path("errorMessage").asText(),
                failed.get(0).path("errorMessage").asText());

        String html = Files.readString(outputDir.resolve("test-report.html"));
        assertTrue(html.contains("Login API status code"), "assertion data must be embedded in the HTML report");
    }
}
