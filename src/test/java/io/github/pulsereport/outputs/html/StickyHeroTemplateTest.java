package io.github.pulsereport.outputs.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests that the html-report.ftl template renders a slim fixed navbar
 * (Phase 1 of the sticky-navbar redesign): the hero is a plain in-flow
 * element and a separate fixed navbar slides in once the hero scrolls away.
 */
class StickyHeroTemplateTest {

    private static final Pattern REPORT_HERO_RULE =
            Pattern.compile("\\.report-hero\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern REPORT_NAVBAR_RULE =
            Pattern.compile("\\.report-navbar\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern REPORT_NAVBAR_VISIBLE_RULE =
            Pattern.compile("\\.report-navbar-visible\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern REPORT_NAVBAR_INNER_RULE =
            Pattern.compile("\\.report-navbar-inner\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern REPORT_NAVBAR_STATS_RULE =
            Pattern.compile("\\.report-navbar-stats\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern NAVBAR_HEADER_ACTIONS_RULE =
            Pattern.compile("\\.report-navbar \\.header-actions\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern NAVBAR_BRAND_TITLE_RULE =
            Pattern.compile("\\.report-navbar \\.brand-title\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern NAVBAR_STATUS_PILL_RULE =
            Pattern.compile("\\.report-navbar \\.hero-status-pill\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern BASE_BRAND_TITLE_RULE =
            Pattern.compile("(?m)^\\s*\\.brand-title\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern BASE_STATUS_PILL_RULE =
            Pattern.compile("(?m)^\\s*\\.hero-status-pill\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static String template;
    private static String reportHeroRule;
    private static String reportNavbarRule;
    private static String reportNavbarVisibleRule;
    private static String reportNavbarInnerRule;
    private static String reportNavbarStatsRule;

    @BeforeAll
    static void loadTemplate() throws IOException {
        try (InputStream in = StickyHeroTemplateTest.class
                .getResourceAsStream("/templates/html-report.ftl")) {
            assertNotNull(in, "templates/html-report.ftl must be on the classpath");
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        Matcher heroMatcher = REPORT_HERO_RULE.matcher(template);
        assertTrue(heroMatcher.find(), "template must contain a .report-hero rule");
        reportHeroRule = heroMatcher.group(1);

        Matcher navbarMatcher = REPORT_NAVBAR_RULE.matcher(template);
        assertTrue(navbarMatcher.find(), "template must contain a .report-navbar rule");
        reportNavbarRule = navbarMatcher.group(1);

        Matcher visibleMatcher = REPORT_NAVBAR_VISIBLE_RULE.matcher(template);
        assertTrue(visibleMatcher.find(), "template must contain a .report-navbar-visible rule");
        reportNavbarVisibleRule = visibleMatcher.group(1);

        Matcher innerMatcher = REPORT_NAVBAR_INNER_RULE.matcher(template);
        assertTrue(innerMatcher.find(), "template must contain a .report-navbar-inner rule");
        reportNavbarInnerRule = innerMatcher.group(1);

        Matcher statsMatcher = REPORT_NAVBAR_STATS_RULE.matcher(template);
        assertTrue(statsMatcher.find(), "template must contain a .report-navbar-stats rule");
        reportNavbarStatsRule = statsMatcher.group(1);
    }

    @Test
    void reportNavbarUsesFixedPosition() {
        assertTrue(reportNavbarRule.contains("position: fixed"),
                ".report-navbar must use position: fixed");
    }

    @Test
    void reportNavbarSpansFullViewportWidth() {
        assertTrue(reportNavbarRule.contains("left: 0"),
                ".report-navbar must declare left: 0");
        assertTrue(reportNavbarRule.contains("right: 0"),
                ".report-navbar must declare right: 0");
    }

    @Test
    void reportNavbarHiddenOffScreenByDefault() {
        assertTrue(reportNavbarRule.contains("transform: translateY(-100%)"),
                ".report-navbar must be hidden off-screen via transform: translateY(-100%)");
        assertTrue(reportNavbarRule.contains("visibility: hidden"),
                ".report-navbar must be visibility: hidden by default");
    }

    @Test
    void reportNavbarDeclaresExplicitStackingOrder() {
        assertTrue(reportNavbarRule.contains("z-index: 300"),
                ".report-navbar must declare z-index: 300");
    }

    @Test
    void reportNavbarVisibleSlidesIn() {
        assertTrue(reportNavbarVisibleRule.contains("transform: translateY(0)"),
                ".report-navbar-visible must slide the navbar in via transform: translateY(0)");
    }

    @Test
    void reportHeroIsNotSticky() {
        assertFalse(reportHeroRule.contains("position: sticky"),
                ".report-hero must not use position: sticky");
        assertFalse(reportHeroRule.contains("position: -webkit-sticky"),
                ".report-hero must not use position: -webkit-sticky");
    }

    @Test
    void templateHasNoStuckClass() {
        assertFalse(template.contains("report-hero-stuck"),
                "template must not reference report-hero-stuck anywhere (CSS, JS, or markup)");
    }

    @Test
    void reportNavbarInnerSpansFullViewportWidth() {
        assertFalse(reportNavbarInnerRule.contains("max-width"),
                ".report-navbar-inner must not cap its width with max-width");
        assertFalse(reportNavbarInnerRule.contains("margin: 0 auto"),
                ".report-navbar-inner must not center itself with margin: 0 auto");
        assertTrue(reportNavbarInnerRule.contains("padding: 10px 28px"),
                ".report-navbar-inner must use the old pinned-hero padding of 10px 28px");
    }

    @Test
    void reportNavbarStatsHugTheBrand() {
        assertFalse(reportNavbarStatsRule.contains("flex: 1"),
                ".report-navbar-stats must not flex-grow; stats must sit beside the brand");
        assertTrue(reportNavbarStatsRule.contains("margin-left: 20px"),
                ".report-navbar-stats must keep margin-left: 20px");
    }

    @Test
    void navbarHeaderActionsStayFlushRight() {
        Matcher matcher = NAVBAR_HEADER_ACTIONS_RULE.matcher(template);
        while (matcher.find()) {
            assertFalse(matcher.group(1).contains("margin-left: 0"),
                    ".report-navbar .header-actions must not zero out margin-left;"
                            + " the base .header-actions margin-left: auto must push the actions flush right");
        }
    }

    @Test
    void navbarBrandTitleUsesCompactFontSize() {
        Matcher matcher = NAVBAR_BRAND_TITLE_RULE.matcher(template);
        assertTrue(matcher.find(), "template must contain a .report-navbar .brand-title rule");
        assertTrue(matcher.group(1).contains("font-size: 1.05rem"),
                ".report-navbar .brand-title must override the hero size with font-size: 1.05rem");
    }

    @Test
    void navbarStatusPillUsesCompactSizing() {
        Matcher matcher = NAVBAR_STATUS_PILL_RULE.matcher(template);
        boolean compactRuleFound = false;
        while (matcher.find()) {
            String body = matcher.group(1);
            if (body.contains("font-size: 0.75rem")
                    && body.contains("min-height: 28px")
                    && body.contains("height: 28px")) {
                compactRuleFound = true;
                break;
            }
        }
        assertTrue(compactRuleFound,
                "a .report-navbar .hero-status-pill rule must exist with font-size: 0.75rem,"
                        + " min-height: 28px and height: 28px (fixed height overrides the base 37px pill)");
    }

    @Test
    void baseBrandTitleKeepsHeroFontSize() {
        Matcher matcher = BASE_BRAND_TITLE_RULE.matcher(template);
        assertTrue(matcher.find(), "template must contain a base .brand-title rule");
        assertTrue(matcher.group(1).contains("font-size: 1.35rem"),
                "base .brand-title must keep font-size: 1.35rem for the hero");
    }

    @Test
    void baseStatusPillKeepsHeroHeight() {
        Matcher matcher = BASE_STATUS_PILL_RULE.matcher(template);
        assertTrue(matcher.find(), "template must contain a base .hero-status-pill rule");
        assertTrue(matcher.group(1).contains("height: 37px"),
                "base .hero-status-pill must keep height: 37px for the hero");
    }
}
