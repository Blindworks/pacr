package com.trainingsplan.dto;

public record CreateAppNewsRequest(
    String title,
    String content,
    String excerpt,
    String topicTag,
    String heroImageFilename,
    Boolean isFeatured
) {}
