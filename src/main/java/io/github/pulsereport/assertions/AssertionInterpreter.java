package io.github.pulsereport.assertions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.github.pulsereport.core.model.AssertionResult;
import io.github.pulsereport.core.model.TestStatus;

/**
 * Turns an intercepted assertion call into an {@link AssertionResult}.
 *
 * <p>Each library orders its parameters differently: JUnit takes
 * {@code (expected, actual, message)}, JUnit 4 puts the message first, and TestNG
 * takes {@code (actual, expected, message)}. Parameter roles are therefore resolved
 * from the declaring class, the method name and its JVM descriptor.</p>
 */
final class AssertionInterpreter {

    static final String JUNIT5 = "org.junit.jupiter.api.Assertions";
    static final String JUNIT4 = "org.junit.Assert";
    static final String TESTNG = "org.testng.Assert";
    static final String HAMCREST = "org.hamcrest.MatcherAssert";

    static final int MAX_VALUE_LENGTH = 2000;

    private static final Set<String> TWO_VALUES = Set.of(
            "assertEquals", "assertNotEquals", "assertSame", "assertNotSame", "assertArrayEquals",
            "assertIterableEquals", "assertLinesMatch", "assertEqualsNoOrder", "assertEqualsDeep",
            "assertNotEqualsDeep", "assertInstanceOf", "assertThrows", "assertThrowsExactly", "expectThrows");
    private static final Set<String> ONE_VALUE = Set.of("assertTrue", "assertFalse", "assertNull", "assertNotNull");
    private static final Set<String> NEGATED = Set.of("assertNotEquals", "assertNotSame", "assertNotEqualsDeep");
    private static final Set<String> THROWS = Set.of("assertThrows", "assertThrowsExactly", "expectThrows");

    private static final String STRING = "Ljava/lang/String;";
    private static final String SUPPLIER = "Ljava/util/function/Supplier;";
    private static final String MATCHER = "Lorg/hamcrest/Matcher;";

    private AssertionInterpreter() {
    }

    static AssertionResult interpret(String owner, String method, String descriptor,
                                     Object[] args, Object returned, Throwable thrown) {
        Object[] values = args != null ? args : new Object[0];
        List<String> types = parameterTypes(descriptor);
        int n = Math.min(types.size(), values.length);

        String message = null;
        String expected = null;
        String actual = null;

        if ("assertThat".equals(method) && n >= 2 && MATCHER.equals(types.get(n - 1))) {
            message = n >= 3 ? text(values[0]) : null;
            actual = describe(values[n - 2]);
            expected = matcherText(values[n - 1]);
        } else if ("assertThat".equals(method) && n == 2 && "Z".equals(types.get(1))) {
            message = text(values[0]);
            expected = "true";
            actual = describe(values[1]);
        } else {
            boolean messageFirst = JUNIT4.equals(owner);
            int messageIndex = messageIndex(method, types, n, messageFirst);
            message = messageIndex >= 0 ? text(values[messageIndex]) : null;

            List<Object> rest = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (i != messageIndex) {
                    rest.add(values[i]);
                }
            }

            if (THROWS.contains(method)) {
                Object type = rest.isEmpty() ? null : rest.get(0);
                expected = type instanceof Class ? ((Class<?>) type).getName() : Throwable.class.getName();
                actual = thrown == null && returned != null ? returned.getClass().getName() : null;
            } else if ("assertTrue".equals(method) || "assertFalse".equals(method)) {
                boolean wanted = "assertTrue".equals(method);
                expected = String.valueOf(wanted);
                Object condition = rest.isEmpty() ? null : rest.get(0);
                actual = condition instanceof Boolean
                        ? condition.toString()
                        : String.valueOf(thrown == null ? wanted : !wanted);
            } else if ("assertNull".equals(method) || "assertNotNull".equals(method)) {
                expected = "assertNull".equals(method) ? "null" : "not null";
                actual = rest.isEmpty() ? null : describe(rest.get(0));
            } else if ("assertInstanceOf".equals(method) && rest.size() >= 2) {
                expected = rest.get(0) instanceof Class ? ((Class<?>) rest.get(0)).getName() : describe(rest.get(0));
                actual = rest.get(1) == null ? "null" : rest.get(1).getClass().getName();
            } else if (TWO_VALUES.contains(method) && rest.size() >= 2) {
                boolean actualFirst = TESTNG.equals(owner);
                Object exp = actualFirst ? rest.get(1) : rest.get(0);
                Object act = actualFirst ? rest.get(0) : rest.get(1);
                expected = (NEGATED.contains(method) ? "not " : "") + describe(exp);
                if (rest.size() >= 3 && rest.get(2) instanceof Number) {
                    expected += " ± " + rest.get(2);
                }
                actual = describe(act);
            } else if (!rest.isEmpty() && !"fail".equals(method)) {
                List<String> shown = new ArrayList<>();
                for (Object value : rest) {
                    shown.add(describe(value));
                }
                actual = truncate(String.join(", ", shown));
            }
        }

