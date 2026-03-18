package com.trainingsplan.controller;

import com.trainingsplan.dto.DailyCoachContextDto;
import com.trainingsplan.dto.DailyCoachExecuteRequest;
import com.trainingsplan.dto.DailyCoachExecuteResponse;
import com.trainingsplan.dto.DailyCoachRecommendationRequest;
import com.trainingsplan.dto.DailyCoachRecommendationResponse;
import com.trainingsplan.entity.DailyCoachSession;
import com.trainingsplan.repository.DailyCoachSessionRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.DailyCoachContextService;
import com.trainingsplan.service.DailyCoachService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ai-trainer")
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class DailyCoachController {

    private static final Logger log = LoggerFactory.getLogger(DailyCoachController.class);

    @Autowired
    private DailyCoachContextService contextService;

    @Autowired
    private DailyCoachService coachService;

    @Autowired
    private DailyCoachSessionRepository sessionRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping("/context")
    public ResponseEntity<DailyCoachContextDto> getContext(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("GET /api/ai-trainer/context rejected: no authenticated user");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(contextService.getContext(userId, date));
    }

    @PostMapping("/recommendation")
    public ResponseEntity<DailyCoachRecommendationResponse> getRecommendation(
            @RequestBody DailyCoachRecommendationRequest request,
            Authentication authentication) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("POST /api/ai-trainer/recommendation rejected: no authenticated user");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(coachService.getRecommendation(userId, request));
    }

    @PostMapping("/execute")
    public ResponseEntity<DailyCoachExecuteResponse> executeDecision(
            @RequestBody DailyCoachExecuteRequest request,
            Authentication authentication) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("POST /api/ai-trainer/execute rejected: no authenticated user");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(coachService.executeDecision(userId, request));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<DailyCoachSession>> getSessions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("GET /api/ai-trainer/sessions rejected: no authenticated user");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(sessionRepository.findByUserIdAndSessionDateBetween(userId, from, to));
    }
}
