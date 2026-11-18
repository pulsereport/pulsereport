package io.github.pulsereport.adapters.appium;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.proxy.ElementAwareWebDriverListener;
import io.appium.java_client.proxy.Helpers;
import io.github.pulsereport.adapters.selenium.SeleniumTracker;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Reports the calls made on an Appium proxy driver, and on the elements it
 * returns, as report steps. Extending {@link ElementAwareWebDriverListener}
 * makes every returned element a proxy that reports to this listener too.
 *
 * <p>Kept apart from {@link AppiumAdapter} so that it, and the java-client 10
 * classes it needs, load only when {@link AppiumAdapter#createDriver} is used.</p>
 */
final class AppiumStepReporter extends ElementAwareWebDriverListener {

    private static final Logger logger = LoggerFactory.getLogger(AppiumStepReporter.class);

    private final ThreadLocal<Boolean> reporting = ThreadLocal.withInitial(() -> false);
    private volatile WebDriverListener steps;
    private volatile RemoteWebDriver driver;
    /** For a driver attached to an existing session: that session's capabilities, which the attached one lacks. */
    private volatile Capabilities sessionCapabilities;

    static <T extends WebDriver> T createDriver(Class<T> driverClass, Object... args) {
        if (driverClass == null) {
            throw new IllegalArgumentException("driverClass cannot be null");
        }
        return proxy(driverClass, args == null ? new Object[0] : args, null);
    }

    /**
     * A tracked driver of the same class on {@code existing}'s session. It is built with
     * java-client's attach constructor, so no new session is started and {@code existing}
     * keeps working.
     */
    @SuppressWarnings("unchecked")
    static <T extends WebDriver> T trackDriver(T existing) {
        if (existing == null) {
            throw new IllegalArgumentException("driver cannot be null");
        }
        Class<?> type = existing.getClass();
        if (type != AndroidDriver.class && type != IOSDriver.class && type != AppiumDriver.class) {
            throw new IllegalArgumentException("trackDriver takes an AndroidDriver, IOSDriver or AppiumDriver, not "
                    + type.getName() + " (a driver from createDriver is tracked already)");
        }
        AppiumDriver appium = (AppiumDriver) existing;
        if (appium.getSessionId() == null) {
            throw new IllegalStateException("The driver has quit; track it before quitting");
        }
        Capabilities caps = appium.getCapabilities();
        URL session = sessionAddress(appium.getRemoteAddress(), appium.getSessionId().toString());
        Object automation = caps.getCapability("appium:automationName") != null
                ? caps.getCapability("appium:automationName") : caps.getCapability("automationName");
        Object[] args;
        if (type == IOSDriver.class) {
            args = new Object[] {session};
        } else if (type == AndroidDriver.class) {
            args = new Object[] {session, automation == null ? "UiAutomator2" : automation.toString()};
        } else {
            args = new Object[] {session, String.valueOf(caps.getCapability("platformName")), String.valueOf(automation)};
        }
        return proxy((Class<T>) type, args, caps);
    }

    private static URL sessionAddress(URL server, String sessionId) {
        String base = server.toString().replaceAll("/+$", "");
        try {
            return new URL(base + "/session/" + sessionId);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Cannot build the session address from " + base, e);
        }
    }

    private static <T extends WebDriver> T proxy(Class<T> driverClass, Object[] args, Capabilities attachedTo) {
        Constructor<?> constructor = constructorFor(driverClass, args);
        AppiumStepReporter reporter = new AppiumStepReporter();
        reporter.sessionCapabilities = attachedTo;
        T driver = Helpers.createProxy(driverClass, args, constructor.getParameterTypes(), List.of(reporter));
        if (driver instanceof RemoteWebDriver) {
            reporter.driver = (RemoteWebDriver) driver;
        }
        Map<String, String> device = driver instanceof HasCapabilities
                ? MobileSessionMetadata.fromCapabilities(((HasCapabilities) driver).getCapabilities()).toTestDetails()
                : Map.of();
        reporter.steps = SeleniumTracker.trackIntercepted(driver, AppiumAdapter.ADAPTER_NAME, device);
        return driver;
    }

    /** The public constructor that takes these arguments; the most specific one when several do. */
    static Constructor<?> constructorFor(Class<?> type, Object[] args) {
        List<Constructor<?>> matching = new ArrayList<>();
        for (Constructor<?> c : type.getConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length == args.length && IntStream.range(0, args.length)
                    .allMatch(i -> args[i] == null ? !params[i].isPrimitive() : params[i].isInstance(args[i]))) {
                matching.add(c);
            }
        }
        for (Constructor<?> candidate : matching) {
            if (matching.stream().allMatch(other -> atLeastAsSpecific(candidate, other))) {
                return candidate;
            }
        }
        String types = Arrays.stream(args).map(a -> a == null ? "null" : a.getClass().getSimpleName())
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException((matching.isEmpty() ? "No" : "More than one") + " public constructor of "
                + type.getName() + " takes (" + types + ")");
    }

    private static boolean atLeastAsSpecific(Constructor<?> a, Constructor<?> b) {
        Class<?>[] pa = a.getParameterTypes();
        Class<?>[] pb = b.getParameterTypes();
        return IntStream.range(0, pa.length).allMatch(i -> pb[i].isAssignableFrom(pa[i]));
    }

    @Override
    public Object call(Object target, Method method, Object[] args, Callable<?> original) throws Throwable {
        Capabilities caps = sessionCapabilities;
        if (caps != null && target instanceof WebDriver && "getCapabilities".equals(method.getName())
                && method.getParameterCount() == 0) {
            return caps;
        }
        WebDriverListener s = steps;
        // Calls made while one is reported come from the driver itself (an element click runs a driver
        // command) or from PulseReport reading the screen; only the outermost call is a step
        if (s == null || reporting.get()) {
            return withFileDetector(super.call(target, method, args, original));
        }
        reporting.set(true);
        try {
            notify(() -> s.beforeAnyCall(target, method, args));
            Object result;
            try {
                result = withFileDetector(super.call(target, method, args, original));
            } catch (Throwable t) {
                notify(() -> s.onError(target, method, args, new InvocationTargetException(t)));
                throw t;
            }
            notify(() -> s.afterAnyCall(target, method, args, result));
            return result;
        } finally {
            reporting.set(false);
        }
    }

    /**
     * The element proxies made by {@link ElementAwareWebDriverListener} get only an id and a
     * parent, and {@code sendKeys} fails without a file detector, so give them the driver's.
     */
    private Object withFileDetector(Object result) {
        RemoteWebDriver d = driver;
        if (d == null) {
            return result;
        }
        if (result instanceof RemoteWebElement) {
            ((RemoteWebElement) result).setFileDetector(d.getFileDetector());
        } else if (result instanceof List) {
            for (Object item : (List<?>) result) {
                if (item instanceof RemoteWebElement) {
                    ((RemoteWebElement) item).setFileDetector(d.getFileDetector());
                }
            }
        }
        return result;
    }

    /** Reporting never breaks the test's own driver call. */
    private static void notify(Runnable event) {
        try {
            event.run();
        } catch (RuntimeException e) {
            logger.debug("PulseReport: could not record an Appium call", e);
        }
    }
}
