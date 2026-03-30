package com.trainingsplan.dto;

public record CreateCommunityRouteRequest(
        Long activityId,
        String name,
        String visibility
) {}
