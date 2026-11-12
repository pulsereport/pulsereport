package io.github.pulsereport.integrations.s3;

import io.github.pulsereport.integrations.PublishConfig;
import io.github.pulsereport.integrations.PublishException;
import io.github.pulsereport.integrations.Publisher;
import io.github.pulsereport.integrations.ReportFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Publisher implementation for AWS S3.
 * Uploads test reports to S3 buckets with retry logic and metadata support.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class S3Publisher implements Publisher {
    
    private static final Logger logger = LoggerFactory.getLogger(S3Publisher.class);
    private static final long MAX_DELAY_MS = 10000;
    
    private final S3Client s3Client;

    /**
     * Constructs an S3Publisher with the specified S3 client.
     * 
     * @param s3Client the AWS S3 client
     */
    public S3Publisher(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void publish(File reportFile, PublishConfig config) throws PublishException {
        if (!(config instanceof S3PublishConfig)) {
            throw new PublishException("Config must be S3PublishConfig");
        }
        
        if (!reportFile.exists()) {
            throw new PublishException("Report file does not exist: " + reportFile.getAbsolutePath());
        }

        try {
            byte[] reportData = Files.readAllBytes(reportFile.toPath());
            publish(reportData, reportFile.getName(), config);
        } catch (IOException e) {
            throw new PublishException("Failed to read report file: " + reportFile.getAbsolutePath(), e);
        }
        if (((S3PublishConfig) config).isUploadArtifacts()) {
            publishLinkedFiles(reportFile, (S3PublishConfig) config);
        }
    }

    /**
     * Uploads the screenshots, videos and logs the report links to by relative
     * path, under the same key prefix, so the published report's links work.
     */
    private void publishLinkedFiles(File reportFile, S3PublishConfig config) throws PublishException {
        List<String> files = ReportFiles.referencedFiles(reportFile);
        if (files.isEmpty()) {
            return;
        }
        File dir = reportFile.getAbsoluteFile().getParentFile();
        for (String relative : files) {
            try {
                byte[] data = Files.readAllBytes(dir.toPath().resolve(relative));
                uploadWithRetry(data, config.getKeyPrefix() + relative, config, ReportFiles.contentType(relative));
            } catch (IOException e) {
                throw new PublishException("Failed to read report file: " + relative, e);
            }
        }
        logger.info("Uploaded {} files linked from the report to s3://{}/{}", files.size(),
                config.getBucketName(), config.getKeyPrefix());
    }

    @Override
    public void publish(byte[] reportData, String fileName, PublishConfig config) throws PublishException {
        if (!(config instanceof S3PublishConfig)) {
            throw new PublishException("Config must be S3PublishConfig");
        }
        
        S3PublishConfig s3Config = (S3PublishConfig) config;
        String key = s3Config.getKeyPrefix() + fileName;
        uploadWithRetry(reportData, key, s3Config, ReportFiles.contentType(fileName));
        logger.info("Successfully published report to S3: s3://{}/{}", s3Config.getBucketName(), key);
    }

    private void uploadWithRetry(byte[] reportData, String key, S3PublishConfig s3Config, String contentType)
            throws PublishException {
        int attempt = 0;
        long delay = s3Config.getRetryDelayMs();
        Exception lastException = null;

        while (attempt < s3Config.getRetryAttempts()) {
            try {
                uploadToS3(reportData, key, s3Config, contentType);
                return;
            } catch (S3Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt >= s3Config.getRetryAttempts()) {
                    break;
                }
                
                logger.warn("S3 upload failed (attempt {}/{}): {}", attempt, s3Config.getRetryAttempts(), e.getMessage());
                
                try {
                    Thread.sleep(Math.min(delay, MAX_DELAY_MS));
                    delay *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new PublishException("Upload interrupted", ie);
                }
            }
        }
        
        throw new PublishException(
                "Failed to publish to S3 after %d attempts".formatted(s3Config.getRetryAttempts()),
                lastException);
    }

    private void uploadToS3(byte[] data, String key, S3PublishConfig config, String contentType) {
        Map<String, String> metadata = new HashMap<>(config.getMetadata());
        
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(config.getBucketName())
                .key(key)
                .metadata(metadata);
        if (contentType != null) {
            requestBuilder.contentType(contentType);
        }
        
        s3Client.putObject(requestBuilder.build(), RequestBody.fromBytes(data));
    }
}
