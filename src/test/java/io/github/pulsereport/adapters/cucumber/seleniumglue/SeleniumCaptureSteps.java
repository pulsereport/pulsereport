package io.github.pulsereport.adapters.cucumber.seleniumglue;

import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.pulsereport.adapters.selenium.SeleniumTracker;

/** Glue for {@code selenium_capture.feature}: a tracked in-memory browser. */
public class SeleniumCaptureSteps {

    private WebDriver driver;

    @Before
    public void openBrowser() {
        driver = SeleniumTracker.track(new StubBrowser());
        driver.get("https://example.test/login");
    }

    @When("I open {string}")
    public void open(String url) {
        driver.get(url);
    }

    @When("I log in")
    public void logIn() {
        SeleniumTracker.step("Log in", () -> driver.get("https://example.test/home"));
    }

    @Then("I click the missing button")
    public void clickMissing() {
        driver.findElement(By.id("missing")).click();
    }

    @After
    public void closeBrowser() {
        driver.quit();
    }

    /** Navigation, screenshots and a page without elements. */
    static final class StubBrowser implements WebDriver, TakesScreenshot {
        private String url = "about:blank";

        @Override
        public <X> X getScreenshotAs(OutputType<X> target) {
            return target.convertFromPngBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        }

        @Override
        public void get(String newUrl) {
            url = newUrl;
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
            throw new NoSuchElementException("no such element: " + by);
        }

        @Override
        public String getPageSource() {
            return "<html><body>stub</body></html>";
        }

        @Override
        public void close() {
        }

        @Override
        public void quit() {
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
    }
}
