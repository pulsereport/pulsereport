package io.github.pulsereport.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestStatus enum.
 */
class TestStatusTest {

    @Test
    void testEnumValues() {
        assertEquals(4, TestStatus.values().length);
        assertNotNull(TestStatus.valueOf("PASSED"));
        assertNotNull(TestStatus.valueOf("FAILED"));
        assertNotNull(TestStatus.valueOf("SKIPPED"));
        assertNotNull(TestStatus.valueOf("FLAKY"));
    }

    @Test
    void testPassedStatus() {
        TestStatus status = TestStatus.PASSED;
        assertEquals("PASSED", status.name());
    }

    @Test
    void testFailedStatus() {
        TestStatus status = TestStatus.FAILED;
        assertEquals("FAILED", status.name());
    }

    @Test
    void testSkippedStatus() {
        TestStatus status = TestStatus.SKIPPED;
        assertEquals("SKIPPED", status.name());
    }

    @Test
    void testFlakyStatus() {
        TestStatus status = TestStatus.FLAKY;
        assertEquals("FLAKY", status.name());
    }
}
