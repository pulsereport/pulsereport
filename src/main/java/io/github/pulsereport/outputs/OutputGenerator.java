package io.github.pulsereport.outputs;

import io.github.pulsereport.core.model.TestRun;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for generating test reports in various formats.
 * Implementations provide different output formats such as HTML, JSON, and JUnit XML.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public interface OutputGenerator {
    
    /**
     * Generates a report from the given test run and writes it to the specified file.
     * 
     * @param testRun the test run data to generate a report from
     * @param outputFile the file to write the report to
     * @throws IOException if an I/O error occurs during report generation
     * @throws IllegalArgumentException if testRun or outputFile is null
     */
    void generate(TestRun testRun, File outputFile) throws IOException;
    
    /**
     * Generates a report from the given test run and writes it to the specified output stream.
     * The output stream is not closed by this method.
     * 
     * @param testRun the test run data to generate a report from
     * @param outputStream the output stream to write the report to
     * @throws IOException if an I/O error occurs during report generation
     * @throws IllegalArgumentException if testRun or outputStream is null
     */
    void generate(TestRun testRun, OutputStream outputStream) throws IOException;
}
