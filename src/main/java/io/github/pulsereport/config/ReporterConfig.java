package io.github.pulsereport.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration POJO for the custom reporter. Supports loading from properties
 * files and builder pattern.
 */
public class ReporterConfig {

    private static final List<String> VALID_FORMATS = Arrays.asList("html", "json", "junit");
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private List<String> outputFormats;
    private File outputDirectory;
    private S3Config s3Config;
    private HttpConfig httpConfig;
    private SlackConfig slackConfig;
    private int maxArtifactContentSize;
    private boolean maskSensitiveData;
    private String maskHeaderFields;
    private boolean maskHeadersEnabled;
    private boolean maskBodyEnabled;
    private String sensitiveBodyFields;
    private boolean maskXmlEnabled;
    private String xmlFields;
    private boolean maskTokens;
    private String videoStorage;

    /**
     * Private constructor - use builder or load methods.
     */
    private ReporterConfig() {
        this.s3Config = new S3Config();
        this.httpConfig = new HttpConfig();
        this.slackConfig = new SlackConfig();
        this.maxArtifactContentSize = 51200; // 50KB default
        this.maskSensitiveData = true;
        this.maskHeaderFields = "Authorization,X-API-Key,Cookie,Set-Cookie";
        this.maskHeadersEnabled = true;
        this.maskBodyEnabled = true;
        this.sensitiveBodyFields = "password,secret,token,access_token,refresh_token,"
                + "id_token,client_secret,api_key,apiKey,authorization";
        this.maskXmlEnabled = true;
        this.xmlFields = "password,secret,token,access_token,refresh_token,"
                + "id_token,client_secret,api_key,apiKey,authorization";
        this.maskTokens = true;
        this.videoStorage = "path"; // path | embed | url
    }

