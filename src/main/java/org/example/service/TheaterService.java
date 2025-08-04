package org.example.service;

import org.example.model.*;
import org.example.repository.TheaterRepository;
import org.example.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TheaterService {

    private final TheaterRepository theaterRepository;

    public TheaterService() {
        this.theaterRepository = new TheaterRepository();
    }

    public TheaterService(TheaterRepository theaterRepository) {
        this.theaterRepository = theaterRepository;
    }

    public Long createTheater(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Theater name cannot be null or empty");
        }
        return theaterRepository.createTheater(name.trim());
    }

    public Theater createTheaterLayout(String theaterName, int numSections, int rowsPerSection, int seatsPerRow) throws SQLException {
        if (numSections <= 0 || rowsPerSection <= 0 || seatsPerRow <= 0) {
            throw new IllegalArgumentException("All layout parameters must be positive");
        }

        // Create theater
        Long theaterId = createTheater(theaterName);
        Theater theater = new Theater(theaterId, theaterName);

        // Create sections
        for (int s = 1; s <= numSections; s++) {
            Section section = new Section("Section " + s);

            // Create rows for this section
            for (int r = 1; r <= rowsPerSection; r++) {
                Row row = new Row(r);

                // Create seats for this row
                for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                    Seat seat = new Seat(seatNum);
                    row.addSeat(seat);
                }

                section.addRow(row);
            }

            theater.addSection(section);
        }

        // Save to database
        theaterRepository.bulkInsertTheaterLayout(theater);

        return theater;
    }

    public List<Theater> getAllTheaters() throws SQLException {
        return theaterRepository.findAllTheaters();
    }

    public Theater getTheaterWithLayout(Long theaterId) throws SQLException {
        return theaterRepository.findTheaterById(theaterId);
    }

    public void displaySeatingMap(Theater theater) {
        System.out.println("\n=== Seating Map for " + theater.getName() + " ===");
        System.out.printf("Total Seats: %d | Available: %d | Booked: %d%n",
                theater.getTotalSeats(),
                theater.getAvailableSeats(),
                theater.getTotalSeats() - theater.getAvailableSeats());
        System.out.println();

        for (Section section : theater.getSections()) {
            System.out.println("Section: " + section.getName() +
                    " (Available: " + section.getAvailableSeats() + "/" + section.getTotalSeats() + ")");

            for (Row row : section.getRows()) {
                System.out.printf("  Row %2d: ", row.getNumber());

                for (Seat seat : row.getSeats()) {
                    String seatDisplay = switch (seat.getStatus()) {
                        case AVAILABLE -> String.format("[%2d]", seat.getNumber());
                        case BOOKED -> " [X]";
                        case RESERVED -> " [R]";
                        case OUT_OF_ORDER -> " [-]";
                    };
                    System.out.print(seatDisplay + " ");
                }
                System.out.println();
            }
            System.out.println();
        }

        System.out.println("Legend: [##] = Available, [X] = Booked, [R] = Reserved, [-] = Out of Order");
        System.out.println();
    }

    public boolean bookSeat(Long seatId) throws SQLException {
        return theaterRepository.bookSeat(seatId);
    }

    public boolean cancelBooking(Long seatId) throws SQLException {
        return theaterRepository.cancelBooking(seatId);
    }

    public boolean updateSeatStatus(Long seatId, SeatStatus status) throws SQLException {
        return theaterRepository.updateSeatStatus(seatId, status);
    }

    public Seat findSeat(Theater theater, String sectionName, int rowNumber, int seatNumber) {
        for (Section section : theater.getSections()) {
            if (section.getName().equalsIgnoreCase(sectionName)) {
                for (Row row : section.getRows()) {
                    if (row.getNumber() == rowNumber) {
                        for (Seat seat : row.getSeats()) {
                            if (seat.getNumber() == seatNumber) {
                                return seat;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public Long createSection(Long theaterId, String sectionName) throws SQLException {
        String sql = "INSERT INTO sections (theater_id, name) VALUES (?, ?) RETURNING id";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, theaterId);
            stmt.setString(2, sectionName);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
            throw new SQLException("Failed to create section");
        }
    }

    public Long createRow(Long sectionId, int rowNumber) throws SQLException {
        String sql = "INSERT INTO rows (section_id, number) VALUES (?, ?) RETURNING id";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sectionId);
            stmt.setInt(2, rowNumber);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
            throw new SQLException("Failed to create row");
        }
    }

    public void createSeatsForRow(Long rowId, int numberOfSeats) throws SQLException {
        String sql = "INSERT INTO seats (row_id, number, is_booked) VALUES (?, ?, false)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int seatNum = 1; seatNum <= numberOfSeats; seatNum++) {
                stmt.setLong(1, rowId);
                stmt.setInt(2, seatNum);
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }
}
