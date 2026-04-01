package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.dto.CycleStatusDto;
import com.trainingsplan.entity.CycleSettings;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.CycleSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cycle-settings")
@RequiresSubscription(SubscriptionPlan.PRO)
public class CycleSettingsController {

    private final CycleSettingsService cycleSettingsService;
    private final SecurityUtils securityUtils;

    public CycleSettingsController(CycleSettingsService cycleSettingsService, SecurityUtils securityUtils) {
        this.cycleSettingsService = cycleSettingsService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<CycleSettings> getSettings() {
        Long userId = requireUserId();
        return cycleSettingsService.getSettings(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CycleSettings> saveSettings(@RequestBody CycleSettings settings) {
        Long userId = requireUserId();
        CycleSettings saved = cycleSettingsService.saveSettings(userId, settings);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/status")
    public ResponseEntity<CycleStatusDto> getStatus() {
        Long userId = requireUserId();
        return cycleSettingsService.getCycleStatus(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Long requireUserId() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userId;
    }
}
