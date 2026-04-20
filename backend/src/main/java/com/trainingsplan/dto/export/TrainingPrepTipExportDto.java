package com.trainingsplan.dto.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trainingsplan.entity.TrainingPrepTip;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrainingPrepTipExportDto {
    public String icon;
    public String title;
    public String text;

    public static TrainingPrepTipExportDto from(TrainingPrepTip p) {
        TrainingPrepTipExportDto dto = new TrainingPrepTipExportDto();
        dto.icon = p.getIcon();
        dto.title = p.getTitle();
        dto.text = p.getText();
        return dto;
    }
}
