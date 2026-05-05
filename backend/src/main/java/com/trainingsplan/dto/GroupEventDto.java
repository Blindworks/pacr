package com.trainingsplan.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
        Integer paceMinSecondsPerKm,
        Integer paceMaxSecondsPerKm,
        Integer maxParticipants,
        int currentParticipants,
        Integer costCents,
        String costCurrency,
        String difficulty,
        String status,
        String trainerUsername,
        Long trainerId,
        LocalDateTime createdAt,
        boolean isRegistered,
        String rrule,
        LocalDate recurrenceEndDate,
        LocalDate occurrenceDate,
        boolean isRecurring,
        String eventImageFilename,
        List<GroupEventParticipantPreviewDto> participantPreview
) {}
