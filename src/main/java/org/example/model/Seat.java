package org.example.model;

public class Seat {
    private Long id;
    private int number;
    private Row row;
    private SeatStatus status;

    public Seat() {
        this.status = SeatStatus.AVAILABLE;
    }

    public Seat(int number) {
        this();
        this.number = number;
    }

    public Seat(Long id, int number, SeatStatus status) {
        this(number);
        this.id = id;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public Row getRow() { return row; }
    public void setRow(Row row) { this.row = row; }

    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public boolean isBooked() {
        return status == SeatStatus.BOOKED;
    }

    @Override
    public String toString() {
        return String.format("Seat{id=%d, number=%d, status=%s}", id, number, status);
    }
}