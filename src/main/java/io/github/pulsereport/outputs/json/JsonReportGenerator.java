package io.github.pulsereport.outputs.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.outputs.OutputGenerator;

/**
 * Generates JSON reports from test run data using Jackson. Output is
 * pretty-printed and includes all test run information.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class JsonReportGenerator implements OutputGenerator {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new JSON report generator with default configuration.
     */
    public JsonReportGenerator() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Generates a JSON report from the given test run and writes it to the
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
     * Generates a JSON report from the given test run and writes it to the
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

        objectMapper.writeValue(outputStream, testRun);
    }
}
