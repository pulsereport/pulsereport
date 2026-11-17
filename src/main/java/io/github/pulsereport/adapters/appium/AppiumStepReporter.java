package io.github.pulsereport.adapters.appium;

import io.appium.java_client.proxy.ElementAwareWebDriverListener;
import io.appium.java_client.proxy.Helpers;
import io.github.pulsereport.adapters.selenium.SeleniumTracker;
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

    static <T extends WebDriver> T createDriver(Class<T> driverClass, Object... args) {
        if (driverClass == null) {
            throw new IllegalArgumentException("driverClass cannot be null");
        }
        Object[] constructorArgs = args == null ? new Object[0] : args;
        Constructor<?> constructor = constructorFor(driverClass, constructorArgs);
        AppiumStepReporter reporter = new AppiumStepReporter();
        T driver = Helpers.createProxy(driverClass, constructorArgs, constructor.getParameterTypes(), List.of(reporter));
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
