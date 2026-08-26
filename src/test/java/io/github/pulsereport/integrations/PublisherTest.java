package io.github.pulsereport.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 * Tests for Publisher interface contract.
 */
class PublisherTest {

    @Test
    void publishException() {
        PublishException ex = new PublishException("Test error");
        assertEquals("Test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void publishExceptionWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        PublishException ex = new PublishException("Test error", cause);
        assertEquals("Test error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
