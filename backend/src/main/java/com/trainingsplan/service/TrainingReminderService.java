package com.trainingsplan.service;

import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserNotificationPreferences;
import com.trainingsplan.entity.UserTrainingEntry;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TrainingReminderService {

    private static final Logger log = LoggerFactory.getLogger(TrainingReminderService.class);

    private final UserNotificationPreferencesService notifPrefsService;
    private final UserTrainingEntryRepository entryRepository;
    private final EmailService emailService;

    public TrainingReminderService(UserNotificationPreferencesService notifPrefsService,
                                   UserTrainingEntryRepository entryRepository,
                                   EmailService emailService) {
        this.notifPrefsService = notifPrefsService;
        this.entryRepository = entryRepository;
        this.emailService = emailService;
    }

    public void sendRemindersForTime(String time) {
        List<UserNotificationPreferences> subscribers = notifPrefsService.findAllReminderSubscribersForTime(time);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        for (UserNotificationPreferences prefs : subscribers) {
            User user = prefs.getUser();
            try {
                List<UserTrainingEntry> tomorrowEntries =
                    entryRepository.findEntriesForUserByDate(user.getId(), tomorrow);

                if (tomorrowEntries.isEmpty()) continue;

                StringBuilder sb = new StringBuilder();
                sb.append("Deine Trainingseinheiten für morgen (").append(tomorrow).append("):\n\n");
                for (UserTrainingEntry entry : tomorrowEntries) {
                    sb.append("- ").append(entry.getTraining().getName());
                    if (entry.getTraining().getDescription() != null) {
                        sb.append(": ").append(entry.getTraining().getDescription());
                    }
                    sb.append("\n");
                }

                emailService.sendSimpleMessage(
                    user.getEmail(),
                    "Dein Training für morgen",
                    sb.toString()
                );
            } catch (Exception e) {
                log.error("Failed to send reminder to user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
