package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "coros_token")
public class CorosToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "open_id", unique = true, length = 64)
    private String openId;

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    @Column(name = "expires_at")
    private Long expiresAt;

    @Column(length = 255)
    private String nickname;

    @Column(name = "profile_photo", length = 1000)
    private String profilePhoto;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getOpenId() { return openId; }
    public void setOpenId(String openId) { this.openId = openId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
}
