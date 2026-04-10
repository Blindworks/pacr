package com.trainingsplan.dto;

import java.time.LocalDateTime;

public record LeaderboardEntryDto(
        int rank,
        String username,
        Long userId,
        int timeSeconds,
        Integer paceSecondsPerKm,
        LocalDateTime date
) {}
