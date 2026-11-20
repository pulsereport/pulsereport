package io.github.pulsereport.core.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogLineTest {

    private static final Instant AT = Instant.parse("2026-11-19T12:00:00Z");

    @Test
    void aLineIsAZeroLengthStepNamedByItsLevel() {
        TestStep line = LogLine.step(LogLine.Level.WARN, "Slow response", AT);

        assertEquals("Warn", line.getKeyword());
        assertEquals("Slow response", line.getName());
        assertEquals(TestStatus.PASSED, line.getStatus());
        assertEquals(0, line.getDuration());
        assertEquals(TestStatus.FAILED, LogLine.step(LogLine.Level.ERROR, "x", AT).getStatus());
        assertEquals("null", LogLine.step(LogLine.Level.INFO, null, AT).getName());
    }

    @Test
    void theFirstErrorIsFoundInSubSteps() {
        TestStep inner = step("Submit", TestStatus.FAILED, LogLine.step(LogLine.Level.ERROR, "Service answered 500", AT));
        TestStep outer = step("Log in", TestStatus.FAILED, LogLine.step(LogLine.Level.WARN, "Slow", AT), inner);

        assertEquals("Service answered 500", LogLine.firstError(List.of(outer)));
        assertNull(LogLine.firstError(List.of(step("Open", TestStatus.PASSED, LogLine.step(LogLine.Level.INFO, "ok", AT)))));
        assertNull(LogLine.firstError(null));
    }

    @Test
    void onlyErrorLinesCountAsErrors() {
        assertTrue(LogLine.isError(LogLine.step(LogLine.Level.ERROR, "x", AT)));
        assertFalse(LogLine.isError(LogLine.step(LogLine.Level.WARN, "x", AT)));
        // A failed step that happens to use the keyword is not a log line
        TestStep realStep = TestStep.builder().keyword("Error").name("x").status(TestStatus.FAILED)
                .startTime(AT).endTime(AT.plusMillis(5)).duration(5).build();
        assertFalse(LogLine.isError(realStep));
    }

    @Test
    void aPassedTestThatWroteAnErrorLineIsFailed() {
        TestCase passed = test(TestStatus.PASSED, step("Clean up", TestStatus.FAILED,
                LogLine.step(LogLine.Level.ERROR, "Cleanup failed", AT)));

        TestCase result = LogLine.failIfErrorLogged(passed);

        assertEquals(TestStatus.FAILED, result.getStatus());
        assertEquals("Error logged: Cleanup failed", result.getErrorMessage());
        assertEquals(TestStatus.FAILED,
                LogLine.failIfErrorLogged(test(TestStatus.FLAKY, LogLine.step(LogLine.Level.ERROR, "x", AT))).getStatus());
    }

    @Test
    void otherTestsAreLeftAsTheyAre() {
        TestCase warned = test(TestStatus.PASSED, LogLine.step(LogLine.Level.WARN, "Slow", AT));
        TestCase skipped = test(TestStatus.SKIPPED, LogLine.step(LogLine.Level.ERROR, "x", AT));

        assertSame(warned, LogLine.failIfErrorLogged(warned));
        assertSame(skipped, LogLine.failIfErrorLogged(skipped));
    }

    private static TestStep step(String name, TestStatus status, TestStep... children) {
        return TestStep.builder().name(name).status(status).startTime(AT).endTime(AT.plusMillis(10)).duration(10)
                .children(List.of(children)).build();
    }

    private static TestCase test(TestStatus status, TestStep... steps) {
        return TestCase.builder().id("suite.Test.login").name("login").className("Test").methodName("login")
                .startTime(AT).endTime(AT.plusMillis(100)).duration(100).status(status).steps(List.of(steps)).build();
    }
}
