package com.trainingsplan.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AdminAchievementDto {

    private Long id;
    private String key;
    private String name;
    private String description;
    private String icon;
    private String category;
    private String metric;
    private double threshold;
    private int sortOrder;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private boolean timeBound;
    private boolean active;
    private boolean expired;
    private int unlockedCount;
    private int inProgressCount;
    private List<UnlockedUser> unlockedUsers;

    public AdminAchievementDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public boolean isTimeBound() { return timeBound; }
    public void setTimeBound(boolean timeBound) { this.timeBound = timeBound; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }

    public int getUnlockedCount() { return unlockedCount; }
    public void setUnlockedCount(int unlockedCount) { this.unlockedCount = unlockedCount; }

    public int getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(int inProgressCount) { this.inProgressCount = inProgressCount; }

    public List<UnlockedUser> getUnlockedUsers() { return unlockedUsers; }
    public void setUnlockedUsers(List<UnlockedUser> unlockedUsers) { this.unlockedUsers = unlockedUsers; }

    public static class UnlockedUser {
        private Long userId;
        private String username;
        private String firstName;
        private String lastName;
        private double currentValue;
        private LocalDateTime unlockedAt;

        public UnlockedUser() {}

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public double getCurrentValue() { return currentValue; }
        public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

        public LocalDateTime getUnlockedAt() { return unlockedAt; }
        public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
    }
}
