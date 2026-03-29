package com.trainingsplan.service;

import com.trainingsplan.dto.CompetitionDto;
import com.trainingsplan.entity.*;
import com.trainingsplan.repository.CompetitionRegistrationRepository;
import com.trainingsplan.repository.CompetitionRepository;
import com.trainingsplan.repository.TrainingPlanRepository;
import com.trainingsplan.repository.TrainingRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
public class OnboardingService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final TrainingRepository trainingRepository;
    private final CompetitionRepository competitionRepository;
    private final CompetitionRegistrationRepository registrationRepository;
    private final UserTrainingScheduleService scheduleService;
    private final SecurityUtils securityUtils;

    public OnboardingService(TrainingPlanRepository trainingPlanRepository,
                             TrainingRepository trainingRepository,
                             CompetitionRepository competitionRepository,
                             CompetitionRegistrationRepository registrationRepository,
                             UserTrainingScheduleService scheduleService,
                             SecurityUtils securityUtils) {
        this.trainingPlanRepository = trainingPlanRepository;
        this.trainingRepository = trainingRepository;
        this.competitionRepository = competitionRepository;
        this.registrationRepository = registrationRepository;
        this.scheduleService = scheduleService;
        this.securityUtils = securityUtils;
    }

    public record OnboardingPlanSetupRequest(Long planId, String startDate, Long competitionId) {}

    @Transactional
    public CompetitionDto setupPlan(OnboardingPlanSetupRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Not authenticated");
        }

        TrainingPlan plan = trainingPlanRepository.findById(request.planId())
                .orElseThrow(() -> new IllegalArgumentException("Training plan not found: " + request.planId()));

        Competition competition;
        CompetitionRegistration registration;

        if (request.competitionId() != null) {
            // User selected an existing competition — build schedule backwards from its date
            final Competition existingCompetition = competitionRepository.findById(request.competitionId())
                    .orElseThrow(() -> new IllegalArgumentException("Competition not found: " + request.competitionId()));
            competition = existingCompetition;

            registration = registrationRepository
                    .findByCompetitionIdAndUserId(existingCompetition.getId(), user.getId())
                    .orElseGet(() -> {
                        CompetitionRegistration r = new CompetitionRegistration();
                        r.setCompetition(existingCompetition);
                        r.setUser(user);
                        r.setRegisteredAt(LocalDateTime.now());
                        return r;
                    });
            registration.setTrainingPlan(plan);
            registration = registrationRepository.save(registration);
        } else {
            // No competition selected — auto-create one from start date
            Integer maxWeek = trainingRepository.findMaxWeekNumberByPlanId(plan.getId());
            if (maxWeek == null) {
                maxWeek = 12;
            }

            LocalDate startDate = LocalDate.parse(request.startDate());
            LocalDate raceDate = startDate.plusWeeks(maxWeek - 1)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            competition = new Competition();
            competition.setName(buildCompetitionName(user, plan));
            competition.setDate(raceDate);
            competition.setDescription("Auto-generated during onboarding");
            competition.setSystemGenerated(true);
            if (plan.getCompetitionType() != null) {
                competition.setType(plan.getCompetitionType());
            }
            competition = competitionRepository.save(competition);

            registration = new CompetitionRegistration();
            registration.setCompetition(competition);
            registration.setUser(user);
            registration.setTrainingPlan(plan);
            registration.setRegisteredAt(LocalDateTime.now());
            registration = registrationRepository.save(registration);
        }

        scheduleService.reassignPlan(registration);

        return new CompetitionDto(competition, registration);
    }

    private String buildCompetitionName(User user, TrainingPlan plan) {
        String targetDistance = user.getTargetDistance();
        if (targetDistance != null) {
            return switch (targetDistance) {
                case "5K" -> "5K Training Goal";
                case "10K" -> "10K Training Goal";
                case "HALF_MARATHON" -> "Half Marathon Training Goal";
                case "MARATHON" -> "Marathon Training Goal";
                default -> plan.getName() + " Goal";
            };
        }
        return plan.getName() + " Goal";
    }
}
