package io.github.pulsereport.outputs.html;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestSuite;

/**
 * Verifies the FOUC-free dark theme hook (Phase 1): dark overrides live on
 * {@code :root[data-theme='dark']} so the theme can be applied before
 * {@code <body>} parses, {@code color-scheme} is declared for UA chrome, and a
 * {@code prefers-color-scheme} media query provides a no-JS fallback.
 */
class HtmlReportThemeTemplateTest {

    private HtmlReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new HtmlReportGenerator();
    }

    @Test
    void shouldUseDataThemeAttributeOnRootForDarkOverrides() throws IOException {
        String html = generateHtml(createMinimalTestRun());
        String styles = extractStyleContent(html);

        assertAll(
                () -> assertFalse(styles.contains("body.dark"),
                        "No CSS selector should remain scoped to body.dark"),
                () -> assertTrue(styles.contains(":root[data-theme='dark']"),
                        "Dark overrides should be scoped to :root[data-theme='dark']"),
                () -> assertTrue(html.contains("html { background: var(--bg); }")
                                || normalizeCssFragment(html).contains("html { background: var(--bg); }"),
                        "html should paint var(--bg) so the first frame has the correct background"));
    }

    @Test
    void shouldDeclareColorSchemeMetaAndProperty() throws IOException {
        String html = generateHtml(createMinimalTestRun());
        String styles = extractStyleContent(html);

        int headOpenEnd = html.indexOf("<head>") + "<head>".length();
        String firstHeadElement = html.substring(headOpenEnd).trim();

        assertAll(
                () -> assertTrue(firstHeadElement.startsWith("<meta name=\"color-scheme\" content=\"light dark\">"),
                        "<meta name=\"color-scheme\" content=\"light dark\"> should be the first element in <head>"),
                () -> assertTrue(styles.contains("color-scheme: light dark"),
                        ":root should declare color-scheme: light dark"),
                () -> assertTrue(styles.contains("color-scheme: dark"),
                        ":root[data-theme='dark'] should declare color-scheme: dark"));
    }

    @Test
    void shouldIncludeNoJsPrefersColorSchemeFallback() throws IOException {
        String html = generateHtml(createMinimalTestRun());
        String styles = extractStyleContent(html);

        int mediaIndex = styles.indexOf("@media (prefers-color-scheme: dark)");
        assertTrue(mediaIndex >= 0,
                "A @media (prefers-color-scheme: dark) block should exist as a no-JS fallback");

        String mediaBlock = extractBlock(styles, mediaIndex);
        assertAll(
                () -> assertTrue(mediaBlock.contains(":root:not([data-theme])"),
                        "The no-JS fallback should target :root:not([data-theme]) "
                                + "so it never conflicts with a stored preference"),
                () -> assertTrue(mediaBlock.contains("--bg: #212529"),
                        "The no-JS fallback should carry the dark background override"),
                () -> assertTrue(mediaBlock.contains("--surface: #2b3035"),
                        "The no-JS fallback should carry the dark surface override"),
                () -> assertTrue(mediaBlock.contains("--text-primary: #f8f9fa"),
                        "The no-JS fallback should carry the dark text override"));
    }

    @Test
    void shouldContainBlockingThemeScriptBeforeMainScript() throws IOException {
        String html = generateHtml(createMinimalTestRun());

        int styleEnd = html.indexOf("</style>");
        assertTrue(styleEnd >= 0, "The template should contain a </style> tag");
        int mainScriptStart = html.indexOf("function openLightbox", styleEnd);
        assertTrue(mainScriptStart >= 0, "The main script should follow the styles");

        String headScript = html.substring(styleEnd, mainScriptStart);

        assertAll(
                () -> assertTrue(headScript.contains("document.documentElement.setAttribute('data-theme'"),
                        "The blocking head script should set data-theme on document.documentElement"),
                () -> assertTrue(headScript.contains("(function()") || headScript.contains("(function ()"),
                        "The blocking head script should be a small IIFE"),
                () -> assertTrue(headScript.contains("matchMedia('(prefers-color-scheme: dark)')"),
                        "The blocking head script should fall back to the OS color scheme"),
                () -> assertFalse(headScript.contains("DOMContentLoaded"),
                        "The blocking head script must not wait for DOMContentLoaded"),
                () -> assertFalse(headScript.contains("addEventListener('load'"),
                        "The blocking head script must not wait for the load event"));
    }

    @Test
    void shouldGuardLocalStorageAccessWithTryCatch() throws IOException {
        String html = generateHtml(createMinimalTestRun());

        int styleEnd = html.indexOf("</style>");
        assertTrue(styleEnd >= 0, "The template should contain a </style> tag");
        int mainScriptStart = html.indexOf("function openLightbox", styleEnd);
        assertTrue(mainScriptStart >= 0, "The main script should follow the styles");

        String headScript = html.substring(styleEnd, mainScriptStart);

        int tryIndex = headScript.indexOf("try");
        int storageIndex = headScript.indexOf("localStorage.getItem('pulse-report-theme')");
        int catchIndex = storageIndex >= 0 ? headScript.indexOf("catch", storageIndex) : -1;

        assertAll(
                () -> assertTrue(storageIndex >= 0,
                        "The blocking head script should read the stored theme preference"),
                () -> assertTrue(tryIndex >= 0 && tryIndex < storageIndex,
                        "The localStorage read should be preceded by try "
                                + "(file:// origins can throw SecurityError)"),
                () -> assertTrue(catchIndex > storageIndex,
                        "The localStorage read should be followed by catch "
                                + "so a blocking-script throw cannot abort theme application"));
    }

    @Test
    void shouldNotReResolveThemeInDomContentLoaded() throws IOException {
        String html = generateHtml(createMinimalTestRun());

        int domReadyIndex = html.indexOf("document.addEventListener('DOMContentLoaded'");
        assertTrue(domReadyIndex >= 0, "The template should keep a DOMContentLoaded init");

        String initBlock = extractBlock(html, domReadyIndex);
        int changeListenerIndex = initBlock.indexOf("addEventListener('change'");
        assertTrue(changeListenerIndex >= 0,
                "The prefers-color-scheme change listener should remain in the init");
        String initBody = initBlock.substring(0, changeListenerIndex);

        assertAll(
                () -> assertFalse(initBody.contains("applyTheme("),
                        "The init should not re-apply the theme from scratch "
                                + "before the change listener"),
                () -> assertFalse(initBody.contains("storedTheme === 'dark'"),
                        "The init should not re-resolve the stored theme preference"),
                () -> assertTrue(initBody.contains("updateThemeIcon()"),
                        "The init should only sync the toggle icon from the "
                                + "already-applied data-theme attribute"));
    }

    private TestRun createMinimalTestRun() {
        TestCase testCase = TestCase.builder()
                .id("tc-theme")
                .name("theme scenario")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-theme")
                .name("Theme Suite")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .testCases(Collections.singletonList(testCase))
                .build();

        return TestRun.builder()
                .id("run-theme")
                .name("Theme Run")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .suites(Collections.singletonList(suite))
                .build();
    }

    private String generateHtml(TestRun testRun) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generator.generate(testRun, outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
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

    private String extractBlock(String content, int openingBraceOwnerIndex) {
        int blockStart = content.indexOf('{', openingBraceOwnerIndex);
        if (blockStart < 0) {
            return "";
        }

        int depth = 0;
        for (int i = blockStart; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(blockStart, i + 1);
                }
            }
        }
        return "";
    }

    private String normalizeCssFragment(String content) {
        return content.replaceAll("\\s+", " ").trim();
    }
}
