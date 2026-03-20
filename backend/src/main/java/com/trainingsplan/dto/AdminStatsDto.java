package com.trainingsplan.dto;

public record AdminStatsDto(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long blockedUsers,
        long pendingVerification,
        long newUsersThisWeek,
        long newUsersThisMonth,
        long stravaConnected,
        long asthmaTrackingEnabled,
        long cycleTrackingEnabled,
        long paceZonesConfigured
) {}
