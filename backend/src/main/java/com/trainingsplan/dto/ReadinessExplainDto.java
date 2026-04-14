package com.trainingsplan.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Diagnose-DTO für den Readiness-Score: enthält den finalen Score, die Empfehlung,
 * alle angewandten Deductions inklusive Rohwerten und die aggregierten Input-Kennzahlen.
 *
 * <p>Wird vom Endpoint {@code GET /api/daily-metrics/explain} zurückgegeben und erlaubt
 * nachzuvollziehen, warum der Score an einem bestimmten Tag den berechneten Wert hat.
 */
public record ReadinessExplainDto(
        LocalDate date,
        int score,
        int baseScore,
        String recommendation,
        List<DeductionDto> deductions,
        ReadinessInputsDto inputs
) {
    /**
     * Ein einzelner Score-Abzug mit Kontext.
     *
     * @param amount          Punktabzug (positiv)
     * @param reason          menschenlesbarer Grund (DE)
     * @param inputValue      gemessener Wert
     * @param threshold       überschrittene Schwelle
     * @param source          maschinenlesbare Quelle (z.B. "weighted_strain")
     * @param contributingDays optional: Beitrag pro Tag (T-1, T-2, T-3) — Map bleibt {@code null} wenn nicht anwendbar
     */
    public record DeductionDto(
            int amount,
            String reason,
            Double inputValue,
            Double threshold,
            String source,
            Map<String, Double> contributingDays
    ) {}

    /**
     * Aggregierte Input-Kennzahlen für den Score, die zur Nachvollziehbarkeit mit ausgeliefert werden.
     */
    public record ReadinessInputsDto(
            String acwrFlag,
            Double acwr,
            Double acute7,
            Double chronic28,
            Map<LocalDate, Double> dailyStrain,
            Map<LocalDate, Double> z4z5PerDay,
            Double weightedStrain,
            Double weightedZ4z5,
            Double lastDecouplingPct,
            LocalDate lastDecouplingDate,
            Integer sleepScore,
            String hrvStatus,
            Integer bodyBattery
    ) {}
}
