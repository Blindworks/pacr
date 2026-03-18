package com.trainingsplan.service;

import com.trainingsplan.dto.DailyCoachContextDto;
import com.trainingsplan.entity.UserTrainingEntry;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the daily coaching LLM prompt by loading
 * {@code classpath:prompts/daily_coaching_prompt.txt} once at startup
 * and replacing placeholders at call time.
 */
@Service
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class DailyCoachPromptBuilder {

    private static final String TEMPLATE_PATH = "classpath:prompts/daily_coaching_prompt.txt";

    private final ResourceLoader resourceLoader;
    private String template;

    public DailyCoachPromptBuilder(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    void loadTemplate() {
        Resource resource = resourceLoader.getResource(TEMPLATE_PATH);
        try {
            this.template = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load daily coaching prompt template: " + TEMPLATE_PATH, e);
        }
    }

    public String buildPrompt(DailyCoachContextDto context, int feelingScore, String feelingText,
                               List<UserTrainingEntry> upcomingEntries) {
        return template
                .replace("{{date}}", context.getDate())
                .replace("{{plannedTraining}}", buildPlannedTrainingText(context.getPlannedEntries()))
                .replace("{{asthmaContext}}", buildAsthmaContext(context.getAsthmaRisk()))
                .replace("{{cycleContext}}", buildCycleContext(context.getCyclePhase()))
                .replace("{{feelingScore}}", String.valueOf(feelingScore))
                .replace("{{feelingText}}", feelingText != null ? feelingText : "Kein Kommentar")
                .replace("{{recentLoad}}", "Keine Daten") // placeholder — extend with DailyMetrics if needed
                .replace("{{upcomingTrainings}}", buildUpcomingTrainingsText(upcomingEntries));
    }

    private String buildPlannedTrainingText(List<UserTrainingEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "Kein Training geplant";
        }
        return entries.stream()
                .map(e -> {
                    String name = e.getTraining() != null ? e.getTraining().getName() : "Unbekannt";
                    String type = e.getTraining() != null ? e.getTraining().getTrainingType() : null;
                    String intensity = e.getTraining() != null ? e.getTraining().getIntensityLevel() : null;
                    Integer duration = e.getTraining() != null ? e.getTraining().getDurationMinutes() : null;
                    return formatTrainingLine(name, type, intensity, duration);
                })
                .collect(Collectors.joining("\n"));
    }

    private String buildUpcomingTrainingsText(List<UserTrainingEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "Keine weiteren Trainings in den nächsten 7 Tagen";
        }
        return entries.stream()
                .map(e -> {
                    String date = e.getTrainingDate() != null ? e.getTrainingDate().toString() : "?";
                    String name = e.getTraining() != null ? e.getTraining().getName() : "Unbekannt";
                    String type = e.getTraining() != null ? e.getTraining().getTrainingType() : null;
                    String intensity = e.getTraining() != null ? e.getTraining().getIntensityLevel() : null;
                    Integer duration = e.getTraining() != null ? e.getTraining().getDurationMinutes() : null;
                    return date + ": " + formatTrainingLine(name, type, intensity, duration);
                })
                .collect(Collectors.joining("\n"));
    }

    private String formatTrainingLine(String name, String type, String intensity, Integer durationMinutes) {
        StringBuilder sb = new StringBuilder(name);
        if (type != null) sb.append(" [").append(type).append("]");
        if (intensity != null) sb.append(" Intensität: ").append(intensity);
        if (durationMinutes != null) sb.append(" ").append(durationMinutes).append(" min");
        return sb.toString();
    }

    private String buildAsthmaContext(DailyCoachContextDto.AsthmaRiskDto asthmaRisk) {
        if (asthmaRisk == null) {
            return "Keine Daten";
        }
        return "Risiko-Index " + asthmaRisk.getRiskIndex() + "/100 (" + asthmaRisk.getLevel() + ")"
                + ", Pollen: " + asthmaRisk.getPollenSummary();
    }

    private String buildCycleContext(DailyCoachContextDto.CyclePhaseDto cyclePhase) {
        if (cyclePhase == null) {
            return "Nicht aktiviert";
        }
        return "Phase: " + cyclePhase.getPhase()
                + ", Energielevel: " + cyclePhase.getEnergyLevel() + "/100"
                + " – " + cyclePhase.getRecommendation();
    }
}
