package com.trainingsplan.controller;

import com.trainingsplan.dto.GroupEventDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.GroupEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only feed of nearby trainer/group events for the News Hub.
 * Visible to ALL logged-in users (no PRO check) so non-PRO users can discover events.
 * Detail-page access remains gated by {@link GroupEventController}'s @RequiresSubscription.
 */
@RestController
@RequestMapping("/api/news-feed/trainer-events")
public class TrainerEventFeedController {

    private final GroupEventService groupEventService;
    private final SecurityUtils securityUtils;

    public TrainerEventFeedController(GroupEventService groupEventService, SecurityUtils securityUtils) {
        this.groupEventService = groupEventService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<List<GroupEventDto>> getNearbyForFeed(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "25") double radiusKm,
            @RequestParam(defaultValue = "30") int days) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(days);
        List<GroupEventDto> all = groupEventService.getNearbyEvents(lat, lon, radiusKm, user);
        List<GroupEventDto> windowed = all.stream()
                .filter(e -> e.eventDate() != null
                        && !e.eventDate().isBefore(today)
                        && !e.eventDate().isAfter(cutoff))
                .toList();
        return ResponseEntity.ok(windowed);
    }
}
