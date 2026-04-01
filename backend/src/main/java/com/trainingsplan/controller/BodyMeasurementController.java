package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.entity.BodyMeasurement;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.service.BodyMeasurementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/body-measurements")
@RequiresSubscription(SubscriptionPlan.PRO)
public class BodyMeasurementController {

    private final BodyMeasurementService bodyMeasurementService;

    public BodyMeasurementController(BodyMeasurementService bodyMeasurementService) {
        this.bodyMeasurementService = bodyMeasurementService;
    }

    @GetMapping
    public ResponseEntity<List<BodyMeasurement>> getAll() {
        return ResponseEntity.ok(bodyMeasurementService.getAllForCurrentUser());
    }

    @GetMapping("/latest")
    public ResponseEntity<BodyMeasurement> getLatest() {
        return bodyMeasurementService.getLatestForCurrentUser()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BodyMeasurement> create(@RequestBody BodyMeasurement measurement) {
        BodyMeasurement created = bodyMeasurementService.create(measurement);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BodyMeasurement> update(
            @PathVariable Long id,
            @RequestBody BodyMeasurement measurement) {
        return ResponseEntity.ok(bodyMeasurementService.update(id, measurement));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bodyMeasurementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
