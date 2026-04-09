package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_routes")
public class CommunityRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonIgnore
    private User creator;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_activity_id", nullable = true, unique = true)
    @JsonIgnore
    private CompletedTraining sourceActivity;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "elevation_gain_m")
    private Integer elevationGainM;

    @Column(name = "start_latitude", nullable = false)
    private Double startLatitude;

    @Column(name = "start_longitude", nullable = false)
    private Double startLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RouteVisibility visibility = RouteVisibility.PUBLIC;

    @Column(name = "gps_track_json", columnDefinition = "LONGTEXT", nullable = false)
    private String gpsTrackJson;

    @Column(name = "location_city", length = 255)
    private String locationCity;

    @Column(name = "admin_uploaded", nullable = false)
    private boolean adminUploaded = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CommunityRoute() {}

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public CompletedTraining getSourceActivity() { return sourceActivity; }
    public void setSourceActivity(CompletedTraining sourceActivity) { this.sourceActivity = sourceActivity; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Integer getElevationGainM() { return elevationGainM; }
    public void setElevationGainM(Integer elevationGainM) { this.elevationGainM = elevationGainM; }

    public Double getStartLatitude() { return startLatitude; }
    public void setStartLatitude(Double startLatitude) { this.startLatitude = startLatitude; }

    public Double getStartLongitude() { return startLongitude; }
    public void setStartLongitude(Double startLongitude) { this.startLongitude = startLongitude; }

    public RouteVisibility getVisibility() { return visibility; }
    public void setVisibility(RouteVisibility visibility) { this.visibility = visibility; }

    public String getGpsTrackJson() { return gpsTrackJson; }
    public void setGpsTrackJson(String gpsTrackJson) { this.gpsTrackJson = gpsTrackJson; }

    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }

    public boolean isAdminUploaded() { return adminUploaded; }
    public void setAdminUploaded(boolean adminUploaded) { this.adminUploaded = adminUploaded; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
