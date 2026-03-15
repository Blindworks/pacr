package com.trainingsplan.service;

import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.service.decoupling.DecouplingResult;
import com.trainingsplan.service.hrzone.HeartRateZoneConfig;
import com.trainingsplan.service.hrzone.ZoneTimeResult;
import com.trainingsplan.service.trimp.TRIMPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Orchestrates HR zone, strain, TRIMP, and aerobic decoupling calculations
 * and persists results in {@code activity_metrics}.
 * Called once per FIT file upload or Strava stream fetch.
 */
@Service
public class ActivityMetricsService {

    private static final Logger log = LoggerFactory.getLogger(ActivityMetricsService.class);

    @Autowired
    private ZoneTimeCalculator zoneTimeCalculator;

    @Autowired
    private StrainCalculator strainCalculator;

    @Autowired
    private TRIMPCalculator trimpCalculator;

    @Autowired
    private AerobicDecouplingCalculator aerobicDecouplingCalculator;

    @Autowired
    private DailyMetricsService dailyMetricsService;

    @Autowired
    private MetricsKernelService metricsKernelService;

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    /**
     * Convenience overload for FIT file uploads (no velocity/distance streams available).
     * Aerobic decoupling is skipped; EF is still computed from the summary fields on
     * {@link CompletedTraining} (averageSpeedKmh / averageHeartRate).
     */
    public void calculateAndPersist(CompletedTraining completedTraining,
                                    List<Integer> timeSeconds,
                                    List<Integer> heartRates,
                                    User user) {
        ActivityMetrics metrics = activityMetricsRepository
                .findByCompletedTrainingId(completedTraining.getId())
                .orElse(new ActivityMetrics());

        metrics.setCompletedTraining(completedTraining);

        // ── HR zones + strain ──────────────────────────────────────────────────
        if (user.getMaxHeartRate() != null && user.getMaxHeartRate() > 0) {
            HeartRateZoneConfig config = HeartRateZoneConfig.fromHrMax(user.getMaxHeartRate());
            ZoneTimeResult result = zoneTimeCalculator.calculate(timeSeconds, heartRates, config);

            if (result.isUnknown()) {
                metrics.setZonesUnknown(true);
            } else {
                metrics.setZonesUnknown(false);
                metrics.setZ1Min(result.getZ1Min());
                metrics.setZ2Min(result.getZ2Min());
                metrics.setZ3Min(result.getZ3Min());
                metrics.setZ4Min(result.getZ4Min());
                metrics.setZ5Min(result.getZ5Min());
                metrics.setHrDataCoverage(result.getHrDataCoverage());

                double rawLoad = strainCalculator.rawLoad(
                        result.getZ1Min(), result.getZ2Min(), result.getZ3Min(),
                        result.getZ4Min(), result.getZ5Min());
                metrics.setRawLoad(rawLoad);
                metrics.setStrain21(strainCalculator.strain21(rawLoad));
            }
        } else {
            metrics.setZonesUnknown(true);
        }

        // ── TRIMP ─────────────────────────────────────────────────────────────
        if (user.getMaxHeartRate() != null && user.getMaxHeartRate() > 0
                && user.getHrRest() != null && user.getHrRest() > 0) {
            double k = TRIMPCalculator.kForGender(user.getGender());
            TRIMPResult trimpResult = trimpCalculator.calculate(
                    timeSeconds, heartRates, user.getHrRest(), user.getMaxHeartRate(), k);
            if (trimpResult != null) {
                metrics.setTrimp(trimpResult.trimp());
                metrics.setTrimpQuality(trimpResult.quality().name());
            }
        }

        // ── Efficiency Factor ─────────────────────────────────────────────────
        metrics.setEfficiencyFactor(computeEF(completedTraining));

        activityMetricsRepository.save(metrics);
        LocalDate trainingDate = completedTraining.getTrainingDate();
        log.info("activity_metrics_saved activityId={} userId={} date={} trigger=fit_upload",
                completedTraining.getId(), user.getId(), trainingDate);
        dailyMetricsService.updateDailyStrain(user, trainingDate);
        dailyMetricsService.updateDailyEf(user, trainingDate);
        metricsKernelService.computeForDate(user, trainingDate);
        // Also update today's rolling metrics so dashboard always shows current status
        LocalDate today = LocalDate.now();
        if (!trainingDate.equals(today)) {
            log.debug("activity_metrics trigger=today_update userId={} today={}", user.getId(), today);
            dailyMetricsService.updateDailyStrain(user, today);
            dailyMetricsService.updateDailyEf(user, today);
            metricsKernelService.computeForDate(user, today);
        }
    }

