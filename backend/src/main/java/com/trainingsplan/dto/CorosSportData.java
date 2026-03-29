package com.trainingsplan.dto;

import java.util.List;

public class CorosSportData {
    private String openId;
    private String labelId;
    private Integer mode;
    private Integer subMode;
    private String deviceName;
    private Double distance;
    private Double calorie;
    private Integer avgSpeed;
    private Integer avgFrequency;
    private Integer step;
    private Long startTime;
    private Long endTime;
    private Integer startTimezone;
    private Integer endTimezone;
    private String fitUrl;
    private Long planWorkoutId;
    private List<CorosTriathlonItem> triathlonItemList;

    public String getOpenId() { return openId; }
    public void setOpenId(String openId) { this.openId = openId; }
    public String getLabelId() { return labelId; }
    public void setLabelId(String labelId) { this.labelId = labelId; }
    public Integer getMode() { return mode; }
    public void setMode(Integer mode) { this.mode = mode; }
    public Integer getSubMode() { return subMode; }
    public void setSubMode(Integer subMode) { this.subMode = subMode; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
    public Double getCalorie() { return calorie; }
    public void setCalorie(Double calorie) { this.calorie = calorie; }
    public Integer getAvgSpeed() { return avgSpeed; }
    public void setAvgSpeed(Integer avgSpeed) { this.avgSpeed = avgSpeed; }
    public Integer getAvgFrequency() { return avgFrequency; }
    public void setAvgFrequency(Integer avgFrequency) { this.avgFrequency = avgFrequency; }
    public Integer getStep() { return step; }
    public void setStep(Integer step) { this.step = step; }
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public Integer getStartTimezone() { return startTimezone; }
    public void setStartTimezone(Integer startTimezone) { this.startTimezone = startTimezone; }
    public Integer getEndTimezone() { return endTimezone; }
    public void setEndTimezone(Integer endTimezone) { this.endTimezone = endTimezone; }
    public String getFitUrl() { return fitUrl; }
    public void setFitUrl(String fitUrl) { this.fitUrl = fitUrl; }
    public Long getPlanWorkoutId() { return planWorkoutId; }
    public void setPlanWorkoutId(Long planWorkoutId) { this.planWorkoutId = planWorkoutId; }
    public List<CorosTriathlonItem> getTriathlonItemList() { return triathlonItemList; }
    public void setTriathlonItemList(List<CorosTriathlonItem> triathlonItemList) { this.triathlonItemList = triathlonItemList; }
}
