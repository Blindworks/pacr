package com.trainingsplan.dto;

public record CreateExternalNewsSourceRequest(
        String name,
        String feedUrl,
        String language,
        Boolean enabled
) {}
