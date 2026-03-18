package com.trainingsplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.DailyCoachExecuteRequest;
import com.trainingsplan.dto.DailyCoachExecuteResponse;
import com.trainingsplan.dto.DailyCoachRecommendationRequest;
import com.trainingsplan.dto.DailyCoachRecommendationResponse;
import com.trainingsplan.dto.DailyCoachContextDto;
import com.trainingsplan.entity.CoachDecision;
import com.trainingsplan.entity.DailyCoachSession;
import com.trainingsplan.entity.UserTrainingEntry;
import com.trainingsplan.repository.DailyCoachSessionRepository;
import com.trainingsplan.repository.UserTrainingEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
public class DailyCoachService {

    private static final Logger log = LoggerFactory.getLogger(DailyCoachService.class);

    private static final String FALLBACK_RECOMMENDATION =
            "Dein Trainingscoach konnte heute keine Empfehlung erstellen. Höre auf deinen Körper und trainiere nach deinem Gefühl.";
    private static final String FALLBACK_ACTION = "PROCEED";
    private static final String FALLBACK_PREVIEW = "Training wie geplant";

    @Autowired
    private DailyCoachContextService contextService;

    @Autowired
    private DailyCoachPromptBuilder promptBuilder;

    @Autowired
    private LLMClientService llmClient;

    @Autowired
    private DailyCoachSessionRepository sessionRepository;

    @Autowired
    private TrainingRestructureService restructureService;

    @Autowired
    private UserTrainingEntryRepository entryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public DailyCoachRecommendationResponse getRecommendation(Long userId, DailyCoachRecommendationRequest req) {
        LocalDate date = LocalDate.parse(req.getDate());

        // 1. Build context for today
        DailyCoachContextDto context = contextService.getContext(userId, date);

        // 2. Load upcoming trainings for the next 7 days to enrich the prompt
        List<UserTrainingEntry> upcoming = entryRepository.findCalendarEntriesForUser(
                userId, date.plusDays(1), date.plusDays(7));

        // 3. Build prompt
        String prompt = promptBuilder.buildPrompt(
                context,
                req.getFeelingScore() != null ? req.getFeelingScore() : 3,
                req.getFeelingText(),
                upcoming);

        // 4. Call LLM
        String llmResponse;
        try {
            llmResponse = llmClient.generateText(prompt);
        } catch (Exception e) {
            log.error("LLM call failed for userId={} date={}: {}", userId, date, e.getMessage());
            llmResponse = null;
        }

        // 5. Parse LLM JSON response — fall back gracefully on any error
        String recommendationText = FALLBACK_RECOMMENDATION;
        String suggestedAction = FALLBACK_ACTION;
        String restructuringPreview = FALLBACK_PREVIEW;

        if (llmResponse != null) {
            try {
                // Strip possible markdown code fences before parsing
                String json = llmResponse.trim();
                if (json.startsWith("```")) {
                    int start = json.indexOf('{');
                    int end = json.lastIndexOf('}');
                    if (start >= 0 && end > start) {
                        json = json.substring(start, end + 1);
                    }
                }
                JsonNode node = objectMapper.readTree(json);
                if (node.has("recommendationText")) {
                    recommendationText = node.get("recommendationText").asText(FALLBACK_RECOMMENDATION);
                }
                if (node.has("suggestedAction")) {
                    suggestedAction = node.get("suggestedAction").asText(FALLBACK_ACTION);
                }
                if (node.has("restructuringPreview")) {
                    restructuringPreview = node.get("restructuringPreview").asText(FALLBACK_PREVIEW);
                }
            } catch (Exception e) {
                log.warn("Failed to parse LLM response for userId={} date={}: {}", userId, date, e.getMessage());
            }
        }

        // 6. Persist session
        DailyCoachSession session = new DailyCoachSession();
        session.setUserId(userId);
        session.setSessionDate(date);
        session.setCreatedAt(LocalDateTime.now());
        session.setUserFeelingScore(req.getFeelingScore());
        session.setUserFeelingText(req.getFeelingText());
        session.setAiRecommendation(recommendationText);
        session.setAiSuggestedAction(suggestedAction);
        session = sessionRepository.save(session);

        // 7. Return response
        return new DailyCoachRecommendationResponse(
                session.getId(), recommendationText, suggestedAction, restructuringPreview);
    }

    @Transactional
    public DailyCoachExecuteResponse executeDecision(Long userId, DailyCoachExecuteRequest req) {
        // 1. Load and authorise session
        DailyCoachSession session = sessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found: " + req.getSessionId()));
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: session does not belong to current user");
        }

        // 2. Persist user decision
        session.setUserDecision(req.getDecision());

        // 3. Restructure plan
        CoachDecision decision;
        try {
            decision = CoachDecision.valueOf(req.getDecision());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid decision value: " + req.getDecision());
        }
        List<DailyCoachExecuteResponse.RestructuringChange> changes =
                restructureService.restructure(userId, session.getSessionDate(), decision);

        // 4. Store restructuring summary as JSON
        try {
            session.setRestructuringSummaryJson(objectMapper.writeValueAsString(changes));
        } catch (Exception e) {
            log.warn("Failed to serialise restructuring summary for sessionId={}: {}", session.getId(), e.getMessage());
        }
        sessionRepository.save(session);

        // 5. Build response
        DailyCoachExecuteResponse response = new DailyCoachExecuteResponse();
        response.setMessage("Plan wurde erfolgreich angepasst.");
        response.setChanges(changes);
        return response;
    }
}
