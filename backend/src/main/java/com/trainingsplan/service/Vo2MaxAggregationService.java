package com.trainingsplan.service;

import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserVo2MaxState;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.UserVo2MaxStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Computes a smoothed, per-user long-term VO2max using an exponentially weighted
 * moving average (EWMA) over per-workout VO2max estimates.
 *
 * Pipeline per workout:
 * 1. Eligibility filter (sport=running, distance>0, duration>=15min, plausible pace,
 *    optional HR-coverage gate — see {@link #evaluate}).
 * 2. Blend pace-based and HR-corrected estimates (0.5/0.5 when both available,
 *    pace-only when max HR or HR data missing).
 * 3. Cold-start (first 3 eligible workouts) — plain incremental mean, no clipping.
 * 4. Outlier clipping (±3.0 ml/kg/min vs current internal value) from the 4th workout on.
 * 5. EWMA update with τ=18 days, Δt clamped to ≥0.5 days.
 * 6. Hysteresis on the displayed integer value (0.5-point threshold).
 *
 * Every state change is appended as a new row in {@code user_vo2max_state} for audit.
 */
@Service
public class Vo2MaxAggregationService {

    private static final Logger log = LoggerFactory.getLogger(Vo2MaxAggregationService.class);

    public enum SkipReason {
        SPORT_NOT_RUNNING,
        NO_DISTANCE,
        TOO_SHORT,
        IMPLAUSIBLE_PACE,
        LOW_HR_COVERAGE,
        NO_SINGLE_ESTIMATE
    }

    @Value("${vo2max.tauDays:18.0}")
    private double tauDays;

    @Value("${vo2max.outlierClipThreshold:3.0}")
    private double outlierClipThreshold;

    @Value("${vo2max.hysteresisThreshold:0.5}")
    private double hysteresisThreshold;

    @Value("${vo2max.coldStartCount:3}")
    private int coldStartCount;

    @Value("${vo2max.minDurationMinutes:15.0}")
    private double minDurationMinutes;

    @Value("${vo2max.minHrCoveragePercent:80.0}")
    private double minHrCoveragePercent;

    @Value("${vo2max.minDeltaDays:0.5}")
    private double minDeltaDays;

    @Autowired
    private UserVo2MaxStateRepository stateRepository;

    @Autowired
    private CompletedTrainingRepository completedTrainingRepository;

    @Autowired
    private Vo2MaxService vo2MaxService;

    @Transactional
    public void updateForWorkout(User user, CompletedTraining training) {
        if (user == null || training == null) return;

        Evaluation eval = evaluate(training);
        if (eval.skipReason != null) return;

        Double vo2maxWorkout = computeWorkoutValue(training, user, eval.useHrCorrected);
        if (vo2maxWorkout == null) return;

        LocalDateTime workoutTimestamp = toTimestamp(training);
        Optional<UserVo2MaxState> previous = stateRepository
                .findTopByUserIdOrderByCreatedAtDescIdDesc(user.getId());

        UserVo2MaxState next = computeNextState(previous.orElse(null), vo2maxWorkout, workoutTimestamp);
        next.setUser(user);
        next.setSourceActivityId(training.getId());
        stateRepository.save(next);
    }

    /**
     * Wipes all state history for the user and replays all eligible workouts in
     * chronological order. Also backfills the {@code quality_ok} /
     * {@code hr_coverage_percent} columns for trainings uploaded before those
     * fields existed, so the debug endpoint reflects persisted values.
     */
    @Transactional
    public void recalculateForUser(User user) {
        if (user == null) return;
        log.info("vo2max_recalc_start userId={}", user.getId());
        stateRepository.deleteByUserId(user.getId());

        List<CompletedTraining> trainings = completedTrainingRepository.findByUserId(user.getId());
        List<CompletedTraining> ordered = trainings.stream()
                .filter(t -> t.getTrainingDate() != null)
                .sorted(Comparator
                        .comparing(CompletedTraining::getTrainingDate)
                        .thenComparing(CompletedTraining::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int eligibleCount = 0;
        for (CompletedTraining t : ordered) {
            backfillQualityFields(t);
            Evaluation eval = evaluate(t);
            if (eval.skipReason == null) {
                if (computeWorkoutValue(t, user, eval.useHrCorrected) != null) {
                    eligibleCount++;
                }
            }
            updateForWorkout(user, t);
        }

        Optional<UserVo2MaxState> finalState = stateRepository.findTopByUserIdOrderByCreatedAtDescIdDesc(user.getId());
        log.info("vo2max_recalc_done userId={} totalActivities={} eligible={} ineligible={} finalDisplayed={} finalInternal={} count={}",
                user.getId(), ordered.size(), eligibleCount, ordered.size() - eligibleCount,
                finalState.map(UserVo2MaxState::getVo2maxDisplayed).orElse(null),
                finalState.map(s -> String.format("%.2f", s.getVo2maxInternal())).orElse(null),
                finalState.map(UserVo2MaxState::getEligibleWorkoutCount).orElse(null));
    }

    public Optional<UserVo2MaxState> getCurrentState(User user) {
        if (user == null) return Optional.empty();
        return stateRepository.findTopByUserIdOrderByCreatedAtDescIdDesc(user.getId());
    }

    public List<UserVo2MaxState> getHistory(User user) {
        if (user == null) return List.of();
        return stateRepository.findByUserIdOrderByCreatedAtAscIdAsc(user.getId());
    }

    // ---------- pipeline steps ----------

    private static class Evaluation {
        SkipReason skipReason;
        boolean useHrCorrected;
    }

    /**
     * Two-phase eligibility:
     *   Phase 1 — hard gates: running sport, distance>0, duration>=minDuration, plausible pace.
     *   Phase 2 — HR coverage: if KNOWN and <threshold, skip entirely. If unknown,
     *             only the HR-corrected estimate is disabled — the pace-based Daniels
     *             estimate alone is still valid.
     */
    private Evaluation evaluate(CompletedTraining t) {
        Evaluation e = new Evaluation();

        if (!isRunningSport(t)) { e.skipReason = SkipReason.SPORT_NOT_RUNNING; return e; }
        if (t.getDistanceKm() == null || t.getDistanceKm() <= 0) { e.skipReason = SkipReason.NO_DISTANCE; return e; }
        Integer duration = t.getDurationSeconds();
        if (duration == null || duration < minDurationMinutes * 60.0) { e.skipReason = SkipReason.TOO_SHORT; return e; }
        Integer pace = t.getAveragePaceSecondsPerKm();
        if (pace == null || pace < 150 || pace > 900) { e.skipReason = SkipReason.IMPLAUSIBLE_PACE; return e; }

        Double coverage = t.getHrCoveragePercent();
        if (coverage != null) {
            if (coverage < minHrCoveragePercent) { e.skipReason = SkipReason.LOW_HR_COVERAGE; return e; }
            e.useHrCorrected = t.getAverageHeartRate() != null && t.getAverageHeartRate() > 0;
        } else {
            // Unknown coverage — pace-based estimate still valid; HR-corrected only if avgHR present
            e.useHrCorrected = t.getAverageHeartRate() != null && t.getAverageHeartRate() > 0;
        }
        return e;
    }

    private boolean isRunningSport(CompletedTraining t) {
        String sport = t.getSport();
        if (sport != null) {
            return sport.toLowerCase().contains("run");
        }
        // Fallback for trainings with missing sport: pace present, or distance present and no cycling power
        return t.getAveragePaceSecondsPerKm() != null
                || (t.getDistanceKm() != null && t.getAveragePowerWatts() == null);
    }

    private boolean deriveQualityOk(CompletedTraining t) {
        return evaluate(t).skipReason == null;
    }

    private void backfillQualityFields(CompletedTraining t) {
        boolean changed = false;
        if (t.getQualityOk() == null) {
            t.setQualityOk(deriveQualityOk(t));
            changed = true;
        }
        if (t.getHrCoveragePercent() == null) {
            double coverage = (t.getAverageHeartRate() != null && t.getAverageHeartRate() > 0) ? 100.0 : 0.0;
            t.setHrCoveragePercent(coverage);
            changed = true;
        }
        if (changed) {
            completedTrainingRepository.save(t);
        }
    }

    private Double computeWorkoutValue(CompletedTraining t, User user, boolean useHrCorrected) {
        Double distanceMeters = t.getDistanceKm() != null ? t.getDistanceKm() * 1000 : null;
        Integer durationTime = t.getDurationSeconds() != null
                ? t.getDurationSeconds()
                : t.getMovingTimeSeconds();
        Integer movingTime = t.getMovingTimeSeconds() != null
                ? t.getMovingTimeSeconds()
                : t.getDurationSeconds();

        Double pace = vo2MaxService.calculate(distanceMeters, durationTime).orElse(null);

        Double hrCorrected = null;
        if (useHrCorrected && user.getMaxHeartRate() != null) {
            hrCorrected = vo2MaxService.calculateHRCorrected(
                    distanceMeters, movingTime, t.getAverageHeartRate(), user.getMaxHeartRate()
            ).orElse(null);
        }

        if (pace != null && hrCorrected != null) return 0.5 * pace + 0.5 * hrCorrected;
        if (pace != null) return pace;
        return hrCorrected;
    }

    /**
     * Pure-function core for testability: given a previous state (may be null)
     * and a new workout value + timestamp, returns the next state row.
     */
    UserVo2MaxState computeNextState(UserVo2MaxState previous, double vo2maxWorkout, LocalDateTime workoutTimestamp) {
        UserVo2MaxState next = new UserVo2MaxState();
        next.setLastUpdateAt(workoutTimestamp);

        if (previous == null) {
            next.setVo2maxInternal(vo2maxWorkout);
            next.setVo2maxDisplayed((int) Math.round(vo2maxWorkout));
            next.setEligibleWorkoutCount(1);
            return next;
        }

        double internalOld = previous.getVo2maxInternal();
        int countOld = previous.getEligibleWorkoutCount() != null ? previous.getEligibleWorkoutCount() : 0;
        int displayedOld = previous.getVo2maxDisplayed() != null ? previous.getVo2maxDisplayed() : (int) Math.round(internalOld);

        double internalNew;
        int countNew = countOld + 1;

        if (countOld < coldStartCount) {
            internalNew = internalOld + (vo2maxWorkout - internalOld) / countNew;
        } else {
            double delta = vo2maxWorkout - internalOld;
            double clipped;
            if (delta > outlierClipThreshold) clipped = internalOld + outlierClipThreshold;
            else if (delta < -outlierClipThreshold) clipped = internalOld - outlierClipThreshold;
            else clipped = vo2maxWorkout;

            double deltaDays = Math.max(minDeltaDays, daysBetween(previous.getLastUpdateAt(), workoutTimestamp));
            double alpha = 1.0 - Math.exp(-deltaDays / tauDays);
            internalNew = alpha * clipped + (1.0 - alpha) * internalOld;
        }

        next.setVo2maxInternal(internalNew);
        next.setEligibleWorkoutCount(countNew);

        int candidate = (int) Math.round(internalNew);
        if (Math.abs(internalNew - displayedOld) >= hysteresisThreshold && candidate != displayedOld) {
            next.setVo2maxDisplayed(candidate);
        } else {
            next.setVo2maxDisplayed(displayedOld);
        }
        return next;
    }

    private static double daysBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return 0.0;
        return Duration.between(from, to).toSeconds() / 86400.0;
    }

    private static LocalDateTime toTimestamp(CompletedTraining t) {
        LocalDate date = t.getTrainingDate() != null ? t.getTrainingDate() : LocalDate.now();
        LocalTime time = t.getStartTime() != null ? t.getStartTime() : LocalTime.NOON;
        return LocalDateTime.of(date, time);
    }
}
