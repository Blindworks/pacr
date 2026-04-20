package com.trainingsplan.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainingPlanBodyExportDto {
    public String name;
    public String description;
    public String targetTime;
    public String prerequisites;
    public String competitionType;
    public List<TrainingPlanWeekExportDto> weeks;
}
