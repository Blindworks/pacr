package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Stores raw Strava stream data for a completed training activity.
 * One-to-one with {@link CompletedTraining}.
 *
 * All JSON fields store serialized arrays as returned by the Strava Streams API.
 * Fields are NULL when the corresponding stream was not available for the activity.
 */
@Entity
@Table(name = "activity_streams")
public class ActivityStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_training_id", nullable = false, unique = true)
    private CompletedTraining completedTraining;

    /** Serialized int[] of elapsed time in seconds since activity start. */
    @Column(name = "time_seconds_json", columnDefinition = "LONGTEXT")
    private String timeSecondsJson;

    /** Serialized Integer[] of heart rate values in bpm (may contain nulls for dropouts). */
    @Column(name = "heartrate_json", columnDefinition = "LONGTEXT")
    private String heartrateJson;

    /** Serialized Double[] of smoothed velocity values in m/s. */
    @Column(name = "velocity_smooth_json", columnDefinition = "LONGTEXT")
    private String velocitySmoothJson;

    /** Serialized Double[] of altitude values in meters. */
    @Column(name = "altitude_json", columnDefinition = "LONGTEXT")
    private String altitudeJson;

    /** Serialized Double[] of distance values in meters from activity start. */
    @Column(name = "distance_json", columnDefinition = "LONGTEXT")
    private String distanceJson;

    /** Serialized latlng stream node as returned by Strava (array of [lat, lng] pairs). */
    @Column(name = "latlng_json", columnDefinition = "LONGTEXT")
    private String latlngJson;

    /** Serialized Integer[] of cadence values in spm/rpm (may contain nulls). */
    @Column(name = "cadence_json", columnDefinition = "LONGTEXT")
    private String cadenceJson;

    /** Serialized Integer[] of power values in watts (may contain nulls). */
    @Column(name = "power_json", columnDefinition = "LONGTEXT")
    private String powerJson;

    /** Timestamp of when this stream data was fetched from Strava. */
    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

    public ActivityStream() {}

    // Getters and setters

    public Long getId() { return id; }

    public CompletedTraining getCompletedTraining() { return completedTraining; }
    public void setCompletedTraining(CompletedTraining completedTraining) {
        this.completedTraining = completedTraining;
    }

    public String getTimeSecondsJson() { return timeSecondsJson; }
    public void setTimeSecondsJson(String timeSecondsJson) { this.timeSecondsJson = timeSecondsJson; }

    public String getHeartrateJson() { return heartrateJson; }
    public void setHeartrateJson(String heartrateJson) { this.heartrateJson = heartrateJson; }

    public String getVelocitySmoothJson() { return velocitySmoothJson; }
    public void setVelocitySmoothJson(String velocitySmoothJson) { this.velocitySmoothJson = velocitySmoothJson; }

    public String getAltitudeJson() { return altitudeJson; }
    public void setAltitudeJson(String altitudeJson) { this.altitudeJson = altitudeJson; }

    public String getDistanceJson() { return distanceJson; }
    public void setDistanceJson(String distanceJson) { this.distanceJson = distanceJson; }

    public String getLatlngJson() { return latlngJson; }
    public void setLatlngJson(String latlngJson) { this.latlngJson = latlngJson; }

    public String getCadenceJson() { return cadenceJson; }
    public void setCadenceJson(String cadenceJson) { this.cadenceJson = cadenceJson; }

    public String getPowerJson() { return powerJson; }
    public void setPowerJson(String powerJson) { this.powerJson = powerJson; }

    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
}
