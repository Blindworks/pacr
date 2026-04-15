package com.trainingsplan.dto;

import java.time.LocalDateTime;

public record AppNewsCommentDto(
    Long id,
    Long userId,
    String username,
    String displayName,
    String profileImageFilename,
    String content,
    LocalDateTime createdAt,
    boolean canDelete
) {}
