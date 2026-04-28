package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "group_events")
public class GroupEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    @JsonIgnore
    private User trainer;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "location_name", nullable = false, length = 255)
    private String locationName;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "pace_min_seconds_per_km")
    private Integer paceMinSecondsPerKm;

    @Column(name = "pace_max_seconds_per_km")
    private Integer paceMaxSecondsPerKm;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "cost_cents")
    private Integer costCents;

    @Column(name = "cost_currency", nullable = false, length = 3)
    private String costCurrency = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GroupEventDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupEventStatus status = GroupEventStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String rrule;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "event_image_filename", length = 255)
    private String eventImageFilename;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<GroupEventRegistration> registrations = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<GroupEventException> exceptions = new ArrayList<>();

    public GroupEvent() {}

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getTrainer() { return trainer; }
    public void setTrainer(User trainer) { this.trainer = trainer; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Integer getPaceMinSecondsPerKm() { return paceMinSecondsPerKm; }
    public void setPaceMinSecondsPerKm(Integer paceMinSecondsPerKm) { this.paceMinSecondsPerKm = paceMinSecondsPerKm; }

    public Integer getPaceMaxSecondsPerKm() { return paceMaxSecondsPerKm; }
    public void setPaceMaxSecondsPerKm(Integer paceMaxSecondsPerKm) { this.paceMaxSecondsPerKm = paceMaxSecondsPerKm; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public Integer getCostCents() { return costCents; }
    public void setCostCents(Integer costCents) { this.costCents = costCents; }

    public String getCostCurrency() { return costCurrency; }
    public void setCostCurrency(String costCurrency) { this.costCurrency = costCurrency; }

    public GroupEventDifficulty getDifficulty() { return difficulty; }
    public void setDifficulty(GroupEventDifficulty difficulty) { this.difficulty = difficulty; }

    public GroupEventStatus getStatus() { return status; }
    public void setStatus(GroupEventStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<GroupEventRegistration> getRegistrations() { return registrations; }
    public void setRegistrations(List<GroupEventRegistration> registrations) { this.registrations = registrations; }

    public String getRrule() { return rrule; }
    public void setRrule(String rrule) { this.rrule = rrule; }

    public LocalDate getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) { this.recurrenceEndDate = recurrenceEndDate; }

    public boolean isRecurring() { return rrule != null && !rrule.isBlank(); }

    public List<GroupEventException> getExceptions() { return exceptions; }
    public void setExceptions(List<GroupEventException> exceptions) { this.exceptions = exceptions; }

    public String getEventImageFilename() { return eventImageFilename; }
    public void setEventImageFilename(String eventImageFilename) { this.eventImageFilename = eventImageFilename; }
}
