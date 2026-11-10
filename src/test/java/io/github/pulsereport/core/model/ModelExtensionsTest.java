package io.github.pulsereport.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Sub-steps, metric labels, adapter names and toBuilder copies. */
class ModelExtensionsTest {

    private static final Instant T = Instant.parse("2026-11-10T10:00:00Z");
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void stepChildrenSurviveJson() throws Exception {
        TestStep child = TestStep.builder().name("Click button \"Sign in\"").status(TestStatus.FAILED)
                .errorMessage("intercepted").build();
        TestStep step = TestStep.builder().name("Log in").status(TestStatus.FAILED).startTime(T)
                .children(List.of(child)).build();

        TestStep read = mapper.readValue(mapper.writeValueAsString(step), TestStep.class);

        assertEquals(step, read);
        assertEquals("intercepted", read.getChildren().get(0).getErrorMessage());
    }

    @Test
    void stepsWithoutChildrenLeaveTheFieldOut() throws Exception {
        String json = mapper.writeValueAsString(TestStep.builder().name("Open").status(TestStatus.PASSED).build());
        assertFalse(json.contains("children"), json);
    }

    @Test
    void metricLabelAndAdapterSurviveJson() throws Exception {
        Metric metric = Metric.builder().name("page.load.time").value(420).unit("ms").timestamp(T)
                .label("https://example.test/login").build();
        TestCase tc = TestCase.builder().id("t").name("login").status(TestStatus.PASSED).adapter("Selenium")
                .metrics(List.of(metric)).build();

        TestCase read = mapper.readValue(mapper.writeValueAsString(tc), TestCase.class);

        assertEquals("Selenium", read.getAdapter());
        assertEquals("https://example.test/login", read.getMetrics().get(0).getLabel());
    }

    @Test
    void toBuilderCopiesEveryField() {
        TestStep step = TestStep.builder().name("s").status(TestStatus.PASSED).keyword("Setup").description("d")
                .startTime(T).endTime(T).duration(5).docString("doc").dataTable(List.of(List.of("a")))
                .errorMessage("e").stackTrace("st")
                .children(List.of(TestStep.builder().name("c").status(TestStatus.PASSED).build())).build();
        TestCase tc = TestCase.builder().id("t").name("n").className("C").methodName("m").startTime(T).endTime(T)
                .duration(9).status(TestStatus.FAILED).errorMessage("e").stackTrace("st").steps(List.of(step))
                .retryCount(1).bddType("SCENARIO").featureName("f").featureDescription("fd")
                .backgroundSteps(List.of(step)).tags(List.of("@x")).adapter("Selenium").build();
        TestSuite suite = TestSuite.builder().id("s").name("suite").secondaryText("sec").startTime(T).endTime(T)
                .duration(9).status(TestStatus.FAILED).testCases(List.of(tc)).totalTests(1).failedTests(1)
                .tags(List.of("smoke")).build();
        TestRun run = TestRun.builder().id("r").name("run").startTime(T).endTime(T).duration(9)
                .status(TestStatus.FAILED).suites(List.of(suite)).environment(Map.of("browser", "Chrome"))
                .totalTests(1).failedTests(1).build();

        assertEquals(step, step.toBuilder().build());
        assertEquals(tc, tc.toBuilder().build());
        assertEquals(suite, suite.toBuilder().build());
        assertEquals(run, run.toBuilder().build());
    }
}
