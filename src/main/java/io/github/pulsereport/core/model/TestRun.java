package io.github.pulsereport.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a complete test run execution containing multiple test suites.
 * This is the top-level container for all test execution data.
 * Immutable value object using Builder pattern.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TestRun {
    
    private final String id;
    private final String name;
    private final Instant startTime;
    private final Instant endTime;
    private final long duration;
    private final TestStatus status;
    private final List<TestSuite> suites;
    private final Map<String, String> environment;
    private final int totalTests;
    private final int passedTests;
    private final int failedTests;
    private final int skippedTests;

    @JsonCreator
    private TestRun(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("startTime") Instant startTime,
            @JsonProperty("endTime") Instant endTime,
            @JsonProperty("duration") long duration,
            @JsonProperty("status") TestStatus status,
            @JsonProperty("suites") List<TestSuite> suites,
            @JsonProperty("environment") Map<String, String> environment,
            @JsonProperty("totalTests") int totalTests,
            @JsonProperty("passedTests") int passedTests,
            @JsonProperty("failedTests") int failedTests,
            @JsonProperty("skippedTests") int skippedTests) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.status = status;
        this.suites = suites != null ? new ArrayList<>(suites) : new ArrayList<>();
        this.environment = environment != null ? new HashMap<>(environment) : null;
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.skippedTests = skippedTests;
    }

    /**
     * Gets the test run ID.
     * @return the test run ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the test run name.
     * @return the test run name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the start time.
     * @return the start time
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Gets the end time.
     * @return the end time
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Gets the duration in milliseconds.
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * Gets the test run status.
     * @return the status
     */
    public TestStatus getStatus() {
        return status;
    }

    /**
     * Gets the test suites in this run.
     * @return an unmodifiable list of test suites
     */
    public List<TestSuite> getSuites() {
        return Collections.unmodifiableList(suites);
    }

    /**
     * Gets the environment metadata.
     * @return an unmodifiable map of environment properties (optional)
     */
    public Map<String, String> getEnvironment() {
        return environment != null ? Collections.unmodifiableMap(environment) : null;
    }

    /**
     * Gets the total number of tests.
     * @return the total tests count
     */
    public int getTotalTests() {
        return totalTests;
    }

    /**
     * Gets the number of passed tests.
     * @return the passed tests count
     */
    public int getPassedTests() {
        return passedTests;
    }

    /**
     * Gets the number of failed tests.
     * @return the failed tests count
     */
    public int getFailedTests() {
        return failedTests;
    }

    /**
     * Gets the number of skipped tests.
     * @return the skipped tests count
     */
    public int getSkippedTests() {
        return skippedTests;
    }

    /**
     * Creates a new builder for TestRun.
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Compares this TestRun to another object for equality.
     * @param o the object to compare with
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestRun testRun = (TestRun) o;
        return duration == testRun.duration &&
                totalTests == testRun.totalTests &&
                passedTests == testRun.passedTests &&
                failedTests == testRun.failedTests &&
                skippedTests == testRun.skippedTests &&
                Objects.equals(id, testRun.id) &&
                Objects.equals(name, testRun.name) &&
                Objects.equals(startTime, testRun.startTime) &&
                Objects.equals(endTime, testRun.endTime) &&
                status == testRun.status &&
                Objects.equals(suites, testRun.suites) &&
                Objects.equals(environment, testRun.environment);
    }

    /**
     * Returns a hash code value for this TestRun.
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, startTime, endTime, duration, status, 
                suites, environment, totalTests, passedTests, failedTests, skippedTests);
    }

    /**
     * Returns a string representation of this TestRun.
     * @return a string representation
     */
    @Override
    public String toString() {
        return "TestRun{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + duration +
                ", status=" + status +
                ", suites=" + suites.size() +
                ", environment=" + environment +
                ", totalTests=" + totalTests +
                ", passedTests=" + passedTests +
                ", failedTests=" + failedTests +
                ", skippedTests=" + skippedTests +
                '}';
    }

    /**
     * Builder for TestRun instances.
     */
    public static final class Builder {
        private String id;
        private String name;
        private Instant startTime;
        private Instant endTime;
        private long duration;
        private TestStatus status;
        private List<TestSuite> suites;
        private Map<String, String> environment;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;

        private Builder() {
        }

        /**
         * Sets the test run ID.
         * @param id the test run ID (required)
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the test run name.
         * @param name the test run name (required)
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the start time.
         * @param startTime the start time (required)
         * @return this builder
         */
        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * Sets the end time.
         * @param endTime the end time (optional)
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
         * Sets the test run status.
         * @param status the status (required)
         * @return this builder
         */
        public Builder status(TestStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the test suites.
         * @param suites the test suites (required)
         * @return this builder
         */
        public Builder suites(List<TestSuite> suites) {
            this.suites = suites;
            return this;
        }

        /**
         * Sets the environment metadata.
         * @param environment the environment properties (optional)
         * @return this builder
         */
        public Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Sets the total tests count.
         * @param totalTests the total tests count
         * @return this builder
         */
        public Builder totalTests(int totalTests) {
            this.totalTests = totalTests;
            return this;
        }

        /**
         * Sets the passed tests count.
         * @param passedTests the passed tests count
         * @return this builder
         */
        public Builder passedTests(int passedTests) {
            this.passedTests = passedTests;
            return this;
        }

        /**
         * Sets the failed tests count.
         * @param failedTests the failed tests count
         * @return this builder
         */
        public Builder failedTests(int failedTests) {
            this.failedTests = failedTests;
            return this;
        }

        /**
         * Sets the skipped tests count.
         * @param skippedTests the skipped tests count
         * @return this builder
         */
        public Builder skippedTests(int skippedTests) {
            this.skippedTests = skippedTests;
            return this;
        }

        /**
         * Builds the TestRun instance.
         * @return a new TestRun
         * @throws IllegalArgumentException if required fields are missing
         */
        public TestRun build() {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("id is required");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("status is required");
            }
            if (startTime == null) {
                throw new IllegalArgumentException("startTime is required");
            }
            if (suites == null) {
                throw new IllegalArgumentException("suites is required");
            }
            return new TestRun(id, name, startTime, endTime, duration, status, 
                    suites, environment, totalTests, passedTests, failedTests, skippedTests);
        }
    }
}
