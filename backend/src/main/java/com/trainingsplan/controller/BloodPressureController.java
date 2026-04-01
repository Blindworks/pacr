package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.entity.BloodPressure;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.service.BloodPressureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blood-pressure")
@RequiresSubscription(SubscriptionPlan.PRO)
public class BloodPressureController {

    private final BloodPressureService bloodPressureService;

    public BloodPressureController(BloodPressureService bloodPressureService) {
        this.bloodPressureService = bloodPressureService;
    }

    @GetMapping
    public ResponseEntity<List<BloodPressure>> getAll() {
        return ResponseEntity.ok(bloodPressureService.getAllForCurrentUser());
    }

    @GetMapping("/latest")
    public ResponseEntity<BloodPressure> getLatest() {
        return bloodPressureService.getLatestForCurrentUser()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BloodPressure> create(@RequestBody BloodPressure bp) {
        BloodPressure created = bloodPressureService.create(bp);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BloodPressure> update(
            @PathVariable Long id,
            @RequestBody BloodPressure bp) {
        return ResponseEntity.ok(bloodPressureService.update(id, bp));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bloodPressureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
