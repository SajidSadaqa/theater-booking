package org.example.model;

public enum SeatStatus {
    AVAILABLE("Available"),
    BOOKED("Booked"),
    RESERVED("Reserved"),
    OUT_OF_ORDER("Out of Order");

    private final String displayName;

    SeatStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
