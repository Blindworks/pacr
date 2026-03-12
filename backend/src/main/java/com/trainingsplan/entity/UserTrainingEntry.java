package com.trainingsplan.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@Entity
@Table(name = "user_training_entries")
public class UserTrainingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_registration_id", nullable = false)
    @JsonIgnore
    private CompetitionRegistration competitionRegistration;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "training_id", nullable = false)
    private Training training;

    @Column(name = "training_date", nullable = false)
    private LocalDate trainingDate;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "completion_status")
    private String completionStatus;

    public UserTrainingEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CompetitionRegistration getCompetitionRegistration() { return competitionRegistration; }
    public void setCompetitionRegistration(CompetitionRegistration competitionRegistration) {
        this.competitionRegistration = competitionRegistration;
    }

    public Training getTraining() { return training; }
    public void setTraining(Training training) { this.training = training; }

    public LocalDate getTrainingDate() { return trainingDate; }
    public void setTrainingDate(LocalDate trainingDate) { this.trainingDate = trainingDate; }

    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public String getCompletionStatus() { return completionStatus; }
    public void setCompletionStatus(String completionStatus) { this.completionStatus = completionStatus; }

    @JsonProperty("registrationId")
    public Long getRegistrationId() {
        return competitionRegistration != null ? competitionRegistration.getId() : null;
    }

    @JsonProperty("competitionId")
    public Long getCompetitionId() {
        return competitionRegistration != null && competitionRegistration.getCompetition() != null
                ? competitionRegistration.getCompetition().getId() : null;
    }

    @JsonProperty("competitionName")
    public String getCompetitionName() {
        return competitionRegistration != null && competitionRegistration.getCompetition() != null
                ? competitionRegistration.getCompetition().getName() : null;
    }
}
