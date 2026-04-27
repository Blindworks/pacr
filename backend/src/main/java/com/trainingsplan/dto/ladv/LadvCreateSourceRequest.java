package com.trainingsplan.dto.ladv;

public record LadvCreateSourceRequest(
        String name,
        String lv,
        Boolean bestenlistenfaehigOnly,
        Boolean enabled
) {}
