package com.trainingsplan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ActivityFeedbackDto {

    @Min(1)
    @Max(10)
    private Integer rpe;

    @Min(1)
    @Max(5)
    private Integer feeling;

    @Min(1)
    @Max(5)
    private Integer trainingQuality;

    @Size(max = 1000)
    private String feedbackNote;

    public Integer getRpe() {
        return rpe;
    }

    public void setRpe(Integer rpe) {
        this.rpe = rpe;
    }

    public Integer getFeeling() {
        return feeling;
    }

    public void setFeeling(Integer feeling) {
        this.feeling = feeling;
    }

    public Integer getTrainingQuality() {
        return trainingQuality;
    }

    public void setTrainingQuality(Integer trainingQuality) {
        this.trainingQuality = trainingQuality;
    }

    public String getFeedbackNote() {
        return feedbackNote;
    }

    public void setFeedbackNote(String feedbackNote) {
        this.feedbackNote = feedbackNote;
    }
}
