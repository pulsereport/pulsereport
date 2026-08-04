package io.github.pulsereport.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.pulsereport.config.ConfigException;
import io.github.pulsereport.config.ReporterConfig;
import io.github.pulsereport.core.model.TestRun;
import io.github.pulsereport.integrations.http.HttpPublishConfig;
import io.github.pulsereport.integrations.http.HttpPublisher;
import io.github.pulsereport.integrations.s3.S3PublishConfig;
import io.github.pulsereport.integrations.s3.S3Publisher;
import io.github.pulsereport.integrations.slack.SlackConfig;
import io.github.pulsereport.integrations.slack.SlackNotifier;
import io.github.pulsereport.outputs.OutputGenerator;
import io.github.pulsereport.outputs.html.HtmlReportGenerator;
import io.github.pulsereport.outputs.json.JsonReportGenerator;
import io.github.pulsereport.outputs.junit.JUnitXmlReportGenerator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main CLI entry point for PulseReport. Provides commands for generating,
 * publishing, and validating reports.
 */
@Command(
        name = "pulsereport",
        mixinStandardHelpOptions = true,
        versionProvider = ReporterCLI.VersionProvider.class,
        description = "PulseReport is the signal-first reporting layer for web, mobile, and API automation",
        subcommands = {
            ReporterCLI.GenerateCommand.class,
            ReporterCLI.PublishCommand.class,
            ReporterCLI.ValidateCommand.class
        }
)
public class ReporterCLI implements Callable<Integer> {

    private static final String VERSION_PROPERTY = "pulsereport.version";
    private static final String VERSION_RESOURCE = "/META-INF/maven/io.github.pulsereport/pulsereport/pom.properties";

    /**
     * Main entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        int exitCode = execute(args);
        System.exit(exitCode);
    }

    /**
     * Execute the CLI with the given arguments. This method is primarily for
     * testing.
     *
     * @param args command line arguments
     * @return exit code (0 = success, 1 = error, 2 = validation error)
     */
    public static int execute(String[] args) {
        CommandLine cmd = new CommandLine(new ReporterCLI());
        return cmd.execute(args);
    }

