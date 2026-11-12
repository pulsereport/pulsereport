package io.github.pulsereport.assertions;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.pulsereport.core.model.AssertionResult;
import io.github.pulsereport.core.model.TestStatus;

/**
 * Turns an intercepted fluent check into {@link AssertionResult}s: an AssertJ
 * assertion such as {@code assertThat(name).isEqualTo("Ann")}, or a REST Assured
 * response check such as {@code then().statusCode(200).body("id", is(1))}.
 *
 * <p>Both libraries are optional, so everything is read reflectively: the actual
 * value from the AssertJ assert object, or from the response REST Assured checks.
 * Anything that can't be read is left empty rather than failing.</p>
 */
final class FluentAssertionInterpreter {

    static final String ASSERTJ_PACKAGE = "org.assertj.core.api.";
    static final String ASSERTJ_ROOT = "org.assertj.core.api.AbstractAssert";
    static final String REST_ASSURED_CHECKS = "io.restassured.internal.ValidatableResponseOptionsImpl";
    private static final String REST_ASSURED_SPEC = "io.restassured.internal.ResponseSpecificationImpl";

    private FluentAssertionInterpreter() {
    }

    static List<AssertionResult> interpret(String owner, String method, String descriptor, Object self,
                                           Object[] args, Throwable thrown) {
        Object[] values = args != null ? args : new Object[0];
        if (owner != null && owner.startsWith("io.restassured.")) {
            return restAssured(method, self, values, thrown);
        }
        return List.of(assertj(method, self, values, thrown));
    }

    // ---------- AssertJ ----------

    private static AssertionResult assertj(String method, Object self, Object[] values, Throwable thrown) {
        String expected;
        if (values.length == 0) {
            expected = words(method);
        } else if (values.length == 1) {
            expected = AssertionInterpreter.describe(values[0]);
        } else {
            List<String> shown = new ArrayList<>();
            for (Object value : values) {
                shown.add(AssertionInterpreter.describe(value));
            }
            expected = AssertionInterpreter.truncate(String.join(", ", shown));
        }
        String actual = AssertionInterpreter.describe(field(self, ASSERTJ_ROOT, "actual"));
        return result("AssertJ", method, assertjDescription(self), expected, actual, thrown == null, thrown);
    }

    /** The text given with {@code as("...")} / {@code describedAs("...")}, if any. */
    private static String assertjDescription(Object self) {
        try {
            Object info = field(self, ASSERTJ_ROOT, "info");
            if (info == null) {
                return null;
            }
            Object text = info.getClass().getMethod("descriptionText").invoke(info);
            return text == null || text.toString().isBlank() ? null : AssertionInterpreter.truncate(text.toString());
        } catch (Throwable t) {
            return null;
        }
    }

    /** {@code isNotNull} → "not null", {@code hasNoDuplicates} → "no duplicates", {@code isTrue} → "true". */
    static String words(String method) {
        String name = method;
        if (name.length() > 2 && name.startsWith("is") && Character.isUpperCase(name.charAt(2))) {
            name = name.substring(2);
        } else if (name.length() > 3 && name.startsWith("has") && Character.isUpperCase(name.charAt(3))) {
            name = name.substring(3);
        }
        return name.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
    }

    // ---------- REST Assured ----------

    private static List<AssertionResult> restAssured(String method, Object self, Object[] values, Throwable thrown) {
        Object response = field(self, REST_ASSURED_CHECKS, "response");
        List<AssertionResult> results = new ArrayList<>();
        switch (method) {
            case "statusCode":
                results.add(rest(method, null, first(values), call(response, "getStatusCode"), thrown));
                break;
            case "statusLine":
                results.add(rest(method, null, first(values), call(response, "getStatusLine"), thrown));
                break;
            case "contentType":
                results.add(rest(method, null, first(values), call(response, "getContentType"), thrown));
                break;
            case "time":
                results.add(time(response, values, thrown));
                break;
            case "header":
                if (values.length >= 2) {
                    String name = String.valueOf(values[0]);
                    results.add(rest(method, name, values[values.length - 1], call(response, "getHeader", name), thrown));
                }
                break;
            case "cookie":
                if (values.length >= 1) {
                    String name = String.valueOf(values[0]);
                    Object expected = values.length >= 2 ? values[1] : null;
                    AssertionResult r = rest(method, name, expected, call(response, "getCookie", name), thrown);
                    results.add(values.length >= 2 ? r : copy(r, "present", r.getStatus(), r.getErrorMessage()));
                }
                break;
            case "headers":
            case "cookies":
                String getter = "headers".equals(method) ? "getHeader" : "getCookie";
                for (Object[] pair : pairs(values)) {
                    String name = String.valueOf(pair[0]);
                    Object actual = call(response, getter, name);
                    results.add(pair(method.substring(0, method.length() - 1), name, pair[1], actual, thrown));
                }
                break;
            case "body":
            case "content":
                results.addAll(body(self, response, values, thrown));
                break;
            case "spec":
                results.add(result("REST Assured", method, "response specification", null, null, thrown == null, thrown));
                break;
            default:
                break;
        }
        if (thrown != null && results.stream().noneMatch(r -> r.getStatus() == TestStatus.FAILED)) {
            // The check failed but no single expectation could be blamed: show them all as failed
            List<AssertionResult> failed = new ArrayList<>();
            results.forEach(r -> failed.add(copy(r, r.getExpected(), TestStatus.FAILED, errorText(thrown))));
            return failed;
        }
        return results;
    }

