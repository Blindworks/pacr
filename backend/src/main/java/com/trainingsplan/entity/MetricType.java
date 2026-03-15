package com.trainingsplan.entity;

/**
 * Zentrales Verzeichnis aller metric_type-Konstanten für body_metrics.
 * Einige Typen (VO2MAX, VO2MAX_HR_CORRECTED) sind aktivitätsbezogen (sourceActivityId gesetzt),
 * die restlichen sind tagesbezogen (sourceActivityId = null).
 */
public final class MetricType {

    // ── Aktivitätsbezogen ─────────────────────────────────────────────────
    public static final String VO2MAX               = "VO2MAX";
    public static final String VO2MAX_HR_CORRECTED  = "VO2MAX_HR_CORRECTED";

    // ── Tagesbezogen: Belastung ───────────────────────────────────────────
    public static final String DAILY_STRAIN21       = "DAILY_STRAIN21";
    public static final String DAILY_TRIMP          = "DAILY_TRIMP";

    // ── Tagesbezogen: Effizienz ───────────────────────────────────────────
    public static final String EF7                  = "EF7";
    public static final String EF28                 = "EF28";

    // ── Tagesbezogen: Lastmodell ──────────────────────────────────────────
    public static final String ACUTE7               = "ACUTE7";
    public static final String CHRONIC28            = "CHRONIC28";
    public static final String ACWR                 = "ACWR";
    /** stringValue = "BLUE"|"GREEN"|"ORANGE"|"RED", auxiliaryText = Meldungstext */
    public static final String ACWR_FLAG            = "ACWR_FLAG";

    // ── Tagesbezogen: Schlafdaten ─────────────────────────────────────────
    public static final String BODY_BATTERY         = "BODY_BATTERY";

    // ── Tagesbezogen: Readiness & Coach ───────────────────────────────────
    public static final String READINESS_SCORE      = "READINESS_SCORE";
    /** stringValue = "REST"|"EASY"|"MODERATE"|"HARD", reasonsJson = Top-3-Gründe */
    public static final String RECOMMENDATION       = "RECOMMENDATION";
    /** value = 0.0 (Platzhalter), auxiliaryText = Titel, reasonsJson = Bullets-JSON */
    public static final String COACH_CARD           = "COACH_CARD";

    private MetricType() {}
}
