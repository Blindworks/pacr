package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "training_step_blocks")
public class TrainingStepBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_id", nullable = false)
    @JsonIgnore
    private Training training;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "repeat_count", nullable = false)
    private Integer repeatCount = 2;

    @Column(length = 100)
    private String label;

    @OneToMany(mappedBy = "block", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("blockSortOrder ASC")
    private List<TrainingStep> steps = new ArrayList<>();

    public TrainingStepBlock() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Training getTraining() { return training; }
    public void setTraining(Training training) { this.training = training; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getRepeatCount() { return repeatCount; }
    public void setRepeatCount(Integer repeatCount) { this.repeatCount = repeatCount; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

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
        step.setBlock(this);
        this.steps.add(step);
    }
}
