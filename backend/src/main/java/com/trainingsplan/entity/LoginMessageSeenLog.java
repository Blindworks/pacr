package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_message_seen_log")
public class LoginMessageSeenLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "login_message_id")
    private LoginMessage loginMessage;

    @Column(name = "seen_at")
    private LocalDateTime seenAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LoginMessage getLoginMessage() { return loginMessage; }
    public void setLoginMessage(LoginMessage loginMessage) { this.loginMessage = loginMessage; }
    public LocalDateTime getSeenAt() { return seenAt; }
    public void setSeenAt(LocalDateTime seenAt) { this.seenAt = seenAt; }
}
