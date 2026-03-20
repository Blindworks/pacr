package com.trainingsplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.AuditLog;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Schreibt einen Audit-Log-Eintrag asynchron in einer eigenen Transaktion.
     * WICHTIG: Darf NICHT SecurityContextHolder.getContext() aufrufen — der Async-Thread
     * erbt den SecurityContext nicht. Actor wird immer explizit übergeben.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User actor, AuditAction action, String targetType, String targetId,
                    Map<String, Object> details) {
        try {
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;
            AuditLog entry = new AuditLog(
                    LocalDateTime.now(),
                    actor != null ? actor.getId() : null,
                    actor != null ? actor.getUsername() : null,
                    action,
                    targetType,
                    targetId,
                    detailsJson
            );
            repository.save(entry);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log details", e);
        }
    }
}
