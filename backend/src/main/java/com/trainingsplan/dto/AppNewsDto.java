package com.trainingsplan.dto;

import java.time.LocalDateTime;

public record AppNewsDto(
    Long id,
    String title,
    String content,
    boolean isPublished,
    LocalDateTime publishedAt,
    LocalDateTime createdAt
) {}
