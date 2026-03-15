package com.trainingsplan.service;

import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Maintains aggregated daily strain and rolling efficiency metrics in {@code daily_metrics}.
 * Recomputes totals by aggregating all activity_metrics records for the day,
 * ensuring idempotency (safe on re-import of the same activity).
 */
@Service
public class DailyMetricsService {

    private static final Logger log = LoggerFactory.getLogger(DailyMetricsService.class);

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    @Autowired
    private DailyMetricsRepository dailyMetricsRepository;

    @Autowired
    private LoadModelService loadModelService;

    @Autowired
    private ReadinessService readinessService;

    @Autowired
    private MetricsKernelService metricsKernelService;

    /**
     * Recomputes and upserts the daily strain21 and TRIMP aggregates for {@code user} on {@code date}.
     */
    public void updateDailyStrain(User user, LocalDate date) {
        Double totalStrain = activityMetricsRepository.sumStrain21ByUserIdAndDate(user.getId(), date);
        if (totalStrain == null) {
            totalStrain = 0.0;
        }

        Double totalTrimp = activityMetricsRepository.sumTrimpByUserIdAndDate(user.getId(), date);

        DailyMetrics daily = dailyMetricsRepository
                .findByUserIdAndDate(user.getId(), date)
                .orElse(new DailyMetrics());

        daily.setUser(user);
        daily.setDate(date);
        daily.setDailyStrain21(totalStrain);
        daily.setDailyTrimp(totalTrimp);
        dailyMetricsRepository.save(daily);
        loadModelService.updateAcwr(user, date);
        readinessService.compute(user, date);
    }

    /**
     * Recomputes and upserts the rolling 7-day and 28-day average Efficiency Factor
     * for {@code user} on {@code date}.
     *
     * <p>ef7 is the average EF of all activities in [{@code date}-6, {@code date}].
     * ef28 is the average EF of all activities in [{@code date}-27, {@code date}].
     * Both are {@code null} when the respective window contains no eligible activity.
     */
    public void updateDailyEf(User user, LocalDate date) {
        List<ActivityMetrics> window28 = activityMetricsRepository
                .findWithEfByUserIdAndDateRange(user.getId(), date.minusDays(27), date);

        Double ef7  = averageEfInWindow(window28, date.minusDays(6), date);
        Double ef28 = averageEfInWindow(window28, date.minusDays(27), date);

        DailyMetrics daily = dailyMetricsRepository
                .findByUserIdAndDate(user.getId(), date)
                .orElse(new DailyMetrics());

        daily.setUser(user);
        daily.setDate(date);
        daily.setEf7(ef7);
        daily.setEf28(ef28);
        dailyMetricsRepository.save(daily);
    }

    /**
     * Recomputes EF rolling averages for the last 90 days (today-89 through today) for the given user.
     * Useful for backfilling after the EF feature is deployed.
     */
    public void recomputeEfForUser(User user) {
        LocalDate today = LocalDate.now();
        for (int i = 89; i >= 0; i--) {
            updateDailyEf(user, today.minusDays(i));
        }
    }

    /**
     * Computes and persists today's strain, EF, ACWR and Readiness metrics.
     * Called when the dashboard is loaded to ensure today's status is always current,
     * even on rest days without any training activity.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void computeToday(User user) {
        LocalDate today = LocalDate.now();
        log.info("daily_metrics trigger=dashboard_load userId={} date={}", user.getId(), today);
        updateDailyStrain(user, today);
        updateDailyEf(user, today);
        metricsKernelService.computeForDate(user, today);
    }

    /**
     * Averages the efficiencyFactor of all records in {@code candidates} whose activity date
     * falls within [{@code windowStart}, {@code windowEnd}] (inclusive).
     * Returns {@code null} when no eligible records fall in the window.
     */
    private Double averageEfInWindow(List<ActivityMetrics> candidates,
                                     LocalDate windowStart,
                                     LocalDate windowEnd) {
        double sum = 0.0;
        int count = 0;
        for (ActivityMetrics am : candidates) {
            LocalDate activityDate = am.getCompletedTraining().getTrainingDate();
            if (!activityDate.isBefore(windowStart) && !activityDate.isAfter(windowEnd)) {
                sum += am.getEfficiencyFactor();
                count++;
            }
        }
        return count > 0 ? sum / count : null;
    }
}
