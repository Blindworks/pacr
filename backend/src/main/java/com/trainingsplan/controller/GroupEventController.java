package com.trainingsplan.controller;

import com.trainingsplan.annotation.RequiresSubscription;
import com.trainingsplan.dto.GroupEventDto;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.GroupEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-events")
@RequiresSubscription(SubscriptionPlan.PRO)
public class GroupEventController {

    private final GroupEventService groupEventService;
    private final SecurityUtils securityUtils;

    public GroupEventController(GroupEventService groupEventService, SecurityUtils securityUtils) {
        this.groupEventService = groupEventService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyEvents(@RequestParam double lat,
                                             @RequestParam double lon,
                                             @RequestParam(defaultValue = "25") double radiusKm) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isGroupEventsEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Group events not enabled");

        List<GroupEventDto> events = groupEventService.getNearbyEvents(lat, lon, radiusKm, user);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcomingEvents() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isGroupEventsEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Group events not enabled");

        List<GroupEventDto> events = groupEventService.getUpcomingEvents(user);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventDetail(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isGroupEventsEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Group events not enabled");

        try {
            GroupEventDto dto = groupEventService.getEventDetail(id, user);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<?> registerForEvent(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!user.isGroupEventsEnabled()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Group events not enabled");

        try {
            groupEventService.registerForEvent(user, id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/register")
    public ResponseEntity<?> cancelRegistration(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            groupEventService.cancelRegistration(user, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my-registrations")
    public ResponseEntity<?> getMyRegistrations() {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<GroupEventDto> events = groupEventService.getMyRegistrations(user);
        return ResponseEntity.ok(events);
    }
}
