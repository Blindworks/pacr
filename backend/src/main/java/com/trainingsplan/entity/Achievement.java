package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "achievement_key", nullable = false, unique = true, length = 50)
    private String key;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 50)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AchievementCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AchievementMetric metric;

    @Column(nullable = false)
    private double threshold;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    public Achievement() {}

    public Achievement(String key, String name, String description, String icon,
                       AchievementCategory category, AchievementMetric metric,
                       double threshold, int sortOrder) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.metric = metric;
        this.threshold = threshold;
        this.sortOrder = sortOrder;
    }

    public boolean isTimeBound() {
        return validFrom != null || validUntil != null;
    }

    public boolean isActive() {
        LocalDate today = LocalDate.now();
        if (validFrom != null && today.isBefore(validFrom)) return false;
        if (validUntil != null && today.isAfter(validUntil)) return false;
        return true;
    }

    public boolean isExpired() {
        return validUntil != null && LocalDate.now().isAfter(validUntil);
    }

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

    public AchievementCategory getCategory() { return category; }
    public void setCategory(AchievementCategory category) { this.category = category; }

    public AchievementMetric getMetric() { return metric; }
    public void setMetric(AchievementMetric metric) { this.metric = metric; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
}
