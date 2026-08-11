package io.github.pulsereport.adapters.appium;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Thread-safe holder that associates the active mobile driver with the
 * current test thread.
 *
 * <p>Mobile tests register their driver in {@code @BeforeMethod} (or right
 * after creating it) so that the {@link AppiumAdapter} can automatically
 * capture failure screenshots, page source, and other driver-derived data at
 * the moment a test fails — without the test code needing to pass the driver
 * around.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @BeforeMethod
 * public void setUp() {
 *     driver = new AndroidDriver(new URL("http://localhost:4723"), caps);
 *     MobileDriverHolder.set(driver);   // register for automatic capture
 * }
 *
 * @AfterMethod(alwaysRun = true)
 * public void tearDown() {
 *     MobileDriverHolder.remove();      // always clean up the ThreadLocal
 *     driver.quit();
 * }
 * }</pre>
 *
 * <p>The holder is thread-safe and suitable for parallel mobile execution —
 * each thread holds its own driver reference.</p>
 *
 * <p><b>Note:</b> The holder stores the driver as a plain
 * {@link WebDriver} to avoid a hard compile-time dependency on the Appium
 * client in this class. Callers may register any Appium driver
 * (AndroidDriver, IOSDriver, etc.) since they all implement WebDriver.</p>
 *
 * @author Pulse Report Team
 * @since 1.1.0
 */
public final class MobileDriverHolder {

    private static final Logger logger = LoggerFactory.getLogger(MobileDriverHolder.class);

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private MobileDriverHolder() {
        // utility class
    }

    /**
     * Registers the driver for the current thread.
     *
     * @param driver the active mobile driver (must not be null)
     */
    public static void set(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver cannot be null");
        }
        DRIVER.set(driver);
        logger.debug("Registered mobile driver for thread '{}'", Thread.currentThread().getName());
    }

    /**
     * Returns the driver registered for the current thread, if any.
     *
     * @return an Optional containing the driver, or empty if none registered
     */
    public static Optional<WebDriver> get() {
        return Optional.ofNullable(DRIVER.get());
    }

    /**
     * Removes the driver for the current thread. Should always be called in
     * teardown to prevent memory leaks in thread-pooled (parallel) execution.
     */
    public static void remove() {
        DRIVER.remove();
        logger.debug("Removed mobile driver for thread '{}'", Thread.currentThread().getName());
    }
}
