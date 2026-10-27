package io.github.pulsereport.assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.github.pulsereport.core.model.AssertionResult;
import io.github.pulsereport.core.model.TestStatus;

class AssertionInterpreterTest {

    private static AssertionResult interpret(String owner, String method, String descriptor, Object... args) {
        return AssertionInterpreter.interpret(owner, method, descriptor, args, null, null);
    }

    @Test
    void junit5TakesExpectedFirstAndMessageLast() {
        AssertionResult r = interpret(AssertionInterpreter.JUNIT5, "assertEquals",
                "(IILjava/lang/String;)V", 200, 404, "Login API status code");

        assertEquals("JUnit 5", r.getFramework());
        assertEquals("assertEquals", r.getMethod());
        assertEquals("Login API status code", r.getMessage());
        assertEquals("200", r.getExpected());
        assertEquals("404", r.getActual());
        assertEquals(TestStatus.PASSED, r.getStatus());
        assertNull(r.getErrorMessage());
    }

    @Test
    void junit5EvaluatesMessageSuppliers() {
        Supplier<String> message = () -> "lazy message";
        AssertionResult r = interpret(AssertionInterpreter.JUNIT5, "assertTrue",
                "(ZLjava/util/function/Supplier;)V", true, message);

        assertEquals("lazy message", r.getMessage());
        assertEquals("true", r.getExpected());
        assertEquals("true", r.getActual());
    }

    @Test
    void junit4TakesMessageFirst() {
        AssertionResult r = AssertionInterpreter.interpret(AssertionInterpreter.JUNIT4, "assertEquals",
                "(Ljava/lang/String;JJ)V", new Object[] {"count", 1L, 2L}, null, new AssertionError("count expected:<1> but was:<2>"));

        assertEquals("JUnit 4", r.getFramework());
        assertEquals("count", r.getMessage());
        assertEquals("1", r.getExpected());
        assertEquals("2", r.getActual());
        assertEquals(TestStatus.FAILED, r.getStatus());
        assertEquals("count expected:<1> but was:<2>", r.getErrorMessage());
    }

    @Test
    void testngTakesActualFirst() {
        AssertionResult r = interpret(AssertionInterpreter.TESTNG, "assertEquals",
                "(IILjava/lang/String;)V", 404, 200, "status");

        assertEquals("200", r.getExpected());
        assertEquals("404", r.getActual());
        assertEquals("status", r.getMessage());
    }

    @Test
    void twoStringValuesAreNotMistakenForAMessage() {
        AssertionResult r = interpret(AssertionInterpreter.TESTNG, "assertEquals",
                "(Ljava/lang/String;Ljava/lang/String;)V", "Login", "Dashboard");

        assertNull(r.getMessage());
        assertEquals("\"Dashboard\"", r.getExpected());
        assertEquals("\"Login\"", r.getActual());
    }

    @Test
    void deltaIsShownWithTheExpectedValue() {
        AssertionResult r = interpret(AssertionInterpreter.JUNIT5, "assertEquals", "(DDD)V", 1.0, 1.05, 0.1);

        assertNull(r.getMessage());
        assertEquals("1.0 ± 0.1", r.getExpected());
        assertEquals("1.05", r.getActual());
    }

    @Test
    void negatedAssertionsArePrefixed() {
        AssertionResult r = interpret(AssertionInterpreter.JUNIT5, "assertNotEquals",
                "(Ljava/lang/Object;Ljava/lang/Object;)V", 5, 6);

        assertEquals("not 5", r.getExpected());
        assertEquals("6", r.getActual());
    }

    @Test
    void failFindsItsMessageAtEitherEnd() {
        AssertionResult r = interpret(AssertionInterpreter.JUNIT5, "fail",
                "(Ljava/lang/String;Ljava/lang/Throwable;)Ljava/lang/Object;", "boom", new RuntimeException());

        assertEquals("boom", r.getMessage());
        assertNull(r.getExpected());
        assertNull(r.getActual());
    }

    @Test
    void hamcrestUsesTheMatcherDescriptionAsExpected() {
        Object matcher = new Object() {
            @Override
            public String toString() {
                return "a value greater than <0>";
            }
        };
        AssertionResult r = interpret(AssertionInterpreter.HAMCREST, "assertThat",
                "(Ljava/lang/String;Ljava/lang/Object;Lorg/hamcrest/Matcher;)V", "name length", 5, matcher);

        assertEquals("Hamcrest", r.getFramework());
        assertEquals("name length", r.getMessage());
        assertEquals("a value greater than <0>", r.getExpected());
        assertEquals("5", r.getActual());
    }

    @Test
    void assertThrowsReportsTheExceptionTypes() {
        AssertionResult r = AssertionInterpreter.interpret(AssertionInterpreter.JUNIT5, "assertThrows",
                "(Ljava/lang/Class;Lorg/junit/jupiter/api/function/Executable;)Ljava/lang/Throwable;",
                new Object[] {IllegalStateException.class, (Runnable) () -> { }}, new IllegalStateException(), null);

        assertEquals(IllegalStateException.class.getName(), r.getExpected());
        assertEquals(IllegalStateException.class.getName(), r.getActual());
    }

    @Test
    void valuesAreRenderedReadably() {
        assertEquals("null", AssertionInterpreter.describe(null));
        assertEquals("[1, 2]", AssertionInterpreter.describe(new int[] {1, 2}));
        assertEquals("[[a], [b]]", AssertionInterpreter.describe(new String[][] {{"a"}, {"b"}}));
        assertEquals("'x'", AssertionInterpreter.describe('x'));
        assertEquals("<lambda>", AssertionInterpreter.describe((Runnable) () -> { }));
    }

    @Test
    void longValuesAreTruncated() {
        String value = AssertionInterpreter.describe("x".repeat(5000));
        assertTrue(value.length() < 2100, "length was " + value.length());
        assertTrue(value.endsWith("more chars)"));
    }

    @Test
    void parsesJvmDescriptors() {
        assertEquals(List.of("[I", "[[Ljava/lang/String;", "J", "Ljava/lang/Object;"),
                AssertionInterpreter.parameterTypes("([I[[Ljava/lang/String;JLjava/lang/Object;)V"));
        assertEquals(List.of(), AssertionInterpreter.parameterTypes("()V"));
    }
}
