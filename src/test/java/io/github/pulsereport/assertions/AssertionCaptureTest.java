package io.github.pulsereport.assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testng.Assert;
import org.testng.asserts.SoftAssert;

import io.github.pulsereport.core.model.AssertionResult;
import io.github.pulsereport.core.model.TestStatus;

/**
 * Runs real assertion libraries under the installed agent. The checks on the
 * captured results run after the collector is closed, so they are not captured
 * themselves.
 */
class AssertionCaptureTest {

    @BeforeAll
    static void installAgent() {
        assertTrue(AssertionCapture.install(), "assertion capture agent should attach in the test JVM");
    }

    private static List<AssertionResult> capture(Runnable body) {
        AssertionCollector.start();
        try {
            body.run();
        } catch (AssertionError expected) {
            // failing assertions are part of the scenarios under test
        }
        return AssertionCollector.stop();
    }

    @Test
    void capturesPassingJUnit5Assertion() {
        List<AssertionResult> captured = capture(() -> Assertions.assertEquals(200, 200, "Login API status code"));

        assertEquals(1, captured.size(), captured.toString());
        AssertionResult r = captured.get(0);
        assertEquals("JUnit 5", r.getFramework());
        assertEquals("assertEquals", r.getMethod());
        assertEquals("Login API status code", r.getMessage());
        assertEquals("200", r.getExpected());
        assertEquals("200", r.getActual());
        assertEquals(TestStatus.PASSED, r.getStatus());
    }

    @Test
    void capturesFailingJUnit5AssertionAndStillThrows() {
        AssertionCollector.start();
        AssertionError thrown = null;
        try {
            Assertions.assertEquals("Dashboard", "Login", "Page title");
        } catch (AssertionError e) {
            thrown = e;
        }
        List<AssertionResult> captured = AssertionCollector.stop();

        assertTrue(thrown != null, "the user's assertion must still fail");
        assertEquals(1, captured.size(), captured.toString());
        AssertionResult r = captured.get(0);
        assertEquals(TestStatus.FAILED, r.getStatus());
        assertEquals("\"Dashboard\"", r.getExpected());
        assertEquals("\"Login\"", r.getActual());
        assertEquals(thrown.getMessage(), r.getErrorMessage());
    }

    @Test
    void recordsEachAssertionInsideAssertAll() {
        List<AssertionResult> captured = capture(() -> Assertions.assertAll(
                () -> Assertions.assertEquals(1, 1, "first"),
                () -> Assertions.assertNotNull("x", "second")));

        assertEquals(List.of("first", "second"), captured.stream().map(AssertionResult::getMessage).toList());
    }

    @Test
    void capturesAssertThrowsOnce() {
        List<AssertionResult> captured = capture(() -> Assertions.assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("nope");
        }));

        assertEquals(1, captured.size(), captured.toString());
        assertEquals(IllegalStateException.class.getName(), captured.get(0).getActual());
        assertEquals(TestStatus.PASSED, captured.get(0).getStatus());
    }

    @Test
    void capturesTestNGWithActualFirstAndOnlyTheOuterOverload() {
        List<AssertionResult> captured = capture(() -> {
            Assert.assertTrue(true);
            Assert.assertEquals(404, 200, "status");
        });

        assertEquals(2, captured.size(), "internal overload delegation must not be recorded: " + captured);
        assertEquals("assertTrue", captured.get(0).getMethod());
        AssertionResult r = captured.get(1);
        assertEquals("TestNG", r.getFramework());
        assertEquals("status", r.getMessage());
        assertEquals("200", r.getExpected());
        assertEquals("404", r.getActual());
        assertEquals(TestStatus.FAILED, r.getStatus());
    }

    @Test
    void capturesEverySoftAssertion() {
        SoftAssert soft = new SoftAssert();
        List<AssertionResult> captured = capture(() -> {
            soft.assertEquals(1, 2, "soft failure");
            soft.assertEquals(3, 3, "soft pass");
        });

        assertEquals(2, captured.size(), captured.toString());
        assertEquals(TestStatus.FAILED, captured.get(0).getStatus());
        assertEquals(TestStatus.PASSED, captured.get(1).getStatus());
        Assertions.assertThrows(AssertionError.class, soft::assertAll);
    }

    @Test
    void capturesHamcrestWithMatcherDescription() {
        List<AssertionResult> captured = capture(() ->
                MatcherAssert.assertThat("name length", 5, Matchers.greaterThan(0)));

        assertEquals(1, captured.size(), captured.toString());
        AssertionResult r = captured.get(0);
        assertEquals("Hamcrest", r.getFramework());
        assertEquals("name length", r.getMessage());
        assertEquals("a value greater than <0>", r.getExpected());
        assertEquals("5", r.getActual());
    }

    @Test
    void ignoresAssertionsWhenNoCollectorIsOpen() {
        Assertions.assertEquals(1, 1);
        assertTrue(AssertionCollector.stop().isEmpty());
    }

    @Test
    void assertionsInsideToStringAreNotRecorded() {
        Object noisy = new Object() {
            @Override
            public String toString() {
                Assertions.assertTrue(true, "inside toString");
                return "noisy";
            }
        };
        List<AssertionResult> captured = capture(() -> Assertions.assertNotNull(noisy, "outer"));

        assertEquals(1, captured.size(), captured.toString());
        assertEquals("outer", captured.get(0).getMessage());
        assertEquals("noisy", captured.get(0).getActual());
        assertNull(captured.get(0).getErrorMessage());
    }
}
