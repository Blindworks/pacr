package com.trainingsplan.service;

import com.trainingsplan.dto.DailyTrainingCompletionDto;
import com.trainingsplan.entity.CompletedTraining;
import com.trainingsplan.entity.UserTrainingEntry;
import com.trainingsplan.repository.CompletedTrainingRepository;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingCompletionService {

    @Autowired
    private UserTrainingEntryRepository entryRepository;

    @Autowired
    private CompletedTrainingRepository completedTrainingRepository;

    @Autowired
    private SecurityUtils securityUtils;

    public DailyTrainingCompletionDto getDailyTrainingCompletion(LocalDate date) {
        Long userId = securityUtils.getCurrentUserId();
        List<UserTrainingEntry> planned = userId != null
                ? entryRepository.findCalendarEntriesForUser(userId, date, date)
                : Collections.emptyList();
        List<CompletedTraining> completedTrainings =
                completedTrainingRepository.findByTrainingDateOrderByUploadDateDesc(date);

        List<String> plannedNames = planned.stream()
                .map(e -> e.getTraining().getName())
                .collect(Collectors.toList());
        List<String> completedSports = completedTrainings.stream()
                .map(ct -> ct.getSport() != null ? ct.getSport() : "Unknown")
                .collect(Collectors.toList());

        return new DailyTrainingCompletionDto(date, planned.size(), completedTrainings.size(),
                plannedNames, completedSports);
    }

    public List<DailyTrainingCompletionDto> getWeeklyTrainingCompletion(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(this::getDailyTrainingCompletion)
                .collect(Collectors.toList());
    }

    public DailyTrainingCompletionDto getTodayTrainingCompletion() {
        return getDailyTrainingCompletion(LocalDate.now());
    }
}
