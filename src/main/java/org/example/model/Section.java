package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class Section {
    private Long id;
    private String name;
    private Theater theater;
    private List<Row> rows;

    public Section() {
        this.rows = new ArrayList<>();
    }

    public Section(String name) {
        this();
        this.name = name;
    }

    public Section(Long id, String name) {
        this(name);
        this.id = id;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Theater getTheater() { return theater; }
    public void setTheater(Theater theater) { this.theater = theater; }

    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) {
        this.rows = rows;
        rows.forEach(row -> row.setSection(this));
    }

    public void addRow(Row row) {
        this.rows.add(row);
        row.setSection(this);
    }

    public int getTotalSeats() {
        return rows.stream()
                .mapToInt(Row::getTotalSeats)
                .sum();
    }

    public int getAvailableSeats() {
        return rows.stream()
                .mapToInt(Row::getAvailableSeats)
                .sum();
    }

    @Override
    public String toString() {
        return String.format("Section{id=%d, name='%s', rows=%d}", id, name, rows.size());
    }
}
