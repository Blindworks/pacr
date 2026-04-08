package com.trainingsplan.dto;

public class AdaptiveSuggestionDto {

    public static class PlannedTraining {
        private String name;
        private String description;
        private Integer durationMinutes;
        private String intensityLevel;
        private String trainingType;

        public PlannedTraining() {}

        public PlannedTraining(String name, String description, Integer durationMinutes,
                               String intensityLevel, String trainingType) {
            this.name = name;
            this.description = description;
            this.durationMinutes = durationMinutes;
            this.intensityLevel = intensityLevel;
            this.trainingType = trainingType;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public String getIntensityLevel() { return intensityLevel; }
        public void setIntensityLevel(String intensityLevel) { this.intensityLevel = intensityLevel; }
        public String getTrainingType() { return trainingType; }
        public void setTrainingType(String trainingType) { this.trainingType = trainingType; }
    }

    public static class SuggestedTraining {
        private String name;
        private String summary;

        public SuggestedTraining() {}

        public SuggestedTraining(String name, String summary) {
            this.name = name;
            this.summary = summary;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }

    private PlannedTraining originalTraining;
    private SuggestedTraining suggestedTraining;
    private String explanation;
    private String currentPhase;
    private boolean aiEnabled;

    public AdaptiveSuggestionDto() {}

    public PlannedTraining getOriginalTraining() { return originalTraining; }
    public void setOriginalTraining(PlannedTraining originalTraining) { this.originalTraining = originalTraining; }
    public SuggestedTraining getSuggestedTraining() { return suggestedTraining; }
    public void setSuggestedTraining(SuggestedTraining suggestedTraining) { this.suggestedTraining = suggestedTraining; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }
    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }
}
