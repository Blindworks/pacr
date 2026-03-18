package com.trainingsplan.dto;

public class DailyCoachExecuteRequest {

    private Long sessionId;
    private String decision; // PROCEED | MODIFY | SKIP

    public DailyCoachExecuteRequest() {}

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}
