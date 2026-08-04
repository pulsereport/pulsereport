package io.github.pulsereport.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test suite for ReporterConfig. Tests configuration loading, validation, and
 * builder pattern.
 */
class ReporterConfigTest {

    @TempDir
    Path tempDir;

    private File configFile;

    @BeforeEach
    void setUp() throws IOException {
        configFile = tempDir.resolve("test-reporter.properties").toFile();
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html,json");
        props.setProperty("reporter.output.directory", tempDir.toString());
        props.setProperty("reporter.s3.enabled", "true");
        props.setProperty("reporter.s3.bucket", "test-bucket");
        props.setProperty("reporter.s3.region", "us-west-2");
        props.setProperty("reporter.http.enabled", "false");
        props.setProperty("reporter.slack.enabled", "false");

        try (FileWriter writer = new FileWriter(configFile)) {
            props.store(writer, "Test configuration");
        }
    }

    @Test
    void testBuilderPattern() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("html", "json"))
                .outputDirectory(new File("target/reports"))
                .build();

        assertNotNull(config);
        assertEquals(2, config.getOutputFormats().size());
        assertTrue(config.getOutputFormats().contains("html"));
        assertTrue(config.getOutputFormats().contains("json"));
        assertEquals(new File("target/reports"), config.getOutputDirectory());
    }

    @Test
    void testLoadFromFile() throws Exception {
        ReporterConfig config = ReporterConfig.loadFromFile(configFile);

        assertNotNull(config);
        assertEquals(2, config.getOutputFormats().size());
        assertTrue(config.getOutputFormats().contains("html"));
        assertTrue(config.getOutputFormats().contains("json"));
        assertNotNull(config.getS3Config());
        assertTrue(config.getS3Config().isEnabled());
        assertEquals("test-bucket", config.getS3Config().getBucket());
        assertEquals("us-west-2", config.getS3Config().getRegion());
    }

    @Test
    void testLoadFromProperties() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "junit");
        props.setProperty("reporter.output.directory", "target/test-reports");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertNotNull(config);
        assertEquals(1, config.getOutputFormats().size());
        assertEquals("junit", config.getOutputFormats().get(0));
    }

    @Test
    void testValidation_ValidConfig() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("html"))
                .outputDirectory(tempDir.toFile())
                .build();

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidation_MissingOutputFormats() {
        ReporterConfig config = ReporterConfig.builder()
                .outputDirectory(tempDir.toFile())
                .build();

        ConfigException exception = assertThrows(ConfigException.class, () -> config.validate());
        assertTrue(exception.getMessage().contains("output formats"));
    }

    @Test
    void testValidation_NullOutputDirectory() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("html"))
                .build();

        ConfigException exception = assertThrows(ConfigException.class, () -> config.validate());
        assertTrue(exception.getMessage().contains("output directory"));
    }

    @Test
    void testValidation_InvalidOutputFormat() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("invalid-format"))
                .outputDirectory(tempDir.toFile())
                .build();

        ConfigException exception = assertThrows(ConfigException.class, () -> config.validate());
        assertTrue(exception.getMessage().contains("Invalid output format"));
    }

    @Test
    void testS3ConfigValidation() throws Exception {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());
        props.setProperty("reporter.s3.enabled", "true");
        props.setProperty("reporter.s3.bucket", ""); // Invalid: empty bucket

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        ConfigException exception = assertThrows(ConfigException.class, () -> config.validate());
        assertTrue(exception.getMessage().contains("S3 bucket"));
    }

    @Test
    void testHttpConfigValidation() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());
        props.setProperty("reporter.http.enabled", "true");
        props.setProperty("reporter.http.url", ""); // Invalid: empty URL

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        ConfigException exception = assertThrows(ConfigException.class, () -> config.validate());
        assertTrue(exception.getMessage().contains("HTTP URL"));
    }

    @Test
    void testSlackConfigValidation() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());
        props.setProperty("reporter.slack.enabled", "true");
        props.setProperty("reporter.slack.webhookUrl", ""); // Invalid: empty webhook

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        ConfigException exception = assertThrows(ConfigException.class, () -> config.validate());
        assertTrue(exception.getMessage().contains("Slack webhook"));
    }

    @Test
    void testEnvironmentVariableInterpolation() {
        System.setProperty("TEST_BUCKET", "my-test-bucket");
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());
        props.setProperty("reporter.s3.enabled", "true");
        props.setProperty("reporter.s3.bucket", "${TEST_BUCKET}");
        props.setProperty("reporter.s3.region", "us-east-1");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertEquals("my-test-bucket", config.getS3Config().getBucket());

        System.clearProperty("TEST_BUCKET");
    }

    @Test
    void testDefaultValues() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("html"))
                .outputDirectory(tempDir.toFile())
                .build();

        assertNotNull(config.getS3Config());
        assertFalse(config.getS3Config().isEnabled());
        assertNotNull(config.getHttpConfig());
        assertFalse(config.getHttpConfig().isEnabled());
        assertNotNull(config.getSlackConfig());
        assertFalse(config.getSlackConfig().isEnabled());
    }

    @Test
    void testToString() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("html", "json"))
                .outputDirectory(new File("target/reports"))
                .build();

        String result = config.toString();

        assertNotNull(result);
        assertTrue(result.contains("html"));
        assertTrue(result.contains("json"));
        assertTrue(result.contains("target/reports"));
    }

    @Test
    void testConfigLoadsContentSizeLimit() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());
        props.setProperty("reporter.maxArtifactContentSize", "102400");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertEquals(102400, config.getMaxArtifactContentSize());
    }

    @Test
    void testConfigLoadsDefaultContentSizeLimit() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertEquals(51200, config.getMaxArtifactContentSize(), "Should default to 51200 (50KB)");
    }

    @Test
    void testConfigLoadsSensitiveDataSettings() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());
        props.setProperty("reporter.maskSensitiveData", "true");
        props.setProperty("reporter.sensitiveHeaders", "Authorization,X-API-Key,Custom-Token");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertTrue(config.isMaskSensitiveData());
        assertEquals("Authorization,X-API-Key,Custom-Token", config.getSensitiveHeaders());
    }

    @Test
    void testConfigLoadsDefaultSensitiveDataSettings() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertTrue(config.isMaskSensitiveData(), "Should default to true");
        assertEquals("Authorization,X-API-Key,Cookie,Set-Cookie", config.getSensitiveHeaders(),
                "Should use default sensitive headers");
    }

    @Test
    void testBuilderWithContentSizeSettings() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("html"))
                .outputDirectory(tempDir.toFile())
                .maxArtifactContentSize(204800)
                .maskSensitiveData(true)
                .sensitiveHeaders("Token,Secret")
                .build();

        assertEquals(204800, config.getMaxArtifactContentSize());
        assertTrue(config.isMaskSensitiveData());
        assertEquals("Token,Secret", config.getSensitiveHeaders());
    }
}
