package io.github.pulsereport.outputs.html;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;
import io.github.pulsereport.core.model.TestSuite;

/**
 * Tests for HtmlReportGenerator. The report renders client-side, so most
 * assertions inspect the embedded run JSON and the static page shell.
 */
class HtmlReportGeneratorTest {

    private static final String DATA_OPEN = "<script type=\"application/json\" id=\"pr-data\">";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HtmlReportGenerator generator;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        generator = new HtmlReportGenerator();
    }

    // ---------- Output and argument handling ----------

    @Test
    void generateToFile() throws IOException {
        File outputFile = new File(tempDir, "nested/test-report.html");

        generator.generate(createSampleTestRun(), outputFile);

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.startsWith("<!DOCTYPE html>"));
        assertTrue(content.contains("Sample Test Run"));
    }

    @Test
    void generateToOutputStream() throws IOException {
        String content = generateHtml(createSampleTestRun());

        assertTrue(content.contains("<html"));
        assertTrue(content.contains("</html>"));
    }

    @Test
    void generateWithNullTestRunToFile() {
        File outputFile = new File(tempDir, "test-report.html");
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null, outputFile));
    }

    @Test
    void generateWithNullFileToFile() {
        TestRun testRun = createSampleTestRun();
        assertThrows(IllegalArgumentException.class, () -> generator.generate(testRun, (File) null));
    }

    @Test
    void generateWithNullTestRunToStream() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null, outputStream));
    }

    @Test
    void generateWithNullOutputStream() {
        TestRun testRun = createSampleTestRun();
        assertThrows(IllegalArgumentException.class, () -> generator.generate(testRun, (OutputStream) null));
    }

    @Test
    void generateWithEmptyTestRun() throws IOException {
        TestRun testRun = TestRun.builder()
                .id("empty-run")
                .name("Empty Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(0)
                .status(TestStatus.PASSED)
                .suites(Collections.emptyList())
                .build();

        String html = generateHtml(testRun);

        assertTrue(html.contains("<title>Empty Test Run - PulseReport</title>"));
        assertEquals(0, embeddedRun(html).path("suites").size());
    }

    @Test
    void configuredTitleReplacesRunName() throws IOException {
        generator = new HtmlReportGenerator("Nightly <checkout>");

        String html = generateHtml(createSampleTestRun());

        assertTrue(html.contains("<title>Nightly &lt;checkout&gt; - PulseReport</title>"));
        assertTrue(html.contains("id=\"run-name\">Nightly &lt;checkout&gt;</h1>"));
        assertEquals("Sample Test Run", embeddedRun(html).path("name").asText(), "run data keeps its own name");
    }

    @Test
    void blankConfiguredTitleFallsBackToRunName() throws IOException {
        generator = new HtmlReportGenerator("  ");

        assertTrue(generateHtml(createSampleTestRun()).contains("<title>Sample Test Run - PulseReport</title>"));
    }

    /** Also writes target/pulsereport/environment-preview.html for checking the top-bar icons by eye. */
    @Test
    void embedsEnvironmentForTheTopBar() throws IOException {
        Map<String, String> environment = new java.util.LinkedHashMap<>();
        environment.put("os.name", "macOS 15.1");
        environment.put("java.version", "21.0.4");
        environment.put("browser.name", "Chrome 129");
        environment.put("git.branch", "release/2.4");
        environment.put("build.number", "1842");
        TestCase skipped = baseCase("tc-skip", TestStatus.SKIPPED).errorMessage("Feature flag disabled").build();
        TestRun sample = createSampleTestRun();
        TestSuite suite = TestSuite.builder()
                .id("suite-env")
                .name("Checkout")
                .startTime(sample.getStartTime())
                .endTime(sample.getEndTime())
                .duration(3000)
                .status(TestStatus.FAILED)
                .testCases(List.of(sample.getSuites().get(0).getTestCases().get(0),
                        sample.getSuites().get(0).getTestCases().get(1), skipped))
                .build();
        TestRun run = TestRun.builder()
                .id("run-env")
                .name("Environment preview")
                .startTime(sample.getStartTime())
                .endTime(sample.getEndTime().plusSeconds(52))
                .duration(55000)
                .status(TestStatus.FAILED)
                .suites(List.of(suite))
                .environment(environment)
                .build();

        File preview = new File("target/pulsereport/environment-preview.html");
        generator.generate(run, preview);

        JsonNode embedded = embeddedRun(Files.readString(preview.toPath(), StandardCharsets.UTF_8)).path("environment");
        environment.forEach((key, value) -> assertEquals(value, embedded.path(key).asText(), key));
    }

    // ---------- Embedded run data ----------

    @Test
    void embedsTheRunAsJson() throws IOException {
        JsonNode run = embeddedRun(generateHtml(createSampleTestRun()));

        assertEquals("Sample Test Run", run.path("name").asText());
        JsonNode cases = run.path("suites").get(0).path("testCases");
        assertEquals(2, cases.size());
        assertEquals("com.example.Test1", cases.get(0).path("className").asText());
        assertEquals("FAILED", cases.get(1).path("status").asText());
        assertEquals("Expected true but was false", cases.get(1).path("errorMessage").asText());
    }

    @Test
    void serializesTimestampsAsIsoStrings() throws IOException {
        JsonNode run = embeddedRun(generateHtml(createSampleTestRun()));

        assertEquals("2026-02-16T10:00:00Z", run.path("startTime").asText());
        assertEquals(3000, run.path("duration").asLong());
    }

    @Test
    void embedsStepsDataTablesAndHttpArtifacts() throws IOException {
        Artifact request = artifact("http-request.txt", "http-request", "/artifacts/http/http-request.txt")
                .mimeType("text/plain").content("POST /api/orders\n\nBody:\n{\"id\":1}").build();
        TestStep step = TestStep.builder()
                .keyword("Given").name("a cart with items").status(TestStatus.PASSED)
                .dataTable(List.of(List.of("SKU", "Qty"), List.of("LM-1", "2")))
                .artifacts(List.of(request))
                .build();
        TestCase testCase = baseCase("tc-bdd", TestStatus.PASSED).bddType("scenario").steps(List.of(step)).build();

        JsonNode embeddedStep = embeddedRun(generateHtml(createTestRunWithTestCase(testCase)))
                .path("suites").get(0).path("testCases").get(0).path("steps").get(0);

        assertEquals("a cart with items", embeddedStep.path("name").asText());
        assertEquals("LM-1", embeddedStep.path("dataTable").get(1).get(0).asText());
        assertEquals("POST /api/orders\n\nBody:\n{\"id\":1}", embeddedStep.path("artifacts").get(0).path("content").asText());
    }

    // ---------- Screenshot inlining ----------

    @Test
    void inlinesPathBackedScreenshotsAsBase64() throws IOException {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        File shot = new File(tempDir, "shot.png");
        Files.write(shot.toPath(), png);
        Artifact artifact = artifact("shot.png", "screenshot", shot.getAbsolutePath()).mimeType("image/png").build();

        JsonNode embedded = firstArtifact(generateHtml(runWithArtifact(artifact)));

        assertEquals(Base64.getEncoder().encodeToString(png), embedded.path("content").asText());
        assertEquals(shot.getAbsolutePath(), embedded.path("path").asText());
    }

    @Test
    void defaultsMimeTypeForInlinedScreenshotWithoutOne() throws IOException {
        File shot = new File(tempDir, "shot");
        Files.write(shot.toPath(), new byte[] {1, 2, 3});
        Artifact artifact = artifact("shot", "screenshot", shot.getAbsolutePath()).build();

        assertEquals("image/png", firstArtifact(generateHtml(runWithArtifact(artifact))).path("mimeType").asText());
    }

    @Test
    void keepsExistingScreenshotContent() throws IOException {
        Artifact artifact = artifact("shot.png", "screenshot", "/does/not/matter.png")
                .mimeType("image/png").content("QUJD").build();

        assertEquals("QUJD", firstArtifact(generateHtml(runWithArtifact(artifact))).path("content").asText());
    }

    @Test
    void leavesUnreadableScreenshotPathOnly() throws IOException {
        Artifact artifact = artifact("missing.png", "screenshot", new File(tempDir, "missing.png").getAbsolutePath())
                .mimeType("image/png").build();

        JsonNode embedded = firstArtifact(generateHtml(runWithArtifact(artifact)));

        assertTrue(embedded.path("content").isMissingNode());
        assertTrue(embedded.path("path").asText().endsWith("missing.png"));
    }

    @Test
    void doesNotInlineNonImageFiles() throws IOException {
        File log = new File(tempDir, "console.log");
        Files.writeString(log.toPath(), "secret log line");
        Artifact artifact = artifact("console.log", "log", log.getAbsolutePath()).mimeType("text/plain").build();

        assertTrue(firstArtifact(generateHtml(runWithArtifact(artifact))).path("content").isMissingNode());
    }

    // ---------- Security ----------

    @Test
    void dataBlockCannotBeTerminatedByRunContent() throws IOException {
        String payload = "</script><script>alert('xss')</script>";
        TestCase testCase = baseCase("tc-xss", TestStatus.FAILED).name(payload).errorMessage(payload).build();

        String html = generateHtml(createTestRunWithTestCase(testCase));

        assertFalse(html.contains("<script>alert('xss')</script>"), "Run content must not create markup");
        JsonNode embedded = embeddedRun(html).path("suites").get(0).path("testCases").get(0);
        assertEquals(payload, embedded.path("name").asText(), "Escaped JSON must decode to the original text");
        assertEquals(payload, embedded.path("errorMessage").asText());
    }

    @Test
    void escapesAngleBracketsAmpersandsAndLineSeparatorsInJson() throws IOException {
        TestCase testCase = baseCase("tc-chars", TestStatus.PASSED).name("a<b>&c\u2028d").build();

        String html = generateHtml(createTestRunWithTestCase(testCase));
        String data = dataBlock(html);

        assertFalse(data.contains("<"));
        assertFalse(data.contains(">"));
        assertFalse(data.contains("&"));
        assertFalse(data.contains("\u2028"));
        assertEquals("a<b>&c\u2028d", embeddedRun(html).path("suites").get(0).path("testCases").get(0).path("name").asText());
    }

    @Test
    void htmlEscapesRunNameInMarkup() throws IOException {
        TestRun testRun = TestRun.builder()
                .id("run-xss")
                .name("\"><img src=x onerror=alert(1)>")
                .startTime(Instant.now())
                .status(TestStatus.PASSED)
                .suites(Collections.emptyList())
                .build();

        String html = generateHtml(testRun);

        assertFalse(html.contains("<img src=x onerror=alert(1)>"));
        assertTrue(html.contains("&quot;&gt;&lt;img src=x onerror=alert(1)&gt;"));
    }

    @Test
    void suiteNameCannotBreakOutOfMarkup() throws IOException {
        TestSuite suite = TestSuite.builder()
                .id("suite-xss")
                .name("\"><script>alert(1)</script><div class=\"")
                .startTime(Instant.now())
                .status(TestStatus.PASSED)
                .testCases(Collections.emptyList())
                .build();

        String html = generateHtml(createTestRunWithSuite(suite));

        assertFalse(html.contains("<script>alert(1)</script>"));
    }

    @Test
    void rendererNeverParsesDataAsHtml() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertFalse(html.contains("innerHTML"));
        assertFalse(html.contains("outerHTML"));
        assertFalse(html.contains("insertAdjacentHTML"));
        assertFalse(html.contains("document.write"));
        assertFalse(html.contains("eval("));
    }

    // ---------- Offline, self-contained page ----------

    @Test
    void htmlIsOfflineCompatible() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertFalse(html.contains("cdn."));
        assertFalse(html.contains("http://"));
        assertFalse(html.contains("https://"));
        assertFalse(html.contains("<script src"));
        assertFalse(html.contains("rel=\"stylesheet\""));
    }

    @Test
    void embedsGeistFontsAsDataUris() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertTrue(hasCss(html, "@font-face\\{font-family:'Geist';src:url\\(data:font/woff2;base64,d09GMg"));
        assertTrue(hasCss(html, "@font-face\\{font-family:'Geist Mono';src:url\\(data:font/woff2;base64,d09GMg"));
    }

    @Test
    void htmlContainsShellAndRenderer() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertTrue(html.contains("<style>"));
        assertTrue(html.contains("id=\"verdict\""));
        assertTrue(html.contains("id=\"tree\""));
        assertTrue(html.contains("id=\"detail\""));
        assertTrue(html.contains("JSON.parse(document.getElementById('pr-data').textContent)"));
        assertTrue(html.contains("<noscript>"));
    }

    @Test
    void viewportAllowsZoom() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertTrue(html.contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"));
        assertFalse(html.contains("user-scalable=no"));
        assertFalse(html.contains("maximum-scale"));
    }

    @Test
    void scrollPanesContainHiddenLabels() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertTrue(hasCss(html, "\\.tree\\s*\\{\\s*position:\\s*relative;"), "sr-only labels in the list must not extend the page");
        assertTrue(hasCss(html, "\\.detail-pane\\s*\\{\\s*position:\\s*relative;"));
    }

    @Test
    void honorsReducedMotionAndPrint() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertTrue(hasCss(html, "@media \\(prefers-reduced-motion:\\s*no-preference\\)"));
        assertTrue(html.contains("@media print"));
        assertTrue(hasCss(html, "@media screen and \\(prefers-color-scheme:\\s*dark\\)"));
    }

    // ---------- Helpers ----------

    private static boolean hasCss(String html, String regex) {
        return java.util.regex.Pattern.compile(regex).matcher(html).find();
    }

    private static Artifact.Builder artifact(String name, String type, String path) {
        return Artifact.builder().name(name).type(type).path(path).timestamp(Instant.parse("2026-02-16T10:00:01Z"));
    }

    private TestCase.Builder baseCase(String id, TestStatus status) {
        return TestCase.builder()
                .id(id)
                .name(id)
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:01Z"))
                .duration(1000)
                .status(status);
    }

    private TestRun runWithArtifact(Artifact artifact) {
        return createTestRunWithTestCase(baseCase("tc-art", TestStatus.PASSED).artifacts(List.of(artifact)).build());
    }

    private TestRun createSampleTestRun() {
        TestCase testCase1 = TestCase.builder()
                .id("tc-1")
                .name("Test Case 1")
                .className("com.example.Test1")
                .methodName("testMethod1")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .build();

        TestCase testCase2 = TestCase.builder()
                .id("tc-2")
                .name("Test Case 2")
                .className("com.example.Test1")
                .methodName("testMethod2")
                .startTime(Instant.parse("2026-02-16T10:00:02Z"))
                .endTime(Instant.parse("2026-02-16T10:00:03Z"))
                .duration(1000)
                .status(TestStatus.FAILED)
                .errorMessage("Expected true but was false")
                .stackTrace("java.lang.AssertionError: Expected true but was false\n\tat com.example.Test1.testMethod2(Test1.java:42)")
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-1")
                .name("Test Suite 1")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:03Z"))
                .duration(3000)
                .status(TestStatus.FAILED)
                .testCases(Arrays.asList(testCase1, testCase2))
                .build();

        return TestRun.builder()
                .id("test-run-1")
                .name("Sample Test Run")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:03Z"))
                .duration(3000)
                .status(TestStatus.FAILED)
                .suites(Arrays.asList(suite))
                .environment(Map.of("Branch", "main"))
                .build();
    }

    private TestRun createTestRunWithTestCase(TestCase testCase) {
        TestSuite suite = TestSuite.builder()
                .id("suite-1")
                .name("Test Suite")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(testCase.getStatus())
                .testCases(Collections.singletonList(testCase))
                .build();
        return createTestRunWithSuite(suite);
    }

    private TestRun createTestRunWithSuite(TestSuite suite) {
        return TestRun.builder()
                .id("run-1")
                .name("Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(suite.getStatus())
                .suites(Collections.singletonList(suite))
                .build();
    }

    private String generateHtml(TestRun testRun) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generate(testRun, outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private static String dataBlock(String html) {
        int start = html.indexOf(DATA_OPEN);
        assertTrue(start >= 0, "Report should embed the run data block");
        start += DATA_OPEN.length();
        int end = html.indexOf("</script>", start);
        assertTrue(end > start, "Run data block should be closed");
        return html.substring(start, end);
    }

    private static JsonNode embeddedRun(String html) throws IOException {
        return MAPPER.readTree(dataBlock(html));
    }

    private static JsonNode firstArtifact(String html) throws IOException {
        return embeddedRun(html).path("suites").get(0).path("testCases").get(0).path("artifacts").get(0);
    }
}
