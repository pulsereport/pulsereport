package io.github.pulsereport.adapters.selenium;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns WebDriver interactions on a {@link SeleniumTracker#track tracked}
 * driver into report steps: element, locator, typed text (masked for
 * sensitive fields), URL changes, page load time, duration and failures.
 * Failed element lookups are remembered so a test failure they cause can be
 * explained by them.
 */
final class SeleniumStepListener implements WebDriverListener {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumStepListener.class);

    private static final Set<String> ELEMENT_ACTIONS = Set.of("click", "submit", "sendKeys", "clear");
    private static final Set<String> NAVIGATION_ACTIONS = Set.of("to", "back", "forward", "refresh");
    private static final Set<String> SWITCH_ACTIONS = Set.of("frame", "parentFrame", "defaultContent", "window",
            "newWindow");
    private static final Set<String> ALERT_ACTIONS = Set.of("accept", "dismiss", "sendKeys");
    private static final Set<String> SCRIPT_ACTIONS = Set.of("executeScript", "executeAsyncScript");
    private static final Set<String> LOOKUPS = Set.of("findElement", "findElements");
    private static final int MAX_LABEL = 40;
    private static final int MAX_TEXT = 60;
    private static final int MAX_URL = 100;
    private static final int MAX_SCRIPT = 80;
    private static final int MAX_REMEMBERED_LOCATORS = 2_000;

    private static final String DESCRIBE_SCRIPT = "const e = arguments[0];"
            + "const label = e.getAttribute('aria-label') || (e.labels && e.labels.length ? e.labels[0].innerText : '')"
            + " || (e.innerText || '').trim() || e.getAttribute('placeholder') || e.getAttribute('title')"
            + " || e.getAttribute('name') || (e.type === 'submit' || e.type === 'button' ? e.value : '') || '';"
            + "return [e.tagName.toLowerCase(), label, e.type || '', location.href, performance.timeOrigin,"
            + " e.getAttribute('autocomplete') || '', e.getAttribute('name') || '', e.id || ''];";

    // timeOrigin changes only when a new document loads, so SPA route changes don't report stale load times
    private static final String PAGE_SCRIPT = "const n = performance.getEntriesByType('navigation')[0];"
            + "return [location.href, performance.timeOrigin,"
            + " n && n.loadEventEnd > 0 ? Math.round(n.loadEventEnd) : -1,"
            + " n && n.responseStart > 0 ? Math.round(n.responseStart) : -1, document.title];";

    private enum Kind { ELEMENT, NAVIGATION, SWITCH, OTHER, LOOKUP }

    private final WebDriver driver;
    private final ReentrantLock driverLock;
    private final SeleniumTracker.Camera camera;
    // Keyed by remote element id: element wrapper objects are short-lived (e.g. PageFactory proxies)
    private final Map<String, String> locators = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_REMEMBERED_LOCATORS;
        }
    });
    private final ThreadLocal<Pending> pending = new ThreadLocal<>();
    private volatile Double lastTimeOrigin;

    SeleniumStepListener(WebDriver driver, ReentrantLock driverLock) {
        this.driver = driver;
        this.driverLock = driverLock;
        this.camera = new SeleniumTracker.Camera(driver, driverLock);
    }

    /** What an element looked like just before the action. */
    private record ElementInfo(String tag, String label, String type, String href, Double timeOrigin,
            String autocomplete, String name, String id) {
    }

    private static final class Pending {
        final Kind kind;
        final String method;
        final Object[] args;
        final Instant startedAt = Instant.now();
        final long startNanos = System.nanoTime();
        WebElement element;
        ElementInfo info;
        String name;
        String detail;

        Pending(Kind kind, String method, Object[] args) {
            this.kind = kind;
            this.method = method;
            this.args = args;
        }
    }

    @Override
    public void beforeAnyCall(Object target, Method method, Object[] args) {
        // Held until afterAnyCall/onError so recorder screenshots never interleave with this command
        driverLock.lock();
        String name = method.getName();
        try {
            if ("quit".equals(name) && target instanceof WebDriver) {
                SeleniumTracker.beforeQuit(driver);
                return;
            }
            Pending p = prepare(target, name, args);
            if (p != null) {
                pending.set(p);
            }
        } catch (RuntimeException e) {
            logger.debug("Could not prepare Selenium step for '{}'", name, e);
        }
    }

    private Pending prepare(Object target, String name, Object[] args) {
        boolean detailed = SeleniumTracker.settings().detailed();
        if (LOOKUPS.contains(name) && (target instanceof WebDriver || target instanceof WebElement)) {
            Pending p = new Pending(Kind.LOOKUP, name, args);
            p.element = target instanceof WebElement ? (WebElement) target : null;
            return p;
        }
        if (target instanceof WebElement && ELEMENT_ACTIONS.contains(name)) {
            Pending p = new Pending(Kind.ELEMENT, name, args);
            p.element = (WebElement) target;
            // Read before acting: the element may be stale once the action navigates away
            p.info = detailed ? describe(p.element) : null;
            return p;
        }
        if ((target instanceof WebDriver && "get".equals(name))
                || (target instanceof WebDriver.Navigation && NAVIGATION_ACTIONS.contains(name))) {
            return new Pending(Kind.NAVIGATION, name, args);
        }
        if (target instanceof WebDriver && "close".equals(name)) {
            return named(new Pending(Kind.OTHER, name, args), "Close window", null);
        }
        if (target instanceof WebDriver.TargetLocator && SWITCH_ACTIONS.contains(name)) {
            return named(new Pending(Kind.SWITCH, name, args), describeSwitch(name, args, detailed), null);
        }
        if (target instanceof Alert && ALERT_ACTIONS.contains(name)) {
            return describeAlert((Alert) target, name, args, detailed);
        }
        if (target instanceof Interactive && "perform".equals(name)) {
            return named(new Pending(Kind.OTHER, name, args), describeActions(args, detailed), null);
        }
        if (target instanceof WebDriver && SCRIPT_ACTIONS.contains(name) && SeleniumTracker.settings().trackScripts()) {
            String script = args != null && args.length > 0 ? String.valueOf(args[0]) : "";
            return named(new Pending(Kind.OTHER, name, args),
                    "executeAsyncScript".equals(name) ? "Run async script" : "Run script", truncate(script, MAX_SCRIPT));
        }
        return null;
    }

    private static Pending named(Pending p, String name, String detail) {
        p.name = name;
        p.detail = detail;
        return p;
    }

    @Override
    public void afterAnyCall(Object target, Method method, Object[] args, Object result) {
        try {
            String name = method.getName();
            if (LOOKUPS.contains(name)) {
                clearPending(name);
                rememberLocator(target, args, result);
            } else if ("quit".equals(name) && target instanceof WebDriver) {
                SeleniumTracker.untrack(driver);
            } else {
                complete(name, null);
            }
        } finally {
            releaseDriver();
        }
    }

    @Override
    public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
        try {
            String name = method.getName();
            if ("quit".equals(name) && target instanceof WebDriver) {
                // The session is gone or unusable either way
                SeleniumTracker.untrack(driver);
            } else if (LOOKUPS.contains(name)) {
                rememberFailedLookup(name, e.getTargetException());
            } else {
                complete(name, e.getTargetException());
            }
        } finally {
            releaseDriver();
        }
    }

    /** Fully releases, so a before-call without a matching after-call can't keep the recorder out for good. */
    private void releaseDriver() {
        while (driverLock.isHeldByCurrentThread()) {
            driverLock.unlock();
        }
    }

    private void clearPending(String method) {
        Pending p = pending.get();
        if (p != null && p.method.equals(method)) {
            pending.remove();
        }
    }

    private void rememberFailedLookup(String method, Throwable error) {
        Pending p = pending.get();
        if (p == null || !p.method.equals(method) || p.kind != Kind.LOOKUP) {
            return;
        }
        pending.remove();
        String locator = p.args != null && p.args.length > 0 ? String.valueOf(p.args[0]) : "element";
        String parent = p.element != null ? locatorOf(p.element) : null;
        SeleniumTracker.rememberFailedLookup(parent != null ? parent + " > " + locator : locator, error,
                p.startedAt, (System.nanoTime() - p.startNanos) / 1_000_000);
    }

    private void complete(String method, Throwable failure) {
        Pending p = pending.get();
        if (p == null || !p.method.equals(method) || p.kind == Kind.LOOKUP) {
            return;
        }
        pending.remove();
        long durationMs = (System.nanoTime() - p.startNanos) / 1_000_000;
        try {
            boolean detailed = SeleniumTracker.settings().detailed();
            List<String> details = new ArrayList<>();
            String name;
            switch (p.kind) {
                case ELEMENT:
                    name = describeElementAction(p, details);
                    if (failure == null && detailed) {
                        afterElementAction(p, details);
                    }
                    break;
                case NAVIGATION:
                    name = describeNavigation(p);
                    if (failure == null && detailed) {
                        afterNavigation(p, details);
                    }
                    break;
                case SWITCH:
                    name = p.name;
                    if (failure == null && detailed && ("window".equals(p.method) || "newWindow".equals(p.method))) {
                        afterWindowSwitch(details);
                    }
                    break;
                default:
                    name = p.name;
                    if (p.detail != null && !p.detail.isEmpty()) {
                        details.add(p.detail);
                    }
            }
            SeleniumTracker.recordAction(name, details.isEmpty() ? null : String.join(" · ", details),
                    failure, p.startedAt, durationMs, camera);
        } catch (RuntimeException e) {
            logger.debug("Could not record Selenium step for '{}'", method, e);
        }
    }

    // ------------------------------------------------------------------ element actions

    private ElementInfo describe(WebElement element) {
        List<?> info = SeleniumTracker.script(camera, DESCRIBE_SCRIPT, unwrap(element));
        if (info.size() != 8) {
            return null;
        }
        Double timeOrigin = info.get(4) instanceof Number ? ((Number) info.get(4)).doubleValue() : null;
        lastTimeOrigin = timeOrigin;
        return new ElementInfo(String.valueOf(info.get(0)), String.valueOf(info.get(1)), String.valueOf(info.get(2)),
                String.valueOf(info.get(3)), timeOrigin, String.valueOf(info.get(5)), String.valueOf(info.get(6)),
                String.valueOf(info.get(7)));
    }

    private String describeElementAction(Pending p, List<String> details) {
        String locator = locatorOf(p.element);
        String target;
        if (p.info != null) {
            target = p.info.tag().isEmpty() ? "element" : p.info.tag();
            if (!p.info.label().isBlank()) {
                target += " \"" + truncate(p.info.label(), MAX_LABEL) + "\"";
            }
            if (locator != null) {
                details.add(locator);
            }
        } else {
            target = locator != null ? locator : "element";
        }
        switch (p.method) {
            case "click":
                return "Click " + target;
            case "submit":
                return "Submit " + target;
            case "clear":
                return "Clear " + target;
            default:
                boolean sensitive = p.info != null
                        ? SeleniumTracker.isSensitiveField(p.info.type(), p.info.autocomplete(), p.info.name(),
                                p.info.id(), p.info.label(), locator)
                        : SeleniumTracker.isSensitiveField(null, null, locator);
                String typed = sensitive ? SeleniumTracker.TYPED_MASK : typedText(p.args);
                return "Type \"" + truncate(typed, MAX_TEXT) + "\" into " + target;
        }
    }

    private void afterElementAction(Pending p, List<String> details) {
        List<?> page = SeleniumTracker.script(camera, PAGE_SCRIPT);
        if (page.size() != 5) {
            return;
        }
        String url = String.valueOf(page.get(0));
        Double timeOrigin = page.get(1) instanceof Number ? ((Number) page.get(1)).doubleValue() : null;
        lastTimeOrigin = timeOrigin;
        if (p.info != null && p.info.href() != null && !url.equals(p.info.href())) {
            details.add("→ " + truncate(SeleniumTracker.maskUrl(url), MAX_URL));
        }
        boolean newDocument = timeOrigin != null && p.info != null && p.info.timeOrigin() != null
                && !timeOrigin.equals(p.info.timeOrigin());
        if (newDocument) {
            pageLoad(url, page, details);
        }
    }

    // ------------------------------------------------------------------ navigation

    private static String describeNavigation(Pending p) {
        switch (p.method) {
            case "back":
                return "Navigate back";
            case "forward":
                return "Navigate forward";
            case "refresh":
                return "Refresh page";
            default:
                String url = p.args != null && p.args.length > 0 ? String.valueOf(p.args[0]) : "";
                return "Open \"" + SeleniumTracker.maskUrl(url) + "\"";
        }
    }

    private void afterNavigation(Pending p, List<String> details) {
        Double before = lastTimeOrigin;
        List<?> page = SeleniumTracker.script(camera, PAGE_SCRIPT);
        if (page.size() != 5) {
            return;
        }
        String url = String.valueOf(page.get(0));
        Double timeOrigin = page.get(1) instanceof Number ? ((Number) page.get(1)).doubleValue() : null;
        lastTimeOrigin = timeOrigin;
        if (!"get".equals(p.method) && !"to".equals(p.method)) {
            details.add(truncate(SeleniumTracker.maskUrl(url), MAX_URL));
        }
        // Same document (hash change, back-forward cache): its navigation entry is an old page load
        if (before == null || timeOrigin == null || !timeOrigin.equals(before)) {
            pageLoad(url, page, details);
        }
    }

    private static void pageLoad(String url, List<?> page, List<String> details) {
        long load = page.get(2) instanceof Number ? ((Number) page.get(2)).longValue() : -1;
        long ttfb = page.get(3) instanceof Number ? ((Number) page.get(3)).longValue() : -1;
        if (load >= 0) {
            details.add("page load " + SeleniumTracker.formatDuration(load));
            SeleniumTracker.recordPageLoad(truncate(SeleniumTracker.maskUrl(url), 300), load, ttfb);
        }
    }

    // ------------------------------------------------------------------ frames, windows, alerts

    private String describeSwitch(String method, Object[] args, boolean detailed) {
        Object arg = args != null && args.length > 0 ? args[0] : null;
        switch (method) {
            case "parentFrame":
                return "Switch to parent frame";
            case "defaultContent":
                return "Switch to main document";
            case "window":
                return "Switch to window";
            case "newWindow":
                return arg == WindowType.TAB ? "Open new tab" : "Open new window";
            default:
                if (arg instanceof Number) {
                    return "Switch to frame #" + arg;
                }
                if (arg instanceof WebElement) {
                    return "Switch to frame " + elementDescription((WebElement) arg, detailed);
                }
                return "Switch to frame \"" + arg + "\"";
        }
    }

    private void afterWindowSwitch(List<String> details) {
        List<?> page = SeleniumTracker.script(camera, PAGE_SCRIPT);
        if (page.size() != 5) {
            return;
        }
        lastTimeOrigin = page.get(1) instanceof Number ? ((Number) page.get(1)).doubleValue() : null;
        String title = String.valueOf(page.get(4));
        if (!title.isBlank()) {
            details.add("\"" + truncate(title, MAX_LABEL) + "\"");
        }
        details.add(truncate(SeleniumTracker.maskUrl(String.valueOf(page.get(0))), MAX_URL));
    }

    private Pending describeAlert(Alert alert, String method, Object[] args, boolean detailed) {
        String text = null;
        if (detailed) {
            try {
                text = alert.getText();
            } catch (RuntimeException e) {
                // no alert open; the action itself will report it
            }
        }
        String detail = text == null || text.isBlank() ? null : "\"" + truncate(text, MAX_TEXT) + "\"";
        Pending p = new Pending(Kind.OTHER, method, args);
        switch (method) {
            case "accept":
                return named(p, "Accept alert", detail);
            case "dismiss":
                return named(p, "Dismiss alert", detail);
            default:
                String typed = SeleniumTracker.isSensitiveField(null, null)
                        ? SeleniumTracker.TYPED_MASK
                        : args != null && args.length > 0 ? String.valueOf(args[0]) : "";
                return named(p, "Type \"" + truncate(typed, MAX_TEXT) + "\" into alert", detail);
        }
    }

    // ------------------------------------------------------------------ Actions API

    /** Summarizes an {@code Actions} chain: hover, click, double/right click, drag, key presses, scrolling. */
    private String describeActions(Object[] args, boolean detailed) {
        if (args == null || args.length == 0 || !(args[0] instanceof Collection)) {
            return "Perform actions";
        }
        List<Map<?, ?>> pointer = new ArrayList<>();
        StringBuilder keys = new StringBuilder();
        boolean wheel = false;
        for (Object item : (Collection<?>) args[0]) {
            if (!(item instanceof Sequence)) {
                continue;
            }
            Map<String, Object> json = ((Sequence) item).toJson();
            String type = String.valueOf(json.get("type"));
            Object actions = json.get("actions");
            if (!(actions instanceof List)) {
                continue;
            }
            for (Object action : (List<?>) actions) {
                if (!(action instanceof Map)) {
                    continue;
                }
                Map<?, ?> a = (Map<?, ?>) action;
                if ("pointer".equals(type)) {
                    pointer.add(a);
                } else if ("key".equals(type) && "keyDown".equals(a.get("type")) && a.get("value") != null) {
                    String.valueOf(a.get("value")).chars().forEach(c -> appendKey(keys, (char) c));
                } else if ("wheel".equals(type) && "scroll".equals(a.get("type"))) {
                    wheel = true;
                }
            }
        }
        int downs = 0;
        int firstButton = -1;
        boolean down = false;
        boolean dragged = false;
        boolean moved = false;
        List<WebElement> origins = new ArrayList<>();
        for (Map<?, ?> a : pointer) {
            Object type = a.get("type");
            if ("pointerMove".equals(type)) {
                moved = true;
                dragged |= down;
                if (a.get("origin") instanceof WebElement) {
                    origins.add((WebElement) a.get("origin"));
                }
            } else if ("pointerDown".equals(type)) {
                downs++;
                down = true;
                if (firstButton < 0 && a.get("button") instanceof Number) {
                    firstButton = ((Number) a.get("button")).intValue();
                }
            } else if ("pointerUp".equals(type)) {
                down = false;
            }
        }
        String target = origins.isEmpty() ? null : elementDescription(origins.get(0), detailed);
        String verb;
        if (downs > 0) {
            verb = firstButton == 2 ? "Right-click" : dragged ? "Drag" : downs >= 2 ? "Double-click" : "Click";
            if (dragged && origins.size() >= 2) {
                target = target + " to " + elementDescription(origins.get(origins.size() - 1), false);
            }
        } else if (moved) {
            verb = "Hover over";
        } else if (wheel) {
            verb = "Scroll";
        } else if (keys.length() > 0) {
            return "Press keys \"" + truncate(maskedKeys(keys.toString()), MAX_TEXT) + "\"";
        } else {
            return "Perform actions";
        }
        String name = verb + (target != null ? " " + target : "");
        if (keys.length() > 0) {
            name += " and press keys \"" + truncate(maskedKeys(keys.toString()), MAX_TEXT) + "\"";
        }
        return name;
    }

    private static String maskedKeys(String keys) {
        return SeleniumTracker.isSensitiveField(null, null) ? SeleniumTracker.TYPED_MASK : keys;
    }

    // ------------------------------------------------------------------ locators

    private String elementDescription(WebElement element, boolean detailed) {
        String locator = locatorOf(element);
        if (detailed) {
            ElementInfo info = describe(element);
            if (info != null) {
                String text = info.tag() + (info.label().isBlank() ? "" : " \"" + truncate(info.label(), MAX_LABEL) + "\"");
                return locator != null ? text + " (" + locator + ")" : text;
            }
        }
        return locator != null ? locator : "element";
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

    private static WebElement unwrap(WebElement element) {
        WebElement unwrapped = element;
        while (unwrapped instanceof WrapsElement) {
            unwrapped = ((WrapsElement) unwrapped).getWrappedElement();
        }
        return unwrapped;
    }

    private static String elementId(WebElement element) {
        WebElement unwrapped = unwrap(element);
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
        Keys key = c >= '' && c <= '' ? Keys.getKeyFromUnicode(c) : null;
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
}
