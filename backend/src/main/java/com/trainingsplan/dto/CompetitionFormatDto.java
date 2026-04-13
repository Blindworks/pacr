package com.trainingsplan.dto;

import com.trainingsplan.entity.CompetitionFormat;
import com.trainingsplan.entity.CompetitionType;

import java.time.LocalDate;
import java.time.LocalTime;

public class CompetitionFormatDto {
    private Long id;
    private CompetitionType type;
    private LocalTime startTime;
    private LocalDate startDate;
    private String description;

    public CompetitionFormatDto() {}

    public CompetitionFormatDto(CompetitionFormat format) {
        this.id = format.getId();
        this.type = format.getType();
        this.startTime = format.getStartTime();
        this.startDate = format.getStartDate();
        this.description = format.getDescription();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CompetitionType getType() { return type; }
    public void setType(CompetitionType type) { this.type = type; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
