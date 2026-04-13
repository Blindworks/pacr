package com.trainingsplan.controller;

import com.trainingsplan.dto.TrainingPlanDto;
import com.trainingsplan.entity.CompetitionType;
import com.trainingsplan.entity.TrainingPlan;
import com.trainingsplan.service.TrainingPlanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/training-plans")
public class TrainingPlanController {

    private static final Logger log = LoggerFactory.getLogger(TrainingPlanController.class);
    private final TrainingPlanService trainingPlanService;

    public TrainingPlanController(TrainingPlanService trainingPlanService) {
        this.trainingPlanService = trainingPlanService;
    }

    @GetMapping
    public ResponseEntity<List<TrainingPlanDto>> getAllTrainingPlans() {
        List<TrainingPlanDto> plans = trainingPlanService.findAll()
                .stream()
                .map(TrainingPlanDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingPlanDto> getTrainingPlanById(@PathVariable Long id) {
        TrainingPlan plan = trainingPlanService.findById(id);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new TrainingPlanDto(plan));
    }

    @PostMapping
    public ResponseEntity<TrainingPlanDto> createTrainingPlan(@Valid @RequestBody TrainingPlan trainingPlan) {
        TrainingPlan saved = trainingPlanService.save(trainingPlan);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TrainingPlanDto(saved));
    }

    @PostMapping("/upload")
    public ResponseEntity<TrainingPlanDto> uploadTrainingPlan(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("competitionId") Long competitionId) {
        try {
            TrainingPlan plan = trainingPlanService.uploadTrainingPlan(file, name, description, competitionId);
            return ResponseEntity.status(HttpStatus.CREATED).body(new TrainingPlanDto(plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainingPlanDto> updateTrainingPlan(
            @PathVariable Long id,
            @Valid @RequestBody TrainingPlan trainingPlan) {
        if (trainingPlanService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        trainingPlan.setId(id);
        TrainingPlan updated = trainingPlanService.save(trainingPlan);
        return ResponseEntity.ok(new TrainingPlanDto(updated));
    }

    /**
     * Updates editable metadata fields (name, description, targetTime, prerequisites).
     * Does not touch jsonContent or training records.
     */
    @PatchMapping("/{id}/metadata")
    public ResponseEntity<TrainingPlanDto> updateMetadata(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> updates) {
        TrainingPlan plan = trainingPlanService.findById(id);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        if (updates.containsKey("name") && updates.get("name") != null && !updates.get("name").isBlank()) {
            plan.setName(updates.get("name"));
        }
        if (updates.containsKey("description")) {
            plan.setDescription(updates.get("description"));
        }
        if (updates.containsKey("targetTime")) {
            plan.setTargetTime(updates.get("targetTime"));
        }
        if (updates.containsKey("prerequisites")) {
            plan.setPrerequisites(updates.get("prerequisites"));
        }
        if (updates.containsKey("competitionType")) {
            plan.setCompetitionType(CompetitionType.fromString(updates.get("competitionType")));
        }
        return ResponseEntity.ok(new TrainingPlanDto(trainingPlanService.save(plan)));
    }

    /**
     * Upload a plan JSON as a reusable template. No competition required.
     * No Training records are created at this point.
     */
    @PostMapping("/upload-template")
    public ResponseEntity<TrainingPlanDto> uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "description", defaultValue = "") String description) {
        try {
            TrainingPlanDto dto = trainingPlanService.uploadAsTemplate(file, name, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Returns all stored plans (all plans are implicitly templates).
     */
    @GetMapping("/templates")
    public ResponseEntity<List<TrainingPlanDto>> getTemplates() {
        return ResponseEntity.ok(trainingPlanService.findAllTemplates());
    }

    /**
     * Assigns an existing plan to a competition and generates Training records.
     * Sets competition.trainingPlan = sourcePlan (no new plan created).
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assignPlanToCompetition(@RequestParam Long planId,
                                                      @RequestParam Long competitionId,
                                                      @RequestParam(required = false) Long formatId) {
        try {
            TrainingPlanDto dto = trainingPlanService.assignPlanToCompetition(planId, competitionId, formatId);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            log.error("Error assigning plan {} to competition {}: {}", planId, competitionId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
