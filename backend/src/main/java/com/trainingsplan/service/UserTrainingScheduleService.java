package com.trainingsplan.service;

import com.trainingsplan.entity.*;
import com.trainingsplan.repository.TrainingRepository;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class UserTrainingScheduleService {

    private static final Logger log = LoggerFactory.getLogger(UserTrainingScheduleService.class);

    @Autowired
    private UserTrainingEntryRepository entryRepository;

    @Autowired
    private TrainingRepository trainingRepository;

    /**
     * Generates UserTrainingEntry records for all Training templates in the registration's plan.
     * Dates are computed relative to the competition race day.
     * weekNumber=1 → race week (last week before race).
     */
    @Transactional
    public List<UserTrainingEntry> generateSchedule(CompetitionRegistration registration) {
        TrainingPlan plan = registration.getTrainingPlan();
        if (plan == null) return Collections.emptyList();

        List<Training> templates = trainingRepository.findByTrainingPlan_Id(plan.getId());
        if (templates.isEmpty()) return Collections.emptyList();

        int maxWeekNumber = templates.stream()
                .mapToInt(t -> t.getWeekNumber() != null ? t.getWeekNumber() : 1)
                .max().orElse(1);

        LocalDate raceDay = registration.getCompetition().getDate();
        LocalDate raceWeekSunday = raceDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<UserTrainingEntry> entries = new ArrayList<>();
        for (Training template : templates) {
            if (template.getWeekNumber() == null || template.getDayOfWeek() == null) continue;

            int weeksBeforeRace = maxWeekNumber - template.getWeekNumber();
            LocalDate weekSunday = raceWeekSunday.minusWeeks(weeksBeforeRace);
            LocalDate weekMonday = weekSunday.minusDays(6);
            // DayOfWeek.getValue(): MONDAY=1, TUESDAY=2, ..., SUNDAY=7
            LocalDate trainingDate = weekMonday.plusDays(template.getDayOfWeek().getValue() - 1);

            UserTrainingEntry entry = new UserTrainingEntry();
            entry.setCompetitionRegistration(registration);
            entry.setTraining(template);
            entry.setTrainingDate(trainingDate);
            entry.setWeekNumber(template.getWeekNumber());
            entry.setCompleted(false);
            entries.add(entry);
        }

        return entryRepository.saveAll(entries);
    }

    /**
     * Deletes all UserTrainingEntry records for the given registration.
     */
    @Transactional
    public void clearSchedule(CompetitionRegistration registration) {
        entryRepository.deleteByCompetitionRegistrationId(registration.getId());
    }

    /**
     * Clears existing schedule and generates a new one (used when plan changes).
     */
    @Transactional
    public List<UserTrainingEntry> reassignPlan(CompetitionRegistration registration) {
        clearSchedule(registration);
        return generateSchedule(registration);
    }

    /**
     * Returns all UserTrainingEntry records for a user within a date range (calendar view).
     */
    @Transactional(readOnly = true)
    public List<UserTrainingEntry> getEntriesForUser(Long userId, LocalDate from, LocalDate to) {
        log.info("getEntriesForUser: userId={}, from={}, to={}", userId, from, to);
        List<UserTrainingEntry> entries = entryRepository.findCalendarEntriesForUser(userId, from, to);
        log.info("getEntriesForUser: {} entries found", entries.size());
        entries.forEach(e -> log.debug("  entry id={} date={} training={}",
                e.getId(), e.getTrainingDate(),
                e.getTraining() != null ? e.getTraining().getName() : "NULL"));
        return entries;
    }

    /**
     * Returns all entries for a given registration.
     */
    public List<UserTrainingEntry> getEntriesByRegistration(Long registrationId) {
        return entryRepository.findByCompetitionRegistrationId(registrationId);
    }

    /**
     * Updates completion status of a UserTrainingEntry. Adjusts intensity of peer entries
     * in the same week if a training is marked as not completed.
     */
    @Transactional
    public UserTrainingEntry updateCompletion(Long entryId, Boolean completed, String status) {
        UserTrainingEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("UserTrainingEntry not found: " + entryId));

        if (Boolean.TRUE.equals(completed)
                && entry.getCompetitionRegistration() != null
                && entry.getCompetitionRegistration().getUser() != null
                && entry.getTrainingDate() != null) {
            Long userId = entry.getCompetitionRegistration().getUser().getId();
            List<UserTrainingEntry> sameDayEntries = entryRepository.findEntriesForUserByDate(userId, entry.getTrainingDate());

            for (UserTrainingEntry candidate : sameDayEntries) {
                if (candidate.getId().equals(entryId)) {
                    continue;
                }
                candidate.setCompleted(false);
                if (!"skipped".equalsIgnoreCase(candidate.getCompletionStatus())) {
                    candidate.setCompletionStatus("skipped");
                }
            }
            entryRepository.saveAll(sameDayEntries);
        }

        entry.setCompleted(completed);
        entry.setCompletionStatus(status);
        return entryRepository.save(entry);
    }
}
