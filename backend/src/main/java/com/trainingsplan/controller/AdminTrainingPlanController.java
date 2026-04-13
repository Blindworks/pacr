package com.trainingsplan.controller;

import com.trainingsplan.service.TrainingPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/training-plans")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    public AdminTrainingPlanController(TrainingPlanService trainingPlanService) {
        this.trainingPlanService = trainingPlanService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrainingPlan(@PathVariable Long id) {
        if (trainingPlanService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        trainingPlanService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
