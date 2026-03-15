package com.trainingsplan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.DashboardDto;
import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.CompetitionRegistration;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.Recommendation;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserTrainingEntry;
import com.trainingsplan.entity.BodyMetric;
import com.trainingsplan.entity.MetricType;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.BodyMetricRepository;
import com.trainingsplan.repository.CompetitionRegistrationRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int DAYS = 28;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final DailyMetricsRepository dailyMetricsRepository;
    private final ActivityMetricsRepository activityMetricsRepository;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final DailyMetricsService dailyMetricsService;
    private final ObjectMapper objectMapper;
    private final CompetitionRegistrationRepository competitionRegistrationRepository;
    private final UserTrainingEntryRepository userTrainingEntryRepository;
    private final BodyMetricRepository bodyMetricRepository;

    public DashboardService(
            DailyMetricsRepository dailyMetricsRepository,
            ActivityMetricsRepository activityMetricsRepository,
            CompletedTrainingRepository completedTrainingRepository,
            DailyMetricsService dailyMetricsService,
            ObjectMapper objectMapper,
            CompetitionRegistrationRepository competitionRegistrationRepository,
            UserTrainingEntryRepository userTrainingEntryRepository,
            BodyMetricRepository bodyMetricRepository
    ) {
        this.dailyMetricsRepository = dailyMetricsRepository;
        this.activityMetricsRepository = activityMetricsRepository;
        this.completedTrainingRepository = completedTrainingRepository;
        this.dailyMetricsService = dailyMetricsService;
        this.objectMapper = objectMapper;
        this.competitionRegistrationRepository = competitionRegistrationRepository;
        this.userTrainingEntryRepository = userTrainingEntryRepository;
        this.bodyMetricRepository = bodyMetricRepository;
    }

    public DashboardDto getDashboard(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(DAYS - 1L);

        dailyMetricsService.computeToday(user);

        List<DailyMetrics> dailyMetrics = dailyMetricsRepository
                .findByUserIdAndDateBetween(user.getId(), startDate, today);

        Map<LocalDate, DailyMetrics> dailyByDate = new HashMap<>();
        for (DailyMetrics daily : dailyMetrics) {
            dailyByDate.put(daily.getDate(), daily);
        }

        DailyMetrics latestWithStrain = dailyMetrics.stream()
                .filter(d -> d.getDailyStrain21() != null)
                .max(Comparator.comparing(DailyMetrics::getDate))
                .orElse(null);

        DailyMetrics latestWithReadiness = dailyMetrics.stream()
                .filter(d -> d.getReadinessScore() != null)
                .max(Comparator.comparing(DailyMetrics::getDate))
                .orElse(null);

        // Readiness-Daten aus body_metrics (geschrieben von MetricsKernelService)
        BodyMetric bmReadiness = bodyMetricRepository
                .findTopByUserIdAndMetricTypeOrderByRecordedAtDesc(user.getId(), MetricType.READINESS_SCORE)
                .orElse(null);
        BodyMetric bmRecommendation = bodyMetricRepository
                .findTopByUserIdAndMetricTypeOrderByRecordedAtDesc(user.getId(), MetricType.RECOMMENDATION)
                .orElse(null);
        BodyMetric bmBodyBattery = bodyMetricRepository
                .findTopByUserIdAndMetricTypeOrderByRecordedAtDesc(user.getId(), MetricType.BODY_BATTERY)
                .orElse(null);
        Integer bodyBattery = bmBodyBattery != null && bmBodyBattery.getValue() != null
                ? bmBodyBattery.getValue().intValue() : null;

        DailyMetrics latestWithAcwr = dailyMetrics.stream()
                .filter(d -> d.getAcwr() != null)
                .max(Comparator.comparing(DailyMetrics::getDate))
                .orElse(null);

        List<DashboardDto.LoadTrendPointDto> loadTrend = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            DailyMetrics daily = dailyByDate.get(date);
            double strain = daily != null && daily.getDailyStrain21() != null ? daily.getDailyStrain21() : 0.0;
            loadTrend.add(new DashboardDto.LoadTrendPointDto(date, strain));
        }

        List<ActivityMetrics> efMetrics = activityMetricsRepository
                .findWithEfByUserIdAndDateRange(user.getId(), startDate, today);
        Map<LocalDate, List<Double>> efByDate = new HashMap<>();
        for (ActivityMetrics metrics : efMetrics) {
            if (metrics.getEfficiencyFactor() == null || metrics.getCompletedTraining() == null) {
                continue;
            }
            LocalDate date = metrics.getCompletedTraining().getTrainingDate();
            efByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(metrics.getEfficiencyFactor());
        }

        List<DashboardDto.EfTrendPointDto> efTrend = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            efTrend.add(new DashboardDto.EfTrendPointDto(date, average(efByDate.get(date))));
        }

        List<ActivityMetrics> driftMetrics = activityMetricsRepository
                .findEligibleDecouplingByUserIdAndDateRange(user.getId(), startDate, today);
        Map<LocalDate, List<Double>> driftByDate = new HashMap<>();
        for (ActivityMetrics metrics : driftMetrics) {
            if (metrics.getDecouplingPct() == null || metrics.getCompletedTraining() == null) {
                continue;
            }
            LocalDate date = metrics.getCompletedTraining().getTrainingDate();
            driftByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(metrics.getDecouplingPct());
        }

        List<DashboardDto.DriftTrendPointDto> driftTrend = new ArrayList<>();
        for (int i = 0; i < DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            driftTrend.add(new DashboardDto.DriftTrendPointDto(date, average(driftByDate.get(date))));
        }

        CompletedTraining lastCompleted = completedTrainingRepository
                .findTopByUserIdAndSportContainingIgnoreCaseOrderByTrainingDateDescUploadDateDesc(user.getId(), "run")
                .orElseGet(() -> completedTrainingRepository
                        .findTopByUserIdOrderByTrainingDateDescUploadDateDesc(user.getId())
                        .orElse(null));

        DashboardDto.LastRunDto lastRun = buildLastRun(lastCompleted, dailyByDate);

        double strain21 = latestWithStrain != null && latestWithStrain.getDailyStrain21() != null
                ? latestWithStrain.getDailyStrain21()
                : 0.0;

        int readinessScore = bmReadiness != null && bmReadiness.getValue() != null
                ? bmReadiness.getValue().intValue()
                : (latestWithReadiness != null && latestWithReadiness.getReadinessScore() != null
                        ? latestWithReadiness.getReadinessScore() : 0);

        String readinessRecommendation = bmRecommendation != null && bmRecommendation.getStringValue() != null
                ? bmRecommendation.getStringValue()
                : (latestWithReadiness != null && latestWithReadiness.getRecommendation() != null
                        ? latestWithReadiness.getRecommendation().name() : "EASY");

        List<String> readinessReasons = List.of();
        String reasonsRaw = bmReadiness != null ? bmReadiness.getReasonsJson() : null;
        if (reasonsRaw == null && latestWithReadiness != null) {
            reasonsRaw = latestWithReadiness.getReasonsJson();
        }
        if (reasonsRaw != null && !reasonsRaw.isBlank()) {
            try {
                readinessReasons = objectMapper.readValue(reasonsRaw, STRING_LIST);
            } catch (Exception ignored) {
                readinessReasons = List.of();
            }
        }

        double acwr = latestWithAcwr != null && latestWithAcwr.getAcwr() != null
                ? latestWithAcwr.getAcwr()
                : 0.0;

        AcwrFlag acwrFlag = latestWithAcwr != null ? latestWithAcwr.getAcwrFlag() : null;

        DashboardDto.LoadStatusDto loadStatus = new DashboardDto.LoadStatusDto(
                acwr,
                acwrFlag != null ? acwrFlag.name() : "BLUE"
        );

        // Next competition countdown
        List<CompetitionRegistration> registrations = competitionRegistrationRepository.findByUserId(user.getId());

        Optional<CompetitionRegistration> nextReg = registrations.stream()
                .filter(r -> !r.getCompetition().getDate().isBefore(today))
                .min(Comparator.comparing(r -> r.getCompetition().getDate()));

        DashboardDto.NextCompetitionDto nextCompetition = nextReg.map(r -> {
            Competition c = r.getCompetition();
            long daysUntil = ChronoUnit.DAYS.between(today, c.getDate());
            List<UserTrainingEntry> nextEntries = userTrainingEntryRepository.findByCompetitionRegistrationId(r.getId());
            LocalDate planStart = nextEntries.stream()
                    .map(UserTrainingEntry::getTrainingDate)
                    .min(Comparator.naturalOrder())
                    .orElse(r.getRegisteredAt().toLocalDate());
            long totalDays = ChronoUnit.DAYS.between(planStart, c.getDate());
            long elapsedDays = ChronoUnit.DAYS.between(planStart, today);
            double elapsedPct = totalDays > 0
                    ? Math.min(100.0, Math.max(0.0, elapsedDays * 100.0 / totalDays))
                    : 0.0;
            return new DashboardDto.NextCompetitionDto(
                    c.getName(), c.getLocation(), c.getDate(), (int) daysUntil, elapsedPct);
        }).orElse(null);

        // Training progress per competition
        List<DashboardDto.TrainingProgressDto> trainingProgress = registrations.stream()
                .filter(r -> !r.getCompetition().getDate().isBefore(today))
                .map(r -> {
                    List<UserTrainingEntry> entries = userTrainingEntryRepository.findByCompetitionRegistrationId(r.getId());
                    int total = entries.size();
                    int done = (int) entries.stream().filter(e -> Boolean.TRUE.equals(e.getCompleted())).count();
                    Competition c = r.getCompetition();
                    return new DashboardDto.TrainingProgressDto(c.getId(), c.getName(), c.getDate(), total, done);
                })
                .filter(p -> p.getTotal() > 0)
                .sorted(Comparator.comparing(DashboardDto.TrainingProgressDto::getCompetitionDate))
                .collect(Collectors.toList());

        return new DashboardDto(
                strain21,
                readinessScore,
                readinessRecommendation,
                readinessReasons,
                loadStatus,
                loadTrend,
                efTrend,
                driftTrend,
                lastRun,
                nextCompetition,
                trainingProgress,
                bodyBattery
        );
    }

    private DashboardDto.LastRunDto buildLastRun(CompletedTraining completedTraining, Map<LocalDate, DailyMetrics> dailyByDate) {
        if (completedTraining == null) {
            return new DashboardDto.LastRunDto(LocalDate.now(), 0.0, 0.0, 0.0, 0.0, List.of());
        }

        ActivityMetrics activityMetrics = activityMetricsRepository
                .findByCompletedTrainingId(completedTraining.getId())
                .orElse(null);

        double strain = activityMetrics != null && activityMetrics.getStrain21() != null
                ? activityMetrics.getStrain21()
                : 0.0;

        double driftPct = activityMetrics != null && activityMetrics.getDecouplingPct() != null
                ? activityMetrics.getDecouplingPct()
                : 0.0;

        double z4Min = activityMetrics != null && activityMetrics.getZ4Min() != null
                ? activityMetrics.getZ4Min()
                : zoneMinutesFromSeconds(completedTraining.getTimeInHrZone4Seconds());

        double z5Min = activityMetrics != null && activityMetrics.getZ5Min() != null
                ? activityMetrics.getZ5Min()
                : zoneMinutesFromSeconds(completedTraining.getTimeInHrZone5Seconds());

        List<String> coachBullets = List.of();
        DailyMetrics daily = dailyByDate.get(completedTraining.getTrainingDate());
        if (daily != null && daily.getCoachBulletsJson() != null && !daily.getCoachBulletsJson().isBlank()) {
            try {
                coachBullets = objectMapper.readValue(daily.getCoachBulletsJson(), STRING_LIST);
            } catch (Exception ignored) {
                coachBullets = List.of();
            }
        }

        return new DashboardDto.LastRunDto(
                completedTraining.getTrainingDate(),
                strain,
                driftPct,
                z4Min,
                z5Min,
                coachBullets
        );
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (Double value : values) {
            if (value != null) {
                sum += value;
            }
        }
        return sum / values.size();
    }

    private double zoneMinutesFromSeconds(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return 0.0;
        }
        return seconds / 60.0;
    }
}
