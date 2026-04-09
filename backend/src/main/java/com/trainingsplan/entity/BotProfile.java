package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bot runner configuration. 1:1 to a {@link User} that has {@code isBot=true}.
 * The scheduler uses {@code nextScheduledRunAt} + {@code enabled} to find due bots.
 */
@Entity
@Table(name = "bot_profiles")
public class BotProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "city_name", length = 100)
    private String cityName;

    @Column(name = "home_latitude", nullable = false)
    private Double homeLatitude;

    @Column(name = "home_longitude", nullable = false)
    private Double homeLongitude;

    @Column(name = "search_radius_km", nullable = false)
    private Double searchRadiusKm = 10.0;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "age")
    private Integer age;

    @Column(name = "pace_min_sec_per_km", nullable = false)
    private Integer paceMinSecPerKm;

    @Column(name = "pace_max_sec_per_km", nullable = false)
    private Integer paceMaxSecPerKm;

    @Column(name = "distance_min_km", nullable = false)
    private Double distanceMinKm;

    @Column(name = "distance_max_km", nullable = false)
    private Double distanceMaxKm;

    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Column(name = "resting_heart_rate")
    private Integer restingHeartRate;

    /** CSV of {@link DayOfWeek} names, e.g. "MONDAY,WEDNESDAY,FRIDAY". */
    @Column(name = "schedule_days", length = 100)
    private String scheduleDays;

    @Column(name = "schedule_start_time")
    private LocalTime scheduleStartTime;

    @Column(name = "schedule_jitter_minutes", nullable = false)
    private Integer scheduleJitterMinutes = 15;

    @Column(name = "next_scheduled_run_at")
    private LocalDateTime nextScheduledRunAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "last_run_status", length = 30)
    private String lastRunStatus;

    @Column(name = "last_run_message", length = 500)
    private String lastRunMessage;

    @Column(name = "include_in_leaderboard", nullable = false)
    private boolean includeInLeaderboard = false;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BotProfile() {}

    /** Parses {@link #scheduleDays} into a set. Returns empty set when null/blank. */
    @Transient
    public Set<DayOfWeek> getScheduleDaySet() {
        if (scheduleDays == null || scheduleDays.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return Arrays.stream(scheduleDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    public void setScheduleDaySet(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            this.scheduleDays = null;
        } else {
            this.scheduleDays = days.stream().map(DayOfWeek::name).collect(Collectors.joining(","));
        }
    }

    // Getters / Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public Double getHomeLatitude() { return homeLatitude; }
    public void setHomeLatitude(Double homeLatitude) { this.homeLatitude = homeLatitude; }

    public Double getHomeLongitude() { return homeLongitude; }
    public void setHomeLongitude(Double homeLongitude) { this.homeLongitude = homeLongitude; }

    public Double getSearchRadiusKm() { return searchRadiusKm; }
    public void setSearchRadiusKm(Double searchRadiusKm) { this.searchRadiusKm = searchRadiusKm; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Integer getPaceMinSecPerKm() { return paceMinSecPerKm; }
    public void setPaceMinSecPerKm(Integer paceMinSecPerKm) { this.paceMinSecPerKm = paceMinSecPerKm; }

    public Integer getPaceMaxSecPerKm() { return paceMaxSecPerKm; }
    public void setPaceMaxSecPerKm(Integer paceMaxSecPerKm) { this.paceMaxSecPerKm = paceMaxSecPerKm; }

    public Double getDistanceMinKm() { return distanceMinKm; }
    public void setDistanceMinKm(Double distanceMinKm) { this.distanceMinKm = distanceMinKm; }

    public Double getDistanceMaxKm() { return distanceMaxKm; }
    public void setDistanceMaxKm(Double distanceMaxKm) { this.distanceMaxKm = distanceMaxKm; }

    public Integer getMaxHeartRate() { return maxHeartRate; }
    public void setMaxHeartRate(Integer maxHeartRate) { this.maxHeartRate = maxHeartRate; }

    public Integer getRestingHeartRate() { return restingHeartRate; }
    public void setRestingHeartRate(Integer restingHeartRate) { this.restingHeartRate = restingHeartRate; }

    public String getScheduleDays() { return scheduleDays; }
    public void setScheduleDays(String scheduleDays) { this.scheduleDays = scheduleDays; }

    public LocalTime getScheduleStartTime() { return scheduleStartTime; }
    public void setScheduleStartTime(LocalTime scheduleStartTime) { this.scheduleStartTime = scheduleStartTime; }

    public Integer getScheduleJitterMinutes() { return scheduleJitterMinutes; }
    public void setScheduleJitterMinutes(Integer scheduleJitterMinutes) {
        this.scheduleJitterMinutes = scheduleJitterMinutes == null ? 0 : scheduleJitterMinutes;
    }

    public LocalDateTime getNextScheduledRunAt() { return nextScheduledRunAt; }
    public void setNextScheduledRunAt(LocalDateTime nextScheduledRunAt) { this.nextScheduledRunAt = nextScheduledRunAt; }

    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }

    public String getLastRunStatus() { return lastRunStatus; }
    public void setLastRunStatus(String lastRunStatus) { this.lastRunStatus = lastRunStatus; }

    public String getLastRunMessage() { return lastRunMessage; }
    public void setLastRunMessage(String lastRunMessage) { this.lastRunMessage = lastRunMessage; }

    public boolean isIncludeInLeaderboard() { return includeInLeaderboard; }
    public void setIncludeInLeaderboard(boolean includeInLeaderboard) { this.includeInLeaderboard = includeInLeaderboard; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
