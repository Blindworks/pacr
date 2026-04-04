package com.trainingsplan.dto;

import java.time.LocalDateTime;

public record GroupEventRegistrationDto(
        Long id,
        Long eventId,
        String eventTitle,
        Long userId,
        String username,
        String status,
        LocalDateTime registeredAt
) {}
