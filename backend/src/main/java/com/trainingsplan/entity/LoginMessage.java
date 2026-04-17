package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "login_messages")
public class LoginMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "is_published")
    private boolean published = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private LoginMessageTargetType targetType = LoginMessageTargetType.ALL;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
        name = "login_message_target_groups",
        joinColumns = @JoinColumn(name = "login_message_id")
    )
    @Column(name = "target_group", length = 20)
    private Set<LoginMessageTargetGroup> targetGroups = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "login_message_target_users",
        joinColumns = @JoinColumn(name = "login_message_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> targetUsers = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public LoginMessageTargetType getTargetType() { return targetType; }
    public void setTargetType(LoginMessageTargetType targetType) { this.targetType = targetType; }
    public Set<LoginMessageTargetGroup> getTargetGroups() { return targetGroups; }
    public void setTargetGroups(Set<LoginMessageTargetGroup> targetGroups) { this.targetGroups = targetGroups; }
    public Set<User> getTargetUsers() { return targetUsers; }
    public void setTargetUsers(Set<User> targetUsers) { this.targetUsers = targetUsers; }
}
