package com.trainingsplan.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trainingsplan.entity.TrainingStepBlock;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainingStepBlockExportDto {
    public Integer repeatCount;
    public String label;
    public List<TrainingStepExportDto> steps;

    public static TrainingStepBlockExportDto from(TrainingStepBlock b) {
        TrainingStepBlockExportDto dto = new TrainingStepBlockExportDto();
        dto.repeatCount = b.getRepeatCount();
        dto.label = b.getLabel();
        dto.steps = b.getSteps().stream().map(TrainingStepExportDto::from).toList();
        return dto;
    }
}
