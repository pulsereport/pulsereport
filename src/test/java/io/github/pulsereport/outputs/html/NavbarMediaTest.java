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
 * Tests Phase 2 of the sticky-navbar redesign: the slide transition is gated
 * behind prefers-reduced-motion: no-preference, the fixed navbar is hidden
 * when printing, and the navbar has responsive rules inside the existing
 * 900px and 640px breakpoints.
 */
class NavbarMediaTest {

    private static String template;

    @BeforeAll
    static void loadTemplate() throws IOException {
        try (InputStream in = NavbarMediaTest.class
                .getResourceAsStream("/templates/html-report.ftl")) {
            assertNotNull(in, "templates/html-report.ftl must be on the classpath");
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Returns [start, end) of the first @media block whose prelude is "@media " + condition. */
    private static int[] mediaBlockSpan(String condition) {
        int start = template.indexOf("@media " + condition);
        assertTrue(start >= 0, "template must contain an @media " + condition + " block");
        int open = template.indexOf('{', start);
        assertTrue(open > start, "@media " + condition + " must open a block");
        int depth = 1;
        int i = open + 1;
        while (i < template.length() && depth > 0) {
            char c = template.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            i++;
        }
        assertEquals(0, depth, "@media " + condition + " block must be closed");
        return new int[] {start, i};
    }

    /** Returns the inner CSS of the first @media block matching the condition. */
    private static String mediaBlock(String condition) {
        int[] span = mediaBlockSpan(condition);
        int open = template.indexOf('{', span[0]);
        return template.substring(open + 1, span[1] - 1);
    }

    /** Returns the body of the first rule for the exact selector within the given CSS scope. */
    private static String ruleBody(String scope, String selector) {
        Matcher matcher = Pattern.compile(Pattern.quote(selector) + "\\s*\\{([^}]*)\\}").matcher(scope);
        assertTrue(matcher.find(), "a rule for " + selector + " must exist in the scope");
        return matcher.group(1);
    }

    @Test
    void navbarTransitionLivesInsideReducedMotionMediaQuery() {
        String motion = mediaBlock("(prefers-reduced-motion: no-preference)");
        assertTrue(motion.contains(".report-navbar"),
                "the no-preference media query must target .report-navbar");
        assertTrue(motion.contains("transition: transform 0.3s cubic-bezier(.4,0,.2,1), visibility 0.3s;"),
                "the navbar slide transition must be declared inside the no-preference media query");
    }

    @Test
    void baseNavbarRuleHasNoTransition() {
        int[] span = mediaBlockSpan("(prefers-reduced-motion: no-preference)");
        String outside = template.substring(0, span[0]) + template.substring(span[1]);
        Matcher matcher = Pattern.compile("\\.report-navbar\\s*\\{([^}]*)\\}").matcher(outside);
        assertTrue(matcher.find(), "a base .report-navbar rule must exist outside the media query");
        do {
            assertFalse(matcher.group(1).contains("transition"),
                    ".report-navbar must not declare a transition outside prefers-reduced-motion: no-preference");
        } while (matcher.find());
    }

    @Test
    void printMediaQueryHidesNavbar() {
        String print = mediaBlock("print");
        String navbarRule = ruleBody(print, ".report-navbar");
        assertTrue(navbarRule.contains("display: none"),
                "@media print must set .report-navbar to display: none");
    }

    @Test
    void smallBreakpointKeepsNavbarStatusPillVisible() {
        String block = mediaBlock("screen and (max-width: 640px)");
        assertFalse(Pattern.compile(Pattern.quote(".report-navbar .hero-status-pill")
                        + "\\s*\\{[^}]*display:\\s*none").matcher(block).find(),
                "the 640px breakpoint must keep the navbar status pill visible");
    }

    @Test
    void smallBreakpointHidesNavbarStats() {
        String block = mediaBlock("screen and (max-width: 640px)");
        assertTrue(ruleBody(block, ".report-navbar-stats").contains("display: none"),
                "the 640px breakpoint must hide .report-navbar-stats entirely");
    }
}
