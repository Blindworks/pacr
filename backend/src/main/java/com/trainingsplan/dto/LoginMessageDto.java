package com.trainingsplan.dto;

import java.time.LocalDateTime;

public record LoginMessageDto(
    Long id,
    String title,
    String content,
    boolean published,
    LocalDateTime publishedAt,
    LocalDateTime createdAt
) {}
