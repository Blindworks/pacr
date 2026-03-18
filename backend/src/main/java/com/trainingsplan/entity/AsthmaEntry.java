package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asthma_entries")
public class AsthmaEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @Column(name = "symptoms", length = 255)
    private String symptoms;

    @Column(name = "severity_score")
    private Integer severityScore;

    @Column(name = "peak_flow_l_min")
    private Integer peakFlowLMin;

    @Column(name = "inhaler_usage", length = 20)
    private String inhalerUsage = "NONE";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public AsthmaEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public Integer getSeverityScore() { return severityScore; }
    public void setSeverityScore(Integer severityScore) { this.severityScore = severityScore; }

    public Integer getPeakFlowLMin() { return peakFlowLMin; }
    public void setPeakFlowLMin(Integer peakFlowLMin) { this.peakFlowLMin = peakFlowLMin; }

    public String getInhalerUsage() { return inhalerUsage; }
    public void setInhalerUsage(String inhalerUsage) { this.inhalerUsage = inhalerUsage; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
