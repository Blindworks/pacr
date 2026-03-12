package com.trainingsplan.controller;

import com.trainingsplan.dto.TrainingFeedbackDto;
import com.trainingsplan.entity.CompetitionRegistration;
import com.trainingsplan.entity.UserTrainingEntry;
import com.trainingsplan.repository.CompetitionRegistrationRepository;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import com.trainingsplan.security.SecurityUtils;
import com.trainingsplan.service.UserTrainingScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/user-training-entries")
public class UserTrainingEntryController {

    private static final Logger log = LoggerFactory.getLogger(UserTrainingEntryController.class);

    @Autowired
    private UserTrainingScheduleService scheduleService;

    @Autowired
    private UserTrainingEntryRepository entryRepository;

    @Autowired
    private CompetitionRegistrationRepository registrationRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<UserTrainingEntry>> getEntriesByRegistration(
            @RequestParam Long registrationId) {
        return ResponseEntity.ok(scheduleService.getEntriesByRegistration(registrationId));
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<UserTrainingEntry>> getCalendarEntries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = securityUtils.getCurrentUserId();
        log.info("GET /calendar from={} to={} userId={}", from, to, userId);
        if (userId == null) {
            log.warn("GET /calendar rejected: no authenticated user");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(scheduleService.getEntriesForUser(userId, from, to));
    }

    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<List<UserTrainingEntry>> getEntriesByCompetition(
            @PathVariable Long competitionId) {
        List<UserTrainingEntry> entries = entryRepository.findByCompetitionRegistration_Competition_Id(competitionId);
        return ResponseEntity.ok(entries);
    }

    @PutMapping("/{id}/feedback")
    public ResponseEntity<UserTrainingEntry> updateFeedback(
            @PathVariable Long id,
            @Valid @RequestBody TrainingFeedbackDto feedback) {
        try {
            UserTrainingEntry entry = scheduleService.updateCompletion(
                    id, feedback.isCompleted(), feedback.getCompletionStatus());
            return ResponseEntity.ok(entry);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<List<UserTrainingEntry>> generateSchedule(
            @RequestParam Long registrationId) {
        CompetitionRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null) return ResponseEntity.notFound().build();
        List<UserTrainingEntry> entries = scheduleService.reassignPlan(reg);
        return ResponseEntity.ok(entries);
    }
}
