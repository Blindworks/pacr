package com.trainingsplan.dto;

public class UserSearchResultDto {
    private Long id;
    private String username;
    private String displayName;
    private String profileImageFilename;
    /** NONE, PENDING_OUT, PENDING_IN, FRIENDS */
    private String friendshipStatus;
    private Long friendshipId;
    private Double distanceKm;

    public UserSearchResultDto() {}

    public UserSearchResultDto(Long id, String username, String displayName,
                                String profileImageFilename, String friendshipStatus, Long friendshipId) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.profileImageFilename = profileImageFilename;
        this.friendshipStatus = friendshipStatus;
        this.friendshipId = friendshipId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getProfileImageFilename() { return profileImageFilename; }
    public void setProfileImageFilename(String profileImageFilename) { this.profileImageFilename = profileImageFilename; }
    public String getFriendshipStatus() { return friendshipStatus; }
    public void setFriendshipStatus(String friendshipStatus) { this.friendshipStatus = friendshipStatus; }
    public Long getFriendshipId() { return friendshipId; }
    public void setFriendshipId(Long friendshipId) { this.friendshipId = friendshipId; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
}
