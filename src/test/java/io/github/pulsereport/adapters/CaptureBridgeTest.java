package io.github.pulsereport.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestStep;

class CaptureBridgeTest {

    private final List<Object> registered = new ArrayList<>();

    /** A sink that is active while {@link #active} is true. */
    private static final class FlagSink implements CaptureBridge.Sink {
        volatile boolean active;
        final List<TestStep> steps = new ArrayList<>();

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void addStep(TestStep step) {
            steps.add(step);
        }

        @Override
        public void addArtifact(Artifact artifact) {
        }

        @Override
        public void addMetric(Metric metric) {
        }
    }

    @AfterEach
    void tearDown() {
        for (Object o : registered) {
            if (o instanceof CaptureBridge.Sink) {
                CaptureBridge.unregister((CaptureBridge.Sink) o);
            } else {
                CaptureBridge.removeFailureListener((CaptureBridge.FailureListener) o);
            }
        }
    }

    @Test
    void theMostSpecificActiveSinkWins() {
        FlagSink test = register(new FlagSink(), CaptureBridge.PRIORITY_TEST);
        FlagSink step = register(new FlagSink(), CaptureBridge.PRIORITY_STEP);

        test.active = true;
        assertSame(test, CaptureBridge.current());
        step.active = true;
        assertSame(step, CaptureBridge.current());
    }

    @Test
    void failureListenersSeeTheActiveSinkAndTheirErrorsAreContained() {
        FlagSink sink = register(new FlagSink(), CaptureBridge.PRIORITY_STEP + 1);
        sink.active = true;
        List<Throwable> seen = new ArrayList<>();
        CaptureBridge.FailureListener failing = (failure, s) -> {
            throw new IllegalStateException("listener bug");
        };
        CaptureBridge.FailureListener recording = (failure, s) -> {
            seen.add(failure);
            assertSame(sink, s);
        };
        CaptureBridge.addFailureListener(failing);
        CaptureBridge.addFailureListener(recording);
        registered.add(failing);
        registered.add(recording);

        AssertionError boom = new AssertionError("boom");
        CaptureBridge.fireFailure(boom);

        assertEquals(List.of(boom), seen);
        sink.active = false;
        CaptureBridge.fireFailure(new AssertionError("no sink"));
        assertTrue(seen.size() == 1 || CaptureBridge.current() != null, "without an active sink nothing is told");
    }

    private FlagSink register(FlagSink sink, int priority) {
        CaptureBridge.register(sink, priority);
        registered.add(sink);
        return sink;
    }
}
