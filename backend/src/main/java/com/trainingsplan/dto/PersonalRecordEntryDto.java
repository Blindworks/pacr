package com.trainingsplan.dto;

import java.time.LocalDate;

public class PersonalRecordEntryDto {

    private Long id;
    private Integer timeSeconds;
    private LocalDate achievedDate;
    private Boolean isManual;
    private Long activityId;

    public PersonalRecordEntryDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(Integer timeSeconds) { this.timeSeconds = timeSeconds; }

    public LocalDate getAchievedDate() { return achievedDate; }
    public void setAchievedDate(LocalDate achievedDate) { this.achievedDate = achievedDate; }

    public Boolean getIsManual() { return isManual; }
    public void setIsManual(Boolean isManual) { this.isManual = isManual; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
}