        return AssertionResult.builder()
                .framework(frameworkLabel(owner))
                .method(method)
                .message(blankToNull(message))
                .expected(expected)
                .actual(actual)
                .status(thrown == null ? TestStatus.PASSED : TestStatus.FAILED)
                .errorMessage(thrown == null ? null : errorText(thrown))
                .timestamp(Instant.now())
                .build();
    }

    static String frameworkLabel(String owner) {
        if (JUNIT5.equals(owner)) return "JUnit 5";
        if (JUNIT4.equals(owner)) return "JUnit 4";
        if (TESTNG.equals(owner)) return "TestNG";
        if (HAMCREST.equals(owner)) return "Hamcrest";
        return owner;
    }

    /**
     * Finds the message parameter. It exists only when the method takes more
     * parameters than it has values (so {@code assertEquals(String, String)} has
     * none), and sits at the library's preferred end with a fallback to the other
     * end for overloads such as {@code fail(String, Throwable)}.
     */
    private static int messageIndex(String method, List<String> types, int n, boolean messageFirst) {
        int arity = "fail".equals(method) ? 0
                : TWO_VALUES.contains(method) ? 2
                : ONE_VALUE.contains(method) ? 1
                : 1;
        if (n <= arity) {
            return -1;
        }
        int preferred = messageFirst ? 0 : n - 1;
        int other = messageFirst ? n - 1 : 0;
        if (isMessageType(types.get(preferred))) return preferred;
        if (isMessageType(types.get(other))) return other;
        return -1;
    }

    private static boolean isMessageType(String type) {
        return STRING.equals(type) || SUPPLIER.equals(type);
    }

    static List<String> parameterTypes(String descriptor) {
        List<String> types = new ArrayList<>();
        if (descriptor == null) {
            return types;
        }
        int i = descriptor.indexOf('(') + 1;
        int end = descriptor.indexOf(')');
        if (i <= 0 || end < i) {
            return types;
        }
        while (i < end) {
            int start = i;
            while (descriptor.charAt(i) == '[') {
                i++;
            }
            if (descriptor.charAt(i) == 'L') {
                i = descriptor.indexOf(';', i);
            }
            i++;
            types.add(descriptor.substring(start, i));
        }
        return types;
    }

    private static String text(Object message) {
        try {
            Object value = message instanceof Supplier ? ((Supplier<?>) message).get() : message;
            return value == null ? null : truncate(value.toString());
        } catch (Throwable t) {
            return null;
        }
    }

    private static String matcherText(Object matcher) {
        try {
            return matcher == null ? null : truncate(matcher.toString());
        } catch (Throwable t) {
            return matcher.getClass().getName();
        }
    }

    static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            String text;
            if (value instanceof CharSequence) {
                text = "\"" + value + "\"";
            } else if (value instanceof Character) {
                text = "'" + value + "'";
            } else if (value instanceof Class) {
                text = ((Class<?>) value).getName();
            } else if (value.getClass().isArray()) {
                String wrapped = Arrays.deepToString(new Object[] {value});
                text = wrapped.substring(1, wrapped.length() - 1);
            } else if (isLambda(value)) {
                text = "<lambda>";
            } else {
                text = String.valueOf(value);
            }
            return truncate(text);
        } catch (Throwable t) {
            return value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
        }
    }

    private static boolean isLambda(Object value) {
        Class<?> type = value.getClass();
        return type.isSynthetic() || type.getName().contains("$$Lambda");
    }

    private static String errorText(Throwable thrown) {
        String message = thrown.getMessage();
        return truncate(message != null && !message.isBlank() ? message : thrown.getClass().getName());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static String truncate(String value) {
        if (value == null || value.length() <= MAX_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_VALUE_LENGTH) + "… (" + (value.length() - MAX_VALUE_LENGTH) + " more chars)";
    }
}