    /**
     * Full calculation including aerobic decoupling. Called for Strava activities
     * where {@code velocity_smooth} and {@code distance} streams are available.
     *
     * <p>Upserts: if a record for the same activity already exists it is overwritten.
     *
     * @param completedTraining the already-persisted activity
     * @param timeSeconds       elapsed seconds from activity start
     * @param heartRates        HR in bpm, parallel to timeSeconds; nulls = missing data
     * @param velocities        speed in m/s (Strava {@code velocity_smooth}), parallel to timeSeconds;
     *                          pass {@code null} to skip decoupling
     * @param distances         cumulative metres (Strava {@code distance}), parallel to timeSeconds;
     *                          pass {@code null} to use time-based half-split in decoupling
     * @param user              the athlete (supplies hrMax, hrRest, gender for zone/TRIMP boundaries)
     */
    public void calculateAndPersist(CompletedTraining completedTraining,
                                    List<Integer> timeSeconds,
                                    List<Integer> heartRates,
                                    List<Double>  velocities,
                                    List<Double>  distances,
                                    User user) {
        ActivityMetrics metrics = activityMetricsRepository
                .findByCompletedTrainingId(completedTraining.getId())
                .orElse(new ActivityMetrics());

        metrics.setCompletedTraining(completedTraining);

        // ── HR zones + strain ──────────────────────────────────────────────────
        if (user.getMaxHeartRate() != null && user.getMaxHeartRate() > 0) {
            HeartRateZoneConfig config = HeartRateZoneConfig.fromHrMax(user.getMaxHeartRate());
            ZoneTimeResult result = zoneTimeCalculator.calculate(timeSeconds, heartRates, config);

            if (result.isUnknown()) {
                metrics.setZonesUnknown(true);
            } else {
                metrics.setZonesUnknown(false);
                metrics.setZ1Min(result.getZ1Min());
                metrics.setZ2Min(result.getZ2Min());
                metrics.setZ3Min(result.getZ3Min());
                metrics.setZ4Min(result.getZ4Min());
                metrics.setZ5Min(result.getZ5Min());
                metrics.setHrDataCoverage(result.getHrDataCoverage());

                double rawLoad = strainCalculator.rawLoad(
                        result.getZ1Min(), result.getZ2Min(), result.getZ3Min(),
                        result.getZ4Min(), result.getZ5Min());
                metrics.setRawLoad(rawLoad);
                metrics.setStrain21(strainCalculator.strain21(rawLoad));
            }
        } else {
            metrics.setZonesUnknown(true);
        }

        // ── TRIMP ─────────────────────────────────────────────────────────────
        if (user.getMaxHeartRate() != null && user.getMaxHeartRate() > 0
                && user.getHrRest() != null && user.getHrRest() > 0) {
            double k = TRIMPCalculator.kForGender(user.getGender());
            TRIMPResult trimpResult = trimpCalculator.calculate(
                    timeSeconds, heartRates, user.getHrRest(), user.getMaxHeartRate(), k);
            if (trimpResult != null) {
                metrics.setTrimp(trimpResult.trimp());
                metrics.setTrimpQuality(trimpResult.quality().name());
            }
        }

        // ── Aerobic decoupling ────────────────────────────────────────────────
        if (velocities != null) {
            DecouplingResult dr = aerobicDecouplingCalculator.calculate(
                    timeSeconds, heartRates, velocities, distances);
            metrics.setDecouplingEligible(dr.eligible());
            metrics.setDecouplingPct(dr.decouplingPct());
            metrics.setDecouplingReason(dr.reason());
        }

        // ── Efficiency Factor ─────────────────────────────────────────────────
        metrics.setEfficiencyFactor(computeEF(completedTraining));

        activityMetricsRepository.save(metrics);
        LocalDate trainingDate = completedTraining.getTrainingDate();
        log.info("activity_metrics_saved activityId={} userId={} date={} trigger=strava_stream",
                completedTraining.getId(), user.getId(), trainingDate);
        dailyMetricsService.updateDailyStrain(user, trainingDate);
        dailyMetricsService.updateDailyEf(user, trainingDate);
        metricsKernelService.computeForDate(user, trainingDate);
        // Also update today's rolling metrics so dashboard always shows current status
        LocalDate today = LocalDate.now();
        if (!trainingDate.equals(today)) {
            log.debug("activity_metrics trigger=today_update userId={} today={}", user.getId(), today);
            dailyMetricsService.updateDailyStrain(user, today);
            dailyMetricsService.updateDailyEf(user, today);
            metricsKernelService.computeForDate(user, today);
        }
    }

    /**
     * Efficiency Factor = avgSpeed (m/s) / avgHR (bpm).
     * Returns {@code null} when speed or heart rate data is unavailable or zero.
     */
    private Double computeEF(CompletedTraining ct) {
        if (ct.getAverageHeartRate() == null || ct.getAverageHeartRate() <= 0) return null;
        if (ct.getAverageSpeedKmh() == null || ct.getAverageSpeedKmh() <= 0) return null;
        double speedMps = ct.getAverageSpeedKmh() / 3.6;
        return speedMps / ct.getAverageHeartRate();
    }
}
