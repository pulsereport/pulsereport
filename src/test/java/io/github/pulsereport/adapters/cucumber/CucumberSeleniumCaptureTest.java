package io.github.pulsereport.adapters.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.core.cli.Main;
import io.github.pulsereport.adapters.CaptureBridge;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;

/** Selenium actions tracked in Cucumber step definitions and hooks. */
class CucumberSeleniumCaptureTest {

    @TempDir
    Path outputDir;

    @Test
    void trackedActionsBecomeSubStepsOfTheirGherkinSteps() throws Exception {
        String previousOutputDir = System.getProperty("reporter.output.directory");
        System.setProperty("reporter.output.directory", outputDir.toString());
        System.setProperty("cucumber.publish.quiet", "true");
        byte exitStatus;
        try {
            exitStatus = Main.run(new String[] {
                "--plugin", CucumberAdapter.class.getName(),
                "--glue", "io.github.pulsereport.adapters.cucumber.seleniumglue",
                "--monochrome",
                "classpath:selenium-capture"
            }, Thread.currentThread().getContextClassLoader());
        } finally {
            if (previousOutputDir == null) {
                System.clearProperty("reporter.output.directory");
            } else {
                System.setProperty("reporter.output.directory", previousOutputDir);
            }
        }
        assertEquals(1, exitStatus, "the last step fails on purpose");

        JsonNode report = new ObjectMapper().readTree(Files.readString(outputDir.resolve("test-report.json")));
        JsonNode scenario = report.path("suites").get(0).path("testCases").get(0);
        assertEquals("Cucumber", scenario.path("adapter").asText());
        JsonNode steps = scenario.path("steps");
        assertEquals(4, steps.size(), "Before hook + three Gherkin steps; the After hook captured nothing");

        JsonNode setup = steps.get(0);
        assertEquals("Setup", setup.path("keyword").asText());
        assertEquals("openBrowser", setup.path("name").asText());
        assertEquals("Open \"https://example.test/login\"", setup.path("children").get(0).path("name").asText());

        assertEquals("Open \"https://example.test/about\"", steps.get(1).path("children").get(0).path("name").asText());

        JsonNode logIn = steps.get(2).path("children").get(0);
        assertEquals("Log in", logIn.path("name").asText());
        assertEquals("Open \"https://example.test/home\"", logIn.path("children").get(0).path("name").asText());

        JsonNode failed = steps.get(3);
        assertEquals("FAILED", failed.path("status").asText());
        JsonNode lookup = failed.path("children").get(0);
        assertEquals("Find element By.id: missing", lookup.path("name").asText());
        JsonNode shot = lookup.path("artifacts").get(0);
        assertEquals("screenshot", shot.path("type").asText());
        assertTrue(shot.path("content").asText().length() > 0, "failure screenshot is embedded");
        List<String> types = new ArrayList<>();
        failed.path("artifacts").forEach(a -> types.add(a.path("type").asText()));
        assertTrue(types.contains("page-source"), types.toString());
    }

    @Test
    void bridgeCapturesGoToTheRunningStep() {
        CaptureBridge.register(CucumberStepContext.SINK, CaptureBridge.PRIORITY_STEP);
        CucumberStepContext.currentStepArtifacts.set(new ArrayList<>());
        CucumberStepContext.beginScenario();
        try {
            assertNotSame(CucumberStepContext.SINK, CaptureBridge.current(), "inactive between steps");
            CucumberStepContext.beginStep();
            CaptureBridge.Sink sink = CaptureBridge.current();
            assertSame(CucumberStepContext.SINK, sink);

            TestStep action = TestStep.builder().name("Click").status(TestStatus.PASSED).build();
            Artifact shot = Artifact.builder().name("s.png").type("screenshot").path("s.png")
                    .timestamp(Instant.now()).build();
            sink.addStep(action);
            sink.addArtifact(shot);
            sink.addTag("Chrome 131");

            assertEquals(List.of(action), CucumberStepContext.endStep());
            assertEquals(List.of(shot), CucumberStepContext.currentStepArtifacts.get());
            assertTrue(CucumberStepContext.currentScenarioTags.get().contains("Chrome 131"));
        } finally {
            CucumberStepContext.endScenario();
            CucumberStepContext.currentStepArtifacts.remove();
        }
    }

    @Test
    void beforeStepHookCapturesCarryToTheNextStep() {
        CucumberStepContext.currentStepArtifacts.set(new ArrayList<>());
        CucumberStepContext.beginScenario();
        try {
            TestStep action = TestStep.builder().name("Open").status(TestStatus.PASSED).build();
            CucumberStepContext.carry(List.of(action), List.of());
            CucumberStepContext.beginStep();
            assertEquals(List.of(action), CucumberStepContext.endStep());
        } finally {
            CucumberStepContext.endScenario();
            CucumberStepContext.currentStepArtifacts.remove();
        }
    }
}
