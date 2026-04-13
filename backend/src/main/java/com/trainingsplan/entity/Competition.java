package com.trainingsplan.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "system_generated", nullable = false)
    private boolean systemGenerated = false;

    @OneToMany(mappedBy = "competition", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CompetitionFormat> formats = new ArrayList<>();

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

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public boolean isSystemGenerated() { return systemGenerated; }
    public void setSystemGenerated(boolean systemGenerated) { this.systemGenerated = systemGenerated; }

    public List<CompetitionFormat> getFormats() { return formats; }
    public void setFormats(List<CompetitionFormat> formats) { this.formats = formats; }
}
