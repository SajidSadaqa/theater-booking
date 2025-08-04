package org.example;

import org.example.exception.DbException;
import org.example.model.*;
import org.example.service.FileUploadService;
import org.example.service.TheaterService;
import org.example.util.DatabaseManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Main {

    private Scanner scanner;
    private TheaterService theaterService;
    private FileUploadService fileUploadService;

    public Main() {
        this.scanner = new Scanner(System.in);
        this.theaterService = new TheaterService();
        this.fileUploadService = new FileUploadService();
    }

    public static void main(String[] args) {
        Main app = new Main();

        try {
            DatabaseManager.getInstance().initialize();
            app.run();

        } catch (DbException e) {
            System.err.println("PostgreSQL database initialization failed: " + e.getMessage());
            System.err.println("Please ensure PostgreSQL is running and accessible.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Application error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            // Cleanup
            app.shutdown();
        }
    }

    public void run() {
        System.out.println("=== Theater Booking Management System ===");
        System.out.println("Welcome! Let's manage your theaters.\n");
        System.out.println("Connected to PostgreSQL database successfully.\n");

        while (true) {
            displayMainMenu();

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1 -> createNewTheater();
                    case 2 -> listAndSelectTheater();
                    case 3 -> viewSeatingMap();
                    case 4 -> bookSeat();
                    case 5 -> cancelBooking();
                    case 6 -> uploadCsvFiles();
                    case 7 -> {
                        System.out.println("Thank you for using Theater Booking System!");
                        return;
                    }
                    default -> System.out.println("Invalid choice. Please try again.\n");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.\n");
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage() + "\n");
            }
        }
    }

    private void displayMainMenu() {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║           MAIN MENU                ║");
        System.out.println("╠════════════════════════════════════╣");
        System.out.println("║ 1. Create New Theater              ║");
        System.out.println("║ 2. List Theaters & Choose One     ║");
        System.out.println("║ 3. View Seating Map               ║");
        System.out.println("║ 4. Book Seat                      ║");
        System.out.println("║ 5. Cancel Booking                 ║");
        System.out.println("║ 6. Upload CSV File(s)             ║");
        System.out.println("║ 7. Exit                           ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.print("Enter your choice: ");
    }

    private void createNewTheater() {
        System.out.println("\n=== Create New Theater ===");

        System.out.print("Enter theater name: ");
        String name = scanner.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Theater name cannot be empty.\n");
            return;
        }

        System.out.print("Do you want to create a custom layout? (y/n): ");
        String createLayout = scanner.nextLine().trim().toLowerCase();

        try {
            if (createLayout.equals("y") || createLayout.equals("yes")) {
                createTheaterWithLayout(name);
            } else {
                Long theaterId = theaterService.createTheater(name);
                System.out.println("Theater '" + name + "' created successfully with ID: " + theaterId);
                System.out.println("You can upload seating layout later using CSV files.\n");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate key") || e.getMessage().contains("unique constraint")) {
                System.out.println("Theater with name '" + name + "' already exists.\n");
            } else {
                System.out.println("Error creating theater: " + e.getMessage() + "\n");
            }
        }
    }

    private void createTheaterWithLayout(String name) throws SQLException {
        System.out.print("Number of sections: ");
        int numSections = Integer.parseInt(scanner.nextLine().trim());

        System.out.println("Choose layout type:");
        System.out.println("1. Uniform layout (same seats per row)");
        System.out.println("2. Custom layout (different seats per row)");
        System.out.print("Enter choice (1-2): ");

        int layoutChoice = Integer.parseInt(scanner.nextLine().trim());

        if (layoutChoice == 1) {
            createUniformLayout(name, numSections);
        } else if (layoutChoice == 2) {
            createCustomLayout(name, numSections);
        } else {
            System.out.println("Invalid choice. Using uniform layout as default.");
            createUniformLayout(name, numSections);
        }
    }

    private void createUniformLayout(String name, int numSections) throws SQLException {
        System.out.print("Rows per section: ");
        int rowsPerSection = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Seats per row: ");
        int seatsPerRow = Integer.parseInt(scanner.nextLine().trim());

        Theater theater = theaterService.createTheaterLayout(name, numSections, rowsPerSection, seatsPerRow);

        System.out.println("Theater '" + name + "' created successfully!");
        System.out.printf("Layout: %d sections, %d rows per section, %d seats per row%n",
                numSections, rowsPerSection, seatsPerRow);
        System.out.printf("Total seats: %d%n%n", theater.getTotalSeats());
    }

    private void createCustomLayout(String name, int numSections) throws SQLException {
        // Create the theater first
        Long theaterId = theaterService.createTheater(name);

        System.out.println("Creating custom layout for theater: " + name);

        int totalSeats = 0;

        for (int sectionNum = 1; sectionNum <= numSections; sectionNum++) {
            System.out.printf("%n=== Section %d Configuration ===%n", sectionNum);
            System.out.print("Section name (or press Enter for default): ");
            String sectionName = scanner.nextLine().trim();
            if (sectionName.isEmpty()) {
                sectionName = "Section " + sectionNum;
            }

            System.out.print("Number of rows in " + sectionName + ": ");
            int numRows = Integer.parseInt(scanner.nextLine().trim());

            // Create section
            Long sectionId = theaterService.createSection(theaterId, sectionName);

            for (int rowNum = 1; rowNum <= numRows; rowNum++) {
                System.out.printf("Row %d - Number of seats: ", rowNum);
                int seatsInRow = Integer.parseInt(scanner.nextLine().trim());

                // Create row
                Long rowId = theaterService.createRow(sectionId, rowNum);

                // Create seats for this row
                for (int seatNum = 1; seatNum <= seatsInRow; seatNum++) {
                    theaterService.createSeatsForRow(rowId, seatNum);
                    totalSeats++;
                }

                System.out.printf("  → Row %d created with %d seats%n", rowNum, seatsInRow);
            }

            System.out.printf("Section '%s' completed with %d rows%n", sectionName, numRows);
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.println("Theater '" + name + "' created successfully!");
        System.out.printf("Custom layout: %d sections, Total seats: %d%n", numSections, totalSeats);
        System.out.println("=".repeat(50) + "\n");
    }

    private void listAndSelectTheater() {
        try {
            List<Theater> theaters = theaterService.getAllTheaters();
            if (theaters.isEmpty()) {
                System.out.println("No theaters available. Please create a theater first.\n");
                return;
            }

            System.out.println("\n=== Available Theaters ===");
            for (int i = 0; i < theaters.size(); i++) {
                Theater theater = theaterService.getTheaterWithLayout(theaters.get(i).getId());
                System.out.printf("%d. %s (Seats: %d/%d available)%n",
                        (i + 1), theater.getName(),
                        theater.getAvailableSeats(), theater.getTotalSeats());
            }

            System.out.print("\nSelect theater (enter number): ");
            int choice = Integer.parseInt(scanner.nextLine().trim()) - 1;

            if (choice >= 0 && choice < theaters.size()) {
                Theater selected = theaterService.getTheaterWithLayout(theaters.get(choice).getId());
                System.out.println("Selected: " + selected.getName());
                theaterService.displaySeatingMap(selected);
            } else {
                System.out.println("Invalid selection.\n");
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage() + "\n");
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.\n");
        }
    }

    private void viewSeatingMap() {
        try {
            Theater theater = selectTheater();
            if (theater != null) {
                theaterService.displaySeatingMap(theater);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage() + "\n");
        }
    }

    private void bookSeat() {
        try {
            Theater theater = selectTheater();
            if (theater == null) return;

            Seat seat = selectSeat(theater, true); // only available seats
            if (seat == null) return;

            if (theaterService.bookSeat(seat.getId())) {
                System.out.println("Seat booked successfully!");
                System.out.printf("Booked: Section %s, Row %d, Seat %d%n%n",
                        seat.getRow().getSection().getName(),
                        seat.getRow().getNumber(),
                        seat.getNumber());
            } else {
                System.out.println("Failed to book seat. It may have been booked by someone else.\n");
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage() + "\n");
        }
    }

    private void cancelBooking() {
        try {
            Theater theater = selectTheater();
            if (theater == null) return;

            Seat seat = selectSeat(theater, false); // only booked seats
            if (seat == null) return;

            if (!seat.isBooked()) {
                System.out.println("Selected seat is not booked.\n");
                return;
            }

            if (theaterService.cancelBooking(seat.getId())) {
                System.out.println("Booking cancelled successfully!");
                System.out.printf("Cancelled: Section %s, Row %d, Seat %d%n%n",
                        seat.getRow().getSection().getName(),
                        seat.getRow().getNumber(),
                        seat.getNumber());
            } else {
                System.out.println("Failed to cancel booking.\n");
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage() + "\n");
        }
    }

    private void uploadCsvFiles() {
        try {
            Theater theater = selectTheater();
            if (theater == null) return;

            List<String> filePaths = new ArrayList<>();

            System.out.println("Enter CSV file paths (one per line, empty line to finish):");
            System.out.println("Example: /path/to/theater-layout.csv");
            while (true) {
                System.out.print("File path: ");
                String filePath = scanner.nextLine().trim();
                if (filePath.isEmpty()) break;
                filePaths.add(filePath);
            }

            if (filePaths.isEmpty()) {
                System.out.println("No files specified.\n");
                return;
            }

            System.out.println("Processing " + filePaths.size() + " file(s) asynchronously...\n");

            CompletableFuture<List<FileUploadService.UploadResult>> future =
                    fileUploadService.processMultipleFiles(filePaths, theater.getId());

            List<FileUploadService.UploadResult> results = future.get();

            System.out.println("=== Upload Results ===");
            int successful = 0;
            for (FileUploadService.UploadResult result : results) {
                System.out.println(result);
                if (result.isSuccess()) successful++;
            }

            System.out.printf("%nSummary: %d/%d files processed successfully%n%n",
                    successful, results.size());

        } catch (Exception e) {
            System.out.println("Upload error: " + e.getMessage() + "\n");
        }
    }

    private Theater selectTheater() throws SQLException {
        List<Theater> theaters = theaterService.getAllTheaters();
        if (theaters.isEmpty()) {
            System.out.println("No theaters available.\n");
            return null;
        }

        System.out.println("\nAvailable theaters:");
        for (int i = 0; i < theaters.size(); i++) {
            System.out.println((i + 1) + ". " + theaters.get(i).getName());
        }

        System.out.print("Select theater (enter number): ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim()) - 1;

            if (choice >= 0 && choice < theaters.size()) {
                return theaterService.getTheaterWithLayout(theaters.get(choice).getId());
            } else {
                System.out.println("Invalid selection.\n");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.\n");
            return null;
        }
    }

    private Seat selectSeat(Theater theater, boolean availableOnly) {
        if (theater.getSections().isEmpty()) {
            System.out.println("Theater has no seating layout.\n");
            return null;
        }

        try {
            // Select section
            System.out.println("\nAvailable sections:");
            List<Section> sections = theater.getSections();
            for (int i = 0; i < sections.size(); i++) {
                Section section = sections.get(i);
                System.out.printf("%d. %s (%d/%d available)%n",
                        (i + 1), section.getName(),
                        section.getAvailableSeats(), section.getTotalSeats());
            }

            System.out.print("Select section (enter number): ");
            int sectionChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;

            if (sectionChoice < 0 || sectionChoice >= sections.size()) {
                System.out.println("Invalid section selection.\n");
                return null;
            }

            Section selectedSection = sections.get(sectionChoice);

            // Select row
            System.out.println("\nAvailable rows in " + selectedSection.getName() + ":");
            List<Row> rows = selectedSection.getRows();
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                System.out.printf("%d. Row %d (%d/%d available)%n",
                        (i + 1), row.getNumber(),
                        row.getAvailableSeats(), row.getTotalSeats());
            }

            System.out.print("Select row (enter number): ");
            int rowChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;

            if (rowChoice < 0 || rowChoice >= rows.size()) {
                System.out.println("Invalid row selection.\n");
                return null;
            }

            Row selectedRow = rows.get(rowChoice);

            // Select seat
            System.out.println("\nSeats in Row " + selectedRow.getNumber() + ":");
            List<Seat> availableSeats = new ArrayList<>();

            for (Seat seat : selectedRow.getSeats()) {
                if (availableOnly && seat.isAvailable()) {
                    availableSeats.add(seat);
                    System.out.printf("%d. Seat %d [Available]%n",
                            availableSeats.size(), seat.getNumber());
                } else if (!availableOnly && seat.isBooked()) {
                    availableSeats.add(seat);
                    System.out.printf("%d. Seat %d [Booked]%n",
                            availableSeats.size(), seat.getNumber());
                }
            }

            if (availableSeats.isEmpty()) {
                if (availableOnly) {
                    System.out.println("No available seats in this row.\n");
                } else {
                    System.out.println("No booked seats in this row.\n");
                }
                return null;
            }

            System.out.print("Select seat (enter number): ");
            int seatChoice = Integer.parseInt(scanner.nextLine().trim()) - 1;

            if (seatChoice >= 0 && seatChoice < availableSeats.size()) {
                return availableSeats.get(seatChoice);
            } else {
                System.out.println("Invalid seat selection.\n");
                return null;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.\n");
            return null;
        }
    }

    public void shutdown() {
        try {
            if (fileUploadService != null) {
                fileUploadService.shutdown();
            }

            if (scanner != null) {
                scanner.close();
            }

            // Close database connections
            DatabaseManager.getInstance().close();

            System.out.println("Application shutdown completed.");

        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
}