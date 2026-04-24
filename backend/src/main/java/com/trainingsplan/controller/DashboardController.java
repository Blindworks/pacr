package com.trainingsplan.controller;

import com.trainingsplan.dto.DashboardDto;
import com.trainingsplan.dto.NewStravaActivityDto;
import com.trainingsplan.dto.ProfileCompletionDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.DashboardService;
import com.trainingsplan.service.UserProfileValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;
    private final UserProfileValidationService userProfileValidationService;
    private final CompletedTrainingRepository completedTrainingRepository;

    public DashboardController(DashboardService dashboardService, SecurityUtils securityUtils,
                               UserProfileValidationService userProfileValidationService,
                               CompletedTrainingRepository completedTrainingRepository) {
        this.dashboardService = dashboardService;
        this.securityUtils = securityUtils;
        this.userProfileValidationService = userProfileValidationService;
        this.completedTrainingRepository = completedTrainingRepository;
    }

    @GetMapping
    public ResponseEntity<?> getDashboard() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        ProfileCompletionDto completion = userProfileValidationService.getProfileCompletion(user);
        if (!completion.complete()) {
            return ResponseEntity.badRequest().body(completion);
        }

        return ResponseEntity.ok(dashboardService.getDashboard(user));
    }

    /**
     * Returns the most recent Strava activity uploaded since the user's previous login,
     * intended to be shown as a one-time popup on the dashboard. Returns 204 if there is
     * no previous login on record or no new activity to show.
     */
    @GetMapping("/new-strava-activity")
    public ResponseEntity<NewStravaActivityDto> getNewStravaActivity() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        if (user.getPreviousLoginAt() == null) {
            return ResponseEntity.noContent().build();
        }
        return completedTrainingRepository
                .findTopByUserIdAndSourceAndUploadDateAfterOrderByUploadDateDesc(
                        user.getId(), "STRAVA", user.getPreviousLoginAt())
                .map(NewStravaActivityDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
