package com.trainingsplan.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_record_entries")
public class PersonalRecordEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_record_id", nullable = false)
    private PersonalRecord personalRecord;

    @Column(name = "time_seconds", nullable = false)
    private Integer timeSeconds;

    @Column(name = "achieved_date", nullable = false)
    private LocalDate achievedDate;

    @Column(name = "is_manual", nullable = false)
    private boolean isManual = false;

    @Column(name = "completed_training_id")
    private Long completedTrainingId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PersonalRecordEntry() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PersonalRecord getPersonalRecord() { return personalRecord; }
    public void setPersonalRecord(PersonalRecord personalRecord) { this.personalRecord = personalRecord; }

    public Integer getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(Integer timeSeconds) { this.timeSeconds = timeSeconds; }

    public LocalDate getAchievedDate() { return achievedDate; }
    public void setAchievedDate(LocalDate achievedDate) { this.achievedDate = achievedDate; }

    public boolean isManual() { return isManual; }
    public void setManual(boolean manual) { isManual = manual; }

    public Long getCompletedTrainingId() { return completedTrainingId; }
    public void setCompletedTrainingId(Long completedTrainingId) { this.completedTrainingId = completedTrainingId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
