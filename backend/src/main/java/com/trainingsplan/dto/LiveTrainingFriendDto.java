package com.trainingsplan.dto;

public record LiveTrainingFriendDto(
    Long friendId,
    String username,
    String displayName,
    String profileImageFilename,
    String trainingTitle,
    String trainingType,
    Integer durationMinutes,
    String workPace
) {}