    private static AssertionResult time(Object response, Object[] values, Throwable thrown) {
        Object unit = values.length >= 2 ? values[1] : null;
        String unitText = unit == null ? "ms" : unit.toString().toLowerCase(Locale.ROOT);
        Object actual = unit == null ? call(response, "getTime") : callWith(response, "getTimeIn", unit);
        String expected = values.length >= 1 ? AssertionInterpreter.describe(values[0]) + " " + unitText : null;
        return result("REST Assured", "time", null, expected,
                actual == null ? null : actual + " " + unitText, thrown == null, thrown);
    }

    /**
     * body(path, matcher, path2, matcher2, ...), body(path, arguments, matcher, ...),
     * body(arguments, matcher, ...) and body(matcher, matchers...) for the whole body.
     */
    private static List<AssertionResult> body(Object self, Object response, Object[] values, Throwable thrown) {
        List<AssertionResult> results = new ArrayList<>();
        if (values.length == 0) {
            return results;
        }
        if (!(values[0] instanceof String) && !(values[0] instanceof List)) {
            // Whole-body matchers
            List<Object> matchers = new ArrayList<>();
            matchers.add(values[0]);
            if (values.length > 1 && values[1] instanceof Object[]) {
                matchers.addAll(List.of((Object[]) values[1]));
            }
            String actual = String.valueOf(call(response, "asString"));
            for (Object matcher : matchers) {
                results.add(pair("body", null, matcher, actual, thrown));
            }
            return results;
        }
        int i = 0;
        String path = values[0] instanceof String ? (String) values[i++] : "";
        boolean formatted = i < values.length && values[i] instanceof List && !((List<?>) values[i]).isEmpty();
        if (i < values.length && values[i] instanceof List) {
            i++;
        }
        Object matcher = i < values.length ? values[i++] : null;
        String root = stringField(field(self, REST_ASSURED_CHECKS, "responseSpec"), REST_ASSURED_SPEC, "bodyRootPath");
        results.add(bodyPath(response, root, path, formatted, matcher, thrown));
        if (i < values.length && values[i] instanceof Object[]) {
            Object[] more = (Object[]) values[i];
            for (int k = 0; k + 1 < more.length; k += 2) {
                results.add(bodyPath(response, root, String.valueOf(more[k]), false, more[k + 1], thrown));
            }
        }
        return results;
    }

    private static AssertionResult bodyPath(Object response, String root, String path, boolean formatted,
                                            Object matcher, Throwable thrown) {
        String full = mergeWithRoot(root, path);
        Object actual = formatted ? null : callWith(response, "path", full, new String[0]);
        String shownPath = full.isEmpty() ? "(root)" : full;
        return pair("body", shownPath, matcher, actual == null && formatted ? Unknown.VALUE : actual, thrown);
    }

    /** The path REST Assured evaluates for {@code key} under the root path (its own merge rules). */
    static String mergeWithRoot(String root, String key) {
        if (root == null || root.isEmpty()) {
            return key;
        }
        if (root.endsWith(".") && key.startsWith(".")) {
            return root + key.substring(1);
        }
        if (!root.endsWith(".") && !key.startsWith(".") && !key.startsWith("?.") && !key.startsWith("[")
                && !key.startsWith("?[") && !key.startsWith("*.")) {
            return root + "." + key;
        }
        return root + key;
    }

    /** One expectation of a check that may hold several: when the check failed, the matcher says which. */
    private static AssertionResult pair(String method, String message, Object expected, Object actual, Throwable thrown) {
        boolean passed = thrown == null || (actual != Unknown.VALUE && matches(expected, actual));
        return result("REST Assured", method, message, AssertionInterpreter.describe(expected),
                actual == Unknown.VALUE ? null : AssertionInterpreter.describe(actual), passed, thrown);
    }

