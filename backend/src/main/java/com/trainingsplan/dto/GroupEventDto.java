package com.trainingsplan.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record GroupEventDto(
        Long id,
        String title,
        String description,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String locationName,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Integer maxParticipants,
        int currentParticipants,
        Integer costCents,
        String costCurrency,
        String difficulty,
        String status,
        String trainerUsername,
        Long trainerId,
        LocalDateTime createdAt,
        boolean isRegistered
) {}
