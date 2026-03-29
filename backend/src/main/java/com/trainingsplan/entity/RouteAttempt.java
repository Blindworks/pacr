package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_attempts")
public class RouteAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @JsonIgnore
    private CommunityRoute route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    @JsonIgnore
    private CompletedTraining activity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.PENDING;

    @Column(name = "time_seconds")
    private Integer timeSeconds;

    @Column(name = "pace_seconds_per_km")
    private Integer paceSecondsPerKm;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RouteAttempt() {}

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CommunityRoute getRoute() { return route; }
    public void setRoute(CommunityRoute route) { this.route = route; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public CompletedTraining getActivity() { return activity; }
    public void setActivity(CompletedTraining activity) { this.activity = activity; }

    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus status) { this.status = status; }

    public Integer getTimeSeconds() { return timeSeconds; }
    public void setTimeSeconds(Integer timeSeconds) { this.timeSeconds = timeSeconds; }

    public Integer getPaceSecondsPerKm() { return paceSecondsPerKm; }
    public void setPaceSecondsPerKm(Integer paceSecondsPerKm) { this.paceSecondsPerKm = paceSecondsPerKm; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
