package com.trainingsplan.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trainingsplan.entity.Training;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainingExportDto {
    public String name;
    public String description;
    public String trainingType;
    public String intensityLevel;
    public Integer intensityPercent;
    public Integer durationMinutes;
    public Integer estimatedDistanceMeters;
    public Integer estimatedCalories;
    public String benefit;
    public String difficulty;
    public String heroImageUrl;
    public String workPace;
    public Integer workTimeSeconds;
    public Integer workDistanceMeters;
    public String recoveryPace;
    public Integer recoveryTimeSeconds;
    public Integer recoveryDistanceMeters;
    public List<TrainingStepExportDto> steps;
    public List<TrainingStepBlockExportDto> blocks;
    public List<TrainingPrepTipExportDto> prepTips;

    public static TrainingExportDto from(Training t) {
        TrainingExportDto dto = new TrainingExportDto();
        dto.name = t.getName();
        dto.description = t.getDescription();
        dto.trainingType = t.getTrainingType();
        dto.intensityLevel = t.getIntensityLevel();
        dto.intensityPercent = t.getIntensityScore();
        dto.durationMinutes = t.getDurationMinutes();
        dto.estimatedDistanceMeters = t.getEstimatedDistanceMeters();
        dto.estimatedCalories = t.getEstimatedCalories();
        dto.benefit = t.getBenefit();
        dto.difficulty = t.getDifficulty();
        dto.heroImageUrl = t.getHeroImageUrl();
        dto.workPace = t.getWorkPace();
        dto.workTimeSeconds = t.getWorkTimeSeconds();
        dto.workDistanceMeters = t.getWorkDistanceMeters();
        dto.recoveryPace = t.getRecoveryPace();
        dto.recoveryTimeSeconds = t.getRecoveryTimeSeconds();
        dto.recoveryDistanceMeters = t.getRecoveryDistanceMeters();
        if (!t.getSteps().isEmpty()) {
            dto.steps = t.getSteps().stream().map(TrainingStepExportDto::from).toList();
        }
        if (!t.getBlocks().isEmpty()) {
            dto.blocks = t.getBlocks().stream().map(TrainingStepBlockExportDto::from).toList();
        }
        if (!t.getPrepTips().isEmpty()) {
            dto.prepTips = t.getPrepTips().stream().map(TrainingPrepTipExportDto::from).toList();
        }
        return dto;
    }
}
