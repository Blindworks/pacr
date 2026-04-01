package com.trainingsplan.controller;

import com.trainingsplan.dto.CreateFeedbackRequest;
import com.trainingsplan.dto.FeedbackDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.UserFeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final UserFeedbackService userFeedbackService;
    private final SecurityUtils securityUtils;

    public FeedbackController(UserFeedbackService userFeedbackService, SecurityUtils securityUtils) {
        this.userFeedbackService = userFeedbackService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<FeedbackDto> create(@RequestBody CreateFeedbackRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        FeedbackDto dto = userFeedbackService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
