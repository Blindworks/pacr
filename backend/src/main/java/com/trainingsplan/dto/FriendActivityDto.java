package com.trainingsplan.dto;

import java.time.LocalDate;

public class FriendActivityDto {
    private Long friendId;
    private String friendUsername;
    private String friendDisplayName;
    private String profileImageFilename;
    private String activityType;
    private String title;
    private LocalDate date;
    private Double distanceKm;
    private Integer durationSeconds;
    private String sport;

    public FriendActivityDto() {}

    public Long getFriendId() { return friendId; }
    public void setFriendId(Long friendId) { this.friendId = friendId; }
    public String getFriendUsername() { return friendUsername; }
    public void setFriendUsername(String friendUsername) { this.friendUsername = friendUsername; }
    public String getFriendDisplayName() { return friendDisplayName; }
    public void setFriendDisplayName(String friendDisplayName) { this.friendDisplayName = friendDisplayName; }
    public String getProfileImageFilename() { return profileImageFilename; }
    public void setProfileImageFilename(String profileImageFilename) { this.profileImageFilename = profileImageFilename; }
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }
}
