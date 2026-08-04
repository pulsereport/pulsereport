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
 * Represents a test suite execution containing multiple test cases. Immutable
 * value object using Builder pattern.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TestSuite {

    private final String id;
    private final String name;
    private final String secondaryText;
    private final Instant startTime;
    private final Instant endTime;
    private final long duration;
    private final TestStatus status;
    private final List<TestCase> testCases;
    private final int totalTests;
    private final int passedTests;
    private final int failedTests;
    private final int skippedTests;
    private final List<String> tags;

    @JsonCreator
    private TestSuite(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("secondaryText") String secondaryText,
            @JsonProperty("startTime") Instant startTime,
            @JsonProperty("endTime") Instant endTime,
            @JsonProperty("duration") long duration,
            @JsonProperty("status") TestStatus status,
            @JsonProperty("testCases") List<TestCase> testCases,
            @JsonProperty("totalTests") int totalTests,
            @JsonProperty("passedTests") int passedTests,
            @JsonProperty("failedTests") int failedTests,
            @JsonProperty("skippedTests") int skippedTests,
            @JsonProperty("tags") List<String> tags) {
        this.id = id;
        this.name = name;
        this.secondaryText = secondaryText;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.status = status;
        this.testCases = testCases != null ? new ArrayList<>(testCases) : new ArrayList<>();
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.skippedTests = skippedTests;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    /**
     * Gets the suite ID.
     *
     * @return the suite ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the suite name.
     *
     * @return the suite name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the optional secondary suite text intended for readable path
     * display.
     *
     * @return the optional secondary text
     */
    public String getSecondaryText() {
        return secondaryText;
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
     * Gets the suite status.
     *
     * @return the status
     */
    public TestStatus getStatus() {
        return status;
    }

    /**
     * Gets the test cases in this suite.
     *
     * @return an unmodifiable list of test cases
     */
    public List<TestCase> getTestCases() {
        return Collections.unmodifiableList(testCases);
    }

    /**
     * Gets the total number of tests.
     *
     * @return the total tests count
     */
    public int getTotalTests() {
        return totalTests;
    }

    /**
     * Gets the number of passed tests.
     *
     * @return the passed tests count
     */
    public int getPassedTests() {
        return passedTests;
    }

    /**
     * Gets the number of failed tests.
     *
     * @return the failed tests count
     */
    public int getFailedTests() {
        return failedTests;
    }

    /**
     * Gets the number of skipped tests.
     *
     * @return the skipped tests count
     */
    public int getSkippedTests() {
        return skippedTests;
    }

    /**
     * Gets the tags associated with this test suite.
     *
     * @return an unmodifiable list of tags
     */
    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    /**
     * Creates a new builder for TestSuite.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Compares this TestSuite to another object for equality.
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
        TestSuite that = (TestSuite) o;
        return duration == that.duration
                && totalTests == that.totalTests
                && passedTests == that.passedTests
                && failedTests == that.failedTests
                && skippedTests == that.skippedTests
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(secondaryText, that.secondaryText)
                && Objects.equals(startTime, that.startTime)
                && Objects.equals(endTime, that.endTime)
                && status == that.status
                && Objects.equals(testCases, that.testCases)
                && Objects.equals(tags, that.tags);
    }

    /**
     * Returns a hash code value for this TestSuite.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, secondaryText, startTime, endTime, duration, status,
                testCases, totalTests, passedTests, failedTests, skippedTests, tags);
    }

    /**
     * Returns a string representation of this TestSuite.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "TestSuite{"
                + "id='" + id + '\''
                + ", name='" + name + '\''
                + ", secondaryText='" + secondaryText + '\''
                + ", startTime=" + startTime
                + ", endTime=" + endTime
                + ", duration=" + duration
                + ", status=" + status
                + ", testCases=" + testCases.size()
                + ", totalTests=" + totalTests
                + ", passedTests=" + passedTests
                + ", failedTests=" + failedTests
                + ", skippedTests=" + skippedTests
                + ", tags=" + tags
                + '}';
    }

    /**
     * Builder for TestSuite instances.
     */
    public static final class Builder {

        private String id;
        private String name;
        private String secondaryText;
        private Instant startTime;
        private Instant endTime;
        private long duration;
        private TestStatus status;
        private List<TestCase> testCases;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;
        private List<String> tags;

        private Builder() {
        }

        /**
         * Sets the suite ID.
         *
         * @param id the suite ID (required)
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the suite name.
         *
         * @param name the suite name (required)
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the optional secondary suite text intended for readable path
         * display.
         *
         * @param secondaryText the optional secondary text
         * @return this builder
         */
        public Builder secondaryText(String secondaryText) {
            this.secondaryText = secondaryText;
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
         * Sets the suite status.
         *
         * @param status the status (required)
         * @return this builder
         */
        public Builder status(TestStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the test cases.
         *
         * @param testCases the test cases (required)
         * @return this builder
         */
        public Builder testCases(List<TestCase> testCases) {
            this.testCases = testCases;
            return this;
        }

        /**
         * Sets the total tests count.
         *
         * @param totalTests the total tests count
         * @return this builder
         */
        public Builder totalTests(int totalTests) {
            this.totalTests = totalTests;
            return this;
        }

        /**
         * Sets the passed tests count.
         *
         * @param passedTests the passed tests count
         * @return this builder
         */
        public Builder passedTests(int passedTests) {
            this.passedTests = passedTests;
            return this;
        }

        /**
         * Sets the failed tests count.
         *
         * @param failedTests the failed tests count
         * @return this builder
         */
        public Builder failedTests(int failedTests) {
            this.failedTests = failedTests;
            return this;
        }

        /**
         * Sets the skipped tests count.
         *
         * @param skippedTests the skipped tests count
         * @return this builder
         */
        public Builder skippedTests(int skippedTests) {
            this.skippedTests = skippedTests;
            return this;
        }

        /**
         * Sets the tags associated with this test suite.
         *
         * @param tags the tags (optional)
         * @return this builder
         */
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Builds the TestSuite instance.
         *
         * @return a new TestSuite
         * @throws IllegalArgumentException if required fields are missing
         */
        public TestSuite build() {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("id is required");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("status is required");
            }
            if (testCases == null) {
                throw new IllegalArgumentException("testCases is required");
            }
            return new TestSuite(id, name, secondaryText, startTime, endTime, duration, status,
                    testCases, totalTests, passedTests, failedTests, skippedTests, tags);
        }
    }
}
