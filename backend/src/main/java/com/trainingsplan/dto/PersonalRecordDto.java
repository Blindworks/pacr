package com.trainingsplan.dto;

import java.time.LocalDate;

public class PersonalRecordDto {

    private Long id;
    private Double distanceKm;
    private String distanceLabel;
    private Integer bestTimeSeconds;
    private Integer goalTimeSeconds;
    private LocalDate achievedDate;
    private Boolean isManual;
    private Long activityId;

    public PersonalRecordDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public String getDistanceLabel() { return distanceLabel; }
    public void setDistanceLabel(String distanceLabel) { this.distanceLabel = distanceLabel; }

    public Integer getBestTimeSeconds() { return bestTimeSeconds; }
    public void setBestTimeSeconds(Integer bestTimeSeconds) { this.bestTimeSeconds = bestTimeSeconds; }

    public Integer getGoalTimeSeconds() { return goalTimeSeconds; }
    public void setGoalTimeSeconds(Integer goalTimeSeconds) { this.goalTimeSeconds = goalTimeSeconds; }

    public LocalDate getAchievedDate() { return achievedDate; }
    public void setAchievedDate(LocalDate achievedDate) { this.achievedDate = achievedDate; }

    public Boolean getIsManual() { return isManual; }
    public void setIsManual(Boolean isManual) { this.isManual = isManual; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
}
