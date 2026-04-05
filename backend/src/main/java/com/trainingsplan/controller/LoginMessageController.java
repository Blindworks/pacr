package com.trainingsplan.controller;

import com.trainingsplan.dto.LoginMessageDto;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.LoginMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/login-messages")
public class LoginMessageController {

    private final LoginMessageService messageService;
    private final SecurityUtils securityUtils;

    public LoginMessageController(LoginMessageService messageService, SecurityUtils securityUtils) {
        this.messageService = messageService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LoginMessageDto>> getPending() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(messageService.findPendingForUser(userId));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable Long id) {
        var user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok().build();
        }
        messageService.markAsSeen(user.getId(), id, user);
        return ResponseEntity.ok().build();
    }
}
