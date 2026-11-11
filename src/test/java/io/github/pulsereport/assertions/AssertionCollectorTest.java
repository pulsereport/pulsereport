package io.github.pulsereport.assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.github.pulsereport.core.model.AssertionResult;
import io.github.pulsereport.core.model.TestStatus;

/** Nested collectors and collectors shared with threads started inside a step. */
class AssertionCollectorTest {

    @AfterEach
    void closeEverything() {
        while (AssertionCollector.isActive()) {
            AssertionCollector.stop();
        }
    }

    @Test
    void nestedCollectorTakesItsOwnAssertionsThenTheOuterOneResumes() {
        Object test = AssertionCollector.open();
        AssertionCollector.add(result("before"));
        Object step = AssertionCollector.open();
        AssertionCollector.add(result("inside"));
        List<AssertionResult> inner = AssertionCollector.close(step);
        AssertionCollector.add(result("after"));
        List<AssertionResult> outer = AssertionCollector.close(test);

        // Checked after closing: with the agent installed, these assertions would be captured too
        assertEquals(List.of("inside"), methods(inner));
        assertEquals(List.of("before", "after"), methods(outer));
        assertFalse(AssertionCollector.isActive());
    }

    @Test
    void closingACollectorAlsoClosesOnesLeftOpenInsideIt() {
        Object test = AssertionCollector.open();
        AssertionCollector.open(); // a step that never reported finishing
        AssertionCollector.close(test);

        assertFalse(AssertionCollector.isActive());
    }

    @Test
    void threadStartedInsideAStepReportsToIt() throws Exception {
        Object step = AssertionCollector.open();
        Thread worker = new Thread(() -> AssertionCollector.add(result("worker")));
        worker.start();
        worker.join();

        assertEquals(List.of("worker"), methods(AssertionCollector.close(step)));
    }

    @Test
    void threadStopsReportingOnceTheStepCloses() throws Exception {
        Object step = AssertionCollector.open();
        Object[] lateActive = new Object[1];
        Thread worker = new Thread(() -> {
            synchronized (lateActive) {
                while (lateActive[0] == null) {
                    try {
                        lateActive.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            lateActive[0] = AssertionCollector.isActive();
            AssertionCollector.add(result("late"));
        });
        worker.start();
        List<AssertionResult> captured = AssertionCollector.close(step);
        synchronized (lateActive) {
            lateActive[0] = "go";
            lateActive.notifyAll();
        }
        worker.join();

        assertTrue(captured.isEmpty());
        assertEquals(Boolean.FALSE, lateActive[0]);
    }

    @Test
    void sharedForkJoinThreadsDoNotReport() {
        Object step = AssertionCollector.open();
        ForkJoinPool pool = new ForkJoinPool(1); // its worker is created inside the step
        try {
            pool.submit(() -> AssertionCollector.add(result("pooled"))).join();
        } finally {
            pool.shutdown();
        }

        assertTrue(AssertionCollector.close(step).isEmpty());
    }

    @Test
    void stopClosesTheInnermostCollector() {
        AssertionCollector.start();
        AssertionCollector.start();
        AssertionCollector.add(result("inner"));
        assertEquals(List.of("inner"), methods(AssertionCollector.stop()));
        assertTrue(AssertionCollector.isActive());
        AssertionCollector.stop();
        assertTrue(AssertionCollector.stop().isEmpty(), "nothing left to close");
    }

    private static AssertionResult result(String method) {
        return AssertionResult.builder().framework("test").method(method).status(TestStatus.PASSED)
                .timestamp(Instant.now()).build();
    }

    private static List<String> methods(List<AssertionResult> results) {
        return results.stream().map(AssertionResult::getMethod).toList();
    }
}
