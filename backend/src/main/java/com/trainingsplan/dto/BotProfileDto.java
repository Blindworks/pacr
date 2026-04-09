package com.trainingsplan.dto;

import com.trainingsplan.entity.BotProfile;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

/** Read DTO for admin bot runner listing / detail. */
public class BotProfileDto {

    public Long id;
    public Long userId;
    public String username;
    public String email;
    public String firstName;
    public String lastName;

    public String cityName;
    public Double homeLatitude;
    public Double homeLongitude;
    public Double searchRadiusKm;

    public String gender;
    public Integer age;

    public Integer paceMinSecPerKm;
    public Integer paceMaxSecPerKm;
    public Double distanceMinKm;
    public Double distanceMaxKm;

    public Integer maxHeartRate;
    public Integer restingHeartRate;

    public Set<String> scheduleDays;
    public LocalTime scheduleStartTime;
    public Integer scheduleJitterMinutes;

    public LocalDateTime nextScheduledRunAt;
    public LocalDateTime lastRunAt;
    public String lastRunStatus;
    public String lastRunMessage;

    public boolean includeInLeaderboard;
    public boolean enabled;

    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public static BotProfileDto from(BotProfile b) {
        BotProfileDto dto = new BotProfileDto();
        dto.id = b.getId();
        if (b.getUser() != null) {
            dto.userId = b.getUser().getId();
            dto.username = b.getUser().getUsername();
            dto.email = b.getUser().getEmail();
            dto.firstName = b.getUser().getFirstName();
            dto.lastName = b.getUser().getLastName();
        }
        dto.cityName = b.getCityName();
        dto.homeLatitude = b.getHomeLatitude();
        dto.homeLongitude = b.getHomeLongitude();
        dto.searchRadiusKm = b.getSearchRadiusKm();
        dto.gender = b.getGender();
        dto.age = b.getAge();
        dto.paceMinSecPerKm = b.getPaceMinSecPerKm();
        dto.paceMaxSecPerKm = b.getPaceMaxSecPerKm();
        dto.distanceMinKm = b.getDistanceMinKm();
        dto.distanceMaxKm = b.getDistanceMaxKm();
        dto.maxHeartRate = b.getMaxHeartRate();
        dto.restingHeartRate = b.getRestingHeartRate();
        dto.scheduleDays = b.getScheduleDaySet().stream()
                .map(Enum::name).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        dto.scheduleStartTime = b.getScheduleStartTime();
        dto.scheduleJitterMinutes = b.getScheduleJitterMinutes();
        dto.nextScheduledRunAt = b.getNextScheduledRunAt();
        dto.lastRunAt = b.getLastRunAt();
        dto.lastRunStatus = b.getLastRunStatus();
        dto.lastRunMessage = b.getLastRunMessage();
        dto.includeInLeaderboard = b.isIncludeInLeaderboard();
        dto.enabled = b.isEnabled();
        dto.createdAt = b.getCreatedAt();
        dto.updatedAt = b.getUpdatedAt();
        return dto;
    }
}
