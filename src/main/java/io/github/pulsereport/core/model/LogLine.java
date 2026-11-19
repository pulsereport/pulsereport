package io.github.pulsereport.core.model;

import java.time.Instant;
import java.util.List;

/**
 * Log lines written during a test, e.g. through {@code SeleniumAdapter.log(...)}. Each is a
 * zero-length step whose keyword is its level, so it sits among the steps in the order it was
 * written. An {@link Level#ERROR} line fails the step it was written in, the steps around it,
 * and the test, even when nothing threw.
 */
public final class LogLine {

    /** How a line is shown, and whether it fails what it was written in. */
    public enum Level {
        /** Plain information. */
        INFO("Info", TestStatus.PASSED),
        /** Something worth a look that does not fail anything. */
        WARN("Warn", TestStatus.PASSED),
        /** A failure: the step, the steps around it and the test are reported as failed. */
        ERROR("Error", TestStatus.FAILED);

        private final String keyword;
        private final TestStatus status;

        Level(String keyword, TestStatus status) {
            this.keyword = keyword;
            this.status = status;
        }

        /** @return the step keyword the report shows for this level, e.g. "Warn" */
        public String keyword() {
            return keyword;
        }
    }

    private LogLine() {
    }

    /**
     * Builds the step for one line.
     *
     * @param level the line's level
     * @param message the text; null is shown as "null"
     * @param at when it was written
     * @return a zero-length step with the level as its keyword
     */
    public static TestStep step(Level level, String message, Instant at) {
        if (level == null) {
            throw new IllegalArgumentException("level cannot be null");
        }
        return TestStep.builder()
                .keyword(level.keyword)
                .name(String.valueOf(message))
                .status(level.status)
                .startTime(at)
                .endTime(at)
                .duration(0)
                .build();
    }

    /**
     * Whether a step is an error line.
     *
     * @param step any step
     * @return true for a step built by {@link #step} with {@link Level#ERROR}
     */
    public static boolean isError(TestStep step) {
        return step != null && Level.ERROR.keyword.equals(step.getKeyword()) && step.getStatus() == TestStatus.FAILED
                && step.getDuration() == 0 && step.getChildren().isEmpty();
    }

    /**
     * Finds the first error line among steps and their sub-steps.
     *
     * @param steps steps in execution order; may be null
     * @return the message of the first error line, or null when there is none
     */
    public static String firstError(List<TestStep> steps) {
        if (steps == null) {
            return null;
        }
        for (TestStep s : steps) {
            if (isError(s)) {
                return s.getName();
            }
            String inner = firstError(s.getChildren());
            if (inner != null) {
                return inner;
            }
        }
        return null;
    }

    /**
     * Reports a test that passed (or passed on retry) but wrote an error line as failed, with that
     * line as its error message. Other tests are returned unchanged.
     *
     * @param test a finished test
     * @return the test, failed when it wrote an error line
     */
    public static TestCase failIfErrorLogged(TestCase test) {
        if (test == null || (test.getStatus() != TestStatus.PASSED && test.getStatus() != TestStatus.FLAKY)) {
            return test;
        }
        String error = firstError(test.getBackgroundSteps());
        if (error == null) {
            error = firstError(test.getSteps());
        }
        return error == null ? test
                : test.toBuilder().status(TestStatus.FAILED).errorMessage("Error logged: " + error).build();
    }
}
