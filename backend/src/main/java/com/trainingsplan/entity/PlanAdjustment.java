package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_adjustments")
public class PlanAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_training_entry_id")
    @JsonIgnore
    private UserTrainingEntry userTrainingEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 20)
    private AdjustmentType adjustmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AdjustmentStatus status = AdjustmentStatus.PENDING;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "trigger_source", length = 30)
    private String triggerSource;

    @Column(name = "original_date")
    private LocalDate originalDate;

    @Column(name = "new_date")
    private LocalDate newDate;

    @Column(name = "original_intensity", length = 20)
    private String originalIntensity;

    @Column(name = "new_intensity", length = 20)
    private String newIntensity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public PlanAdjustment() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public UserTrainingEntry getUserTrainingEntry() { return userTrainingEntry; }
    public void setUserTrainingEntry(UserTrainingEntry userTrainingEntry) { this.userTrainingEntry = userTrainingEntry; }

    public AdjustmentType getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(AdjustmentType adjustmentType) { this.adjustmentType = adjustmentType; }

    public AdjustmentStatus getStatus() { return status; }
    public void setStatus(AdjustmentStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }

    public LocalDate getOriginalDate() { return originalDate; }
    public void setOriginalDate(LocalDate originalDate) { this.originalDate = originalDate; }

    public LocalDate getNewDate() { return newDate; }
    public void setNewDate(LocalDate newDate) { this.newDate = newDate; }

    public String getOriginalIntensity() { return originalIntensity; }
    public void setOriginalIntensity(String originalIntensity) { this.originalIntensity = originalIntensity; }

    public String getNewIntensity() { return newIntensity; }
    public void setNewIntensity(String newIntensity) { this.newIntensity = newIntensity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public Long getUserTrainingEntryId() {
        return userTrainingEntry != null ? userTrainingEntry.getId() : null;
    }

    public String getTrainingName() {
        return userTrainingEntry != null && userTrainingEntry.getTraining() != null
                ? userTrainingEntry.getTraining().getName() : null;
    }
}
