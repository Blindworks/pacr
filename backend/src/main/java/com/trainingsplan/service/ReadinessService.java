package com.trainingsplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.Recommendation;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import com.trainingsplan.service.readiness.ReadinessDeductionCalculator;
import com.trainingsplan.service.readiness.ReadinessDeductionCalculator.Deduction;
import com.trainingsplan.service.readiness.ReadinessDeductionCalculator.WeightedResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Computes the Readiness Proxy score (v2) from load and activity metrics.
 *
 * <p>No HRV or sleep data required — uses only Strava/load-derived signals.
 *
 * <h3>Heuristic v2 (zeitgewichtet)</h3>
 * <pre>
 * Base score: 100
 *
 * Deductions:
 *   acwrFlag == RED    → −30  (recommendation capped at EASY)
 *   acwrFlag == ORANGE → −18
 *   weighted strain21 over T-1..T-3   (0.80/0.55/0.30 weights) → −6 / −12 / −18
 *   weighted (z4Min+z5Min) over T-1..T-3                        → −4 / −8  / −12
 *   last eligible decouplingPct > 10% → −10
 *   last eligible decouplingPct > 5%  → −5
 *
 * Score clamped to [0, 100].
 * </pre>
 *
 * <p>Die zeitgewichteten Deductions ersetzen die vorherigen Einzel-Tages-Checks, damit
 * harte Sessions mehrere Tage nachwirken (Recovery-τ ≈ 2 Tage).
 *
 * <h3>Recommendation thresholds</h3>
 * <pre>
 *   score &lt; 30  → REST
 *   30–49       → EASY
 *   50–69       → MODERATE
 *   ≥ 70        → HARD
 * </pre>
 *
 * When acwrFlag == RED the recommendation is capped at EASY (cannot be MODERATE or HARD).
 *
 * @see com.trainingsplan.service.readiness.ReadinessDeductionCalculator
 */
@Service
public class ReadinessService {

