package com.trainingsplan.controller;

import com.trainingsplan.entity.CycleEntry;
import com.trainingsplan.service.CycleEntryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cycle-entries")
public class CycleEntryController {

    private final CycleEntryService cycleEntryService;

    public CycleEntryController(CycleEntryService cycleEntryService) {
        this.cycleEntryService = cycleEntryService;
    }

    @GetMapping
    public ResponseEntity<List<CycleEntry>> getAll() {
        return ResponseEntity.ok(cycleEntryService.getAllForCurrentUser());
    }

    @GetMapping("/latest")
    public ResponseEntity<CycleEntry> getLatest() {
        return cycleEntryService.getLatestForCurrentUser()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-date")
    public ResponseEntity<CycleEntry> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return cycleEntryService.getByDateForCurrentUser(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CycleEntry> create(@RequestBody CycleEntry entry) {
        CycleEntry created = cycleEntryService.create(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CycleEntry> update(
            @PathVariable Long id,
            @RequestBody CycleEntry entry) {
        return ResponseEntity.ok(cycleEntryService.update(id, entry));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cycleEntryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
