package io.github.pulsereport.outputs.html;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests that collapse containers in html-report.ftl establish a containing
 * block for absolutely-positioned descendants.
 *
 * <p>Regression guard for a scroll-overflow bug: BDD step icons contain
 * {@code .sr-only} spans with {@code position: absolute}. Their ancestor
 * collapse containers ({@code .test-case-body} and similar) use
 * {@code max-height: 0; overflow: hidden} but were {@code position: static}.
 * Per CSS 2.1 §11.1.1, an absolutely-positioned descendant is NOT clipped by
 * {@code overflow: hidden} on ancestors that are not its containing block, so
 * collapsed {@code .sr-only} spans laid out at phantom static positions and
 * inflated {@code documentElement.scrollHeight} past the footer. Adding
 * {@code position: relative} to each collapse container makes it the
 * containing block so {@code overflow: hidden} clips the abspos descendants.
 */
class OverflowClipContainmentTest {

    private static final Pattern TEST_CASE_BODY_RULE =
            Pattern.compile("\\.test-case-body\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern BDD_STEP_ARTIFACTS_BODY_RULE =
            Pattern.compile("\\.bdd-step-artifacts-body\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern API_CALL_BODY_RULE =
            Pattern.compile("\\.api-call-body\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern ARTIFACT_CONTENT_RULE =
            Pattern.compile("\\.artifact-content\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern CODE_WRAPPER_RULE =
            Pattern.compile("\\.code-wrapper\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern COPY_BTN_RULE =
            Pattern.compile("(?m)^\\s*\\.copy-btn\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static final Pattern SR_ONLY_RULE =
            Pattern.compile("\\.sr-only\\s*\\{([^}]*)\\}", Pattern.DOTALL);

    private static String testCaseBodyRule;
    private static String bddStepArtifactsBodyRule;
    private static String apiCallBodyRule;
    private static String artifactContentRule;
    private static String codeWrapperRule;
    private static String copyBtnRule;
    private static String srOnlyRule;

    @BeforeAll
    static void loadTemplate() throws IOException {
        String template;
        try (InputStream in = OverflowClipContainmentTest.class
                .getResourceAsStream("/templates/html-report.ftl")) {
            assertNotNull(in, "templates/html-report.ftl must be on the classpath");
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        testCaseBodyRule = extractRule(TEST_CASE_BODY_RULE, template, ".test-case-body");
        bddStepArtifactsBodyRule = extractRule(BDD_STEP_ARTIFACTS_BODY_RULE, template,
                ".bdd-step-artifacts-body");
        apiCallBodyRule = extractRule(API_CALL_BODY_RULE, template, ".api-call-body");
        artifactContentRule = extractRule(ARTIFACT_CONTENT_RULE, template, ".artifact-content");
        codeWrapperRule = extractRule(CODE_WRAPPER_RULE, template, ".code-wrapper");
        copyBtnRule = extractRule(COPY_BTN_RULE, template, ".copy-btn");
        srOnlyRule = extractRule(SR_ONLY_RULE, template, ".sr-only");
    }

    private static String extractRule(Pattern pattern, String template, String selector) {
        Matcher matcher = pattern.matcher(template);
        assertTrue(matcher.find(), "template must contain a " + selector + " rule");
        return matcher.group(1);
    }

    @Test
    void testCaseBodyIsContainingBlockForAbsposDescendants() {
        assertTrue(testCaseBodyRule.contains("position: relative"),
                ".test-case-body must use position: relative so overflow: hidden clips "
                        + "absolutely-positioned descendants (THE fix)");
        assertTrue(testCaseBodyRule.contains("overflow: hidden"),
                ".test-case-body must keep overflow: hidden (the clipping mechanism)");
    }

    @Test
    void bddStepArtifactsBodyIsContainingBlockForAbsposDescendants() {
        assertTrue(bddStepArtifactsBodyRule.contains("position: relative"),
                ".bdd-step-artifacts-body must use position: relative (defense-in-depth)");
    }

    @Test
    void apiCallBodyIsContainingBlockForAbsposDescendants() {
        assertTrue(apiCallBodyRule.contains("position: relative"),
                ".api-call-body must use position: relative (defense-in-depth)");
    }

    @Test
    void artifactContentIsContainingBlockForAbsposDescendants() {
        assertTrue(artifactContentRule.contains("position: relative"),
                ".artifact-content must use position: relative (defense-in-depth)");
    }

    @Test
    void copyButtonSafetyAssumptionStillHolds() {
        assertTrue(copyBtnRule.contains("position: absolute"),
                ".copy-btn must still exist and use position: absolute");
        assertTrue(codeWrapperRule.contains("position: relative"),
                ".code-wrapper must still use position: relative so .copy-btn stays scoped "
                        + "to the nearer positioned ancestor");
    }

    @Test
    void srOnlyStillUsesAbsolutePosition() {
        assertTrue(srOnlyRule.contains("position: absolute"),
                ".sr-only must still use position: absolute (documents the mechanism "
                        + "being contained by this fix)");
    }
}
