package io.github.pulsereport.integrations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Finds the files a report links to (screenshots, videos and logs that
 * adapters store under the report folder, such as
 * {@code artifacts/screenshots/00012-step-failure.png} or
 * {@code videos/LoginTest.login-1a2b.mp4}), so publishers can upload them
 * next to the report and its links keep working.
 *
 * @since 1.3.0
 */
public final class ReportFiles {

    private static final String DEFAULT_JSON = "test-report.json";

    private ReportFiles() {
    }

    /**
     * Lists the existing files, relative to the report's folder, that the
     * report's JSON references by relative path. Uses the report itself when it
     * is JSON, otherwise a JSON file with the same base name or
     * {@code test-report.json} next to it.
     *
     * @param reportFile the published report (HTML or JSON)
     * @return relative paths with forward slashes; empty when no JSON is found
     */
    public static List<String> referencedFiles(File reportFile) {
        File dir = reportFile.getAbsoluteFile().getParentFile();
        File json = findJson(reportFile);
        if (dir == null || json == null) {
            return List.of();
        }
        Set<String> paths = new LinkedHashSet<>();
        try {
            collect(new ObjectMapper().readTree(json), dir.toPath().normalize(), paths);
        } catch (IOException e) {
            return List.of();
        }
        return new ArrayList<>(paths);
    }

    /**
     * @param fileName a file name
     * @return a content type for common report file types, or null
     */
    public static String contentType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        switch (dot < 0 ? "" : lower.substring(dot + 1)) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "html":
            case "htm":
                return "text/html; charset=utf-8";
            case "json":
            case "har":
                return "application/json";
            case "xml":
                return "application/xml";
            case "log":
            case "txt":
                return "text/plain; charset=utf-8";
            default:
                return null;
        }
    }

    private static File findJson(File reportFile) {
        if (reportFile.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
            return reportFile;
        }
        File dir = reportFile.getAbsoluteFile().getParentFile();
        String name = reportFile.getName();
        int dot = name.lastIndexOf('.');
        File sameName = new File(dir, (dot > 0 ? name.substring(0, dot) : name) + ".json");
        if (sameName.isFile()) {
            return sameName;
        }
        File fallback = new File(dir, DEFAULT_JSON);
        return fallback.isFile() ? fallback : null;
    }

    private static void collect(JsonNode node, Path dir, Set<String> paths) {
        if (node.isObject() && node.path("path").isTextual() && node.has("type")) {
            add(node.path("path").asText(), dir, paths);
        }
        node.forEach(child -> collect(child, dir, paths));
    }

    private static void add(String path, Path dir, Set<String> paths) {
        if (path.isBlank() || path.contains(":") || path.startsWith("/") || path.startsWith("\\")) {
            return; // absolute paths and URLs are not part of the report folder
        }
        try {
            Path resolved = dir.resolve(Paths.get(path)).normalize();
            if (resolved.startsWith(dir) && Files.isRegularFile(resolved)) {
                paths.add(dir.relativize(resolved).toString().replace('\\', '/'));
            }
        } catch (InvalidPathException e) {
            // not a file path
        }
    }
}
