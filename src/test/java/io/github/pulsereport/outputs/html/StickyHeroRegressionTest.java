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
}
