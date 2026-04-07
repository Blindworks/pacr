package com.trainingsplan.dto;

import java.time.LocalDateTime;

public class FriendshipDto {
    private Long id;
    private UserSearchResultDto otherUser;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    /** "INCOMING" if current user is the addressee, "OUTGOING" if requester. */
    private String direction;

    public FriendshipDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserSearchResultDto getOtherUser() { return otherUser; }
    public void setOtherUser(UserSearchResultDto otherUser) { this.otherUser = otherUser; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}
