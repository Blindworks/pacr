package com.trainingsplan.controller;

import com.trainingsplan.dto.NotificationPreferencesDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserNotificationPreferences;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.service.UserNotificationPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users/me/notification-preferences")
public class UserNotificationPreferencesController {

    private final UserNotificationPreferencesService service;
    private final UserRepository userRepository;

    public UserNotificationPreferencesController(UserNotificationPreferencesService service,
                                                  UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<NotificationPreferencesDto> get(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        UserNotificationPreferences prefs = service.getOrCreateForUser(user.getId());
        return ResponseEntity.ok(new NotificationPreferencesDto(
            prefs.isEmailReminderEnabled(),
            prefs.getEmailReminderTime(),
            prefs.isEmailNewsEnabled()
        ));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferencesDto> update(
            @RequestBody NotificationPreferencesDto dto, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
        UserNotificationPreferences prefs = service.updatePreferences(user.getId(), dto);
        return ResponseEntity.ok(new NotificationPreferencesDto(
            prefs.isEmailReminderEnabled(),
            prefs.getEmailReminderTime(),
            prefs.isEmailNewsEnabled()
        ));
    }
}
