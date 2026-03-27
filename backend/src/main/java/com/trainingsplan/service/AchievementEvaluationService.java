package com.trainingsplan.service;

import com.trainingsplan.entity.*;
import com.trainingsplan.event.TrainingCompletedEvent;
import com.trainingsplan.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AchievementEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AchievementEvaluationService.class);

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final CompletedTrainingRepository completedTrainingRepository;
    private final PersonalRecordRepository personalRecordRepository;
    private final PersonalRecordEntryRepository personalRecordEntryRepository;
    private final UserTrainingEntryRepository userTrainingEntryRepository;

    public AchievementEvaluationService(AchievementRepository achievementRepository,
                                         UserAchievementRepository userAchievementRepository,
                                         CompletedTrainingRepository completedTrainingRepository,
                                         PersonalRecordRepository personalRecordRepository,
                                         PersonalRecordEntryRepository personalRecordEntryRepository,
                                         UserTrainingEntryRepository userTrainingEntryRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.completedTrainingRepository = completedTrainingRepository;
        this.personalRecordRepository = personalRecordRepository;
        this.personalRecordEntryRepository = personalRecordEntryRepository;
        this.userTrainingEntryRepository = userTrainingEntryRepository;
    }

    @EventListener
    @Transactional
    public void onTrainingCompleted(TrainingCompletedEvent event) {
        try {
            evaluateAll(event.getUser());
        } catch (Exception e) {
            log.warn("Achievement evaluation failed for user {}: {}", event.getUser().getId(), e.getMessage());
        }
    }

    @Transactional
    public List<Achievement> evaluateAll(User user) {
        List<Achievement> allAchievements = achievementRepository.findAllByOrderBySortOrderAsc();
        List<Achievement> newlyUnlocked = new ArrayList<>();

        for (Achievement achievement : allAchievements) {
            // Skip expired achievements that user hasn't unlocked yet
            if (achievement.isExpired() && !isAlreadyUnlocked(user, achievement)) {
                continue;
            }
            // Skip achievements that haven't started yet
            if (achievement.getValidFrom() != null && LocalDate.now().isBefore(achievement.getValidFrom())) {
                continue;
            }

            double currentValue = calculateCurrentValue(user, achievement);
            if (upsertProgress(user, achievement, currentValue)) {
                newlyUnlocked.add(achievement);
            }
        }
        return newlyUnlocked;
    }

    private boolean isAlreadyUnlocked(User user, Achievement achievement) {
        return userAchievementRepository
                .existsByUserIdAndAchievementIdAndUnlockedAtIsNotNull(user.getId(), achievement.getId());
    }

    private double calculateCurrentValue(User user, Achievement achievement) {
        LocalDate from = achievement.getValidFrom();
        LocalDate until = achievement.getValidUntil();
        boolean timeBound = achievement.isTimeBound();

        switch (achievement.getCategory()) {
            case DISTANCE:
                return calculateDistance(user, from, until, timeBound);
            case STREAK:
                return calculateStreak(user, from, until, timeBound);
            case PR:
                return calculatePR(user, achievement);
            case PLAN_COMPLETION:
                return calculatePlanCompletion(user, achievement);
            default:
                return 0.0;
        }
    }

    private double calculateDistance(User user, LocalDate from, LocalDate until, boolean timeBound) {
        if (timeBound) {
            LocalDate effectiveFrom = from != null ? from : LocalDate.of(2000, 1, 1);
            LocalDate effectiveUntil = until != null ? until : LocalDate.now();
            return completedTrainingRepository.sumDistanceKmByUserIdAndDateBetween(
                    user.getId(), effectiveFrom, effectiveUntil);
        }
        return completedTrainingRepository.sumDistanceKmByUserId(user.getId());
    }

    private double calculateStreak(User user, LocalDate from, LocalDate until, boolean timeBound) {
        List<LocalDate> dates;
        if (timeBound) {
            LocalDate effectiveFrom = from != null ? from : LocalDate.of(2000, 1, 1);
            LocalDate effectiveUntil = until != null ? until : LocalDate.now();
            dates = completedTrainingRepository.findDistinctTrainingDatesByUserIdAndDateBetween(
                    user.getId(), effectiveFrom, effectiveUntil);
        } else {
            dates = completedTrainingRepository
                    .findDistinctTrainingDatesByUserIdOrderByTrainingDateDesc(user.getId());
        }
        int currentStreak = calculateCurrentStreak(dates);
        int longestStreak = calculateLongestStreak(dates);
        return Math.max(currentStreak, longestStreak);
    }

    private double calculatePR(User user, Achievement achievement) {
        String key = achievement.getKey();
        long totalPREntries = personalRecordEntryRepository.countByUserId(user.getId());
        // Match by key prefix for known PR types
        if (key.startsWith("first_pr")) {
            return totalPREntries > 0 ? 1.0 : 0.0;
        } else if (key.startsWith("pr_all_distances")) {
            return personalRecordEntryRepository.countDistinctPersonalRecordsByUserId(user.getId());
        } else if (key.startsWith("pr_10_broken")) {
            return totalPREntries;
        }
        return totalPREntries;
    }

    private double calculatePlanCompletion(User user, Achievement achievement) {
        try {
            List<CompetitionRegistration> registrations = getActiveRegistrations(user);
            String key = achievement.getKey();

            if (key.startsWith("week_100_pct")) {
                for (CompetitionRegistration reg : registrations) {
                    List<UserTrainingEntry> entries = userTrainingEntryRepository
                            .findByCompetitionRegistrationId(reg.getId());
                    if (entries.isEmpty()) continue;
                    java.util.Map<Integer, List<UserTrainingEntry>> byWeek = new java.util.HashMap<>();
                    for (UserTrainingEntry entry : entries) {
                        byWeek.computeIfAbsent(entry.getWeekNumber(), k -> new ArrayList<>()).add(entry);
                    }
                    for (var weekEntries : byWeek.values()) {
                        if (!weekEntries.isEmpty() && weekEntries.stream().allMatch(e -> Boolean.TRUE.equals(e.getCompleted()))) {
                            return 1.0;
                        }
                    }
                }
                return 0.0;
            } else if (key.startsWith("plan_completed")) {
                for (CompetitionRegistration reg : registrations) {
                    List<UserTrainingEntry> entries = userTrainingEntryRepository
                            .findByCompetitionRegistrationId(reg.getId());
                    if (!entries.isEmpty() && entries.stream().allMatch(e -> Boolean.TRUE.equals(e.getCompleted()))) {
                        return 1.0;
                    }
                }
                return 0.0;
            }
        } catch (Exception e) {
            log.warn("Could not evaluate plan completion: {}", e.getMessage());
        }
        return 0.0;
    }

    private List<CompetitionRegistration> getActiveRegistrations(User user) {
        LocalDate from = LocalDate.now().minusYears(2);
        LocalDate to = LocalDate.now();
        List<UserTrainingEntry> entries = userTrainingEntryRepository
                .findCalendarEntriesForUser(user.getId(), from, to);

        return entries.stream()
                .map(UserTrainingEntry::getCompetitionRegistration)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * Upsert achievement progress for a user. Returns true if the achievement was newly unlocked.
     */
    private boolean upsertProgress(User user, Achievement achievement, double currentValue) {
        UserAchievement ua = userAchievementRepository
                .findByUserIdAndAchievementId(user.getId(), achievement.getId())
                .orElse(null);

        if (ua == null) {
            ua = new UserAchievement(user, achievement, currentValue);
        } else {
            ua.setCurrentValue(currentValue);
        }

        boolean newlyUnlocked = false;
        if (!ua.isUnlocked() && currentValue >= achievement.getThreshold()) {
            ua.unlock();
            newlyUnlocked = true;
            log.info("Achievement unlocked: {} for user {}", achievement.getKey(), user.getId());
        }

        userAchievementRepository.save(ua);
        return newlyUnlocked;
    }

    public int calculateCurrentStreak(List<LocalDate> sortedDatesDesc) {
        if (sortedDatesDesc.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        LocalDate expected = today;

        // Allow streak to start from today or yesterday
        if (!sortedDatesDesc.get(0).equals(today)) {
            if (sortedDatesDesc.get(0).equals(today.minusDays(1))) {
                expected = today.minusDays(1);
            } else {
                return 0;
            }
        }

        int streak = 0;
        for (LocalDate date : sortedDatesDesc) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    public int calculateLongestStreak(List<LocalDate> sortedDatesDesc) {
        if (sortedDatesDesc.isEmpty()) return 0;

        List<LocalDate> ascending = new ArrayList<>(sortedDatesDesc);
        java.util.Collections.reverse(ascending);

        int longest = 1;
        int current = 1;
        for (int i = 1; i < ascending.size(); i++) {
            if (ascending.get(i).equals(ascending.get(i - 1).plusDays(1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }

    public int getCurrentStreakForUser(Long userId) {
        List<LocalDate> dates = completedTrainingRepository
                .findDistinctTrainingDatesByUserIdOrderByTrainingDateDesc(userId);
        return calculateCurrentStreak(dates);
    }

    public int getLongestStreakForUser(Long userId) {
        List<LocalDate> dates = completedTrainingRepository
                .findDistinctTrainingDatesByUserIdOrderByTrainingDateDesc(userId);
        return calculateLongestStreak(dates);
    }
}
