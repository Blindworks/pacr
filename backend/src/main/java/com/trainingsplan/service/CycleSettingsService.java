package com.trainingsplan.service;

import com.trainingsplan.dto.CycleStatusDto;
import com.trainingsplan.entity.CycleSettings;
import com.trainingsplan.repository.CycleEntryRepository;
import com.trainingsplan.repository.CycleSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class CycleSettingsService {

    @Autowired
    private CycleSettingsRepository cycleSettingsRepository;

    @Autowired
    private CycleEntryRepository cycleEntryRepository;

    public Optional<CycleSettings> getSettings(Long userId) {
        return cycleSettingsRepository.findByUserId(userId);
    }

    @Transactional
    public CycleSettings saveSettings(Long userId, CycleSettings settings) {
        CycleSettings existing = cycleSettingsRepository.findByUserId(userId).orElse(new CycleSettings());
        existing.setUserId(userId);
        existing.setFirstDayOfLastPeriod(settings.getFirstDayOfLastPeriod());
        existing.setAverageCycleLength(
                settings.getAverageCycleLength() != null ? settings.getAverageCycleLength() : 28);
        existing.setAveragePeriodDuration(
                settings.getAveragePeriodDuration() != null ? settings.getAveragePeriodDuration() : 5);
        return cycleSettingsRepository.save(existing);
    }

    public Optional<CycleStatusDto> getCycleStatus(Long userId) {
        return cycleSettingsRepository.findByUserId(userId).map(settings -> buildStatus(settings, userId));
    }

    private CycleStatusDto buildStatus(CycleSettings settings, Long userId) {
        LocalDate today = LocalDate.now();
        int cycleLength = settings.getAverageCycleLength();
        int periodDuration = settings.getAveragePeriodDuration();

        long daysSinceStart = ChronoUnit.DAYS.between(settings.getFirstDayOfLastPeriod(), today);
        int cycleDay = (int) (daysSinceStart % cycleLength) + 1;
        LocalDate currentCycleStart = settings.getFirstDayOfLastPeriod()
                .plusDays(daysSinceStart - (cycleDay - 1));

        String currentPhase = determinePhase(cycleDay, cycleLength, periodDuration);
        int phaseEndDay = phaseEndDay(currentPhase, cycleLength, periodDuration);
        int daysRemainingInPhase = phaseEndDay - cycleDay + 1;
        String nextPhase = nextPhase(currentPhase);

        LocalDate nextPeriodStart = currentCycleStart.plusDays(cycleLength);
        boolean shouldShowNewCyclePrompt = false;
        if (!today.isBefore(nextPeriodStart)) {
            shouldShowNewCyclePrompt = !cycleEntryRepository
                    .existsByUserIdAndEntryDateGreaterThanEqualAndFlowIntensityIsNotNull(userId, nextPeriodStart);
        }

        return new CycleStatusDto(currentPhase, cycleDay, cycleLength, daysRemainingInPhase,
                nextPhase, shouldShowNewCyclePrompt);
    }

    private String determinePhase(int cycleDay, int cycleLength, int periodDuration) {
        if (cycleDay <= periodDuration) {
            return "menstrual";
        }
        int follicularEnd = cycleLength / 2 - 2;
        if (cycleDay <= follicularEnd) {
            return "follicular";
        }
        int ovulationEnd = cycleLength / 2;
        if (cycleDay <= ovulationEnd) {
            return "ovulation";
        }
        return "luteal";
    }

    private int phaseEndDay(String phase, int cycleLength, int periodDuration) {
        return switch (phase) {
            case "menstrual" -> periodDuration;
            case "follicular" -> cycleLength / 2 - 2;
            case "ovulation" -> cycleLength / 2;
            default -> cycleLength; // luteal
        };
    }

    private String nextPhase(String currentPhase) {
        return switch (currentPhase) {
            case "menstrual" -> "follicular";
            case "follicular" -> "ovulation";
            case "ovulation" -> "luteal";
            default -> "menstrual"; // luteal
        };
    }
}
