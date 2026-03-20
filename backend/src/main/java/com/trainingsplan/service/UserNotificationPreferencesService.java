package com.trainingsplan.service;

import com.trainingsplan.dto.NotificationPreferencesDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserNotificationPreferences;
import com.trainingsplan.repository.UserNotificationPreferencesRepository;
import com.trainingsplan.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserNotificationPreferencesService {

    private final UserNotificationPreferencesRepository prefsRepo;
    private final UserRepository userRepository;

    public UserNotificationPreferencesService(UserNotificationPreferencesRepository prefsRepo,
                                              UserRepository userRepository) {
        this.prefsRepo = prefsRepo;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserNotificationPreferences getOrCreateForUser(Long userId) {
        return prefsRepo.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            UserNotificationPreferences prefs = new UserNotificationPreferences();
            prefs.setUser(user);
            prefs.setEmailReminderEnabled(false);
            prefs.setEmailReminderTime("18:00");
            prefs.setEmailNewsEnabled(false);
            prefs.setCreatedAt(LocalDateTime.now());
            prefs.setUpdatedAt(LocalDateTime.now());
            return prefsRepo.save(prefs);
        });
    }

    @Transactional
    public UserNotificationPreferences updatePreferences(Long userId, NotificationPreferencesDto dto) {
        UserNotificationPreferences prefs = getOrCreateForUser(userId);
        prefs.setEmailReminderEnabled(dto.emailReminderEnabled());
        prefs.setEmailReminderTime(dto.emailReminderTime());
        prefs.setEmailNewsEnabled(dto.emailNewsEnabled());
        prefs.setUpdatedAt(LocalDateTime.now());
        return prefsRepo.save(prefs);
    }

    public List<UserNotificationPreferences> findAllReminderSubscribersForTime(String time) {
        return prefsRepo.findAllByEmailReminderEnabledTrueAndEmailReminderTime(time);
    }

    public List<UserNotificationPreferences> findAllNewsSubscribers() {
        return prefsRepo.findAllByEmailNewsEnabledTrue();
    }
}
