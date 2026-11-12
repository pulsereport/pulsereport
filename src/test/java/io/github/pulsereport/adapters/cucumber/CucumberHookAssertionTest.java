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

/** Assertions made in Cucumber hooks land on the hook's step, or on the Gherkin step it surrounds. */
class CucumberHookAssertionTest {

    @TempDir
    Path outputDir;

    @Test
    void hookAssertionsAppearWithTheirSteps() throws Exception {
        String previousOutputDir = System.getProperty("pulsereport.output.directory");
        System.setProperty("pulsereport.output.directory", outputDir.toString());
        System.setProperty("cucumber.publish.quiet", "true");
        byte exitStatus;
        try {
            exitStatus = Main.run(new String[] {
                "--plugin", CucumberAdapter.class.getName(),
                "--glue", "io.github.pulsereport.adapters.cucumber.assertglue",
                "--monochrome",
                "classpath:assertion-hooks"
            }, Thread.currentThread().getContextClassLoader());
        } finally {
            if (previousOutputDir == null) {
                System.clearProperty("pulsereport.output.directory");
            } else {
                System.setProperty("pulsereport.output.directory", previousOutputDir);
            }
        }
        assertEquals(0, exitStatus);

        JsonNode report = new ObjectMapper().readTree(Files.readString(outputDir.resolve("test-report.json")));
        JsonNode steps = report.path("suites").get(0).path("testCases").get(0).path("steps");
        assertEquals(3, steps.size(), steps.toString());

        assertEquals("@Before", steps.get(0).path("keyword").asText());
        assertEquals(List.of("data seeded"), messages(steps.get(0)));
        assertEquals(List.of("before step check", "total", "after step check"), messages(steps.get(1)),
                "before-step and after-step hook checks join the step they surround");
        assertEquals("@After", steps.get(2).path("keyword").asText());
        assertEquals(List.of("data removed"), messages(steps.get(2)));
    }

    private static List<String> messages(JsonNode step) {
        List<String> messages = new ArrayList<>();
        step.path("assertions").forEach(a -> messages.add(a.path("message").asText()));
        return messages;
    }
}
