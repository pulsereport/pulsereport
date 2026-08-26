package io.github.pulsereport.adapters.cucumber;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.EventPublisher;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestSuite;

class CucumberAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    void buildTestRunUsesReadableFeaturePathAsSuiteSecondaryText() throws Exception {
        CucumberAdapter adapter = new CucumberAdapter();
        URI featureUri = URI.create(
                "file:///Users/test/project/src/test/resources/features/error_display.feature");

        TestCase testCase = TestCase.builder()
                .id("case-1")
                .name("Scenario: error display")
                .status(TestStatus.PASSED)
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .steps(List.of())
                .backgroundSteps(List.of())
                .bddType("scenario")
                .featureName("Error Display")
                .featureDescription("")
                .build();

        setField(adapter, "testCasesByFeature", new LinkedHashMap<>(
                Map.of(featureUri, new ArrayList<>(List.of(testCase)))));
        setField(adapter, "featureNameByUri", new ConcurrentHashMap<>(
                Map.of(featureUri, "Error Display")));

        TestRun testRun = adapter.buildTestRun(
                Instant.parse("2026-03-10T10:00:00Z"),
                Instant.parse("2026-03-10T10:00:01Z"));

        assertEquals("PulseReport BDD Run", testRun.getName());
        TestSuite suite = testRun.getSuites().get(0);
        assertEquals("Error Display", suite.getName());
        assertEquals("features/error_display.feature", suite.getSecondaryText());
    }

    @Test
    void buildTestRunDoesNotTreatNotFeaturesSegmentAsFeaturesDirectory() throws Exception {
        TestSuite suite = buildSuiteForUri(
                "file:///Users/test/project/src/test/resources/notfeatures/error_display.feature",
                "Error Display");

        assertEquals("error_display.feature", suite.getSecondaryText());
    }

    @Test
    void buildTestRunDoesNotTreatMyFeaturesSegmentAsFeaturesDirectory() throws Exception {
        TestSuite suite = buildSuiteForUri(
                "file:///Users/test/project/src/test/resources/myfeatures/nested/error_display.feature",
                "Error Display");

        assertEquals("error_display.feature", suite.getSecondaryText());
    }

    @Test
    void buildTestRunUsesReadableFeaturePathForClasspathFeatureUri() throws Exception {
        TestSuite suite = buildSuiteForUri(
                "classpath:features/api/error_display.feature",
                "Error Display");

        assertEquals("features/api/error_display.feature", suite.getSecondaryText());
    }

    @Test
    void buildTestRunLeavesSecondaryTextNullWhenNoFeaturePathCanBeDerived() throws Exception {
        TestSuite suite = buildSuiteForUri(
                "classpath:notfeatures/api/readme.txt",
                "Readme");

        assertNull(suite.getSecondaryText());
    }

    @Test
    void generateReportsUsesPulseReportDefaultOutputDirectory() throws Exception {
        CucumberAdapter adapter = new CucumberAdapter();
        TestRun testRun = TestRun.builder()
                .id("run-1")
                .name("PulseReport BDD Run")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .suites(List.of())
                .build();

        Method generateReports = CucumberAdapter.class.getDeclaredMethod("generateReports", TestRun.class);
        generateReports.setAccessible(true);

        String previousUserDir = System.getProperty("user.dir");
        String previousOutputDir = System.getProperty("reporter.output.directory");

        try {
            System.clearProperty("reporter.output.directory");
            System.setProperty("user.dir", tempDir.toString());

            generateReports.invoke(adapter, testRun);

            Path reportDir = tempDir.resolve("target/pulsereport");
            assertTrue(Files.exists(reportDir.resolve("test-report.html")),
                    "Cucumber adapter should generate the HTML report in the PulseReport default directory");
            assertTrue(Files.exists(reportDir.resolve("test-report.json")),
                    "Cucumber adapter should generate the JSON report in the PulseReport default directory");
        } finally {
            if (previousOutputDir == null) {
                System.clearProperty("reporter.output.directory");
            } else {
                System.setProperty("reporter.output.directory", previousOutputDir);
            }

            if (previousUserDir == null) {
                System.clearProperty("user.dir");
            } else {
                System.setProperty("user.dir", previousUserDir);
            }
        }
    }

    @Test
    void adapterRegistersAllEventHandlers() {
        CucumberAdapter adapter = new CucumberAdapter();
        AtomicInteger handlerCount = new AtomicInteger(0);

        EventPublisher mockPublisher = new EventPublisher() {
            @Override
            public <T> void registerHandlerFor(Class<T> eventType, EventHandler<T> handler) {
                handlerCount.incrementAndGet();
            }

            @Override
            public <T> void removeHandlerFor(Class<T> eventType, EventHandler<T> handler) {
            }
        };

        adapter.setEventPublisher(mockPublisher);

        assertTrue(handlerCount.get() >= 6,
                "Expected at least 6 event handlers registered, got " + handlerCount.get());
    }

    private TestSuite buildSuiteForUri(String featureUriValue, String featureName) throws Exception {
        CucumberAdapter adapter = new CucumberAdapter();
        URI featureUri = URI.create(featureUriValue);

        TestCase testCase = TestCase.builder()
                .id("case-1")
                .name("Scenario: error display")
                .status(TestStatus.PASSED)
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .steps(List.of())
                .backgroundSteps(List.of())
                .bddType("scenario")
                .featureName(featureName)
                .featureDescription("")
                .build();

        setField(adapter, "testCasesByFeature", new LinkedHashMap<>(
                Map.of(featureUri, new ArrayList<>(List.of(testCase)))));
        setField(adapter, "featureNameByUri", new ConcurrentHashMap<>(
                Map.of(featureUri, featureName)));

        TestRun testRun = adapter.buildTestRun(
                Instant.parse("2026-03-10T10:00:00Z"),
                Instant.parse("2026-03-10T10:00:01Z"));

        return testRun.getSuites().get(0);
    }

    private void setField(CucumberAdapter adapter, String fieldName, Object value) throws Exception {
        Field field = CucumberAdapter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(adapter, value);
    }
}
