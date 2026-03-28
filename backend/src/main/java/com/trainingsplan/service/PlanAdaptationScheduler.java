package com.trainingsplan.service;

import com.trainingsplan.entity.CompetitionRegistration;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.CompetitionRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PlanAdaptationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlanAdaptationScheduler.class);

    private final PlanAdaptationService planAdaptationService;
    private final CompetitionRegistrationRepository registrationRepository;

    public PlanAdaptationScheduler(PlanAdaptationService planAdaptationService,
                                   CompetitionRegistrationRepository registrationRepository) {
        this.planAdaptationService = planAdaptationService;
        this.registrationRepository = registrationRepository;
    }

    /**
     * Weekly plan check: every Monday at 6 AM.
     * Evaluates the past week and creates adjustments for all users with active registrations.
     */
    @Scheduled(cron = "0 0 6 * * MON")
    public void weeklyPlanCheck() {
        log.info("Starting weekly plan adaptation check");

        List<User> activeUsers = registrationRepository.findAll().stream()
                .map(CompetitionRegistration::getUser)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a))
                .values().stream().toList();

        for (User user : activeUsers) {
            try {
                planAdaptationService.evaluateAndAdapt(user);
            } catch (Exception e) {
                log.error("Plan adaptation failed for user {}: {}", user.getId(), e.getMessage(), e);
            }
        }

        log.info("Weekly plan adaptation check completed for {} users", activeUsers.size());
    }

    /**
     * Trigger adaptation for a specific user (e.g., after activity import).
     */
    public void triggerForUser(User user) {
        try {
            planAdaptationService.evaluateAndAdapt(user);
        } catch (Exception e) {
            log.error("On-demand plan adaptation failed for user {}: {}", user.getId(), e.getMessage(), e);
        }
    }
}
