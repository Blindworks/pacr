package com.trainingsplan.dto;

import com.trainingsplan.entity.UserTrainingEntry;

import java.util.List;

public class DailyCoachContextDto {

    private String date;
    private List<UserTrainingEntry> plannedEntries;
    private AsthmaRiskDto asthmaRisk;
    private CyclePhaseDto cyclePhase;
    private ExistingSessionDto existingSession;

    public DailyCoachContextDto() {}

    // ---- Inner DTOs ----

    public static class AsthmaRiskDto {
        private int riskIndex;
        private String pollenSummary;
        private String level; // LOW / MEDIUM / HIGH

        public AsthmaRiskDto() {}

        public int getRiskIndex() { return riskIndex; }
        public void setRiskIndex(int riskIndex) { this.riskIndex = riskIndex; }

        public String getPollenSummary() { return pollenSummary; }
        public void setPollenSummary(String pollenSummary) { this.pollenSummary = pollenSummary; }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
    }

    public static class CyclePhaseDto {
        private String phase;
        private int energyLevel;
        private String recommendation;

        public CyclePhaseDto() {}

        public String getPhase() { return phase; }
        public void setPhase(String phase) { this.phase = phase; }

        public int getEnergyLevel() { return energyLevel; }
        public void setEnergyLevel(int energyLevel) { this.energyLevel = energyLevel; }

        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }

    public static class ExistingSessionDto {
        private Long id;
        private String aiRecommendation;
        private String aiSuggestedAction;
        private String userDecision;

        public ExistingSessionDto() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getAiRecommendation() { return aiRecommendation; }
        public void setAiRecommendation(String aiRecommendation) { this.aiRecommendation = aiRecommendation; }

        public String getAiSuggestedAction() { return aiSuggestedAction; }
        public void setAiSuggestedAction(String aiSuggestedAction) { this.aiSuggestedAction = aiSuggestedAction; }

        public String getUserDecision() { return userDecision; }
        public void setUserDecision(String userDecision) { this.userDecision = userDecision; }
    }

    // ---- Main getters/setters ----

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<UserTrainingEntry> getPlannedEntries() { return plannedEntries; }
    public void setPlannedEntries(List<UserTrainingEntry> plannedEntries) { this.plannedEntries = plannedEntries; }

    public AsthmaRiskDto getAsthmaRisk() { return asthmaRisk; }
    public void setAsthmaRisk(AsthmaRiskDto asthmaRisk) { this.asthmaRisk = asthmaRisk; }

    public CyclePhaseDto getCyclePhase() { return cyclePhase; }
    public void setCyclePhase(CyclePhaseDto cyclePhase) { this.cyclePhase = cyclePhase; }

    public ExistingSessionDto getExistingSession() { return existingSession; }
    public void setExistingSession(ExistingSessionDto existingSession) { this.existingSession = existingSession; }
}
