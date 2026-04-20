package com.trainingsplan.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trainingsplan.entity.TrainingStep;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainingStepExportDto {
    public String stepType;
    public String title;
    public String subtitle;
    public Integer durationMinutes;
    public Integer durationSeconds;
    public Integer distanceMeters;
    public String paceDisplay;
    public String icon;
    public Boolean highlight;
    public Boolean muted;

    public static TrainingStepExportDto from(TrainingStep s) {
        TrainingStepExportDto dto = new TrainingStepExportDto();
        dto.stepType = s.getStepType();
        dto.title = s.getTitle();
        dto.subtitle = s.getSubtitle();
        dto.durationMinutes = s.getDurationMinutes();
        dto.durationSeconds = s.getDurationSeconds();
        dto.distanceMeters = s.getDistanceMeters();
        dto.paceDisplay = s.getPaceDisplay();
        dto.icon = s.getIcon();
        dto.highlight = s.getHighlight();
        dto.muted = s.getMuted();
        return dto;
    }
}