    /**
     * Creates a new builder for ReporterConfig.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Loads configuration from a properties file.
     *
     * @param configFile the configuration file
     * @return loaded configuration
     * @throws IOException if file cannot be read
     */
    public static ReporterConfig loadFromFile(File configFile) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        }
        return loadFromProperties(props);
    }

    private static final String[] AUTO_DETECT_PATHS = {
        "reporter.properties",
        "src/main/resources/reporter.properties"
    };

    /**
     * Attempts to find and load a configuration file from well-known locations.
     *
     * @return loaded configuration, or null if no config file found
     */
    public static ReporterConfig autoDetect() {
        for (String path : AUTO_DETECT_PATHS) {
            File file = new File(path);
            if (file.isFile()) {
                try {
                    return loadFromFile(file);
                } catch (IOException e) {
                    // skip unreadable files
                }
            }
        }
        try (InputStream is = ReporterConfig.class.getResourceAsStream("/reporter.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return loadFromProperties(props);
            }
        } catch (IOException e) {
            // skip
        }
        return null;
    }

    /**
     * Resolves the report output directory using the following precedence
     * (highest first):
     * <ol>
     * <li>The {@code System.getProperty(systemPropertyName)} value, if non-null
     * and non-blank</li>
     * <li>The {@code reporter.output.directory} value from an auto-detected
     * {@code reporter.properties} file (see {@link #autoDetect()}), if
     * present</li>
     * <li>The supplied {@code defaultDirectory}</li>
     * </ol>
     * This method never throws; any failure during auto-detection falls through
     * to the default.
     *
     * @param systemPropertyName name of the system property that overrides the
     * output directory (e.g. {@code "reporter.output.directory"})
     * @param defaultDirectory fallback directory used when neither the system
     * property nor an auto-detected properties file provides a value
     * @return the resolved output directory path
     */
    public static String resolveOutputDirectory(String systemPropertyName, String defaultDirectory) {
        String sysProp = System.getProperty(systemPropertyName);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp;
        }
        try {
            ReporterConfig config = autoDetect();
            if (config != null && config.getOutputDirectory() != null) {
                return config.getOutputDirectory().getPath();
            }
        } catch (RuntimeException e) {
            // fall through to default
        }
        return defaultDirectory;
    }

    /**
     * Loads configuration from Properties object.
     *
     * @param props the properties object
     * @return loaded configuration
     */
    public static ReporterConfig loadFromProperties(Properties props) {
        ReporterConfig config = new ReporterConfig();

        String formats = interpolate(props.getProperty("reporter.output.formats", ""));
        if (!formats.isEmpty()) {
            config.outputFormats = Arrays.asList(formats.split(","));
            config.outputFormats = config.outputFormats.stream()
                    .map(String::trim)
                    .collect(java.util.stream.Collectors.toList());
        }

        String outputDir = interpolate(props.getProperty("reporter.output.directory", ""));
        if (!outputDir.isEmpty()) {
            config.outputDirectory = new File(outputDir);
        }

        config.s3Config.enabled = Boolean.parseBoolean(props.getProperty("reporter.s3.enabled", "false"));
        config.s3Config.bucket = interpolate(props.getProperty("reporter.s3.bucket", ""));
        config.s3Config.region = interpolate(props.getProperty("reporter.s3.region", ""));
        config.s3Config.keyPrefix = interpolate(props.getProperty("reporter.s3.keyPrefix", ""));

        config.httpConfig.enabled = Boolean.parseBoolean(props.getProperty("reporter.http.enabled", "false"));
        config.httpConfig.url = interpolate(props.getProperty("reporter.http.url", ""));
        config.httpConfig.method = interpolate(props.getProperty("reporter.http.method", "POST"));
        config.httpConfig.authType = interpolate(props.getProperty("reporter.http.auth.type", ""));
        config.httpConfig.authToken = interpolate(props.getProperty("reporter.http.auth.token", ""));

        config.slackConfig.enabled = Boolean.parseBoolean(props.getProperty("reporter.slack.enabled", "false"));
        config.slackConfig.webhookUrl = interpolate(props.getProperty("reporter.slack.webhookUrl", ""));
        config.slackConfig.channel = interpolate(props.getProperty("reporter.slack.channel", ""));
        config.slackConfig.mentionOnFailure = interpolate(props.getProperty("reporter.slack.mentionOnFailure", ""));
        config.slackConfig.reportUrl = interpolate(props.getProperty("reporter.slack.reportUrl", ""));

        config.maxArtifactContentSize = Integer.parseInt(props.getProperty("reporter.maxArtifactContentSize", "51200"));
        config.maskSensitiveData = Boolean.parseBoolean(props.getProperty("reporter.maskSensitiveData", "true"));
        config.maskHeadersEnabled = Boolean.parseBoolean(props.getProperty("reporter.maskHeaders.enabled", "true"));
        config.maskHeaderFields = interpolate(props.getProperty("reporter.maskHeaders.fields", "Authorization,X-API-Key,Cookie,Set-Cookie"));
        config.maskBodyEnabled = Boolean.parseBoolean(props.getProperty("reporter.maskBody.enabled", "true"));
        config.sensitiveBodyFields = interpolate(props.getProperty("reporter.maskBody.fields",
                "password,secret,token,access_token,refresh_token,id_token,client_secret,api_key,apiKey,authorization"));
        config.maskXmlEnabled = Boolean.parseBoolean(props.getProperty("reporter.maskXml.enabled", "true"));
        String xmlFieldsValue = interpolate(props.getProperty("reporter.maskXml.fields"));
        config.xmlFields = (xmlFieldsValue == null || xmlFieldsValue.isEmpty())
                ? config.sensitiveBodyFields
                : xmlFieldsValue;
        config.maskTokens = Boolean.parseBoolean(props.getProperty("reporter.maskBody.maskTokens", "true"));
        String videoStorageValue = interpolate(props.getProperty("reporter.video.storage", "path"));
        config.videoStorage = videoStorageValue == null ? "path" : videoStorageValue.trim().toLowerCase();

        return config;
    }

    /**
     * Interpolates environment variables in the format ${VAR_NAME}.
     *
     * @param value the value to interpolate
     * @return interpolated value
     */
    private static String interpolate(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        Matcher matcher = ENV_VAR_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = System.getProperty(varName, System.getenv(varName));
            if (varValue == null) {
                varValue = matcher.group(0); // Keep original if not found
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(varValue));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Validates the configuration.
     *
     * @throws ConfigException if validation fails
     */
    public void validate() throws ConfigException {
        if (outputFormats == null || outputFormats.isEmpty()) {
            throw new ConfigException("output formats must be specified");
        }

        for (String format : outputFormats) {
            if (!VALID_FORMATS.contains(format)) {
                throw new ConfigException("Invalid output format: " + format
                        + ". Valid formats are: " + String.join(", ", VALID_FORMATS));
            }
        }

        if (outputDirectory == null) {
            throw new ConfigException("output directory must be specified");
        }

        if (s3Config.enabled) {
            if (s3Config.bucket == null || s3Config.bucket.isEmpty()) {
                throw new ConfigException("S3 bucket must be specified when S3 is enabled");
            }
        }

        if (httpConfig.enabled) {
            if (httpConfig.url == null || httpConfig.url.isEmpty()) {
                throw new ConfigException("HTTP URL must be specified when HTTP publishing is enabled");
            }
        }

        if (slackConfig.enabled) {
            if (slackConfig.webhookUrl == null || slackConfig.webhookUrl.isEmpty()) {
                throw new ConfigException("Slack webhook URL must be specified when Slack is enabled");
            }
        }

        if (videoStorage != null
                && !videoStorage.equals("path")
                && !videoStorage.equals("embed")
                && !videoStorage.equals("url")) {
            throw new ConfigException("Invalid video storage: " + videoStorage
                    + ". Valid values are: path, embed, url");
        }
    }

    // Getters
    public List<String> getOutputFormats() {
        return outputFormats;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public S3Config getS3Config() {
        return s3Config;
    }

    public HttpConfig getHttpConfig() {
        return httpConfig;
    }

    public SlackConfig getSlackConfig() {
        return slackConfig;
    }

    public int getMaxArtifactContentSize() {
        return maxArtifactContentSize;
    }

    public boolean isMaskSensitiveData() {
        return maskSensitiveData;
    }

    /**
     * Gets the comma-separated list of header names to mask.
     *
     * @return the mask header fields
     */
    public String getMaskHeaderFields() {
        return maskHeaderFields;
    }

    /**
     * Whether request/response header masking is enabled. Defaults to true.
     *
     * @return true if header masking is enabled
     */
    public boolean isMaskHeadersEnabled() {
        return maskHeadersEnabled;
    }

    /**
     * Whether request/response body masking is enabled. Defaults to true.
     *
     * @return true if body masking is enabled
     */
    public boolean isMaskBodyEnabled() {
        return maskBodyEnabled;
    }

    /**
     * Gets the comma-separated list of body field names (JSON keys or
     * form-urlencoded keys) to mask. The list is authoritative: it fully
     * replaces the built-in defaults when set.
     *
     * @return the sensitive body fields
     */
    public String getSensitiveBodyFields() {
        return sensitiveBodyFields;
    }

    /**
     * Whether XML body element masking is enabled. Defaults to true.
     *
     * @return true if XML masking is enabled
     */
    public boolean isMaskXmlEnabled() {
        return maskXmlEnabled;
    }

    /**
     * Gets the comma-separated list of XML element names to mask. When not
     * explicitly configured, this inherits the sensitive body fields list.
     *
     * @return the XML fields to mask
     */
    public String getXmlFields() {
        return xmlFields;
    }

    /**
     * Whether Bearer/JWT token regex masking passes are applied to bodies.
     * Defaults to true.
     *
     * @return true if token masking is enabled
     */
    public boolean isMaskTokens() {
        return maskTokens;
    }

    /**
     * Gets the video storage mode: "path" (reference local/hosted file),
     * "embed" (inline base64), or "url" (external URL). Defaults to "path".
     *
     * @return the video storage mode
     */
    public String getVideoStorage() {
        return videoStorage;
    }

    @Override
    public String toString() {
        return "ReporterConfig{"
                + "outputFormats=" + outputFormats
                + ", outputDirectory=" + outputDirectory
                + ", s3Config=" + s3Config
                + ", httpConfig=" + httpConfig
                + ", slackConfig=" + slackConfig
                + ", maxArtifactContentSize=" + maxArtifactContentSize
                + ", maskSensitiveData=" + maskSensitiveData
                + ", maskHeadersEnabled=" + maskHeadersEnabled
                + ", maskHeaderFields='" + maskHeaderFields + '\''
                + ", maskBodyEnabled=" + maskBodyEnabled
                + ", sensitiveBodyFields='" + sensitiveBodyFields + '\''
                + ", maskXmlEnabled=" + maskXmlEnabled
                + ", xmlFields='" + xmlFields + '\''
                + ", maskTokens=" + maskTokens
                + ", videoStorage='" + videoStorage + '\''
                + '}';
    }

    /**
     * Builder for ReporterConfig.
     */
    public static class Builder {

        private final ReporterConfig config;

        private Builder() {
            this.config = new ReporterConfig();
        }

        public Builder outputFormats(List<String> formats) {
            config.outputFormats = formats;
            return this;
        }

        public Builder outputDirectory(File directory) {
            config.outputDirectory = directory;
            return this;
        }

        public Builder s3Config(S3Config s3Config) {
            config.s3Config = s3Config;
            return this;
        }

        public Builder httpConfig(HttpConfig httpConfig) {
            config.httpConfig = httpConfig;
            return this;
        }

        public Builder slackConfig(SlackConfig slackConfig) {
            config.slackConfig = slackConfig;
            return this;
        }

        public Builder maxArtifactContentSize(int maxArtifactContentSize) {
            config.maxArtifactContentSize = maxArtifactContentSize;
            return this;
        }

        public Builder maskSensitiveData(boolean maskSensitiveData) {
            config.maskSensitiveData = maskSensitiveData;
            return this;
        }

        public Builder maskHeaderFields(String maskHeaderFields) {
            config.maskHeaderFields = maskHeaderFields;
            return this;
        }

        public Builder maskHeadersEnabled(boolean maskHeadersEnabled) {
            config.maskHeadersEnabled = maskHeadersEnabled;
            return this;
        }

        public Builder maskBodyEnabled(boolean maskBodyEnabled) {
            config.maskBodyEnabled = maskBodyEnabled;
            return this;
        }

        public Builder sensitiveBodyFields(String sensitiveBodyFields) {
            config.sensitiveBodyFields = sensitiveBodyFields;
            return this;
        }

        public Builder maskXmlEnabled(boolean maskXmlEnabled) {
            config.maskXmlEnabled = maskXmlEnabled;
            return this;
        }

        public Builder maskXmlFields(String xmlFields) {
            config.xmlFields = xmlFields;
            return this;
        }

        public Builder maskTokens(boolean maskTokens) {
            config.maskTokens = maskTokens;
            return this;
        }

        public Builder videoStorage(String videoStorage) {
            config.videoStorage = videoStorage;
            return this;
        }

        public ReporterConfig build() {
            return config;
        }
    }

    /**
     * S3 configuration.
     */
    public static class S3Config {

        private boolean enabled;
        private String bucket;
        private String region;
        private String keyPrefix;

        public S3Config() {
            this.enabled = false;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        @Override
        public String toString() {
            return "S3Config{enabled=" + enabled + ", bucket='" + bucket + "', region='" + region + "'}";
        }
    }

    /**
     * HTTP configuration.
     */
    public static class HttpConfig {

        private boolean enabled;
        private String url;
        private String method;
        private String authType;
        private String authToken;

        public HttpConfig() {
            this.enabled = false;
            this.method = "POST";
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        @Override
        public String toString() {
            return "HttpConfig{enabled=" + enabled + ", url='" + url + "', method='" + method + "'}";
        }
    }

    /**
     * Slack configuration.
     */
    public static class SlackConfig {

        private boolean enabled;
        private String webhookUrl;
        private String channel;
        private String mentionOnFailure;
        private String reportUrl;

        public SlackConfig() {
            this.enabled = false;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getMentionOnFailure() {
            return mentionOnFailure;
        }

        public void setMentionOnFailure(String mentionOnFailure) {
            this.mentionOnFailure = mentionOnFailure;
        }

        public String getReportUrl() {
            return reportUrl;
        }

        public void setReportUrl(String reportUrl) {
            this.reportUrl = reportUrl;
        }

        @Override
        public String toString() {
            return "SlackConfig{enabled=" + enabled + ", channel='" + channel + "'}";
        }
    }
}
