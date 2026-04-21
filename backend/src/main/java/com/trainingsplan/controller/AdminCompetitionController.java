package com.trainingsplan.controller;

import com.trainingsplan.service.CompetitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/competitions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompetitionController {

    private final CompetitionService competitionService;

    public AdminCompetitionController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        if (competitionService.findEntityById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        competitionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
