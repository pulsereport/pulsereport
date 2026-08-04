package io.github.pulsereport.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an individual test case execution. Immutable value object using
 * Builder pattern.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TestCase {

    private final String id;
    private final String name;
    private final String className;
    private final String methodName;
    private final Instant startTime;
    private final Instant endTime;
    private final long duration;
    private final TestStatus status;
    private final String errorMessage;
    private final String stackTrace;
    private final List<TestStep> steps;
    private final List<Artifact> artifacts;
    private final List<Metric> metrics;
    private final int retryCount;
    private final String bddType;
    private final String featureName;
    private final String featureDescription;
    private final List<TestStep> backgroundSteps;
    private final List<String> tags;

    @JsonCreator
    private TestCase(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("className") String className,
            @JsonProperty("methodName") String methodName,
            @JsonProperty("startTime") Instant startTime,
            @JsonProperty("endTime") Instant endTime,
            @JsonProperty("duration") long duration,
            @JsonProperty("status") TestStatus status,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("stackTrace") String stackTrace,
            @JsonProperty("steps") List<TestStep> steps,
            @JsonProperty("artifacts") List<Artifact> artifacts,
            @JsonProperty("metrics") List<Metric> metrics,
            @JsonProperty("retryCount") int retryCount,
            @JsonProperty("bddType") String bddType,
            @JsonProperty("featureName") String featureName,
            @JsonProperty("featureDescription") String featureDescription,
            @JsonProperty("backgroundSteps") List<TestStep> backgroundSteps,
            @JsonProperty("tags") List<String> tags) {
        this.id = id;
        this.name = name;
        this.className = className;
        this.methodName = methodName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.status = status;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.steps = steps != null ? new ArrayList<>(steps) : new ArrayList<>();
        this.artifacts = artifacts != null ? new ArrayList<>(artifacts) : new ArrayList<>();
        this.metrics = metrics != null ? new ArrayList<>(metrics) : new ArrayList<>();
        this.retryCount = retryCount;
        this.bddType = bddType;
        this.featureName = featureName;
        this.featureDescription = featureDescription;
        this.backgroundSteps = backgroundSteps != null ? new ArrayList<>(backgroundSteps) : new ArrayList<>();
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    /**
     * Gets the test case ID.
     *
     * @return the test case ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the test case name.
     *
     * @return the test case name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the test class name.
     *
     * @return the class name (optional)
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the test method name.
     *
     * @return the method name (optional)
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gets the start time.
     *
     * @return the start time
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Gets the end time.
     *
     * @return the end time
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Gets the duration in milliseconds.
     *
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Gets the test status.
     *
     * @return the status
     */
    public TestStatus getStatus() {
        return status;
    }

    /**
     * Gets the error message (for failed tests).
     *
     * @return the error message (optional)
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the stack trace (for failed tests).
     *
     * @return the stack trace (optional)
     */
    public String getStackTrace() {
        return stackTrace;
    }

    /**
     * Gets the test steps.
     *
     * @return an unmodifiable list of test steps
     */
    public List<TestStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * Gets the artifacts.
     *
     * @return an unmodifiable list of artifacts
     */
    public List<Artifact> getArtifacts() {
        return Collections.unmodifiableList(artifacts);
    }

    /**
     * Gets the metrics.
     *
     * @return an unmodifiable list of metrics
     */
    public List<Metric> getMetrics() {
        return Collections.unmodifiableList(metrics);
    }

    /**
     * Gets the retry count.
     *
     * @return the number of retries
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Gets the BDD type ("scenario", "scenario_outline", or null for non-BDD).
     *
     * @return the BDD type (optional)
     */
    public String getBddType() {
        return bddType;
    }

    /**
     * Gets the Gherkin Feature name.
     *
     * @return the feature name (optional)
     */
    public String getFeatureName() {
        return featureName;
    }

    /**
     * Gets the Gherkin Feature description.
     *
     * @return the feature description (optional)
     */
    public String getFeatureDescription() {
        return featureDescription;
    }

    /**
     * Gets the background steps inlined per scenario.
     *
     * @return an unmodifiable list of background steps
     */
    public List<TestStep> getBackgroundSteps() {
        return Collections.unmodifiableList(backgroundSteps);
    }

    /**
     * Gets the tags associated with this test case.
     *
     * @return an unmodifiable list of tags
     */
    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    /**
     * Creates a new builder for TestCase.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Compares this TestCase to another object for equality.
     *
     * @param o the object to compare with
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestCase testCase = (TestCase) o;
        return duration == testCase.duration
                && retryCount == testCase.retryCount
                && Objects.equals(id, testCase.id)
                && Objects.equals(name, testCase.name)
                && Objects.equals(className, testCase.className)
                && Objects.equals(methodName, testCase.methodName)
                && Objects.equals(startTime, testCase.startTime)
                && Objects.equals(endTime, testCase.endTime)
                && status == testCase.status
                && Objects.equals(errorMessage, testCase.errorMessage)
                && Objects.equals(stackTrace, testCase.stackTrace)
                && Objects.equals(steps, testCase.steps)
                && Objects.equals(artifacts, testCase.artifacts)
                && Objects.equals(metrics, testCase.metrics)
                && Objects.equals(bddType, testCase.bddType)
                && Objects.equals(featureName, testCase.featureName)
                && Objects.equals(featureDescription, testCase.featureDescription)
                && Objects.equals(backgroundSteps, testCase.backgroundSteps)
                && Objects.equals(tags, testCase.tags);
    }

    /**
     * Returns a hash code value for this TestCase.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, className, methodName, startTime, endTime,
                duration, status, errorMessage, stackTrace, steps, artifacts, metrics, retryCount,
                bddType, featureName, featureDescription, backgroundSteps, tags);
    }

    /**
     * Returns a string representation of this TestCase.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "TestCase{"
                + "id='" + id + '\''
                + ", name='" + name + '\''
                + ", className='" + className + '\''
                + ", methodName='" + methodName + '\''
                + ", startTime=" + startTime
                + ", endTime=" + endTime
                + ", duration=" + duration
                + ", status=" + status
                + ", errorMessage='" + errorMessage + '\''
                + ", stackTrace='" + stackTrace + '\''
                + ", steps=" + steps.size()
                + ", artifacts=" + artifacts.size()
                + ", metrics=" + metrics.size()
                + ", retryCount=" + retryCount
                + ", tags=" + tags
                + '}';
    }

    /**
     * Builder for TestCase instances.
     */
    public static final class Builder {

        private String id;
        private String name;
        private String className;
        private String methodName;
        private Instant startTime;
        private Instant endTime;
        private long duration;
        private TestStatus status;
        private String errorMessage;
        private String stackTrace;
        private List<TestStep> steps;
        private List<Artifact> artifacts;
        private List<Metric> metrics;
        private int retryCount;
        private String bddType;
        private String featureName;
        private String featureDescription;
        private List<TestStep> backgroundSteps;
        private List<String> tags;

        private Builder() {
        }

        /**
         * Sets the test case ID.
         *
         * @param id the test case ID (required)
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the test case name.
         *
         * @param name the test case name (required)
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the test class name.
         *
         * @param className the class name (optional)
         * @return this builder
         */
        public Builder className(String className) {
            this.className = className;
            return this;
        }

        /**
         * Sets the test method name.
         *
         * @param methodName the method name (optional)
         * @return this builder
         */
        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        /**
         * Sets the start time.
         *
         * @param startTime the start time
         * @return this builder
         */
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * Sets the end time.
         *
         * @param endTime the end time
         * @return this builder
         */
        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * Sets the duration in milliseconds.
         *
         * @param duration the duration
         * @return this builder
         */
        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Sets the test status.
         *
         * @param status the status (required)
         * @return this builder
         */
        public Builder status(TestStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param errorMessage the error message (optional)
         * @return this builder
         */
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Sets the stack trace.
         *
         * @param stackTrace the stack trace (optional)
         * @return this builder
         */
        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        /**
         * Sets the test steps.
         *
         * @param steps the test steps (optional)
         * @return this builder
         */
        public Builder steps(List<TestStep> steps) {
            this.steps = steps;
            return this;
        }

        /**
         * Sets the artifacts.
         *
         * @param artifacts the artifacts (optional)
         * @return this builder
         */
        public Builder artifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        /**
         * Sets the metrics.
         *
         * @param metrics the metrics (optional)
         * @return this builder
         */
        public Builder metrics(List<Metric> metrics) {
            this.metrics = metrics;
            return this;
        }

        /**
         * Sets the retry count.
         *
         * @param retryCount the retry count (default 0)
         * @return this builder
         */
        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        /**
         * Sets the BDD type ("scenario", "scenario_outline", or null for
         * non-BDD).
         *
         * @param bddType the BDD type (optional)
         * @return this builder
         */
        public Builder bddType(String bddType) {
            this.bddType = bddType;
            return this;
        }

        /**
         * Sets the Gherkin Feature name.
         *
         * @param featureName the feature name (optional)
         * @return this builder
         */
        public Builder featureName(String featureName) {
            this.featureName = featureName;
            return this;
        }

        /**
         * Sets the Gherkin Feature description.
         *
         * @param featureDescription the feature description (optional)
         * @return this builder
         */
        public Builder featureDescription(String featureDescription) {
            this.featureDescription = featureDescription;
            return this;
        }

        /**
         * Sets the background steps inlined per scenario.
         *
         * @param backgroundSteps the background steps (optional)
         * @return this builder
         */
        public Builder backgroundSteps(List<TestStep> backgroundSteps) {
            this.backgroundSteps = backgroundSteps;
            return this;
        }

        /**
         * Sets the tags associated with this test case.
         *
         * @param tags the tags (optional)
         * @return this builder
         */
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Builds the TestCase instance.
         *
         * @return a new TestCase
         * @throws IllegalArgumentException if required fields are missing
         */
        public TestCase build() {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("id is required");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("status is required");
            }
            return new TestCase(id, name, className, methodName, startTime, endTime,
                    duration, status, errorMessage, stackTrace, steps, artifacts, metrics, retryCount,
                    bddType, featureName, featureDescription, backgroundSteps, tags);
        }
    }
}
