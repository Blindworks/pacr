package com.trainingsplan.entity;

/**
 * Defines what is actually measured for an achievement. Decouples evaluation
 * logic from the achievement key so admins can create new achievements via the
 * UI without code changes.
 */
public enum AchievementMetric {
    TOTAL_DISTANCE_KM(AchievementCategory.DISTANCE),
    STREAK_DAYS(AchievementCategory.STREAK),
    PR_TOTAL_COUNT(AchievementCategory.PR),
    PR_DISTINCT_DISTANCES(AchievementCategory.PR),
    PERFECT_WEEKS_COUNT(AchievementCategory.PLAN_COMPLETION),
    COMPLETED_PLANS_COUNT(AchievementCategory.PLAN_COMPLETION);

    private final AchievementCategory category;

    AchievementMetric(AchievementCategory category) {
        this.category = category;
    }

    public AchievementCategory getCategory() {
        return category;
    }
}
