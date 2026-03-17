package com.trainingsplan.controller;

import com.trainingsplan.entity.BodyMetric;
import com.trainingsplan.service.BodyMetricService;
import com.trainingsplan.service.RaceTimePredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/body-metrics")
public class BodyMetricController {

    private final BodyMetricService          bodyMetricService;
    private final RaceTimePredictionService  raceTimePredictionService;

    public BodyMetricController(BodyMetricService bodyMetricService,
                                RaceTimePredictionService raceTimePredictionService) {
        this.bodyMetricService         = bodyMetricService;
        this.raceTimePredictionService = raceTimePredictionService;
    }

    /**
     * Returns the latest value for each metric type for the authenticated user.
     * Response example:
     * [
     *   { "metricType": "VO2MAX", "label": "VO2max (Pace-basiert)", "value": 52.3, "unit": "ml/kg/min", "recordedAt": "2025-01-15" },
     *   { "metricType": "VO2MAX_HR_CORRECTED", "label": "VO2max (HF-korrigiert)", "value": 48.7, "unit": "ml/kg/min", "recordedAt": "2025-01-15" }
     * ]
     */
    @GetMapping("/current")
    public ResponseEntity<List<Map<String, Object>>> getCurrentMetrics() {
        List<BodyMetric> metrics = bodyMetricService.getLatestMetricsForCurrentUser();
        List<Map<String, Object>> response = metrics.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Returns context-aware race time predictions for the current user.
     * Combines latest VO2Max with recent training volume, long-run data and ACWR.
     * Response includes per-distance base and adjusted times, plus training context.
     */
    @GetMapping("/race-predictions-current")
    public ResponseEntity<Map<String, Object>> getCurrentRacePredictions() {
        Map<String, Object> result = raceTimePredictionService.computeForCurrentUser();
        if (result.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Returns the full VO2Max history for the current user, enriched with
     * race-time predictions for standard distances.
     * Response example:
     * [
     *   { "date": "2025-01-15", "vo2max": 52.3,
     *     "predictions": { "1km": "3:45", "5km": "20:30", "10km": "42:15",
     *                      "Halbmarathon": "1:32:10", "Marathon": "3:12:30" } }
     * ]
     */
    @GetMapping("/vo2max-history")
    public ResponseEntity<List<Map<String, Object>>> getVo2MaxHistory() {
        return ResponseEntity.ok(bodyMetricService.getVo2MaxHistoryForCurrentUser());
    }

    /**
     * Returns all body metrics (VO2MAX, VO2MAX_HR_CORRECTED) for a specific activity.
     * Returns 204 if no metrics exist for the given activity.
     */
    @GetMapping("/by-activity/{activityId}")
    public ResponseEntity<List<Map<String, Object>>> getByActivity(@PathVariable Long activityId) {
        List<BodyMetric> metrics = bodyMetricService.getMetricsForActivity(activityId);
        if (metrics.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(metrics.stream().map(this::toDto).toList());
    }

    /**
     * Recalculates all body metrics for the current user from their stored
     * CompletedTraining records. Use this to populate metrics for activities
     * uploaded before this feature existed.
     */
    @PostMapping("/recalculate")
    public ResponseEntity<Map<String, Object>> recalculate() {
        int count = bodyMetricService.recalculateForCurrentUser();
        return ResponseEntity.ok(Map.of(
                "message", "Metriken neu berechnet",
                "activitiesProcessed", count
        ));
    }

    private Map<String, Object> toDto(BodyMetric m) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("metricType", m.getMetricType());
        dto.put("label", labelFor(m.getMetricType()));
        dto.put("value", m.getValue());
        dto.put("unit", m.getUnit());
        dto.put("recordedAt", m.getRecordedAt() != null ? m.getRecordedAt().toString() : null);
        dto.put("sourceActivityId", m.getSourceActivityId());
        return dto;
    }

    private String labelFor(String metricType) {
        return switch (metricType) {
            case "VO2MAX" -> "VO2max (Pace-basiert)";
            case "VO2MAX_HR_CORRECTED" -> "VO2max (HF-korrigiert)";
            default -> metricType;
        };
    }
}
