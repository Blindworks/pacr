package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "competition_formats",
       uniqueConstraints = @UniqueConstraint(columnNames = {"competition_id", "type"}))
public class CompetitionFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    @JsonIgnore
    private Competition competition;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CompetitionType type;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(length = 1000)
    private String description;

    public CompetitionFormat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Competition getCompetition() { return competition; }
    public void setCompetition(Competition competition) { this.competition = competition; }

    public CompetitionType getType() { return type; }
    public void setType(CompetitionType type) { this.type = type; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
