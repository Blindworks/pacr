package com.trainingsplan.dto.ladv;

public record LadvImportRunSummary(
        Long sourceId,
        String sourceName,
        int fetched,
        int newItems,
        int skipped,
        boolean success,
        String message
) {}
