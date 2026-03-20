package com.trainingsplan.repository;

import com.trainingsplan.entity.UserNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationPreferencesRepository extends JpaRepository<UserNotificationPreferences, Long> {

    Optional<UserNotificationPreferences> findByUserId(Long userId);

    List<UserNotificationPreferences> findAllByEmailReminderEnabledTrueAndEmailReminderTime(String time);

    List<UserNotificationPreferences> findAllByEmailNewsEnabledTrue();
}
