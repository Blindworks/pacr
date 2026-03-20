package com.trainingsplan.dto;

public class CreatePersonalRecordRequest {

    private Double distanceKm;
    private String distanceLabel;
    private Integer goalTimeSeconds;

    public CreatePersonalRecordRequest() {}

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public String getDistanceLabel() { return distanceLabel; }
    public void setDistanceLabel(String distanceLabel) { this.distanceLabel = distanceLabel; }

    public Integer getGoalTimeSeconds() { return goalTimeSeconds; }
    public void setGoalTimeSeconds(Integer goalTimeSeconds) { this.goalTimeSeconds = goalTimeSeconds; }
}
