package com.trainingsplan.dto;

import java.time.LocalDateTime;

public record ExternalNewsSourceDto(
        Long id,
        String name,
        String feedUrl,
        String language,
        boolean enabled,
        LocalDateTime lastFetchedAt,
        String lastFetchStatus,
        LocalDateTime createdAt
) {}
