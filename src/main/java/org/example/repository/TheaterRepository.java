package org.example.repository;

import org.example.model.*;
import org.example.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TheaterRepository {

    private final DatabaseManager dbManager;

    public TheaterRepository() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Long createTheater(String name) throws SQLException {
        String sql = "INSERT INTO theaters (name) VALUES (?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Failed to get generated theater ID");
            }
        }
    }

    public List<Theater> findAllTheaters() throws SQLException {
        List<Theater> theaters = new ArrayList<>();
        String sql = "SELECT id, name FROM theaters ORDER BY name";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Theater theater = new Theater(rs.getLong("id"), rs.getString("name"));
                theaters.add(theater);
            }
        }

        return theaters;
    }

    public Theater findTheaterById(Long theaterId) throws SQLException {
        Theater theater = null;

        String theaterSql = "SELECT id, name FROM theaters WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(theaterSql)) {

            stmt.setLong(1, theaterId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    theater = new Theater(rs.getLong("id"), rs.getString("name"));
                }
            }
        }

        if (theater == null) return null;

        loadSectionsForTheater(theater);

        return theater;
    }

    private void loadSectionsForTheater(Theater theater) throws SQLException {
        String sectionSql = "SELECT id, name FROM sections WHERE theater_id = ? ORDER BY name";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sectionSql)) {

            stmt.setLong(1, theater.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Section section = new Section(rs.getLong("id"), rs.getString("name"));
                    loadRowsForSection(section);
                    theater.addSection(section);
                }
            }
        }
    }

    private void loadRowsForSection(Section section) throws SQLException {
        String rowSql = "SELECT id, number FROM rows WHERE section_id = ? ORDER BY number";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(rowSql)) {

            stmt.setLong(1, section.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Row row = new Row(rs.getLong("id"), rs.getInt("number"));
                    loadSeatsForRow(row);
                    section.addRow(row);
                }
            }
        }
    }

    private void loadSeatsForRow(Row row) throws SQLException {
        String seatSql = "SELECT id, number, status FROM seats WHERE row_id = ? ORDER BY number";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(seatSql)) {

            stmt.setLong(1, row.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SeatStatus status = SeatStatus.valueOf(rs.getString("status"));
                    Seat seat = new Seat(rs.getLong("id"), rs.getInt("number"), status);
                    row.addSeat(seat);
                }
            }
        }
    }

    public void bulkInsertTheaterLayout(Theater theater) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                insertSections(conn, theater);

                for (Section section : theater.getSections()) {
                    insertRows(conn, section);

                    for (Row row : section.getRows()) {
                        insertSeats(conn, row);
                    }
                }
                conn.commit();
                System.out.println("Theater layout inserted successfully.");

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void insertSections(Connection conn, Theater theater) throws SQLException {
        String sql = "INSERT INTO sections (theater_id, name) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Section section : theater.getSections()) {
                stmt.setLong(1, theater.getId());
                stmt.setString(2, section.getName());
                stmt.addBatch();
            }

            stmt.executeBatch();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                int index = 0;
                while (rs.next() && index < theater.getSections().size()) {
                    theater.getSections().get(index++).setId(rs.getLong(1));
                }
            }
        }
    }

    private void insertRows(Connection conn, Section section) throws SQLException {
        String sql = "INSERT INTO rows (section_id, number) VALUES (?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Row row : section.getRows()) {
                stmt.setLong(1, section.getId());
                stmt.setInt(2, row.getNumber());
                stmt.addBatch();
            }

            stmt.executeBatch();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                int index = 0;
                while (rs.next() && index < section.getRows().size()) {
                    section.getRows().get(index++).setId(rs.getLong(1));
                }
            }
        }
    }

    private void insertSeats(Connection conn, Row row) throws SQLException {
        String sql = "INSERT INTO seats (row_id, number, status) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Seat seat : row.getSeats()) {
                stmt.setLong(1, row.getId());
                stmt.setInt(2, seat.getNumber());
                stmt.setString(3, seat.getStatus().name());
                stmt.addBatch();
            }

            stmt.executeBatch();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                int index = 0;
                while (rs.next() && index < row.getSeats().size()) {
                    row.getSeats().get(index++).setId(rs.getLong(1));
                }
            }
        }
    }

    public boolean updateSeatStatus(Long seatId, SeatStatus status) throws SQLException {
        String sql = "UPDATE seats SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setLong(2, seatId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean bookSeat(Long seatId) throws SQLException {
        String sql = "UPDATE seats SET status = 'BOOKED', updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ? AND status = 'AVAILABLE'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, seatId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean cancelBooking(Long seatId) throws SQLException {
        String sql = "UPDATE seats SET status = 'AVAILABLE', updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ? AND status = 'BOOKED'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, seatId);
            return stmt.executeUpdate() > 0;
        }
    }
}
