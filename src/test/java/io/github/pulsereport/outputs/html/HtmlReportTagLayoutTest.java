package io.github.pulsereport.outputs.html;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Verifies the minimal inline-flow tags/stats layout (Phase 1 rewrite):
 * collapsed = single-line truncation at a 40ch budget, expanded = plain inline
 * flow with no flex machinery and no JS measurement.
 */
class HtmlReportTagLayoutTest {

    private HtmlReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new HtmlReportGenerator();
    }

    @Test
    void collapsedTagsUseEllipsisAt40ch() throws IOException {
        String html = generateHtml(createTestRunWithManyTags(20));
        String styles = extractStyleContent(html);

        // Shared base rule: truncated inline-block span at both suite and
        // scenario level when collapsed
        String base = extractCssRuleDeclarations(styles, ".test-case-tags");
        assertAll(
                () -> assertTrue(base.contains("display: inline-block"),
                        "Collapsed tags should be an inline-block span"),
                () -> assertTrue(base.contains("max-width: 40ch"),
                        "Collapsed tags should truncate at a 40ch character budget"),
                () -> assertTrue(base.contains("white-space: nowrap"),
                        "Collapsed tags should render on a single line"),
                () -> assertTrue(base.contains("overflow: hidden"),
                        "Collapsed tags should hide overflow"),
                () -> assertTrue(base.contains("text-overflow: ellipsis"),
                        "Collapsed tags should render a real ellipsis at the cut point"),
                () -> assertTrue(base.contains("color: var(--text-muted)"),
                        "Collapsed tags should be muted so the ellipsis is muted"));

        // Scenario-level tags keep their separation from the name in all states
        String scenarioTags = extractCssRuleDeclarations(styles, ".test-case-name .test-case-tags");
        assertTrue(scenarioTags.contains("margin-left: 6px"),
                "Scenario tags should be separated from the name by a small margin");
    }

    @Test
    void expandedTagsFlowInline() throws IOException {
        String html = generateHtml(createTestRunWithManyTags(20));
        String styles = extractStyleContent(html);

        // Expanded state: one rule covering both levels, plain inline flow
        String expanded = extractCssRuleDeclarations(styles,
                ".test-suite-header:not(.collapsed) .test-case-tags",
                ".test-case-header.expanded .test-case-name .test-case-tags");
        assertAll(
                () -> assertTrue(expanded.contains("max-width: none"),
                        "Expanded tags should lift the character budget"),
                () -> assertTrue(expanded.contains("overflow: visible"),
                        "Expanded tags should not clip"),
                () -> assertTrue(expanded.contains("white-space: normal"),
                        "Expanded tags should wrap naturally"),
                () -> assertTrue(expanded.contains("text-overflow: clip"),
                        "Expanded tags should not render an ellipsis"),
                () -> assertFalse(expanded.contains("display: flex"),
                        "Expanded tags must not switch to flex"),
                () -> assertFalse(expanded.contains("display: block"),
                        "Expanded tags must not switch to block"),
                () -> assertFalse(expanded.contains("flex:"),
                        "Expanded tags must not carry a flex basis"));
    }

    @Test
    void suiteStatsRowWrapsWhenExpanded() throws IOException {
        String html = generateHtml(createTestRunWithManyTags(20));
        String styles = extractStyleContent(html);

        // Collapsed: the stats row is a clipped single line
        String stats = extractCssRuleDeclarations(styles, ".test-suite-stats");
        assertAll(
                () -> assertTrue(stats.contains("white-space: nowrap"),
                        "Collapsed suite stats row should stay on a single line"),
                () -> assertTrue(stats.contains("overflow: hidden"),
                        "Collapsed suite stats row should clip overflow"),
                () -> assertFalse(stats.contains("inline-flex"),
                        "Suite stats row must not be a flex container"),
                () -> assertFalse(stats.contains("flex-wrap"),
                        "Suite stats row must not wrap via flex"));

        // Expanded: the row flows as a normal paragraph so ratio/duration land
        // immediately after the last tag
        String expanded = extractCssRuleDeclarations(styles,
                ".test-suite-header:not(.collapsed) .test-suite-stats");
        assertAll(
                () -> assertTrue(expanded.contains("white-space: normal"),
                        "Expanded suite stats row should allow wrapping"),
                () -> assertTrue(expanded.contains("overflow: visible"),
                        "Expanded suite stats row should not clip"));
    }

    @Test
    void noLegacyLayoutCodeRemains() throws IOException {
        String html = generateHtml(createTestRunWithManyTags(20));

        String[] legacy = {
                "tags-truncated", "markTruncatedTags", "tag-more", "tag-hidden",
                "Show less", "MAX_VISIBLE_TAGS", "initTagTruncation", "toggleTags",
                "0 0 100%"
        };
        for (String fragment : legacy) {
            assertFalse(html.contains(fragment),
                    "Rendered report should not contain legacy layout fragment: " + fragment);
        }
    }

    @Test
    void tagsContainerHasFullListInTitle() throws IOException {
        String html = generateHtml(createTestRunWithManyTags(20));

        Pattern tagsPattern = Pattern.compile("<span class=\"test-case-tags\" title=\"([^\"]*)\">");
        Matcher matcher = tagsPattern.matcher(html);
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            titles.add(matcher.group(1));
        }

        assertTrue(titles.size() >= 2,
                "Both the suite-level and scenario-level tags containers should carry a title attribute");

        for (String title : titles) {
            assertAll(
                    () -> assertTrue(title.contains("@tag1"),
                            "title should contain the first tag"),
                    () -> assertTrue(title.contains("@tag10"),
                            "title should contain a middle tag"),
                    () -> assertTrue(title.contains("@tag20"),
                            "title should contain the last tag"));
        }
    }

    @Test
    void statsSpansAreSiblingsAfterTags() throws IOException {
        String html = generateHtml(createTestRunWithManyTags(20));

        // Stats must remain siblings AFTER the tags container, not nested inside it
        Pattern statsBlock = Pattern.compile(
                "(?s)<div class=\"test-suite-stats\">.*?</div>");
        Matcher matcher = statsBlock.matcher(html);
        assertTrue(matcher.find(), "Rendered HTML should contain a .test-suite-stats block");
        String stats = matcher.group(0);

        int tagsOpen = stats.indexOf("<span class=\"test-case-tags\"");
        int tagsClose = stats.indexOf("</span>", tagsOpen);
        int ratio = stats.indexOf("suite-stats-ratio");
        int duration = stats.indexOf("suite-stats-duration");

        assertAll(
                () -> assertTrue(tagsOpen >= 0, "Suite stats should contain the tags container"),
                () -> assertTrue(ratio > tagsClose,
                        "suite-stats-ratio must appear AFTER the tags container closes (sibling, not nested)"),
                () -> assertTrue(duration > ratio,
                        "suite-stats-duration must follow suite-stats-ratio"));
    }

    @Test
    void noTrailingSpaceAfterLastTag() throws IOException {
        String html = generateHtml(createTestRunWithManyTags(20));

        // Suite level: the last tag pill must be directly followed by the tags-span
        // close and then the separator dot, with no space in between
        assertAll(
                () -> assertTrue(html.contains("</span></span><span class=\"suite-stats-sep\">"),
                        "Suite tags span should close immediately after the last tag pill, "
                                + "directly before the separator dot"),
                () -> assertFalse(html.contains("</span> </span><span class=\"suite-stats-sep\">"),
                        "Suite tags span must not contain a trailing space before the separator dot"));

        // Both levels: every tags span's inner content must end with the last
        // pill's </span>, never with a space
        Pattern tagsSpan = Pattern.compile(
                "<span class=\"test-case-tags\"[^>]*>((?:<span class=\"tag-label\">[^<]*</span> ?)*)</span>");
        Matcher matcher = tagsSpan.matcher(html);
        int count = 0;
        while (matcher.find()) {
            count++;
            String inner = matcher.group(1);
            assertFalse(inner.endsWith(" "),
                    "Tags span content must not end with a trailing space, but was: [" + inner + "]");
            assertTrue(inner.endsWith("</span>"),
                    "Tags span content should end with the last tag pill's closing tag");
        }
        assertTrue(count >= 2,
                "Both the suite-level and scenario-level tags spans should be rendered");
    }

    private TestRun createTestRunWithManyTags(int tagCount) {
        List<String> manyTags = new ArrayList<>();
        for (int i = 1; i <= tagCount; i++) {
            manyTags.add("@tag" + i);
        }

        TestCase testCase = TestCase.builder()
                .id("tc-tags")
                .name("scenario with many tags")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .tags(manyTags)
                .build();

        TestSuite suite = TestSuite.builder()
                .id("suite-tags")
                .name("Tagged Suite")
                .startTime(Instant.parse("2026-02-16T10:00:00Z"))
                .endTime(Instant.parse("2026-02-16T10:00:01Z"))
                .duration(1000)
                .status(TestStatus.PASSED)
                .tags(manyTags)
                .testCases(Collections.singletonList(testCase))
                .build();

        return TestRun.builder()
                .id("run-tags")
                .name("Tag Layout Run")
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

    private String extractCssRuleDeclarations(String styles, String... selectors) {
        String uncommented = styles.replaceAll("(?s)/\\*.*?\\*/", "");
        Pattern rulePattern = Pattern.compile("(?s)([^{}]+)\\{([^{}]*)\\}");
        Matcher matcher = rulePattern.matcher(uncommented);
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
}
