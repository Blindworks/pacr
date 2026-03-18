package com.trainingsplan.dto;

public class DailyCoachRecommendationRequest {

    private String date; // YYYY-MM-DD
    private Integer feelingScore; // 1-5
    private String feelingText;

    public DailyCoachRecommendationRequest() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Integer getFeelingScore() { return feelingScore; }
    public void setFeelingScore(Integer feelingScore) { this.feelingScore = feelingScore; }

    public String getFeelingText() { return feelingText; }
    public void setFeelingText(String feelingText) { this.feelingText = feelingText; }
}
