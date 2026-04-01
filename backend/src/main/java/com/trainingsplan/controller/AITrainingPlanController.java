package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.dto.AITrainingPlanDTO;
import com.trainingsplan.dto.AITrainingPlanGenerateRequest;
import com.trainingsplan.dto.MessageResponse;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.logging.CorrelationIdFilter;
import com.trainingsplan.service.AIPlanGeneratorService;
import com.trainingsplan.service.AIPlanPersistenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/training-plan")
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
@RequiresSubscription(SubscriptionPlan.PRO)
public class AITrainingPlanController {

    private static final Logger log = LoggerFactory.getLogger(AITrainingPlanController.class);

    private final AIPlanGeneratorService aiPlanGeneratorService;
    private final AIPlanPersistenceService aiPlanPersistenceService;

    public AITrainingPlanController(AIPlanGeneratorService aiPlanGeneratorService,
                                    AIPlanPersistenceService aiPlanPersistenceService) {
        this.aiPlanGeneratorService = aiPlanGeneratorService;
        this.aiPlanPersistenceService = aiPlanPersistenceService;
    }

    @GetMapping("/{planId}")
    public ResponseEntity<?> getPlan(@PathVariable String planId) {
        try {
            return ResponseEntity.ok(aiPlanPersistenceService.getById(planId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody AITrainingPlanGenerateRequest request) {
        long startNanos = System.nanoTime();
        try {
            Long numericUserId = parseUserId(request.userId());
            AITrainingPlanDTO generatedPlan = aiPlanGeneratorService.generateWeeklyPlan(
                    numericUserId,
                    request.weekStart()
            );
            AITrainingPlanDTO savedPlan = aiPlanPersistenceService.save(generatedPlan, numericUserId);

            long generationTimeMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info(
                    "event=ai_training_plan_generated correlationId={} userId={} weekStart={} generationTimeMs={} planId={}",
                    MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY),
                    numericUserId,
                    request.weekStart(),
                    generationTimeMs,
                    savedPlan.getId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPlan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("userId must be a numeric user id");
        }
    }
}
