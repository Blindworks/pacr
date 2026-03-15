package com.trainingsplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "body_metrics")
public class BodyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /** e.g. "VO2MAX", "VO2MAX_HR_CORRECTED" */
    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(nullable = false)
    private Double value;

    /** e.g. "ml/kg/min", "bpm" */
    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "recorded_at", nullable = false)
    private LocalDate recordedAt;

    /** ID of the CompletedTraining this was derived from (nullable for manually entered values) */
    @Column(name = "source_activity_id")
    private Long sourceActivityId;

    /** Enum-Werte für ACWR_FLAG ("RED", "GREEN", …) und RECOMMENDATION ("REST", "HARD", …) */
    @Column(name = "string_value", length = 20)
    private String stringValue;

    /** JSON-Array der Top-3 Readiness-Gründe oder Coach-Bullets (z.B. ["Grund 1","Grund 2"]) */
    @Column(name = "reasons_json", length = 500)
    private String reasonsJson;

    /** Freitext-Ergänzung: ACWR-Meldung oder Coach-Titel */
    @Column(name = "auxiliary_text", length = 100)
    private String auxiliaryText;

    public BodyMetric() {}

    // Getters and setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDate getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDate recordedAt) { this.recordedAt = recordedAt; }
    public Long getSourceActivityId() { return sourceActivityId; }
    public void setSourceActivityId(Long sourceActivityId) { this.sourceActivityId = sourceActivityId; }
    public String getStringValue() { return stringValue; }
    public void setStringValue(String stringValue) { this.stringValue = stringValue; }
    public String getReasonsJson() { return reasonsJson; }
    public void setReasonsJson(String reasonsJson) { this.reasonsJson = reasonsJson; }
    public String getAuxiliaryText() { return auxiliaryText; }
    public void setAuxiliaryText(String auxiliaryText) { this.auxiliaryText = auxiliaryText; }
}
