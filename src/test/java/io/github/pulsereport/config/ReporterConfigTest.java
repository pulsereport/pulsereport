package io.github.pulsereport.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
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
        props.setProperty("reporter.maskHeaders.fields", "Authorization,X-API-Key,Custom-Token");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertTrue(config.isMaskSensitiveData());
        assertEquals("Authorization,X-API-Key,Custom-Token", config.getMaskHeaderFields());
    }

    @Test
    void testConfigLoadsDefaultSensitiveDataSettings() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertTrue(config.isMaskSensitiveData(), "Should default to true");
        assertEquals("Authorization,X-API-Key,Cookie,Set-Cookie", config.getMaskHeaderFields(),
                "Should use default mask header fields");
    }

    @Test
    void testBuilderWithContentSizeSettings() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("html"))
                .outputDirectory(tempDir.toFile())
                .maxArtifactContentSize(204800)
                .maskSensitiveData(true)
                .maskHeaderFields("Token,Secret")
                .build();

        assertEquals(204800, config.getMaxArtifactContentSize());
        assertTrue(config.isMaskSensitiveData());
        assertEquals("Token,Secret", config.getMaskHeaderFields());
    }

    @Test
    void testMaskBodyDefaults() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("html"))
                .outputDirectory(tempDir.toFile())
                .build();

        assertTrue(config.isMaskBodyEnabled(), "maskBodyEnabled should default to true");
        assertEquals(
                "password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization",
                config.getSensitiveBodyFields(),
                "Should use default sensitive body fields");
        assertTrue(config.isMaskTokens(), "maskTokens should default to true");
    }

    @Test
    void testConfigLoadsMaskBodySettings() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());
        props.setProperty("reporter.maskBody.enabled", "false");
        props.setProperty("reporter.maskBody.fields", "ssn,pin");
        props.setProperty("reporter.maskBody.maskTokens", "false");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertFalse(config.isMaskBodyEnabled());
        assertEquals("ssn,pin", config.getSensitiveBodyFields());
        assertFalse(config.isMaskTokens());
    }

    @Test
    void testConfigLoadsDefaultMaskBodySettings() {
        Properties props = new Properties();
        props.setProperty("reporter.output.formats", "html");
        props.setProperty("reporter.output.directory", tempDir.toString());

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertTrue(config.isMaskBodyEnabled(), "Should default to true");
        assertEquals(
                "password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization",
                config.getSensitiveBodyFields(),
                "Should use default sensitive body fields");
        assertTrue(config.isMaskTokens(), "Should default to true");
    }

    @Test
    void testBuilderWithMaskBodySettings() {
        ReporterConfig config = ReporterConfig.builder()
                .outputFormats(Arrays.asList("html"))
                .outputDirectory(tempDir.toFile())
                .maskBodyEnabled(false)
                .sensitiveBodyFields("ssn,pin")
                .maskTokens(false)
                .build();

        assertFalse(config.isMaskBodyEnabled());
        assertEquals("ssn,pin", config.getSensitiveBodyFields());
        assertFalse(config.isMaskTokens());
    }

    @Test
    void loadFromProperties_parsesNewGranularKeys() {
        Properties props = new Properties();
        props.setProperty("reporter.maskHeaders.enabled", "false");
        props.setProperty("reporter.maskHeaders.fields", "X-Custom");
        props.setProperty("reporter.maskXml.enabled", "false");
        props.setProperty("reporter.maskXml.fields", "a,b");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertFalse(config.isMaskHeadersEnabled());
        assertEquals("X-Custom", config.getMaskHeaderFields());
        assertFalse(config.isMaskXmlEnabled());
        assertEquals("a,b", config.getXmlFields());
    }

    @Test
    void loadFromProperties_newKeysDefaultToTrueAndXmlFieldsInheritsBodyFields() {
        Properties props = new Properties();
        props.setProperty("reporter.maskBody.fields", "foo,bar");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertTrue(config.isMaskHeadersEnabled(), "maskHeadersEnabled should default to true");
        assertTrue(config.isMaskXmlEnabled(), "maskXmlEnabled should default to true");
        assertEquals("Authorization,X-API-Key,Cookie,Set-Cookie", config.getMaskHeaderFields(),
                "Should use default mask header fields");
        assertEquals("foo,bar", config.getXmlFields(),
                "xmlFields should inherit the resolved body fields when not explicitly set");
    }

    @Test
    void loadFromProperties_xmlFieldsExplicitOverridesInheritance() {
        Properties props = new Properties();
        props.setProperty("reporter.maskBody.fields", "foo");
        props.setProperty("reporter.maskXml.fields", "baz");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertEquals("baz", config.getXmlFields(),
                "Explicit reporter.maskXml.fields should override inheritance from body fields");
    }

    @Test
    void loadFromProperties_legacySensitiveHeadersKeyIgnored() {
        Properties props = new Properties();
        props.setProperty("reporter.sensitiveHeaders", "X-Legacy");

        ReporterConfig config = ReporterConfig.loadFromProperties(props);

        assertEquals("Authorization,X-API-Key,Cookie,Set-Cookie", config.getMaskHeaderFields(),
                "Legacy reporter.sensitiveHeaders key must have no effect");
    }

    @Test
    void builder_supportsNewGranularSetters() {
        ReporterConfig config = ReporterConfig.builder()
                .maskHeadersEnabled(false)
                .maskHeaderFields("X-Custom")
                .maskXmlEnabled(false)
                .maskXmlFields("a,b")
                .build();

        assertFalse(config.isMaskHeadersEnabled());
        assertEquals("X-Custom", config.getMaskHeaderFields());
        assertFalse(config.isMaskXmlEnabled());
        assertEquals("a,b", config.getXmlFields());
    }

    // --- resolveOutputDirectory tests (adapter auto-detect, Phase 1) ---
    //
    // DEVIATION NOTE: src/main/resources/reporter.properties (the library's own
    // bundled resource) IS on the test classpath AND is discoverable via the
    // relative path "src/main/resources/reporter.properties" because Surefire
    // runs with the repo root as the working directory. That file sets
    // reporter.output.directory=target/pulsereport, which happens to equal the
    // built-in default. Tests below document where this affects assertions.

    private static final String OUTPUT_DIR_PROPERTY = "reporter.output.directory";
    private static final File WORKING_DIR_PROPS = new File("reporter.properties").getAbsoluteFile();

    /**
     * Writes a working-dir reporter.properties for auto-detection. Fails rather
     * than clobbering a pre-existing repo-root reporter.properties.
     *
     * @return true if this call created the file (caller must delete it)
     */
    private boolean writeWorkingDirProps(Properties props) throws IOException {
        if (WORKING_DIR_PROPS.exists()) {
            throw new IllegalStateException(
                    "Repo-root reporter.properties already exists; refusing to clobber: "
                            + WORKING_DIR_PROPS);
        }
        try (FileWriter writer = new FileWriter(WORKING_DIR_PROPS)) {
            props.store(writer, "temporary test config");
        }
        return true;
    }

    /**
     * Best-effort removal of the temporary working-dir reporter.properties.
     */
    private static void deleteWorkingDirProps() {
        try {
            boolean deleted = Files.deleteIfExists(WORKING_DIR_PROPS.toPath());
            if (!deleted && WORKING_DIR_PROPS.exists()) {
                throw new IllegalStateException("Failed to delete " + WORKING_DIR_PROPS);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete " + WORKING_DIR_PROPS, e);
        }
    }

    @Test
    void resolveOutputDirectory_systemPropertyWins() throws IOException {
        String saved = System.getProperty(OUTPUT_DIR_PROPERTY);
        boolean created = false;
        try {
            Properties props = new Properties();
            props.setProperty(OUTPUT_DIR_PROPERTY, "target/from-props");
            created = writeWorkingDirProps(props);
            System.setProperty(OUTPUT_DIR_PROPERTY, "custom/dir");

            assertEquals("custom/dir",
                    ReporterConfig.resolveOutputDirectory(OUTPUT_DIR_PROPERTY, "target/pulsereport"),
                    "System property must win over auto-detected reporter.properties");
        } finally {
            if (saved == null) {
                System.clearProperty(OUTPUT_DIR_PROPERTY);
            } else {
                System.setProperty(OUTPUT_DIR_PROPERTY, saved);
            }
            if (created) {
                deleteWorkingDirProps();
            }
        }
    }

    @Test
    void resolveOutputDirectory_fallsBackToPropertiesFile() throws IOException {
        String saved = System.getProperty(OUTPUT_DIR_PROPERTY);
        boolean created = false;
        try {
            System.clearProperty(OUTPUT_DIR_PROPERTY);
            Properties props = new Properties();
            props.setProperty(OUTPUT_DIR_PROPERTY, "target/from-props");
            created = writeWorkingDirProps(props);

            assertEquals("target/from-props",
                    ReporterConfig.resolveOutputDirectory(OUTPUT_DIR_PROPERTY, "target/pulsereport"),
                    "Auto-detected working-dir reporter.properties value must be used "
                            + "when the system property is absent");
        } finally {
            if (saved == null) {
                System.clearProperty(OUTPUT_DIR_PROPERTY);
            } else {
                System.setProperty(OUTPUT_DIR_PROPERTY, saved);
            }
            if (created) {
                deleteWorkingDirProps();
            }
        }
    }

    @Test
    void resolveOutputDirectory_defaultWhenNothingConfigured() {
        String saved = System.getProperty(OUTPUT_DIR_PROPERTY);
        try {
            System.clearProperty(OUTPUT_DIR_PROPERTY);
            assertFalse(WORKING_DIR_PROPS.exists(),
                    "Test requires no repo-root reporter.properties to be present");

            // DEVIATION: the library's bundled reporter.properties
            // (src/main/resources/reporter.properties) is auto-detected from the
            // test classpath and sets reporter.output.directory=target/pulsereport.
            // So the value asserted here comes from the classpath resource, not the
            // literal fallback default — the two coincidentally match.
            assertEquals("target/pulsereport",
                    ReporterConfig.resolveOutputDirectory(OUTPUT_DIR_PROPERTY, "target/pulsereport"),
                    "Bundled classpath reporter.properties sets reporter.output.directory"
                            + "=target/pulsereport, which equals the built-in default");
        } finally {
            if (saved == null) {
                System.clearProperty(OUTPUT_DIR_PROPERTY);
            } else {
                System.setProperty(OUTPUT_DIR_PROPERTY, saved);
            }
        }
    }

    @Test
    void resolveOutputDirectory_propertiesWithoutOutputDirFallsToDefault() throws IOException {
        String saved = System.getProperty(OUTPUT_DIR_PROPERTY);
        boolean created = false;
        try {
            System.clearProperty(OUTPUT_DIR_PROPERTY);
            // Working-dir properties file present but WITHOUT reporter.output.directory.
            // The working-dir file shadows the bundled classpath resource in
            // autoDetect()'s lookup order, so resolution must fall through to the
            // default. (The bundled resource's value equals the default anyway.)
            Properties props = new Properties();
            props.setProperty("reporter.output.formats", "html,json");
            created = writeWorkingDirProps(props);

            assertEquals("target/pulsereport",
                    ReporterConfig.resolveOutputDirectory(OUTPUT_DIR_PROPERTY, "target/pulsereport"),
                    "Properties file without reporter.output.directory must fall back "
                            + "to the default");
        } finally {
            if (saved == null) {
                System.clearProperty(OUTPUT_DIR_PROPERTY);
            } else {
                System.setProperty(OUTPUT_DIR_PROPERTY, saved);
            }
            if (created) {
                deleteWorkingDirProps();
            }
        }
    }
}
