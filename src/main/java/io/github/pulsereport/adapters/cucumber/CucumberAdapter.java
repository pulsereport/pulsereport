package io.github.pulsereport.adapters.cucumber;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Examples;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.TableCell;
import io.cucumber.messages.types.TableRow;

import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.DataTableArgument;
import io.cucumber.plugin.event.DocStringArgument;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Node;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.StepArgument;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import io.cucumber.plugin.event.TestSourceParsed;
import io.cucumber.plugin.event.TestSourceRead;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;
import io.github.pulsereport.core.model.TestSuite;
import io.github.pulsereport.config.ReporterConfig;
import io.github.pulsereport.outputs.html.HtmlReportGenerator;
import io.github.pulsereport.outputs.json.JsonReportGenerator;

/**
 * Cucumber plugin that maps Gherkin BDD events to PulseReport's data model.
 *
 * <h2>Hierarchy mapping</h2>
 * <pre>
 *   Cucumber Feature     → TestSuite  (one per feature file URI)
 *   Cucumber Scenario    → TestCase   (bddType="scenario")
 *   Cucumber Outline row → TestCase   (bddType="scenario_outline", name includes param values)
 *   Background steps     → TestCase.backgroundSteps (inlined per scenario)
 *   Scenario steps       → TestCase.steps
 *   HTTP artifacts       → TestStep.artifacts  (via CucumberAdapter.currentStepArtifacts ThreadLocal)
 * </pre>
 *
 * <h2>Usage</h2>
 * <p>
 * Register as a Cucumber plugin. With JUnit Platform:</p>
 * <pre>{@code
 * @ConfigurationParameter(
 *     key = PLUGIN_PROPERTY_NAME,
 *     value = "io.github.pulsereport.adapters.cucumber.CucumberAdapter"
 * )
 * }</pre>
 * <p>
 * Or with the {@code @CucumberOptions} annotation:</p>
 * <pre>{@code
 * @CucumberOptions(plugin = "io.github.pulsereport.adapters.cucumber.CucumberAdapter")
 * }</pre>
 * <p>
 * Pair with
 * {@link io.github.pulsereport.adapters.restassured.RestAssuredAdapter} as a
 * REST-assured filter; HTTP request/response artifacts are automatically routed
 * to the currently executing step.</p>
 *
 * @author PulseReport Team
 * @since 1.0.0
 */
public class CucumberAdapter implements EventListener {

    private static final Logger logger = LoggerFactory.getLogger(CucumberAdapter.class);

    /**
     * Active scenario context per thread.
     */
    private static final ThreadLocal<CucumberScenarioContext> currentScenario = new ThreadLocal<>();

    /**
     * Completed test cases grouped by feature URI. LinkedHashMap preserves
     * insertion order for deterministic output.
     */
    private final Map<URI, List<TestCase>> testCasesByFeature = new LinkedHashMap<>();

    /**
     * Feature name (display name) per feature URI, populated from
     * {@link TestSourceParsed}.
     */
    private final Map<URI, String> featureNameByUri = new ConcurrentHashMap<>();

    /**
     * Feature-level tags per feature URI, parsed from Gherkin source.
     */
    private final Map<URI, List<String>> featureTagsByUri = new ConcurrentHashMap<>();

    /**
     * Background step line numbers per feature URI. A step whose line number
     * appears in this set belongs to the Background section.
     */
    private final Map<URI, java.util.Set<Integer>> backgroundLinesByUri = new ConcurrentHashMap<>();

    /**
     * Examples body row data per feature URI, keyed by the row's 1-based
     * source line. Populated by parsing the Gherkin AST on
     * {@link TestSourceRead}. A Scenario Outline pickle's
     * {@code TestCase.getLine()} points at its Examples body row, which is
     * used as the join key.
     */
    private final Map<URI, Map<Integer, ExampleRowData>> exampleRowsByUri = new ConcurrentHashMap<>();

    /**
     * When the whole test run started.
     */
    private volatile Instant runStartTime;

