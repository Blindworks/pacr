package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.entity.SleepData;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.service.SleepDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sleep-data")
@RequiresSubscription(SubscriptionPlan.PRO)
public class SleepDataController {

    private final SleepDataService sleepDataService;

    public SleepDataController(SleepDataService sleepDataService) {
        this.sleepDataService = sleepDataService;
    }

    @GetMapping
    public ResponseEntity<List<SleepData>> getAll() {
        return ResponseEntity.ok(sleepDataService.getAllForCurrentUser());
    }

    @GetMapping("/latest")
    public ResponseEntity<SleepData> getLatest() {
        return sleepDataService.getLatestForCurrentUser()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SleepData> create(@RequestBody SleepData sleepData) {
        SleepData created = sleepDataService.create(sleepData);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SleepData> update(
            @PathVariable Long id,
            @RequestBody SleepData sleepData) {
        return ResponseEntity.ok(sleepDataService.update(id, sleepData));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sleepDataService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Integer>> importCsv(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(sleepDataService.importFromGarminCsv(file));
    }
}
