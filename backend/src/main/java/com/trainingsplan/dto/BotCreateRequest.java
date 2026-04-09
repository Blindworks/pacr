package com.trainingsplan.dto;

import java.time.LocalTime;
import java.util.Set;

/** Write payload for creating / updating a bot runner. */
public class BotCreateRequest {

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

    /** Day-of-week names, e.g. ["MONDAY","WEDNESDAY"]. */
    public Set<String> scheduleDays;
    public LocalTime scheduleStartTime;
    public Integer scheduleJitterMinutes;

    public Boolean includeInLeaderboard;
    public Boolean enabled;
}
