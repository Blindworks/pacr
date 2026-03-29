package com.trainingsplan.controller;

import com.trainingsplan.dto.CompetitionDto;
import com.trainingsplan.entity.Competition;
import com.trainingsplan.entity.CompetitionRegistration;
import com.trainingsplan.entity.CompetitionType;
import com.trainingsplan.service.CompetitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/competitions")
public class CompetitionController {

    private static final Logger log = LoggerFactory.getLogger(CompetitionController.class);

    @Autowired
    private CompetitionService competitionService;

    @GetMapping("/types")
    public ResponseEntity<List<String>> getCompetitionTypes() {
        List<String> types = java.util.Arrays.stream(CompetitionType.values())
                .map(CompetitionType::getDisplayName)
                .toList();
        return ResponseEntity.ok(types);
    }

    @GetMapping
    public ResponseEntity<List<CompetitionDto>> getAllCompetitions(
            @RequestParam(defaultValue = "false") boolean includeSystemGenerated) {
        log.info("[CompetitionController] GET /api/competitions, includeSystemGenerated={}", includeSystemGenerated);
        try {
            List<CompetitionDto> result = competitionService.findAll(includeSystemGenerated);
            log.info("[CompetitionController] returning {} competitions", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[CompetitionController] GET /api/competitions failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitionDto> getCompetitionById(@PathVariable Long id) {
        CompetitionDto dto = competitionService.findById(id);
        if (dto != null) return ResponseEntity.ok(dto);
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<CompetitionDto> createCompetition(@Valid @RequestBody Competition competition) {
        return ResponseEntity.ok(competitionService.save(competition));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitionDto> updateCompetition(@PathVariable Long id,
                                                            @Valid @RequestBody Competition competition) {
        if (competitionService.findEntityById(id) == null) return ResponseEntity.notFound().build();
        competition.setId(id);
        return ResponseEntity.ok(competitionService.save(competition));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        if (competitionService.findEntityById(id) == null) return ResponseEntity.notFound().build();
        competitionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<CompetitionRegistration> registerForCompetition(@PathVariable Long id,
                                                                          @RequestBody(required = false) Map<String, Object> body) {
        try {
            String targetTime = body != null ? (String) body.get("targetTime") : null;
            Boolean registeredWithOrganizer = body != null ? (Boolean) body.get("registeredWithOrganizer") : null;
            String ranking = body != null ? (String) body.get("ranking") : null;
            CompetitionRegistration reg = competitionService.register(id, targetTime, registeredWithOrganizer, ranking);
            return ResponseEntity.ok(reg);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/register")
    public ResponseEntity<CompetitionRegistration> updateRegistration(@PathVariable Long id,
                                                                      @RequestBody Map<String, Object> body) {
        try {
            String ranking = (String) body.get("ranking");
            String targetTime = (String) body.get("targetTime");
            Boolean registeredWithOrganizer = (Boolean) body.get("registeredWithOrganizer");
            CompetitionRegistration reg = competitionService.updateRegistration(id, ranking, targetTime, registeredWithOrganizer);
            return ResponseEntity.ok(reg);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/register")
    public ResponseEntity<Void> unregisterFromCompetition(@PathVariable Long id) {
        competitionService.unregister(id);
        return ResponseEntity.noContent().build();
    }
}
