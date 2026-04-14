package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "training_steps")
public class TrainingStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_id", nullable = false)
    @JsonIgnore
    private Training training;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    @JsonIgnore
    private TrainingStepBlock block;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "block_sort_order")
    private Integer blockSortOrder;

    @Column(name = "step_type", length = 20)
    private String stepType;

    @Column
    private String title;

    @Column(length = 300)
    private String subtitle;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Column(name = "pace_display", length = 50)
    private String paceDisplay;

    @Column(length = 100)
    private String icon;

    private Boolean highlight;

    private Boolean muted;

    public TrainingStep() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Training getTraining() { return training; }
    public void setTraining(Training training) { this.training = training; }

    public TrainingStepBlock getBlock() { return block; }
    public void setBlock(TrainingStepBlock block) { this.block = block; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getBlockSortOrder() { return blockSortOrder; }
    public void setBlockSortOrder(Integer blockSortOrder) { this.blockSortOrder = blockSortOrder; }

    public String getStepType() { return stepType; }
    public void setStepType(String stepType) { this.stepType = stepType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Integer getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }

    public String getPaceDisplay() { return paceDisplay; }
    public void setPaceDisplay(String paceDisplay) { this.paceDisplay = paceDisplay; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Boolean getHighlight() { return highlight; }
    public void setHighlight(Boolean highlight) { this.highlight = highlight; }

    public Boolean getMuted() { return muted; }
    public void setMuted(Boolean muted) { this.muted = muted; }
}
