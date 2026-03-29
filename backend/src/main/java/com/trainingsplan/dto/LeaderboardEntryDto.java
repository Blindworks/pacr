package com.trainingsplan.dto;

import java.time.LocalDate;

public record LeaderboardEntryDto(
        int rank,
        String username,
        Long userId,
        int timeSeconds,
        Integer paceSecondsPerKm,
        LocalDate date
) {}
