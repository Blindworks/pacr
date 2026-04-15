package com.trainingsplan.dto;

public record TrendingTopicDto(
    String tag,
    long viewCount,
    long newsCount,
    String headline
) {}
