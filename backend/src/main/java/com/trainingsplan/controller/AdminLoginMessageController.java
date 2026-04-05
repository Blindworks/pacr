package com.trainingsplan.controller;

import com.trainingsplan.dto.CreateLoginMessageRequest;
import com.trainingsplan.dto.LoginMessageDto;
import com.trainingsplan.entity.LoginMessage;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.service.LoginMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/login-messages")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoginMessageController {

    private final LoginMessageService messageService;
    private final UserRepository userRepository;

    public AdminLoginMessageController(LoginMessageService messageService, UserRepository userRepository) {
        this.messageService = messageService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<LoginMessageDto> getAll() {
        return messageService.findAll().stream().map(messageService::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<LoginMessageDto> create(@RequestBody CreateLoginMessageRequest request, Principal principal) {
        User creator = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        LoginMessage msg = messageService.create(request, creator);
        return ResponseEntity.ok(messageService.toDto(msg));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoginMessageDto> update(@PathVariable Long id, @RequestBody CreateLoginMessageRequest request) {
        LoginMessage msg = messageService.update(id, request);
        return ResponseEntity.ok(messageService.toDto(msg));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        messageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<LoginMessageDto> publish(@PathVariable Long id) {
        LoginMessage msg = messageService.publish(id);
        return ResponseEntity.ok(messageService.toDto(msg));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<LoginMessageDto> unpublish(@PathVariable Long id) {
        LoginMessage msg = messageService.unpublish(id);
        return ResponseEntity.ok(messageService.toDto(msg));
    }
}
