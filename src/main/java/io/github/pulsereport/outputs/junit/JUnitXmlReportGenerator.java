package io.github.pulsereport.outputs.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.core.model.TestStatus;
import io.github.pulsereport.core.model.TestSuite;
import io.github.pulsereport.outputs.OutputGenerator;

/**
 * Generates JUnit XML reports from test run data. Output is compatible with
 * CI/CD parsers like Maven Surefire, Jenkins, and GitHub Actions.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class JUnitXmlReportGenerator implements OutputGenerator {

    /**
     * Creates a new JUnit XML report generator.
     */
    public JUnitXmlReportGenerator() {
    }

    /**
     * Generates a JUnit XML report from the given test run and writes it to the
     * specified file.
     *
     * @param testRun the test run data to generate a report from
     * @param outputFile the file to write the report to
     * @throws IOException if an I/O error occurs during report generation
     * @throws IllegalArgumentException if testRun or outputFile is null
     */
    @Override
    public void generate(TestRun testRun, File outputFile) throws IOException {
        if (testRun == null) {
            throw new IllegalArgumentException("testRun cannot be null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile cannot be null");
        }

        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            generate(testRun, fos);
        }
    }

    /**
     * Generates a JUnit XML report from the given test run and writes it to the
     * specified output stream. The output stream is not closed by this method.
     *
     * @param testRun the test run data to generate a report from
     * @param outputStream the output stream to write the report to
     * @throws IOException if an I/O error occurs during report generation
     * @throws IllegalArgumentException if testRun or outputStream is null
     */
    @Override
    public void generate(TestRun testRun, OutputStream outputStream) throws IOException {
        if (testRun == null) {
            throw new IllegalArgumentException("testRun cannot be null");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream cannot be null");
        }

        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writeXml(testRun, writer);
            writer.flush();
        }
    }

    /**
     * Writes the JUnit XML content to the given writer.
     *
     * @param testRun the test run data
     * @param writer the writer to write XML to
     * @throws IOException if an I/O error occurs
     */
    private void writeXml(TestRun testRun, Writer writer) throws IOException {
        int totalTests = 0;
        int totalFailures = 0;
        int totalErrors = 0;
        int totalSkipped = 0;
        double totalTime = 0.0;

        for (TestSuite suite : testRun.getSuites()) {
            totalTests += suite.getTestCases().size();
            for (TestCase testCase : suite.getTestCases()) {
                if (testCase.getStatus() == TestStatus.FAILED) {
                    totalFailures++;
                } else if (testCase.getStatus() == TestStatus.SKIPPED) {
                    totalSkipped++;
                }
            }
            totalTime += suite.getDuration() / 1000.0; // Convert to seconds
        }

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        writer.write("<testsuites tests=\"%d\" failures=\"%d\" errors=\"%d\" skipped=\"%d\" time=\"%.3f\">\n".formatted(
                totalTests, totalFailures, totalErrors, totalSkipped, totalTime));

        for (TestSuite suite : testRun.getSuites()) {
            writeTestSuite(suite, writer);
        }

        writer.write("</testsuites>\n");
    }

    /**
     * Writes a test suite element to the XML.
     *
     * @param suite the test suite
     * @param writer the writer to write XML to
     * @throws IOException if an I/O error occurs
     */
    private void writeTestSuite(TestSuite suite, Writer writer) throws IOException {
        int tests = suite.getTestCases().size();
        int failures = 0;
        int errors = 0;
        int skipped = 0;
        double time = suite.getDuration() / 1000.0; // Convert to seconds

        for (TestCase testCase : suite.getTestCases()) {
            if (testCase.getStatus() == TestStatus.FAILED) {
                failures++;
            } else if (testCase.getStatus() == TestStatus.SKIPPED) {
                skipped++;
            }
        }

        writer.write("  <testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" errors=\"%d\" skipped=\"%d\" time=\"%.3f\">\n".formatted(
                escapeXml(suite.getName()), tests, failures, errors, skipped, time));

        for (TestCase testCase : suite.getTestCases()) {
            writeTestCase(testCase, writer);
        }

        writer.write("  </testsuite>\n");
    }

    /**
     * Writes a test case element to the XML.
     *
     * @param testCase the test case
     * @param writer the writer to write XML to
     * @throws IOException if an I/O error occurs
     */
    private void writeTestCase(TestCase testCase, Writer writer) throws IOException {
        String name = testCase.getName();
        String className = testCase.getClassName() != null ? testCase.getClassName() : "";
        double time = testCase.getDuration() / 1000.0; // Convert to seconds

        writer.write("    <testcase name=\"%s\" classname=\"%s\" time=\"%.3f\"".formatted(
                escapeXml(name), escapeXml(className), time));

        // If test passed or is flaky, self-close the tag
        if (testCase.getStatus() == TestStatus.PASSED || testCase.getStatus() == TestStatus.FLAKY) {
            writer.write(" />\n");
        } else if (testCase.getStatus() == TestStatus.SKIPPED) {
            writer.write(">\n");
            writer.write("      <skipped />\n");
            writer.write("    </testcase>\n");
        } else if (testCase.getStatus() == TestStatus.FAILED) {
            writer.write(">\n");
            String message = testCase.getErrorMessage() != null ? testCase.getErrorMessage() : "Test failed";
            String stackTrace = testCase.getStackTrace() != null ? testCase.getStackTrace() : "";

            writer.write("      <failure message=\"%s\">%s</failure>\n".formatted(
                    escapeXml(message), escapeXml(stackTrace)));
            writer.write("    </testcase>\n");
        } else {
            writer.write(" />\n");
        }
    }

    /**
     * Escapes special XML characters in text.
     *
     * @param text the text to escape
     * @return the escaped text
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
