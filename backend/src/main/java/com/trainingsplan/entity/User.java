package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private Integer heightCm;

    @Column
    private Double weightKg;

    @Column
    private Integer maxHeartRate;

    /** Resting heart rate in bpm. Required for Bannister TRIMP calculation. */
    @Column(name = "hr_rest")
    private Integer hrRest;

    /** Biological sex for Bannister k coefficient: {@code MALE} (k=1.92) or {@code FEMALE} (k=1.67). */
    @Column(name = "gender", length = 10)
    private String gender;

    @JsonIgnore
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private UserStatus status = UserStatus.ACTIVE;

    @JsonIgnore
    @Column(name = "email_verification_code", length = 6)
    private String emailVerificationCode;

    @JsonIgnore
    @Column(name = "email_verification_expires_at")
    private LocalDateTime emailVerificationExpiresAt;

    @JsonIgnore
    @Column(name = "password_reset_token_hash", length = 255)
    private String passwordResetTokenHash;

    @JsonIgnore
    @Column(name = "password_reset_token_expires_at")
    private LocalDateTime passwordResetTokenExpiresAt;

    @Column(name = "profile_image", length = 255)
    private String profileImageFilename;

    /** Reference race distance in meters for pace zone calculation. */
    @Column(name = "pace_ref_distance_m")
    private Double paceRefDistanceM;

    /** Reference race time in seconds for pace zone calculation. */
    @Column(name = "pace_ref_time_seconds")
    private Integer paceRefTimeSeconds;

    /** Human-readable label for the reference race, e.g. "10K" or "Halbmarathon". */
    @Column(name = "pace_ref_label", length = 50)
    private String paceRefLabel;

    /** Calculated lactate threshold pace in seconds per km. */
    @Column(name = "threshold_pace_sec_per_km")
    private Integer thresholdPaceSecPerKm;

    /** DWD region ID for bio-weather and pollen data (default: 50 = Bayern-Nord). */
    @Column(name = "dwd_region_id")
    private Integer dwdRegionId;

    /** Whether asthma tracking and bio-weather features are enabled for this user. */
    @Column(name = "asthma_tracking_enabled", nullable = false)
    private boolean asthmaTrackingEnabled = false;

    /** Whether cycle tracking is enabled for this user. */
    @Column(name = "cycle_tracking_enabled", nullable = false)
    private boolean cycleTrackingEnabled = false;

    /** Whether community routes and leaderboards feature is enabled for this user. */
    @Column(name = "community_routes_enabled", nullable = false)
    private boolean communityRoutesEnabled = false;

    /** Whether group events feature is enabled for this user. */
    @Column(name = "group_events_enabled", nullable = false)
    private boolean groupEventsEnabled = false;

    /** Whether this user can be discovered/found by other runners. */
    @Column(name = "discoverable_by_others", nullable = false)
    private boolean discoverableByOthers = false;

    /** User's latitude for nearby discovery (optional). */
    @Column(name = "latitude")
    private Double latitude;

    /** User's longitude for nearby discovery (optional). */
    @Column(name = "longitude")
    private Double longitude;

    /** When the user's location was last updated. */
    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 20)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "target_distance", length = 20)
    private String targetDistance;

    @Column(name = "weekly_volume_km", length = 10)
    private String weeklyVolumeKm;

    @Column(name = "theme", nullable = false, length = 10)
    private String theme = "dark";

    @JsonIgnore
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private StravaToken stravaToken;

    public User() {}

    public User(String username, String email, LocalDateTime createdAt) {
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }

    // UserDetails implementation

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @Column(nullable = false, unique = true, length = 100)
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return status != UserStatus.BLOCKED; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return status == UserStatus.ACTIVE; }

    // Standard getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public StravaToken getStravaToken() {
        return stravaToken;
    }

    public void setStravaToken(StravaToken stravaToken) {
        this.stravaToken = stravaToken;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(Integer maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public Integer getHrRest() {
        return hrRest;
    }

    public void setHrRest(Integer hrRest) {
        this.hrRest = hrRest;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role == null ? UserRole.USER : role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status == null ? UserStatus.ACTIVE : status;
    }

    public String getEmailVerificationCode() {
        return emailVerificationCode;
    }

    public void setEmailVerificationCode(String emailVerificationCode) {
        this.emailVerificationCode = emailVerificationCode;
    }

    public LocalDateTime getEmailVerificationExpiresAt() {
        return emailVerificationExpiresAt;
    }

    public void setEmailVerificationExpiresAt(LocalDateTime emailVerificationExpiresAt) {
        this.emailVerificationExpiresAt = emailVerificationExpiresAt;
    }

    public String getPasswordResetTokenHash() {
        return passwordResetTokenHash;
    }

    public void setPasswordResetTokenHash(String passwordResetTokenHash) {
        this.passwordResetTokenHash = passwordResetTokenHash;
    }

    public LocalDateTime getPasswordResetTokenExpiresAt() {
        return passwordResetTokenExpiresAt;
    }

    public void setPasswordResetTokenExpiresAt(LocalDateTime passwordResetTokenExpiresAt) {
        this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
    }

    public String getProfileImageFilename() {
        return profileImageFilename;
    }

    public void setProfileImageFilename(String profileImageFilename) {
        this.profileImageFilename = profileImageFilename;
    }

    public Double getPaceRefDistanceM() {
        return paceRefDistanceM;
    }

    public void setPaceRefDistanceM(Double paceRefDistanceM) {
        this.paceRefDistanceM = paceRefDistanceM;
    }

    public Integer getPaceRefTimeSeconds() {
        return paceRefTimeSeconds;
    }

    public void setPaceRefTimeSeconds(Integer paceRefTimeSeconds) {
        this.paceRefTimeSeconds = paceRefTimeSeconds;
    }

    public String getPaceRefLabel() {
        return paceRefLabel;
    }

    public void setPaceRefLabel(String paceRefLabel) {
        this.paceRefLabel = paceRefLabel;
    }

    public Integer getThresholdPaceSecPerKm() {
        return thresholdPaceSecPerKm;
    }

    public void setThresholdPaceSecPerKm(Integer thresholdPaceSecPerKm) {
        this.thresholdPaceSecPerKm = thresholdPaceSecPerKm;
    }

    public Integer getDwdRegionId() { return dwdRegionId; }
    public void setDwdRegionId(Integer dwdRegionId) { this.dwdRegionId = dwdRegionId; }

    public boolean isAsthmaTrackingEnabled() { return asthmaTrackingEnabled; }
    public void setAsthmaTrackingEnabled(boolean asthmaTrackingEnabled) { this.asthmaTrackingEnabled = asthmaTrackingEnabled; }

    public boolean isCycleTrackingEnabled() { return cycleTrackingEnabled; }
    public void setCycleTrackingEnabled(boolean cycleTrackingEnabled) { this.cycleTrackingEnabled = cycleTrackingEnabled; }

    public boolean isCommunityRoutesEnabled() { return communityRoutesEnabled; }
    public void setCommunityRoutesEnabled(boolean communityRoutesEnabled) { this.communityRoutesEnabled = communityRoutesEnabled; }

    public boolean isGroupEventsEnabled() { return groupEventsEnabled; }
    public void setGroupEventsEnabled(boolean groupEventsEnabled) { this.groupEventsEnabled = groupEventsEnabled; }

    public boolean isDiscoverableByOthers() { return discoverableByOthers; }
    public void setDiscoverableByOthers(boolean discoverableByOthers) { this.discoverableByOthers = discoverableByOthers; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(LocalDateTime locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public SubscriptionPlan getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan == null ? SubscriptionPlan.FREE : subscriptionPlan;
    }

    public LocalDateTime getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
    public void setSubscriptionExpiresAt(LocalDateTime subscriptionExpiresAt) { this.subscriptionExpiresAt = subscriptionExpiresAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }

    public String getTargetDistance() { return targetDistance; }
    public void setTargetDistance(String targetDistance) { this.targetDistance = targetDistance; }

    public String getWeeklyVolumeKm() { return weeklyVolumeKm; }
    public void setWeeklyVolumeKm(String weeklyVolumeKm) { this.weeklyVolumeKm = weeklyVolumeKm; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme != null ? theme : "dark"; }
}
