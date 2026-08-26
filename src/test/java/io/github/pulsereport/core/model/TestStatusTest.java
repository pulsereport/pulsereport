package io.github.pulsereport.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestStatus enum.
 */
class TestStatusTest {

    @Test
    void enumValues() {
        assertEquals(4, TestStatus.values().length);
        assertNotNull(TestStatus.valueOf("PASSED"));
        assertNotNull(TestStatus.valueOf("FAILED"));
        assertNotNull(TestStatus.valueOf("SKIPPED"));
        assertNotNull(TestStatus.valueOf("FLAKY"));
    }

    @Test
    void passedStatus() {
        TestStatus status = TestStatus.PASSED;
        assertEquals("PASSED", status.name());
    }

    @Test
    void failedStatus() {
        TestStatus status = TestStatus.FAILED;
        assertEquals("FAILED", status.name());
    }

    @Test
    void skippedStatus() {
        TestStatus status = TestStatus.SKIPPED;
        assertEquals("SKIPPED", status.name());
    }

    @Test
    void flakyStatus() {
        TestStatus status = TestStatus.FLAKY;
        assertEquals("FLAKY", status.name());
    }
}
