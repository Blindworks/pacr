package com.trainingsplan.dto;

public record NotificationPreferencesDto(
    boolean emailReminderEnabled,
    String emailReminderTime,
    boolean emailNewsEnabled
) {}
