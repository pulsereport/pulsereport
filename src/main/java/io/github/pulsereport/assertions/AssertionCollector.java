package io.github.pulsereport.assertions;

import java.util.ArrayList;
import java.util.List;

import io.github.pulsereport.core.model.AssertionResult;

/**
 * Per-thread buffer that receives assertions captured by {@link AssertionCapture}.
 *
 * <p>An adapter calls {@link #start()} when a step begins and {@link #stop()} when
 * it ends; every assertion evaluated on that thread in between is returned by
 * {@code stop()}. Assertions evaluated while no collector is open (for example in
 * hooks) are ignored at near-zero cost.</p>
 */
public final class AssertionCollector {

    private static final ThreadLocal<List<AssertionResult>> CURRENT = new ThreadLocal<>();

    private AssertionCollector() {
    }

    /** Opens a fresh collector on the current thread, discarding any previous one. */
    public static void start() {
        CURRENT.set(new ArrayList<>());
    }

    /**
     * Closes the current thread's collector.
     *
     * @return the assertions captured since {@link #start()}, in execution order;
     * empty when nothing was captured or no collector was open
     */
    public static List<AssertionResult> stop() {
        List<AssertionResult> captured = CURRENT.get();
        CURRENT.remove();
        return captured != null ? captured : new ArrayList<>();
    }

    /**
     * @return true if a collector is open on the current thread
     */
    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    static void add(AssertionResult result) {
        List<AssertionResult> captured = CURRENT.get();
        if (captured != null) {
            captured.add(result);
        }
    }
}
