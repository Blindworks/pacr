package com.trainingsplan.dto;

public class DailyCoachRecommendationResponse {

    private Long sessionId;
    private String recommendationText;
    private String suggestedAction; // PROCEED | MODIFY | SKIP
    private String restructuringPreview;

    public DailyCoachRecommendationResponse() {}

    public DailyCoachRecommendationResponse(Long sessionId, String recommendationText,
                                             String suggestedAction, String restructuringPreview) {
        this.sessionId = sessionId;
        this.recommendationText = recommendationText;
        this.suggestedAction = suggestedAction;
        this.restructuringPreview = restructuringPreview;
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getRecommendationText() { return recommendationText; }
    public void setRecommendationText(String recommendationText) { this.recommendationText = recommendationText; }

    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }

    public String getRestructuringPreview() { return restructuringPreview; }
    public void setRestructuringPreview(String restructuringPreview) { this.restructuringPreview = restructuringPreview; }
}
