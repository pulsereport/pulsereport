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
 * Represents an individual step within a test case execution.
 * Useful for BDD-style reporting or detailed step-by-step tracking.
 * Immutable value object using Builder pattern.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TestStep {
    
    private final String name;
    private final TestStatus status;
    private final Instant startTime;
    private final Instant endTime;
    private final long duration;
    private final String description;
    private final String keyword;
    private final String docString;
    private final List<List<String>> dataTable;
    private final String errorMessage;
    private final String stackTrace;
    private final List<Artifact> artifacts;

    @JsonCreator
    private TestStep(
            @JsonProperty("name") String name,
            @JsonProperty("status") TestStatus status,
            @JsonProperty("startTime") Instant startTime,
            @JsonProperty("endTime") Instant endTime,
            @JsonProperty("duration") long duration,
            @JsonProperty("description") String description,
            @JsonProperty("keyword") String keyword,
            @JsonProperty("docString") String docString,
            @JsonProperty("dataTable") List<List<String>> dataTable,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("stackTrace") String stackTrace,
            @JsonProperty("artifacts") List<Artifact> artifacts) {
        this.name = name;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.description = description;
        this.keyword = keyword;
        this.docString = docString;
        this.dataTable = dataTable != null ? new ArrayList<>(dataTable) : null;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.artifacts = artifacts != null ? new ArrayList<>(artifacts) : new ArrayList<>();
    }

    /**
     * Gets the step name.
     * @return the step name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the step status.
     * @return the status
     */
    public TestStatus getStatus() {
        return status;
    }

    /**
     * Gets the step start time.
     * @return the start time
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Gets the step end time.
     * @return the end time
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Gets the step duration in milliseconds.
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Gets the step description.
     * @return the description (optional)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the BDD keyword (Given, When, Then, And, But, *).
     * @return the keyword (optional)
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Gets the docstring argument for this step.
     * @return the docstring (optional)
     */
    public String getDocString() {
        return docString;
    }

    /**
     * Gets the data table rows for this step.
     * @return the data table (optional)
     */
    public List<List<String>> getDataTable() {
        return dataTable != null ? Collections.unmodifiableList(dataTable) : null;
    }

    /**
     * Gets the step-level error message.
     * @return the error message (optional)
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the step-level stack trace.
     * @return the stack trace (optional)
     */
    public String getStackTrace() {
        return stackTrace;
    }

    /**
     * Gets the HTTP artifacts attached to this step.
     * @return an unmodifiable list of artifacts
     */
    public List<Artifact> getArtifacts() {
        return Collections.unmodifiableList(artifacts);
    }

    /**
     * Creates a new builder for TestStep.
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Compares this TestStep to another object for equality.
     * @param o the object to compare with
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestStep testStep = (TestStep) o;
        return duration == testStep.duration &&
                Objects.equals(name, testStep.name) &&
                status == testStep.status &&
                Objects.equals(startTime, testStep.startTime) &&
                Objects.equals(endTime, testStep.endTime) &&
                Objects.equals(description, testStep.description) &&
                Objects.equals(keyword, testStep.keyword) &&
                Objects.equals(docString, testStep.docString) &&
                Objects.equals(dataTable, testStep.dataTable) &&
                Objects.equals(errorMessage, testStep.errorMessage) &&
                Objects.equals(stackTrace, testStep.stackTrace) &&
                Objects.equals(artifacts, testStep.artifacts);
    }

    /**
     * Returns a hash code value for this TestStep.
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, status, startTime, endTime, duration, description,
                keyword, docString, dataTable, errorMessage, stackTrace, artifacts);
    }

    /**
     * Returns a string representation of this TestStep.
     * @return a string representation
     */
    @Override
    public String toString() {
        return "TestStep{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + duration +
                ", description='" + description + '\'' +
                ", keyword='" + keyword + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    /**
     * Builder for TestStep instances.
     */
    public static final class Builder {
        private String name;
        private TestStatus status;
        private Instant startTime;
        private Instant endTime;
        private long duration;
        private String description;
        private String keyword;
        private String docString;
        private List<List<String>> dataTable;
        private String errorMessage;
        private String stackTrace;
        private List<Artifact> artifacts;

        private Builder() {
        }

        /**
         * Sets the step name.
         * @param name the step name (required)
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the step status.
         * @param status the status (required)
         * @return this builder
         */
        public Builder status(TestStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the start time.
         * @param startTime the start time
         * @return this builder
         */
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * Sets the end time.
         * @param endTime the end time
         * @return this builder
         */
        public Builder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * Sets the duration in milliseconds.
         * @param duration the duration
         * @return this builder
         */
        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Sets the description.
         * @param description the description (optional)
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the BDD keyword.
         * @param keyword the BDD keyword (optional)
         * @return this builder
         */
        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        /**
         * Sets the docstring argument.
         * @param docString the docstring (optional)
         * @return this builder
         */
        public Builder docString(String docString) {
            this.docString = docString;
            return this;
        }

        /**
         * Sets the data table rows.
         * @param dataTable the data table (optional)
         * @return this builder
         */
        public Builder dataTable(List<List<String>> dataTable) {
            this.dataTable = dataTable;
            return this;
        }

        /**
         * Sets the step-level error message.
         * @param errorMessage the error message (optional)
         * @return this builder
         */
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Sets the step-level stack trace.
         * @param stackTrace the stack trace (optional)
         * @return this builder
         */
        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        /**
         * Sets the artifacts attached to this step.
         * @param artifacts the artifacts (optional)
         * @return this builder
         */
        public Builder artifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        /**
         * Builds the TestStep instance.
         * @return a new TestStep
         * @throws IllegalArgumentException if required fields are missing
         */
        public TestStep build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("status is required");
            }
            return new TestStep(name, status, startTime, endTime, duration, description,
                    keyword, docString, dataTable, errorMessage, stackTrace, artifacts);
        }
    }
}
