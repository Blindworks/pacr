package com.trainingsplan.controller;

import com.trainingsplan.dto.BioWeatherDto;
import com.trainingsplan.entity.AsthmaEntry;
import com.trainingsplan.service.AsthmaEntryService;
import com.trainingsplan.service.DwdWeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asthma")
public class AsthmaController {

    private final AsthmaEntryService asthmaEntryService;
    private final DwdWeatherService dwdWeatherService;

    public AsthmaController(AsthmaEntryService asthmaEntryService, DwdWeatherService dwdWeatherService) {
        this.asthmaEntryService = asthmaEntryService;
        this.dwdWeatherService = dwdWeatherService;
    }

    @GetMapping("/environment")
    public ResponseEntity<BioWeatherDto> getEnvironment(
            @RequestParam(defaultValue = "50") int regionId) {
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
