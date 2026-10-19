package io.github.pulsereport.adapters.selenium;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;

class SeleniumVideoRecorderTest {

    private static final byte[] BLANK = FakeBrowser.png(Color.WHITE, false);
    private static final byte[] CONTENT = FakeBrowser.png(Color.WHITE, true);

    @TempDir
    Path tempDir;

    private SeleniumVideoRecorder recorder;

    @AfterEach
    void cleanUp() {
        if (recorder != null) {
            recorder.stop();
            recorder.discard();
        }
    }

    /** Serves frames from {@code frames} by call number and counts down {@code calls} after each. */
    private static final class Camera implements TakesScreenshot {
        final AtomicInteger count = new AtomicInteger();
        final CountDownLatch calls;
        final IntFunction<byte[]> frames;

        Camera(int expectedCalls, IntFunction<byte[]> frames) {
            this.calls = new CountDownLatch(expectedCalls);
            this.frames = frames;
        }

        @Override
        public <X> X getScreenshotAs(OutputType<X> target) {
            int n = count.getAndIncrement();
            try {
                return target.convertFromPngBytes(frames.apply(n));
            } finally {
                calls.countDown();
            }
        }
    }

    private Camera recordUntil(int calls, IntFunction<byte[]> frames) throws Exception {
        Camera camera = new Camera(calls, frames);
        recorder = new SeleniumVideoRecorder(camera, 30, new ReentrantLock());
        assertTrue(camera.calls.await(5, TimeUnit.SECONDS), "recorder should keep capturing");
        recorder.stop();
        return camera;
    }

    @Test
    void videoStartsAtTheFirstFrameWithContent() throws Exception {
        recordUntil(6, n -> n < 3 ? BLANK : CONTENT);

        String list = Files.readString(recorder.writeFrameList());

        assertTrue(list.startsWith("file 'frame-000003.png'"), list);
        assertFalse(list.contains("frame-000000.png"));
    }

    @Test
    void keepsTheLastFrameWhenEveryFrameIsBlank() throws Exception {
        recordUntil(3, n -> BLANK);

        String list = Files.readString(recorder.writeFrameList());

        assertTrue(list.startsWith("file 'frame-00000"), list);
        assertEquals(2, list.lines().filter(l -> l.startsWith("file")).count(), "one frame, listed twice for ffmpeg");
    }

    @Test
    void waitsWhileTheTestThreadIsUsingTheDriver() throws Exception {
        ReentrantLock driverLock = new ReentrantLock();
        Camera camera = new Camera(1, n -> CONTENT);
        driverLock.lock();
        try {
            recorder = new SeleniumVideoRecorder(camera, 30, driverLock);
            Thread.sleep(300);
            assertEquals(0, camera.count.get(), "no screenshot while a test command is in flight");
        } finally {
            driverLock.unlock();
        }
        assertTrue(camera.calls.await(5, TimeUnit.SECONDS), "capturing resumes once the command finishes");
    }

    @Test
    void stopAndDiscardAreIdempotentAndRemoveFrames() throws Exception {
        recordUntil(2, n -> CONTENT);
        Path frames = recorder.frameDir();

        recorder.stop();
        recorder.discard();
        recorder.discard();

        assertFalse(Files.exists(frames));
    }

    @Test
    void noFramesMeansNoVideo() throws Exception {
        recordUntil(2, n -> {
            throw new WebDriverException("browser busy");
        });
        File output = tempDir.resolve("v.mp4").toFile();

        assertNull(recorder.writeFrameList());
        assertFalse(recorder.encode(output, "ffmpeg", 2));
        assertFalse(output.exists());
    }

    @Test
    void missingFfmpegIsReportedAsUnavailable() {
        assertFalse(SeleniumVideoRecorder.isFfmpegAvailable(tempDir.resolve("no-ffmpeg").toString()));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void hungFfmpegIsKilledAtTheTimeout() throws Exception {
        recordUntil(2, n -> CONTENT);
        File output = tempDir.resolve("v.mp4").toFile();
        String ffmpeg = script("hang", "sleep 30");

        long started = System.nanoTime();
        boolean written = recorder.encode(output, ffmpeg, 2, 1);
        long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started);

        assertFalse(written);
        assertTrue(seconds < 10, "encode should give up after its timeout, took " + seconds + "s");
        assertFalse(output.exists());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void failingFfmpegWritesNoVideo() throws Exception {
        recordUntil(2, n -> CONTENT);
        File output = tempDir.resolve("v.mp4").toFile();

        assertFalse(recorder.encode(output, script("fail", "echo 'bad input' >&2; exit 1"), 2));
        assertFalse(output.exists());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void chattyFfmpegCannotBlockOnItsOutput() throws Exception {
        recordUntil(2, n -> CONTENT);
        File output = tempDir.resolve("v.mp4").toFile();
        // Far more output than a pipe buffer holds, then writes the last argument (the output file)
        String ffmpeg = script("chatty", "yes log | head -c 2000000; for last; do :; done; echo ok > \"$last\"");

        assertTrue(recorder.encode(output, ffmpeg, 2, 20));
        assertTrue(output.exists());
    }

    private String script(String name, String body) throws IOException {
        Path file = tempDir.resolve(name + ".sh");
        Files.writeString(file, "#!/bin/sh\n" + body + "\n");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
        return file.toString();
    }
}
