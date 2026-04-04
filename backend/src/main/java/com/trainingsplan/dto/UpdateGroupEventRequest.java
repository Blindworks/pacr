package com.trainingsplan.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateGroupEventRequest(
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
        Integer costCents,
        String costCurrency,
        String difficulty,
        String rrule,
        LocalDate recurrenceEndDate
) {}
