package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.dto.BioWeatherDto;
import com.trainingsplan.entity.AsthmaEntry;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.AsthmaEntryService;
import com.trainingsplan.service.DwdWeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asthma")
@RequiresSubscription(SubscriptionPlan.PRO)
public class AsthmaController {

    private final AsthmaEntryService asthmaEntryService;
    private final DwdWeatherService dwdWeatherService;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    public AsthmaController(AsthmaEntryService asthmaEntryService, DwdWeatherService dwdWeatherService, SecurityUtils securityUtils, UserRepository userRepository) {
        this.asthmaEntryService = asthmaEntryService;
        this.dwdWeatherService = dwdWeatherService;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
    }

    @GetMapping("/environment")
    public ResponseEntity<BioWeatherDto> getEnvironment() {
        Long userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();
        int regionId = user.getDwdRegionId() != null ? user.getDwdRegionId() : 50;
        return ResponseEntity.ok(dwdWeatherService.getEnvironmentData(regionId));
    }

    @GetMapping("/entries")
    public ResponseEntity<List<AsthmaEntry>> getEntries() {
        return ResponseEntity.ok(asthmaEntryService.getAllForCurrentUser());
    }

    @GetMapping("/entries/last7days")
    public ResponseEntity<List<AsthmaEntry>> getLast7Days() {
        return ResponseEntity.ok(asthmaEntryService.getLast7DaysForCurrentUser());
    }

    @PostMapping("/entries")
    public ResponseEntity<AsthmaEntry> createEntry(@RequestBody AsthmaEntry entry) {
        return ResponseEntity.status(201).body(asthmaEntryService.create(entry));
    }

    @PutMapping("/entries/{id}")
    public ResponseEntity<AsthmaEntry> updateEntry(
            @PathVariable Long id,
            @RequestBody AsthmaEntry entry) {
        return ResponseEntity.ok(asthmaEntryService.update(id, entry));
    }

    @DeleteMapping("/entries/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        asthmaEntryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
