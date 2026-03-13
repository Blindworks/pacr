package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cycle_settings")
public class CycleSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "first_day_of_last_period", nullable = false)
    private LocalDate firstDayOfLastPeriod;

    @Column(name = "average_cycle_length", nullable = false)
    private Integer averageCycleLength = 28;

    @Column(name = "average_period_duration", nullable = false)
    private Integer averagePeriodDuration = 5;

    public CycleSettings() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getFirstDayOfLastPeriod() { return firstDayOfLastPeriod; }
    public void setFirstDayOfLastPeriod(LocalDate firstDayOfLastPeriod) { this.firstDayOfLastPeriod = firstDayOfLastPeriod; }

    public Integer getAverageCycleLength() { return averageCycleLength; }
    public void setAverageCycleLength(Integer averageCycleLength) { this.averageCycleLength = averageCycleLength; }

    public Integer getAveragePeriodDuration() { return averagePeriodDuration; }
    public void setAveragePeriodDuration(Integer averagePeriodDuration) { this.averagePeriodDuration = averagePeriodDuration; }
}
