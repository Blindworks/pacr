package com.trainingsplan.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "personal_records")
public class PersonalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "distance_label", nullable = false)
    private String distanceLabel;

    @Column(name = "goal_time_seconds")
    private Integer goalTimeSeconds;

    @OneToMany(mappedBy = "personalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PersonalRecordEntry> entries = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PersonalRecord() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public String getDistanceLabel() { return distanceLabel; }
    public void setDistanceLabel(String distanceLabel) { this.distanceLabel = distanceLabel; }

    public Integer getGoalTimeSeconds() { return goalTimeSeconds; }
    public void setGoalTimeSeconds(Integer goalTimeSeconds) { this.goalTimeSeconds = goalTimeSeconds; }

    public List<PersonalRecordEntry> getEntries() { return entries; }
    public void setEntries(List<PersonalRecordEntry> entries) { this.entries = entries; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
