package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class Row {
    private Long id;
    private int number;
    private Section section;
    private List<Seat> seats;

    public Row() {
        this.seats = new ArrayList<>();
    }

    public Row(int number) {
        this();
        this.number = number;
    }

    public Row(Long id, int number) {
        this(number);
        this.id = id;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public Section getSection() { return section; }
    public void setSection(Section section) { this.section = section; }

    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) {
        this.seats = seats;
        seats.forEach(seat -> seat.setRow(this));
    }

    public void addSeat(Seat seat) {
        this.seats.add(seat);
        seat.setRow(this);
    }

    public int getTotalSeats() {
        return seats.size();
    }

    public int getAvailableSeats() {
        return (int) seats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                .count();
    }

    @Override
    public String toString() {
        return String.format("Row{id=%d, number=%d, seats=%d}", id, number, seats.size());
    }
}