package com.trainingsplan.controller;

import com.trainingsplan.dto.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-trainer")
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "false", matchIfMissing = true)
public class DailyCoachDisabledController {

    private static final Logger log = LoggerFactory.getLogger(DailyCoachDisabledController.class);
    private static final String DISABLED_MESSAGE =
            "AI Trainer feature is disabled. Set pacr.ai.enabled=true to enable it.";

    @GetMapping("/context")
    public ResponseEntity<MessageResponse> getContext(Authentication authentication) {
        log.warn("event=ai_trainer_disabled endpoint=/api/ai-trainer/context user={} reason=pacr.ai.enabled_false",
                authentication != null ? authentication.getName() : "anonymous");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new MessageResponse(DISABLED_MESSAGE));
    }

    @PostMapping("/recommendation")
    public ResponseEntity<MessageResponse> getRecommendation(Authentication authentication) {
        log.warn("event=ai_trainer_disabled endpoint=/api/ai-trainer/recommendation user={} reason=pacr.ai.enabled_false",
                authentication != null ? authentication.getName() : "anonymous");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new MessageResponse(DISABLED_MESSAGE));
    }

    @PostMapping("/execute")
    public ResponseEntity<MessageResponse> executeDecision(Authentication authentication) {
        log.warn("event=ai_trainer_disabled endpoint=/api/ai-trainer/execute user={} reason=pacr.ai.enabled_false",
                authentication != null ? authentication.getName() : "anonymous");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new MessageResponse(DISABLED_MESSAGE));
    }

    @GetMapping("/sessions")
    public ResponseEntity<MessageResponse> getSessions(Authentication authentication) {
        log.warn("event=ai_trainer_disabled endpoint=/api/ai-trainer/sessions user={} reason=pacr.ai.enabled_false",
                authentication != null ? authentication.getName() : "anonymous");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new MessageResponse(DISABLED_MESSAGE));
    }
}
