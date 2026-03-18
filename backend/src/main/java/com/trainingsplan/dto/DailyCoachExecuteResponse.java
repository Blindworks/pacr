package com.trainingsplan.dto;

import java.util.List;

public class DailyCoachExecuteResponse {

    private String message;
    private List<RestructuringChange> changes;

    public DailyCoachExecuteResponse() {}

    public static class RestructuringChange {
        private Long entryId;
        private String action;  // MOVED | SKIPPED | UNCHANGED
        private String newDate; // nullable, only set for MOVED

        public RestructuringChange() {}

        public Long getEntryId() { return entryId; }
        public void setEntryId(Long entryId) { this.entryId = entryId; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getNewDate() { return newDate; }
        public void setNewDate(String newDate) { this.newDate = newDate; }
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<RestructuringChange> getChanges() { return changes; }
    public void setChanges(List<RestructuringChange> changes) { this.changes = changes; }
}
