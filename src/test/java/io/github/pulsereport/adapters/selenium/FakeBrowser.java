package io.github.pulsereport.adapters.selenium;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/** In-memory WebDriver for adapter tests: navigation, screenshots and a quit that can fail. */
final class FakeBrowser implements WebDriver, TakesScreenshot, JavascriptExecutor {

    final AtomicInteger screenshots = new AtomicInteger();
    volatile boolean failQuit;
    private volatile String url = "about:blank";

    static byte[] png(Color color, boolean withDot) {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        if (withDot) {
            image.setRGB(4, 4, Color.BLACK.getRGB());
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) {
        screenshots.incrementAndGet();
        return target.convertFromPngBytes(png(Color.WHITE, true));
    }

    @Override
    public void get(String url) {
        this.url = url;
    }

    @Override
    public String getCurrentUrl() {
        return url;
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public List<WebElement> findElements(By by) {
        return List.of();
    }

    @Override
    public WebElement findElement(By by) {
        throw new NoSuchElementException(by.toString());
    }

    @Override
    public String getPageSource() {
        return "";
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public void quit() {
        if (failQuit) {
            throw new WebDriverException("browser stopped responding");
        }
    }

    @Override
    public Set<String> getWindowHandles() {
        return Set.of("main");
    }

    @Override
    public String getWindowHandle() {
        return "main";
    }

    @Override
    public TargetLocator switchTo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Navigation navigate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Options manage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object executeScript(String script, Object... args) {
        return null;
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        return null;
    }
}
