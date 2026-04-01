package com.trainingsplan.controller;

import com.trainingsplan.dto.FeedbackDto;
import com.trainingsplan.dto.UpdateFeedbackRequest;
import com.trainingsplan.entity.FeedbackStatus;
import com.trainingsplan.service.UserFeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/feedback")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeedbackController {

    private final UserFeedbackService userFeedbackService;

    public AdminFeedbackController(UserFeedbackService userFeedbackService) {
        this.userFeedbackService = userFeedbackService;
    }

    @GetMapping
    public ResponseEntity<List<FeedbackDto>> getAll(@RequestParam(required = false) FeedbackStatus status) {
        List<FeedbackDto> result = status != null
                ? userFeedbackService.findByStatus(status)
                : userFeedbackService.findAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userFeedbackService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeedbackDto> update(@PathVariable Long id, @RequestBody UpdateFeedbackRequest request) {
        return ResponseEntity.ok(userFeedbackService.update(id, request));
    }
}
