package com.trainingsplan.dto;

public class CycleStatusDto {

    private String currentPhase;
    private int currentDay;
    private int cycleLength;
    private int daysRemainingInPhase;
    private String nextPhase;
    private boolean shouldShowNewCyclePrompt;

    public CycleStatusDto() {}

    public CycleStatusDto(String currentPhase, int currentDay, int cycleLength,
                          int daysRemainingInPhase, String nextPhase, boolean shouldShowNewCyclePrompt) {
        this.currentPhase = currentPhase;
        this.currentDay = currentDay;
        this.cycleLength = cycleLength;
        this.daysRemainingInPhase = daysRemainingInPhase;
        this.nextPhase = nextPhase;
        this.shouldShowNewCyclePrompt = shouldShowNewCyclePrompt;
    }

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }

    public int getCurrentDay() { return currentDay; }
    public void setCurrentDay(int currentDay) { this.currentDay = currentDay; }

    public int getCycleLength() { return cycleLength; }
    public void setCycleLength(int cycleLength) { this.cycleLength = cycleLength; }

    public int getDaysRemainingInPhase() { return daysRemainingInPhase; }
    public void setDaysRemainingInPhase(int daysRemainingInPhase) { this.daysRemainingInPhase = daysRemainingInPhase; }

    public String getNextPhase() { return nextPhase; }
    public void setNextPhase(String nextPhase) { this.nextPhase = nextPhase; }

    public boolean isShouldShowNewCyclePrompt() { return shouldShowNewCyclePrompt; }
    public void setShouldShowNewCyclePrompt(boolean shouldShowNewCyclePrompt) { this.shouldShowNewCyclePrompt = shouldShowNewCyclePrompt; }
}
