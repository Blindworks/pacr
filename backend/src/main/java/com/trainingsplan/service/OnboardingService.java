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

    public record OnboardingPlanSetupRequest(Long planId, String startDate) {}

    @Transactional
    public CompetitionDto setupPlan(OnboardingPlanSetupRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Not authenticated");
        }

        TrainingPlan plan = trainingPlanRepository.findById(request.planId())
                .orElseThrow(() -> new IllegalArgumentException("Training plan not found: " + request.planId()));

        Integer maxWeek = trainingRepository.findMaxWeekNumberByPlanId(plan.getId());
        if (maxWeek == null) {
            maxWeek = 12;
        }

        LocalDate startDate = LocalDate.parse(request.startDate());
        LocalDate raceDate = startDate.plusWeeks(maxWeek)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        Competition competition = new Competition();
        competition.setName(buildCompetitionName(user, plan));
        competition.setDate(raceDate);
        competition.setDescription("Auto-generated during onboarding");
        if (plan.getCompetitionType() != null) {
            competition.setType(plan.getCompetitionType());
        }
        competition = competitionRepository.save(competition);

        CompetitionRegistration registration = new CompetitionRegistration();
        registration.setCompetition(competition);
        registration.setUser(user);
        registration.setTrainingPlan(plan);
        registration.setRegisteredAt(LocalDateTime.now());
        registration = registrationRepository.save(registration);

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
