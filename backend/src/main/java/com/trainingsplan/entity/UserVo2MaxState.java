package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_vo2max_state")
public class UserVo2MaxState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "vo2max_internal", nullable = false)
    private Double vo2maxInternal;

    @Column(name = "vo2max_displayed", nullable = false)
    private Integer vo2maxDisplayed;

    @Column(name = "eligible_workout_count", nullable = false)
    private Integer eligibleWorkoutCount;

    @Column(name = "last_update_at", nullable = false)
    private LocalDateTime lastUpdateAt;

    @Column(name = "source_activity_id")
    private Long sourceActivityId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public UserVo2MaxState() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Double getVo2maxInternal() { return vo2maxInternal; }
    public void setVo2maxInternal(Double v) { this.vo2maxInternal = v; }
    public Integer getVo2maxDisplayed() { return vo2maxDisplayed; }
    public void setVo2maxDisplayed(Integer v) { this.vo2maxDisplayed = v; }
    public Integer getEligibleWorkoutCount() { return eligibleWorkoutCount; }
    public void setEligibleWorkoutCount(Integer c) { this.eligibleWorkoutCount = c; }
    public LocalDateTime getLastUpdateAt() { return lastUpdateAt; }
    public void setLastUpdateAt(LocalDateTime t) { this.lastUpdateAt = t; }
    public Long getSourceActivityId() { return sourceActivityId; }
    public void setSourceActivityId(Long id) { this.sourceActivityId = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
