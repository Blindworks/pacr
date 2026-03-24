package com.trainingsplan.controller;

import com.trainingsplan.dto.StravaActivityDto;
import com.trainingsplan.dto.StravaStatusDto;
import com.trainingsplan.service.StravaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/strava")
public class StravaController {

    private final StravaService stravaService;

    public StravaController(StravaService stravaService) {
        this.stravaService = stravaService;
    }

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        String url = stravaService.getAuthorizationUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {
        stravaService.exchangeCodeForToken(code);
        response.sendRedirect(stravaService.getFrontendCallbackRedirectUrl());
    }

    @GetMapping("/status")
    public ResponseEntity<StravaStatusDto> getStatus() {
        return ResponseEntity.ok(stravaService.getStatus());
    }

    @GetMapping("/activities")
    public ResponseEntity<List<StravaActivityDto>> getActivities(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(stravaService.getActivities(startDate, endDate));
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        stravaService.disconnect();
        return ResponseEntity.ok().build();
    }
}
