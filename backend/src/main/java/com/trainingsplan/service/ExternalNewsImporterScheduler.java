package com.trainingsplan.service;

import com.trainingsplan.dto.ImportRunSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the external news importer twice a day (09:00 and 17:00 server time).
 * {@code @EnableScheduling} is already active via {@code SmartTrainingsplanApplication}.
 */
@Component
public class ExternalNewsImporterScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExternalNewsImporterScheduler.class);

    private final ExternalNewsImporterService importer;

    public ExternalNewsImporterScheduler(ExternalNewsImporterService importer) {
        this.importer = importer;
    }

    /** Cron: 09:00 and 17:00 every day (seconds minutes hours day-of-month month day-of-week). */
    @Scheduled(cron = "0 0 9,17 * * *")
    public void runTwiceDaily() {
        log.info("ExternalNewsImporterScheduler: starting scheduled import run");
        try {
            ImportRunSummary summary = importer.importAllEnabled();
            log.info("ExternalNewsImporterScheduler: imported {} new item(s) from {} source(s)",
                    summary.newItems(), summary.sourcesProcessed());
        } catch (Exception e) {
            log.error("ExternalNewsImporterScheduler: unexpected error during scheduled run", e);
        }
    }
}
