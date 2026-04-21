package com.trainingsplan.controller;

import com.trainingsplan.dto.AdminRegistrationDto;
import com.trainingsplan.service.CompetitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/registrations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRegistrationController {

    private final CompetitionService competitionService;

    public AdminRegistrationController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping
    public ResponseEntity<List<AdminRegistrationDto>> listAll() {
        List<AdminRegistrationDto> dtos = competitionService.findAllRegistrationsForAdmin()
                .stream()
                .map(AdminRegistrationDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistration(@PathVariable Long id) {
        boolean deleted = competitionService.deleteRegistrationById(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
