package com.trainingsplan.service.readiness;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Pure-math Utility für zeitgewichtete Ermüdungs-Deductions des Readiness-Scores.
 *
 * <p>Betrachtet die letzten drei Tage vor {@code date} (T-1, T-2, T-3) mit exponentiell
 * abklingender Gewichtung. Der aktuelle Tag (T0) wird bewusst ausgeklammert, weil er
 * noch nicht abgeschlossen ist und der Score den Zustand "vor dem heutigen Training"
 * repräsentiert.
 *
 * <pre>
 * Gewichte:
 *   T-1 (gestern)     → 0.80
 *   T-2 (vorgestern)  → 0.55
 *   T-3 (vor 3 Tagen) → 0.30
 * </pre>
 *
 * <p>Diese Kurve entspricht näherungsweise einer exponentiellen Abklingzeit τ ≈ 2 Tagen,
 * was dem physiologischen Recovery-Verständnis für gemischte Energiesysteme entspricht.
 *
 * <p>Die Klasse ist stateless und wird von {@link com.trainingsplan.service.MetricsKernelService}
 * und {@link com.trainingsplan.service.ReadinessService} gemeinsam genutzt, damit beide
 * Readiness-Pfade (neues body_metrics + legacy daily_metrics) die gleiche Logik anwenden.
 */
public final class ReadinessDeductionCalculator {

    /** Gewicht für T-1 (gestern). */
    public static final double WEIGHT_T1 = 0.80;
    /** Gewicht für T-2 (vorgestern). */
    public static final double WEIGHT_T2 = 0.55;
    /** Gewicht für T-3 (vor 3 Tagen). */
    public static final double WEIGHT_T3 = 0.30;

    // Strain-Schwellen (gewichtete Summe über T-1..T-3)
    public static final double STRAIN_THRESHOLD_VERY_HIGH = 20.0;
    public static final double STRAIN_THRESHOLD_HIGH = 14.0;
    public static final double STRAIN_THRESHOLD_MODERATE = 9.0;

    public static final int STRAIN_DEDUCTION_VERY_HIGH = 18;
    public static final int STRAIN_DEDUCTION_HIGH = 12;
    public static final int STRAIN_DEDUCTION_MODERATE = 6;

    // Z4+Z5-Schwellen (gewichtete Summe über T-1..T-3)
    public static final double Z45_THRESHOLD_VERY_HIGH = 30.0;
    public static final double Z45_THRESHOLD_HIGH = 20.0;
    public static final double Z45_THRESHOLD_MODERATE = 12.0;

    public static final int Z45_DEDUCTION_VERY_HIGH = 12;
    public static final int Z45_DEDUCTION_HIGH = 8;
    public static final int Z45_DEDUCTION_MODERATE = 4;

    private ReadinessDeductionCalculator() {
        // Utility class
    }

    /**
     * Berechnet die gewichtete Summe von drei Werten gemäß {@link #WEIGHT_T1}, {@link #WEIGHT_T2}, {@link #WEIGHT_T3}.
     * {@code null}-Eingaben werden als 0.0 behandelt.
     */
    public static WeightedResult weightedSum(Double valueT1, Double valueT2, Double valueT3) {
        double v1 = valueT1 != null ? valueT1 : 0.0;
        double v2 = valueT2 != null ? valueT2 : 0.0;
        double v3 = valueT3 != null ? valueT3 : 0.0;
        double sum = v1 * WEIGHT_T1 + v2 * WEIGHT_T2 + v3 * WEIGHT_T3;
        return new WeightedResult(sum, v1, v2, v3);
    }

    /**
     * Hilfsfunktion: Liest für die drei Vortage (T-1, T-2, T-3) Werte aus einer Map.
     */
    public static WeightedResult weightedSumFromMap(Map<LocalDate, Double> valuesByDate, LocalDate date) {
        Double v1 = valuesByDate != null ? valuesByDate.get(date.minusDays(1)) : null;
        Double v2 = valuesByDate != null ? valuesByDate.get(date.minusDays(2)) : null;
        Double v3 = valuesByDate != null ? valuesByDate.get(date.minusDays(3)) : null;
        return weightedSum(v1, v2, v3);
    }

