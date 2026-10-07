package io.github.pulsereport.outputs.html;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.github.pulsereport.config.ReporterConfig;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.outputs.OutputGenerator;

/**
 * Generates a self-contained HTML report. The test run is embedded as JSON and
 * rendered in the browser, so the file needs no network access.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class HtmlReportGenerator implements OutputGenerator {

    private static final String DEFAULT_RUN_NAME = "Test run";

    private final Configuration freemarkerConfig;
    private final ObjectMapper objectMapper;
    private final String reportTitle;

    /**
     * Creates a generator whose title comes from {@code reporter.report.title}
     * (system property or auto-detected reporter.properties), falling back to
     * the test run name.
     */
    public HtmlReportGenerator() {
        this(ReporterConfig.resolveReportTitle());
    }

    /**
     * Creates a generator with an explicit report title.
     *
     * @param reportTitle title for the header and browser tab; null or blank
     * uses the test run name
     */
    public HtmlReportGenerator(String reportTitle) {
        this.reportTitle = reportTitle == null || reportTitle.isBlank() ? null : reportTitle.trim();
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        this.freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates");
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
        this.freemarkerConfig.setLogTemplateExceptions(false);

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Generates an HTML report from the given test run and writes it to the
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
     * Generates an HTML report from the given test run and writes it to the
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

        try {
            Template template = freemarkerConfig.getTemplate("html-report.ftl");

            Map<String, Object> dataModel = new HashMap<>();
            String name = reportTitle != null ? reportTitle : testRun.getName();
            dataModel.put("runName", name == null || name.isBlank() ? DEFAULT_RUN_NAME : name);
            dataModel.put("runJson", toScriptSafeJson(testRun));

            try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                template.process(dataModel, writer);
                writer.flush();
            }
        } catch (TemplateException e) {
            throw new IOException("Failed to process FreeMarker template", e);
        }
    }

    private String toScriptSafeJson(TestRun testRun) throws IOException {
        JsonNode tree = objectMapper.valueToTree(testRun);
        inlineImageArtifacts(tree);
        return escapeForScriptBlock(objectMapper.writeValueAsString(tree));
    }

    private void inlineImageArtifacts(JsonNode node) {
        JsonNode artifacts = node.isObject() ? node.get("artifacts") : null;
        if (artifacts != null && artifacts.isArray()) {
            for (JsonNode artifact : artifacts) {
                if (artifact.isObject()) {
                    inlineImage((ObjectNode) artifact);
                }
            }
        }
        node.forEach(this::inlineImageArtifacts);
    }

    /** Reads a path-backed screenshot into base64 content so the report stays viewable when moved. */
    private void inlineImage(ObjectNode artifact) {
        if (!artifact.path("content").asText("").isEmpty()) {
            return;
        }
        String mimeType = artifact.path("mimeType").asText("");
        boolean image = mimeType.startsWith("image/") || "screenshot".equals(artifact.path("type").asText(""));
        String path = artifact.path("path").asText("");
        if (!image || path.isEmpty()) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            artifact.put("content", Base64.getEncoder().encodeToString(bytes));
            if (mimeType.isEmpty()) {
                artifact.put("mimeType", "image/png");
            }
        } catch (IOException | InvalidPathException e) {
            // Unreadable screenshots stay path-only; the report renders them as not embedded.
        }
    }

    /**
     * Escapes characters that could end a {@code <script>} element or break JavaScript parsing.
     * They only occur inside JSON strings, where the unicode escapes decode to the same text.
     */
    private static String escapeForScriptBlock(String json) {
        StringBuilder out = new StringBuilder(json.length() + 32);
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            switch (c) {
                case '<' -> out.append("\\u003c");
                case '>' -> out.append("\\u003e");
                case '&' -> out.append("\\u0026");
                case '\u2028' -> out.append("\\u2028");
                case '\u2029' -> out.append("\\u2029");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
