package com.trainingsplan.service;

import com.trainingsplan.dto.BioWeatherDto;
import com.trainingsplan.dto.CycleStatusDto;
import com.trainingsplan.dto.DailyCoachContextDto;
import com.trainingsplan.entity.DailyCoachSession;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserTrainingEntry;
import com.trainingsplan.repository.DailyCoachSessionRepository;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import com.trainingsplan.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class DailyCoachContextService {

    @Autowired
    private UserTrainingEntryRepository entryRepository;

    @Autowired
    private DwdWeatherService dwdWeatherService;

    @Autowired
    private CycleSettingsService cycleSettingsService;

    @Autowired
    private DailyCoachSessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    public DailyCoachContextDto getContext(Long userId, LocalDate date) {
        DailyCoachContextDto ctx = new DailyCoachContextDto();
        ctx.setDate(date.toString());

        // 1. Planned training entries for the requested date
        List<UserTrainingEntry> entries = entryRepository.findEntriesForUserByDate(userId, date);
        ctx.setPlannedEntries(entries);

        // 2. Load user to check feature flags
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 3. Asthma / environment data
            if (user.isAsthmaTrackingEnabled() && user.getDwdRegionId() != null) {
                try {
                    BioWeatherDto bio = dwdWeatherService.getEnvironmentData(user.getDwdRegionId());
                    ctx.setAsthmaRisk(buildAsthmaRiskDto(bio));
                } catch (Exception ignored) {
                    // External service failure must not break the coach context
                }
            }
        }

        // 4. Cycle phase (optional feature, available regardless of asthma setting)
        cycleSettingsService.getSettings(userId).ifPresent(settings -> {
            Optional<CycleStatusDto> statusOpt = cycleSettingsService.getCycleStatus(userId);
            statusOpt.ifPresent(status -> ctx.setCyclePhase(buildCyclePhaseDto(status)));
        });

        // 5. Existing session for today
        sessionRepository.findByUserIdAndSessionDate(userId, date)
                .ifPresent(session -> ctx.setExistingSession(buildExistingSessionDto(session)));

        return ctx;
    }

    private DailyCoachContextDto.AsthmaRiskDto buildAsthmaRiskDto(BioWeatherDto bio) {
        DailyCoachContextDto.AsthmaRiskDto dto = new DailyCoachContextDto.AsthmaRiskDto();
        int riskIndex = bio.getAsthmaRiskIndex() != null ? bio.getAsthmaRiskIndex() : 0;
        dto.setRiskIndex(riskIndex);
        dto.setPollenSummary(buildPollenSummary(bio));
        dto.setLevel(classifyRiskLevel(riskIndex));
        return dto;
    }

    private String buildPollenSummary(BioWeatherDto bio) {
        StringBuilder sb = new StringBuilder();
        appendPollen(sb, "Birke", bio.getPollenBirch());
        appendPollen(sb, "Gräser", bio.getPollenGrasses());
        appendPollen(sb, "Beifuß", bio.getPollenMugwort());
        appendPollen(sb, "Erle", bio.getPollenAlder());
        appendPollen(sb, "Esche", bio.getPollenAsh());
        return sb.length() > 0 ? sb.toString() : "Keine Pollendaten";
    }

    private void appendPollen(StringBuilder sb, String name, Integer value) {
        if (value != null && value > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(name).append(" ").append(value).append("/3");
        }
    }

    private String classifyRiskLevel(int riskIndex) {
        if (riskIndex >= 60) return "HIGH";
        if (riskIndex >= 30) return "MEDIUM";
        return "LOW";
    }

    private DailyCoachContextDto.CyclePhaseDto buildCyclePhaseDto(CycleStatusDto status) {
        DailyCoachContextDto.CyclePhaseDto dto = new DailyCoachContextDto.CyclePhaseDto();
        dto.setPhase(status.getCurrentPhase());
        dto.setEnergyLevel(estimateEnergyLevel(status.getCurrentPhase()));
        dto.setRecommendation(cyclePhaseRecommendation(status.getCurrentPhase()));
        return dto;
    }

    private int estimateEnergyLevel(String phase) {
        return switch (phase != null ? phase : "") {
            case "follicular", "ovulation" -> 80;
            case "luteal"                  -> 60;
            case "menstrual"               -> 40;
            default                        -> 70;
        };
    }

    private String cyclePhaseRecommendation(String phase) {
        return switch (phase != null ? phase : "") {
            case "follicular" -> "Gute Phase für intensivere Einheiten";
            case "ovulation"  -> "Maximale Energie – Wettkampf- und Tempotraining geeignet";
            case "luteal"     -> "Moderate Belastung empfohlen";
            case "menstrual"  -> "Leichtes Training oder Ruhe bevorzugen";
            default           -> "Normales Training";
        };
    }

    private DailyCoachContextDto.ExistingSessionDto buildExistingSessionDto(DailyCoachSession session) {
        DailyCoachContextDto.ExistingSessionDto dto = new DailyCoachContextDto.ExistingSessionDto();
        dto.setId(session.getId());
        dto.setAiRecommendation(session.getAiRecommendation());
        dto.setAiSuggestedAction(session.getAiSuggestedAction());
        dto.setUserDecision(session.getUserDecision());
        return dto;
    }
}
