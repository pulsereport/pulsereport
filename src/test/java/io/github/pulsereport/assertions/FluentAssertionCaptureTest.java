package io.github.pulsereport.assertions;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import io.github.pulsereport.core.model.AssertionResult;
import io.github.pulsereport.core.model.TestStatus;

/**
 * AssertJ fluent checks and REST Assured response checks under the installed
 * agent. The checks on the captured results run after the collector is closed,
 * so they are not captured themselves.
 */
class FluentAssertionCaptureTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void installAgentAndStartServer() throws Exception {
        assertTrue(AssertionCapture.install(), "assertion capture agent should attach in the test JVM");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/users/1", exchange -> {
            byte[] body = "{\"id\":1,\"name\":\"Ann\",\"roles\":[\"admin\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("X-Request-Id", "abc-123");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static List<AssertionResult> capture(Runnable body) {
        Object collector = AssertionCollector.open();
        try {
            body.run();
        } catch (AssertionError expected) {
            // failing checks are part of the scenarios under test
        }
        return AssertionCollector.close(collector);
    }

    // ---------- AssertJ ----------

    @Test
    void capturesPassingAssertJCheckWithItsDescription() {
        List<AssertionResult> captured = capture(() -> Assertions.assertThat("Ann").as("user name").isEqualTo("Ann"));

        assertEquals(1, captured.size(), captured.toString());
        AssertionResult r = captured.get(0);
        assertEquals("AssertJ", r.getFramework());
        assertEquals("isEqualTo", r.getMethod());
        assertEquals("user name", r.getMessage());
        assertEquals("\"Ann\"", r.getExpected());
        assertEquals("\"Ann\"", r.getActual());
        assertEquals(TestStatus.PASSED, r.getStatus());
    }

    @Test
    void capturesFailingAssertJCheckAndStillThrows() {
        AssertionError[] thrown = new AssertionError[1];
        List<AssertionResult> captured = capture(() -> {
            try {
                Assertions.assertThat(List.of("a", "b")).hasSize(3);
            } catch (AssertionError e) {
                thrown[0] = e;
                throw e;
            }
        });

        assertNotNull(thrown[0], "the user's assertion must still fail");
        assertEquals(1, captured.size(), captured.toString());
        AssertionResult r = captured.get(0);
        assertEquals("hasSize", r.getMethod());
        assertEquals("3", r.getExpected());
        assertEquals("[a, b]", r.getActual());
        assertEquals(TestStatus.FAILED, r.getStatus());
        assertNotNull(r.getErrorMessage());
    }

    @Test
    void eachCheckInAChainIsItsOwnEntry() {
        List<AssertionResult> captured = capture(() -> Assertions.assertThat("Ann").isNotNull().startsWith("A"));

        assertEquals(List.of("isNotNull", "startsWith"), captured.stream().map(AssertionResult::getMethod).toList());
        assertEquals("not null", captured.get(0).getExpected());
    }

    @Test
    void softAssertionFailuresAreRecordedAsFailed() {
        List<AssertionResult> captured = capture(() -> {
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(1).isEqualTo(2);
            softly.assertThat("x").isEqualTo("x");
            softly.assertAll();
        });

        assertEquals(List.of(TestStatus.FAILED, TestStatus.PASSED),
                captured.stream().map(AssertionResult::getStatus).toList(), captured.toString());
    }

    @Test
    void assertionsInsideSatisfiesStayVisible() {
        List<AssertionResult> captured = capture(() -> Assertions.assertThat("Ann")
                .satisfies(name -> Assertions.assertThat(name).hasSize(3)));

        assertEquals(List.of("hasSize"), captured.stream().map(AssertionResult::getMethod).toList());
    }

    // ---------- REST Assured ----------

    @Test
    void capturesRestAssuredStatusAndHeaderChecks() {
        List<AssertionResult> captured = capture(() -> given().get(baseUrl + "/users/1").then()
                .statusCode(200)
                .header("X-Request-Id", "abc-123")
                .contentType(startsWith("application/json")));

        assertEquals(3, captured.size(), captured.toString());
        AssertionResult status = captured.get(0);
        assertEquals("REST Assured", status.getFramework());
        assertEquals("statusCode", status.getMethod());
        assertEquals("200", status.getExpected());
        assertEquals("200", status.getActual());
        assertEquals(TestStatus.PASSED, status.getStatus());

        AssertionResult header = captured.get(1);
        assertEquals("header", header.getMethod());
        assertEquals("X-Request-Id", header.getMessage());
        assertEquals("\"abc-123\"", header.getActual());
        assertEquals("contentType", captured.get(2).getMethod());
    }

    @Test
    void failingStatusCheckShowsTheActualStatus() {
        List<AssertionResult> captured = capture(() -> given().get(baseUrl + "/users/1").then().statusCode(404));

        AssertionResult r = captured.get(0);
        assertEquals("404", r.getExpected());
        assertEquals("200", r.getActual());
        assertEquals(TestStatus.FAILED, r.getStatus());
        assertNotNull(r.getErrorMessage());
    }

    @Test
    void bodyCheckWithSeveralPathsBlamesOnlyTheFailingOne() {
        List<AssertionResult> captured = capture(() -> given().get(baseUrl + "/users/1").then()
                .body("id", equalTo(1), "name", equalTo("Bob")));

        assertEquals(2, captured.size(), captured.toString());
        AssertionResult id = captured.get(0);
        assertEquals("body", id.getMethod());
        assertEquals("id", id.getMessage());
        assertEquals("1", id.getActual());
        assertEquals(TestStatus.PASSED, id.getStatus());

        AssertionResult name = captured.get(1);
        assertEquals("name", name.getMessage());
        assertEquals("\"Ann\"", name.getActual());
        assertEquals(TestStatus.FAILED, name.getStatus());
    }

    @Test
    void bodyPathsAreResolvedAgainstTheRootPath() {
        List<AssertionResult> captured = capture(() -> given().get(baseUrl + "/users/1").then()
                .rootPath("roles").body("[0]", equalTo("admin")));

        assertEquals(1, captured.size(), captured.toString());
        assertEquals("\"admin\"", captured.get(0).getActual());
        assertEquals(TestStatus.PASSED, captured.get(0).getStatus());
    }
}