    /**
     * Gibt eine Strain-Deduction zurück, falls die gewichtete Strain-Summe eine Schwelle überschreitet.
     */
    public static Optional<Deduction> strainDeduction(WeightedResult weighted) {
        double w = weighted.value();
        if (w > STRAIN_THRESHOLD_VERY_HIGH) {
            return Optional.of(new Deduction(
                    STRAIN_DEDUCTION_VERY_HIGH,
                    "Sehr hohe akute Belastung (letzte 3 Tage)",
                    w, STRAIN_THRESHOLD_VERY_HIGH, "weighted_strain", weighted.contributingDays()));
        }
        if (w > STRAIN_THRESHOLD_HIGH) {
            return Optional.of(new Deduction(
                    STRAIN_DEDUCTION_HIGH,
                    "Hohe akute Belastung (letzte 3 Tage)",
                    w, STRAIN_THRESHOLD_HIGH, "weighted_strain", weighted.contributingDays()));
        }
        if (w > STRAIN_THRESHOLD_MODERATE) {
            return Optional.of(new Deduction(
                    STRAIN_DEDUCTION_MODERATE,
                    "Moderate akute Belastung (letzte 3 Tage)",
                    w, STRAIN_THRESHOLD_MODERATE, "weighted_strain", weighted.contributingDays()));
        }
        return Optional.empty();
    }

    /**
     * Gibt eine Z4+Z5-Minuten-Deduction zurück, falls die gewichtete Summe eine Schwelle überschreitet.
     */
    public static Optional<Deduction> z45Deduction(WeightedResult weighted) {
        double w = weighted.value();
        if (w > Z45_THRESHOLD_VERY_HIGH) {
            return Optional.of(new Deduction(
                    Z45_DEDUCTION_VERY_HIGH,
                    "Sehr viele Hochintensivminuten (letzte 3 Tage)",
                    w, Z45_THRESHOLD_VERY_HIGH, "weighted_z4z5", weighted.contributingDays()));
        }
        if (w > Z45_THRESHOLD_HIGH) {
            return Optional.of(new Deduction(
                    Z45_DEDUCTION_HIGH,
                    "Viele Hochintensivminuten (letzte 3 Tage, >20 min Z4/Z5 gewichtet)",
                    w, Z45_THRESHOLD_HIGH, "weighted_z4z5", weighted.contributingDays()));
        }
        if (w > Z45_THRESHOLD_MODERATE) {
            return Optional.of(new Deduction(
                    Z45_DEDUCTION_MODERATE,
                    "Erhöhte Hochintensivminuten (letzte 3 Tage)",
                    w, Z45_THRESHOLD_MODERATE, "weighted_z4z5", weighted.contributingDays()));
        }
        return Optional.empty();
    }

    /**
     * Ergebnis der gewichteten Summe — enthält den Summen-Wert und die drei Roh-Werte für Diagnose.
     */
    public record WeightedResult(double value, double t1, double t2, double t3) {
        /** Gibt die Roh-Werte pro Tags-Offset zurück (für Diagnose / Explain-DTO). */
        public Map<String, Double> contributingDays() {
            Map<String, Double> map = new LinkedHashMap<>();
            map.put("T-1", t1);
            map.put("T-2", t2);
            map.put("T-3", t3);
            return map;
        }
    }

    /**
     * Strukturierte Deduction mit vollständigem Diagnose-Kontext.
     *
     * @param amount          Punktabzug (positiv, wird vom Basis-Score abgezogen)
     * @param reason          menschenlesbarer Grund (auf Deutsch, für Dashboard)
     * @param inputValue      tatsächlich gemessener Wert (z.B. gewichtete Summe)
     * @param threshold       Schwelle, die überschritten wurde
     * @param source          maschinenlesbare Quelle (z.B. "weighted_strain", "acwr_flag", "sleep_score")
     * @param contributingDays optional: beigetragene Tageswerte (kann {@code null} sein)
     */
    public record Deduction(int amount, String reason, Double inputValue, Double threshold,
                            String source, Map<String, Double> contributingDays) {
    }
}
