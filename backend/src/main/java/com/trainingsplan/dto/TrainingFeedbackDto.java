package com.trainingsplan.dto;

import jakarta.validation.constraints.NotNull;

public class TrainingFeedbackDto {
    @NotNull
    private Boolean completed;

    private String completionStatus;

    public TrainingFeedbackDto() {}

    public TrainingFeedbackDto(Boolean completed, String completionStatus) {
        this.completed = completed;
        this.completionStatus = completionStatus;
    }

    public Boolean isCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(String completionStatus) {
        this.completionStatus = completionStatus;
    }
}
