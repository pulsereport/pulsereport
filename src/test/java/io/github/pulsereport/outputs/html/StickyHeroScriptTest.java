package io.github.pulsereport.outputs.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests that the html-report.ftl template uses a guarded IntersectionObserver
 * to slide the slim fixed navbar in once the hero has scrolled out of view
 * (Phase 1 of the sticky-navbar redesign). The hero never changes size, so
 * the boundary is stable: no hysteresis, no scroll listeners.
 */
class StickyHeroScriptTest {

    private static final Pattern THEME_TOGGLE_ID = Pattern.compile("id=\"themeToggle\"");

    private static String template;

    @BeforeAll
    static void loadTemplate() throws IOException {
        try (InputStream in = StickyHeroScriptTest.class
                .getResourceAsStream("/templates/html-report.ftl")) {
            assertNotNull(in, "templates/html-report.ftl must be on the classpath");
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void usesIntersectionObserver() {
        assertTrue(template.contains("IntersectionObserver"),
                "template must use IntersectionObserver to detect the hero leaving the viewport");
    }

    @Test
    void observerUsesZeroThreshold() {
        assertTrue(template.contains("threshold: [0]"),
                "observer must use threshold: [0] so it fires when the hero fully leaves the viewport");
    }

    @Test
    void observerIsFeatureGuarded() {
        assertTrue(template.contains("'IntersectionObserver' in window"),
                "observer usage must be guarded with 'IntersectionObserver' in window");
    }

    @Test
    void togglesNavbarVisibleClass() {
        assertTrue(template.contains("report-navbar-visible"),
                "template must toggle the report-navbar-visible class on the navbar");
    }

    @Test
    void observerReadsBoundingClientRectBottom() {
        assertTrue(template.contains("boundingClientRect.bottom"),
                "observer callback must decide visibility from boundingClientRect.bottom");
    }

    @Test
    void noScrollListener() {
        assertFalse(template.contains("addEventListener('scroll'"),
                "template must not register a scroll listener");
    }

    @Test
    void noStuckClass() {
        assertFalse(template.contains("report-hero-stuck"),
                "template must not reference report-hero-stuck anywhere");
    }

    @Test
    void noOffsetTopGeometryReads() {
        assertFalse(template.contains("offsetTop"),
                "template must not read offsetTop (no geometry reads beyond boundingClientRect.bottom)");
    }

    @Test
    void navbarMarkupPresent() {
        assertTrue(template.contains("<nav class=\"report-navbar\""),
                "template must contain the <nav class=\"report-navbar\" element");
        assertTrue(template.contains("id=\"reportNavbar\""),
                "navbar must carry id=\"reportNavbar\"");
    }

    @Test
    void navbarStartsAriaHidden() {
        int navIdx = template.indexOf("<nav class=\"report-navbar\"");
        assertTrue(navIdx >= 0, "template must contain the navbar element");
        int closeIdx = template.indexOf('>', navIdx);
        assertTrue(closeIdx > navIdx, "navbar opening tag must be well-formed");
        String openingTag = template.substring(navIdx, closeIdx);
        assertTrue(openingTag.contains("aria-hidden=\"true\""),
                "navbar must start with aria-hidden=\"true\" (hidden off-screen by default)");
    }

    @Test
    void exactlyOneThemeToggleId() {
        int count = 0;
        Matcher matcher = THEME_TOGGLE_ID.matcher(template);
        while (matcher.find()) {
            count++;
        }
        assertEquals(1, count,
                "template must contain exactly one id=\"themeToggle\" (navbar button is class-only)");
    }
}
