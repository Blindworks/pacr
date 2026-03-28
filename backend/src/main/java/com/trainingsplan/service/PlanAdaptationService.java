package com.trainingsplan.service;

import com.trainingsplan.entity.*;
import com.trainingsplan.repository.DailyMetricsRepository;
import com.trainingsplan.repository.PlanAdjustmentRepository;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PlanAdaptationService {

    private static final Logger log = LoggerFactory.getLogger(PlanAdaptationService.class);

    private static final Set<String> CRITICAL_TYPES = Set.of(
            "speed", "endurance", "race", "fartlek"
    );
    private static final Set<String> DROPPABLE_TYPES = Set.of(
            "recovery", "general"
    );
    private static final int READINESS_RED_THRESHOLD = 40;
    private static final int MISSED_LOOKBACK_DAYS = 7;
    private static final int RESCHEDULE_WINDOW_DAYS = 7;
    private static final int INTENSITY_REDUCE_DAYS = 3;

    private final UserTrainingEntryRepository entryRepository;
    private final PlanAdjustmentRepository adjustmentRepository;
    private final DailyMetricsRepository dailyMetricsRepository;

    public PlanAdaptationService(UserTrainingEntryRepository entryRepository,
                                 PlanAdjustmentRepository adjustmentRepository,
                                 DailyMetricsRepository dailyMetricsRepository) {
        this.entryRepository = entryRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.dailyMetricsRepository = dailyMetricsRepository;
    }

    /**
     * Main entry point: detect missed workouts and low readiness, create pending adjustments.
     */
    @Transactional
    public void evaluateAndAdapt(User user) {
        LocalDate today = LocalDate.now();
        Long userId = user.getId();

        log.info("Evaluating plan adaptation for user {}", userId);

        detectMissedWorkouts(user, today);
        detectLowReadiness(user, today);
    }

    private void detectMissedWorkouts(User user, LocalDate today) {
        LocalDate since = today.minusDays(MISSED_LOOKBACK_DAYS);
        List<UserTrainingEntry> missed = entryRepository.findMissedWorkouts(
                user.getId(), today, since);

        for (UserTrainingEntry entry : missed) {
            if (hasPendingAdjustment(entry.getId())) {
                continue;
            }

            Training training = entry.getTraining();
            String type = training.getTrainingType() != null
                    ? training.getTrainingType().toLowerCase() : "";

            if (CRITICAL_TYPES.contains(type)) {
                createRescheduleAdjustment(user, entry, today);
            } else if (DROPPABLE_TYPES.contains(type)) {
                createDropAdjustment(user, entry);
            } else {
                // Default: attempt reschedule for unclassified types
                createRescheduleAdjustment(user, entry, today);
            }
        }
    }

    private void createRescheduleAdjustment(User user, UserTrainingEntry entry, LocalDate today) {
        LocalDate newDate = findNextAvailableDay(user.getId(), today, RESCHEDULE_WINDOW_DAYS);
        if (newDate == null) {
            // No available slot — drop instead
            createDropAdjustment(user, entry);
            return;
        }

        PlanAdjustment adjustment = new PlanAdjustment();
        adjustment.setUser(user);
        adjustment.setUserTrainingEntry(entry);
        adjustment.setAdjustmentType(AdjustmentType.RESCHEDULE);
        adjustment.setTriggerSource("MISSED_WORKOUT");
        adjustment.setReason(String.format("Missed '%s' on %s — reschedule to %s",
                entry.getTraining().getName(), entry.getTrainingDate(), newDate));
        adjustment.setOriginalDate(entry.getTrainingDate());
        adjustment.setNewDate(newDate);
        adjustmentRepository.save(adjustment);

        log.info("Created RESCHEDULE adjustment for entry {} → {}", entry.getId(), newDate);
    }

    private void createDropAdjustment(User user, UserTrainingEntry entry) {
        PlanAdjustment adjustment = new PlanAdjustment();
        adjustment.setUser(user);
        adjustment.setUserTrainingEntry(entry);
        adjustment.setAdjustmentType(AdjustmentType.DROP);
        adjustment.setTriggerSource("MISSED_WORKOUT");
        adjustment.setReason(String.format("Missed recovery '%s' on %s — dropped to avoid overloading",
                entry.getTraining().getName(), entry.getTrainingDate()));
        adjustment.setOriginalDate(entry.getTrainingDate());
        adjustmentRepository.save(adjustment);

        log.info("Created DROP adjustment for entry {}", entry.getId());
    }

    private void detectLowReadiness(User user, LocalDate today) {
        dailyMetricsRepository.findByUserIdAndDate(user.getId(), today).ifPresent(metrics -> {
            Integer readiness = metrics.getReadinessScore();
            if (readiness != null && readiness < READINESS_RED_THRESHOLD) {
                createIntensityReductions(user, today, readiness);
            }
        });
    }

    private void createIntensityReductions(User user, LocalDate today, int readinessScore) {
        LocalDate from = today;
        LocalDate to = today.plusDays(INTENSITY_REDUCE_DAYS);
        List<UserTrainingEntry> upcoming = entryRepository.findUpcomingEntries(
                user.getId(), from, to);

        for (UserTrainingEntry entry : upcoming) {
            if (hasPendingAdjustment(entry.getId())) {
                continue;
            }

            String currentIntensity = entry.getTraining().getIntensityLevel();
            if (currentIntensity == null) {
                continue;
            }

            String reduced = reduceIntensity(currentIntensity.toLowerCase());
            if (reduced.equals(currentIntensity.toLowerCase())) {
                continue; // already at lowest
            }

            PlanAdjustment adjustment = new PlanAdjustment();
            adjustment.setUser(user);
            adjustment.setUserTrainingEntry(entry);
            adjustment.setAdjustmentType(AdjustmentType.INTENSITY_REDUCE);
            adjustment.setTriggerSource("LOW_READINESS");
            adjustment.setReason(String.format(
                    "Readiness score %d (< %d) — reducing '%s' intensity from %s to %s",
                    readinessScore, READINESS_RED_THRESHOLD,
                    entry.getTraining().getName(), currentIntensity, reduced));
            adjustment.setOriginalDate(entry.getTrainingDate());
            adjustment.setOriginalIntensity(currentIntensity);
            adjustment.setNewIntensity(reduced);
            adjustmentRepository.save(adjustment);

            log.info("Created INTENSITY_REDUCE adjustment for entry {}: {} → {}",
                    entry.getId(), currentIntensity, reduced);
        }
    }

    private String reduceIntensity(String intensity) {
        return switch (intensity) {
            case "high" -> "medium";
            case "medium" -> "low";
            case "low" -> "recovery";
            default -> intensity;
        };
    }

    /**
     * Find the next day with no existing training entry within the given window.
     */
    private LocalDate findNextAvailableDay(Long userId, LocalDate from, int windowDays) {
        LocalDate to = from.plusDays(windowDays);
        List<UserTrainingEntry> existing = entryRepository.findUpcomingEntries(userId, from, to);
        Set<LocalDate> occupiedDates = new java.util.HashSet<>();
        for (UserTrainingEntry e : existing) {
            occupiedDates.add(e.getTrainingDate());
        }

        for (int i = 1; i <= windowDays; i++) {
            LocalDate candidate = from.plusDays(i);
            if (!occupiedDates.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean hasPendingAdjustment(Long entryId) {
        return adjustmentRepository.existsByUserTrainingEntryIdAndStatusIn(
                entryId, List.of(AdjustmentStatus.PENDING, AdjustmentStatus.ACCEPTED));
    }

    // ── Accept / Reject ──────────────────────────────────────────────────

    @Transactional
    public PlanAdjustment acceptAdjustment(Long adjustmentId, Long userId) {
        PlanAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new IllegalArgumentException("Adjustment not found: " + adjustmentId));

        if (!adjustment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Adjustment does not belong to user");
        }
        if (adjustment.getStatus() != AdjustmentStatus.PENDING) {
            throw new IllegalStateException("Adjustment is not pending");
        }

        UserTrainingEntry entry = adjustment.getUserTrainingEntry();

        switch (adjustment.getAdjustmentType()) {
            case RESCHEDULE -> {
                if (entry != null && adjustment.getNewDate() != null) {
                    entry.setOriginalTrainingDate(entry.getTrainingDate());
                    entry.setTrainingDate(adjustment.getNewDate());
                    entry.setCompletionStatus("rescheduled");
                    entryRepository.save(entry);
                }
            }
            case DROP -> {
                if (entry != null) {
                    entry.setCompletionStatus("dropped");
                    entryRepository.save(entry);
                }
            }
            case INTENSITY_REDUCE -> {
                if (entry != null) {
                    entry.setCompletionStatus("intensity_reduced");
                    entryRepository.save(entry);
                }
            }
        }

        adjustment.setStatus(AdjustmentStatus.ACCEPTED);
        adjustment.setResolvedAt(LocalDateTime.now());
        return adjustmentRepository.save(adjustment);
    }

    @Transactional
    public PlanAdjustment rejectAdjustment(Long adjustmentId, Long userId) {
        PlanAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new IllegalArgumentException("Adjustment not found: " + adjustmentId));

        if (!adjustment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Adjustment does not belong to user");
        }
        if (adjustment.getStatus() != AdjustmentStatus.PENDING) {
            throw new IllegalStateException("Adjustment is not pending");
        }

        adjustment.setStatus(AdjustmentStatus.REJECTED);
        adjustment.setResolvedAt(LocalDateTime.now());
        return adjustmentRepository.save(adjustment);
    }

    /**
     * Returns all pending adjustments for a user.
     */
    public List<PlanAdjustment> getPendingAdjustments(Long userId) {
        return adjustmentRepository.findByUserIdAndStatus(userId, AdjustmentStatus.PENDING);
    }

    /**
     * Returns adjustment history for AI Coach context.
     */
    public List<PlanAdjustment> getAdjustmentHistory(Long userId) {
        return adjustmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
