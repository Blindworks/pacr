package com.trainingsplan.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainingPlanExportDto {
    @JsonProperty("format_version")
    public String formatVersion = "2.0";

    public TrainingPlanBodyExportDto plan;
}
