package io.github.pulsereport.core.model;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Unit tests for Artifact model class.
 */
class ArtifactTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testBuilderCreatesArtifact() {
        Instant now = Instant.ofEpochMilli(1000);
        Artifact artifact = Artifact.builder()
                .name("screenshot1")
                .type("screenshot")
                .path("/path/to/screenshot.png")
                .timestamp(now)
                .build();

        assertEquals("screenshot1", artifact.getName());
        assertEquals("screenshot", artifact.getType());
        assertEquals("/path/to/screenshot.png", artifact.getPath());
        assertEquals(now, artifact.getTimestamp());
        assertNull(artifact.getMimeType());
        assertEquals(0, artifact.getSize());
    }

    @Test
    void testBuilderWithAllFields() {
        Instant now = Instant.ofEpochMilli(2000);
        Artifact artifact = Artifact.builder()
                .name("video1")
                .type("video")
                .path("/videos/test.mp4")
                .mimeType("video/mp4")
                .size(1024000)
                .timestamp(now)
                .build();

        assertEquals("video1", artifact.getName());
        assertEquals("video", artifact.getType());
        assertEquals("/videos/test.mp4", artifact.getPath());
        assertEquals("video/mp4", artifact.getMimeType());
        assertEquals(1024000, artifact.getSize());
        assertEquals(now, artifact.getTimestamp());
    }

    @Test
    void testBuilderRequiresName() {
        assertThrows(IllegalArgumentException.class, () ->
                Artifact.builder()
                        .type("log")
                        .path("/log.txt")
                        .timestamp(Instant.ofEpochMilli(1000))
                        .build()
        );
    }

    @Test
    void testBuilderRequiresType() {
        assertThrows(IllegalArgumentException.class, () ->
                Artifact.builder()
                        .name("artifact")
                        .path("/path")
                        .timestamp(Instant.ofEpochMilli(1000))
                        .build()
        );
    }

    @Test
    void testBuilderRequiresPath() {
        assertThrows(IllegalArgumentException.class, () ->
                Artifact.builder()
                        .name("artifact")
                        .type("log")
                        .timestamp(Instant.ofEpochMilli(1000))
                        .build()
        );
    }

    @Test
    void testBuilderRequiresTimestamp() {
        assertThrows(IllegalArgumentException.class, () ->
                Artifact.builder()
                        .name("artifact")
                        .type("log")
                        .path("/path")
                        .build()
        );
    }

    @Test
    void testJsonSerialization() throws Exception {
        Instant now = Instant.parse("2026-02-16T10:00:00Z");
        Artifact artifact = Artifact.builder()
                .name("log1")
                .type("log")
                .path("/logs/test.log")
                .mimeType("text/plain")
                .size(2048)
                .timestamp(now)
                .build();

        String json = objectMapper.writeValueAsString(artifact);
        assertTrue(json.contains("\"name\":\"log1\""));
        assertTrue(json.contains("\"type\":\"log\""));
        assertTrue(json.contains("\"path\":\"/logs/test.log\""));
        assertTrue(json.contains("\"mimeType\":\"text/plain\""));
        assertTrue(json.contains("\"size\":2048"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"name\":\"screenshot\",\"type\":\"screenshot\",\"path\":\"/img.png\",\"mimeType\":\"image/png\",\"size\":4096,\"timestamp\":\"2026-02-16T10:00:00Z\"}";
        Artifact artifact = objectMapper.readValue(json, Artifact.class);

        assertEquals("screenshot", artifact.getName());
        assertEquals("screenshot", artifact.getType());
        assertEquals("/img.png", artifact.getPath());
        assertEquals("image/png", artifact.getMimeType());
        assertEquals(4096, artifact.getSize());
        assertEquals(Instant.parse("2026-02-16T10:00:00Z"), artifact.getTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.ofEpochMilli(3000);
        Artifact artifact1 = Artifact.builder()
                .name("file")
                .type("document")
                .path("/doc.pdf")
                .timestamp(now)
                .build();

        Artifact artifact2 = Artifact.builder()
                .name("file")
                .type("document")
                .path("/doc.pdf")
                .timestamp(now)
                .build();

        assertEquals(artifact1, artifact2);
        assertEquals(artifact1.hashCode(), artifact2.hashCode());
    }

    @Test
    void testToString() {
        Artifact artifact = Artifact.builder()
                .name("test")
                .type("http-request")
                .path("/request.json")
                .timestamp(Instant.ofEpochMilli(4000))
                .build();

        String str = artifact.toString();
        assertTrue(str.contains("test"));
        assertTrue(str.contains("http-request"));
        assertTrue(str.contains("/request.json"));
    }

    @Test
    void testArtifactWithContent() {
        Instant now = Instant.ofEpochMilli(5000);
        String content = "{\"request\":\"data\"}";

        Artifact artifact = Artifact.builder()
                .name("http-request")
                .type("http-request")
                .path("/request.json")
                .content(content)
                .timestamp(now)
                .build();

        assertEquals("http-request", artifact.getName());
        assertEquals("http-request", artifact.getType());
        assertEquals("/request.json", artifact.getPath());
        assertEquals(content, artifact.getContent());
        assertEquals(now, artifact.getTimestamp());
    }

    @Test
    void testArtifactWithoutContent() {
        Instant now = Instant.ofEpochMilli(6000);

        Artifact artifact = Artifact.builder()
                .name("screenshot")
                .type("screenshot")
                .path("/screenshot.png")
                .timestamp(now)
                .build();

        assertEquals("screenshot", artifact.getName());
        assertEquals("screenshot", artifact.getType());
        assertEquals("/screenshot.png", artifact.getPath());
        assertNull(artifact.getContent());
        assertEquals(now, artifact.getTimestamp());
    }

    @Test
    void testArtifactContentSerialization() throws Exception {
        Instant now = Instant.parse("2026-02-17T10:00:00Z");
        String content = "{\"status\":200,\"body\":\"response\"}";
        Artifact artifact = Artifact.builder()
                .name("response")
                .type("http-response")
                .path("/response.json")
                .content(content)
                .mimeType("application/json")
                .size(32)
                .timestamp(now)
                .build();

        String json = objectMapper.writeValueAsString(artifact);

        assertTrue(json.contains("\"name\":\"response\""));
        assertTrue(json.contains("\"type\":\"http-response\""));
        assertTrue(json.contains("\"content\":\"" + content.replace("\"", "\\\"") + "\""));

        Artifact deserialized = objectMapper.readValue(json, Artifact.class);
        assertEquals(artifact.getName(), deserialized.getName());
        assertEquals(artifact.getContent(), deserialized.getContent());
    }
}
