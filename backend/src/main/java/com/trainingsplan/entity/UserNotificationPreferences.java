package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_notification_preferences")
public class UserNotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "email_reminder_enabled")
    private boolean emailReminderEnabled = false;

    @Column(name = "email_reminder_time", length = 5)
    private String emailReminderTime = "18:00";

    @Column(name = "email_news_enabled")
    private boolean emailNewsEnabled = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public boolean isEmailReminderEnabled() { return emailReminderEnabled; }
    public void setEmailReminderEnabled(boolean emailReminderEnabled) { this.emailReminderEnabled = emailReminderEnabled; }
    public String getEmailReminderTime() { return emailReminderTime; }
    public void setEmailReminderTime(String emailReminderTime) { this.emailReminderTime = emailReminderTime; }
    public boolean isEmailNewsEnabled() { return emailNewsEnabled; }
    public void setEmailNewsEnabled(boolean emailNewsEnabled) { this.emailNewsEnabled = emailNewsEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
