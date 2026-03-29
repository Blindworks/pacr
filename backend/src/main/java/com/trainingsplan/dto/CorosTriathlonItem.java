package com.trainingsplan.dto;

public class CorosTriathlonItem {
    private Integer mode;
    private Integer subMode;
    private Double distance;
    private Double calorie;
    private Integer duration;
    private Integer step;
    private String fitUrl;

    public Integer getMode() { return mode; }
    public void setMode(Integer mode) { this.mode = mode; }
    public Integer getSubMode() { return subMode; }
    public void setSubMode(Integer subMode) { this.subMode = subMode; }
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
    public Double getCalorie() { return calorie; }
    public void setCalorie(Double calorie) { this.calorie = calorie; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getStep() { return step; }
    public void setStep(Integer step) { this.step = step; }
    public String getFitUrl() { return fitUrl; }
    public void setFitUrl(String fitUrl) { this.fitUrl = fitUrl; }
}
