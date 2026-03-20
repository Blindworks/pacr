package com.trainingsplan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
public class TrainingReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrainingReminderScheduler.class);

    private final TrainingReminderService trainingReminderService;

    public TrainingReminderScheduler(TrainingReminderService trainingReminderService) {
        this.trainingReminderService = trainingReminderService;
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void runHourlyReminders() {
        String currentHour = String.format("%02d:00", LocalTime.now().getHour());
        log.debug("Running training reminders for time: {}", currentHour);
        trainingReminderService.sendRemindersForTime(currentHour);
    }
}
