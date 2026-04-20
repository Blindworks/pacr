package com.trainingsplan.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainingPlanWeekExportDto {
    public Integer weekNumber;
    public Map<String, TrainingExportDto> schedule = new LinkedHashMap<>();
}
