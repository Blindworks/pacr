package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_coach_sessions")
public class DailyCoachSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "wellbeing_context_json", columnDefinition = "TEXT")
    private String wellbeingContextJson;

    @Column(name = "user_feeling_score")
    private Integer userFeelingScore;

    @Column(name = "user_feeling_text", length = 500)
    private String userFeelingText;

    @Column(name = "ai_recommendation", columnDefinition = "TEXT")
    private String aiRecommendation;

    @Column(name = "ai_suggested_action", length = 20)
    private String aiSuggestedAction;

    @Column(name = "user_decision", length = 20)
    private String userDecision;

    @Column(name = "restructuring_summary_json", columnDefinition = "TEXT")
    private String restructuringSummaryJson;

    public DailyCoachSession() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getWellbeingContextJson() { return wellbeingContextJson; }
    public void setWellbeingContextJson(String wellbeingContextJson) { this.wellbeingContextJson = wellbeingContextJson; }

    public Integer getUserFeelingScore() { return userFeelingScore; }
    public void setUserFeelingScore(Integer userFeelingScore) { this.userFeelingScore = userFeelingScore; }

    public String getUserFeelingText() { return userFeelingText; }
    public void setUserFeelingText(String userFeelingText) { this.userFeelingText = userFeelingText; }

    public String getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(String aiRecommendation) { this.aiRecommendation = aiRecommendation; }

    public String getAiSuggestedAction() { return aiSuggestedAction; }
    public void setAiSuggestedAction(String aiSuggestedAction) { this.aiSuggestedAction = aiSuggestedAction; }

    public String getUserDecision() { return userDecision; }
    public void setUserDecision(String userDecision) { this.userDecision = userDecision; }

    public String getRestructuringSummaryJson() { return restructuringSummaryJson; }
    public void setRestructuringSummaryJson(String restructuringSummaryJson) { this.restructuringSummaryJson = restructuringSummaryJson; }
}
