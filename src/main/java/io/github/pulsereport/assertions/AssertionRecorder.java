package io.github.pulsereport.assertions;

/**
 * Runtime entry point invoked by the bytecode that {@link AssertionCapture} weaves
 * into assertion libraries. Not intended to be called directly.
 *
 * <p>Libraries call their own overloads internally (TestNG's
 * {@code assertTrue(boolean)} delegates to {@code assertTrue(boolean, String)},
 * JUnit 4's {@code assertThat} delegates to Hamcrest), so a per-thread depth
 * counter ensures only the outermost call — the one the user wrote — is recorded.
 * Recording happens while the depth is still raised, so assertions triggered by a
 * value's {@code toString()} are not recorded either.</p>
 *
 * <p>This class must never throw: any failure here would surface as a failure of
 * the user's assertion.</p>
 */
public final class AssertionRecorder {

    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private AssertionRecorder() {
    }

    /**
     * Marks entry into an instrumented assertion method.
     *
     * @return true if this is the outermost assertion call on the current thread
     */
    public static boolean enter() {
        return DEPTH.get()[0]++ == 0;
    }

    /**
     * Marks exit from an instrumented assertion method and records it when it was
     * the outermost call and a collector is open.
     *
     * @param outermost value returned by the matching {@link #enter()}
     * @param owner declaring class name
     * @param method method name
     * @param descriptor JVM method descriptor
     * @param args the call's arguments
     * @param returned the return value, or null for void methods
     * @param thrown the throwable raised by the assertion, or null if it passed
     */
    public static void exit(boolean outermost, String owner, String method, String descriptor,
                            Object[] args, Object returned, Throwable thrown) {
        int[] depth = DEPTH.get();
        try {
            if (outermost && AssertionCollector.isActive()) {
                AssertionCollector.add(AssertionInterpreter.interpret(owner, method, descriptor, args, returned, thrown));
            }
        } catch (Throwable ignored) {
            // Never let reporting break the user's assertion.
        } finally {
            depth[0] = Math.max(0, depth[0] - 1);
        }
    }
}
