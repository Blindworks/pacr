package com.trainingsplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.AdaptiveSuggestionDto;
import com.trainingsplan.dto.CycleStatusDto;
import com.trainingsplan.entity.CycleEntry;
import com.trainingsplan.entity.Training;
import com.trainingsplan.entity.UserTrainingEntry;
import com.trainingsplan.repository.CycleEntryRepository;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CycleAdaptiveTrainingService {

    private static final Logger log = LoggerFactory.getLogger(CycleAdaptiveTrainingService.class);

    private final UserTrainingEntryRepository userTrainingEntryRepository;
    private final CycleSettingsService cycleSettingsService;
    private final CycleEntryRepository cycleEntryRepository;
    private final ObjectProvider<LLMClientService> llmClientProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CycleAdaptiveTrainingService(UserTrainingEntryRepository userTrainingEntryRepository,
                                        CycleSettingsService cycleSettingsService,
                                        CycleEntryRepository cycleEntryRepository,
                                        ObjectProvider<LLMClientService> llmClientProvider) {
        this.userTrainingEntryRepository = userTrainingEntryRepository;
        this.cycleSettingsService = cycleSettingsService;
        this.cycleEntryRepository = cycleEntryRepository;
        this.llmClientProvider = llmClientProvider;
    }

    public AdaptiveSuggestionDto getAdaptiveSuggestion(Long userId, String lang) {
        AdaptiveSuggestionDto result = new AdaptiveSuggestionDto();
        LocalDate today = LocalDate.now();

        // 1. Today's planned training
        List<UserTrainingEntry> entries = userTrainingEntryRepository.findEntriesForUserByDate(userId, today);
        Training todayTraining = entries.stream()
                .map(UserTrainingEntry::getTraining)
                .filter(t -> t != null)
                .findFirst()
                .orElse(null);

        if (todayTraining != null) {
            result.setOriginalTraining(new AdaptiveSuggestionDto.PlannedTraining(
                    todayTraining.getName(),
                    todayTraining.getDescription(),
                    todayTraining.getDurationMinutes(),
                    todayTraining.getIntensityLevel(),
                    todayTraining.getTrainingType()
            ));
        }

        // 2. Cycle status
        CycleStatusDto cycleStatus = cycleSettingsService.getCycleStatus(userId).orElse(null);
        if (cycleStatus != null) {
            result.setCurrentPhase(cycleStatus.getCurrentPhase());
        }

        // 3. Today's cycle entry (optional)
        Optional<CycleEntry> todayEntry = cycleEntryRepository.findByUserIdAndEntryDate(userId, today);

        // 4. Call LLM if available
        LLMClientService llm = llmClientProvider.getIfAvailable();
        if (llm == null || cycleStatus == null) {
            result.setAiEnabled(false);
            return result;
        }
        result.setAiEnabled(true);

        try {
            String prompt = buildPrompt(todayTraining, cycleStatus, todayEntry.orElse(null), lang);
            String raw = llm.generateText(prompt);
            parseAndApply(raw, result);
        } catch (Exception e) {
            log.warn("Adaptive suggestion LLM call failed: {}", e.getMessage());
        }

        return result;
    }

    private String buildPrompt(Training training, CycleStatusDto cycle, CycleEntry entry, String lang) {
        String languageName = resolveLanguageName(lang);
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert running coach advising a female athlete who tracks her menstrual cycle. ");
        sb.append("Adapt her planned workout to fit her current hormonal phase and daily wellbeing. ");
        sb.append("Keep the suggestion realistic and specific (distance, intensity, pace if relevant).\n");
        sb.append("IMPORTANT: Write the values of the JSON fields \"name\", \"summary\" and \"explanation\" in ")
          .append(languageName).append(". The JSON keys themselves must stay in English.\n\n");

        sb.append("Cycle phase: ").append(cycle.getCurrentPhase())
          .append(" (day ").append(cycle.getCurrentDay())
          .append("/").append(cycle.getCycleLength()).append(")\n");

        if (entry != null) {
            if (entry.getEnergyLevel() != null) sb.append("Energy level (1-5): ").append(entry.getEnergyLevel()).append("\n");
            if (entry.getSleepQuality() != null) sb.append("Sleep quality: ").append(entry.getSleepQuality()).append("\n");
            if (entry.getMood() != null) sb.append("Mood: ").append(entry.getMood()).append("\n");
            if (entry.getPhysicalSymptoms() != null && !entry.getPhysicalSymptoms().isBlank())
                sb.append("Symptoms: ").append(entry.getPhysicalSymptoms()).append("\n");
        }

        if (training != null) {
            sb.append("\nPlanned training for today:\n");
            sb.append("- Name: ").append(training.getName()).append("\n");
            if (training.getTrainingType() != null) sb.append("- Type: ").append(training.getTrainingType()).append("\n");
            if (training.getIntensityLevel() != null) sb.append("- Intensity: ").append(training.getIntensityLevel()).append("\n");
            if (training.getDurationMinutes() != null) sb.append("- Duration: ").append(training.getDurationMinutes()).append(" min\n");
            if (training.getDescription() != null) sb.append("- Description: ").append(training.getDescription()).append("\n");
        } else {
            sb.append("\nNo training planned for today. Suggest something phase-appropriate.\n");
        }

        sb.append("\nReply with ONLY a minified JSON object of the form:\n");
        sb.append("{\"name\":\"short workout name\",\"summary\":\"one-line workout description with concrete numbers\",\"explanation\":\"one sentence explaining why this fits the current phase\"}\n");
        sb.append("No markdown, no code fences, no extra text.\n");
        sb.append("CRITICAL LANGUAGE REQUIREMENT: The string values inside the JSON (name, summary, explanation) MUST be written in ")
          .append(languageName).append(" and ONLY in ").append(languageName)
          .append(". Do NOT use English. Translate every word, including units and workout terms, into ")
          .append(languageName).append(".");
        return sb.toString();
    }

    private String resolveLanguageName(String lang) {
        if (lang == null || lang.isBlank()) return "English";
        String code = lang.toLowerCase();
        if (code.startsWith("de")) return "German";
        if (code.startsWith("fr")) return "French";
        if (code.startsWith("es")) return "Spanish";
        if (code.startsWith("it")) return "Italian";
        return "English";
    }

    private void parseAndApply(String raw, AdaptiveSuggestionDto result) throws Exception {
        if (raw == null) return;
        String cleaned = raw.trim();
        // Strip possible code fences
        if (cleaned.startsWith("```")) {
            int firstNl = cleaned.indexOf('\n');
            if (firstNl > 0) cleaned = cleaned.substring(firstNl + 1);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) return;
        cleaned = cleaned.substring(start, end + 1);

        JsonNode node = objectMapper.readTree(cleaned);
        String name = node.path("name").asText(null);
        String summary = node.path("summary").asText(null);
        String explanation = node.path("explanation").asText(null);

        if (name != null || summary != null) {
            result.setSuggestedTraining(new AdaptiveSuggestionDto.SuggestedTraining(name, summary));
        }
        if (explanation != null) {
            result.setExplanation(explanation);
        }
    }
}
