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
        List<Achievement> newlyUnlocked = new ArrayList<>();
        newlyUnlocked.addAll(evaluateDistanceMilestones(user));
        newlyUnlocked.addAll(evaluateStreaks(user));
        newlyUnlocked.addAll(evaluatePRs(user));
        newlyUnlocked.addAll(evaluatePlanCompletion(user));
        return newlyUnlocked;
    }

    private List<Achievement> evaluateDistanceMilestones(User user) {
        List<Achievement> unlocked = new ArrayList<>();
        double totalKm = completedTrainingRepository.sumDistanceKmByUserId(user.getId());

        for (AchievementDefinition def : AchievementDefinition.values()) {
            if (def.getCategory() != AchievementCategory.DISTANCE) continue;
            if (upsertProgress(user, def, totalKm)) {
                achievementRepository.findByKey(def.getKey()).ifPresent(unlocked::add);
            }
        }
        return unlocked;
    }

    private List<Achievement> evaluateStreaks(User user) {
        List<Achievement> unlocked = new ArrayList<>();
        List<LocalDate> dates = completedTrainingRepository
                .findDistinctTrainingDatesByUserIdOrderByTrainingDateDesc(user.getId());

        int currentStreak = calculateCurrentStreak(dates);
        int longestStreak = calculateLongestStreak(dates);
        int bestStreak = Math.max(currentStreak, longestStreak);

        for (AchievementDefinition def : AchievementDefinition.values()) {
            if (def.getCategory() != AchievementCategory.STREAK) continue;
            if (upsertProgress(user, def, bestStreak)) {
                achievementRepository.findByKey(def.getKey()).ifPresent(unlocked::add);
            }
        }
        return unlocked;
    }

    private List<Achievement> evaluatePRs(User user) {
        List<Achievement> unlocked = new ArrayList<>();
        long totalPREntries = personalRecordEntryRepository.countByUserId(user.getId());
        List<PersonalRecord> records = personalRecordRepository.findByUserIdOrderByDistanceKmAsc(user.getId());
        long distinctRecordsWithEntries = personalRecordEntryRepository.countDistinctPersonalRecordsByUserId(user.getId());

        for (AchievementDefinition def : AchievementDefinition.values()) {
            if (def.getCategory() != AchievementCategory.PR) continue;

            double currentValue;
            switch (def) {
                case FIRST_PR:
                    currentValue = totalPREntries > 0 ? 1.0 : 0.0;
                    break;
                case PR_ALL_DISTANCES:
                    currentValue = distinctRecordsWithEntries;
                    break;
                case PR_10_BROKEN:
                    currentValue = totalPREntries;
                    break;
                default:
                    continue;
            }

            if (upsertProgress(user, def, currentValue)) {
                achievementRepository.findByKey(def.getKey()).ifPresent(unlocked::add);
            }
        }
        return unlocked;
    }

    private List<Achievement> evaluatePlanCompletion(User user) {
        List<Achievement> unlocked = new ArrayList<>();

        // Check for perfect weeks: find any week where all entries are completed
        // and for full plan completion
        try {
            List<CompetitionRegistration> registrations = getActiveRegistrations(user);
            boolean hasCompletedWeek = false;
            boolean hasCompletedPlan = false;

            for (CompetitionRegistration reg : registrations) {
                List<UserTrainingEntry> entries = userTrainingEntryRepository
                        .findByCompetitionRegistrationId(reg.getId());

                if (entries.isEmpty()) continue;

                // Group by week and check for 100% week completion
                java.util.Map<Integer, List<UserTrainingEntry>> byWeek = new java.util.HashMap<>();
                for (UserTrainingEntry entry : entries) {
                    byWeek.computeIfAbsent(entry.getWeekNumber(), k -> new ArrayList<>()).add(entry);
                }

                boolean allWeeksComplete = true;
                for (var weekEntries : byWeek.values()) {
                    boolean weekComplete = weekEntries.stream().allMatch(e -> Boolean.TRUE.equals(e.getCompleted()));
                    if (weekComplete && !weekEntries.isEmpty()) {
                        hasCompletedWeek = true;
                    } else {
                        allWeeksComplete = false;
                    }
                }
                if (allWeeksComplete && !entries.isEmpty()) {
                    hasCompletedPlan = true;
                }
            }

            if (upsertProgress(user, AchievementDefinition.WEEK_100_PCT, hasCompletedWeek ? 1.0 : 0.0)) {
                achievementRepository.findByKey(AchievementDefinition.WEEK_100_PCT.getKey()).ifPresent(unlocked::add);
            }
            if (upsertProgress(user, AchievementDefinition.PLAN_COMPLETED, hasCompletedPlan ? 1.0 : 0.0)) {
                achievementRepository.findByKey(AchievementDefinition.PLAN_COMPLETED.getKey()).ifPresent(unlocked::add);
            }
        } catch (Exception e) {
            log.warn("Could not evaluate plan completion: {}", e.getMessage());
        }
        return unlocked;
    }

    private List<CompetitionRegistration> getActiveRegistrations(User user) {
        // Use the entries query to find registrations indirectly
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
    private boolean upsertProgress(User user, AchievementDefinition def, double currentValue) {
        Achievement achievement = achievementRepository.findByKey(def.getKey()).orElse(null);
        if (achievement == null) return false;

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
            log.info("Achievement unlocked: {} for user {}", def.getKey(), user.getId());
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

        // Reverse to ascending order for simpler iteration
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
