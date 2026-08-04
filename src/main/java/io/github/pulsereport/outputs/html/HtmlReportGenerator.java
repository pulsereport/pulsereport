package io.github.pulsereport.outputs.html;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.outputs.OutputGenerator;

/**
 * Generates HTML reports from test run data using FreeMarker templates. Output
 * is self-contained with embedded CSS for offline viewing.
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class HtmlReportGenerator implements OutputGenerator {

    private final Configuration freemarkerConfig;

    /**
     * Creates a new HTML report generator with default FreeMarker
     * configuration.
     */
    public HtmlReportGenerator() {
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        this.freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates");
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
        this.freemarkerConfig.setLogTemplateExceptions(false);

        // Set the date/time format to handle Java 8 Instant
        this.freemarkerConfig.setAPIBuiltinEnabled(true);
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
            dataModel.put("testRun", testRun);
            dataModel.put("prettyPrintJson", new PrettyPrintJsonMethod());
            dataModel.put("prettyPrintXml", new PrettyPrintXmlMethod());
            dataModel.put("prettyPrintHttpBody", new PrettyPrintHttpBodyMethod());
            dataModel.put("toDataUri", new ToDataUriMethod());

            try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                template.process(dataModel, writer);
                writer.flush();
            }
        } catch (TemplateException e) {
            throw new IOException("Failed to process FreeMarker template", e);
        }
    }

    /**
     * Pretty-prints JSON with indentation.
     *
     * @param json the JSON string to format
     * @return formatted JSON with newlines and indentation
     */
    private String prettyPrintJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return json;
        }

        StringBuilder result = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);

            if (escape) {
                result.append(ch);
                escape = false;
                continue;
            }

            if (ch == '\\') {
                result.append(ch);
                escape = true;
                continue;
            }

            if (ch == '"') {
                inString = !inString;
                result.append(ch);
                continue;
            }

            if (inString) {
                result.append(ch);
                continue;
            }

            switch (ch) {
                case '{':
                case '[':
                    result.append(ch);
                    result.append('\n');
                    indent++;
                    result.append("  ".repeat(indent));
                    break;
                case '}':
                case ']':
                    result.append('\n');
                    indent--;
                    result.append("  ".repeat(indent));
                    result.append(ch);
                    break;
                case ',':
                    result.append(ch);
                    result.append('\n');
                    result.append("  ".repeat(indent));
                    break;
                case ':':
                    result.append(ch);
                    result.append(' ');
                    break;
                default:
                    if (!Character.isWhitespace(ch)) {
                        result.append(ch);
                    }
                    break;
            }
        }

        return result.toString();
    }

    /**
     * Pretty-prints XML with indentation.
     *
     * @param xml the XML string to format
     * @return formatted XML with newlines and indentation
     */
    private String prettyPrintXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return xml;
        }

        try {
            StringBuilder result = new StringBuilder();
            int indent = 0;
            boolean inTag = false;

            for (int i = 0; i < xml.length(); i++) {
                char ch = xml.charAt(i);

                if (ch == '<') {
                    if (i + 1 < xml.length() && xml.charAt(i + 1) == '/') {
                        indent = Math.max(0, indent - 1);
                        if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
                            result.append('\n');
                        }
                        result.append("  ".repeat(indent));
                    } else if (i > 0 && xml.charAt(i - 1) == '>') {
                        result.append('\n');
                        result.append("  ".repeat(indent));
                    }
                    inTag = true;
                    result.append(ch);
                } else if (ch == '>') {
                    result.append(ch);
                    inTag = false;
                    // Check if it's not a self-closing or closing tag
                    if (i > 0 && xml.charAt(i - 1) != '/') {
                        // Look ahead to see if next is immediately a closing tag
                        if (i + 1 < xml.length() && xml.charAt(i + 1) == '<'
                                && i + 2 < xml.length() && xml.charAt(i + 2) != '/') {
                            indent++;
                        } else if (i + 1 >= xml.length() || xml.charAt(i + 1) != '<') {
                            // There's content between tags
                            indent++;
                        }
                    }
                } else if (!inTag || !Character.isWhitespace(ch)) {
                    result.append(ch);
                }
            }

            return result.toString().trim();
        } catch (Exception e) {
            return xml;
        }
    }

    /**
     * Reformats an HTTP request content string so that any JSON found after the
     * "Body:" label is pretty-printed while the rest of the text is kept
     * unchanged.
     */
    private String prettyPrintHttpBody(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        // Split on the "Body:" separator (handles both "\nBody:\n" and leading "Body:\n")
        int bodyIdx = content.indexOf("\nBody:\n");
        if (bodyIdx == -1) {
            return content;
        }
        String prefix = content.substring(0, bodyIdx + "\nBody:\n".length());
        String body = content.substring(bodyIdx + "\nBody:\n".length());
        String trimmedBody = body.trim();
        if (!trimmedBody.isEmpty() && (trimmedBody.charAt(0) == '{' || trimmedBody.charAt(0) == '[')) {
            return prefix + prettyPrintJson(trimmedBody);
        }
        return content;
    }

    /**
     * FreeMarker method adapter for prettyPrintJson.
     */
    private class PrettyPrintJsonMethod implements TemplateMethodModelEx {

        @Override
        @SuppressWarnings("rawtypes")
        public Object exec(List arguments) throws TemplateModelException {
            if (arguments.isEmpty()) {
                return "";
            }
            String json = arguments.get(0).toString();
            return prettyPrintJson(json);
        }
    }

    /**
     * FreeMarker method adapter for prettyPrintHttpBody.
     */
    private class PrettyPrintHttpBodyMethod implements TemplateMethodModelEx {

        @Override
        @SuppressWarnings("rawtypes")
        public Object exec(List arguments) throws TemplateModelException {
            if (arguments.isEmpty()) {
                return "";
            }
            String content = arguments.get(0).toString();
            return prettyPrintHttpBody(content);
        }
    }

    /**
     * FreeMarker method adapter for prettyPrintXml.
     */
    private class PrettyPrintXmlMethod implements TemplateMethodModelEx {

        @Override
        @SuppressWarnings("rawtypes")
        public Object exec(List arguments) throws TemplateModelException {
            if (arguments.isEmpty()) {
                return "";
            }
            String xml = arguments.get(0).toString();
            return prettyPrintXml(xml);
        }
    }

    /**
     * FreeMarker method that reads an image file and returns a base64 data URI
     * so screenshots can be embedded directly inside the HTML report.
     */
    private class ToDataUriMethod implements TemplateMethodModelEx {

        @Override
        @SuppressWarnings("rawtypes")
        public Object exec(List arguments) throws TemplateModelException {
            if (arguments.size() < 2) {
                return "";
            }
            String path = arguments.get(0).toString();
            String mimeType = arguments.get(1).toString();
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(path));
                return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            } catch (IOException e) {
                return "";
            }
        }
    }
}
