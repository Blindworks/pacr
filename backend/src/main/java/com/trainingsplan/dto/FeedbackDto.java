package com.trainingsplan.dto;

import com.trainingsplan.entity.FeedbackCategory;
import com.trainingsplan.entity.FeedbackStatus;

import java.time.LocalDateTime;

public record FeedbackDto(
    Long id,
    Long userId,
    String username,
    FeedbackCategory category,
    String subject,
    String message,
    FeedbackStatus status,
    String adminNotes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
