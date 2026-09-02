package io.github.pulsereport.outputs.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the sticky-navbar redesign (Phase 1):
 * legacy sentinel markup/CSS removal, removal of the old sticky-hero
 * stuck-class mechanism, and verification of the regenerated
 * testng-example report against the new architecture.
 */
class StickyHeroRegressionTest {

    private static final Path TESTNG_EXAMPLE_REPORT =
            Paths.get("examples", "testng-example", "target", "pulsereport", "test-report.html");

    private static String template;

    @BeforeAll
    static void loadTemplate() throws IOException {
        try (InputStream in = StickyHeroRegressionTest.class
                .getResourceAsStream("/templates/html-report.ftl")) {
            assertNotNull(in, "templates/html-report.ftl must be on the classpath");
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void noHeroSentinelReferences() {
        assertFalse(template.contains("heroSentinel"),
                "template must not reference the removed #heroSentinel element anywhere");
    }

    @Test
    void noHeroScrollSentinelClass() {
        assertFalse(template.contains("hero-scroll-sentinel"),
                "template must not contain the removed .hero-scroll-sentinel CSS class or markup");
    }

    @Test
    void noReportHeroStuckClass() {
        assertFalse(template.contains("report-hero-stuck"),
                "template must not reference the removed report-hero-stuck class anywhere (CSS, JS, or markup)");
    }

    @Test
    void generatedTestNgExampleReportHasNavbarWithoutSentinel() throws IOException {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(TESTNG_EXAMPLE_REPORT),
                "testng-example report not generated yet; run `mvn verify` in examples/testng-example");
        String html = Files.readString(TESTNG_EXAMPLE_REPORT, StandardCharsets.UTF_8);
        assertTrue(html.contains("<nav class=\"report-navbar\""),
                "regenerated testng-example report must contain the slim fixed navbar element");
        assertTrue(html.contains("report-navbar-visible"),
                "regenerated testng-example report must contain the report-navbar-visible toggle class");
        assertTrue(html.contains("threshold: [0]"),
                "regenerated testng-example report must use IntersectionObserver with threshold: [0]");
        assertFalse(html.contains("report-hero-stuck"),
                "regenerated testng-example report must not contain the removed report-hero-stuck class");
        assertFalse(html.contains("heroSentinel"),
                "regenerated testng-example report must not contain the removed heroSentinel markup");
        assertFalse(html.contains("hero-scroll-sentinel"),
                "regenerated testng-example report must not contain the removed hero-scroll-sentinel class");
        assertFalse(html.contains("position: sticky"),
                "regenerated testng-example report must not use position: sticky anywhere "
                        + "(hero is plain in-flow; template has no other sticky usage)");
    }

    @Test
    void generatedTestNgExampleReportReflectsPhase2MediaRules() throws IOException {
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(TESTNG_EXAMPLE_REPORT),
                "testng-example report not generated yet; run `mvn verify` in examples/testng-example");
        String html = Files.readString(TESTNG_EXAMPLE_REPORT, StandardCharsets.UTF_8);
        assertTrue(html.contains("@media (prefers-reduced-motion: no-preference)"),
                "regenerated testng-example report must gate the navbar slide transition behind "
                        + "prefers-reduced-motion: no-preference (Phase 2)");
        assertTrue(html.contains("@media print"),
                "regenerated testng-example report must hide the navbar in print media (Phase 2)");
    }
}
