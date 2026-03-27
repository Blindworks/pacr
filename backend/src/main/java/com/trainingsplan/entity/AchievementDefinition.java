package com.trainingsplan.entity;

public enum AchievementDefinition {

    // Distance milestones (total km)
    DISTANCE_50K("distance_50k", "50 km Club", "Run a total of 50 kilometers", "directions_run", AchievementCategory.DISTANCE, 50.0, 1),
    DISTANCE_100K("distance_100k", "Century Runner", "Run a total of 100 kilometers", "sprint", AchievementCategory.DISTANCE, 100.0, 2),
    DISTANCE_500K("distance_500k", "Road Warrior", "Run a total of 500 kilometers", "hiking", AchievementCategory.DISTANCE, 500.0, 3),
    DISTANCE_1000K("distance_1000k", "Thousand Miler", "Run a total of 1000 kilometers", "landscape", AchievementCategory.DISTANCE, 1000.0, 4),

    // Streak achievements (consecutive days)
    STREAK_7("streak_7", "Week Warrior", "Train 7 consecutive days", "local_fire_department", AchievementCategory.STREAK, 7.0, 10),
    STREAK_14("streak_14", "Fortnight Fighter", "Train 14 consecutive days", "whatshot", AchievementCategory.STREAK, 14.0, 11),
    STREAK_30("streak_30", "Monthly Machine", "Train 30 consecutive days", "bolt", AchievementCategory.STREAK, 30.0, 12),
    STREAK_100("streak_100", "Unstoppable", "Train 100 consecutive days", "military_tech", AchievementCategory.STREAK, 100.0, 13),

    // Personal record achievements
    FIRST_PR("first_pr", "Record Breaker", "Set your first personal record", "emoji_events", AchievementCategory.PR, 1.0, 20),
    PR_ALL_DISTANCES("pr_all_distances", "Complete Racer", "Set a PR at every standard distance", "workspace_premium", AchievementCategory.PR, 4.0, 21),
    PR_10_BROKEN("pr_10_broken", "PR Hunter", "Break 10 personal records", "stars", AchievementCategory.PR, 10.0, 22),

    // Plan completion
    WEEK_100_PCT("week_100_pct", "Perfect Week", "Complete 100% of a training week", "task_alt", AchievementCategory.PLAN_COMPLETION, 1.0, 30),
    PLAN_COMPLETED("plan_completed", "Plan Master", "Complete an entire training plan", "verified", AchievementCategory.PLAN_COMPLETION, 1.0, 31);

    private final String key;
    private final String name;
    private final String description;
    private final String icon;
    private final AchievementCategory category;
    private final double threshold;
    private final int sortOrder;

    AchievementDefinition(String key, String name, String description, String icon,
                          AchievementCategory category, double threshold, int sortOrder) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.threshold = threshold;
        this.sortOrder = sortOrder;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public AchievementCategory getCategory() { return category; }
    public double getThreshold() { return threshold; }
    public int getSortOrder() { return sortOrder; }
}
