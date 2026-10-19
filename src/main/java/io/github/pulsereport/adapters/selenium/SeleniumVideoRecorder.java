package io.github.pulsereport.adapters.selenium;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records a browser session as a video by capturing screenshots on a
 * background thread and encoding them with ffmpeg.
 */
final class SeleniumVideoRecorder {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumVideoRecorder.class);
    private static final long ENCODE_TIMEOUT_SECONDS = 180;
    private static final long VERSION_CHECK_TIMEOUT_SECONDS = 10;
    private static final long STOP_TIMEOUT_MS = 5_000;
    private static final long MAX_FRAME_BYTES = 1L << 30;
    private static final int MAX_LOG_CHARS = 2_000;
    private static final double MIN_LAST_FRAME_SECONDS = 0.5;
    private static final int MIN_OUTPUT_FPS = 10;
    private static final Map<String, Boolean> ffmpegAvailability = new ConcurrentHashMap<>();
    // Recorders whose frames are still on disk; the JVM may exit before a test finishes
    private static final Set<SeleniumVideoRecorder> undiscarded = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> List.copyOf(undiscarded).forEach(SeleniumVideoRecorder::discard), "pulsereport-video-cleanup"));
    }

    private final TakesScreenshot camera;
    private final Lock driverLock;
    private final long intervalMs;
    private final Path frameDir;
    private final List<Long> frameTimes = new ArrayList<>();
    private final Thread worker;
    private final AtomicBoolean stopped = new AtomicBoolean();
    private volatile boolean running = true;
    private volatile long stoppedAt;
    private long frameBytes;
    private boolean discarded;

    /**
     * @param driverLock held by the test thread while it talks to the same browser session
     */
    SeleniumVideoRecorder(TakesScreenshot camera, int fps, Lock driverLock) throws IOException {
        this.camera = camera;
        this.driverLock = driverLock;
        this.intervalMs = 1000L / fps;
        this.frameDir = Files.createTempDirectory("pulsereport-video-");
        undiscarded.add(this);
        this.worker = new Thread(this::captureLoop, "pulsereport-video-recorder");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /** Checks once per path whether ffmpeg can be run, warning if not. */
    static boolean isFfmpegAvailable(String ffmpegPath) {
        return ffmpegAvailability.computeIfAbsent(ffmpegPath, path -> {
            Process process = null;
            try {
                process = new ProcessBuilder(path, "-version").redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
                if (process.waitFor(VERSION_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    return true;
                }
            } catch (IOException e) {
                // reported below
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (process != null) {
                    process.destroyForcibly();
                }
            }
            logger.warn("PulseReport: video recording is enabled but ffmpeg was not found at '{}'. "
                    + "Install ffmpeg or set reporter.video.ffmpegPath. Videos will be skipped.", path);
            return false;
        });
    }

    private void captureLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            long started = System.currentTimeMillis();
            if (!captureFrame()) {
                return;
            }
            long sleep = intervalMs - (System.currentTimeMillis() - started);
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /** @return false once recording should end */
    private boolean captureFrame() {
        try {
            // Waits at most one frame for the test's own driver command, so the two never interleave
            if (!driverLock.tryLock(intervalMs, TimeUnit.MILLISECONDS)) {
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        long takenAt;
        byte[] png;
        try {
            takenAt = System.currentTimeMillis();
            png = camera.getScreenshotAs(OutputType.BYTES);
        } catch (RuntimeException e) {
            // Browser busy, closed, or between pages: skip this frame
            logger.trace("Video frame skipped", e);
            return true;
        } finally {
            driverLock.unlock();
        }
        if (frameBytes + png.length > MAX_FRAME_BYTES) {
            logger.warn("PulseReport: video frames reached {} MB; the rest of this test is not recorded",
                    MAX_FRAME_BYTES >> 20);
            return false;
        }
        try {
            synchronized (frameTimes) {
                if (discarded) {
                    return false;
                }
                Files.write(frameDir.resolve(frameName(frameTimes.size())), png);
                frameTimes.add(takenAt);
            }
            frameBytes += png.length;
        } catch (IOException e) {
            logger.trace("Video frame not written", e);
        }
        return true;
    }

    /** Stops capturing; safe to call more than once. */
    void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        running = false;
        stoppedAt = System.currentTimeMillis();
        try {
            worker.join(STOP_TIMEOUT_MS);
            if (worker.isAlive()) {
                // Usually blocked on a screenshot from a browser that stopped answering
                worker.interrupt();
                worker.join(STOP_TIMEOUT_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (worker.isAlive()) {
            logger.debug("PulseReport: video recorder thread did not stop; its frames are ignored");
        }
    }

    /**
     * Encodes captured frames to an H.264 MP4, preserving real timing.
     *
     * @return true if the video was written
     */
    boolean encode(File output, String ffmpegPath, int fps) {
        return encode(output, ffmpegPath, fps, ENCODE_TIMEOUT_SECONDS);
    }

    boolean encode(File output, String ffmpegPath, int fps, long timeoutSeconds) {
        Path log = frameDir.resolve("ffmpeg.log");
        Process process = null;
        boolean written = false;
        try {
            Path list = writeFrameList();
            if (list == null) {
                logger.warn("PulseReport: no video frames captured; skipping {}", output.getName());
                return false;
            }
            Files.createDirectories(output.getParentFile().toPath());
            List<String> command = List.of(ffmpegPath, "-nostdin", "-y", "-loglevel", "error",
                    "-f", "concat", "-safe", "0", "-i", list.toString(),
                    "-vf", "fps=" + Math.max(MIN_OUTPUT_FPS, fps) + ",scale='min(1280,iw)':-2,format=yuv420p",
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "28",
                    "-movflags", "+faststart", output.getAbsolutePath());
            // Output goes to a file: a pipe nobody drains can block ffmpeg and defeat the timeout
            process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                logger.warn("PulseReport: ffmpeg timed out after {}s encoding {}", timeoutSeconds, output.getName());
            } else if (process.exitValue() != 0) {
                logger.warn("PulseReport: ffmpeg failed encoding {}: {}", output.getName(), readLog(log));
            } else {
                written = true;
            }
        } catch (IOException e) {
            logger.warn("PulseReport: could not run ffmpeg ('{}'): {}", ffmpegPath, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (!written) {
                deletePartialOutput(output);
            }
        }
        return written;
    }

    private static void deletePartialOutput(File output) {
        try {
            Files.deleteIfExists(output.toPath());
        } catch (IOException e) {
            logger.debug("Could not delete partial video {}", output, e);
        }
    }

    /** Deletes captured frames; safe to call more than once. */
    void discard() {
        synchronized (frameTimes) {
            if (discarded) {
                return;
            }
            discarded = true;
        }
        undiscarded.remove(this);
        try (Stream<Path> files = Files.walk(frameDir)) {
            files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            logger.debug("Could not delete video frames in {}", frameDir, e);
        }
    }

    Path frameDir() {
        return frameDir;
    }

    /** Writes the ffmpeg concat list, or returns null when no frames were captured. */
    Path writeFrameList() throws IOException {
        List<Long> times;
        synchronized (frameTimes) {
            times = new ArrayList<>(frameTimes);
        }
        return times.isEmpty() ? null : writeConcatList(times, firstContentFrame(times.size()));
    }

    private static String readLog(Path log) {
        try {
            String text = new String(Files.readAllBytes(log), StandardCharsets.UTF_8).trim();
            return text.length() <= MAX_LOG_CHARS ? text : "…" + text.substring(text.length() - MAX_LOG_CHARS);
        } catch (IOException e) {
            return "";
        }
    }

    /** The browser shows a blank tab until the first page paints; the video starts at the first frame with content. */
    private int firstContentFrame(int count) {
        for (int i = 0; i < count - 1; i++) {
            if (!isBlank(frameDir.resolve(frameName(i)))) {
                return i;
            }
        }
        return count - 1;
    }

    private static boolean isBlank(Path frame) {
        try {
            BufferedImage image = ImageIO.read(frame.toFile());
            if (image == null) {
                return false;
            }
            int color = image.getRGB(0, 0);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if (image.getRGB(x, y) != color) {
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Path writeConcatList(List<Long> times, int first) throws IOException {
        long end = stoppedAt > 0 ? stoppedAt : System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (int i = first; i < times.size(); i++) {
            double seconds = i + 1 < times.size()
                    ? (times.get(i + 1) - times.get(i)) / 1000.0
                    : Math.max(MIN_LAST_FRAME_SECONDS, (end - times.get(i)) / 1000.0);
            sb.append("file '").append(frameName(i)).append("'\n")
                    .append(String.format(Locale.ROOT, "duration %.3f%n", seconds));
        }
        // The concat demuxer ignores the last entry's duration unless the file is repeated
        sb.append("file '").append(frameName(times.size() - 1)).append("'\n");
        Path list = frameDir.resolve("frames.txt");
        Files.writeString(list, sb.toString());
        return list;
    }

    private static String frameName(int index) {
        return String.format(Locale.ROOT, "frame-%06d.png", index);
    }
}
