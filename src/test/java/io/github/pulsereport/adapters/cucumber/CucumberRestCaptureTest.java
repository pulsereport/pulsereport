package io.github.pulsereport.adapters.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.core.cli.Main;

/** REST Assured calls and recordings captured in Cucumber steps and hooks. */
class CucumberRestCaptureTest {

    @TempDir
    Path outputDir;

    @Test
    void apiCallsInHooksAndStepsAndHookRecordingsAreKept() throws Exception {
        String previousOutputDir = System.getProperty("reporter.output.directory");
        System.setProperty("reporter.output.directory", outputDir.toString());
        System.setProperty("cucumber.publish.quiet", "true");
        byte exitStatus;
        try {
            exitStatus = Main.run(new String[] {
                "--plugin", CucumberAdapter.class.getName(),
                "--glue", "io.github.pulsereport.adapters.cucumber.restglue",
                "--monochrome",
                "classpath:rest-capture"
            }, Thread.currentThread().getContextClassLoader());
        } finally {
            if (previousOutputDir == null) {
                System.clearProperty("reporter.output.directory");
            } else {
                System.setProperty("reporter.output.directory", previousOutputDir);
            }
        }
        assertEquals(0, exitStatus);

        JsonNode report = new ObjectMapper().readTree(Files.readString(outputDir.resolve("test-report.json")));
        JsonNode scenario = report.path("suites").get(0).path("testCases").get(0);
        JsonNode steps = scenario.path("steps");
        assertEquals(2, steps.size(), "Setup + the Gherkin step; the After hook only attached a recording");

        JsonNode setup = steps.get(0);
        assertEquals("Setup", setup.path("keyword").asText());
        assertEquals("createUser", setup.path("name").asText());
        assertEquals(List.of("http-request", "http-response"), types(setup.path("artifacts")));
        assertEquals(List.of("http-request", "http-response"), types(steps.get(1).path("artifacts")));

        assertEquals("api.response.time", scenario.path("metrics").get(0).path("name").asText());
        assertEquals(List.of("video"), types(scenario.path("artifacts")), "the recording is on the scenario");
        assertEquals("https://minio.example.test/videos/run-1/user.mp4",
                scenario.path("artifacts").get(0).path("path").asText());
    }

    private static List<String> types(JsonNode artifacts) {
        List<String> types = new ArrayList<>();
        artifacts.forEach(a -> types.add(a.path("type").asText()));
        return types;
    }
}
