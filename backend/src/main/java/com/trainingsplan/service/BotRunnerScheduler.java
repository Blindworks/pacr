package com.trainingsplan.service;

import com.trainingsplan.entity.BotProfile;
import com.trainingsplan.repository.BotProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ticks every minute and executes any bot whose {@code nextScheduledRunAt} is due.
 */
@Component
public class BotRunnerScheduler {

    private static final Logger log = LoggerFactory.getLogger(BotRunnerScheduler.class);

    private final BotProfileRepository botProfileRepository;
    private final BotRunnerService botRunnerService;

    public BotRunnerScheduler(BotProfileRepository botProfileRepository,
                              BotRunnerService botRunnerService) {
        this.botProfileRepository = botProfileRepository;
        this.botRunnerService = botRunnerService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void runDueBots() {
        LocalDateTime now = LocalDateTime.now();
        List<BotProfile> due = botProfileRepository
                .findByEnabledTrueAndNextScheduledRunAtLessThanEqual(now);
        if (due.isEmpty()) return;

        log.info("BotRunnerScheduler: executing {} due bot(s)", due.size());
        for (BotProfile bot : due) {
            try {
                botRunnerService.executeBot(bot);
            } catch (Exception e) {
                log.error("Failed to execute bot {}: {}", bot.getId(), e.getMessage(), e);
            }
        }
    }
}
