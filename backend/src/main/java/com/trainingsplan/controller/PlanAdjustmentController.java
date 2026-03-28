package com.trainingsplan.controller;

import com.trainingsplan.dto.PlanAdjustmentDto;
import com.trainingsplan.entity.PlanAdjustment;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.PlanAdaptationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plan-adjustments")
public class PlanAdjustmentController {

    private final PlanAdaptationService planAdaptationService;
    private final SecurityUtils securityUtils;

    public PlanAdjustmentController(PlanAdaptationService planAdaptationService,
                                    SecurityUtils securityUtils) {
        this.planAdaptationService = planAdaptationService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PlanAdjustmentDto>> getPendingAdjustments() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        List<PlanAdjustmentDto> dtos = planAdaptationService.getPendingAdjustments(userId)
                .stream()
                .map(PlanAdjustmentDto::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<PlanAdjustmentDto> acceptAdjustment(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        PlanAdjustment accepted = planAdaptationService.acceptAdjustment(id, userId);
        return ResponseEntity.ok(PlanAdjustmentDto.fromEntity(accepted));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PlanAdjustmentDto> rejectAdjustment(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        PlanAdjustment rejected = planAdaptationService.rejectAdjustment(id, userId);
        return ResponseEntity.ok(PlanAdjustmentDto.fromEntity(rejected));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PlanAdjustmentDto>> getAdjustmentHistory() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        List<PlanAdjustmentDto> dtos = planAdaptationService.getAdjustmentHistory(userId)
                .stream()
                .map(PlanAdjustmentDto::fromEntity)
                .toList();

        return ResponseEntity.ok(dtos);
    }
}
