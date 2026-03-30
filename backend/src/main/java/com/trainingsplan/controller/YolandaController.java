package com.trainingsplan.controller;

import com.trainingsplan.dto.YolandaStatusDto;
import com.trainingsplan.entity.BodyMeasurement;
import com.trainingsplan.service.YolandaService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/yolanda")
public class YolandaController {

    private final YolandaService yolandaService;

    public YolandaController(YolandaService yolandaService) {
        this.yolandaService = yolandaService;
    }

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        String url = yolandaService.getAuthorizationUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {
        yolandaService.exchangeCodeForToken(code);
        response.sendRedirect(yolandaService.getFrontendCallbackRedirectUrl());
    }

    @GetMapping("/status")
    public ResponseEntity<YolandaStatusDto> getStatus() {
        return ResponseEntity.ok(yolandaService.getStatus());
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        yolandaService.disconnect();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sync")
    public ResponseEntity<List<BodyMeasurement>> syncMeasurements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<BodyMeasurement> synced = yolandaService.syncMeasurements(from, to);
        return ResponseEntity.ok(synced);
    }
}
