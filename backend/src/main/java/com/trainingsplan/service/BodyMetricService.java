package com.trainingsplan.service;

import com.trainingsplan.entity.BodyMetric;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.BodyMetricRepository;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class BodyMetricService {

    private static final Logger log = LoggerFactory.getLogger(BodyMetricService.class);

    private static final List<String> METRIC_TYPES = List.of("VO2MAX", "VO2MAX_HR_CORRECTED");

    @Autowired
    private BodyMetricRepository bodyMetricRepository;

    @Autowired
    private CompletedTrainingRepository completedTrainingRepository;

    @Autowired
    private Vo2MaxService vo2MaxService;

    @Autowired
    private DailyMetricsService dailyMetricsService;

    @Autowired
    private MetricsKernelService metricsKernelService;

    @Autowired
    private SecurityUtils securityUtils;

    /**
     * Called after a CompletedTraining is saved. Calculates all applicable
     * body metrics and upserts them in the database.
     */
    public void calculateAndStore(CompletedTraining training, User user) {
        if (!isRunning(training)) return;

        Double distanceMeters = training.getDistanceKm() != null ? training.getDistanceKm() * 1000 : null;
        // Daniels: total duration (designed for continuous/race effort)
        Integer durationTime = training.getDurationSeconds() != null
                ? training.getDurationSeconds()
                : training.getMovingTimeSeconds();
        // HR-corrected: actual running pace → use moving time (excludes stops)
        Integer movingTime = training.getMovingTimeSeconds() != null
                ? training.getMovingTimeSeconds()
                : training.getDurationSeconds();

        // Standard VO2Max (Daniels/VDOT)
        vo2MaxService.calculate(distanceMeters, durationTime)
                .ifPresent(v -> upsert(user, "VO2MAX", v, "ml/kg/min",
                        training.getTrainingDate(), training.getId()));

        // HR-corrected VO2Max — only when the athlete's maxHR is set in their profile
        if (user.getMaxHeartRate() != null) {
            vo2MaxService.calculateHRCorrected(
                            distanceMeters, movingTime,
                            training.getAverageHeartRate(),
                            user.getMaxHeartRate())
                    .ifPresent(v -> upsert(user, "VO2MAX_HR_CORRECTED", v, "ml/kg/min",
                            training.getTrainingDate(), training.getId()));
        }
    }

    /**
     * Returns the latest value for each known metric type for the current user.
     * Only types with at least one stored value are included.
     */
    public List<BodyMetric> getLatestMetricsForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        List<BodyMetric> result = new ArrayList<>();
        for (String type : METRIC_TYPES) {
            bodyMetricRepository
                    .findTopByUserIdAndMetricTypeOrderByRecordedAtDesc(userId, type)
                    .ifPresent(result::add);
        }
        return result;
    }

    /**
     * Returns all VO2Max measurements for the current user ordered chronologically,
     * each enriched with race-time predictions for standard distances.
     */
    public List<Map<String, Object>> getVo2MaxHistoryForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) return Collections.emptyList();

        List<BodyMetric> metrics = bodyMetricRepository
                .findByUserIdAndMetricTypeOrderByRecordedAtDesc(userId, "VO2MAX");
        Collections.reverse(metrics); // ascending (chronological) order

        return metrics.stream().map(m -> {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", m.getRecordedAt() != null ? m.getRecordedAt().toString() : null);
            point.put("vo2max", m.getValue());
            point.put("predictions", vo2MaxService.predictRaceTimes(m.getValue()));
            return point;
        }).toList();
    }

    /**
     * Recalculates all body metrics for the current user from their existing
     * CompletedTraining records. Also claims activities that have no user assigned
     * (uploaded before authentication was introduced). Deletes previous metrics first.
     *
     * @return number of activities processed (including non-running ones)
     */
    @Transactional
    public int recalculateForCurrentUser() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return 0;

        bodyMetricRepository.deleteByUserId(user.getId());

        List<CompletedTraining> trainings = completedTrainingRepository.findByUserId(user.getId());

        int total = trainings.size();
        for (CompletedTraining t : trainings) {
            calculateAndStore(t, user);
        }

        dailyMetricsService.recomputeEfForUser(user);

        // Backfill aller Tagesmetriken in body_metrics für die letzten 90 Tage
        LocalDate today = LocalDate.now();
        log.info("body_metrics_recalculate trigger=manual userId={} activities={}", user.getId(), total);
        metricsKernelService.computeForDateRange(user, today.minusDays(89), today);

        return total;
    }

    private void upsert(User user, String metricType, Double value, String unit,
                        LocalDate recordedAt, Long sourceActivityId) {
        BodyMetric metric = bodyMetricRepository
                .findByUserIdAndMetricTypeAndSourceActivityId(user.getId(), metricType, sourceActivityId)
                .orElse(new BodyMetric());
        metric.setUser(user);
        metric.setMetricType(metricType);
        metric.setValue(value);
        metric.setUnit(unit);
        metric.setRecordedAt(recordedAt);
        metric.setSourceActivityId(sourceActivityId);
        bodyMetricRepository.save(metric);
    }

    private boolean isRunning(CompletedTraining t) {
        String sport = t.getSport();
        if (sport != null) {
            return sport.toLowerCase().contains("run");
        }
        // Fallback for activities uploaded before sport field was reliably parsed:
        // treat as running if pace data is present and no power data (cycling)
        return t.getAveragePaceSecondsPerKm() != null
                || (t.getDistanceKm() != null && t.getAveragePowerWatts() == null);
    }
}
