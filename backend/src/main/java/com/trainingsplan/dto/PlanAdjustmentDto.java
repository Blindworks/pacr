package com.trainingsplan.dto;

import com.trainingsplan.entity.AdjustmentStatus;
import com.trainingsplan.entity.AdjustmentType;
import com.trainingsplan.entity.PlanAdjustment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PlanAdjustmentDto {

    private Long id;
    private AdjustmentType adjustmentType;
    private AdjustmentStatus status;
    private String reason;
    private String triggerSource;
    private LocalDate originalDate;
    private LocalDate newDate;
    private String originalIntensity;
    private String newIntensity;
    private String trainingName;
    private Long userTrainingEntryId;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public PlanAdjustmentDto() {}

    public static PlanAdjustmentDto fromEntity(PlanAdjustment entity) {
        PlanAdjustmentDto dto = new PlanAdjustmentDto();
        dto.setId(entity.getId());
        dto.setAdjustmentType(entity.getAdjustmentType());
        dto.setStatus(entity.getStatus());
        dto.setReason(entity.getReason());
        dto.setTriggerSource(entity.getTriggerSource());
        dto.setOriginalDate(entity.getOriginalDate());
        dto.setNewDate(entity.getNewDate());
        dto.setOriginalIntensity(entity.getOriginalIntensity());
        dto.setNewIntensity(entity.getNewIntensity());
        dto.setTrainingName(entity.getTrainingName());
        dto.setUserTrainingEntryId(entity.getUserTrainingEntryId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setResolvedAt(entity.getResolvedAt());
        return dto;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getTrainingName() { return trainingName; }
    public void setTrainingName(String trainingName) { this.trainingName = trainingName; }

    public Long getUserTrainingEntryId() { return userTrainingEntryId; }
    public void setUserTrainingEntryId(Long userTrainingEntryId) { this.userTrainingEntryId = userTrainingEntryId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
