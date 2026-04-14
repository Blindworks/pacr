package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.SQLRestriction;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trainings")
public class Training {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "intensity_level")
    private String intensityLevel;

    @Column(name = "training_type")
    private String trainingType;

    @Column(name = "work_pace", length = 50)
    private String workPace;

    @Column(name = "work_time_seconds")
    private Integer workTimeSeconds;

    @Column(name = "work_distance_meters")
    private Integer workDistanceMeters;

    @Column(name = "recovery_pace", length = 50)
    private String recoveryPace;

    @Column(name = "recovery_time_seconds")
    private Integer recoveryTimeSeconds;

    @Column(name = "recovery_distance_meters")
    private Integer recoveryDistanceMeters;

    @Column(name = "intensity_score")
    private Integer intensityScore;

    @Column(name = "estimated_calories")
    private Integer estimatedCalories;

    @Column(length = 100)
    private String benefit;

    @Column(name = "estimated_distance_meters")
    private Integer estimatedDistanceMeters;

    @Column(length = 50)
    private String difficulty;

    @Column(name = "hero_image_url", length = 500)
    private String heroImageUrl;

    @OneToMany(mappedBy = "training", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @SQLRestriction("block_id IS NULL")
    private List<TrainingStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "training", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TrainingStepBlock> blocks = new ArrayList<>();

    @OneToMany(mappedBy = "training", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<TrainingPrepTip> prepTips = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id")
    @JsonIgnore
    private TrainingPlan trainingPlan;

    public Training() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getIntensityLevel() { return intensityLevel; }
    public void setIntensityLevel(String intensityLevel) { this.intensityLevel = intensityLevel; }

    public String getTrainingType() { return trainingType; }
    public void setTrainingType(String trainingType) { this.trainingType = trainingType; }

    public String getWorkPace() { return workPace; }
    public void setWorkPace(String workPace) { this.workPace = workPace; }

    public Integer getWorkTimeSeconds() { return workTimeSeconds; }
    public void setWorkTimeSeconds(Integer workTimeSeconds) { this.workTimeSeconds = workTimeSeconds; }

    public Integer getWorkDistanceMeters() { return workDistanceMeters; }
    public void setWorkDistanceMeters(Integer workDistanceMeters) { this.workDistanceMeters = workDistanceMeters; }

    public String getRecoveryPace() { return recoveryPace; }
    public void setRecoveryPace(String recoveryPace) { this.recoveryPace = recoveryPace; }

    public Integer getRecoveryTimeSeconds() { return recoveryTimeSeconds; }
    public void setRecoveryTimeSeconds(Integer recoveryTimeSeconds) { this.recoveryTimeSeconds = recoveryTimeSeconds; }

    public Integer getRecoveryDistanceMeters() { return recoveryDistanceMeters; }
    public void setRecoveryDistanceMeters(Integer recoveryDistanceMeters) { this.recoveryDistanceMeters = recoveryDistanceMeters; }

    public Integer getIntensityScore() { return intensityScore; }
    public void setIntensityScore(Integer intensityScore) { this.intensityScore = intensityScore; }

    public Integer getEstimatedCalories() { return estimatedCalories; }
    public void setEstimatedCalories(Integer estimatedCalories) { this.estimatedCalories = estimatedCalories; }

    public String getBenefit() { return benefit; }
    public void setBenefit(String benefit) { this.benefit = benefit; }

    public Integer getEstimatedDistanceMeters() { return estimatedDistanceMeters; }
    public void setEstimatedDistanceMeters(Integer estimatedDistanceMeters) { this.estimatedDistanceMeters = estimatedDistanceMeters; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getHeroImageUrl() { return heroImageUrl; }
    public void setHeroImageUrl(String heroImageUrl) { this.heroImageUrl = heroImageUrl; }

    public List<TrainingStep> getSteps() { return steps; }
    public void setSteps(List<TrainingStep> steps) {
        this.steps.clear();
        if (steps != null) {
            steps.forEach(this::addStep);
        }
    }

    public void addStep(TrainingStep step) {
        if (step == null) {
            return;
        }
        step.setTraining(this);
        this.steps.add(step);
    }

    public List<TrainingStepBlock> getBlocks() { return blocks; }
    public void setBlocks(List<TrainingStepBlock> blocks) {
        this.blocks.clear();
        if (blocks != null) {
            blocks.forEach(this::addBlock);
        }
    }

    public void addBlock(TrainingStepBlock block) {
        if (block == null) {
            return;
        }
        block.setTraining(this);
        this.blocks.add(block);
    }

    public List<TrainingPrepTip> getPrepTips() { return prepTips; }
    public void setPrepTips(List<TrainingPrepTip> prepTips) {
        this.prepTips.clear();
        if (prepTips != null) {
            prepTips.forEach(this::addPrepTip);
        }
    }

    public void addPrepTip(TrainingPrepTip prepTip) {
        if (prepTip == null) {
            return;
        }
        prepTip.setTraining(this);
        this.prepTips.add(prepTip);
    }

    public TrainingPlan getTrainingPlan() { return trainingPlan; }
    public void setTrainingPlan(TrainingPlan trainingPlan) { this.trainingPlan = trainingPlan; }

    @JsonProperty("trainingPlanName")
    public String getTrainingPlanName() {
        return trainingPlan != null ? trainingPlan.getName() : null;
    }

    @JsonProperty("trainingPlanId")
    public Long getTrainingPlanId() {
        return trainingPlan != null ? trainingPlan.getId() : null;
    }
}
