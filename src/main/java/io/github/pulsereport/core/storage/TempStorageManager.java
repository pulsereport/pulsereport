package io.github.pulsereport.core.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.github.pulsereport.core.model.TestCase;

/**
 * Manages temporary file storage for intermediate test results. Used by
 * StreamingAggregator to persist batches of test cases to disk, enabling
 * memory-efficient processing of large test suites.
 *
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>JSON serialization using Jackson</li>
 * <li>Batch-based storage with sequential numbering</li>
 * <li>Streaming reads for memory efficiency</li>
 * <li>Auto-cleanup of temp files</li>
 * </ul>
 *
 * <p>
 * Batch files are named: batch-0.json, batch-1.json, etc.
 * </p>
 *
 * @author Pulse Report Team
 * @since 1.0.0
 */
public class TempStorageManager {

    private static final Logger logger = LoggerFactory.getLogger(TempStorageManager.class);
    private static final String BATCH_FILE_PREFIX = "batch-";
    private static final String BATCH_FILE_SUFFIX = ".json";

    private final Path storageDir;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new TempStorageManager.
     *
     * @param storageDir directory for temporary storage
     * @throws IOException if directory cannot be created
     */
    public TempStorageManager(Path storageDir) throws IOException {
        if (storageDir == null) {
            throw new IllegalArgumentException("Storage directory cannot be null");
        }

        this.storageDir = storageDir;
        this.objectMapper = createObjectMapper();

        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
            logger.debug("Created temp storage directory: {}", storageDir);
        }
    }

    /**
     * Writes a batch of test cases to disk.
     *
     * @param testCases list of test cases to write
     * @param batchNumber sequential batch number
     * @throws IOException if write fails
     * @throws IllegalArgumentException if testCases is null
     */
    public void writeBatch(List<TestCase> testCases, int batchNumber) throws IOException {
        if (testCases == null) {
            throw new IllegalArgumentException("Test cases cannot be null");
        }

        Path batchFile = getBatchFilePath(batchNumber);

        objectMapper.writeValue(batchFile.toFile(), testCases);

        logger.debug("Wrote batch {} with {} test cases to {}", batchNumber, testCases.size(), batchFile);
    }

    /**
     * Reads all batches from storage as a stream. Batches are read in order
     * (batch-0, batch-1, ...).
     *
     * @return stream of all test cases
     * @throws IOException if read fails
     */
    public Stream<TestCase> readAllBatches() throws IOException {
        if (!Files.exists(storageDir)) {
            logger.debug("Storage directory does not exist, returning empty stream");
            return Stream.empty();
        }

        List<Path> batchFiles = Files.list(storageDir)
                .filter(path -> path.getFileName().toString().startsWith(BATCH_FILE_PREFIX))
                .filter(path -> path.getFileName().toString().endsWith(BATCH_FILE_SUFFIX))
                .sorted(Comparator.comparing(this::extractBatchNumber))
                .collect(Collectors.toList());

        if (batchFiles.isEmpty()) {
            logger.debug("No batch files found in {}", storageDir);
            return Stream.empty();
        }

        logger.debug("Found {} batch files in {}", batchFiles.size(), storageDir);

        return batchFiles.stream()
                .flatMap(this::readBatchFile);
    }

    /**
     * Deletes all temporary files and the storage directory.
     *
     * @throws IOException if cleanup fails
     */
    public void cleanup() throws IOException {
        if (!Files.exists(storageDir)) {
            logger.debug("Storage directory does not exist, nothing to clean up");
            return;
        }

        try (Stream<Path> files = Files.walk(storageDir)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            logger.trace("Deleted: {}", path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete: {}", path, e);
                        }
                    });
        }

        logger.debug("Cleaned up temp storage directory: {}", storageDir);
    }

    /**
     * Gets the storage directory path.
     *
     * @return storage directory path
     */
    public Path getStorageDir() {
        return storageDir;
    }

    /**
     * Creates and configures the Jackson ObjectMapper.
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    /**
     * Gets the file path for a batch number.
     */
    private Path getBatchFilePath(int batchNumber) {
        String fileName = BATCH_FILE_PREFIX + batchNumber + BATCH_FILE_SUFFIX;
        return storageDir.resolve(fileName);
    }

    /**
     * Reads a single batch file and returns a stream of test cases.
     */
    private Stream<TestCase> readBatchFile(Path batchFile) {
        try {
            TestCase[] testCases = objectMapper.readValue(batchFile.toFile(), TestCase[].class);
            logger.debug("Read {} test cases from {}", testCases.length, batchFile);
            return Stream.of(testCases);
        } catch (IOException e) {
            logger.error("Failed to read batch file: {}", batchFile, e);
            return Stream.empty();
        }
    }

    /**
     * Extracts the batch number from a batch file path. Example: batch-5.json
     * -> 5
     */
    private int extractBatchNumber(Path path) {
        String fileName = path.getFileName().toString();
        String numberStr = fileName
                .substring(BATCH_FILE_PREFIX.length(), fileName.length() - BATCH_FILE_SUFFIX.length());
        try {
            return Integer.parseInt(numberStr);
        } catch (NumberFormatException e) {
            logger.warn("Invalid batch file name: {}", fileName);
            return Integer.MAX_VALUE; // Sort invalid files to the end
        }
    }
}