    private static AssertionResult rest(String method, String message, Object expected, Object actual, Throwable thrown) {
        return result("REST Assured", method, message, expected == null ? null : AssertionInterpreter.describe(expected),
                actual == null ? null : AssertionInterpreter.describe(actual), thrown == null, thrown);
    }

    /** Whether a Hamcrest matcher (or plain expected value) accepts the actual value; false when unsure. */
    private static boolean matches(Object expected, Object actual) {
        try {
            for (Class<?> type : interfaces(expected.getClass())) {
                if ("org.hamcrest.Matcher".equals(type.getName())) {
                    Method m = type.getMethod("matches", Object.class);
                    return Boolean.TRUE.equals(m.invoke(expected, actual));
                }
            }
            return expected.equals(actual) || String.valueOf(expected).equals(String.valueOf(actual));
        } catch (Throwable t) {
            return false;
        }
    }

    private static List<Class<?>> interfaces(Class<?> type) {
        List<Class<?>> all = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            collect(c, all);
        }
        return all;
    }

    private static void collect(Class<?> type, List<Class<?>> all) {
        for (Class<?> i : type.getInterfaces()) {
            if (!all.contains(i)) {
                all.add(i);
                collect(i, all);
            }
        }
    }

    /** (Map) or (name, value, name2, value2, ...) as name/value pairs. */
    private static List<Object[]> pairs(Object[] values) {
        List<Object[]> pairs = new ArrayList<>();
        if (values.length == 1 && values[0] instanceof Map) {
            ((Map<?, ?>) values[0]).forEach((k, v) -> pairs.add(new Object[] {k, v}));
            return pairs;
        }
        List<Object> flat = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Object[]) {
                flat.addAll(List.of((Object[]) value));
            } else {
                flat.add(value);
            }
        }
        for (int i = 0; i + 1 < flat.size(); i += 2) {
            pairs.add(new Object[] {flat.get(i), flat.get(i + 1)});
        }
        return pairs;
    }

    private static Object first(Object[] values) {
        return values.length == 0 ? null : values[0];
    }

    // ---------- Reflection ----------

    /** Reads a field declared by the class named {@code declaringClass} in {@code target}'s hierarchy. */
    static Object field(Object target, String declaringClass, String name) {
        if (target == null) {
            return null;
        }
        try {
            for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
                if (c.getName().equals(declaringClass)) {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(target);
                }
            }
        } catch (Throwable t) {
            // unreadable: leave the value empty
        }
        return null;
    }

    private static String stringField(Object target, String declaringClass, String name) {
        Object value = field(target, declaringClass, name);
        return value == null ? null : value.toString();
    }

    private static Object call(Object target, String method, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == args.length && accepts(m, args)) {
                    return m.invoke(target, args);
                }
            }
        } catch (Throwable t) {
            // unreadable: leave the value empty
        }
        return null;
    }

    private static Object callWith(Object target, String method, Object... args) {
        return call(target, method, args);
    }

    private static boolean accepts(Method m, Object[] args) {
        Class<?>[] types = m.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (args[i] != null && !wrap(types[i]).isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        return type;
    }

    // ---------- Results ----------

    private static AssertionResult result(String framework, String method, String message, String expected,
                                          String actual, boolean passed, Throwable thrown) {
        return AssertionResult.builder()
                .framework(framework)
                .method(method)
                .message(message)
                .expected(expected)
                .actual(actual)
                .status(passed ? TestStatus.PASSED : TestStatus.FAILED)
                .errorMessage(passed || thrown == null ? null : errorText(thrown))
                .timestamp(Instant.now())
                .build();
    }

    private static AssertionResult copy(AssertionResult r, String expected, TestStatus status, String error) {
        return AssertionResult.builder()
                .framework(r.getFramework())
                .method(r.getMethod())
                .message(r.getMessage())
                .expected(expected)
                .actual(r.getActual())
                .status(status)
                .errorMessage(error)
                .timestamp(r.getTimestamp())
                .build();
    }

    private static String errorText(Throwable thrown) {
        String message = thrown.getMessage();
        return AssertionInterpreter.truncate(message != null && !message.isBlank() ? message.trim() : thrown.getClass().getName());
    }

    /** Marks an actual value that couldn't be read (as opposed to one that is null). */
    private enum Unknown {
        VALUE
    }
}
