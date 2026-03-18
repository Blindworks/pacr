package com.trainingsplan.service;

import com.trainingsplan.dto.DailyCoachExecuteResponse;
import com.trainingsplan.entity.CoachDecision;
import com.trainingsplan.entity.UserTrainingEntry;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class TrainingRestructureService {

    @Autowired
    private UserTrainingEntryRepository entryRepository;

    @Transactional
    public List<DailyCoachExecuteResponse.RestructuringChange> restructure(Long userId, LocalDate date, CoachDecision decision) {
        if (decision == CoachDecision.PROCEED) {
            return List.of();
        }

        List<UserTrainingEntry> entries = entryRepository.findEntriesForUserByDate(userId, date);
        List<DailyCoachExecuteResponse.RestructuringChange> changes = new ArrayList<>();

        for (UserTrainingEntry entry : entries) {
            if (isKeyTraining(entry)) {
                LocalDate newDate = findNextFreeDay(userId, date.plusDays(1));
                entry.setTrainingDate(newDate);
                entryRepository.save(entry);

                DailyCoachExecuteResponse.RestructuringChange change = new DailyCoachExecuteResponse.RestructuringChange();
                change.setEntryId(entry.getId());
                change.setAction("MOVED");
                change.setNewDate(newDate.toString());
                changes.add(change);
            } else {
                entry.setCompletionStatus("SKIPPED");
                entryRepository.save(entry);

                DailyCoachExecuteResponse.RestructuringChange change = new DailyCoachExecuteResponse.RestructuringChange();
                change.setEntryId(entry.getId());
                change.setAction("SKIPPED");
                changes.add(change);
            }
        }

        return changes;
    }

    private boolean isKeyTraining(UserTrainingEntry entry) {
        if (entry.getTraining() == null) return false;
        String type = entry.getTraining().getTrainingType();
        String intensity = entry.getTraining().getIntensityLevel();
        boolean isKeyType = List.of("speed", "race", "fartlek")
                .contains(type != null ? type.toLowerCase() : "");
        boolean isHighIntensity = "high".equalsIgnoreCase(intensity);
        return isKeyType || isHighIntensity;
    }

    private LocalDate findNextFreeDay(Long userId, LocalDate from) {
        for (int i = 0; i < 7; i++) {
            LocalDate candidate = from.plusDays(i);
            List<UserTrainingEntry> existing = entryRepository.findEntriesForUserByDate(userId, candidate);
            boolean hasActiveTraining = existing.stream()
                    .anyMatch(e -> !"SKIPPED".equals(e.getCompletionStatus()));
            if (!hasActiveTraining) {
                return candidate;
            }
        }
        return from; // Fallback: use the starting date if no free day found
    }
}
