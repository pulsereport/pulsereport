package io.github.pulsereport.adapters;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.pulsereport.core.model.Artifact;
import io.github.pulsereport.core.model.Metric;
import io.github.pulsereport.core.model.TestStep;

/**
 * Connects capture code that runs inside a test (such as the Selenium tracker)
 * to whichever adapter owns the test running on the current thread, without
 * either side depending on the other's framework.
 *
 * <p>
 * Adapters register a {@link Sink}; the TestNG adapter's sink is active while a
 * test or a configuration method ({@code @BeforeMethod}, {@code @AfterMethod},
 * ...) runs, and the Cucumber adapter's sink while a scenario runs. Capture
 * code asks {@link #current()} for the most specific active sink and records
 * steps, artifacts and metrics into it.</p>
 *
 * <p>
 * Adapters also call {@link #fireFailure(Throwable)} when a test or step fails,
 * so capture code can add its evidence (the browser lookup that failed, a
 * screenshot, the page source) to the failing context while it is still
 * open.</p>
 *
 * <p>
 * This class has no test-framework or Selenium imports, so every adapter can
 * use it whatever else is on the classpath.</p>
 *
 * @since 1.3.0
 */
public final class CaptureBridge {

    /** Priority of a sink that records into a whole test (TestNG). */
    public static final int PRIORITY_TEST = 0;
    /** Priority of a sink that records into a step of a test (Cucumber); wins over a test sink. */
    public static final int PRIORITY_STEP = 100;

    private static final Logger logger = LoggerFactory.getLogger(CaptureBridge.class);

    private static final List<Registration> sinks = new CopyOnWriteArrayList<>();
    private static final List<FailureListener> failureListeners = new CopyOnWriteArrayList<>();

    /** Where captures made on the current thread end up. */
    public interface Sink {

        /**
         * @return true when this sink has a test, step, setup or teardown
         * running on the current thread
         */
        boolean isActive();

        /** @param step a step to append to the active context */
        void addStep(TestStep step);

        /** @param artifact an artifact to attach to the active context */
        void addArtifact(Artifact artifact);

        /** @param metric a metric to record on the active test */
        void addMetric(Metric metric);

        /**
         * Names the adapter whose captures the active test contains (e.g.
         * "Selenium"), so the report can label the test. Optional.
         *
         * @param adapterName the adapter name
         */
        default void markAdapter(String adapterName) {
        }

        /**
         * Adds a tag to the active test (e.g. the browser it ran in). Optional.
         *
         * @param tag the tag
         */
        default void addTag(String tag) {
        }
    }

    /** Adds evidence to a test or step that just failed. */
    @FunctionalInterface
    public interface FailureListener {

        /**
         * @param failure what the test or step failed with
         * @param sink the failing context, still open
         */
        void onFailure(Throwable failure, Sink sink);
    }

    private record Registration(Sink sink, int priority) {
    }

    private CaptureBridge() {
    }

    /**
     * Registers a sink. Registering the same sink again has no effect.
     *
     * @param sink the sink
     * @param priority higher priorities are asked first; see {@link #PRIORITY_TEST}
     * and {@link #PRIORITY_STEP}
     */
    public static synchronized void register(Sink sink, int priority) {
        if (sink == null || sinks.stream().anyMatch(r -> r.sink() == sink)) {
            return;
        }
        sinks.add(new Registration(sink, priority));
        sinks.sort(Comparator.comparingInt(Registration::priority).reversed());
    }

    /** @param sink a previously registered sink to remove */
    public static synchronized void unregister(Sink sink) {
        sinks.removeIf(r -> r.sink() == sink);
    }

    /**
     * @return the highest-priority sink active on the current thread, or null
     * when no test is running here
     */
    public static Sink current() {
        for (Registration r : sinks) {
            try {
                if (r.sink().isActive()) {
                    return r.sink();
                }
            } catch (RuntimeException | LinkageError e) {
                logger.debug("PulseReport: capture sink {} failed", r.sink(), e);
            }
        }
        return null;
    }

    /**
     * Adds a failure listener. Adding the same listener again has no effect.
     *
     * @param listener the listener
     */
    public static synchronized void addFailureListener(FailureListener listener) {
        if (listener != null && !failureListeners.contains(listener)) {
            failureListeners.add(listener);
        }
    }

    /** @param listener a previously added listener to remove */
    public static synchronized void removeFailureListener(FailureListener listener) {
        failureListeners.remove(listener);
    }

    /**
     * Tells the failure listeners that the context active on this thread failed.
     * Listener errors are logged, never thrown.
     *
     * @param failure what the test or step failed with (ignored when null)
     */
    public static void fireFailure(Throwable failure) {
        if (failure == null || failureListeners.isEmpty()) {
            return;
        }
        Sink sink = current();
        if (sink == null) {
            return;
        }
        for (FailureListener listener : failureListeners) {
            try {
                listener.onFailure(failure, sink);
            } catch (RuntimeException | LinkageError e) {
                logger.warn("PulseReport: could not capture failure evidence: {}", e.toString());
                logger.debug("PulseReport: failure listener error", e);
            }
        }
    }

    /**
     * Whether an artifact describes the whole test rather than the step or hook
     * it was captured in. A recording covers the whole test, and the report
     * syncs steps against it only when it is attached to the test, so sinks
     * attach a recording to the test even when it arrives during a hook (e.g. a
     * video uploaded in {@code @AfterMethod} or a Cucumber {@code @After} hook).
     *
     * @param artifact the captured artifact
     * @return true when the artifact belongs on the test itself
     */
    public static boolean belongsToTest(Artifact artifact) {
        return artifact != null && "video".equalsIgnoreCase(artifact.getType());
    }
}
