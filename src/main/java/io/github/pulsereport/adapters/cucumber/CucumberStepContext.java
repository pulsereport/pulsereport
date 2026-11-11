package io.github.pulsereport.adapters.cucumber;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.pulsereport.adapters.CaptureBridge;
import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestStep;

/**
 * Lightweight context bridge that exposes a {@link ThreadLocal} buffer for
 * {@link Artifact} objects belonging to the currently executing Cucumber step.
 *
 * <p>This class has <strong>no Cucumber API imports</strong>, so it can be safely
 * referenced by {@link io.github.pulsereport.adapters.restassured.RestAssuredAdapter}
 * without requiring the Cucumber JARs to be present at runtime (they are
 * {@code provided} scope for the library consumer).</p>
 *
 * <p>{@code CucumberAdapter} sets/clears this ThreadLocal during
 * {@code TestStepStarted} / {@code TestStepFinished} events.
 * {@code RestAssuredAdapter} checks it in {@code addArtifact} to route
 * HTTP request/response artifacts to the active step instead of the legacy
 * test-level map.</p>
 */
public final class CucumberStepContext {

    /**
     * The per-thread artifact buffer for the currently active Cucumber step.
     * {@code null} when no Cucumber step is executing on this thread.
     */
    public static final ThreadLocal<List<Artifact>> currentStepArtifacts = new ThreadLocal<>();

    /**
     * Sub-steps (e.g. tracked Selenium actions) recorded while the current
     * Gherkin step or scenario hook runs; {@code null} outside of one.
     */
    static final ThreadLocal<List<TestStep>> currentStepChildren = new ThreadLocal<>();

    /** Artifacts that belong to the scenario itself (recordings), captured while it runs. */
    static final ThreadLocal<List<Artifact>> currentScenarioArtifacts = new ThreadLocal<>();

    /** Metrics recorded while the current scenario runs. */
    static final ThreadLocal<List<Metric>> currentScenarioMetrics = new ThreadLocal<>();

    /** Tags added while the current scenario runs (e.g. the browser it used). */
    static final ThreadLocal<Set<String>> currentScenarioTags = new ThreadLocal<>();

    /** What a before-step hook captured, handed to the step that follows it. */
    private static final ThreadLocal<List<TestStep>> carriedChildren = new ThreadLocal<>();
    private static final ThreadLocal<List<Artifact>> carriedArtifacts = new ThreadLocal<>();

    /**
     * Routes captures made through {@link CaptureBridge} (e.g. by the Selenium
     * tracker) into the running Gherkin step or hook. Registered by
     * {@code CucumberAdapter}; it wins over a TestNG runner's sink.
     */
    static final CaptureBridge.Sink SINK = new CaptureBridge.Sink() {
        @Override
        public boolean isActive() {
            return currentStepChildren.get() != null;
        }

        @Override
        public void addStep(TestStep step) {
            List<TestStep> children = currentStepChildren.get();
            if (children != null) {
                children.add(step);
            }
        }

        @Override
        public void addArtifact(Artifact artifact) {
            // A recording goes on the scenario, where the report syncs the steps against it
            List<Artifact> scenario = currentScenarioArtifacts.get();
            if (scenario != null && CaptureBridge.belongsToTest(artifact)) {
                scenario.add(artifact);
                return;
            }
            List<Artifact> artifacts = currentStepArtifacts.get();
            if (artifacts != null) {
                artifacts.add(artifact);
            }
        }

        @Override
        public void addMetric(Metric metric) {
            List<Metric> metrics = currentScenarioMetrics.get();
            if (metrics != null) {
                metrics.add(metric);
            }
        }

        @Override
        public void addTag(String tag) {
            Set<String> tags = currentScenarioTags.get();
            if (tags != null && tag != null && !tag.isBlank()) {
                tags.add(tag);
            }
        }
    };

    /** Starts capturing for a scenario. */
    static void beginScenario() {
        currentScenarioArtifacts.set(new ArrayList<>());
        currentScenarioMetrics.set(new ArrayList<>());
        currentScenarioTags.set(new LinkedHashSet<>());
        carriedChildren.remove();
        carriedArtifacts.remove();
    }

    /** Stops capturing for a scenario. */
    static void endScenario() {
        currentStepChildren.remove();
        currentScenarioArtifacts.remove();
        currentScenarioMetrics.remove();
        currentScenarioTags.remove();
        carriedChildren.remove();
        carriedArtifacts.remove();
    }

    /** Starts capturing for a Gherkin step or hook, seeded with what a before-step hook captured. */
    static void beginStep() {
        List<Artifact> artifacts = currentStepArtifacts.get();
        if (artifacts != null) {
            artifacts.clear();
            List<Artifact> carried = carriedArtifacts.get();
            if (carried != null) {
                artifacts.addAll(carried);
            }
        }
        List<TestStep> children = new ArrayList<>();
        List<TestStep> carried = carriedChildren.get();
        if (carried != null) {
            children.addAll(carried);
        }
        carriedChildren.remove();
        carriedArtifacts.remove();
        currentStepChildren.set(children);
    }

    /** @return the sub-steps captured since {@link #beginStep()}, and stops capturing them */
    static List<TestStep> endStep() {
        List<TestStep> children = currentStepChildren.get();
        currentStepChildren.remove();
        return children == null ? new ArrayList<>() : children;
    }

    /** Hands a before-step hook's captures to the next Gherkin step. */
    static void carry(List<TestStep> children, List<Artifact> artifacts) {
        carriedChildren.set(new ArrayList<>(children));
        carriedArtifacts.set(new ArrayList<>(artifacts));
    }

    private CucumberStepContext() {
        // utility class
    }
}
