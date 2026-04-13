package com.trainingsplan.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Entity
@Table(name = "competition_registrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"competition_id", "user_id"}))
public class CompetitionRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    @JsonIgnore
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id", nullable = true)
    @JsonIgnore
    private TrainingPlan trainingPlan;

    @Column(name = "ranking", length = 255)
    private String ranking;

    @Column(name = "target_time", length = 8)
    private String targetTime;

    @Column(name = "registered_with_organizer", nullable = false)
    private boolean registeredWithOrganizer = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_format_id", nullable = true)
    @JsonIgnore
    private CompetitionFormat competitionFormat;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    public CompetitionRegistration() {}

    public CompetitionRegistration(Competition competition, User user) {
        this.competition = competition;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Competition getCompetition() { return competition; }
    public void setCompetition(Competition competition) { this.competition = competition; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TrainingPlan getTrainingPlan() { return trainingPlan; }
    public void setTrainingPlan(TrainingPlan trainingPlan) { this.trainingPlan = trainingPlan; }

    public String getRanking() { return ranking; }
    public void setRanking(String ranking) { this.ranking = ranking; }

    public String getTargetTime() { return targetTime; }
    public void setTargetTime(String targetTime) { this.targetTime = targetTime; }

    public boolean isRegisteredWithOrganizer() { return registeredWithOrganizer; }
    public void setRegisteredWithOrganizer(boolean registeredWithOrganizer) { this.registeredWithOrganizer = registeredWithOrganizer; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    @JsonProperty("trainingPlanId")
    public Long getTrainingPlanId() {
        return trainingPlan != null ? trainingPlan.getId() : null;
    }

    @JsonProperty("trainingPlanName")
    public String getTrainingPlanName() {
        return trainingPlan != null ? trainingPlan.getName() : null;
    }

    public CompetitionFormat getCompetitionFormat() { return competitionFormat; }
    public void setCompetitionFormat(CompetitionFormat competitionFormat) { this.competitionFormat = competitionFormat; }

    @JsonProperty("competitionFormatId")
    public Long getCompetitionFormatId() {
        return competitionFormat != null ? competitionFormat.getId() : null;
    }
}
