package com.trainingsplan.controller;

import com.trainingsplan.dto.AdminStatsDto;
import com.trainingsplan.dto.AuditLogDto;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.repository.AuditLogRepository;
import com.trainingsplan.service.AdminStatsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminStatsService adminStatsService;
    private final AuditLogRepository auditLogRepository;

    public AdminController(AdminStatsService adminStatsService,
                           AuditLogRepository auditLogRepository) {
        this.adminStatsService = adminStatsService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminStatsService.getStats());
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<AuditLogDto>> getAuditLog(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        AuditAction actionEnum = action != null && !action.isBlank()
                ? AuditAction.valueOf(action) : null;
        LocalDateTime fromDt = from != null ? LocalDate.parse(from).atStartOfDay() : null;
        LocalDateTime toDt = to != null ? LocalDate.parse(to).atTime(23, 59, 59) : null;

        Page<AuditLogDto> result = auditLogRepository
                .findFiltered(actionEnum, fromDt, toDt, PageRequest.of(page, size))
                .map(AuditLogDto::from);

        return ResponseEntity.ok(result);
    }
}
