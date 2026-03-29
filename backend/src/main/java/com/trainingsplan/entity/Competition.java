package com.trainingsplan.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@Table(name = "competitions")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 255)
    private CompetitionType type;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "system_generated", nullable = false)
    private boolean systemGenerated = false;

    public Competition() {}

    public Competition(String name, LocalDate date, String description) {
        this.name = name;
        this.date = date;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CompetitionType getType() { return type; }
    public void setType(CompetitionType type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isSystemGenerated() { return systemGenerated; }
    public void setSystemGenerated(boolean systemGenerated) { this.systemGenerated = systemGenerated; }
}
