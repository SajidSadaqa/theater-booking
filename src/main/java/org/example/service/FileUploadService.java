package org.example.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.example.exception.BusinessException;
import org.example.model.*;
import org.example.repository.TheaterRepository;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class FileUploadService {

    private final TheaterRepository theaterRepository;
    private final ExecutorService executorService;
    private final int maxConcurrentUploads;

    public FileUploadService() {
        this.theaterRepository = new TheaterRepository();
        this.maxConcurrentUploads = 5; // From config
        this.executorService = Executors.newFixedThreadPool(maxConcurrentUploads);
    }

    public FileUploadService(TheaterRepository theaterRepository, int maxConcurrentUploads) {
        this.theaterRepository = theaterRepository;
        this.maxConcurrentUploads = maxConcurrentUploads;
        this.executorService = Executors.newFixedThreadPool(maxConcurrentUploads);
    }

    /*
     * Process multiple CSV files asynchronously
     */

    public CompletableFuture<List<UploadResult>> processMultipleFiles(List<String> filePaths, Long theaterId) {
        List<CompletableFuture<UploadResult>> futures = filePaths.stream()
                .map(filePath -> processFileAsync(filePath, theaterId))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    /*
     * Process a single CSV file asynchronously
     */

    public CompletableFuture<UploadResult> processFileAsync(String filePath, Long theaterId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processCsvFile(filePath, theaterId);
            } catch (Exception e) {
                return new UploadResult(filePath, false, e.getMessage(), 0);
            }
        }, executorService);
    }

    /*
     * Process a single CSV file synchronously
     */

    public UploadResult processCsvFile(String filePath, Long theaterId) {
        try {
            // Validate file
            validateFile(filePath);

            // Parse and verify format
            List<SeatData> seatDataList = parseCsvFile(filePath);

            // Verify data integrity
            verifySeatData(seatDataList);

            // Convert to theater layout
            Theater theaterLayout = convertToTheaterLayout(seatDataList, theaterId);

            // Bulk insert
            theaterRepository.bulkInsertTheaterLayout(theaterLayout);

            return new UploadResult(filePath, true, "Success", seatDataList.size());

        } catch (BusinessException e) {
            return new UploadResult(filePath, false, "Business Error: " + e.getMessage(), 0);
        } catch (SQLException e) {
            return new UploadResult(filePath, false, "Database Error: " + e.getMessage(), 0);
        } catch (Exception e) {
            return new UploadResult(filePath, false, "Unexpected Error: " + e.getMessage(), 0);
        }
    }

    private void validateFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new BusinessException("File path cannot be null or empty");
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new BusinessException("File does not exist: " + filePath);
        }

        if (!filePath.toLowerCase().endsWith(".csv")) {
            throw new BusinessException("Unsupported file type. Only CSV files are supported.");
        }

        try {
            long fileSize = Files.size(path);
            long maxSize = 10 * 1024 * 1024; // 10MB
            if (fileSize > maxSize) {
                throw new BusinessException("File size exceeds maximum allowed size of 10MB");
            }
        } catch (IOException e) {
            throw new BusinessException("Cannot read file size: " + e.getMessage());
        }
    }

    private List<SeatData> parseCsvFile(String filePath) {
        List<SeatData> seatDataList = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();

            if (records.isEmpty()) {
                throw new BusinessException("CSV file is empty");
            }

            // Validate headers
            String[] headers = records.get(0);
            validateCsvHeaders(headers);

            // Process data rows
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                if (record.length < 4) {
                    throw new BusinessException("Invalid data at line " + (i + 1) + ": insufficient columns");
                }

                try {
                    SeatData seatData = new SeatData(
                            record[0].trim(), // section
                            Integer.parseInt(record[1].trim()), // row
                            Integer.parseInt(record[2].trim()), // seat_start
                            Integer.parseInt(record[3].trim())  // seat_end
                    );

                    if (record.length > 4 && !record[4].trim().isEmpty()) {
                        seatData.setStatus(SeatStatus.valueOf(record[4].trim().toUpperCase()));
                    }

                    seatDataList.add(seatData);

                } catch (NumberFormatException e) {
                    throw new BusinessException("Invalid number format at line " + (i + 1) + ": " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("Invalid seat status at line " + (i + 1) + ": " + e.getMessage());
                }
            }

        } catch (IOException | CsvException e) {
            throw new BusinessException("Error reading CSV file: " + e.getMessage(), e);
        }

        return seatDataList;
    }

    private void validateCsvHeaders(String[] headers) {
        if (headers.length < 4) {
            throw new BusinessException("CSV must have at least 4 columns: section, row, seat_start, seat_end");
        }

        // Trim whitespace from headers
        for (int i = 0; i < headers.length; i++) {
            headers[i] = headers[i].trim();
        }

        String[] expectedHeaders = {"section", "row", "seat_start", "seat_end"};
        for (int i = 0; i < expectedHeaders.length; i++) {
            if (!headers[i].equalsIgnoreCase(expectedHeaders[i])) {
                throw new BusinessException("Invalid CSV headers. Expected: section, row, seat_start, seat_end [, status]");
            }
        }

        // status column
        if (headers.length > 4 && !headers[4].equalsIgnoreCase("status")) {
            throw new BusinessException("Fifth column must be 'status' if present");
        }
    }

    private void verifySeatData(List<SeatData> seatDataList) {
        if (seatDataList.isEmpty()) {
            throw new BusinessException("No valid seat data found in CSV file");
        }

        List<String> errors = new ArrayList<>();
        Set<String> duplicateCheck = new HashSet<>();

        for (int i = 0; i < seatDataList.size(); i++) {
            SeatData data = seatDataList.get(i);
            int lineNumber = i + 2; // +2 because index starts at 0 and we skip header

            // Validate seat range
            if (data.getSeatStart() <= 0 || data.getSeatEnd() <= 0) {
                errors.add("Line " + lineNumber + ": Seat numbers must be positive");
            }

            if (data.getSeatStart() > data.getSeatEnd()) {
                errors.add("Line " + lineNumber + ": seat_start cannot be greater than seat_end");
            }

            if (data.getRowNumber() <= 0) {
                errors.add("Line " + lineNumber + ": Row number must be positive");
            }

            if (data.getSectionName().isEmpty()) {
                errors.add("Line " + lineNumber + ": Section name cannot be empty");
            }

            // Check for duplicates
            for (int seatNum = data.getSeatStart(); seatNum <= data.getSeatEnd(); seatNum++) {
                String seatKey = data.getSectionName() + "-" + data.getRowNumber() + "-" + seatNum;
                if (!duplicateCheck.add(seatKey)) {
                    errors.add("Line " + lineNumber + ": Duplicate seat found: " + seatKey);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new BusinessException("Data validation failed:\n" + String.join("\n", errors));
        }
    }

    private Theater convertToTheaterLayout(List<SeatData> seatDataList, Long theaterId) {
        Theater theater = new Theater(theaterId, "Theater " + theaterId);
        Map<String, Section> sectionMap = new HashMap<>();

        for (SeatData data : seatDataList) {
            // Get or create section
            Section section = sectionMap.computeIfAbsent(data.getSectionName(),
                    name -> {
                        Section s = new Section(name);
                        theater.addSection(s);
                        return s;
                    });

            // Find or create row
            Row row = section.getRows().stream()
                    .filter(r -> r.getNumber() == data.getRowNumber())
                    .findFirst()
                    .orElseGet(() -> {
                        Row r = new Row(data.getRowNumber());
                        section.addRow(r);
                        return r;
                    });

            // Add seats
            for (int seatNum = data.getSeatStart(); seatNum <= data.getSeatEnd(); seatNum++) {
                Seat seat = new Seat(seatNum);
                seat.setStatus(data.getStatus());
                row.addSeat(seat);
            }
        }

        return theater;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    // Inner classes
    public static class SeatData {
        private String sectionName;
        private int rowNumber;
        private int seatStart;
        private int seatEnd;
        private SeatStatus status = SeatStatus.AVAILABLE;

        public SeatData(String sectionName, int rowNumber, int seatStart, int seatEnd) {
            this.sectionName = sectionName;
            this.rowNumber = rowNumber;
            this.seatStart = seatStart;
            this.seatEnd = seatEnd;
        }

        // Getters and setters
        public String getSectionName() { return sectionName; }
        public void setSectionName(String sectionName) { this.sectionName = sectionName; }

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

        public int getSeatStart() { return seatStart; }
        public void setSeatStart(int seatStart) { this.seatStart = seatStart; }

        public int getSeatEnd() { return seatEnd; }
        public void setSeatEnd(int seatEnd) { this.seatEnd = seatEnd; }

        public SeatStatus getStatus() { return status; }
        public void setStatus(SeatStatus status) { this.status = status; }
    }

    public static class UploadResult {
        private String fileName;
        private boolean success;
        private String message;
        private int recordsProcessed;

        public UploadResult(String fileName, boolean success, String message, int recordsProcessed) {
            this.fileName = fileName;
            this.success = success;
            this.message = message;
            this.recordsProcessed = recordsProcessed;
        }

        public String getFileName() { return fileName; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getRecordsProcessed() { return recordsProcessed; }

        @Override
        public String toString() {
            return String.format("File: %s | Success: %s | Records: %d | Message: %s",
                    fileName, success, recordsProcessed, message);
        }
    }
}

