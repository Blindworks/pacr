package com.trainingsplan.dto;

import com.trainingsplan.entity.CompletedTraining;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NewStravaActivityDto(
        Long id,
        String activityName,
        String sport,
        LocalDate trainingDate,
        LocalDateTime uploadDate,
        Double distanceKm,
        Integer durationSeconds,
        Integer movingTimeSeconds,
        Integer averageHeartRate,
        Integer maxHeartRate
) {
    public static NewStravaActivityDto from(CompletedTraining ct) {
        return new NewStravaActivityDto(
                ct.getId(),
                ct.getActivityName(),
                ct.getSport(),
                ct.getTrainingDate(),
                ct.getUploadDate(),
                ct.getDistanceKm(),
                ct.getDurationSeconds(),
                ct.getMovingTimeSeconds(),
                ct.getAverageHeartRate(),
                ct.getMaxHeartRate()
        );
    }
}
