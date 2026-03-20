package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String details;

    public AuditLog() {}

    public AuditLog(LocalDateTime timestamp, Long actorId, String actorUsername,
                    AuditAction action, String targetType, String targetId, String details) {
        this.timestamp = timestamp;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }

    // Getters
    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Long getActorId() { return actorId; }
    public String getActorUsername() { return actorUsername; }
    public AuditAction getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getDetails() { return details; }
}
