package com.trainingsplan.dto.ladv;

import com.trainingsplan.entity.LadvStagedEvent;
import com.trainingsplan.entity.LadvStagedEventStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record LadvStagedEventDto(
        Long id,
        Long sourceId,
        String sourceName,
        Long ladvId,
        String veranstaltungsnummer,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        String kategorie,
        String veranstalter,
        String homepage,
        String ort,
        String plz,
        Double latitude,
        Double longitude,
        boolean abgesagt,
        List<DistanceDto> distances,
        LadvStagedEventStatus status,
        Long importedCompetitionId,
        LocalDateTime importedAt,
        LocalDateTime fetchedAt
) {
    public record DistanceDto(String name, Integer meters) {}
}
