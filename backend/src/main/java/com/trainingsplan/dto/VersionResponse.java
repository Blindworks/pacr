package com.trainingsplan.dto;

public record VersionResponse(
    String name,
    String version,
    String buildTimestamp
) {}
