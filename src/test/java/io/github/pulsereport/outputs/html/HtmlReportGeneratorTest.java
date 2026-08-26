package io.github.pulsereport.outputs.html;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import io.github.pulsereport.adapters.selenium.SeleniumAdapter;
import io.github.pulsereport.core.aggregator.TestResultAggregator;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestStep;
import io.github.pulsereport.core.model.TestSuite;

/**
 * Tests for HtmlReportGenerator.
 */
class HtmlReportGeneratorTest {

    private HtmlReportGenerator generator;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        generator = new HtmlReportGenerator();
    }

    @Test
    void generateToFile() throws IOException {
        TestRun testRun = createSampleTestRun();
        File outputFile = new File(tempDir, "test-report.html");

        generator.generate(testRun, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertFalse(content.isEmpty());

        assertTrue(content.contains("<!DOCTYPE html>") || content.contains("<html"));
        assertTrue(content.contains("Sample Test Run"));
    }

    @Test
    void generateToOutputStream() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertFalse(content.isEmpty());
        assertTrue(content.contains("<html") || content.contains("<!DOCTYPE html>"));
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
        assertThrows(IllegalArgumentException.class, () -> generator.generate(testRun, (java.io.OutputStream) null));
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
        File outputFile = new File(tempDir, "empty-report.html");

        generator.generate(testRun, outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("Empty Test Run"));
    }

    @Test
    void htmlContainsSummary() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("Total") || content.contains("total"));
    }

    @Test
    void htmlContainsTestCaseDetails() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("Test Case 1"));
        assertTrue(content.contains("Test Case 2"));
        assertTrue(content.contains("com.example.Test1"));
    }

    @Test
    void htmlContainsErrorMessages() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("Expected true but was false"));
    }

    @Test
    void htmlIsOfflineCompatible() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertFalse(content.contains("cdn."));
        assertFalse(content.contains("http://"));
        assertFalse(content.contains("https://"));
    }

    @Test
    void htmlContainsEmbeddedStyles() throws IOException {
        TestRun testRun = createSampleTestRun();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("<style>") || content.contains("style="));
    }

    @Test
    void htmlContainsApprovedDarkThemeTokens() throws IOException {
        String content = generateHtml(createSampleTestRun());

        assertAll(
                () -> assertTrue(content.contains(":root[data-theme='dark']"), "HTML should include a dark theme selector"),
                () -> assertTrue(content.contains("--bg: #212529;"),
                        "HTML should use Bootstrap dark background token"),
                () -> assertTrue(content.contains("--surface: #2b3035;"),
                        "HTML should use the dark surface token"),
                () -> assertTrue(content.contains("--text-primary: #f8f9fa;"),
                        "HTML should use light neutral text in dark mode"),
                () -> assertTrue(content.contains("--text-secondary: #dee2e6;"),
                        "HTML should use muted secondary text in dark mode"),
                () -> assertTrue(content.contains("--logo-tile-fill: #343a40;"),
                        "HTML should darken the logo tile fill in dark mode"),
                () -> assertTrue(content.contains("--logo-tile-stroke: var(--color-gray-700);"),
                        "HTML should set the logo tile border in dark mode"),
                () -> assertTrue(content.contains("--logo-pulse-stroke: var(--color-teal-400);"),
                        "HTML should use brand primary light for the pulse stroke in dark mode"),
                () -> assertTrue(content.contains("--accent-light: #2b3035;"),
                        "HTML should use the surface token for dark-mode accent-light"));
    }

    @Test
    void htmlContainsApprovedLightThemeTokens() throws IOException {
        String content = generateHtml(createSampleTestRun());

        assertAll(
                () -> assertTrue(content.contains(":root {"),
                        "HTML should continue to define the light-theme tokens in the root selector"),
                () -> assertTrue(content.contains("--bg: var(--color-gray-100);"),
                        "HTML should use the neutral-100 light background token"),
                () -> assertTrue(content.contains("--surface: var(--color-gray-0);"),
                        "HTML should keep report cards on a clean neutral surface"),
                () -> assertTrue(content.contains("--text-primary: var(--color-gray-900);"),
                        "HTML should use brand-secondary for light-theme text"),
                () -> assertTrue(content.contains("--logo-tile-fill: var(--color-gray-100);"),
                        "HTML should keep the light-theme logo tile on the neutral-100 surface"),
                () -> assertTrue(content.contains("--logo-tile-stroke: var(--color-gray-300);"),
                        "HTML should keep the light-theme logo tile border on the neutral-300 edge"),
                () -> assertTrue(content.contains("--logo-pulse-stroke: var(--accent);"),
                        "HTML should keep the light-theme logo pulse aliased to accent"),
                () -> assertTrue(content.contains("--accent: var(--color-teal-600);"),
                        "HTML should use Signal Teal as the primary accent"),
                () -> assertTrue(content.contains("--accent-light: var(--color-gray-200);"),
                        "HTML should use neutral-200 for light-theme emphasis surfaces"));
    }

    @Test
    void htmlUsesSharedLightThemeHoverTreatmentForFilterAndExpandButtons() throws IOException {
        String content = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(content);
        String sharedHoverDeclarations = extractCssRuleDeclarations(
                styles,
                ".filter-btn:not(.active):hover",
                ".tag-filter-btn:not(.has-selection):hover",
                ".expand-btn:not(.active):hover");

        assertAll(
                () -> assertTrue(content.contains("class=\"filter-btn"),
                        "HTML should retain filter button markup hooks"),
                () -> assertTrue(content.contains("class=\"expand-btn\""),
                        "HTML should retain expand button markup hooks"),
                () -> assertTrue(!sharedHoverDeclarations.isEmpty(),
                        "HTML should define a light-theme base CSS rule that combines .filter-btn:hover and .expand-btn:hover"),
                () -> assertTrue(sharedHoverDeclarations.contains("background: var(--surface-overlay);"),
                        "HTML should give the shared light-theme hover rule the surface-overlay background"),
                () -> assertTrue(sharedHoverDeclarations.contains("color: var(--accent);"),
                        "HTML should give the shared light-theme hover rule the approved accent text color"),
                () -> assertTrue(sharedHoverDeclarations.contains("border-color: var(--accent);"),
                        "HTML should give the shared light-theme hover rule the approved accent border color"));
    }

    @Test
    void statusFilterButtonsHaveNoTintedDefaultState() throws IOException {
        String content = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(content);

        assertAll(
                () -> assertTrue(
                        extractCssRuleDeclarations(styles, ".filter-btn-failed:not(.active)").isEmpty(),
                        "HTML should not define a tinted default-state rule for the failed filter button"),
                () -> assertTrue(
                        extractCssRuleDeclarations(styles, ".filter-btn-passed:not(.active)").isEmpty(),
                        "HTML should not define a tinted default-state rule for the passed filter button"),
                () -> assertTrue(
                        extractCssRuleDeclarations(styles, ".filter-btn-skipped:not(.active)").isEmpty(),
                        "HTML should not define a tinted default-state rule for the skipped filter button"),
                () -> assertTrue(
                        extractCssRuleDeclarations(styles, ":root[data-theme='dark'] .filter-btn-failed:not(.active)").isEmpty(),
                        "HTML should not define a dark-mode tinted default-state rule for the failed filter button"),
                () -> assertTrue(
                        extractCssRuleDeclarations(styles, ":root[data-theme='dark'] .filter-btn-passed:not(.active)").isEmpty(),
                        "HTML should not define a dark-mode tinted default-state rule for the passed filter button"),
                () -> assertTrue(
                        extractCssRuleDeclarations(styles, ":root[data-theme='dark'] .filter-btn-skipped:not(.active)").isEmpty(),
                        "HTML should not define a dark-mode tinted default-state rule for the skipped filter button"));
    }

    @Test
    void statusFilterButtonHoverChangesOnlyTextAndBorder() throws IOException {
        String content = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(content);
        String failedHoverDeclarations = extractCssRuleDeclarations(styles, ".filter-btn-failed:not(.active):hover");
        String passedHoverDeclarations = extractCssRuleDeclarations(styles, ".filter-btn-passed:not(.active):hover");
        String skippedHoverDeclarations = extractCssRuleDeclarations(styles, ".filter-btn-skipped:not(.active):hover");

        assertAll(
                () -> assertTrue(!failedHoverDeclarations.isEmpty(),
                        "HTML should define a hover rule for the inactive failed filter button"),
                () -> assertTrue(failedHoverDeclarations.contains("background: var(--bg);"),
                        "Failed filter hover should reset the background to the base surface"),
                () -> assertTrue(failedHoverDeclarations.contains("color: var(--red);"),
                        "Failed filter hover should recolor text with the red status variable"),
                () -> assertTrue(failedHoverDeclarations.contains("border-color: var(--red);"),
                        "Failed filter hover should recolor the border with the red status variable"),
                () -> assertTrue(!passedHoverDeclarations.isEmpty(),
                        "HTML should define a hover rule for the inactive passed filter button"),
                () -> assertTrue(passedHoverDeclarations.contains("background: var(--bg);"),
                        "Passed filter hover should reset the background to the base surface"),
                () -> assertTrue(passedHoverDeclarations.contains("color: var(--green);"),
                        "Passed filter hover should recolor text with the green status variable"),
                () -> assertTrue(passedHoverDeclarations.contains("border-color: var(--green);"),
                        "Passed filter hover should recolor the border with the green status variable"),
                () -> assertTrue(!skippedHoverDeclarations.isEmpty(),
                        "HTML should define a hover rule for the inactive skipped filter button"),
                () -> assertTrue(skippedHoverDeclarations.contains("background: var(--bg);"),
                        "Skipped filter hover should reset the background to the base surface"),
                () -> assertTrue(skippedHoverDeclarations.contains("color: var(--amber);"),
                        "Skipped filter hover should recolor text with the amber status variable"),
                () -> assertTrue(skippedHoverDeclarations.contains("border-color: var(--amber);"),
                        "Skipped filter hover should recolor the border with the amber status variable"));
    }

    @Test
    void statusFilterButtonActiveTextIsAlwaysWhite() throws IOException {
        String content = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(content);
        String failedActiveDeclarations = extractCssRuleDeclarations(styles, ".filter-btn-failed.active");
        String passedActiveDeclarations = extractCssRuleDeclarations(styles, ".filter-btn-passed.active");
        String skippedActiveDeclarations = extractCssRuleDeclarations(styles, ".filter-btn-skipped.active");
        String darkActiveDeclarations = extractCssRuleDeclarations(
                styles,
                ":root[data-theme='dark'] .filter-btn-failed.active",
                ":root[data-theme='dark'] .filter-btn-passed.active",
                ":root[data-theme='dark'] .filter-btn-skipped.active");

        assertAll(
                () -> assertTrue(failedActiveDeclarations.contains("color: #fff;"),
                        "Active failed filter button should keep white text in light mode"),
                () -> assertFalse(failedActiveDeclarations.contains("#212529"),
                        "Active failed filter button should not use dark text"),
                () -> assertTrue(passedActiveDeclarations.contains("color: #fff;"),
                        "Active passed filter button should keep white text in light mode"),
                () -> assertFalse(passedActiveDeclarations.contains("#212529"),
                        "Active passed filter button should not use dark text"),
                () -> assertTrue(skippedActiveDeclarations.contains("color: #fff;"),
                        "Active skipped filter button should keep white text in light mode"),
                () -> assertFalse(skippedActiveDeclarations.contains("#212529"),
                        "Active skipped filter button should not use dark text"),
                () -> assertTrue(!darkActiveDeclarations.isEmpty(),
                        "HTML should define a combined dark-mode active rule for the status filter buttons"),
                () -> assertTrue(darkActiveDeclarations.contains("color: #fff;"),
                        "Active status filter buttons should keep white text in dark mode"),
                () -> assertFalse(darkActiveDeclarations.contains("#212529"),
                        "Active status filter buttons should not use dark text in dark mode"));
    }

    @Test
    void htmlUsesPulseReportProductNameAndBrandTitle() throws IOException {
        String content = generateHtml(createSampleTestRun());

        assertAll(
                () -> assertTrue(content.contains("<title>PulseReport"),
                        "HTML should use the exact PulseReport product name in the document title"),
                () -> assertFalse(content.contains("Pulse Report"),
                        "HTML should not expose the split Pulse Report product name in product-facing copy"));
    }

    @Test
    void htmlRendersUnifiedHeaderWithInlineMetadataAndNoLegacyInformationBlock() throws IOException {
        TestRun testRun = createSeleniumHeaderTestRun(Instant.parse("2026-03-25T10:07:09Z"), true);

        String content = generateHtml(testRun);
        assertAll(
                () -> assertTrue(content.contains("class=\"report-hero\""),
                        "HTML should render the unified report hero container at the top of the page"),
                () -> assertTrue(content.contains("class=\"report-hero-title\">Unified Header Run<"),
                        "HTML should render the run name inside the unified report hero title"),
                () -> assertTrue(content.contains("class=\"report-hero-meta\""),
                        "HTML should render inline metadata inside the unified report hero"),
                () -> assertTrue(content.contains("class=\"report-meta-pill status-pill hero-status-pill failed\""),
                        "HTML should render the hero status pill with a semantic failed-state class for styling"),
                () -> assertTrue(content.contains("Status: FAILED"),
                        "HTML should continue to render the run status value in the unified report hero"),
                () -> assertTrue(content.contains("class=\"report-meta-pill report-meta-pill-icon report-meta-pill-started\""),
                        "HTML should render the start metadata pill with an icon styling hook"),
                () -> assertTrue(content.contains("class=\"report-meta-pill report-meta-pill-icon report-meta-pill-ended\""),
                        "HTML should render the end metadata pill with an icon styling hook when the run has an end time"),
                () -> assertTrue(content.contains("<span class=\"sr-only\">Started </span>"),
                        "HTML should provide hidden accessible text for the iconized start metadata pill"),
                () -> assertTrue(content.contains("<span class=\"sr-only\">Ended </span>"),
                        "HTML should provide hidden accessible text for the iconized end metadata pill"),
                () -> assertTrue(content.contains("class=\"report-meta-icon\" aria-hidden=\"true\""),
                        "HTML should render decorative icon wrappers for iconized header metadata pills"),
                () -> assertFalse(content.contains("Started <span class=\"local-time\""),
                        "HTML should no longer render a visible Started label in the unified report hero metadata"),
                () -> assertFalse(content.contains("Ended <span class=\"local-time\""),
                        "HTML should no longer render a visible Ended label in the unified report hero metadata"),
                () -> assertFalse(content.contains("aria-label=\"Started\""),
                        "HTML should not rely on aria-label alone for the iconized start metadata pill"),
                () -> assertFalse(content.contains("aria-label=\"Ended\""),
                        "HTML should not rely on aria-label alone for the iconized end metadata pill"),
                () -> assertFalse(content.contains("Test Run Information"),
                        "HTML should no longer render the standalone Test Run Information heading"));
    }

    @Test
    void htmlShowsBrowserMetadataOnlyForWebRuns() throws IOException {
        TestRun webRun = createSeleniumHeaderTestRun(Instant.parse("2026-03-25T10:07:09Z"), true);
        TestRun apiRun = createHeaderTestRun(createBrowserEnvironment(), Instant.parse("2026-03-25T10:07:09Z"), false);

        String webContent = generateHtml(webRun);
        String apiContent = generateHtml(apiRun);

        assertAll(
                () -> assertTrue(webContent.contains("Browser: Chrome 124"),
                        "HTML should show browser metadata inline for web test runs"),
                () -> assertTrue(webContent.contains("Platform: macOS 14"),
                        "HTML should show platform metadata inline for web test runs"),
                () -> assertFalse(apiContent.contains("Browser: Chrome 124"),
                        "HTML should omit browser metadata when the run is not a web test"),
                () -> assertFalse(apiContent.contains("Platform: macOS 14"),
                        "HTML should omit browser platform metadata when the run is not a web test"));
    }

    @Test
    void htmlShowsBrowserMetadataForScreenshotOnlySeleniumRuns() throws IOException {
        TestRun screenshotOnlyWebRun = createSeleniumHeaderTestRun(Instant.parse("2026-03-25T10:07:09Z"), true, false);

        String content = generateHtml(screenshotOnlyWebRun);

        assertAll(
                () -> assertTrue(content.contains("Browser: Chrome 124"),
                        "HTML should show browser metadata when a Selenium run only provides screenshot-based web evidence"),
                () -> assertTrue(content.contains("Platform: macOS 14"),
                        "HTML should show platform metadata for screenshot-only Selenium runs"));
    }

    @Test
    void htmlUnifiedHeaderOmitsEndTimeWhenRunHasNoEndTime() throws IOException {
        TestRun inProgressRun = createSeleniumHeaderTestRun(null, true);

        String content = generateHtml(inProgressRun);

        assertAll(
                () -> assertTrue(content.contains("class=\"report-hero\""),
                        "HTML should still render the unified report hero when end time is absent"),
                () -> assertTrue(content.contains("class=\"report-meta-pill report-meta-pill-icon report-meta-pill-started\""),
                        "HTML should continue to render the iconized start metadata when end time is absent"),
                () -> assertTrue(content.contains("<span class=\"sr-only\">Started </span>"),
                        "HTML should keep the hidden accessible text for the iconized start metadata when end time is absent"),
                () -> assertFalse(content.contains("class=\"report-meta-pill report-meta-pill-icon report-meta-pill-ended\""),
                        "HTML should omit the iconized end-time metadata when end time is absent"),
                () -> assertFalse(content.contains("<span class=\"sr-only\">Ended </span>"),
                        "HTML should omit the hidden accessible end-time label when end time is absent"),
                () -> assertFalse(content.contains("Started <span class=\"local-time\""),
                        "HTML should not reintroduce a visible Started label when end time is absent"),
                () -> assertFalse(content.contains("data-utc=\"null\""),
                        "HTML should not emit a null UTC attribute when end time is absent"));
    }

    @Test
    void htmlMapsHeroStatusPillClassFromRunStatus() throws IOException {
        String passedContent = generateHtml(createHeroStatusTestRun(TestStatus.PASSED));
        String failedContent = generateHtml(createHeroStatusTestRun(TestStatus.FAILED));
        String skippedContent = generateHtml(createHeroStatusTestRun(TestStatus.SKIPPED));
        String flakyContent = generateHtml(createHeroStatusTestRun(TestStatus.FLAKY));
        String notRunContent = generateHtmlForHeroStatusValue("NOT_RUN");

        assertAll(
                () -> assertTrue(passedContent.contains("class=\"report-meta-pill status-pill hero-status-pill passed\""),
                        "HTML should map PASSED runs to the passed hero status class"),
                () -> assertTrue(failedContent.contains("class=\"report-meta-pill status-pill hero-status-pill failed\""),
                        "HTML should map FAILED runs to the failed hero status class"),
                () -> assertTrue(skippedContent.contains("class=\"report-meta-pill status-pill hero-status-pill skipped\""),
                        "HTML should map SKIPPED runs to the skipped hero status class"),
                () -> assertTrue(flakyContent.contains("class=\"report-meta-pill status-pill hero-status-pill flaky\""),
                        "HTML should map FLAKY runs to the flaky hero status class"),
                () -> assertTrue(notRunContent.contains("class=\"report-meta-pill status-pill hero-status-pill not-run\""),
                        "HTML should map NOT_RUN runs to the not-run hero status class"));
    }

    @Test
    void htmlUsesCurrentPulseReportLogoMarkInsteadOfLegacyWave() throws IOException {
        String content = generateHtml(createSampleTestRun());

        assertAll(
                () -> assertTrue(content.contains("<symbol id=\"pulse-mark\""),
                        "HTML should embed the current PulseReport logo mark definition"),
                () -> assertTrue(content.contains("<use href=\"#pulse-mark\""),
                        "HTML header should render the current PulseReport logo mark"),
                () -> assertTrue(content.contains("fill=\"var(--logo-tile-fill)\""),
                        "HTML should render the logo tile with theme-aware fill tokens"),
                () -> assertTrue(content.contains("stroke=\"var(--logo-tile-stroke)\""),
                        "HTML should render the logo tile with theme-aware stroke tokens"),
                () -> assertTrue(content.contains("stroke=\"var(--logo-pulse-stroke)\""),
                        "HTML should render the pulse line with theme-aware stroke tokens"),
                () -> assertTrue(content.contains("rx=\"12\""),
                        "HTML should render the rounded-square container from the current PulseReport mark"),
                () -> assertFalse(content.contains("<symbol id=\"pulse-wave\""),
                        "HTML should not embed the legacy pulse-wave logo definition"));
    }

    @Test
    void htmlContainsPersistedThemePreferenceHooks() throws IOException {
        String content = generateHtml(createSampleTestRun());
        String scriptContent = extractScriptContent(content);
        int storedThemeReadIndex = indexOfScriptStatementWithMarkers(
                scriptContent,
                "localStorage.getItem",
                "pulse-report-theme");
        int storedThemeWriteIndex = indexOfScriptStatementWithMarkers(
                scriptContent,
                "localStorage.setItem",
                "pulse-report-theme");
        int systemPreferenceIndex = indexOfScriptStatementWithMarkers(
                scriptContent,
                "window.matchMedia",
                "prefers-color-scheme: dark");
        int storedDarkPreferenceBranchIndex = indexOfScriptStatementWithMarkers(
                scriptContent,
                "storedTheme",
                "dark");
        int storedLightPreferenceBranchIndex = indexOfScriptStatementWithMarkers(
                scriptContent,
                "storedTheme",
                "light");

        assertAll(
                () -> assertTrue(storedThemeReadIndex >= 0,
                        "HTML script should read stored theme preference via localStorage.getItem using pulse-report-theme"),
                () -> assertTrue(storedThemeWriteIndex >= 0,
                        "HTML script should write theme preference updates via localStorage.setItem using pulse-report-theme"),
                () -> assertTrue(storedDarkPreferenceBranchIndex >= 0 && storedLightPreferenceBranchIndex >= 0,
                        "HTML script should branch on stored dark and light preferences before applying fallback logic"),
                () -> assertTrue(systemPreferenceIndex >= 0,
                        "HTML script should still consult system preference via matchMedia('(prefers-color-scheme: dark)') as a fallback"),
                () -> assertTrue(storedThemeReadIndex >= 0 && systemPreferenceIndex >= 0
                        && storedThemeReadIndex < systemPreferenceIndex,
                        "HTML script should consult stored preference before system fallback without relying on whole-document ordering"),
                () -> assertTrue(scriptContent.contains("toggleTheme") || scriptContent.contains("themeToggle"),
                        "HTML script should retain the theme-toggle behavior hook"));
    }

    @Test
    void htmlRetainsThemeToggleButtonMarkup() throws IOException {
        String content = generateHtml(createSampleTestRun());

        assertAll(
                () -> assertTrue(content.contains("class=\"theme-toggle\""),
                        "HTML should retain the theme toggle button class"),
                () -> assertTrue(content.contains("id=\"themeToggle\""),
                        "HTML should retain the theme toggle button id"),
                () -> assertTrue(content.contains("onclick=\"toggleTheme()\""),
                        "HTML should retain the theme toggle click handler"),
                () -> assertTrue(content.contains("aria-label=\"Toggle dark mode\""),
                        "HTML should retain accessible theme toggle markup"));
    }

    @Test
    void htmlContainsArtifactsSection() throws IOException {
        Artifact httpRequest = Artifact.builder()
                .name("HTTP Request")
                .type("http-request")
                .path("/artifacts/http-request.json")
                .mimeType("application/json")
                .content("{\"url\":\"https://api.example.com/users\",\"method\":\"GET\"}")
                .size(100)
                .timestamp(Instant.now())
                .build();

        TestCase testCase = TestCase.builder()
                .id("tc-1")
                .name("Test with Artifacts")
                .className("com.example.Test")
                .methodName("testMethod")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .artifacts(Collections.singletonList(httpRequest))
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-1")
                .name("Test Suite")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .testCases(Collections.singletonList(testCase))
                .build();

        TestRun testRun = TestRun.builder()
                .id("run-1")
                .name("Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .suites(Collections.singletonList(suite))
                .build();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("artifact-section"), "HTML should contain artifact-section class");
        assertTrue(content.contains("api-call-card") || content.contains("artifact-item") || content.contains("{\"url\""),
                "HTML should contain API call structure or artifact content");
    }

    @Test
    void htmlRendersHttpRequest() throws IOException {
        Artifact httpRequest = Artifact.builder()
                .name("GET Request")
                .type("http-request")
                .path("/artifacts/request.json")
                .mimeType("application/json")
                .content("{\"url\":\"https://api.example.com/users\",\"method\":\"GET\"}")
                .size(100)
                .timestamp(Instant.now())
                .build();

        TestCase testCase = TestCase.builder()
                .id("tc-1")
                .name("Test HTTP")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .artifacts(Collections.singletonList(httpRequest))
                .build();

        TestRun testRun = createTestRunWithTestCase(testCase);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("REQUEST") || content.contains("api-call-card"), "HTML should contain REQUEST label or API call card");
        assertTrue(content.contains("GET") || content.contains("api.example.com"), "HTML should contain request method or URL");
    }

    @Test
    void htmlRendersHttpResponse() throws IOException {
        Artifact httpResponse = Artifact.builder()
                .name("API Response")
                .type("http-response")
                .path("/artifacts/response.json")
                .mimeType("application/json")
                .content("{\"status\":200,\"body\":{\"id\":1,\"name\":\"John\"}}")
                .size(150)
                .timestamp(Instant.now())
                .build();

        TestCase testCase = TestCase.builder()
                .id("tc-1")
                .name("Test HTTP Response")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .artifacts(Collections.singletonList(httpResponse))
                .build();

        TestRun testRun = createTestRunWithTestCase(testCase);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("RESPONSE") || content.contains("api-call-card"), "HTML should contain RESPONSE label or API call card");
        assertTrue(content.contains("200") || content.contains("John"), "HTML should contain response status or body");
    }

    @Test
    void htmlPrettyPrintsJson() throws IOException {
        String jsonContent = "{\"user\":{\"id\":1,\"name\":\"John\",\"active\":true}}";
        Artifact jsonArtifact = Artifact.builder()
                .name("JSON Data")
                .type("data")
                .path("/artifacts/data.json")
                .mimeType("application/json")
                .content(jsonContent)
                .size(jsonContent.length())
                .timestamp(Instant.now())
                .build();

        TestCase testCase = TestCase.builder()
                .id("tc-1")
                .name("Test JSON")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .artifacts(Collections.singletonList(jsonArtifact))
                .build();

        TestRun testRun = createTestRunWithTestCase(testCase);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("{") && content.contains("}"), "HTML should contain JSON braces");
        assertTrue(content.contains("user") && content.contains("id"), "HTML should contain JSON keys");
    }

    @Test
    void htmlPrettyPrintsXml() throws IOException {
        String xmlContent = "<user><id>1</id><name>John</name><active>true</active></user>";
        Artifact xmlArtifact = Artifact.builder()
                .name("XML Data")
                .type("data")
                .path("/artifacts/data.xml")
                .mimeType("application/xml")
                .content(xmlContent)
                .size(xmlContent.length())
                .timestamp(Instant.now())
                .build();

        TestCase testCase = TestCase.builder()
                .id("tc-1")
                .name("Test XML")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .artifacts(Collections.singletonList(xmlArtifact))
                .build();

        TestRun testRun = createTestRunWithTestCase(testCase);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("<user>") || content.contains("&lt;user&gt;"), "HTML should contain XML tags");
    }

    @Test
    void htmlShowsBinaryAsDownloadLink() throws IOException {
        Artifact binaryArtifact = Artifact.builder()
                .name("screenshot.png")
                .type("screenshot")
                .mimeType("image/png")
                .path("/path/to/screenshot.png")
                .size(50000)
                .timestamp(Instant.now())
                .build();

        TestCase testCase = TestCase.builder()
                .id("tc-1")
                .name("Test Binary")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .artifacts(Collections.singletonList(binaryArtifact))
                .build();

        TestRun testRun = createTestRunWithTestCase(testCase);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("Download") || content.contains("download"), "HTML should contain download link");
        assertTrue(content.contains("screenshot.png"), "HTML should contain file name");
    }

    @Test
    void htmlHandlesNoArtifacts() throws IOException {
        TestCase testCase = TestCase.builder()
                .id("tc-1")
                .name("Test No Artifacts")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(TestStatus.PASSED)
                .artifacts(Collections.emptyList())
                .build();

        TestRun testRun = createTestRunWithTestCase(testCase);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        generator.generate(testRun, outputStream);

        String content = outputStream.toString();
        assertTrue(content.contains("Test No Artifacts"), "HTML should render test case");
        assertTrue(content.contains("<!DOCTYPE html>") || content.contains("<html"), "HTML should be valid");
    }

    @Test
    void htmlDoesNotRenderExpandHookForPassedNonBddTestWithoutDetails() throws IOException {
        TestCase testCase = TestCase.builder()
                .id("tc-no-details")
                .name("Passed Test Without Details")
                .className("com.example.EmptyDetailsTest")
                .methodName("testWithoutDetails")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .build();

        String content = generateHtml(createTestRunWithTestCase(testCase));

        assertAll(
                () -> assertTrue(content.contains("Passed Test Without Details"),
                        "HTML should render the empty test case row"),
                () -> assertFalse(content.contains("onclick=\"toggleTestCase(this)\""),
                        "HTML should not render a row-level expand click hook for tests without details"),
                () -> assertFalse(content.contains("class=\"test-case-toggle\""),
                        "HTML should not render a toggle chevron for tests without details"),
                () -> assertFalse(content.contains("class=\"test-case-body\""),
                        "HTML should not render expandable body markup for tests without details"));
    }

    @Test
    void htmlDoesNotRenderExpandHookForPassedExpectedExceptionResult() throws IOException {
        TestResultAggregator aggregator = new TestResultAggregator();
        ITestResult result = mock(ITestResult.class);
        ITestNGMethod method = mock(ITestNGMethod.class);

        when(result.getName()).thenReturn("testDivisionByZero");
        when(result.getTestName()).thenReturn("testDivisionByZero");
        when(result.getStatus()).thenReturn(ITestResult.SUCCESS);
        when(result.getStartMillis()).thenReturn(100L);
        when(result.getEndMillis()).thenReturn(200L);
        when(result.getThrowable()).thenReturn(new ArithmeticException("Division by zero"));
        when(result.getParameters()).thenReturn(new Object[0]);
        when(result.getMethod()).thenReturn(method);
        when(method.getMethodName()).thenReturn("testDivisionByZero");
        when(method.getRealClass()).thenReturn(HtmlReportGeneratorTest.class);

        TestCase testCase = aggregator.convertToTestCase(result);

        String content = generateHtml(createTestRunWithTestCase(testCase));

        assertEquals(TestStatus.PASSED, testCase.getStatus());
        assertNull(testCase.getErrorMessage(), "Passed expected-exception results should not carry an error message into HTML rendering");
        assertNull(testCase.getStackTrace(), "Passed expected-exception results should not carry a stack trace into HTML rendering");
        assertAll(
                () -> assertTrue(content.contains("testDivisionByZero"),
                        "HTML should render the expected-exception test case row"),
                () -> assertFalse(content.contains("onclick=\"toggleTestCase(this)\""),
                        "HTML should not render a row-level expand click hook for passed expected-exception tests"),
                () -> assertFalse(content.contains("class=\"test-case-toggle\""),
                        "HTML should not render a toggle chevron for passed expected-exception tests"),
                () -> assertFalse(content.contains("class=\"test-case-body\""),
                        "HTML should not render expandable body markup for passed expected-exception tests"));
    }

    @Test
    void htmlUsesSuiteSecondaryTextInSuiteHeaderWhenPresent() throws IOException {
        TestCase testCase = TestCase.builder()
                .id("tc-bdd-1")
                .name("Successful login")
                .bddType("scenario")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-bdd-1")
                .name("Login Feature")
                .secondaryText("features/authentication/login.feature")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .testCases(Collections.singletonList(testCase))
                .build();

        String content = generateHtml(createTestRunWithSuite(suite));

        assertAll(
                () -> assertTrue(content.contains("Login Feature"),
                        "HTML should render the suite name as the primary suite header text"),
                () -> assertTrue(content.contains("<span class=\"suite-class-path\">features/authentication/login.feature</span>"),
                        "HTML should render suite secondaryText as the suite header secondary text when present"),
                () -> assertFalse(content.contains("<span class=\"suite-class-path\">Surefire test</span>"),
                        "HTML should not fall back to unrelated generic header text when explicit suite secondary text is present"));
    }

    @Test
    void htmlFallsBackToFirstNonBddClassPathForGenericSuiteNames() throws IOException {
        TestCase firstTestCase = TestCase.builder()
                .id("tc-1")
                .name("Fallback test")
                .className("com.example.payments.CheckoutFlowTest")
                .methodName("shouldSubmitOrder")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-generic-1")
                .name("Surefire test")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .testCases(Collections.singletonList(firstTestCase))
                .build();

        String content = generateHtml(createTestRunWithSuite(suite));

        assertAll(
                () -> assertTrue(content.contains("CheckoutFlowTest"),
                        "HTML should derive the suite title from the first non-BDD test class for generic suite names"),
                () -> assertTrue(content.contains("<span class=\"suite-class-path\">com.example.payments.CheckoutFlowTest</span>"),
                        "HTML should keep rendering the derived fully qualified class name as the fallback suite secondary text"),
                () -> assertFalse(content.contains(">Surefire test<"),
                        "HTML should not leave the generic Surefire suite name in the suite header when a class-based fallback is available"));
    }

    @Test
    void htmlPrefersSuiteSecondaryTextOverDerivedClassPathForGenericSuiteNames() throws IOException {
        TestCase firstTestCase = TestCase.builder()
                .id("tc-1")
                .name("Fallback test")
                .className("com.example.checkout.OrderPlacementTest")
                .methodName("shouldPlaceOrder")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-generic-2")
                .name("Surefire suite")
                .secondaryText("src/test/java/com/example/checkout/OrderPlacementTest.java")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .testCases(Collections.singletonList(firstTestCase))
                .build();

        String content = generateHtml(createTestRunWithSuite(suite));

        assertAll(
                () -> assertTrue(content.contains("OrderPlacementTest"),
                        "HTML should still derive a readable suite title from the first non-BDD test class for generic suite names"),
                () -> assertTrue(content.contains("<span class=\"suite-class-path\">src/test/java/com/example/checkout/OrderPlacementTest.java</span>"),
                        "HTML should prefer suite.secondaryText over the derived class path when both are available"),
                () -> assertFalse(content.contains("<span class=\"suite-class-path\">com.example.checkout.OrderPlacementTest</span>"),
                        "HTML should not render the derived class path when an explicit suite secondary text is present"));
    }

    @Test
    void cssApiCallBodyExpandedUsesNoMaxHeightCap() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(html);

        String apiCallBodyExpanded = extractCssRuleDeclarations(styles, ".api-call-body.expanded");
        assertTrue(apiCallBodyExpanded.contains("max-height: none"),
                "api-call-body.expanded should use max-height: none instead of a fixed pixel cap");
        assertTrue(apiCallBodyExpanded.contains("overflow: visible"),
                "api-call-body.expanded should use overflow: visible, not overflow-y: auto");
        assertFalse(apiCallBodyExpanded.contains("800px"),
                "api-call-body.expanded should NOT contain the old 800px cap");
    }

    @Test
    void cssArtifactContentExpandedUsesNoMaxHeightCap() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(html);

        String artifactContentExpanded = extractCssRuleDeclarations(styles, ".artifact-content.expanded");
        assertTrue(artifactContentExpanded.contains("max-height: none"),
                "artifact-content.expanded should use max-height: none instead of a fixed pixel cap");
        assertTrue(artifactContentExpanded.contains("overflow: visible"),
                "artifact-content.expanded should use overflow: visible, not overflow-y: auto");
        assertFalse(artifactContentExpanded.contains("500px"),
                "artifact-content.expanded should NOT contain the old 500px cap");
    }

    @Test
    void cssBddStepArtifactsBodyExpandedUsesNoMaxHeightCap() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(html);

        String bddExpanded = extractCssRuleDeclarations(styles, ".bdd-step-artifacts-body.expanded");
        assertTrue(bddExpanded.contains("max-height: none"),
                "bdd-step-artifacts-body.expanded should use max-height: none instead of a fixed pixel cap");
        assertFalse(bddExpanded.contains("2000px"),
                "bdd-step-artifacts-body.expanded should NOT contain the old 2000px cap");
    }

    @Test
    void jsContainsSmoothExpandCollapseHelpers() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String scripts = extractScriptContent(html);

        assertTrue(scripts.contains("function smoothExpand"),
                "JS should contain smoothExpand helper function");
        assertTrue(scripts.contains("function smoothCollapse"),
                "JS should contain smoothCollapse helper function");
        assertTrue(scripts.contains("scrollHeight"),
                "JS should use scrollHeight for measured height transitions");
    }

    @Test
    void jsToggleFunctionsUseSmoothHelpers() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String scripts = extractScriptContent(html);

        assertTrue(scripts.contains("smoothExpand(content)") || scripts.contains("smoothExpand(body)"),
                "toggleArtifact/toggleApiCall should delegate to smoothExpand");
        assertTrue(scripts.contains("smoothCollapse(content)") || scripts.contains("smoothCollapse(body)"),
                "toggleArtifact/toggleApiCall should delegate to smoothCollapse");
    }

    @Test
    void cssApiCallTitleHasEllipsisOverflow() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(html);

        String apiCallTitle = extractCssRuleDeclarations(styles, ".api-call-title");
        assertTrue(apiCallTitle.contains("text-overflow: ellipsis"),
                "api-call-title should use text-overflow: ellipsis for long URLs");
        assertTrue(apiCallTitle.contains("white-space: nowrap"),
                "api-call-title should use white-space: nowrap");
        assertTrue(apiCallTitle.contains("overflow: hidden"),
                "api-call-title should use overflow: hidden");
        assertTrue(apiCallTitle.contains("min-width: 0"),
                "api-call-title should use min-width: 0 for flex shrink");
    }

    @Test
    void cssArtifactCodeHasOverflowWrapAnywhere() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(html);

        String artifactCode = extractCssRuleDeclarations(styles, ".artifact-code");
        assertTrue(artifactCode.contains("overflow-wrap: anywhere"),
                "artifact-code should use overflow-wrap: anywhere for long tokens");
        assertTrue(artifactCode.contains("overflow-x: auto"),
                "artifact-code should use overflow-x: auto as safety net");
    }

    @Test
    void multipleApiCallsPerStepRendersApiCallCards() throws IOException {
        List<Artifact> artifacts = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            artifacts.add(Artifact.builder()
                    .name("Request " + i)
                    .type("http-request")
                    .content("POST /api/v1/endpoint" + i + " HTTP/1.1\nHost: example.com\nContent-Type: application/json\n\n{\"id\":" + i + "}")
                    .mimeType("application/json")
                    .path("/artifacts/req" + i + ".json")
                    .size(100)
                    .timestamp(Instant.now())
                    .build());
            artifacts.add(Artifact.builder()
                    .name("Response " + i)
                    .type("http-response")
                    .content("Status: 200 OK\nContent-Type: application/json\n\n{\"result\":\"success\",\"id\":" + i + "}")
                    .mimeType("application/json")
                    .path("/artifacts/rsp" + i + ".json")
                    .size(100)
                    .timestamp(Instant.now())
                    .build());
        }

        TestStep step = TestStep.builder()
                .name("multiple API calls are made")
                .keyword("When")
                .status(TestStatus.PASSED)
                .artifacts(artifacts)
                .build();

        TestCase testCase = TestCase.builder()
                .id("tc-multi")
                .name("Multi API Step Test")
                .className("com.example.MultiApiTest")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(3000)
                .status(TestStatus.PASSED)
                .bddType("Scenario")
                .featureName("Multi API Feature")
                .steps(Collections.singletonList(step))
                .build();

        String html = generateHtml(createTestRunWithTestCase(testCase));

        // Then - should render multiple api-call-card elements
        int cardCount = 0;
        int fromIndex = 0;
        while ((fromIndex = html.indexOf("api-call-card", fromIndex)) != -1) {
            cardCount++;
            fromIndex++;
        }
        assertTrue(cardCount >= 3, "Should render at least 3 api-call-card elements for 3 API call pairs, found " + cardCount);

        assertTrue(html.contains("/api/v1/endpoint1"), "Should contain first API path");
        assertTrue(html.contains("/api/v1/endpoint2"), "Should contain second API path");
        assertTrue(html.contains("/api/v1/endpoint3"), "Should contain third API path");
    }

    @Test
    void renderedHtml_containsNoExamplesTableMarkup() throws IOException {
        TestCase outlineCase = TestCase.builder()
                .id("tc-outline-1")
                .name("Login with alice")
                .bddType("scenario_outline")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-outline-1")
                .name("Login Feature")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .testCases(Collections.singletonList(outlineCase))
                .build();

        String html = generateHtml(createTestRunWithSuite(suite));

        assertAll(
                () -> assertFalse(html.contains("examples-table-wrapper"),
                        "Rendered HTML should not contain examples-table-wrapper markup"),
                () -> assertFalse(html.contains("examples-table-caption"),
                        "Rendered HTML should not contain examples-table-caption markup"),
                () -> assertFalse(html.contains("examples-table-row"),
                        "Rendered HTML should not contain examples-table-row markup"),
                () -> assertFalse(html.contains("examples-table"),
                        "Rendered HTML should not contain examples-table markup"),
                () -> assertFalse(html.contains("data-outline"),
                        "Rendered HTML should not contain data-outline attributes"),
                () -> assertTrue(html.contains("Login with alice"),
                        "The scenario outline test case card should still render with its substituted name"));
    }

    @Test
    void renderedHtml_wrapsStepDataTablesInScrollContainer() throws IOException {
        List<List<String>> wideTable = new ArrayList<>();
        List<String> headerRow = new ArrayList<>();
        List<String> dataRow = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            headerRow.add("Header " + i);
            dataRow.add("very-long-unbroken-cell-value-token-" + i);
        }
        wideTable.add(headerRow);
        wideTable.add(dataRow);

        TestStep backgroundStep = TestStep.builder()
                .name("the system is initialized")
                .keyword("Given")
                .status(TestStatus.PASSED)
                .dataTable(wideTable)
                .build();

        TestStep scenarioStep = TestStep.builder()
                .name("a wide table is provided")
                .keyword("When")
                .status(TestStatus.PASSED)
                .dataTable(wideTable)
                .build();

        TestCase testCase = TestCase.builder()
                .id("tc-datatable-scroll")
                .name("Wide datatable scenario")
                .bddType("scenario")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .backgroundSteps(Collections.singletonList(backgroundStep))
                .steps(Collections.singletonList(scenarioStep))
                .build();

        String html = generateHtml(createTestRunWithTestCase(testCase));

        Matcher wrapperMatcher = Pattern.compile(
                "<div class=\"bdd-table-scroll\">\\s*<table class=\"bdd-step-datatable\">").matcher(html);
        int wrapperCount = 0;
        while (wrapperMatcher.find()) {
            wrapperCount++;
        }
        int wrapped = wrapperCount;

        int tableCount = html.split("<table class=\"bdd-step-datatable\">", -1).length - 1;

        String styles = extractStyleContent(html);
        String cellRule = extractCssRuleDeclarations(styles, ".bdd-step-datatable th", ".bdd-step-datatable td");

        assertAll(
                () -> assertTrue(wrapped >= 2,
                        "Both background and scenario step datatables should be wrapped in a bdd-table-scroll container, found " + wrapped),
                () -> assertTrue(tableCount >= 2,
                        "Both background and scenario steps should still render bdd-step-datatable tables, found " + tableCount),
                () -> assertTrue(Pattern.compile("\\.bdd-table-scroll\\s*\\{[^}]*overflow-x:\\s*auto").matcher(html).find(),
                        "Emitted CSS should contain a .bdd-table-scroll rule with overflow-x: auto"),
                () -> assertFalse(cellRule.isEmpty(),
                        "Emitted CSS should contain a .bdd-step-datatable th, .bdd-step-datatable td rule"),
                () -> assertFalse(cellRule.contains("word-break: break-word"),
                        ".bdd-step-datatable cells must not use word-break: break-word (crushes wide tables into slivers)"),
                () -> assertFalse(cellRule.contains("overflow-wrap: anywhere"),
                        ".bdd-step-datatable cells must not use overflow-wrap: anywhere (crushes wide tables into slivers)"),
                () -> assertTrue(cellRule.contains("overflow-wrap: break-word"),
                        ".bdd-step-datatable cells should use overflow-wrap: break-word so only overflowing tokens wrap"),
                () -> assertTrue(cellRule.contains("max-width: 320px"),
                        ".bdd-step-datatable cells should cap at max-width: 320px so long tokens wrap within the cell"));
    }

    @Test
    void renderedHtml_stepNameRuleAllowsWrapping() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(html);

        String stepName = extractCssRuleDeclarations(styles, ".bdd-step-name");
        String tableScroll = extractCssRuleDeclarations(styles, ".bdd-table-scroll");
        String artifactsExpanded = extractCssRuleDeclarations(styles, ".bdd-step-artifacts-body.expanded");

        assertAll(
                () -> assertTrue(stepName.contains("min-width: 0"),
                        ".bdd-step-name should use min-width: 0 so long unbroken step names can shrink within the flex row"),
                () -> assertTrue(stepName.contains("word-break: break-word"),
                        ".bdd-step-name should use word-break: break-word so long names wrap instead of overflowing"),
                () -> assertFalse(tableScroll.isEmpty(),
                        "Emitted CSS should contain a .bdd-table-scroll rule"),
                () -> assertTrue(tableScroll.contains("overflow-x: auto"),
                        ".bdd-table-scroll should use overflow-x: auto (Phase 3 regression guard)"),
                () -> assertFalse(artifactsExpanded.isEmpty(),
                        "Emitted CSS should contain a .bdd-step-artifacts-body.expanded rule"),
                () -> assertFalse(artifactsExpanded.contains("overflow: visible"),
                        ".bdd-step-artifacts-body.expanded should not use overflow: visible; wide expanded content must not escape the card"));
    }

    @Test
    void renderedHtml_docStringRuleBreaksLongLines() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(html);

        String docString = extractCssRuleDeclarations(styles, ".bdd-step-docstring");

        assertAll(
                () -> assertFalse(docString.isEmpty(),
                        "Emitted CSS should contain a .bdd-step-docstring rule"),
                () -> assertTrue(docString.contains("word-break: break-word"),
                        ".bdd-step-docstring should use word-break: break-word so long unbroken tokens (URLs, JWTs) wrap"),
                () -> assertTrue(docString.contains("overflow-wrap: anywhere"),
                        ".bdd-step-docstring should use overflow-wrap: anywhere so long unbroken tokens (URLs, JWTs) wrap"));
    }

    @Test
    void renderedHtml_tableScrollUsesNativeScrollbar() throws IOException {
        String html = generateHtml(createSampleTestRun());
        String styles = extractStyleContent(html);
        String scripts = extractScriptContent(html);

        String tableScroll = extractCssRuleDeclarations(styles, ".bdd-table-scroll");
        String webkitScrollbar = extractCssRuleDeclarations(styles, ".bdd-table-scroll::-webkit-scrollbar");
        String webkitThumb = extractCssRuleDeclarations(styles, ".bdd-table-scroll::-webkit-scrollbar-thumb");
        String webkitTrack = extractCssRuleDeclarations(styles, ".bdd-table-scroll::-webkit-scrollbar-track");
        String indicatorTrack = extractCssRuleDeclarations(styles, ".bdd-scroll-indicator");
        String indicatorVisible = extractCssRuleDeclarations(styles, ".bdd-scroll-indicator.visible");
        String indicatorThumb = extractCssRuleDeclarations(styles, ".bdd-scroll-indicator-thumb");

        assertAll(
                () -> assertFalse(tableScroll.isEmpty(),
                        "Emitted CSS should contain a .bdd-table-scroll rule"),
                () -> assertTrue(tableScroll.contains("overflow-x: auto"),
                        ".bdd-table-scroll should use overflow-x: auto for native horizontal scrolling"),
                () -> assertTrue(tableScroll.contains("max-width: 100%"),
                        ".bdd-table-scroll should use max-width: 100% so the wrapper never exceeds its container"),
                () -> assertFalse(tableScroll.contains("scrollbar-width"),
                        ".bdd-table-scroll must not set scrollbar-width; the native scrollbar stays visible"),
                () -> assertFalse(tableScroll.contains("scrollbar-color"),
                        ".bdd-table-scroll must not set scrollbar-color; the native scrollbar stays unstyled"),
                () -> assertTrue(webkitScrollbar.isEmpty(),
                        "Emitted CSS must not contain a .bdd-table-scroll::-webkit-scrollbar rule"),
                () -> assertTrue(webkitThumb.isEmpty(),
                        "Emitted CSS must not contain a .bdd-table-scroll::-webkit-scrollbar-thumb rule"),
                () -> assertTrue(webkitTrack.isEmpty(),
                        "Emitted CSS must not contain a .bdd-table-scroll::-webkit-scrollbar-track rule"),
                () -> assertTrue(indicatorTrack.isEmpty(),
                        "Emitted CSS must not contain a .bdd-scroll-indicator rule (custom scrollbar removed)"),
                () -> assertTrue(indicatorVisible.isEmpty(),
                        "Emitted CSS must not contain a .bdd-scroll-indicator.visible rule (custom scrollbar removed)"),
                () -> assertTrue(indicatorThumb.isEmpty(),
                        "Emitted CSS must not contain a .bdd-scroll-indicator-thumb rule (custom scrollbar removed)"),
                () -> assertFalse(styles.contains("bdd-scroll-indicator"),
                        "Emitted CSS must not reference bdd-scroll-indicator at all (custom scrollbar removed)"),
                () -> assertFalse(scripts.contains("initTableScrollIndicators"),
                        "JS must not contain or call initTableScrollIndicators (custom scrollbar removed)"));

        // Phase 5 regression guard: the wrapper markup stays; only the scrollbar chrome was removed
        List<List<String>> table = new ArrayList<>();
        table.add(Arrays.asList("Header 1", "Header 2"));
        table.add(Arrays.asList("value-1", "value-2"));

        TestStep step = TestStep.builder()
                .name("a table is provided")
                .keyword("When")
                .status(TestStatus.PASSED)
                .dataTable(table)
                .build();

        TestCase testCase = TestCase.builder()
                .id("tc-datatable-native-scroll")
                .name("Datatable scenario")
                .bddType("scenario")
                .startTime(Instant.parse("2026-03-10T10:00:00Z"))
                .endTime(Instant.parse("2026-03-10T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .steps(Collections.singletonList(step))
                .build();

        String datatableHtml = generateHtml(createTestRunWithTestCase(testCase));
        assertTrue(datatableHtml.contains("<div class=\"bdd-table-scroll\">"),
                "Rendered output should still wrap step datatables in a bdd-table-scroll container (Phase 5 regression guard)");
    }

    @Test
    void xssEscapingInTestName() throws IOException {
        TestCase xssCase = TestCase.builder()
                .id("tc-xss-name")
                .name("<script>alert('xss')</script>")
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(100))
                .duration(100)
                .status(TestStatus.PASSED)
                .build();

        TestRun run = createTestRunWithTestCase(xssCase);
        File outputFile = new File(tempDir, "xss-test.html");
        generator.generate(run, outputFile);

        String html = Files.readString(outputFile.toPath());
        assertFalse(html.contains("<script>alert('xss')</script>"), "Unescaped script tag found in output");
        assertTrue(html.contains("&lt;script&gt;") || html.contains("&#60;script&#62;"),
                "XSS payload should be HTML-escaped");
    }

    @Test
    void xssEscapingInErrorMessage() throws IOException {
        TestCase xssCase = TestCase.builder()
                .id("tc-xss-error")
                .name("failingTest")
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(100))
                .duration(100)
                .status(TestStatus.FAILED)
                .errorMessage("<img src=x onerror=alert('xss')>")
                .stackTrace("at <script>document.cookie</script>\n")
                .build();

        TestRun run = createTestRunWithTestCase(xssCase);
        File outputFile = new File(tempDir, "xss-error-test.html");
        generator.generate(run, outputFile);

        String html = Files.readString(outputFile.toPath());
        assertFalse(html.contains("<img src=x onerror="), "Unescaped img/onerror found in output");
        assertFalse(html.contains("<script>document.cookie</script>"), "Unescaped script in stack trace");
    }

    @Test
    void xssEscapingInSuiteName() throws IOException {
        TestSuite suite = TestSuite.builder()
                .id("suite-xss")
                .name("\"><script>alert(1)</script><div class=\"")
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(100))
                .duration(100)
                .status(TestStatus.PASSED)
                .testCases(Collections.emptyList())
                .build();

        TestRun run = createTestRunWithSuite(suite);
        File outputFile = new File(tempDir, "xss-suite-test.html");
        generator.generate(run, outputFile);

        String html = Files.readString(outputFile.toPath());
        assertFalse(html.contains("\"><script>alert(1)</script>"),
                "Unescaped attribute breakout XSS found");
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
                .build();
    }

    private TestRun createHeaderTestRun(Map<String, String> environment, Instant endTime, boolean includeWebSignals) {
        return createHeaderTestRun(environment, endTime, includeWebSignals, includeWebSignals);
    }

    private TestRun createHeroStatusTestRun(TestStatus status) {
        TestRun baseRun = createHeaderTestRun(createBrowserEnvironment(), Instant.parse("2026-03-25T10:07:09Z"), false, false);

        return TestRun.builder()
                .id(baseRun.getId())
                .name(baseRun.getName())
                .startTime(baseRun.getStartTime())
                .endTime(baseRun.getEndTime())
                .duration(baseRun.getDuration())
                .status(status)
                .environment(baseRun.getEnvironment())
                .totalTests(baseRun.getTotalTests())
                .passedTests(baseRun.getPassedTests())
                .failedTests(baseRun.getFailedTests())
                .skippedTests(baseRun.getSkippedTests())
                .suites(baseRun.getSuites())
                .build();
    }

    private String generateHtmlForHeroStatusValue(String status) throws IOException {
        Configuration configuration = new Configuration(new Version(2, 3, 31));
        configuration.setClassForTemplateLoading(HtmlReportGenerator.class, "/templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setLogTemplateExceptions(false);
        configuration.setAPIBuiltinEnabled(true);

        Map<String, Object> testRun = new LinkedHashMap<>();
        testRun.put("id", "run-hero-status");
        testRun.put("name", "Hero Status Mapping Run");
        testRun.put("startTime", Instant.parse("2026-03-25T10:00:00Z"));
        testRun.put("endTime", Instant.parse("2026-03-25T10:07:09Z"));
        testRun.put("duration", 4500L);
        testRun.put("status", status);
        testRun.put("environment", createBrowserEnvironment());
        testRun.put("totalTests", 0);
        testRun.put("passedTests", 0);
        testRun.put("failedTests", 0);
        testRun.put("skippedTests", 0);
        testRun.put("suites", Collections.emptyList());

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("testRun", testRun);

        try (StringWriter writer = new StringWriter()) {
            Template template = configuration.getTemplate("html-report.ftl");
            template.process(dataModel, writer);
            return writer.toString();
        } catch (TemplateException e) {
            throw new IOException("Failed to render template for hero status mapping test", e);
        }
    }

    private TestRun createHeaderTestRun(
            Map<String, String> environment,
            Instant endTime,
            boolean includeWebScreenshotSignal,
            boolean includeWebMetricSignal) {
        TestCase passedTest = TestCase.builder()
                .id("tc-header-pass")
                .name("Header Passed Test")
                .className("com.example.HeaderTest")
                .methodName("passes")
                .startTime(Instant.parse("2026-03-25T10:00:00Z"))
                .endTime(Instant.parse("2026-03-25T10:00:02Z"))
                .duration(2000)
                .status(TestStatus.PASSED)
                .artifacts(includeWebScreenshotSignal ? Collections.singletonList(createBrowserScreenshotArtifact()) : Collections.emptyList())
                .metrics(includeWebMetricSignal ? Collections.singletonList(createPageLoadMetric()) : Collections.emptyList())
                .build();

        TestCase failedTest = TestCase.builder()
                .id("tc-header-fail")
                .name("Header Failed Test")
                .className("com.example.HeaderTest")
                .methodName("fails")
                .startTime(Instant.parse("2026-03-25T10:00:02Z"))
                .endTime(Instant.parse("2026-03-25T10:00:04Z"))
                .duration(2000)
                .status(TestStatus.FAILED)
                .errorMessage("boom")
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-header")
                .name("Header Suite")
                .startTime(Instant.parse("2026-03-25T10:00:00Z"))
                .endTime(Instant.parse("2026-03-25T10:00:04Z"))
                .duration(4000)
                .status(TestStatus.FAILED)
                .testCases(Arrays.asList(passedTest, failedTest))
                .build();

        return TestRun.builder()
                .id("run-header")
                .name("Unified Header Run")
                .startTime(Instant.parse("2026-03-25T10:00:00Z"))
                .endTime(endTime)
                .duration(4500)
                .status(TestStatus.FAILED)
                .environment(environment)
                .totalTests(2)
                .passedTests(1)
                .failedTests(1)
                .skippedTests(0)
                .suites(Arrays.asList(suite))
                .build();
    }

    private TestRun createSeleniumHeaderTestRun(Instant endTime, boolean includeWebSignals) {
        return createSeleniumHeaderTestRun(endTime, includeWebSignals, includeWebSignals);
    }

    private TestRun createSeleniumHeaderTestRun(Instant endTime, boolean includeWebScreenshotSignal, boolean includeWebMetricSignal) {
        SeleniumAdapter adapter = new SeleniumAdapter();
        ISuite mockSuite = mock(ISuite.class);
        ITestContext mockContext = mock(ITestContext.class);
        ITestResult mockResult = mock(ITestResult.class);
        ITestNGMethod mockMethod = mock(ITestNGMethod.class);

        when(mockSuite.getName()).thenReturn("Unified Header Run");
        when(mockContext.getName()).thenReturn("Header Suite");
        when(mockContext.getSuite()).thenReturn(mockSuite);
        when(mockContext.getStartDate()).thenReturn(new java.util.Date(Instant.parse("2026-03-25T10:00:00Z").toEpochMilli()));
        when(mockContext.getEndDate()).thenReturn(new java.util.Date((endTime != null ? endTime : Instant.parse("2026-03-25T10:00:04Z")).toEpochMilli()));

        org.testng.IResultMap passedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap failedTests = mock(org.testng.IResultMap.class);
        org.testng.IResultMap skippedTests = mock(org.testng.IResultMap.class);
        when(mockContext.getPassedTests()).thenReturn(passedTests);
        when(mockContext.getFailedTests()).thenReturn(failedTests);
        when(mockContext.getSkippedTests()).thenReturn(skippedTests);
        when(skippedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(failedTests.getAllResults()).thenReturn(new java.util.HashSet<>());
        when(passedTests.getAllResults()).thenReturn(new java.util.HashSet<>(Collections.singleton(mockResult)));

        when(mockResult.getMethod()).thenReturn(mockMethod);
        when(mockResult.getName()).thenReturn("Header Passed Test");
        when(mockResult.getStatus()).thenReturn(ITestResult.SUCCESS);
        when(mockResult.getStartMillis()).thenReturn(Instant.parse("2026-03-25T10:00:00Z").toEpochMilli());
        when(mockResult.getEndMillis()).thenReturn(Instant.parse("2026-03-25T10:00:04Z").toEpochMilli());
        when(mockResult.getTestContext()).thenReturn(mockContext);
        when(mockMethod.getMethodName()).thenReturn("passes");
        when(mockMethod.getRealClass()).thenReturn(HtmlReportGeneratorTest.class);

        adapter.onStart(mockSuite);
        adapter.onStart(mockContext);
        adapter.onTestStart(mockResult);
        adapter.recordBrowserMetadata("Chrome", "124", "macOS 14");
        if (includeWebScreenshotSignal) {
            adapter.captureBrowserScreenshot("Header Passed Test", "homepage.png", "/artifacts/screenshots/homepage.png", 2048);
        }
        if (includeWebMetricSignal) {
            adapter.recordPageLoadTime("Header Passed Test", 842);
        }
        adapter.onTestSuccess(mockResult);
        adapter.onFinish(mockContext);
        adapter.onFinish(mockSuite);

        TestRun seleniumRun = adapter.getTestRun();
        return TestRun.builder()
                .id(seleniumRun.getId())
                .name("Unified Header Run")
                .startTime(Instant.parse("2026-03-25T10:00:00Z"))
                .endTime(endTime)
                .duration(4500)
                .status(TestStatus.FAILED)
                .environment(seleniumRun.getEnvironment())
                .totalTests(2)
                .passedTests(1)
                .failedTests(1)
                .skippedTests(0)
                .suites(createHeaderTestRun(seleniumRun.getEnvironment(), endTime, includeWebScreenshotSignal, includeWebMetricSignal).getSuites())
                .build();
    }

    private Map<String, String> createBrowserEnvironment() {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("browser", "Chrome");
        environment.put("browserVersion", "124");
        environment.put("platform", "macOS 14");
        return environment;
    }

    private Artifact createBrowserScreenshotArtifact() {
        return Artifact.builder()
                .name("homepage.png")
                .type("screenshot")
                .path("/artifacts/screenshots/homepage.png")
                .mimeType("image/png")
                .size(2048)
                .timestamp(Instant.parse("2026-03-25T10:00:01Z"))
                .build();
    }

    private Metric createPageLoadMetric() {
        return Metric.builder()
                .name("page.load.time")
                .value(842)
                .unit("ms")
                .timestamp(Instant.parse("2026-03-25T10:00:01Z"))
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

        return TestRun.builder()
                .id("run-1")
                .name("Test Run")
                .startTime(Instant.now())
                .endTime(Instant.now())
                .duration(1000)
                .status(testCase.getStatus())
                .suites(Collections.singletonList(suite))
                .build();
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

    private String extractScriptContent(String content) {
        StringBuilder scriptContent = new StringBuilder();
        int searchStart = 0;

        while (true) {
            int scriptStart = content.indexOf("<script", searchStart);
            if (scriptStart < 0) {
                return scriptContent.toString();
            }

            int scriptOpenEnd = content.indexOf('>', scriptStart);
            if (scriptOpenEnd < 0) {
                return scriptContent.toString();
            }

            int scriptEnd = content.indexOf("</script>", scriptOpenEnd);
            if (scriptEnd < 0) {
                return scriptContent.toString();
            }

            if (scriptContent.length() > 0) {
                scriptContent.append('\n');
            }
            scriptContent.append(content, scriptOpenEnd + 1, scriptEnd);
            searchStart = scriptEnd + "</script>".length();
        }
    }

    private int indexOfScriptStatementWithMarkers(String content, String anchor, String requiredMarker) {
        int anchorIndex = content.indexOf(anchor);
        while (anchorIndex >= 0) {
            int statementEnd = content.indexOf(';', anchorIndex);
            int searchEnd = statementEnd >= 0 ? statementEnd : content.length();
            if (content.substring(anchorIndex, searchEnd).contains(requiredMarker)) {
                return anchorIndex;
            }
            anchorIndex = content.indexOf(anchor, anchorIndex + anchor.length());
        }
        return -1;
    }

    private String extractStyleContent(String content) {
        StringBuilder styleContent = new StringBuilder();
        int searchStart = 0;

        while (true) {
            int styleStart = content.indexOf("<style", searchStart);
            if (styleStart < 0) {
                return styleContent.toString();
            }

            int styleOpenEnd = content.indexOf('>', styleStart);
            if (styleOpenEnd < 0) {
                return styleContent.toString();
            }

            int styleEnd = content.indexOf("</style>", styleOpenEnd);
            if (styleEnd < 0) {
                return styleContent.toString();
            }

            if (styleContent.length() > 0) {
                styleContent.append('\n');
            }
            styleContent.append(content, styleOpenEnd + 1, styleEnd);
            searchStart = styleEnd + "</style>".length();
        }
    }

    private String extractCssRuleDeclarations(String styles, String... selectors) {
        Pattern rulePattern = Pattern.compile("(?s)([^{}]+)\\{([^{}]*)\\}");
        Matcher matcher = rulePattern.matcher(styles);
        List<String> expectedSelectors = new ArrayList<>();
        for (String selector : selectors) {
            expectedSelectors.add(normalizeCssFragment(selector));
        }
        Collections.sort(expectedSelectors);

        while (matcher.find()) {
            List<String> actualSelectors = new ArrayList<>();
            for (String selector : matcher.group(1).split(",")) {
                String normalizedSelector = normalizeCssFragment(selector);
                if (!normalizedSelector.isEmpty()) {
                    actualSelectors.add(normalizedSelector);
                }
            }

            Collections.sort(actualSelectors);
            if (actualSelectors.equals(expectedSelectors)) {
                return normalizeCssFragment(matcher.group(2));
            }
        }

        return "";
    }

    private String normalizeCssFragment(String content) {
        return content.replaceAll("\\s+", " ").trim();
    }

    private String generateHtml(TestRun testRun) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generate(testRun, outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
