package com.trainingsplan.service.readiness;

import com.trainingsplan.service.readiness.ReadinessDeductionCalculator.Deduction;
import com.trainingsplan.service.readiness.ReadinessDeductionCalculator.WeightedResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReadinessDeductionCalculatorTest {

    // ─── weightedSum ──────────────────────────────────────────────────────────

    @Test
    void weightedSum_allZero_isZero() {
        WeightedResult result = ReadinessDeductionCalculator.weightedSum(0.0, 0.0, 0.0);
        assertEquals(0.0, result.value(), 1e-9);
    }

    @Test
    void weightedSum_nullsAreTreatedAsZero() {
        WeightedResult result = ReadinessDeductionCalculator.weightedSum(null, null, null);
        assertEquals(0.0, result.value(), 1e-9);
    }

    @Test
    void weightedSum_appliesCorrectWeights() {
        // T-1 = 10 × 0.80 = 8.0
        // T-2 = 10 × 0.55 = 5.5
        // T-3 = 10 × 0.30 = 3.0
        // Σ = 16.5
        WeightedResult result = ReadinessDeductionCalculator.weightedSum(10.0, 10.0, 10.0);
        assertEquals(16.5, result.value(), 1e-9);
    }

    @Test
    void weightedSum_exposesContributingDays() {
        WeightedResult result = ReadinessDeductionCalculator.weightedSum(5.0, 8.0, 2.0);
        assertEquals(5.0, result.t1());
        assertEquals(8.0, result.t2());
        assertEquals(2.0, result.t3());
    }

    @Test
    void weightedSumFromMap_picksCorrectDays() {
        LocalDate today = LocalDate.of(2026, 4, 14);
        Map<LocalDate, Double> strain = new HashMap<>();
        strain.put(today.minusDays(1), 12.0);
        strain.put(today.minusDays(2), 8.0);
        strain.put(today.minusDays(3), 20.0);
        strain.put(today, 0.0); // heute sollte ignoriert werden

        WeightedResult result = ReadinessDeductionCalculator.weightedSumFromMap(strain, today);
        // 12*0.80 + 8*0.55 + 20*0.30 = 9.6 + 4.4 + 6.0 = 20.0
        assertEquals(20.0, result.value(), 1e-9);
    }

    @Test
    void weightedSumFromMap_missingDaysTreatedAsZero() {
        LocalDate today = LocalDate.of(2026, 4, 14);
        Map<LocalDate, Double> strain = new HashMap<>();
        strain.put(today.minusDays(1), 10.0);
        // T-2 und T-3 fehlen

        WeightedResult result = ReadinessDeductionCalculator.weightedSumFromMap(strain, today);
        assertEquals(8.0, result.value(), 1e-9); // 10 * 0.80
    }

    // ─── strainDeduction ──────────────────────────────────────────────────────

    @Test
    void strainDeduction_belowModerate_returnsEmpty() {
        WeightedResult w = ReadinessDeductionCalculator.weightedSum(5.0, 5.0, 5.0);
        // 5*0.80 + 5*0.55 + 5*0.30 = 8.25 → unter 9.0-Schwelle
        assertTrue(ReadinessDeductionCalculator.strainDeduction(w).isEmpty());
    }

    @Test
    void strainDeduction_moderate_returnsSixPoints() {
        WeightedResult w = ReadinessDeductionCalculator.weightedSum(8.0, 4.0, 5.0);
        // 8*0.80 + 4*0.55 + 5*0.30 = 6.4 + 2.2 + 1.5 = 10.1 → > 9, < 14
        Optional<Deduction> d = ReadinessDeductionCalculator.strainDeduction(w);
        assertTrue(d.isPresent());
        assertEquals(6, d.get().amount());
        assertEquals("weighted_strain", d.get().source());
    }

    @Test
    void strainDeduction_high_returnsTwelvePoints() {
        // Erwartung: gewichtete Summe zwischen 14 und 20
        WeightedResult w = ReadinessDeductionCalculator.weightedSum(12.0, 8.0, 10.0);
        // 12*0.80 + 8*0.55 + 10*0.30 = 9.6 + 4.4 + 3.0 = 17.0
        Optional<Deduction> d = ReadinessDeductionCalculator.strainDeduction(w);
        assertTrue(d.isPresent());
        assertEquals(12, d.get().amount());
    }

    @Test
    void strainDeduction_veryHigh_returnsEighteenPoints() {
        WeightedResult w = ReadinessDeductionCalculator.weightedSum(20.0, 15.0, 10.0);
        // 20*0.80 + 15*0.55 + 10*0.30 = 16 + 8.25 + 3 = 27.25
        Optional<Deduction> d = ReadinessDeductionCalculator.strainDeduction(w);
        assertTrue(d.isPresent());
        assertEquals(18, d.get().amount());
    }

    // ─── z45Deduction ─────────────────────────────────────────────────────────

    @Test
    void z45Deduction_belowModerate_returnsEmpty() {
        WeightedResult w = ReadinessDeductionCalculator.weightedSum(5.0, 5.0, 5.0);
        // 8.25 Min gewichtet → unter 12-Schwelle
        assertTrue(ReadinessDeductionCalculator.z45Deduction(w).isEmpty());
    }

    @Test
    void z45Deduction_high_returnsEightPoints() {
        WeightedResult w = ReadinessDeductionCalculator.weightedSum(20.0, 10.0, 5.0);
        // 20*0.80 + 10*0.55 + 5*0.30 = 16 + 5.5 + 1.5 = 23.0 → > 20, < 30
        Optional<Deduction> d = ReadinessDeductionCalculator.z45Deduction(w);
        assertTrue(d.isPresent());
        assertEquals(8, d.get().amount());
    }

    // ─── Realistisches Szenario: harte Samstag-Session ────────────────────────

    @Test
    void saturdaySession_threeDaysAgo_stillTriggersStrainDeduction() {
        // User macht Samstag eine harte Session (strain21 = 18), Sonntag+Montag nur wenig Training
        LocalDate tuesday = LocalDate.of(2026, 4, 14);
        Map<LocalDate, Double> strain = new HashMap<>();
        strain.put(tuesday.minusDays(3), 18.0); // Samstag: sehr hart
        strain.put(tuesday.minusDays(2), 5.0);  // Sonntag: locker
        strain.put(tuesday.minusDays(1), 5.0);  // Montag: locker

        WeightedResult w = ReadinessDeductionCalculator.weightedSumFromMap(strain, tuesday);
        // 5*0.80 + 5*0.55 + 18*0.30 = 4 + 2.75 + 5.4 = 12.15 → moderate (−6)
        assertEquals(12.15, w.value(), 1e-9);

        Optional<Deduction> d = ReadinessDeductionCalculator.strainDeduction(w);
        assertTrue(d.isPresent(), "Samstag-Session soll auch am Dienstag noch spürbar sein");
        assertEquals(6, d.get().amount());
    }

    @Test
    void hardWeekendWithModerateWeekdays_triggersHighDeduction() {
        LocalDate tuesday = LocalDate.of(2026, 4, 14);
        Map<LocalDate, Double> strain = new HashMap<>();
        strain.put(tuesday.minusDays(3), 20.0); // Samstag: sehr hart
        strain.put(tuesday.minusDays(2), 12.0); // Sonntag: moderate
        strain.put(tuesday.minusDays(1), 8.0);  // Montag: Erholung

        WeightedResult w = ReadinessDeductionCalculator.weightedSumFromMap(strain, tuesday);
        // 8*0.80 + 12*0.55 + 20*0.30 = 6.4 + 6.6 + 6.0 = 19.0
        assertEquals(19.0, w.value(), 1e-9);

        Optional<Deduction> d = ReadinessDeductionCalculator.strainDeduction(w);
        assertTrue(d.isPresent());
        assertEquals(12, d.get().amount()); // "high"-Schwelle (> 14)
    }
}
