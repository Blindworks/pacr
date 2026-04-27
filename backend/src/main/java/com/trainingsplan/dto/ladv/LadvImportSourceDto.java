package com.trainingsplan.dto.ladv;

import com.trainingsplan.entity.LadvImportSource;

import java.time.LocalDateTime;

public record LadvImportSourceDto(
        Long id,
        String name,
        String lv,
        boolean bestenlistenfaehigOnly,
        boolean enabled,
        LocalDateTime lastFetchedAt,
        String lastFetchStatus,
        LocalDateTime createdAt
) {
    public static LadvImportSourceDto from(LadvImportSource s) {
        return new LadvImportSourceDto(
                s.getId(),
                s.getName(),
                s.getLv(),
                s.isBestenlistenfaehigOnly(),
                s.isEnabled(),
                s.getLastFetchedAt(),
                s.getLastFetchStatus(),
                s.getCreatedAt()
        );
    }
}
