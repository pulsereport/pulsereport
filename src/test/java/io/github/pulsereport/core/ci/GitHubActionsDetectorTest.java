package io.github.pulsereport.core.ci;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class GitHubActionsDetectorTest {

    private static final String SHA = "0123456789abcdef0123456789abcdef01234567";

    private static Map<String, String> pushEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("GITHUB_ACTIONS", "true");
        env.put("GITHUB_SHA", SHA);
        env.put("GITHUB_REF_NAME", "main");
        env.put("GITHUB_HEAD_REF", "");
        env.put("GITHUB_RUN_NUMBER", "42");
        env.put("GITHUB_WORKFLOW", "CI");
        env.put("GITHUB_SERVER_URL", "https://github.com");
        env.put("GITHUB_REPOSITORY", "acme/shop");
        env.put("GITHUB_RUN_ID", "123456789");
        env.put("GITHUB_RUN_ATTEMPT", "1");
        return env;
    }

    @Test
    public void returnsEmptyOutsideGitHubActions() {
        Map<String, String> env = pushEnv();
        env.remove("GITHUB_ACTIONS");
        assertTrue(GitHubActionsDetector.detect(env).isEmpty());

        env.put("GITHUB_ACTIONS", "false");
        assertTrue(GitHubActionsDetector.detect(env).isEmpty());

        assertTrue(GitHubActionsDetector.detect(null).isEmpty());
    }

    @Test
    public void pushEventBuildsCommitAndRunUrls() {
        Map<String, String> result = GitHubActionsDetector.detect(pushEnv());

        assertEquals(SHA, result.get("git.commit"));
        assertEquals("https://github.com/acme/shop/commit/" + SHA, result.get("git.commitUrl"));
        assertEquals("main", result.get("git.branch"));
        assertEquals("Run #42 · CI", result.get("ci.run"));
        assertEquals("https://github.com/acme/shop/actions/runs/123456789", result.get("ci.runUrl"));
        assertFalse(result.containsKey("ci.jobUrl"));
        assertEquals(List.of("git.commit", "git.commitUrl", "git.branch", "ci.run", "ci.runUrl"),
                List.copyOf(result.keySet()));
    }

    @Test
    public void pullRequestUsesHeadRefForBranch() {
        Map<String, String> env = pushEnv();
        env.put("GITHUB_REF_NAME", "17/merge");
        env.put("GITHUB_HEAD_REF", "  feature/login  ");

        assertEquals("feature/login", GitHubActionsDetector.detect(env).get("git.branch"));
    }

    @Test
    public void appendsAttemptOnlyWhenGreaterThanOne() {
        Map<String, String> env = pushEnv();
        env.put("GITHUB_RUN_ATTEMPT", "3");
        assertEquals("https://github.com/acme/shop/actions/runs/123456789/attempts/3",
                GitHubActionsDetector.detect(env).get("ci.runUrl"));

        env.put("GITHUB_RUN_ATTEMPT", "abc");
        assertEquals("https://github.com/acme/shop/actions/runs/123456789",
                GitHubActionsDetector.detect(env).get("ci.runUrl"));

        env.remove("GITHUB_RUN_ATTEMPT");
        assertEquals("https://github.com/acme/shop/actions/runs/123456789",
                GitHubActionsDetector.detect(env).get("ci.runUrl"));
    }

    @Test
    public void supportsEnterpriseServerUrl() {
        Map<String, String> env = pushEnv();
        env.put("GITHUB_SERVER_URL", "https://ghe.example.com//");

        Map<String, String> result = GitHubActionsDetector.detect(env);

        assertEquals("https://ghe.example.com/acme/shop/commit/" + SHA, result.get("git.commitUrl"));
        assertEquals("https://ghe.example.com/acme/shop/actions/runs/123456789", result.get("ci.runUrl"));
    }

    @Test
    public void defaultsServerUrlWhenMissing() {
        Map<String, String> env = pushEnv();
        env.remove("GITHUB_SERVER_URL");

        assertEquals("https://github.com/acme/shop/actions/runs/123456789",
                GitHubActionsDetector.detect(env).get("ci.runUrl"));
    }

    @Test
    public void rejectsNonHttpServerUrl() {
        for (String server : List.of("javascript:alert(1)", "file:///etc", "ftp://github.com",
                "https://user:pw@github.com", "https://", "not a url")) {
            Map<String, String> env = pushEnv();
            env.put("GITHUB_SERVER_URL", server);

            Map<String, String> result = GitHubActionsDetector.detect(env);

            assertFalse(result.containsKey("git.commitUrl"), server);
            assertFalse(result.containsKey("ci.runUrl"), server);
            assertEquals(SHA, result.get("git.commit"), server);
            assertEquals("Run #42 · CI", result.get("ci.run"), server);
        }
    }

    @Test
    public void rejectsMalformedRepositoryOrSha() {
        for (String repo : List.of("acme", "acme/shop/extra", "../shop", "acme/..", "acme/sh op", "acme/<x>")) {
            Map<String, String> env = pushEnv();
            env.put("GITHUB_REPOSITORY", repo);

            Map<String, String> result = GitHubActionsDetector.detect(env);

            assertFalse(result.containsKey("git.commitUrl"), repo);
            assertFalse(result.containsKey("ci.runUrl"), repo);
        }

        for (String sha : List.of("abc123", SHA + "0", "z" + SHA.substring(1))) {
            Map<String, String> env = pushEnv();
            env.put("GITHUB_SHA", sha);

            Map<String, String> result = GitHubActionsDetector.detect(env);

            assertFalse(result.containsKey("git.commit"), sha);
            assertFalse(result.containsKey("git.commitUrl"), sha);
            assertEquals("https://github.com/acme/shop/actions/runs/123456789", result.get("ci.runUrl"));
        }

        Map<String, String> sha256 = pushEnv();
        sha256.put("GITHUB_SHA", (SHA + SHA).substring(0, 64).toUpperCase());
        assertEquals((SHA + SHA).substring(0, 64).toUpperCase(),
                GitHubActionsDetector.detect(sha256).get("git.commit"));

        Map<String, String> badRunId = pushEnv();
        badRunId.put("GITHUB_RUN_ID", "12a");
        assertFalse(GitHubActionsDetector.detect(badRunId).containsKey("ci.runUrl"));
    }

    @Test
    public void buildsJobUrlFromOptionalJobId() {
        Map<String, String> env = pushEnv();
        env.put("GITHUB_RUN_ATTEMPT", "2");
        env.put("PULSEREPORT_JOB_ID", " 987654 ");
        assertEquals("https://github.com/acme/shop/actions/runs/123456789/job/987654",
                GitHubActionsDetector.detect(env).get("ci.jobUrl"));

        env.put("PULSEREPORT_JOB_ID", "job-1");
        assertFalse(GitHubActionsDetector.detect(env).containsKey("ci.jobUrl"));

        env.put("PULSEREPORT_JOB_ID", "987654");
        env.put("GITHUB_RUN_ID", "");
        assertFalse(GitHubActionsDetector.detect(env).containsKey("ci.jobUrl"));
    }

    @Test
    public void buildsRunLabelFromAvailableParts() {
        Map<String, String> env = pushEnv();
        env.remove("GITHUB_WORKFLOW");
        assertEquals("Run #42", GitHubActionsDetector.detect(env).get("ci.run"));

        env.put("GITHUB_WORKFLOW", "Nightly");
        env.remove("GITHUB_RUN_NUMBER");
        assertEquals("Nightly", GitHubActionsDetector.detect(env).get("ci.run"));

        env.remove("GITHUB_WORKFLOW");
        assertFalse(GitHubActionsDetector.detect(env).containsKey("ci.run"));
    }

    @Test
    public void doesNotOverrideUserProvidedKeys() {
        Map<String, String> userEnv = Map.of("git.commit", "user-sha", "browser", "Chrome");

        Map<String, String> merged = GitHubActionsDetector.mergeInto(userEnv, GitHubActionsDetector.detect(pushEnv()));

        assertEquals("user-sha", merged.get("git.commit"));
        assertEquals("Chrome", merged.get("browser"));
        assertEquals("https://github.com/acme/shop/actions/runs/123456789", merged.get("ci.runUrl"));
        assertEquals(2, userEnv.size());

        assertNull(GitHubActionsDetector.mergeInto(null, Map.of()));
        assertEquals(Map.of("browser", "Chrome"), GitHubActionsDetector.mergeInto(Map.of("browser", "Chrome"), null));
    }

    @Test
    public void skipsCommitUrlWhenUserProvidedDifferentCommit() {
        Map<String, String> userEnv = Map.of("git.commit", "user-sha");

        Map<String, String> merged = GitHubActionsDetector.mergeInto(userEnv, GitHubActionsDetector.detect(pushEnv()));

        assertEquals("user-sha", merged.get("git.commit"));
        assertFalse(merged.containsKey("git.commitUrl"));
        assertEquals("main", merged.get("git.branch"));
        assertEquals("https://github.com/acme/shop/actions/runs/123456789", merged.get("ci.runUrl"));
        assertEquals(1, userEnv.size());
    }

    @Test
    public void keepsCommitUrlWhenUserCommitMatchesDetected() {
        Map<String, String> userEnv = Map.of("git.commit", SHA.toUpperCase());

        Map<String, String> merged = GitHubActionsDetector.mergeInto(userEnv, GitHubActionsDetector.detect(pushEnv()));

        assertEquals(SHA.toUpperCase(), merged.get("git.commit"));
        assertEquals("https://github.com/acme/shop/commit/" + SHA, merged.get("git.commitUrl"));
    }
}
