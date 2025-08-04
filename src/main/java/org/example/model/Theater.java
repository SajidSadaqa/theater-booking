package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class Theater {
    private Long id;
    private String name;
    private List<Section> sections;

    public Theater() {
        this.sections = new ArrayList<>();
    }

    public Theater(String name) {
        this();
        this.name = name;
    }

    public Theater(Long id, String name) {
        this(name);
        this.id = id;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) { this.sections = sections; }

    public void addSection(Section section) {
        this.sections.add(section);
        section.setTheater(this);
    }

    public int getTotalSeats() {
        return sections.stream()
                .mapToInt(Section::getTotalSeats)
                .sum();
    }

    public int getAvailableSeats() {
        return sections.stream()
                .mapToInt(Section::getAvailableSeats)
                .sum();
    }

    @Override
    public String toString() {
        return String.format("Theater{id=%d, name='%s', sections=%d, totalSeats=%d}",
                id, name, sections.size(), getTotalSeats());
    }
}
