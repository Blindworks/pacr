package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_event_exceptions",
    uniqueConstraints = @UniqueConstraint(name = "uk_group_event_exc_event_date", columnNames = {"event_id", "exception_date"}))
public class GroupEventException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private GroupEvent event;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GroupEventException() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GroupEvent getEvent() { return event; }
    public void setEvent(GroupEvent event) { this.event = event; }

    public LocalDate getExceptionDate() { return exceptionDate; }
    public void setExceptionDate(LocalDate exceptionDate) { this.exceptionDate = exceptionDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
