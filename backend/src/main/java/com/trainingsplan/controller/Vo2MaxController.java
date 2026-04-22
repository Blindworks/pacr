package com.trainingsplan.controller;

import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.UserVo2MaxState;
import com.trainingsplan.service.CompletedTrainingService;
import com.trainingsplan.dto.StravaActivityDto;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.Vo2MaxAggregationService;
import com.trainingsplan.service.Vo2MaxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vo2max")
public class Vo2MaxController {

    private final Vo2MaxService vo2MaxService;
    private final CompletedTrainingService completedTrainingService;
    private final SecurityUtils securityUtils;
    private final Vo2MaxAggregationService vo2MaxAggregationService;

    public Vo2MaxController(Vo2MaxService vo2MaxService, CompletedTrainingService completedTrainingService,
                            SecurityUtils securityUtils, Vo2MaxAggregationService vo2MaxAggregationService) {
        this.vo2MaxService = vo2MaxService;
        this.completedTrainingService = completedTrainingService;
        this.securityUtils = securityUtils;
        this.vo2MaxAggregationService = vo2MaxAggregationService;
    }

    @GetMapping("/state/current")
    public ResponseEntity<?> getCurrentState() {
        com.trainingsplan.entity.User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(vo2MaxAggregationService.getCurrentState(user)
                .map(this::toStateResponse)
                .orElse(Map.of()));
    }

    @GetMapping("/state/history")
    public ResponseEntity<?> getStateHistory() {
        com.trainingsplan.entity.User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(401).build();
        List<Map<String, Object>> history = vo2MaxAggregationService.getHistory(user).stream()
                .map(this::toStateResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

    private Map<String, Object> toStateResponse(UserVo2MaxState s) {
        Map<String, Object> m = new HashMap<>();
        m.put("vo2maxInternal", s.getVo2maxInternal());
        m.put("vo2maxDisplayed", s.getVo2maxDisplayed());
        m.put("eligibleWorkoutCount", s.getEligibleWorkoutCount());
        m.put("lastUpdateAt", s.getLastUpdateAt());
        m.put("sourceActivityId", s.getSourceActivityId());
        m.put("createdAt", s.getCreatedAt());
        return m;
    }

    @PostMapping("/estimate/training")
    public ResponseEntity<?> estimateFromTraining(@RequestBody Vo2MaxTrainingRequest request) {
        if (request.getSport() == null || !request.getSport().toLowerCase().contains("run")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "VO2max wird nur für Laufaktivitäten berechnet."
            ));
        }

        Double distanceMeters = request.getDistanceKm() != null ? request.getDistanceKm() * 1000 : null;
        Optional<Double> vo2Max = vo2MaxService.calculate(distanceMeters, request.getMovingTimeSeconds());
        if (vo2Max.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "VO2max konnte nicht berechnet werden. Distanz oder Zeit ungültig."
            ));
        }

        Integer userMaxHR = null;
        com.trainingsplan.entity.User currentUser = securityUtils.getCurrentUser();
        if (currentUser != null) {
            userMaxHR = currentUser.getMaxHeartRate();
        }
        Optional<Double> vo2MaxHR = (userMaxHR != null && request.getAvgHeartRate() != null)
                ? vo2MaxService.calculateHRCorrected(distanceMeters, request.getMovingTimeSeconds(),
                        request.getAvgHeartRate(), userMaxHR)
                : Optional.empty();

        Map<String, Object> response = new HashMap<>();
        response.put("vo2max", vo2Max.get());
        response.put("vo2maxHRCorrected", vo2MaxHR.orElse(null));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/estimate")
    public ResponseEntity<?> estimateFromActivity(@RequestBody StravaActivityDto activity) {
        Optional<Double> vo2Max = vo2MaxService.calculateFromActivity(activity);
        if (vo2Max.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "VO2max konnte nicht berechnet werden. Erwartet wird eine Laufaktivitaet mit gueltiger Distanz und Zeit."
            ));
        }

        return ResponseEntity.ok(Map.of("vo2max", vo2Max.get()));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentVo2Max() {
        Optional<CompletedTraining> trainingOptional = completedTrainingService.getLatestRunningTrainingForCurrentUser();
        if (trainingOptional.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("vo2max", null);
            return ResponseEntity.ok(response);
        }

        CompletedTraining training = trainingOptional.get();
        // Daniels formula is designed for race/continuous effort → use total duration
        Integer durationSeconds = training.getDurationSeconds() != null
                ? training.getDurationSeconds()
                : training.getMovingTimeSeconds();
        // HR-corrected uses actual running pace → use moving time (excludes stops)
        Integer movingTimeSeconds = training.getMovingTimeSeconds() != null
                ? training.getMovingTimeSeconds()
                : training.getDurationSeconds();
        Double distanceMeters = training.getDistanceKm() != null ? training.getDistanceKm() * 1000 : null;

        Optional<Double> vo2Max = vo2MaxService.calculate(distanceMeters, durationSeconds);
        if (vo2Max.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("vo2max", null);
            return ResponseEntity.ok(response);
        }

        Integer userMaxHR = null;
        com.trainingsplan.entity.User currentUser = securityUtils.getCurrentUser();
        if (currentUser != null) {
            userMaxHR = currentUser.getMaxHeartRate();
        }
        Optional<Double> vo2MaxHR = userMaxHR != null
                ? vo2MaxService.calculateHRCorrected(distanceMeters, movingTimeSeconds,
                        training.getAverageHeartRate(), userMaxHR)
                : Optional.empty();

        // Compute %HRmax and derived %vVO2max for display
        Integer avgHR = training.getAverageHeartRate();
        Double percentHRmax = (avgHR != null && userMaxHR != null && userMaxHR > 0)
                ? Math.round((double) avgHR / userMaxHR * 1000) / 10.0
                : null;
        Double percentVVO2max = (percentHRmax != null) ? Math.round((percentHRmax - 5) * 10) / 10.0 : null;

        Map<String, Object> response = new HashMap<>();
        response.put("vo2max", vo2Max.get());
        response.put("vo2maxHRCorrected", vo2MaxHR.orElse(null));
        response.put("avgHeartRate", avgHR);
        response.put("maxHeartRate", training.getMaxHeartRate());
        response.put("percentHRmax", percentHRmax);
        response.put("percentVVO2max", percentVVO2max);
        response.put("trainingDate", training.getTrainingDate());
        response.put("activityName", training.getActivityName());
        return ResponseEntity.ok(response);
    }

    public static class Vo2MaxTrainingRequest {
        private Double distanceKm;
        private Integer movingTimeSeconds;
        private String sport;
        private Integer avgHeartRate;

        public Double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
        public Integer getMovingTimeSeconds() { return movingTimeSeconds; }
        public void setMovingTimeSeconds(Integer movingTimeSeconds) { this.movingTimeSeconds = movingTimeSeconds; }
        public String getSport() { return sport; }
        public void setSport(String sport) { this.sport = sport; }
        public Integer getAvgHeartRate() { return avgHeartRate; }
        public void setAvgHeartRate(Integer avgHeartRate) { this.avgHeartRate = avgHeartRate; }
    }
}
