package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_kudos",
        uniqueConstraints = @UniqueConstraint(name = "uk_activity_kudos_pair",
                columnNames = {"completed_training_id", "user_id"}))
public class ActivityKudos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "completed_training_id", nullable = false)
    private CompletedTraining completedTraining;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ActivityKudos() {}

    public ActivityKudos(CompletedTraining completedTraining, User user) {
        this.completedTraining = completedTraining;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CompletedTraining getCompletedTraining() { return completedTraining; }
    public void setCompletedTraining(CompletedTraining completedTraining) { this.completedTraining = completedTraining; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
