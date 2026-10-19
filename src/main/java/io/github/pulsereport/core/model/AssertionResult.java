package io.github.pulsereport.core.model;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single assertion evaluated while a step was running, such as
 * {@code assertEquals(200, status, "Login API status code")}.
 *
 * <p>Captured automatically by {@link io.github.pulsereport.assertions.AssertionCapture}
 * for passing and failing assertions alike. Values are stored as display strings
 * so the report never holds references to live test objects.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AssertionResult {

    private final String framework;
    private final String method;
    private final String message;
    private final String expected;
    private final String actual;
    private final TestStatus status;
    private final String errorMessage;
    private final Instant timestamp;

    @JsonCreator
    private AssertionResult(
            @JsonProperty("framework") String framework,
            @JsonProperty("method") String method,
            @JsonProperty("message") String message,
            @JsonProperty("expected") String expected,
            @JsonProperty("actual") String actual,
            @JsonProperty("status") TestStatus status,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("timestamp") Instant timestamp) {
        this.framework = framework;
        this.method = method;
        this.message = message;
        this.expected = expected;
        this.actual = actual;
        this.status = status;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }

    /**
     * Gets the assertion library that evaluated the check (e.g. "JUnit 5", "TestNG", "Hamcrest").
     * @return the framework label (optional)
     */
    public String getFramework() {
        return framework;
    }

    /**
     * Gets the assertion method name (e.g. "assertEquals").
     * @return the method name
     */
    public String getMethod() {
        return method;
    }

    /**
     * Gets the user-supplied assertion message or Hamcrest reason.
     * @return the message (optional)
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the display form of the expected value or matcher description.
     * @return the expected value (optional)
     */
    public String getExpected() {
        return expected;
    }

    /**
     * Gets the display form of the actual value.
     * @return the actual value (optional)
     */
    public String getActual() {
        return actual;
    }

    /**
     * Gets the outcome: {@link TestStatus#PASSED} or {@link TestStatus#FAILED}.
     * @return the status
     */
    public TestStatus getStatus() {
        return status;
    }

    /**
     * Gets the failure message raised by the assertion library.
     * @return the error message, present only for failed assertions
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets when the assertion finished.
     * @return the timestamp (optional)
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Creates a new builder.
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssertionResult that = (AssertionResult) o;
        return Objects.equals(framework, that.framework)
                && Objects.equals(method, that.method)
                && Objects.equals(message, that.message)
                && Objects.equals(expected, that.expected)
                && Objects.equals(actual, that.actual)
                && status == that.status
                && Objects.equals(errorMessage, that.errorMessage)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(framework, method, message, expected, actual, status, errorMessage, timestamp);
    }

    @Override
    public String toString() {
        return "AssertionResult{" +
                "framework='" + framework + '\'' +
                ", method='" + method + '\'' +
                ", message='" + message + '\'' +
                ", expected='" + expected + '\'' +
                ", actual='" + actual + '\'' +
                ", status=" + status +
                '}';
    }

    /**
     * Builder for AssertionResult instances.
     */
    public static final class Builder {
        private String framework;
        private String method;
        private String message;
        private String expected;
        private String actual;
        private TestStatus status;
        private String errorMessage;
        private Instant timestamp;

        private Builder() {
        }

        /**
         * Sets the assertion library label.
         * @param framework the framework (optional)
         * @return this builder
         */
        public Builder framework(String framework) {
            this.framework = framework;
            return this;
        }

        /**
         * Sets the assertion method name.
         * @param method the method name (required)
         * @return this builder
         */
        public Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the assertion message.
         * @param message the message (optional)
         * @return this builder
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the expected value display string.
         * @param expected the expected value (optional)
         * @return this builder
         */
        public Builder expected(String expected) {
            this.expected = expected;
            return this;
        }

        /**
         * Sets the actual value display string.
         * @param actual the actual value (optional)
         * @return this builder
         */
        public Builder actual(String actual) {
            this.actual = actual;
            return this;
        }

        /**
         * Sets the outcome.
         * @param status PASSED or FAILED (required)
         * @return this builder
         */
        public Builder status(TestStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the failure message.
         * @param errorMessage the error message (optional)
         * @return this builder
         */
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * Sets when the assertion finished.
         * @param timestamp the timestamp (optional)
         * @return this builder
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds the AssertionResult.
         * @return a new AssertionResult
         * @throws IllegalArgumentException if method or status is missing
         */
        public AssertionResult build() {
            if (method == null || method.isBlank()) {
                throw new IllegalArgumentException("method is required");
            }
            if (status == null) {
                throw new IllegalArgumentException("status is required");
            }
            return new AssertionResult(framework, method, message, expected, actual, status, errorMessage, timestamp);
        }
    }
}
