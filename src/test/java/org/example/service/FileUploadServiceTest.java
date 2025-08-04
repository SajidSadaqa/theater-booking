package org.example.service;

import org.example.exception.BusinessException;
import org.example.repository.TheaterRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FileUploadServiceTest {

    @Mock
    private TheaterRepository theaterRepository;

    private FileUploadService fileUploadService;
    private File tempDir;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        fileUploadService = new FileUploadService(theaterRepository, 2);
        tempDir = new File(System.getProperty("java.io.tmpdir"));
    }

    @Test(expected = BusinessException.class)
    public void testProcessInvalidFileExtension() {
        fileUploadService.processCsvFile("test.txt", 1L);
    }

    @Test(expected = BusinessException.class)
    public void testProcessNonExistentFile() {
        fileUploadService.processCsvFile("nonexistent.csv", 1L);
    }

    @Test
    public void testProcessValidCsvFile() throws IOException, SQLException {
        File csvFile = createTestCsvFile("valid_test.csv",
                "section,row,seat_start,seat_end\n" +
                        "Orchestra,1,1,10\n" +
                        "Orchestra,2,1,10\n" +
                        "Balcony,1,1,8\n");

        try {
            doNothing().when(theaterRepository).bulkInsertTheaterLayout(any());

            FileUploadService.UploadResult result = fileUploadService.processCsvFile(
                    csvFile.getAbsolutePath(), 1L);

            assertTrue(result.isSuccess());
            assertEquals(3, result.getRecordsProcessed());

        } finally {
            csvFile.delete();
        }
    }

    @Test
    public void testProcessCsvFileWithInvalidHeaders() throws IOException {
        File csvFile = createTestCsvFile("invalid_headers.csv",
                "wrong,headers,here,invalid\n" +
                        "Orchestra,1,1,10\n");

        try {
            FileUploadService.UploadResult result = fileUploadService.processCsvFile(
                    csvFile.getAbsolutePath(), 1L);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Invalid CSV headers"));

        } finally {
            csvFile.delete();
        }
    }

    @Test
    public void testProcessCsvFileWithInvalidSeatRange() throws IOException {
        File csvFile = createTestCsvFile("invalid_range.csv",
                "section,row,seat_start,seat_end\n" +
                        "Orchestra,1,10,5\n"); // Invalid: start > end

        try {
            FileUploadService.UploadResult result = fileUploadService.processCsvFile(
                    csvFile.getAbsolutePath(), 1L);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("seat_start cannot be greater than seat_end"));

        } finally {
            csvFile.delete();
        }
    }

    @Test
    public void testProcessMultipleFilesAsync() throws Exception {
        File csvFile1 = createTestCsvFile("test1.csv",
                "section,row,seat_start,seat_end\n" +
                        "Orchestra,1,1,5\n");

        File csvFile2 = createTestCsvFile("test2.csv",
                "section,row,seat_start,seat_end\n" +
                        "Balcony,1,1,3\n");

        try {
            doNothing().when(theaterRepository).bulkInsertTheaterLayout(any());

            List<String> filePaths = Arrays.asList(
                    csvFile1.getAbsolutePath(),
                    csvFile2.getAbsolutePath());

            CompletableFuture<List<FileUploadService.UploadResult>> future =
                    fileUploadService.processMultipleFiles(filePaths, 1L);

            List<FileUploadService.UploadResult> results = future.get();

            assertEquals(2, results.size());
            assertTrue(results.get(0).isSuccess());
            assertTrue(results.get(1).isSuccess());

        } finally {
            csvFile1.delete();
            csvFile2.delete();
        }
    }

    private File createTestCsvFile(String fileName, String content) throws IOException {
        File csvFile = new File(tempDir, fileName);
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write(content);
        }
        return csvFile;
    }
}
