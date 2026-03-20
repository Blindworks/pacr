package com.trainingsplan.controller;

import com.trainingsplan.dto.AdminStatsDto;
import com.trainingsplan.dto.AuditLogDto;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.service.AdminStatsService;
import com.trainingsplan.service.AuditLogService;
import org.springframework.data.domain.Page;
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
    private final AuditLogService auditLogService;

    public AdminController(AdminStatsService adminStatsService,
                           AuditLogService auditLogService) {
        this.adminStatsService = adminStatsService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminStatsService.getStats());
    }

    @GetMapping("/audit-log")
    public ResponseEntity<?> getAuditLog(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        AuditAction actionEnum = null;
        if (action != null && !action.isBlank()) {
            try {
                actionEnum = AuditAction.valueOf(action);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        LocalDateTime fromDt = from != null ? LocalDate.parse(from).atStartOfDay() : null;
        LocalDateTime toDt = to != null ? LocalDate.parse(to).atTime(23, 59, 59) : null;

        Page<AuditLogDto> result = auditLogService.findFiltered(actionEnum, fromDt, toDt, page, size);

        return ResponseEntity.ok(result);
    }
}