    // EventListener
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::onTestRunStarted);
        publisher.registerHandlerFor(TestSourceRead.class, this::onTestSourceRead);
        publisher.registerHandlerFor(TestSourceParsed.class, this::onTestSourceParsed);
        publisher.registerHandlerFor(TestCaseStarted.class, this::onTestCaseStarted);
        publisher.registerHandlerFor(TestStepStarted.class, this::onTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onTestStepFinished);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onTestRunFinished);
    }

    // Event handlers
    private void onTestRunStarted(TestRunStarted event) {
        runStartTime = event.getInstant();
        logger.info("Cucumber test run started");
    }

    /**
     * Parse the raw Gherkin source to identify which line numbers belong to the
     * Background section. This is the only reliable way since Cucumber 7's
     * {@code Node.Feature.elements()} does not expose a Background node.
     */
    private void onTestSourceRead(TestSourceRead event) {
        java.util.Set<Integer> bgLines = new java.util.HashSet<>();
        List<String> featureTags = new ArrayList<>();
        String source = event.getSource();
        if (source != null) {
            String[] lines = source.split("\n", -1);
            boolean inBackground = false;
            boolean featureFound = false;
            for (int i = 0; i < lines.length; i++) {
                String trimmed = lines[i].trim();

                if (!featureFound) {
                    if (trimmed.startsWith("@")) {
                        for (String token : trimmed.split("\\s+")) {
                            if (token.startsWith("@")) {
                                featureTags.add(token);
                            }
                        }
                        continue;
                    } else if (trimmed.matches("Feature[^:]*:.*")) {
                        featureFound = true;
                        continue;
                    }
                }

                if (trimmed.matches("Background[^:]*:.*")) {
                    inBackground = true;
                } else if (inBackground) {
                    if (trimmed.matches("(Scenario|Feature|Rule|Examples)[^:]*:.*")) {
                        inBackground = false;
                    } else if (!trimmed.isEmpty() && !trimmed.startsWith("#")
                            && !trimmed.startsWith("@")) {
                        bgLines.add(i + 1); // lines are 1-indexed
                    }
                }
            }
        }
        if (!featureTags.isEmpty()) {
            featureTagsByUri.put(event.getUri(), featureTags);
            logger.debug("Feature tags for {}: {}", event.getUri(), featureTags);
        }
        if (!bgLines.isEmpty()) {
            backgroundLinesByUri.put(event.getUri(), bgLines);
            logger.debug("Background step lines for {}: {}", event.getUri(), bgLines);
        }
        indexExampleRows(event.getUri(), source);
    }

    /**
     * Parses the Gherkin AST to index Examples body rows so that flattened
     * Scenario Outline pickles can be enriched with the un-substituted
     * outline name and per-row parameter values. Failures are non-fatal:
     * outline cases simply carry no example data.
     */
    private void indexExampleRows(URI uri, String source) {
        if (source == null || source.isBlank()) {
            return;
        }
        try {
            Map<Integer, ExampleRowData> rows = new LinkedHashMap<>();
            GherkinParser.builder().build()
                    .parse(uri.toString(), source.getBytes(StandardCharsets.UTF_8))
                    .forEach(envelope -> envelope.getGherkinDocument()
                            .flatMap(doc -> doc.getFeature())
                            .ifPresent(feature ->
                                    collectExampleRows(feature.getChildren(), rows)));
            if (!rows.isEmpty()) {
                exampleRowsByUri.put(uri, rows);
                logger.debug("Indexed {} example rows for {}", rows.size(), uri);
            }
        } catch (RuntimeException e) {
            logger.debug("Could not parse Gherkin AST for {}: {}", uri, e.getMessage());
        }
    }

    private void collectExampleRows(List<FeatureChild> children, Map<Integer, ExampleRowData> rows) {
        for (FeatureChild child : children) {
            child.getScenario().ifPresent(scenario -> collectScenarioExamples(scenario, rows));
            child.getRule().ifPresent(rule -> rule.getChildren().forEach(ruleChild ->
                    ruleChild.getScenario().ifPresent(scenario ->
                            collectScenarioExamples(scenario, rows))));
        }
    }

    private void collectScenarioExamples(Scenario scenario, Map<Integer, ExampleRowData> rows) {
        for (Examples examples : scenario.getExamples()) {
            List<String> headers = examples.getTableHeader()
                    .map(header -> header.getCells().stream()
                            .map(TableCell::getValue)
                            .toList())
                    .orElse(List.of());
            for (TableRow row : examples.getTableBody()) {
                Map<String, String> params = new LinkedHashMap<>();
                List<TableCell> cells = row.getCells();
                for (int i = 0; i < headers.size() && i < cells.size(); i++) {
                    params.put(headers.get(i), cells.get(i).getValue());
                }
                rows.put(row.getLocation().getLine().intValue(),
                        new ExampleRowData(scenario.getName(), params));
            }
        }
    }

    /**
     * Example row data extracted from the Gherkin AST: the un-substituted
     * Scenario Outline name plus this row's parameter values in column
     * order.
     */
    private static final class ExampleRowData {
        private final String outlineName;
        private final Map<String, String> params;

        private ExampleRowData(String outlineName, Map<String, String> params) {
            this.outlineName = outlineName;
            this.params = params;
        }
    }

    private void onTestSourceParsed(TestSourceParsed event) {
        Collection<Node> nodes = event.getNodes();
        for (Node node : nodes) {
            if (node instanceof Node.Feature) {
                node.getName().ifPresent(name -> {
                    featureNameByUri.put(event.getUri(), name);
                    logger.debug("Feature registered: {} -> {}", event.getUri(), name);
                });
            }
        }
    }

    private void onTestCaseStarted(TestCaseStarted event) {
        io.cucumber.plugin.event.TestCase tc = event.getTestCase();

        String featureName = featureNameByUri.getOrDefault(
                tc.getUri(), deriveFeatureNameFromUri(tc.getUri().toString()));

        String keyword = tc.getKeyword();
        boolean isOutline = keyword != null
                && (keyword.contains("Outline") || keyword.contains("Template"));
        String bddType = isOutline ? "scenario_outline" : "scenario";

        List<String> tags = tc.getTags();

        CucumberScenarioContext ctx = new CucumberScenarioContext(
                tc.getId().toString(),
                tc.getName(),
                bddType,
                featureName,
                "",
                tags
        );
        ctx.setStartTime(event.getInstant());

        if (isOutline) {
            ExampleRowData rowData = findExampleRowData(tc);
            if (rowData != null) {
                ctx.setScenarioOutlineName(rowData.outlineName);
                ctx.setExampleParams(rowData.params);
            }
        }

        currentScenario.set(ctx);
        CucumberStepContext.currentStepArtifacts.set(new ArrayList<>());
        logger.debug("Scenario started: {}", tc.getName());
    }

    private void onTestStepStarted(TestStepStarted event) {
        if (!(event.getTestStep() instanceof PickleStepTestStep)) {
            return;
        }
        PickleStepTestStep pStep = (PickleStepTestStep) event.getTestStep();
        CucumberScenarioContext ctx = currentScenario.get();
        if (ctx == null) {
            return;
        }

        // A step is a background step if its source line is in the pre-parsed
        // background line set for this feature. This is reliable for both regular
        // Scenarios and Scenario Outlines (whose testCase location points at the
        // Examples table row, making line-comparison-based detection unreliable).
        int stepLine = pStep.getStep().getLine();
        URI featureUri = event.getTestCase().getUri();
        java.util.Set<Integer> bgLines = backgroundLinesByUri.get(featureUri);
        boolean isBackground = bgLines != null && bgLines.contains(stepLine);

        String keyword = pStep.getStep().getKeyword().trim();
        String name = pStep.getStep().getText();
        ctx.startStep(keyword, name, event.getInstant(), isBackground);

        List<Artifact> buf = CucumberStepContext.currentStepArtifacts.get();
        if (buf != null) {
            buf.clear();
        }
        logger.debug("Step started: {} {}", keyword, name);
    }

    private void onTestStepFinished(TestStepFinished event) {
        if (!(event.getTestStep() instanceof PickleStepTestStep)) {
            return;
        }
        CucumberScenarioContext ctx = currentScenario.get();
        if (ctx == null) {
            return;
        }

        PickleStepTestStep pickleStep = (PickleStepTestStep) event.getTestStep();
        Result result = event.getResult();
        Instant endTime = event.getInstant();
        Instant startTime = ctx.getCurrentStepStart() != null ? ctx.getCurrentStepStart() : endTime;
        long duration = Duration.between(startTime, endTime).toMillis();

        TestStatus status = convertStatus(result.getStatus());
        String errorMessage = result.getError() != null ? result.getError().getMessage() : null;
        String stackTrace = result.getError() != null ? stackTraceOf(result.getError()) : null;

        String docString = null;
        List<List<String>> dataTable = null;
        StepArgument stepArg = pickleStep.getStep().getArgument();
        if (stepArg instanceof DocStringArgument) {
            docString = ((DocStringArgument) stepArg).getContent();
        } else if (stepArg instanceof DataTableArgument) {
            dataTable = new ArrayList<>(((DataTableArgument) stepArg).cells());
        }

        List<Artifact> stepArtifacts = new ArrayList<>();
        List<Artifact> buf = CucumberStepContext.currentStepArtifacts.get();
        if (buf != null) {
            stepArtifacts.addAll(buf);
            buf.clear();
        }

        TestStep step = TestStep.builder()
                .name(ctx.getCurrentStepName())
                .keyword(ctx.getCurrentStepKeyword())
                .status(status)
                .startTime(startTime)
                .endTime(endTime)
                .duration(duration)
                .errorMessage(errorMessage)
                .stackTrace(stackTrace)
                .docString(docString)
                .dataTable(dataTable)
                .artifacts(stepArtifacts.isEmpty() ? null : stepArtifacts)
                .build();

        if (ctx.isCurrentStepBackground()) {
            ctx.addBackgroundStep(step);
        } else {
            ctx.addStep(step);
        }
        logger.debug("Step finished: {} {} -> {}", ctx.getCurrentStepKeyword(),
                ctx.getCurrentStepName(), status);
    }

    private void onTestCaseFinished(TestCaseFinished event) {
        CucumberScenarioContext ctx = currentScenario.get();
        if (ctx == null) {
            return;
        }

        io.cucumber.plugin.event.TestCase tc = event.getTestCase();
        Result result = event.getResult();
        Instant endTime = event.getInstant();
        Instant startTime = ctx.getStartTime() != null ? ctx.getStartTime() : endTime;
        long duration = Duration.between(startTime, endTime).toMillis();

        TestStatus status = convertStatus(result.getStatus());
        String errorMessage = result.getError() != null ? result.getError().getMessage() : null;
        String stackTrace = result.getError() != null ? stackTraceOf(result.getError()) : null;

        TestCase testCase = TestCase.builder()
                .id(UUID.randomUUID().toString())
                .name(ctx.getScenarioName())
                .startTime(startTime)
                .endTime(endTime)
                .duration(duration)
                .status(status)
                .errorMessage(errorMessage)
                .stackTrace(stackTrace)
                .steps(ctx.getSteps())
                .backgroundSteps(ctx.getBackgroundSteps())
                .bddType(ctx.getBddType())
                .featureName(ctx.getFeatureName())
                .featureDescription(ctx.getFeatureDescription())
                .tags(ctx.getTags())
                .scenarioOutlineName(ctx.getScenarioOutlineName())
                .exampleParams(ctx.getExampleParams())
                .build();

        URI featureUri = tc.getUri();
        synchronized (this) {
            testCasesByFeature.computeIfAbsent(featureUri, k -> new ArrayList<>()).add(testCase);
        }

        currentScenario.remove();
        List<Artifact> buf2 = CucumberStepContext.currentStepArtifacts.get();
        if (buf2 != null) {
            buf2.clear();
        }
        CucumberStepContext.currentStepArtifacts.remove();

        logger.debug("Scenario finished: {} -> {}", ctx.getScenarioName(), status);
    }

    private void onTestRunFinished(TestRunFinished event) {
        Instant endTime = event.getInstant();
        Instant startTime = runStartTime != null ? runStartTime : endTime;

        TestRun testRun = buildTestRun(startTime, endTime);
        logger.info("Cucumber test run finished. Suites: {}, Total tests: {}",
                testRun.getSuites().size(), testRun.getTotalTests());

        generateReports(testRun);
    }

    // Report building
    /**
     * Builds a {@link TestRun} from all collected scenario results.
     */
    TestRun buildTestRun(Instant startTime, Instant endTime) {
        List<TestSuite> suites = new ArrayList<>();

        for (Map.Entry<URI, List<TestCase>> entry : testCasesByFeature.entrySet()) {
            URI featureUri = entry.getKey();
            List<TestCase> cases = Collections.unmodifiableList(entry.getValue());

            String featureName = featureNameByUri.getOrDefault(featureUri,
                    deriveFeatureNameFromUri(featureUri.toString()));
            String readableFeaturePath = deriveReadableFeaturePath(featureUri.toString());

            int passed = 0, failed = 0, skipped = 0;
            Instant suiteStart = null, suiteEnd = null;
            for (TestCase tc : cases) {
                switch (tc.getStatus()) {
                    case PASSED ->
                        passed++;
                    case FAILED ->
                        failed++;
                    default ->
                        skipped++;
                }
                if (tc.getStartTime() != null) {
                    if (suiteStart == null || tc.getStartTime().isBefore(suiteStart)) {
                        suiteStart = tc.getStartTime();
                    }
                }
                if (tc.getEndTime() != null) {
                    if (suiteEnd == null || tc.getEndTime().isAfter(suiteEnd)) {
                        suiteEnd = tc.getEndTime();
                    }
                }
            }
            if (suiteStart == null) {
                suiteStart = startTime;
            }
            if (suiteEnd == null) {
                suiteEnd = endTime;
            }
            long suiteDuration = Duration.between(suiteStart, suiteEnd).toMillis();

            TestStatus suiteStatus = failed > 0 ? TestStatus.FAILED : TestStatus.PASSED;

            List<String> featureTags = featureTagsByUri.getOrDefault(featureUri, Collections.emptyList());

            TestSuite suite = TestSuite.builder()
                    .id(UUID.randomUUID().toString())
                    .name(featureName)
                    .secondaryText(readableFeaturePath)
                    .startTime(suiteStart)
                    .endTime(suiteEnd)
                    .duration(suiteDuration)
                    .status(suiteStatus)
                    .testCases(cases)
                    .totalTests(cases.size())
                    .passedTests(passed)
                    .failedTests(failed)
                    .skippedTests(skipped)
                    .tags(featureTags)
                    .build();
            suites.add(suite);
        }

        int totalPassed = 0, totalFailed = 0, totalSkipped = 0;
        for (TestSuite s : suites) {
            totalPassed += s.getPassedTests();
            totalFailed += s.getFailedTests();
            totalSkipped += s.getSkippedTests();
        }
        int totalTests = totalPassed + totalFailed + totalSkipped;
        long runDuration = Duration.between(startTime, endTime).toMillis();
        TestStatus runStatus = totalFailed > 0 ? TestStatus.FAILED : TestStatus.PASSED;

        return TestRun.builder()
                .id(UUID.randomUUID().toString())
                .name("PulseReport BDD Run")
                .startTime(startTime)
                .endTime(endTime)
                .duration(runDuration)
                .status(runStatus)
                .suites(suites)
                .totalTests(totalTests)
                .passedTests(totalPassed)
                .failedTests(totalFailed)
                .skippedTests(totalSkipped)
                .build();
    }

    private void generateReports(TestRun testRun) {
        try {
            String outputDir = ReporterConfig.resolveOutputDirectory("reporter.output.directory",
                    "target/pulsereport");
            File reportDir = resolveOutputDirectory(outputDir);
            reportDir.mkdirs();

            File htmlReport = new File(reportDir, "test-report.html");
            new HtmlReportGenerator().generate(testRun, htmlReport);
            logger.info("\n========================================\n"
                    + "PulseReport: HTML report generated\n"
                    + "Location: {}\n"
                    + "========================================", htmlReport.getAbsolutePath());

            File jsonReport = new File(reportDir, "test-report.json");
            new JsonReportGenerator().generate(testRun, jsonReport);
            logger.info("PulseReport: JSON report generated at {}", jsonReport.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Failed to generate BDD reports", e);
        }
    }

    private File resolveOutputDirectory(String outputDir) {
        File configuredOutputDir = new File(outputDir);
        if (configuredOutputDir.isAbsolute()) {
            return configuredOutputDir;
        }

        String workingDirectory = System.getProperty("user.dir", ".");
        return new File(workingDirectory, outputDir);
    }

    // Helpers
    /**
     * Looks up the Examples body row data for a Scenario Outline pickle.
     * The pickle's line points at its Examples body row in the source.
     */
    private ExampleRowData findExampleRowData(io.cucumber.plugin.event.TestCase tc) {
        Map<Integer, ExampleRowData> rows = exampleRowsByUri.get(tc.getUri());
        if (rows == null || tc.getLocation() == null) {
            return null;
        }
        return rows.get(tc.getLocation().getLine());
    }

    private TestStatus convertStatus(Status status) {
        return switch (status) {
            case PASSED ->
                TestStatus.PASSED;
            case FAILED ->
                TestStatus.FAILED;
            case SKIPPED ->
                TestStatus.SKIPPED;
            case PENDING ->
                TestStatus.SKIPPED;
            case UNDEFINED ->
                TestStatus.SKIPPED;
            case AMBIGUOUS ->
                TestStatus.FAILED;
            case UNUSED ->
                TestStatus.SKIPPED;
        };
    }

    private String deriveFeatureNameFromUri(String uri) {
        String path = uri;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            path = path.substring(lastSlash + 1);
        }
        if (path.endsWith(".feature")) {
            path = path.substring(0, path.length() - 8);
        }
        path = path.replace('_', ' ').replace('-', ' ');
        return path.isEmpty() ? path
                : Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }

    private String deriveReadableFeaturePath(String uriOrPath) {
        if (uriOrPath == null || uriOrPath.isBlank()) {
            return null;
        }

        String normalized = uriOrPath.trim().replace('\\', '/');
        if (normalized.startsWith("classpath:")) {
            normalized = normalized.substring("classpath:".length());
        } else {
            try {
                URI uri = URI.create(normalized);
                if (uri.getPath() != null && !uri.getPath().isBlank()) {
                    normalized = uri.getPath();
                }
            } catch (IllegalArgumentException ignored) {
                // Fall back to the raw string when it is not a valid URI.
            }
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        boolean isFeatureFile = normalized.endsWith(".feature");
        if (isFeatureFile) {
            String[] segments = normalized.split("/");
            int featuresSegmentIndex = -1;
            for (int i = 0; i < segments.length; i++) {
                if ("features".equals(segments[i])) {
                    featuresSegmentIndex = i;
                }
            }
            if (featuresSegmentIndex >= 0) {
                return String.join("/",
                        java.util.Arrays.copyOfRange(segments, featuresSegmentIndex, segments.length));
            }
        }

        if (isFeatureFile) {
            int previousSlash = normalized.lastIndexOf('/');
            if (previousSlash >= 0) {
                return normalized.substring(previousSlash + 1);
            }
            return normalized;
        }

        return null;
    }

    private String stackTraceOf(Throwable t) {
        if (t == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
