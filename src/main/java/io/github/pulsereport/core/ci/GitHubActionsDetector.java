package io.github.pulsereport.core.ci;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects GitHub Actions run metadata (commit, branch, run and job links) from
 * environment variables for display in {@code TestRun.environment}.
 *
 * <p>All values are validated; URLs are only produced from an http(s) server,
 * an {@code owner/repo} repository and numeric identifiers. Never throws.</p>
 */
public final class GitHubActionsDetector {

    public static final String GIT_COMMIT = "git.commit";
    public static final String GIT_COMMIT_URL = "git.commitUrl";
    public static final String GIT_BRANCH = "git.branch";
    public static final String CI_RUN = "ci.run";
    public static final String CI_RUN_URL = "ci.runUrl";
    public static final String CI_JOB_URL = "ci.jobUrl";

    private static final Logger logger = LoggerFactory.getLogger(GitHubActionsDetector.class);

    private static final String DEFAULT_SERVER = "https://github.com";
    private static final Pattern SHA = Pattern.compile("^(?:[0-9a-fA-F]{40}|[0-9a-fA-F]{64})$");
    private static final Pattern REPOSITORY = Pattern.compile("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$");
    private static final Pattern NUMBER = Pattern.compile("^\\d{1,18}$");

    private GitHubActionsDetector() {
    }

    /**
     * Detects metadata from the current process environment.
     *
     * @return ordered metadata entries; empty outside GitHub Actions
     */
    public static Map<String, String> detect() {
        try {
            return detect(System.getenv());
        } catch (RuntimeException e) {
            logger.debug("Could not read process environment for CI detection: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * Detects metadata from the given environment variables.
     *
     * @param env environment variables (may be null)
     * @return ordered metadata entries; empty outside GitHub Actions
     */
    public static Map<String, String> detect(Map<String, String> env) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            if (env == null || !"true".equalsIgnoreCase(value(env, "GITHUB_ACTIONS"))) {
                return result;
            }

            String repoBase = repositoryBaseUrl(value(env, "GITHUB_SERVER_URL"), value(env, "GITHUB_REPOSITORY"));

            String sha = value(env, "GITHUB_SHA");
            if (sha != null && SHA.matcher(sha).matches()) {
                result.put(GIT_COMMIT, sha);
                if (repoBase != null) {
                    result.put(GIT_COMMIT_URL, repoBase + "/commit/" + sha);
                }
            }

            String branch = value(env, "GITHUB_HEAD_REF");
            if (branch == null) {
                branch = value(env, "GITHUB_REF_NAME");
            }
            if (branch != null) {
                result.put(GIT_BRANCH, branch);
            }

            String runLabel = runLabel(number(env, "GITHUB_RUN_NUMBER"), value(env, "GITHUB_WORKFLOW"));
            if (runLabel != null) {
                result.put(CI_RUN, runLabel);
            }

            if (repoBase != null) {
                putRunUrls(env, repoBase, result);
            }
        } catch (RuntimeException e) {
            logger.debug("GitHub Actions metadata detection failed: {}", e.getMessage());
            result.clear();
        }
        return result;
    }

    /**
     * Merges detected entries into a copy of an environment map without
     * overriding existing keys.
     *
     * @param environment existing environment (may be null or unmodifiable)
     * @param detected detected entries (may be null)
     * @return a new merged map, or null when both inputs are null/empty and
     *         {@code environment} was null
     */
    public static Map<String, String> mergeInto(Map<String, String> environment, Map<String, String> detected) {
        if ((detected == null || detected.isEmpty()) && environment == null) {
            return null;
        }
        Map<String, String> merged = new LinkedHashMap<>();
        if (environment != null) {
            merged.putAll(environment);
        }
        if (detected != null) {
            String userCommit = environment != null ? environment.get(GIT_COMMIT) : null;
            // A detected commit URL would point to a different SHA than the user-supplied commit.
            boolean skipCommitUrl = userCommit != null && !userCommit.equalsIgnoreCase(detected.get(GIT_COMMIT));
            detected.forEach((key, value) -> {
                if (!(skipCommitUrl && GIT_COMMIT_URL.equals(key))) {
                    merged.putIfAbsent(key, value);
                }
            });
        }
        return merged;
    }

    private static void putRunUrls(Map<String, String> env, String repoBase, Map<String, String> result) {
        String runId = number(env, "GITHUB_RUN_ID");
        if (runId == null) {
            return;
        }
        String runBase = repoBase + "/actions/runs/" + runId;
        String attempt = number(env, "GITHUB_RUN_ATTEMPT");
        result.put(CI_RUN_URL, attempt != null && Long.parseLong(attempt) > 1
                ? runBase + "/attempts/" + attempt
                : runBase);

        String jobId = number(env, "PULSEREPORT_JOB_ID");
        if (jobId != null) {
            result.put(CI_JOB_URL, runBase + "/job/" + jobId);
        }
    }

    private static String repositoryBaseUrl(String serverValue, String repository) {
        String server = serverValue != null ? serverValue : DEFAULT_SERVER;
        while (server.endsWith("/")) {
            server = server.substring(0, server.length() - 1);
        }
        if (!isSafeServer(server) || !isValidRepository(repository)) {
            return null;
        }
        return server + "/" + repository;
    }

    private static boolean isSafeServer(String server) {
        try {
            URI uri = new URI(server);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
            return ("http".equals(scheme) || "https".equals(scheme))
                    && uri.getHost() != null && !uri.getHost().isEmpty()
                    && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isValidRepository(String repository) {
        if (repository == null || !REPOSITORY.matcher(repository).matches()) {
            return false;
        }
        for (String segment : repository.split("/")) {
            if (".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    private static String runLabel(String runNumber, String workflow) {
        if (runNumber == null) {
            return workflow;
        }
        return workflow != null ? "Run #" + runNumber + " · " + workflow : "Run #" + runNumber;
    }

    private static String number(Map<String, String> env, String key) {
        String value = value(env, key);
        return value != null && NUMBER.matcher(value).matches() ? value : null;
    }

    private static String value(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
