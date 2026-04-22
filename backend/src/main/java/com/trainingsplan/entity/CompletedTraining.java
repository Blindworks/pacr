package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "completed_trainings")
public class CompletedTraining {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "training_date", nullable = false)
    private LocalDate trainingDate;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    // Basic Training Metrics
    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "moving_time_seconds")
    private Integer movingTimeSeconds;

    // Pace and Speed
    @Column(name = "average_pace_seconds_per_km")
    private Integer averagePaceSecondsPerKm;

    @Column(name = "average_speed_kmh")
    private Double averageSpeedKmh;

    @Column(name = "max_speed_kmh")
    private Double maxSpeedKmh;

    // Heart Rate Data
    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Column(name = "min_heart_rate")
    private Integer minHeartRate;

    // Elevation Data
    @Column(name = "elevation_gain_m")
    private Integer elevationGainM;

    @Column(name = "elevation_loss_m")
    private Integer elevationLossM;

    @Column(name = "min_elevation_m")
    private Double minElevationM;

    @Column(name = "max_elevation_m")
    private Double maxElevationM;

    // Power Data (für Radfahren)
    @Column(name = "average_power_watts")
    private Integer averagePowerWatts;

    @Column(name = "max_power_watts")
    private Integer maxPowerWatts;

    @Column(name = "normalized_power_watts")
    private Integer normalizedPowerWatts;

    // Cadence Data
    @Column(name = "average_cadence")
    private Integer averageCadence;

    @Column(name = "max_cadence")
    private Integer maxCadence;

    // Temperature
    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    // Calories
    @Column(name = "calories")
    private Integer calories;

    // Training Zones (Zeit in verschiedenen HR-Zonen)
    @Column(name = "time_in_hr_zone_1_seconds")
    private Integer timeInHrZone1Seconds;

    @Column(name = "time_in_hr_zone_2_seconds")
    private Integer timeInHrZone2Seconds;

    @Column(name = "time_in_hr_zone_3_seconds")
    private Integer timeInHrZone3Seconds;

    @Column(name = "time_in_hr_zone_4_seconds")
    private Integer timeInHrZone4Seconds;

    @Column(name = "time_in_hr_zone_5_seconds")
    private Integer timeInHrZone5Seconds;

    // Lap Data
    @Column(name = "total_laps")
    private Integer totalLaps;

    @Column(name = "best_lap_time_seconds")
    private Integer bestLapTimeSeconds;

    // File Information
    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "device_manufacturer")
    private String deviceManufacturer;

    @Column(name = "device_product")
    private String deviceProduct;

    @Column(name = "device_serial_number")
    private String deviceSerialNumber;

    @Column(name = "software_version")
    private String softwareVersion;

    // Sport Type
    @Column(name = "sport")
    private String sport;

    @Column(name = "sub_sport")
    private String subSport;

    // Training Type (user-defined label, e.g. "Langer Lauf", "Intervalle")
    @Column(name = "training_type")
    private String trainingType;

    // Source tracking
    @Column(name = "source", nullable = false)
    private String source = "FIT_FILE";

    @Column(name = "strava_activity_id", unique = true)
    private Long stravaActivityId;

    @Column(name = "coros_label_id", unique = true, length = 64)
    private String corosLabelId;

    @Column(name = "activity_name")
    private String activityName;

    // GPS Data Quality
    @Column(name = "total_gps_points")
    private Integer totalGpsPoints;

    @Column(name = "start_latitude")
    private Double startLatitude;

    @Column(name = "start_longitude")
    private Double startLongitude;

    @Column(name = "end_latitude")
    private Double endLatitude;

    @Column(name = "end_longitude")
    private Double endLongitude;

    // Quality indicators for VO2max aggregation (see Vo2MaxAggregationService)
    @Column(name = "quality_ok")
    private Boolean qualityOk;

    @Column(name = "hr_coverage_percent")
    private Double hrCoveragePercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    public CompletedTraining() {
        this.uploadDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getTrainingDate() {
        return trainingDate;
    }

    public void setTrainingDate(LocalDate trainingDate) {
        this.trainingDate = trainingDate;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getMovingTimeSeconds() {
        return movingTimeSeconds;
    }

    public void setMovingTimeSeconds(Integer movingTimeSeconds) {
        this.movingTimeSeconds = movingTimeSeconds;
    }

    public Integer getAveragePaceSecondsPerKm() {
        return averagePaceSecondsPerKm;
    }

    public void setAveragePaceSecondsPerKm(Integer averagePaceSecondsPerKm) {
        this.averagePaceSecondsPerKm = averagePaceSecondsPerKm;
    }

    public Double getAverageSpeedKmh() {
        return averageSpeedKmh;
    }

    public void setAverageSpeedKmh(Double averageSpeedKmh) {
        this.averageSpeedKmh = averageSpeedKmh;
    }

    public Double getMaxSpeedKmh() {
        return maxSpeedKmh;
    }

    public void setMaxSpeedKmh(Double maxSpeedKmh) {
        this.maxSpeedKmh = maxSpeedKmh;
    }

    public Integer getAverageHeartRate() {
        return averageHeartRate;
    }

    public void setAverageHeartRate(Integer averageHeartRate) {
        this.averageHeartRate = averageHeartRate;
    }

    public Integer getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(Integer maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public Integer getMinHeartRate() {
        return minHeartRate;
    }

    public void setMinHeartRate(Integer minHeartRate) {
        this.minHeartRate = minHeartRate;
    }

    public Integer getElevationGainM() {
        return elevationGainM;
    }

    public void setElevationGainM(Integer elevationGainM) {
        this.elevationGainM = elevationGainM;
    }

    public Integer getElevationLossM() {
        return elevationLossM;
    }

    public void setElevationLossM(Integer elevationLossM) {
        this.elevationLossM = elevationLossM;
    }

    public Double getMinElevationM() {
        return minElevationM;
    }

    public void setMinElevationM(Double minElevationM) {
        this.minElevationM = minElevationM;
    }

    public Double getMaxElevationM() {
        return maxElevationM;
    }

    public void setMaxElevationM(Double maxElevationM) {
        this.maxElevationM = maxElevationM;
    }

    public Integer getAveragePowerWatts() {
        return averagePowerWatts;
    }

    public void setAveragePowerWatts(Integer averagePowerWatts) {
        this.averagePowerWatts = averagePowerWatts;
    }

    public Integer getMaxPowerWatts() {
        return maxPowerWatts;
    }

    public void setMaxPowerWatts(Integer maxPowerWatts) {
        this.maxPowerWatts = maxPowerWatts;
    }

    public Integer getNormalizedPowerWatts() {
        return normalizedPowerWatts;
    }

    public void setNormalizedPowerWatts(Integer normalizedPowerWatts) {
        this.normalizedPowerWatts = normalizedPowerWatts;
    }

    public Integer getAverageCadence() {
        return averageCadence;
    }

    public void setAverageCadence(Integer averageCadence) {
        this.averageCadence = averageCadence;
    }

    public Integer getMaxCadence() {
        return maxCadence;
    }

    public void setMaxCadence(Integer maxCadence) {
        this.maxCadence = maxCadence;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Integer getTimeInHrZone1Seconds() {
        return timeInHrZone1Seconds;
    }

    public void setTimeInHrZone1Seconds(Integer timeInHrZone1Seconds) {
        this.timeInHrZone1Seconds = timeInHrZone1Seconds;
    }

    public Integer getTimeInHrZone2Seconds() {
        return timeInHrZone2Seconds;
    }

    public void setTimeInHrZone2Seconds(Integer timeInHrZone2Seconds) {
        this.timeInHrZone2Seconds = timeInHrZone2Seconds;
    }

    public Integer getTimeInHrZone3Seconds() {
        return timeInHrZone3Seconds;
    }

    public void setTimeInHrZone3Seconds(Integer timeInHrZone3Seconds) {
        this.timeInHrZone3Seconds = timeInHrZone3Seconds;
    }

    public Integer getTimeInHrZone4Seconds() {
        return timeInHrZone4Seconds;
    }

    public void setTimeInHrZone4Seconds(Integer timeInHrZone4Seconds) {
        this.timeInHrZone4Seconds = timeInHrZone4Seconds;
    }

    public Integer getTimeInHrZone5Seconds() {
        return timeInHrZone5Seconds;
    }

    public void setTimeInHrZone5Seconds(Integer timeInHrZone5Seconds) {
        this.timeInHrZone5Seconds = timeInHrZone5Seconds;
    }

    public Integer getTotalLaps() {
        return totalLaps;
    }

    public void setTotalLaps(Integer totalLaps) {
        this.totalLaps = totalLaps;
    }

    public Integer getBestLapTimeSeconds() {
        return bestLapTimeSeconds;
    }

    public void setBestLapTimeSeconds(Integer bestLapTimeSeconds) {
        this.bestLapTimeSeconds = bestLapTimeSeconds;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getDeviceManufacturer() {
        return deviceManufacturer;
    }

    public void setDeviceManufacturer(String deviceManufacturer) {
        this.deviceManufacturer = deviceManufacturer;
    }

    public String getDeviceProduct() {
        return deviceProduct;
    }

    public void setDeviceProduct(String deviceProduct) {
        this.deviceProduct = deviceProduct;
    }

    public String getDeviceSerialNumber() {
        return deviceSerialNumber;
    }

    public void setDeviceSerialNumber(String deviceSerialNumber) {
        this.deviceSerialNumber = deviceSerialNumber;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getSubSport() {
        return subSport;
    }

    public void setSubSport(String subSport) {
        this.subSport = subSport;
    }

    public Integer getTotalGpsPoints() {
        return totalGpsPoints;
    }

    public void setTotalGpsPoints(Integer totalGpsPoints) {
        this.totalGpsPoints = totalGpsPoints;
    }

    public Double getStartLatitude() {
        return startLatitude;
    }

    public void setStartLatitude(Double startLatitude) {
        this.startLatitude = startLatitude;
    }

    public Double getStartLongitude() {
        return startLongitude;
    }

    public void setStartLongitude(Double startLongitude) {
        this.startLongitude = startLongitude;
    }

    public Double getEndLatitude() {
        return endLatitude;
    }

    public void setEndLatitude(Double endLatitude) {
        this.endLatitude = endLatitude;
    }

    public Double getEndLongitude() {
        return endLongitude;
    }

    public void setEndLongitude(Double endLongitude) {
        this.endLongitude = endLongitude;
    }

    public String getTrainingType() {
        return trainingType;
    }

    public void setTrainingType(String trainingType) {
        this.trainingType = trainingType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getStravaActivityId() {
        return stravaActivityId;
    }

    public void setStravaActivityId(Long stravaActivityId) {
        this.stravaActivityId = stravaActivityId;
    }

    public String getCorosLabelId() {
        return corosLabelId;
    }

    public void setCorosLabelId(String corosLabelId) {
        this.corosLabelId = corosLabelId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getQualityOk() {
        return qualityOk;
    }

    public void setQualityOk(Boolean qualityOk) {
        this.qualityOk = qualityOk;
    }

    public Double getHrCoveragePercent() {
        return hrCoveragePercent;
    }

    public void setHrCoveragePercent(Double hrCoveragePercent) {
        this.hrCoveragePercent = hrCoveragePercent;
    }
}