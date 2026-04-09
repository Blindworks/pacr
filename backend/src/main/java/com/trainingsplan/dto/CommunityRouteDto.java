package com.trainingsplan.dto;

import java.time.LocalDateTime;

public record CommunityRouteDto(
        Long id,
        String name,
        Double distanceKm,
        Integer elevationGainM,
        Double startLatitude,
        Double startLongitude,
        String creatorUsername,
        Long creatorId,
        int athleteCount,
        Integer recordTimeSeconds,
        String recordHolder,
        String visibility,
        LocalDateTime createdAt,
        String locationCity,
        boolean adminUploaded,
        double[][] previewTrack
) {}
