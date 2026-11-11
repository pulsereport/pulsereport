package io.github.pulsereport.assertions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ForkJoinWorkerThread;

import io.github.pulsereport.core.model.AssertionResult;

/**
 * Per-thread buffers that receive assertions captured by {@link AssertionCapture}.
 *
 * <p>Adapters open a collector when a step, hook or test begins and close it when
 * it ends; every assertion evaluated in between is returned on close. Collectors
 * nest: one opened inside another (a tracked sub-step inside a Cucumber step, a
 * Cucumber step inside a TestNG test) receives the assertions until it closes, then
 * the outer one resumes. Assertions evaluated while no collector is open are
 * ignored at near-zero cost.</p>
 *
 * <p>A thread started while a collector is open (directly, or by an executor
 * created inside the step) reports to that collector until it closes. Threads of
 * shared fork/join pools (parallel streams, {@code CompletableFuture}'s default
 * executor) are not counted: they outlive the step and serve other tests.</p>
 */
public final class AssertionCollector {

    /** One open collector. */
    private static final class Frame {
        final List<AssertionResult> results = Collections.synchronizedList(new ArrayList<>());
        final Thread owner = Thread.currentThread();
        volatile boolean open = true;
    }

    private static final InheritableThreadLocal<Deque<Frame>> FRAMES = new InheritableThreadLocal<>() {
        @Override
        protected Deque<Frame> childValue(Deque<Frame> parent) {
            // A new thread reports to the collector open in the thread that started it
            Deque<Frame> child = new ArrayDeque<>();
            Frame top = parent == null ? null : parent.peek();
            if (top != null && top.open) {
                child.push(top);
            }
            return child;
        }
    };

    private AssertionCollector() {
    }

    /**
     * Opens a collector on the current thread, nested in any that is already open.
     *
     * @return a token for {@link #close(Object)}
     */
    public static Object open() {
        Deque<Frame> frames = FRAMES.get();
        if (frames == null) {
            frames = new ArrayDeque<>();
            FRAMES.set(frames);
        }
        Frame frame = new Frame();
        frames.push(frame);
        return frame;
    }

    /**
     * Closes the collector opened with {@code token}, and any opened inside it that
     * were left open.
     *
     * @param token the value returned by {@link #open()}
     * @return the assertions captured by that collector, in execution order
     */
    public static List<AssertionResult> close(Object token) {
        if (!(token instanceof Frame)) {
            return new ArrayList<>();
        }
        Frame frame = (Frame) token;
        Deque<Frame> frames = FRAMES.get();
        if (frames != null && frames.contains(frame)) {
            Frame popped;
            do {
                popped = frames.pop();
                popped.open = false;
            } while (popped != frame);
            if (frames.isEmpty()) {
                FRAMES.remove();
            }
        }
        frame.open = false;
        synchronized (frame.results) {
            return new ArrayList<>(frame.results);
        }
    }

    /** Opens a collector on the current thread. Same as {@link #open()}, for callers that close with {@link #stop()}. */
    public static void start() {
        open();
    }

    /**
     * Closes the innermost collector this thread opened.
     *
     * @return the assertions it captured, in execution order; empty when nothing
     * was captured or no collector was open
     */
    public static List<AssertionResult> stop() {
        Deque<Frame> frames = FRAMES.get();
        Frame top = frames == null ? null : frames.peek();
        if (top == null || top.owner != Thread.currentThread()) {
            return new ArrayList<>();
        }
        return close(top);
    }

    /**
     * @return true if a collector is open for the current thread
     */
    public static boolean isActive() {
        return current() != null;
    }

    static void add(AssertionResult result) {
        Frame frame = current();
        if (frame == null) {
            return;
        }
        if (frame.owner != Thread.currentThread() && Thread.currentThread() instanceof ForkJoinWorkerThread) {
            return;
        }
        frame.results.add(result);
    }

    private static Frame current() {
        Deque<Frame> frames = FRAMES.get();
        if (frames == null) {
            return null;
        }
        // Collectors inherited from a parent thread close without this thread knowing
        while (!frames.isEmpty() && !frames.peek().open) {
            frames.pop();
        }
        return frames.peek();
    }
}
