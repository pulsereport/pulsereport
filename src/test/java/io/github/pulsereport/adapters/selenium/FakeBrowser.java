package io.github.pulsereport.adapters.selenium;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

/**
 * In-memory WebDriver for adapter tests: navigation with page timing, elements
 * that can be clicked and typed into, screenshots, capabilities, and a quit
 * that can fail. Answers the tracker's scripts the way a browser would.
 */
final class FakeBrowser implements WebDriver, TakesScreenshot, JavascriptExecutor, HasCapabilities {

    final AtomicInteger screenshots = new AtomicInteger();
    final List<String> typed = new CopyOnWriteArrayList<>();
    final Map<String, FakeElement> elements = new ConcurrentHashMap<>();
    volatile boolean failQuit;
    volatile String pageSource = "<html><body>fake</body></html>";
    volatile String title = "Fake page";
    volatile Capabilities capabilities = new ImmutableCapabilities("browserName", "chrome",
            "browserVersion", "131.0.6778.86", "platformName", "mac");
    private volatile String url = "about:blank";
    private volatile double timeOrigin = 1_000;

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

    /** Adds an element that {@code findElement(by)} returns. */
    FakeElement element(By by, String tag, String label, String type) {
        FakeElement element = new FakeElement(this, tag, label, type);
        element.setId("el-" + elements.size());
        elements.put(by.toString(), element);
        return element;
    }

    void navigate(String newUrl) {
        url = newUrl;
        timeOrigin += 1_000; // a new document
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) {
        screenshots.incrementAndGet();
        return target.convertFromPngBytes(png(Color.WHITE, true));
    }

    @Override
    public Capabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public void get(String newUrl) {
        navigate(newUrl);
    }

    @Override
    public String getCurrentUrl() {
        return url;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public List<WebElement> findElements(By by) {
        FakeElement element = elements.get(by.toString());
        return element == null ? List.of() : List.of(element);
    }

    @Override
    public WebElement findElement(By by) {
        FakeElement element = elements.get(by.toString());
        if (element == null) {
            throw new NoSuchElementException("no such element: " + by);
        }
        return element;
    }

    @Override
    public String getPageSource() {
        return pageSource;
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
        if (script.contains("e.tagName") && args.length == 1 && args[0] instanceof FakeElement) {
            FakeElement e = (FakeElement) args[0];
            return new ArrayList<>(List.of(e.tag, e.label, e.type, url, timeOrigin, e.autocomplete, e.name, e.htmlId));
        }
        if (script.contains("getEntriesByType('navigation')")) {
            return new ArrayList<>(List.of(url, timeOrigin, 420L, 80L, title));
        }
        if (script.contains("document.title, window.innerWidth")) {
            return new ArrayList<>(List.of(url, title, 1280L, 720L));
        }
        return null;
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        return null;
    }

    /** An element of the fake page. */
    static final class FakeElement extends RemoteWebElement {
        final FakeBrowser browser;
        final String tag;
        final String label;
        final String type;
        volatile String autocomplete = "";
        volatile String name = "";
        volatile String htmlId = "";
        volatile String navigatesTo;
        volatile boolean clickFails;

        FakeElement(FakeBrowser browser, String tag, String label, String type) {
            this.browser = browser;
            this.tag = tag;
            this.label = label;
            this.type = type;
        }

        @Override
        public void click() {
            if (clickFails) {
                throw new ElementClickInterceptedException("element click intercepted");
            }
            if (navigatesTo != null) {
                browser.navigate(navigatesTo);
            }
        }

        @Override
        public void submit() {
            click();
        }

        @Override
        public void sendKeys(CharSequence... keysToSend) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence cs : keysToSend) {
                sb.append(cs);
            }
            browser.typed.add(sb.toString());
        }

        @Override
        public void clear() {
            // nothing to clear
        }

        @Override
        public String getTagName() {
            return tag;
        }
    }
}