    static String resolveVersion() {
        String configuredVersion = System.getProperty(VERSION_PROPERTY);
        if (hasText(configuredVersion)) {
            return configuredVersion;
        }

        Package reporterPackage = ReporterCLI.class.getPackage();
        if (reporterPackage != null && hasText(reporterPackage.getImplementationVersion())) {
            return reporterPackage.getImplementationVersion();
        }

        try (InputStream inputStream = ReporterCLI.class.getResourceAsStream(VERSION_RESOURCE)) {
            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);
                String pomVersion = properties.getProperty("version");
                if (hasText(pomVersion)) {
                    return pomVersion;
                }
            }
        } catch (IOException ignored) {
            // Fall through to the final fallback when Maven metadata is unavailable.
        }

        return "unknown";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[]{"PulseReport " + resolveVersion()};
        }
    }

    /**
     * Generate command - generates reports from test results.
     */
    @Command(
            name = "generate",
            description = "Generate reports from test results"
    )
    static class GenerateCommand implements Callable<Integer> {

        @Option(names = {"-i", "--input"}, description = "Input test results file", required = true)
        private File inputFile;

        @Option(names = {"-o", "--output"}, description = "Output directory for reports")
        private File outputDirectory;

        @Option(names = {"-f", "--format"}, description = "Output formats (comma-separated: html,json,junit)", split = ",")
        private List<String> formats;

        @Option(names = {"-c", "--config"}, description = "Configuration file")
        private File configFile;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        private boolean verbose;

        @Option(names = {"--dry-run"}, description = "Dry run - show what would be done without doing it")
        private boolean dryRun;

        @Override
        public Integer call() {
            try {
                // Validate input file exists
                if (!inputFile.exists()) {
                    System.err.println("Error: Input file does not exist: " + inputFile);
                    return 2;
                }

                ReporterConfig config;
                if (configFile != null) {
                    if (verbose) {
                        System.out.println("Loading configuration from: " + configFile);
                    }
                    config = ReporterConfig.loadFromFile(configFile);
                } else {
                    config = ReporterConfig.autoDetect();
                    if (config != null) {
                        if (verbose) {
                            System.out.println("Auto-detected configuration");
                        }
                    } else {
                        ReporterConfig.Builder builder = ReporterConfig.builder();

                        if (formats != null) {
                            builder.outputFormats(formats);
                        }

                        if (outputDirectory != null) {
                            builder.outputDirectory(outputDirectory);
                        } else {
                            builder.outputDirectory(new File("target/pulsereport"));
                        }

                        config = builder.build();
                    }
                }

                if (formats != null) {
                    config = ReporterConfig.builder()
                            .outputFormats(formats)
                            .outputDirectory(outputDirectory != null ? outputDirectory : config.getOutputDirectory())
                            .build();
                }

                try {
                    config.validate();
                } catch (ConfigException e) {
                    System.err.println("Configuration validation failed: " + e.getMessage());
                    return 2;
                }

                if (dryRun) {
                    System.out.println("DRY RUN - Would generate reports:");
                    System.out.println("  Input: " + inputFile);
                    System.out.println("  Output: " + config.getOutputDirectory());
                    System.out.println("  Formats: " + config.getOutputFormats());
                    return 0;
                }

                if (verbose) {
                    System.out.println("Generating reports...");
                    System.out.println("  Input: " + inputFile);
                    System.out.println("  Output: " + config.getOutputDirectory());
                    System.out.println("  Formats: " + config.getOutputFormats());
                }

                if (!config.getOutputDirectory().exists()) {
                    config.getOutputDirectory().mkdirs();
                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                TestRun testRun = mapper.readValue(inputFile, TestRun.class);

                for (String format : config.getOutputFormats()) {
                    OutputGenerator generator = createGenerator(format);
                    String extension = getFileExtension(format);
                    File outputFile = new File(config.getOutputDirectory(), "test-report." + extension);
                    generator.generate(testRun, outputFile);
                    if (verbose) {
                        System.out.println("  Generated: " + outputFile.getAbsolutePath());
                    }
                }

                System.out.println("Reports generated successfully to: " + config.getOutputDirectory());

                return 0;

            } catch (Exception e) {
                System.err.println("Error generating reports: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace(new PrintWriter(System.err, true));
                }
                return 1;
            }
        }

        private static OutputGenerator createGenerator(String format) {
            switch (format.toLowerCase()) {
                case "html":
                    return new HtmlReportGenerator();
                case "json":
                    return new JsonReportGenerator();
                case "junit":
                    return new JUnitXmlReportGenerator();
                default:
                    throw new IllegalArgumentException("Unsupported format: " + format);
            }
        }

        private static String getFileExtension(String format) {
            switch (format.toLowerCase()) {
                case "html":
                    return "html";
                case "json":
                    return "json";
                case "junit":
                    return "xml";
                default:
                    return format;
            }
        }
    }

    /**
     * Publish command - publishes reports to integrations.
     */
    @Command(
            name = "publish",
            description = "Publish reports to integrations (S3, HTTP, Slack)"
    )
    static class PublishCommand implements Callable<Integer> {

        @Option(names = {"-i", "--input"}, description = "Input report file", required = true)
        private File inputFile;

        @Option(names = {"-t", "--target"}, description = "Publishing targets (comma-separated: s3,http,slack)", split = ",", required = true)
        private List<String> targets;

        @Option(names = {"-c", "--config"}, description = "Configuration file (auto-detected if omitted)")
        private File configFile;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        private boolean verbose;

        @Option(names = {"--dry-run"}, description = "Dry run - show what would be done without doing it")
        private boolean dryRun;

        @Override
        public Integer call() {
            try {
                if (!inputFile.exists()) {
                    System.err.println("Error: Input file does not exist: " + inputFile);
                    return 2;
                }

                ReporterConfig config;
                if (configFile != null) {
                    if (verbose) {
                        System.out.println("Loading configuration from: " + configFile);
                    }
                    config = ReporterConfig.loadFromFile(configFile);
                } else {
                    config = ReporterConfig.autoDetect();
                    if (config == null) {
                        System.err.println("Error: No configuration file found. Use --config or place reporter.properties in the project root.");
                        return 2;
                    }
                    if (verbose) {
                        System.out.println("Auto-detected configuration");
                    }
                }

                try {
                    config.validate();
                } catch (ConfigException e) {
                    System.err.println("Configuration validation failed: " + e.getMessage());
                    return 2;
                }

                if (dryRun) {
                    System.out.println("DRY RUN - Would publish report:");
                    System.out.println("  Input: " + inputFile);
                    System.out.println("  Targets: " + targets);
                    return 0;
                }

                if (verbose) {
                    System.out.println("Publishing report...");
                    System.out.println("  Input: " + inputFile);
                    System.out.println("  Targets: " + targets);
                }

                for (String target : targets) {
                    switch (target.toLowerCase()) {
                        case "s3":
                            if (!config.getS3Config().isEnabled()) {
                                System.err.println("Warning: S3 publishing not enabled in configuration, skipping");
                                continue;
                            }
                            S3PublishConfig s3Config = S3PublishConfig.builder()
                                    .bucketName(config.getS3Config().getBucket())
                                    .region(config.getS3Config().getRegion())
                                    .keyPrefix(config.getS3Config().getKeyPrefix())
                                    .build();
                            new S3Publisher(software.amazon.awssdk.services.s3.S3Client.create())
                                    .publish(inputFile, s3Config);
                            break;
                        case "http":
                            if (!config.getHttpConfig().isEnabled()) {
                                System.err.println("Warning: HTTP publishing not enabled in configuration, skipping");
                                continue;
                            }
                            HttpPublishConfig.Builder httpBuilder = HttpPublishConfig.builder()
                                    .endpoint(config.getHttpConfig().getUrl())
                                    .method(config.getHttpConfig().getMethod());
                            if ("bearer".equalsIgnoreCase(config.getHttpConfig().getAuthType())) {
                                httpBuilder.bearerToken(config.getHttpConfig().getAuthToken());
                            }
                            HttpPublishConfig httpConfig = httpBuilder.build();
                            new HttpPublisher().publish(inputFile, httpConfig);
                            break;
                        case "slack":
                            if (!config.getSlackConfig().isEnabled()) {
                                System.err.println("Warning: Slack publishing not enabled in configuration, skipping");
                                continue;
                            }
                            ObjectMapper slackMapper = new ObjectMapper();
                            slackMapper.registerModule(new JavaTimeModule());
                            TestRun run = slackMapper.readValue(inputFile, TestRun.class);
                            SlackConfig slackCfg = SlackConfig.builder()
                                    .webhookUrl(config.getSlackConfig().getWebhookUrl())
                                    .channel(config.getSlackConfig().getChannel())
                                    .mentionOnFailure(config.getSlackConfig().getMentionOnFailure())
                                    .reportUrl(config.getSlackConfig().getReportUrl())
                                    .build();
                            new SlackNotifier().notify(run, slackCfg);
                            break;
                        default:
                            System.err.println("Unknown target: " + target);
                            return 2;
                    }
                    if (verbose) {
                        System.out.println("  Published to: " + target);
                    }
                }

                System.out.println("Report published successfully to: " + targets);

                return 0;

            } catch (Exception e) {
                System.err.println("Error publishing report: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace(new PrintWriter(System.err, true));
                }
                return 1;
            }
        }
    }

    /**
     * Validate command - validates configuration file.
     */
    @Command(
            name = "validate",
            description = "Validate configuration file"
    )
    static class ValidateCommand implements Callable<Integer> {

        @Option(names = {"-c", "--config"}, description = "Configuration file to validate", required = true)
        private File configFile;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        private boolean verbose;

        @Override
        public Integer call() {
            try {
                if (verbose) {
                    System.out.println("Validating configuration: " + configFile);
                }

                ReporterConfig config = ReporterConfig.loadFromFile(configFile);

                if (verbose) {
                    System.out.println("Loaded configuration:");
                    System.out.println(config);
                }

                try {
                    config.validate();
                    System.out.println("✓ Configuration is valid");
                    return 0;
                } catch (ConfigException e) {
                    System.err.println("✗ Configuration is invalid:");
                    System.err.println("  " + e.getMessage());
                    return 2;
                }

            } catch (Exception e) {
                System.err.println("Error reading configuration file: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace(new PrintWriter(System.err, true));
                }
                return 1;
            }
        }
    }
}
