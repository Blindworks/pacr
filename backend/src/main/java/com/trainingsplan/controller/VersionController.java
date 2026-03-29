package com.trainingsplan.controller;

import com.trainingsplan.dto.VersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    @Value("${app.version:unknown}")
    private String version;

    @Value("${app.build.timestamp:unknown}")
    private String buildTimestamp;

    @GetMapping
    public VersionResponse getVersion() {
        return new VersionResponse("PACR", version, buildTimestamp);
    }
}
