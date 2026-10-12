package io.github.pulsereport.adapters.selenium;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns WebDriver interactions on a {@link SeleniumAdapter#track tracked}
 * driver into report steps: element, locator, typed text (masked for password
 * fields), URL changes, page load time, duration, and failures.
 */
final class SeleniumStepListener implements WebDriverListener {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumStepListener.class);

    private static final Set<String> ELEMENT_ACTIONS = Set.of("click", "submit", "sendKeys", "clear");
    private static final Set<String> NAVIGATION_ACTIONS = Set.of("to", "back", "forward", "refresh");
    private static final int MAX_LABEL = 40;
    private static final int MAX_TEXT = 60;
    private static final int MAX_URL = 100;
    private static final int MAX_REMEMBERED_LOCATORS = 2_000;

    private static final String DESCRIBE_SCRIPT = "const e = arguments[0];"
            + "const label = e.getAttribute('aria-label') || (e.innerText || '').trim()"
            + " || e.getAttribute('placeholder') || e.getAttribute('title') || e.getAttribute('name')"
            + " || (e.type === 'submit' || e.type === 'button' ? e.value : '') || '';"
            + "return [e.tagName.toLowerCase(), label, e.type || '', location.href];";

    private static final String PAGE_SCRIPT = "const n = performance.getEntriesByType('navigation')[0];"
            + "return [location.href, n ? Math.round(n.loadEventEnd > 0 ? n.loadEventEnd : n.duration) : -1];";

    private final SeleniumAdapter adapter;
    private final WebDriver driver;
    // Keyed by remote element id: element wrapper objects are short-lived (e.g. PageFactory proxies)
    private final Map<String, String> locators = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_REMEMBERED_LOCATORS;
        }
    });
    private final ThreadLocal<Pending> pending = new ThreadLocal<>();

    SeleniumStepListener(SeleniumAdapter adapter, WebDriver driver) {
        this.adapter = adapter;
        this.driver = driver;
    }

    private static final class Pending {
        final String method;
        final Object[] args;
        final WebElement element;
        final Instant startedAt = Instant.now();
        final long startNanos = System.nanoTime();
        String tag = "";
        String label = "";
        String type = "";
        String urlBefore;

        Pending(String method, Object[] args, WebElement element) {
            this.method = method;
            this.args = args;
            this.element = element;
        }
    }

    @Override
    public void beforeAnyCall(Object target, Method method, Object[] args) {
        String name = method.getName();
        if ("quit".equals(name) && target instanceof WebDriver) {
            adapter.beforeQuit(driver);
        } else if (target instanceof WebElement && ELEMENT_ACTIONS.contains(name)) {
            WebElement element = (WebElement) target;
            Pending p = new Pending(name, args, element);
            // Read before acting: the element may be stale once the action navigates away
            List<?> info = runScript(DESCRIBE_SCRIPT, element);
            if (info.size() == 4) {
                p.tag = String.valueOf(info.get(0));
                p.label = String.valueOf(info.get(1));
                p.type = String.valueOf(info.get(2));
                p.urlBefore = String.valueOf(info.get(3));
            }
            pending.set(p);
        } else if ((target instanceof WebDriver && "get".equals(name))
                || (target instanceof WebDriver.Navigation && NAVIGATION_ACTIONS.contains(name))) {
            pending.set(new Pending(name, args, null));
        }
    }

    @Override
    public void afterAnyCall(Object target, Method method, Object[] args, Object result) {
        String name = method.getName();
        if ("findElement".equals(name) || "findElements".equals(name)) {
            rememberLocator(target, args, result);
        } else if ("quit".equals(name) && target instanceof WebDriver) {
            adapter.untrack(driver);
        } else {
            complete(name, null);
        }
    }

    @Override
    public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
        complete(method.getName(), e.getTargetException());
    }

    private void complete(String method, Throwable failure) {
        Pending p = pending.get();
        if (p == null || !p.method.equals(method)) {
            return;
        }
        pending.remove();
        long durationMs = (System.nanoTime() - p.startNanos) / 1_000_000;
        try {
            List<String> details = new ArrayList<>();
            String name = p.element != null ? describeElementAction(p, details) : describeNavigation(p);
            if (failure == null) {
                appendPageState(p, details);
            }
            adapter.recordAction(name, details.isEmpty() ? null : String.join(" · ", details),
                    failure, p.startedAt, durationMs, driver);
        } catch (RuntimeException e) {
            logger.debug("Could not record Selenium step for '{}'", method, e);
        }
    }

    private String describeElementAction(Pending p, List<String> details) {
        String target = p.tag.isEmpty() ? "element" : p.tag;
        if (!p.label.isBlank()) {
            target += " \"" + truncate(p.label, MAX_LABEL) + "\"";
        }
        String locator = locatorOf(p.element);
        if (locator != null) {
            details.add(locator);
        }
        switch (p.method) {
            case "click":
                return "Click " + target;
            case "submit":
                return "Submit " + target;
            case "clear":
                return "Clear " + target;
            default:
                String typed = "password".equalsIgnoreCase(p.type) ? "••••••" : typedText(p.args);
                return "Type \"" + truncate(typed, MAX_TEXT) + "\" into " + target;
        }
    }

    private static String describeNavigation(Pending p) {
        switch (p.method) {
            case "back":
                return "Navigate back";
            case "forward":
                return "Navigate forward";
            case "refresh":
                return "Refresh page";
            default:
                return "Open \"" + (p.args != null && p.args.length > 0 ? p.args[0] : "") + "\"";
        }
    }

    private void appendPageState(Pending p, List<String> details) {
        if (p.element != null) {
            String url = currentUrl();
            if (url != null && p.urlBefore != null && !url.equals(p.urlBefore)) {
                details.add("→ " + truncate(url, MAX_URL));
            }
            return;
        }
        List<?> page = runScript(PAGE_SCRIPT);
        if (page.size() != 2) {
            return;
        }
        if (!"get".equals(p.method) && !"to".equals(p.method)) {
            details.add(truncate(String.valueOf(page.get(0)), MAX_URL));
        }
        if (page.get(1) instanceof Number && ((Number) page.get(1)).longValue() >= 0) {
            details.add("page load " + SeleniumAdapter.formatDuration(((Number) page.get(1)).longValue()));
        }
    }

    private void rememberLocator(Object target, Object[] args, Object result) {
        if (args == null || args.length == 0 || !(args[0] instanceof By)) {
            return;
        }
        String locator = args[0].toString();
        if (target instanceof WebElement) {
            String parent = locatorOf((WebElement) target);
            if (parent != null) {
                locator = parent + " > " + locator;
            }
        }
        if (result instanceof WebElement) {
            rememberLocator((WebElement) result, locator);
        } else if (result instanceof List) {
            for (Object item : (List<?>) result) {
                if (item instanceof WebElement) {
                    rememberLocator((WebElement) item, locator);
                }
            }
        }
    }

    private void rememberLocator(WebElement element, String locator) {
        String id = elementId(element);
        if (id != null) {
            locators.put(id, locator);
        }
    }

    private String locatorOf(WebElement element) {
        String id = elementId(element);
        return id == null ? null : locators.get(id);
    }

    private static String elementId(WebElement element) {
        WebElement unwrapped = element;
        while (unwrapped instanceof WrapsElement) {
            unwrapped = ((WrapsElement) unwrapped).getWrappedElement();
        }
        return unwrapped instanceof RemoteWebElement ? ((RemoteWebElement) unwrapped).getId() : null;
    }

    private static String typedText(Object[] args) {
        StringBuilder sb = new StringBuilder();
        if (args != null && args.length > 0 && args[0] instanceof CharSequence[]) {
            for (CharSequence cs : (CharSequence[]) args[0]) {
                if (cs != null) {
                    cs.chars().forEach(c -> appendKey(sb, (char) c));
                }
            }
        }
        return sb.toString();
    }

    private static void appendKey(StringBuilder sb, char c) {
        Keys key = c >= '\uE000' && c <= '\uE0FF' ? Keys.getKeyFromUnicode(c) : null;
        if (key != null) {
            sb.append('[').append(key.name()).append(']');
        } else {
            sb.append(c);
        }
    }

    private static String truncate(String value, int max) {
        String collapsed = value.replaceAll("\\s+", " ").trim();
        return collapsed.length() <= max ? collapsed : collapsed.substring(0, max - 1) + "…";
    }

    private List<?> runScript(String script, Object... args) {
        if (!(driver instanceof JavascriptExecutor)) {
            return List.of();
        }
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(script, args);
            return result instanceof List ? (List<?>) result : List.of();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private String currentUrl() {
        try {
            return driver.getCurrentUrl();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