    @Autowired
    private DailyMetricsRepository dailyMetricsRepository;

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    @Autowired
    private CoachCardService coachCardService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Computes and persists the readiness score for {@code user} on {@code date}.
     *
     * <p>Must be called AFTER {@link LoadModelService#updateAcwr} has persisted today's
     * ACWR data, so that the acwrFlag is available.
     */
    public void compute(User user, LocalDate date) {
        int baseScore = 100;
        List<Deduction> deductions = new ArrayList<>();
        boolean redFlag = false;

        // ── 1. ACWR flag ──────────────────────────────────────────────────────
        AcwrFlag acwrFlag = dailyMetricsRepository
                .findByUserIdAndDate(user.getId(), date)
                .map(DailyMetrics::getAcwrFlag)
                .orElse(null);

        if (acwrFlag == AcwrFlag.RED) {
            deductions.add(new Deduction(30, "Hohes ACWR – Verletzungsrisiko (ROT)",
                    0.0, 1.6, "acwr_flag", null));
            redFlag = true;
        } else if (acwrFlag == AcwrFlag.ORANGE) {
            deductions.add(new Deduction(18, "Erhöhtes ACWR – Belastung beachten (ORANGE)",
                    0.0, 1.3, "acwr_flag", null));
        }

        // ── 2. Zeitgewichtete Strain-Deduction (T-1, T-2, T-3) ────────────────
        Map<LocalDate, Double> dailyStrain = loadDailyStrainMap(user, date);
        WeightedResult strainWeighted = ReadinessDeductionCalculator.weightedSumFromMap(dailyStrain, date);
        Optional<Deduction> strainDed = ReadinessDeductionCalculator.strainDeduction(strainWeighted);
        strainDed.ifPresent(deductions::add);

        // Für CoachCard unten weiterhin der reine yesterday-Wert
        Double yesterdayStrain = dailyStrain.get(date.minusDays(1));

        // ── 3. Last eligible decoupling ───────────────────────────────────────
        List<ActivityMetrics> latestDecoupling = activityMetricsRepository
                .findEligibleDecouplingByUserId(user.getId(), PageRequest.of(0, 1));

        if (!latestDecoupling.isEmpty()) {
            Double decPct = latestDecoupling.get(0).getDecouplingPct();
            if (decPct != null && decPct > 10.0) {
                deductions.add(new Deduction(10, "Starkes Herzdriften zuletzt (>10%)",
                        decPct, 10.0, "decoupling_pct", null));
            } else if (decPct != null && decPct > 5.0) {
                deductions.add(new Deduction(5, "Leichtes Herzdriften zuletzt (>5%)",
                        decPct, 5.0, "decoupling_pct", null));
            }
        }

        // ── 4. Zeitgewichtete Z4+Z5-Deduction (T-1, T-2, T-3) ─────────────────
        Map<LocalDate, Double> z4z5PerDay = loadZ4Z5Map(user, date);
        WeightedResult z45Weighted = ReadinessDeductionCalculator.weightedSumFromMap(z4z5PerDay, date);
        Optional<Deduction> z45Ded = ReadinessDeductionCalculator.z45Deduction(z45Weighted);
        z45Ded.ifPresent(deductions::add);

        // Für CoachCard weiterhin die Summe der letzten 2 Tage (Coach-Narrativ kurzfristig)
        double z45Sum = activityMetricsRepository
                .sumZ4Z5MinByUserIdAndDateRange(user.getId(), date.minusDays(1), date);

        // ── Score clampen ─────────────────────────────────────────────────────
        int totalDeduction = deductions.stream().mapToInt(Deduction::amount).sum();
        int score = Math.max(0, Math.min(100, baseScore - totalDeduction));

        List<String> reasons = deductions.stream()
                .sorted(Comparator.comparingInt(Deduction::amount).reversed())
                .map(Deduction::reason)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        // ── Recommendation ────────────────────────────────────────────────────
        Recommendation recommendation;
        if (score < 30) {
            recommendation = Recommendation.REST;
        } else if (score < 50) {
            recommendation = Recommendation.EASY;
        } else if (score < 70) {
            recommendation = Recommendation.MODERATE;
        } else {
            recommendation = Recommendation.HARD;
        }

        // Cap at EASY when RED flag (score math already makes this unlikely,
        // but the spec requires it explicitly)
        if (redFlag && recommendation != Recommendation.REST) {
            recommendation = Recommendation.EASY;
        }

        // ── Serialize reasons ─────────────────────────────────────────────────
        List<String> topReasons = reasons.subList(0, Math.min(3, reasons.size()));
        String reasonsJson;
        try {
            reasonsJson = objectMapper.writeValueAsString(topReasons);
        } catch (JsonProcessingException e) {
            reasonsJson = "[]";
        }

        // ── Persist ───────────────────────────────────────────────────────────
        DailyMetrics daily = dailyMetricsRepository
                .findByUserIdAndDate(user.getId(), date)
                .orElse(new DailyMetrics());

        daily.setUser(user);
        daily.setDate(date);
        daily.setReadinessScore(score);
        daily.setRecommendation(recommendation);
        daily.setReasonsJson(reasonsJson);

        // ── Coach card ─────────────────────────────────────────────────────────
        Double lastDecouplingPct = latestDecoupling.isEmpty() ? null
                : latestDecoupling.get(0).getDecouplingPct();

        CoachCardService.CoachCard card = coachCardService.generate(
                recommendation, acwrFlag, score, yesterdayStrain, lastDecouplingPct, z45Sum);

        String coachBulletsJson;
        try {
            coachBulletsJson = objectMapper.writeValueAsString(card.bullets());
        } catch (JsonProcessingException e) {
            coachBulletsJson = "[]";
        }
        daily.setCoachTitle(card.title());
        daily.setCoachBulletsJson(coachBulletsJson);

        dailyMetricsRepository.save(daily);
    }

    /**
     * Recomputes readiness for the last 90 days for the given user.
     * Requires ACWR to already be populated for those days.
     */
    public void recomputeForUser(User user) {
        LocalDate today = LocalDate.now();
        for (int i = 89; i >= 0; i--) {
            compute(user, today.minusDays(i));
        }
    }

    /**
     * Lädt {@code dailyStrain21} aus daily_metrics für das Fenster [date-3, date]
     * und füllt fehlende Tage mit 0.0 auf, damit der Calculator eine stabile Map erhält.
     */
    private Map<LocalDate, Double> loadDailyStrainMap(User user, LocalDate date) {
        LocalDate from = date.minusDays(3);
        List<DailyMetrics> entries = dailyMetricsRepository
                .findByUserIdAndDateBetween(user.getId(), from, date);

        Map<LocalDate, Double> map = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(date); d = d.plusDays(1)) {
            map.put(d, 0.0);
        }
        for (DailyMetrics dm : entries) {
            if (dm.getDailyStrain21() != null && dm.getDate() != null) {
                map.put(dm.getDate(), dm.getDailyStrain21());
            }
        }
        return map;
    }

    /**
     * Lädt die summierten Z4+Z5-Minuten pro Tag für das Fenster [date-3, date].
     * Fehlende Tage werden mit 0.0 aufgefüllt.
     */
    private Map<LocalDate, Double> loadZ4Z5Map(User user, LocalDate date) {
        LocalDate from = date.minusDays(3);
        List<Object[]> rows = activityMetricsRepository
                .sumZ4Z5MinByUserIdAndDateRangeGrouped(user.getId(), from, date);

        Map<LocalDate, Double> map = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(date); d = d.plusDays(1)) {
            map.put(d, 0.0);
        }
        for (Object[] row : rows) {
            LocalDate d = (LocalDate) row[0];
            Double v = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            map.put(d, v);
        }
        return map;
    }
}
