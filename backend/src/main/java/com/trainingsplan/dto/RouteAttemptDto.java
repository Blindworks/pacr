package com.trainingsplan.dto;

import java.time.LocalDateTime;

public record RouteAttemptDto(
        Long id,
        Long routeId,
        String routeName,
        String status,
        Integer timeSeconds,
        Integer paceSecondsPerKm,
        LocalDateTime completedAt,
        Integer leaderboardPosition
) {}
