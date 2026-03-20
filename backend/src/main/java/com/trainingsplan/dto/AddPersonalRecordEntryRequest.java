package com.trainingsplan.dto;

import java.time.LocalDate;

public class AddPersonalRecordEntryRequest {

    private Integer timeSeconds;
    private LocalDate achievedDate;

    public AddPersonalRecordEntryRequest() {}

    public Integer getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(Integer timeSeconds) { this.timeSeconds = timeSeconds; }

    public LocalDate getAchievedDate() { return achievedDate; }
    public void setAchievedDate(LocalDate achievedDate) { this.achievedDate = achievedDate; }
}
