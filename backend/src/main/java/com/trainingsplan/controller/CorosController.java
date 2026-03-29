package com.trainingsplan.controller;

import com.trainingsplan.dto.CorosStatusDto;
import com.trainingsplan.dto.CorosWebhookRequest;
import com.trainingsplan.service.CorosService;
import com.trainingsplan.service.CorosWebhookService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/coros")
public class CorosController {

    private static final Logger log = LoggerFactory.getLogger(CorosController.class);

    private final CorosService corosService;
    private final CorosWebhookService webhookService;

    public CorosController(CorosService corosService, CorosWebhookService webhookService) {
        this.corosService = corosService;
        this.webhookService = webhookService;
    }

    // --- OAuth endpoints (JWT-authenticated) ---

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        String url = corosService.getAuthorizationUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {
        corosService.exchangeCodeForToken(code);
        response.sendRedirect(corosService.getFrontendCallbackRedirectUrl());
    }

    @GetMapping("/connection-status")
    public ResponseEntity<CorosStatusDto> getConnectionStatus() {
        return ResponseEntity.ok(corosService.getStatus());
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        corosService.disconnect();
        return ResponseEntity.ok().build();
    }

    // --- Webhook endpoints (no JWT, verified by client/secret headers) ---

    /**
     * Service Status Check URL — COROS calls this GET endpoint to verify our service is operational.
     * Must return HTTP 200.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("result", "0000", "message", "ok"));
    }

    /**
     * Workout Data Receiving Endpoint URL — COROS pushes workout summary data here.
     * The client and secret are sent in the HTTP request headers for verification.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> receiveWorkoutData(
            @RequestHeader(value = "client", required = false) String client,
            @RequestHeader(value = "secret", required = false) String secret,
            @RequestBody CorosWebhookRequest request) {

        if (!webhookService.validateRequest(client, secret)) {
            log.warn("COROS webhook: invalid client/secret credentials");
            return ResponseEntity.status(401)
                    .body(Map.of("result", "4001", "message", "Invalid credentials"));
        }

        try {
            webhookService.processWorkoutData(request);
            return ResponseEntity.ok(Map.of("result", "0000", "message", "ok"));
        } catch (Exception e) {
            log.error("COROS webhook processing error: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("result", "5000", "message", "Processing error: " + e.getMessage()));
        }
    }
}
