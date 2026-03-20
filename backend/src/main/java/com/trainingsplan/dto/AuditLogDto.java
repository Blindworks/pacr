package com.trainingsplan.dto;

import com.trainingsplan.entity.AuditLog;
import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        LocalDateTime timestamp,
        Long actorId,
        String actorUsername,
        String action,
        String targetType,
        String targetId,
        String details
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getTimestamp(),
                log.getActorId(),
                log.getActorUsername(),
                log.getAction().name(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetails()
        );
    }
}
