package com.trainingsplan.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AchievementDto {

    private Long id;
    private String key;
    private String name;
    private String description;
    private String icon;
    private String category;
    private double threshold;
    private Double currentValue;
    private boolean unlocked;
    private LocalDateTime unlockedAt;
    private double progress;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private boolean timeBound;
    private boolean expired;
    private boolean active;

    public AchievementDto() {}

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

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }

    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public boolean isTimeBound() { return timeBound; }
    public void setTimeBound(boolean timeBound) { this.timeBound = timeBound; }

    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
