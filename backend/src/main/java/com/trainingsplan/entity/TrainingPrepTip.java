package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "training_prep_tips")
public class TrainingPrepTip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_id", nullable = false)
    @JsonIgnore
    private Training training;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(length = 100)
    private String icon;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String text;

    public TrainingPrepTip() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Training getTraining() { return training; }
    public void setTraining(Training training) { this.training = training; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
