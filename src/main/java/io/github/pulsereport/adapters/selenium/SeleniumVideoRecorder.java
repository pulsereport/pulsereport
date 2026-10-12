package io.github.pulsereport.adapters.selenium;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
    private static final double MIN_LAST_FRAME_SECONDS = 0.5;
    private static final int MIN_OUTPUT_FPS = 10;
    private static final Map<String, Boolean> ffmpegAvailability = new ConcurrentHashMap<>();

    private final TakesScreenshot camera;
    private final long intervalMs;
    private final Path frameDir;
    private final List<Long> frameTimes = new ArrayList<>();
    private final Thread worker;
    private volatile boolean running = true;
    private long stoppedAt;

    SeleniumVideoRecorder(TakesScreenshot camera, int fps) throws IOException {
        this.camera = camera;
        this.intervalMs = 1000L / fps;
        this.frameDir = Files.createTempDirectory("pulsereport-video-");
        this.worker = new Thread(this::captureLoop, "pulsereport-video-recorder");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /** Checks once per path whether ffmpeg can be run, warning if not. */
    static boolean isFfmpegAvailable(String ffmpegPath) {
        return ffmpegAvailability.computeIfAbsent(ffmpegPath, path -> {
            try {
                Process process = new ProcessBuilder(path, "-version").redirectErrorStream(true).start();
                process.getInputStream().transferTo(OutputStream.nullOutputStream());
                if (process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    return true;
                }
            } catch (IOException e) {
                // reported below
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logger.warn("PulseReport: video recording is enabled but ffmpeg was not found at '{}'. "
                    + "Install ffmpeg or set reporter.video.ffmpegPath. Videos will be skipped.", path);
            return false;
        });
    }

    private void captureLoop() {
        while (running) {
            long started = System.currentTimeMillis();
            try {
                byte[] png = camera.getScreenshotAs(OutputType.BYTES);
                synchronized (frameTimes) {
                    Files.write(frameDir.resolve(frameName(frameTimes.size())), png);
                    frameTimes.add(started);
                }
            } catch (IOException | RuntimeException e) {
                // Browser busy, closed, or between pages: skip this frame
                logger.trace("Video frame skipped", e);
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

    /** Stops capturing; safe to call more than once. */
    void stop() {
        if (!running) {
            return;
        }
        running = false;
        stoppedAt = System.currentTimeMillis();
        try {
            worker.join(5_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Encodes captured frames to an H.264 MP4, preserving real timing.
     *
     * @return true if the video was written
     */
    boolean encode(File output, String ffmpegPath, int fps) {
        List<Long> times;
        synchronized (frameTimes) {
            times = new ArrayList<>(frameTimes);
        }
        if (times.isEmpty()) {
            logger.warn("PulseReport: no video frames captured; skipping {}", output.getName());
            return false;
        }
        try {
            Path list = writeConcatList(times);
            Files.createDirectories(output.getParentFile().toPath());
            List<String> command = List.of(ffmpegPath, "-nostdin", "-y", "-loglevel", "error",
                    "-f", "concat", "-safe", "0", "-i", list.toString(),
                    "-vf", "fps=" + Math.max(MIN_OUTPUT_FPS, fps) + ",scale='min(1280,iw)':-2,format=yuv420p",
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "28",
                    "-movflags", "+faststart", output.getAbsolutePath());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String ffmpegOutput;
            try (InputStream in = process.getInputStream()) {
                ffmpegOutput = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!process.waitFor(ENCODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                logger.warn("PulseReport: ffmpeg timed out encoding {}", output.getName());
                return false;
            }
            if (process.exitValue() != 0) {
                logger.warn("PulseReport: ffmpeg failed encoding {}: {}", output.getName(), ffmpegOutput.trim());
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.warn("PulseReport: could not run ffmpeg ('{}'): {}", ffmpegPath, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Deletes captured frames. */
    void discard() {
        try (Stream<Path> files = Files.walk(frameDir)) {
            files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            logger.debug("Could not delete video frames in {}", frameDir, e);
        }
    }

    private Path writeConcatList(List<Long> times) throws IOException {
        long end = stoppedAt > 0 ? stoppedAt : System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times.size(); i++) {
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
