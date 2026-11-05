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
        environment.put("git.commit", "3f9c2ab71d04e5c86b2f1a9e0d7c4b5a6e8f1023");
        environment.put("git.commitUrl",
                "https://github.com/acme/shop/commit/3f9c2ab71d04e5c86b2f1a9e0d7c4b5a6e8f1023");
        environment.put("ci.run", "Run #42 · CI");
        environment.put("ci.runUrl", "https://github.com/acme/shop/actions/runs/9876543210");
        environment.put("ci.jobUrl", "https://github.com/acme/shop/actions/runs/9876543210/job/123");
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

    @Test
    void topBarRendersCiLinksFromEnvironment() throws IOException {
        Map<String, String> environment = new java.util.LinkedHashMap<>();
        environment.put("git.commit", "3f9c2ab71d04e5c86b2f1a9e0d7c4b5a6e8f1023");
        environment.put("git.commitUrl", "https://github.com/acme/shop/commit/3f9c2ab");
        environment.put("git.branch", "main");
        environment.put("ci.run", "Run #42 · CI");
        environment.put("ci.runUrl", "https://github.com/acme/shop/actions/runs/1");
        environment.put("ci.jobUrl", "https://github.com/acme/shop/actions/runs/1/job/2");
        TestRun sample = createSampleTestRun();
        TestRun run = TestRun.builder()
                .id("run-ci")
                .name("CI run")
                .startTime(sample.getStartTime())
                .endTime(sample.getEndTime())
                .duration(3000)
                .status(TestStatus.FAILED)
                .suites(sample.getSuites())
                .environment(environment)
                .build();

        String html = generateHtml(run);
        JsonNode embedded = embeddedRun(html).path("environment");
        environment.forEach((key, value) -> assertEquals(value, embedded.path(key).asText(), key));

        String fn = functionSource(html, "renderTopbar");
        for (String key : new String[] {"git.commitUrl", "ci.runUrl", "ci.jobUrl"}) {
            assertTrue(html.contains("'" + key + "'"), "Top bar should read " + key);
        }
        assertTrue(fn.contains("ciChips("), "renderTopbar should render the CI chips");
        String chips = functionSource(html, "ciChips");
        assertTrue(chips.contains("ci.jobUrl") && chips.contains("ci.runUrl") && chips.contains("git.commitUrl"),
                "CI chips should link the commit, job and run");
        String link = functionSource(html, "metaChip");
        assertTrue(link.contains("rel: 'noopener noreferrer'"), "CI links must not leak the opener or referrer");
        assertTrue(link.contains("target: '_blank'"), "CI links should open in a new tab");
        assertTrue(html.contains("CI_KEYS"), "CI keys should be excluded from generic environment entries");
        assertTrue(hasCss(html, "\\.run-meta a\\.meta:hover"), "CI links should react on hover");
        assertTrue(hasCss(html, "\\.run-meta \\.meta \\.v\\s*\\{[^}]*text-overflow:\\s*ellipsis;"),
                "Long chip values should truncate");
    }

    @Test
    void topBarRejectsNonHttpLinks() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String fn = functionSource(html, "safeHref");

        assertTrue(fn.contains("new URL("), "Links should be parsed with the URL API");
        assertTrue(fn.contains("'http:'") && fn.contains("'https:'"), "Only http(s) links should be allowed");
        assertFalse(fn.contains("'file:'"), "CI links must not allow file: URLs");
        for (String name : new String[] {"renderTopbar", "ciChips", "metaChip", "safeHref"}) {
            assertFalse(functionSource(html, name).contains("innerHTML"), name + " must not build markup from strings");
        }
        assertTrue(functionSource(html, "metaChip").contains("safeHref("), "Chips should only link vetted URLs");
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
    void leavesOversizedScreenshotsPathOnly() throws IOException {
        File shot = new File(tempDir, "huge.png");
        Files.write(shot.toPath(), new byte[(int) HtmlReportGenerator.MAX_INLINE_IMAGE_BYTES + 1]);
        Artifact artifact = artifact("huge.png", "screenshot", shot.getAbsolutePath()).mimeType("image/png").build();

        assertTrue(firstArtifact(generateHtml(runWithArtifact(artifact))).path("content").isMissingNode());
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
        TestCase testCase = baseCase("tc-chars", TestStatus.PASSED).name("a<b>&c\u2028d\u2029e<!--f").build();

        String html = generateHtml(createTestRunWithTestCase(testCase));
        String data = dataBlock(html);

        assertFalse(data.contains("<"));
        assertFalse(data.contains(">"));
        assertFalse(data.contains("&"));
        assertFalse(data.contains("\u2028"));
        assertFalse(data.contains("\u2029"));
        assertEquals("a<b>&c\u2028d\u2029e<!--f",
                embeddedRun(html).path("suites").get(0).path("testCases").get(0).path("name").asText());
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

    // ---------- Layout ----------

    @Test
    void brandMarkIs32px() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertTrue(hasCss(html, "\\.brand-mark\\s*\\{[^}]*width:\\s*32px;"), ".brand-mark should be 32px wide");
        assertTrue(hasCss(html, "\\.brand-mark\\s*\\{[^}]*height:\\s*32px;"), ".brand-mark should be 32px high");
    }

    @Test
    void detailViewHasNoFixedMaxWidth() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertFalse(html.contains("max-width: 1100px"), "Detail view should fit the available width");
        assertTrue(hasCss(html, "\\.detail\\s*\\{[^}]*container-type:\\s*inline-size;"),
                ".detail must keep container-type: inline-size for the @container rules");
    }

    // ---------- Feature tag tooltips ----------

    @Test
    void embedsFeatureTagsInRunJson() throws IOException {
        TestSuite suite = TestSuite.builder()
                .id("suite-tags")
                .name("Checkout Feature")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .tags(List.of("@smoke", "@api"))
                .testCases(List.of(baseCase("tc-tag", TestStatus.PASSED).build()))
                .build();

        JsonNode tags = embeddedRun(generateHtml(createTestRunWithSuite(suite)))
                .path("suites").get(0).path("tags");

        assertEquals(2, tags.size());
        assertEquals("@smoke", tags.get(0).asText());
        assertEquals("@api", tags.get(1).asText());
    }

    @Test
    void treeTooltipIncludesSuiteTags() throws IOException {
        String html = generateHtml(createSampleTestRun());

        int tree = html.indexOf("class: 'group dz'");
        assertTrue(tree >= 0, "Tree should render suite groups");
        int marker = html.indexOf("suite.secondaryText || suite.name || ''", tree);
        assertTrue(marker >= 0, "Tree summary should keep its data-tip tooltip");
        String construction = html.substring(marker, Math.min(marker + 200, html.length()));
        assertTrue(construction.contains("suite.tags"),
                "Tree summary tooltip should append the suite's feature tags");
    }

    // ---------- Resizable list pane ----------

    @Test
    void rendersAccessiblePaneSplitter() throws IOException {
        String html = generateHtml(createSampleTestRun());

        int role = html.indexOf("role=\"separator\"");
        assertTrue(role >= 0, "Report should render a splitter between the list and detail panes");
        String tag = html.substring(html.lastIndexOf("<div", role), html.indexOf('>', role));
        assertTrue(tag.contains("aria-orientation=\"vertical\""), "Splitter should be a vertical separator");
        assertTrue(tag.contains("aria-controls=\"list-pane\""), "Splitter aria-controls should reference the list pane id");
        assertTrue(tag.contains("tabindex=\"0\""), "Splitter should be keyboard focusable");
        assertTrue(tag.contains("aria-label="), "Splitter should have an accessible name");
        assertTrue(tag.contains("aria-valuenow="), "Splitter should expose its current width");
        assertTrue(tag.contains("aria-valuemin="), "Splitter should expose its minimum width");
        assertTrue(tag.contains("aria-valuemax="), "Splitter should expose its maximum width");
        assertTrue(html.contains("id=\"list-pane\""), "List pane needs an id for aria-controls to reference");
    }

    @Test
    void panesUseListWidthVariable() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertTrue(hasCss(html, "\\.panes\\s*\\{[^}]*grid-template-columns:\\s*var\\(--list-w,\\s*320px\\)"),
                ".panes should size the list column with var(--list-w, 320px)");
        assertFalse(html.contains("minmax(280px, 360px)"),
                "The fixed minmax list column should be replaced by the --list-w variable");
    }

    // ---------- Assertion table ----------

    @Test
    void assertionTableHeadersInMethodFirstOrder() throws IOException {
        String fn = assertionListSource(generateHtml(createSampleTestRun()));

        int thead = fn.indexOf("'thead'");
        assertTrue(thead >= 0, "assertionList should build a thead");
        String head = fn.substring(thead, fn.indexOf("'tbody'", thead));
        int assertion = head.indexOf("'Assertion'");
        int expected = head.indexOf("'Expected'");
        int actual = head.indexOf("'Actual'");
        int message = head.indexOf("'Message'");
        assertTrue(assertion >= 0 && expected > assertion && actual > expected && message > actual,
                "Headers should read Assertion, Expected, Actual, Message");
        assertFalse(head.contains("'Method'"), "The method column is now the Assertion column");
    }

    @Test
    void assertionMessageCellIsExpandable() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String fn = assertionListSource(html);

        assertTrue(fn.contains("class: 'a-clip'"), "Message should render in a shared .a-clip element");
        assertTrue(fn.contains("clip(a.message)"), "Message cell should use the shared clip button");
        assertTrue(fn.contains("type: 'button'"), "Message toggle should be a real button");
        assertTrue(fn.contains("'aria-expanded': 'false'"), "Message button should start collapsed");
        String sync = functionSource(html, "syncClip");
        assertTrue(sync.contains("dataset.tip"), "Clip tooltip should be set through dataset, not markup");
        assertFalse(sync.contains("innerHTML"), "Clip tooltip must never build markup from strings");
        assertTrue(hasCss(html, "\\.a-clip\\s*\\{[^}]*white-space:\\s*nowrap;"), ".a-clip should be single-line");
        assertTrue(hasCss(html, "\\.a-clip\\s*\\{[^}]*text-overflow:\\s*ellipsis;"), ".a-clip should truncate with an ellipsis");
        assertTrue(hasCss(html, "tr\\.is-open \\.a-clip\\s*\\{[^}]*white-space:\\s*pre-wrap;"),
                "Expanded rows should wrap");
        assertTrue(html.contains("closest?.('.a-clip')"), "Clip toggling should use delegated handlers");
        assertFalse(html.contains("a-msg"), "The message-only clip is replaced by the shared clip");
    }

    @Test
    void assertionColumnsAreEqualWidth() throws IOException {
        String html = generateHtml(createSampleTestRun());

        assertTrue(assertionListSource(html).contains("['c-fn', 'c-exp', 'c-act', 'c-msg']"),
                "Columns should stay Assertion, Expected, Actual, Message");
        assertTrue(hasCss(html, "\\.dt\\.at\\s*\\{[^}]*table-layout:\\s*fixed;"), "Assertion table keeps a fixed layout");
        assertTrue(hasCss(html,
                "\\.at \\.c-fn,\\s*\\.at \\.c-exp,\\s*\\.at \\.c-act,\\s*\\.at \\.c-msg\\s*\\{\\s*width:\\s*25%;\\s*\\}"),
                "All four assertion columns should be 25% wide");
        assertFalse(hasCss(html, "\\.at \\.c-[a-z]+[^{]*\\{\\s*width:\\s*2[26]%"), "Old 22%/26% widths should be gone");
    }

    @Test
    void assertionValuesAreTruncatedLikeMessages() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String fn = assertionListSource(html);

        assertTrue(fn.contains("clip(a.expected)") && fn.contains("clip(a.actual)") && fn.contains("clip(a.message)"),
                "Expected, Actual and Message should share the clip button");
        assertTrue(fn.contains("class: 'a-clip', 'aria-expanded': 'false'"), "Clip buttons should start collapsed");

        String row = functionSource(html, "setClipRow");
        assertTrue(row.contains("classList.toggle('is-open'"), "Expansion should be tracked on the row");
        assertTrue(row.contains("querySelectorAll('.a-clip')") && row.contains("setAttribute('aria-expanded'"),
                "Every clip in the row should sync aria-expanded");
        assertTrue(hasCss(html, "e\\.key === 'Escape'[\\s\\S]{0,200}setClipRow\\([^,]+, false\\)"),
                "Escape should collapse the row");

        String sync = functionSource(html, "syncClip");
        assertTrue(sync.contains("scrollWidth > ") && sync.contains("clientWidth"), "Overflow should be detected lazily");
        assertTrue(sync.contains("'data-clipped'"), "Overflowing clips should be marked");
        assertTrue(hasCss(html, "\\.a-clip\\[data-clipped\\],\\s*\\.at tr\\.is-open \\.a-clip\\s*\\{[^}]*cursor:\\s*pointer;"),
                "Only clipped or expanded cells should look clickable");
        assertTrue(hasCss(html, "@media print\\s*\\{[\\s\\S]*\\.at \\.a-clip\\s*\\{[^}]*white-space:\\s*pre-wrap;"),
                "Print should expand every clipped cell");
    }

    @Test
    void clipTooltipIsCappedForLongValues() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String sync = functionSource(html, "syncClip");

        assertTrue(html.contains("const CLIP_TIP_MAX = 300;"), "Tooltip preview should be capped at 300 chars");
        assertTrue(sync.contains(".slice(0, CLIP_TIP_MAX)") && sync.contains("'\\u2026'"),
                "Long values should be cut with an ellipsis in the tooltip");
        assertTrue(sync.contains("delete btn.dataset.tip"), "Unclipped cells should not get a tooltip");
    }

    private static String assertionListSource(String html) {
        return functionSource(html, "assertionList");
    }

    // ---------- HTTP body tools ----------

    @Test
    void httpSidesRenderCopyAndSearchButtons() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String fn = functionSource(html, "httpSide");

        assertTrue(fn.contains("copyButton(") && fn.contains("'Copy ' + label"),
                "Each side should get a copy button labelled per side");
        assertTrue(fn.contains("'aria-label': 'Search ' + label"), "Each side should get a search button labelled per side");
        assertTrue(fn.contains("type: 'button'"), "Toolbar icons should be real buttons");
        assertTrue(fn.contains("'aria-expanded': 'false'"), "Search button should expose its toggle state");
        assertTrue(fn.contains("'aria-controls'"), "Search button should reference the search row");
        assertTrue(fn.contains("type: 'search'"), "Search row should hold a search input");
        assertTrue(fn.contains("'aria-label': 'Find in ' + label"), "Search input needs an accessible name");
        assertTrue(fn.contains("role: 'status'"), "Match count should be announced as a status");
        assertTrue(fn.contains("class: 'xtools'"), "Buttons should be grouped outside the body <pre>");
        assertFalse(fn.contains("[p.line, ...p.headers"), "Sides without a body should not get a line+headers copy");

        String copy = functionSource(html, "copyButton");
        assertTrue(copy.contains("type: 'button'"), "Shared copy button should be a real button");
        assertTrue(functionSource(html, "renderTrace").contains("copyButton("),
                "Stack trace copy should reuse the shared copy button");
        String clip = functionSource(html, "copyText");
        assertTrue(clip.contains("navigator.clipboard") && clip.contains("isSecureContext"),
                "Copy should use the async clipboard API when it is available");
        assertTrue(clip.contains("execCommand('copy')"), "Copy should fall back to execCommand for file:// pages");
        assertTrue(html.contains("'aria-live': 'polite'"), "Copy feedback should be announced politely");

        for (String name : new String[] {"search", "chev-up", "chev-down", "copy", "check", "close"}) {
            assertTrue(html.contains("data-i=\"" + name + "\""), "Icon set should include " + name);
        }
        assertTrue(hasCss(html, "\\.xtools\\s*\\{[^}]*right:\\s*14px;"), "Toolbar should be right aligned, clear of the scrollbar");
        assertTrue(hasCss(html,
                "@media print\\s*\\{[\\s\\S]*\\.body-box > \\.xtools,\\s*\\.body-box > \\.xsearch\\s*\\{\\s*display:\\s*none;"),
                "Toolbar and search row should be hidden in print");
    }

    @Test
    void httpToolsSitInsideBodyContainer() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String fn = functionSource(html, "httpSide");

        int box = fn.indexOf("class: 'body-box'");
        assertTrue(box >= 0, "The body <pre> should be wrapped in a .body-box container");
        String boxCall = fn.substring(box, fn.indexOf(';', box));
        assertTrue(boxCall.contains("class: 'xtools'") && boxCall.contains("ui.row") && boxCall.contains("body)"),
                "Tools, search row and body should all be children of the body container");
        assertFalse(hasCss(fn, "class: 'xlabel' \\}[^\\n]*xtools"), "Tools should no longer sit in the label row");
        assertTrue(fn.contains("bodySearch(body, ui)"), "Search should still run against the <pre>");

        assertTrue(hasCss(html, "\\.body-box\\s*\\{[^}]*position:\\s*relative;"), "Container should anchor the overlay");
        assertTrue(hasCss(html, "\\.body-box > \\.xtools\\s*\\{[^}]*position:\\s*absolute;"), "Tools should overlay the body");
        assertTrue(hasCss(html, "\\.body-box > \\.xtools\\s*\\{[^}]*background:\\s*var\\(--sunken\\);"),
                "Tools should sit on the sunken body colour");
        assertTrue(hasCss(html, "\\.body-box > \\.xsearch\\s*\\{[^}]*position:\\s*absolute;"),
                "Search should float inside the body container");
        assertTrue(hasCss(html,
                "\\.body-box:hover > \\.xtools,\\s*\\.body-box:focus-within > \\.xtools\\s*\\{[^}]*opacity:\\s*1;"),
                "Tools should be fully opaque on hover or focus");
        assertFalse(hasCss(html, "\\.xtools\\s*\\{[^}]*margin-left:\\s*auto;"), "Old label-row placement should be removed");
    }

    @Test
    void bodySearchUsesHighlightApiWithMarkFallback() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String fn = functionSource(html, "bodySearch");

        assertTrue(html.contains("CSS.highlights"), "Search should use the CSS Custom Highlight API");
        assertTrue(fn.contains("new Highlight("), "Search should register Highlight objects");
        assertTrue(fn.contains("createTreeWalker"), "Search should walk text nodes of the body");
        assertTrue(fn.contains("createElement('mark')"), "Search should fall back to DOM-built <mark> elements");
        assertFalse(fn.contains("innerHTML"), "Search must never build markup from strings");
        assertTrue(fn.contains("normalize()"), "Fallback marks should be unwrapped and normalized on clear");
        assertTrue(hasCss(html, "::highlight\\(xsearch\\)\\s*\\{"), "CSS should style the xsearch highlight");
        assertTrue(hasCss(html, "::highlight\\(xsearch-active\\)\\s*\\{"), "CSS should style the active match");
        assertTrue(hasCss(html, "mark\\.xs-hit\\s*\\{"), "CSS should style fallback marks");
    }

    @Test
    void messageColumnHeaderIsRightAligned() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String css = styles(html);

        assertTrue(hasCss(css, "\\.at th:last-child,\\s*\\.at \\.a-text,\\s*\\.at \\.a-text \\.a-clip\\s*\\{[^}]*text-align:\\s*right;"),
                "Message header, cell and clip should hug the table's right edge");
        assertTrue(hasCss(css, "\\.at \\.a-clip\\s*\\{[^}]*text-overflow:\\s*ellipsis;"), "Message should still truncate");
        assertTrue(assertionListSource(html).contains("['Assertion', 'Expected', 'Actual', 'Message']"),
                "Message should stay the last column");

        String narrow = cssBlock(css, "@container (max-width:720px)");
        assertFalse(narrow.contains("text-align: right"), "Stacked layout should not right-align anything");
        assertTrue(hasCss(narrow, "\\.at \\.a-text,\\s*\\.at \\.a-text \\.a-clip\\s*\\{[^}]*text-align:\\s*left;"),
                "Stacked layout should keep the labelled message left-aligned");
    }

    @Test
    void bodyToolsHiddenUntilHoverOrFocus() throws IOException {
        String css = styles(generateHtml(createSampleTestRun()));

        String base = cssRule(css, ".body-box > .xtools {");
        assertTrue(hasCss(base, "opacity:\\s*0;"), "Tools should be fully hidden at rest");
        assertTrue(hasCss(base, "pointer-events:\\s*none;"), "Hidden tools should not catch clicks");
        assertFalse(hasCss(base, "display:\\s*none|visibility:\\s*hidden"), "Hidden tools must stay focusable");

        String shown = cssRule(css, ".body-box:hover > .xtools,\n        .body-box:focus-within > .xtools {");
        assertTrue(hasCss(shown, "opacity:\\s*1;") && hasCss(shown, "pointer-events:\\s*auto;"),
                "Hover or keyboard focus should reveal the tools fully");

        String touch = cssBlock(css, "@media (hover: none)");
        assertTrue(hasCss(touch, "\\.body-box > \\.xtools\\s*\\{[^}]*opacity:\\s*1;[^}]*pointer-events:\\s*auto;"),
                "Touch devices should always show the tools");
        assertFalse(hasCss(css, "\\.xtools\\s*\\{[^}]*opacity:\\s*0\\.6;"), "The dim resting state should be gone");
        assertTrue(hasCss(cssBlock(css, "@media (prefers-reduced-motion: no-preference)"),
                "\\.body-box > \\.xtools\\s*\\{\\s*transition:\\s*opacity"), "Fade should respect reduced motion");
    }

    @Test
    void bodySearchInputMatchesFeatureSearch() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String css = styles(html);
        String fn = functionSource(html, "httpSide");

        assertTrue(fn.contains("h('div', { class: 'search-wrap' }, icon('search'), ui.input)"),
                "Find input should reuse the feature search wrapper and icon");
        int at = fn.indexOf("input: h('input'");
        assertTrue(at >= 0, "httpSide should build the find input");
        String input = fn.substring(at, fn.indexOf("})", at));
        assertTrue(input.contains("type: 'search'") && input.contains("'aria-label': 'Find in ' + label"),
                "Find input keeps its type and accessible name");
        assertFalse(input.contains("class:"), "Find input should be styled only by .search-wrap");
        assertFalse(css.contains("xs-input"), "Conflicting .xs-input styling should be removed");
        assertFalse(hasCss(css, "\\.xsearch[^{]*input[^{]*\\{[^}]*(height|border|background|padding):"),
                "No widget rule should override the shared input look");
        assertTrue(hasCss(css, "\\.xsearch > \\.search-wrap\\s*\\{[^}]*min-width:\\s*0;"), "Wrapper should shrink in the widget");

        String widget = cssRule(css, ".body-box > .xsearch {");
        assertTrue(widget.contains("background: var(--raised);") && widget.contains("border: 1px solid var(--border-strong);"),
                "Widget should use full-contrast surface tokens");
        assertFalse(widget.contains("opacity"), "Widget must not be dimmed");
        String disabled = cssRule(css, ".ghost.xbtn:disabled {");
        assertTrue(hasCss(disabled, "opacity:\\s*0\\.(4[5-9]|[5-9])"), "Disabled buttons should stay legible");
        assertTrue(disabled.contains("color: var(--ink-muted);"), "Disabled buttons should not brighten on hover");
    }

    @Test
    void bodySearchHighlightsUseAccent() throws IOException {
        String css = styles(generateHtml(createSampleTestRun()));

        String all = cssRule(css, "::highlight(xsearch) {");
        String active = cssRule(css, "::highlight(xsearch-active) {");
        String mark = cssRule(css, "mark.xs-hit {");
        String markOn = cssRule(css, "mark.xs-hit.on {");
        for (String rule : new String[] {all, mark}) {
            assertTrue(rule.contains("color-mix(in srgb, var(--accent)"), "Matches should tint with the accent: " + rule);
        }
        for (String rule : new String[] {active, markOn}) {
            assertTrue(hasCss(rule, "background(-color)?:\\s*var\\(--accent\\);"), "Active match should be solid accent: " + rule);
            assertTrue(rule.contains("color: var(--raised);"), "Active match text should contrast with the accent: " + rule);
        }
        for (String rule : new String[] {all, active, mark, markOn}) {
            assertFalse(rule.contains("--bar-skip") || rule.contains("#212529"), "Yellow highlight should be gone: " + rule);
        }
    }

    // The main stylesheet; the first <style> only holds the inlined font faces.
    private static String styles(String html) {
        return html.substring(0, html.lastIndexOf("</style>"));
    }

    // Body of the first rule starting with the exact selector text.
    private static String cssRule(String css, String selector) {
        int at = css.indexOf(selector);
        assertTrue(at >= 0, "CSS should contain " + selector);
        return css.substring(at, css.indexOf('}', at) + 1);
    }

    // Whole at-rule block (brace-balanced) starting at the given header.
    private static String cssBlock(String css, String header) {
        int at = css.indexOf(header);
        assertTrue(at >= 0, "CSS should contain " + header);
        int depth = 0;
        for (int i = css.indexOf('{', at); i < css.length(); i++) {
            char c = css.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return css.substring(at, i + 1);
        }
        return css.substring(at);
    }

    private static String functionSource(String html, String name) {
        int start = html.indexOf("function " + name + "(");
        assertTrue(start >= 0, "Template should define " + name + "()");
        int end = html.indexOf("\n            function ", start + 1);
        int next = html.indexOf("\n            const ", start + 1);
        if (next > start && (end < 0 || next < end)) end = next;
        return html.substring(start, end > start ? end : html.length());
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
