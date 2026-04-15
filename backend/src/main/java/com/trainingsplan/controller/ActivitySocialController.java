package com.trainingsplan.controller;

import com.trainingsplan.dto.ActivityCommentDto;
import com.trainingsplan.dto.ActivityKudosDto;
import com.trainingsplan.dto.CreateActivityCommentRequest;
import com.trainingsplan.entity.User;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.ActivitySocialService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities/{activityId}")
public class ActivitySocialController {

    private final ActivitySocialService socialService;
    private final SecurityUtils securityUtils;

    public ActivitySocialController(ActivitySocialService socialService, SecurityUtils securityUtils) {
        this.socialService = socialService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/kudos")
    public ResponseEntity<?> getKudos(@PathVariable Long activityId) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            ActivityKudosDto dto = socialService.getKudos(activityId, user);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/kudos")
    public ResponseEntity<?> toggleKudos(@PathVariable Long activityId) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            ActivityKudosDto dto = socialService.toggleKudos(activityId, user);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/comments")
    public ResponseEntity<?> listComments(@PathVariable Long activityId) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            List<ActivityCommentDto> list = socialService.listComments(activityId, user);
            return ResponseEntity.ok(list);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping("/comments")
    public ResponseEntity<?> addComment(@PathVariable Long activityId,
                                        @RequestBody CreateActivityCommentRequest body) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            ActivityCommentDto saved = socialService.addComment(activityId, body.content(), user);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long activityId, @PathVariable Long commentId) {
        User user = securityUtils.getCurrentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(String.valueOf(user.getRole()));
        try {
            socialService.deleteComment(activityId, commentId, user, isAdmin);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
