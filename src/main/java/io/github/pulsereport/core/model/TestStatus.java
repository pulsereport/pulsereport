package io.github.pulsereport.core.model;

/**
 * Enum representing the status of a test execution.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public enum TestStatus {
    /**
     * Test passed successfully.
     */
    PASSED,
    
    /**
     * Test failed.
     */
    FAILED,
    
    /**
     * Test was skipped.
     */
    SKIPPED,
    
    /**
     * Test passed after one or more retries (flaky test).
     */
    FLAKY
}
