package com.trainingsplan.controller;

import com.trainingsplan.dto.*;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.GroupEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer/events")
@PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
public class TrainerEventController {

    private final GroupEventService groupEventService;
    private final SecurityUtils securityUtils;

    public TrainerEventController(GroupEventService groupEventService, SecurityUtils securityUtils) {
        this.groupEventService = groupEventService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody CreateGroupEventRequest request) {
        User trainer = securityUtils.getCurrentUser();
        if (trainer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            GroupEventDto dto = groupEventService.createEvent(trainer, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @RequestBody UpdateGroupEventRequest request) {
        User trainer = securityUtils.getCurrentUser();
        if (trainer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            GroupEventDto dto = groupEventService.updateEvent(trainer, id, request);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<?> publishEvent(@PathVariable Long id) {
        User trainer = securityUtils.getCurrentUser();
        if (trainer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            GroupEventDto dto = groupEventService.publishEvent(trainer, id);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelEvent(@PathVariable Long id) {
        User trainer = securityUtils.getCurrentUser();
        if (trainer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            groupEventService.cancelEvent(trainer, id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        User trainer = securityUtils.getCurrentUser();
        if (trainer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            groupEventService.deleteEvent(trainer, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getTrainerEvents() {
        User trainer = securityUtils.getCurrentUser();
        if (trainer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<GroupEventDto> events = groupEventService.getTrainerEvents(trainer);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<?> getParticipants(@PathVariable Long id) {
        User trainer = securityUtils.getCurrentUser();
        if (trainer == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            List<GroupEventRegistrationDto> participants = groupEventService.getEventParticipants(trainer, id);
            return ResponseEntity.ok(participants);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
