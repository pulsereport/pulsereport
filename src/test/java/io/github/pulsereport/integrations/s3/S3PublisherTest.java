package io.github.pulsereport.integrations.s3;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pulsereport.integrations.PublishException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Tests for S3Publisher.
 */
class S3PublisherTest {

    @TempDir
    File tempDir;

    private S3Client mockS3Client;
    private S3Publisher publisher;
    private S3PublishConfig s3Config;

    @BeforeEach
    void setUp() {
        mockS3Client = mock(S3Client.class);

        s3Config = S3PublishConfig.builder()
                .bucketName("test-bucket")
                .keyPrefix("reports/")
                .region("us-east-1")
                .retryAttempts(3)
                .retryDelayMs(100)
                .build();

        publisher = new S3Publisher(mockS3Client);
    }

    @Test
    void publishFile_success() throws Exception {
        File reportFile = new File(tempDir, "report.json");
        Files.writeString(reportFile.toPath(), "{\"test\": \"data\"}");

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        assertDoesNotThrow(() -> publisher.publish(reportFile, s3Config));

        verify(mockS3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void publishBytes_success() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        assertDoesNotThrow(() -> publisher.publish(reportData, fileName, s3Config));

        verify(mockS3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void publish_withMetadata() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("buildId", "12345");
        metadata.put("branch", "main");

        S3PublishConfig configWithMetadata = S3PublishConfig.builder()
                .bucketName("test-bucket")
                .keyPrefix("reports/")
                .region("us-east-1")
                .metadata(metadata)
                .build();

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        publisher.publish(reportData, fileName, configWithMetadata);

        verify(mockS3Client).putObject(
                argThat((PutObjectRequest request)
                        -> request.metadata().containsKey("buildId")
                && request.metadata().get("buildId").equals("12345")
                ),
                any(RequestBody.class)
        );
    }

    @Test
    void publish_retryOnFailure() throws Exception {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        // Fail twice, then succeed
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Throttling").statusCode(503).build())
                .thenThrow(S3Exception.builder().message("Throttling").statusCode(503).build())
                .thenReturn(PutObjectResponse.builder().build());

        assertDoesNotThrow(() -> publisher.publish(reportData, fileName, s3Config));

        verify(mockS3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void publish_failAfterRetries() {
        byte[] reportData = "{\"test\": \"data\"}".getBytes();
        String fileName = "report.json";

        // Always fail
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Access Denied").statusCode(403).build());

        PublishException ex = assertThrows(PublishException.class, () -> publisher.publish(reportData, fileName, s3Config));
        assertNotNull(ex.getMessage());

        verify(mockS3Client, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void publish_fileNotFound() {
        File nonExistentFile = new File(tempDir, "nonexistent.json");

        PublishException ex = assertThrows(PublishException.class, () -> publisher.publish(nonExistentFile, s3Config));
        assertNotNull(ex.getMessage());
    }

    @Test
    void s3PublishConfig() {
        S3PublishConfig config = S3PublishConfig.builder()
                .bucketName("my-bucket")
                .keyPrefix("prefix/")
                .region("eu-west-1")
                .retryAttempts(5)
                .retryDelayMs(2000)
                .build();

        assertEquals("my-bucket", config.getBucketName());
        assertEquals("prefix/", config.getKeyPrefix());
        assertEquals("eu-west-1", config.getRegion());
        assertEquals(5, config.getRetryAttempts());
        assertEquals(2000, config.getRetryDelayMs());
    }
}
