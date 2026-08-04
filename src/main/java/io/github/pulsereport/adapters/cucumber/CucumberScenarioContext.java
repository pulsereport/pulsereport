package io.github.pulsereport.adapters.cucumber;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.github.pulsereport.core.model.TestStep;

/**
 * Mutable in-flight state for a single Cucumber scenario executing on one
 * thread. Holds step results, background steps, and any artifacts pending
 * attachment.
 *
 * <p>
 * This is an internal helper class used by {@link CucumberAdapter}. Not part of
 * the public API.</p>
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
class CucumberScenarioContext {

    /**
     * Scenario id from Cucumber (pickle id).
     */
    private final String scenarioId;

    /**
     * Human-readable scenario name (with parameter values for Scenario
     * Outlines).
     */
    private final String scenarioName;

    /**
     * BDD type: "scenario" or "scenario_outline".
     */
    private final String bddType;

    /**
     * Feature name this scenario belongs to.
     */
    private final String featureName;

    /**
     * Feature description (may be multi-line).
     */
    private final String featureDescription;

    /**
     * Tags associated with this scenario (includes inherited feature-level
     * tags).
     */
    private final List<String> tags;

    /**
     * When the scenario started.
     */
    private Instant startTime;

    /**
     * Steps collected for this scenario (excludes background).
     */
    private final List<TestStep> steps = new ArrayList<>();

    /**
     * Background steps collected for this scenario.
     */
    private final List<TestStep> backgroundSteps = new ArrayList<>();

    /**
     * Step currently being executed.
     */
    private String currentStepName;
    private String currentStepKeyword;
    private Instant currentStepStart;
    private boolean currentStepIsBackground;

    CucumberScenarioContext(String scenarioId, String scenarioName, String bddType,
            String featureName, String featureDescription, List<String> tags) {
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.bddType = bddType;
        this.featureName = featureName;
        this.featureDescription = featureDescription;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    String getScenarioId() {
        return scenarioId;
    }

    String getScenarioName() {
        return scenarioName;
    }

    String getBddType() {
        return bddType;
    }

    String getFeatureName() {
        return featureName;
    }

    String getFeatureDescription() {
        return featureDescription;
    }

    List<String> getTags() {
        return tags;
    }

    Instant getStartTime() {
        return startTime;
    }

    void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    List<TestStep> getSteps() {
        return steps;
    }

    List<TestStep> getBackgroundSteps() {
        return backgroundSteps;
    }

    String getCurrentStepName() {
        return currentStepName;
    }

    String getCurrentStepKeyword() {
        return currentStepKeyword;
    }

    Instant getCurrentStepStart() {
        return currentStepStart;
    }

    boolean isCurrentStepBackground() {
        return currentStepIsBackground;
    }

    void startStep(String keyword, String name, Instant start, boolean isBackground) {
        this.currentStepKeyword = keyword;
        this.currentStepName = name;
        this.currentStepStart = start;
        this.currentStepIsBackground = isBackground;
    }

    void addStep(TestStep step) {
        steps.add(step);
    }

    void addBackgroundStep(TestStep step) {
        backgroundSteps.add(step);
    }
}
