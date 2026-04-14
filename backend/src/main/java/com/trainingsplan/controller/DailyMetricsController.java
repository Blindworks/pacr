package com.trainingsplan.controller;

import com.trainingsplan.dto.ProfileCompletionDto;
import com.trainingsplan.dto.ReadinessExplainDto;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.DailyMetricsRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.DailyMetricsService;
import com.trainingsplan.service.MetricsKernelService;
import com.trainingsplan.service.ReadinessService;
import com.trainingsplan.service.UserProfileValidationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/daily-metrics")
public class DailyMetricsController {

    private final DailyMetricsRepository dailyMetricsRepository;
    private final DailyMetricsService dailyMetricsService;
    private final SecurityUtils securityUtils;
    private final ReadinessService readinessService;
    private final MetricsKernelService metricsKernelService;
    private final UserProfileValidationService userProfileValidationService;

    public DailyMetricsController(DailyMetricsRepository dailyMetricsRepository,
                                   DailyMetricsService dailyMetricsService,
                                   SecurityUtils securityUtils,
                                   ReadinessService readinessService,
                                   MetricsKernelService metricsKernelService,
                                   UserProfileValidationService userProfileValidationService) {
        this.dailyMetricsRepository = dailyMetricsRepository;
        this.dailyMetricsService = dailyMetricsService;
        this.securityUtils = securityUtils;
        this.readinessService = readinessService;
        this.metricsKernelService = metricsKernelService;
        this.userProfileValidationService = userProfileValidationService;
    }

    @GetMapping
    public ResponseEntity<List<DailyMetrics>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = securityUtils.getCurrentUserId();
        List<DailyMetrics> metrics = dailyMetricsRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    /**
     * Recomputes the rolling 7-day and 28-day EF averages for the last 90 days
     * for the currently authenticated user.
     */
    @PostMapping("/recompute-ef")
    public ResponseEntity<?> recomputeEf() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        ProfileCompletionDto completion = userProfileValidationService.getProfileCompletion(user);
        if (!completion.complete()) {
            return ResponseEntity.badRequest().body(completion);
        }
        dailyMetricsService.recomputeEfForUser(user);
        return ResponseEntity.ok(Map.of(
                "message", "EF rolling averages recomputed",
                "daysProcessed", 90
        ));
    }

    /**
     * Recomputes the Readiness Proxy score for the last 90 days
     * for the currently authenticated user.
     * Requires ACWR data to already be populated for those days.
     */
    @PostMapping("/recompute-readiness")
    public ResponseEntity<?> recomputeReadiness() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        ProfileCompletionDto completion = userProfileValidationService.getProfileCompletion(user);
        if (!completion.complete()) {
            return ResponseEntity.badRequest().body(completion);
        }
        readinessService.recomputeForUser(user);
        return ResponseEntity.ok().build();
    }

    /**
     * Computes today's metrics (strain, ACWR, Readiness) for the authenticated user.
     * Called by the dashboard on load to ensure today's status is always current,
     * even on rest days with no training activity.
     */
    @PostMapping("/compute-today")
    public ResponseEntity<?> computeToday() {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        ProfileCompletionDto completion = userProfileValidationService.getProfileCompletion(user);
        if (!completion.complete()) {
            return ResponseEntity.badRequest().body(completion);
        }
        dailyMetricsService.computeToday(user);
        return ResponseEntity.ok().build();
    }

    /**
     * Liefert eine ausführliche Diagnose des Readiness-Scores für ein bestimmtes Datum:
     * alle Input-Rohwerte, alle angewandten Deductions inklusive Schwellen und die
     * finale Empfehlung. Ideal um nachzuvollziehen, warum der Score so ist wie er ist.
     *
     * <p>Read-only: persistiert nichts.
     */
    @GetMapping("/explain")
    public ResponseEntity<?> explainReadiness(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        LocalDate target = date != null ? date : LocalDate.now();
        ReadinessExplainDto dto = metricsKernelService.explainReadiness(user, target);
        return ResponseEntity.ok(dto);
    }
}
