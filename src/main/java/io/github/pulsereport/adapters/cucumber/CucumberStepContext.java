package io.github.pulsereport.adapters.cucumber;

import java.util.List;

import io.github.pulsereport.core.model.Artifact;

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

    private CucumberStepContext() {
        // utility class
    }
}
