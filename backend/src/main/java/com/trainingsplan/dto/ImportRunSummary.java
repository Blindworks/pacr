package com.trainingsplan.dto;

import java.util.List;

/**
 * Result of one run of the external news importer.
 *
 * @param sourcesProcessed total number of sources attempted in this run
 * @param newItems         total number of new news items persisted across all sources
 * @param results          per-source outcome
 */
public record ImportRunSummary(
        int sourcesProcessed,
        int newItems,
        List<SourceResult> results
) {
    public record SourceResult(
            Long sourceId,
            String sourceName,
            int newItems,
            int skippedDuplicates,
            boolean success,
            String message
    ) {}
}
