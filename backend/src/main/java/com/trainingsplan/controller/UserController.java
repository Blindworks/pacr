package com.trainingsplan.controller;

import com.trainingsplan.dto.CompetitionDto;
import com.trainingsplan.dto.ProfileCompletionDto;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.AuditLogService;
import com.trainingsplan.service.OnboardingService;
import com.trainingsplan.service.PaceZoneService;
import com.trainingsplan.service.UserProfileValidationService;
import com.trainingsplan.service.UserDeletionService;
import com.trainingsplan.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final UserProfileValidationService userProfileValidationService;
    private final PaceZoneService paceZoneService;
    private final OnboardingService onboardingService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final UserDeletionService userDeletionService;

    public UserController(UserService userService, SecurityUtils securityUtils,
                          UserProfileValidationService userProfileValidationService,
                          PaceZoneService paceZoneService,
                          OnboardingService onboardingService,
                          PasswordEncoder passwordEncoder,
                          AuditLogService auditLogService,
                          UserDeletionService userDeletionService) {
        this.userService = userService;
        this.securityUtils = securityUtils;
        this.userProfileValidationService = userProfileValidationService;
        this.paceZoneService = paceZoneService;
        this.onboardingService = onboardingService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.userDeletionService = userDeletionService;
    }

    public record CreateUserRequest(String username, String email) {}

    public record SetPaceZoneReferenceRequest(
            Double referenceDistanceM,
            Integer referenceTimeSeconds,
            String referenceLabel
    ) {}

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    public record DeleteUserRequest(String confirmUsername) {}

    public record UpdateUserRequest(
            String username,
            String email,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Integer heightCm,
            Double weightKg,
            Integer maxHeartRate,
            Integer hrRest,
            String gender,
            String status,
            Integer dwdRegionId,
            Boolean asthmaTrackingEnabled,
            Boolean cycleTrackingEnabled,
            Boolean communityRoutesEnabled,
            Boolean groupEventsEnabled,
            Boolean discoverableByOthers,
            String role,
            String subscriptionPlan,
            LocalDateTime subscriptionExpiresAt,
            String targetDistance,
            String weeklyVolumeKm,
            String theme
    ) {}

    @GetMapping("/me")
    public ResponseEntity<User> getMe() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (request.currentPassword() == null || request.newPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Aktuelles und neues Passwort sind erforderlich"));
        }
        if (request.newPassword().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("message", "Das neue Passwort muss mindestens 8 Zeichen lang sein"));
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Das aktuelle Passwort ist falsch"));
        }
        userService.changePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
        auditLogService.log(user, AuditAction.PASSWORD_CHANGED, "USER", String.valueOf(user.getId()), null);
        return ResponseEntity.ok(Map.of("message", "Passwort erfolgreich geaendert"));
    }

    @GetMapping("/me/profile-completion")
    public ResponseEntity<ProfileCompletionDto> getMyProfileCompletion() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userProfileValidationService.getProfileCompletion(user));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        User created = userService.createUser(request.username(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!currentUser.getId().equals(id) && currentUser.getRole() != com.trainingsplan.entity.UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(userService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean isAdmin = currentUser.getRole() == com.trainingsplan.entity.UserRole.ADMIN;
        if (!currentUser.getId().equals(id) && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // Non-admin users cannot modify sensitive fields (role, status, subscription)
        String role = isAdmin ? request.role() : null;
        String status = isAdmin ? request.status() : null;
        String subscriptionPlan = isAdmin ? request.subscriptionPlan() : null;
        LocalDateTime subscriptionExpiresAt = isAdmin ? request.subscriptionExpiresAt() : null;
        try {
            User updated = userService.updateUser(id, request.username(), request.email(),
                    request.firstName(), request.lastName(),
                    request.dateOfBirth(), request.heightCm(), request.weightKg(),
                    request.maxHeartRate(), request.hrRest(), request.gender(), status,
                    request.dwdRegionId(), Boolean.TRUE.equals(request.asthmaTrackingEnabled()),
                    Boolean.TRUE.equals(request.cycleTrackingEnabled()),
                    Boolean.TRUE.equals(request.communityRoutesEnabled()),
                    Boolean.TRUE.equals(request.groupEventsEnabled()),
                    Boolean.TRUE.equals(request.discoverableByOthers()),
                    role, subscriptionPlan, subscriptionExpiresAt,
                    request.targetDistance(), request.weeklyVolumeKm(), request.theme());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestBody DeleteUserRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            userDeletionService.deleteUserCompletely(id, currentUser.getId(),
                    request != null ? request.confirmUsername() : null);
            auditLogService.log(currentUser, AuditAction.USER_DELETED, "USER", String.valueOf(id), null);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me/pace-zones")
    public ResponseEntity<PaceZoneService.PaceZonesDto> getMyPaceZones() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (user.getThresholdPaceSecPerKm() == null) {
            return ResponseEntity.notFound().build();
        }
        var zones = paceZoneService.calculateZones(user.getThresholdPaceSecPerKm());
        var dto = new PaceZoneService.PaceZonesDto(
                user.getPaceRefDistanceM(),
                user.getPaceRefTimeSeconds(),
                user.getPaceRefLabel(),
                user.getThresholdPaceSecPerKm(),
                zones
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me/pace-zones")
    public ResponseEntity<PaceZoneService.PaceZonesDto> setMyPaceZones(
            @RequestBody SetPaceZoneReferenceRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (request.referenceDistanceM() == null || request.referenceTimeSeconds() == null
                || request.referenceDistanceM() <= 0 || request.referenceTimeSeconds() <= 0) {
            return ResponseEntity.badRequest().build();
        }
        int thresholdPace = paceZoneService.calculateThresholdPace(
                request.referenceDistanceM(), request.referenceTimeSeconds());
        userService.updatePaceZoneReference(user.getId(),
                request.referenceDistanceM(), request.referenceTimeSeconds(),
                request.referenceLabel(), thresholdPace);
        var zones = paceZoneService.calculateZones(thresholdPace);
        var dto = new PaceZoneService.PaceZonesDto(
                request.referenceDistanceM(),
                request.referenceTimeSeconds(),
                request.referenceLabel(),
                thresholdPace,
                zones
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping(path = "/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!currentUser.getId().equals(id) && currentUser.getRole() != com.trainingsplan.entity.UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userService.uploadProfileImage(id, file);
        return ResponseEntity.ok().build();
    }

    public record UpdateLocationRequest(Double latitude, Double longitude) {}

    @PutMapping("/me/location")
    public ResponseEntity<?> updateMyLocation(@RequestBody UpdateLocationRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        try {
            return ResponseEntity.ok(userService.updateLocation(user.getId(),
                    request.latitude(), request.longitude()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/me/location")
    public ResponseEntity<?> clearMyLocation() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userService.clearLocation(user.getId()));
    }

    @PutMapping("/me/complete-onboarding")
    public ResponseEntity<User> completeOnboarding() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.completeOnboarding(user.getId()));
    }

    @PostMapping("/me/onboarding-plan-setup")
    public ResponseEntity<CompetitionDto> onboardingPlanSetup(
            @RequestBody OnboardingService.OnboardingPlanSetupRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            CompetitionDto result = onboardingService.setupPlan(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/profile-image")
    public ResponseEntity<Resource> getProfileImage(@PathVariable Long id) {
        UserService.ProfileImageData profileImage = userService.loadProfileImage(id);
        if (profileImage == null) {
            return ResponseEntity.noContent().build();
        }
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(profileImage.contentType());
        } catch (IllegalArgumentException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(profileImage.resource());
    }
}
