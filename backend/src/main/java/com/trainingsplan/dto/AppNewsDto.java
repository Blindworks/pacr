package com.trainingsplan.dto;

import java.time.LocalDateTime;

public record AppNewsDto(
    Long id,
    String title,
    String content,
    String excerpt,
    String topicTag,
    String heroImageFilename,
    boolean isFeatured,
    boolean isPublished,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    long viewCount,
    long likeCount,
    long commentCount,
    boolean hasLiked,
    boolean isTrending
) {}
