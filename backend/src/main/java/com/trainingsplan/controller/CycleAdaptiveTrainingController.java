package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.dto.AdaptiveSuggestionDto;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.CycleAdaptiveTrainingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cycle-tracking")
@RequiresSubscription(SubscriptionPlan.PRO)
public class CycleAdaptiveTrainingController {

    private final CycleAdaptiveTrainingService service;
    private final SecurityUtils securityUtils;

    public CycleAdaptiveTrainingController(CycleAdaptiveTrainingService service, SecurityUtils securityUtils) {
        this.service = service;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/adaptive-suggestion")
    public ResponseEntity<AdaptiveSuggestionDto> getAdaptiveSuggestion(
            @RequestParam(value = "lang", required = false) String lang) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return ResponseEntity.ok(service.getAdaptiveSuggestion(userId, lang));
    }
}
