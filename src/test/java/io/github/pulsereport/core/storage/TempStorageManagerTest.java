package io.github.pulsereport.core.storage;

import io.github.pulsereport.core.model.TestCase;
import io.github.pulsereport.core.model.TestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TempStorageManager.
 * 
 * @author Custom Reporter Team
 * @since 1.0.0
 */
public class TempStorageManagerTest {

    private TempStorageManager storageManager;
    private Path tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("reporter-test-");
        storageManager = new TempStorageManager(tempDir);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (storageManager != null) {
            storageManager.cleanup();
        }
    }

    @Test
    public void testWriteAndReadSingleBatch() throws IOException {
        List<TestCase> testCases = createTestCases(10);
        
        storageManager.writeBatch(testCases, 0);
        
        List<TestCase> readCases = storageManager.readAllBatches().collect(Collectors.toList());
        
        assertEquals(10, readCases.size());
        assertEquals(testCases.get(0).getId(), readCases.get(0).getId());
        assertEquals(testCases.get(9).getId(), readCases.get(9).getId());
    }

    @Test
    public void testWriteAndReadMultipleBatches() throws IOException {
        List<TestCase> batch1 = createTestCases(100);
        List<TestCase> batch2 = createTestCases(100);
        List<TestCase> batch3 = createTestCases(100);
        
        storageManager.writeBatch(batch1, 0);
        storageManager.writeBatch(batch2, 1);
        storageManager.writeBatch(batch3, 2);
        
        List<TestCase> allCases = storageManager.readAllBatches().collect(Collectors.toList());
        
        assertEquals(300, allCases.size());
    }

    @Test
    public void testEmptyBatch() throws IOException {
        List<TestCase> emptyBatch = new ArrayList<>();
        
        storageManager.writeBatch(emptyBatch, 0);
        
        List<TestCase> readCases = storageManager.readAllBatches().collect(Collectors.toList());
        
        assertEquals(0, readCases.size());
    }

    @Test
    public void testCleanup() throws IOException {
        List<TestCase> testCases = createTestCases(10);
        storageManager.writeBatch(testCases, 0);
        
        assertTrue(Files.exists(tempDir));
        
        storageManager.cleanup();
        
        assertFalse(Files.exists(tempDir));
    }

    @Test
    public void testLargeBatch() throws IOException {
        List<TestCase> largeBatch = createTestCases(1000);
        
        storageManager.writeBatch(largeBatch, 0);
        
        List<TestCase> readCases = storageManager.readAllBatches().collect(Collectors.toList());
        
        assertEquals(1000, readCases.size());
    }

    @Test
    public void testPreservesTestCaseData() throws IOException {
        TestCase original = TestCase.builder()
                .id("test-123")
                .name("Sample Test")
                .className("com.example.TestClass")
                .methodName("testMethod")
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(5))
                .duration(5000)
                .status(TestStatus.PASSED)
                .errorMessage(null)
                .stackTrace(null)
                .retryCount(0)
                .build();
        
        List<TestCase> batch = List.of(original);
        storageManager.writeBatch(batch, 0);
        
        TestCase restored = storageManager.readAllBatches().findFirst().orElse(null);
        
        assertNotNull(restored);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getClassName(), restored.getClassName());
        assertEquals(original.getMethodName(), restored.getMethodName());
        assertEquals(original.getStatus(), restored.getStatus());
        assertEquals(original.getDuration(), restored.getDuration());
    }

    @Test
    public void testReadEmptyStorage() throws IOException {
        List<TestCase> readCases = storageManager.readAllBatches().collect(Collectors.toList());
        
        assertEquals(0, readCases.size());
    }

    @Test
    public void testGetStorageDirectory() {
        assertEquals(tempDir, storageManager.getStorageDir());
    }

    @Test
    public void testNullBatchThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            storageManager.writeBatch(null, 0);
        });
    }

    /**
     * Helper method to create test cases for testing.
     */
    private List<TestCase> createTestCases(int count) {
        List<TestCase> testCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            TestCase testCase = TestCase.builder()
                    .id("test-" + i)
                    .name("Test " + i)
                    .className("com.example.TestClass")
                    .methodName("testMethod" + i)
                    .startTime(Instant.now())
                    .endTime(Instant.now().plusSeconds(1))
                    .duration(1000)
                    .status(i % 10 == 0 ? TestStatus.FAILED : TestStatus.PASSED)
                    .errorMessage(i % 10 == 0 ? "Test failed" : null)
                    .stackTrace(i % 10 == 0 ? "at TestClass.testMethod" + i : null)
                    .retryCount(0)
                    .build();
            
            testCases.add(testCase);
        }
        
        return testCases;
    }
}
