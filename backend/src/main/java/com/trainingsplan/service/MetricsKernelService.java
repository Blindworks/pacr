package com.trainingsplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.BodyMetric;
import com.trainingsplan.entity.MetricType;
import com.trainingsplan.entity.ActivityMetrics;
import com.trainingsplan.entity.Recommendation;
import com.trainingsplan.entity.SleepData;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.BodyMetricRepository;
import com.trainingsplan.repository.SleepDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Zentraler Metriken-Rechenkern.
 *
 * <p>Berechnet für einen User und ein Datum alle aggregierten Metriken sequenziell
 * in 5 Phasen und schreibt die Ergebnisse als tagesbezogene Einträge in {@code body_metrics}
 * (sourceActivityId = null).
 *
 * <p>Wird parallel zu den bestehenden DailyMetricsService-Schreibpfaden betrieben
 * (Parallelbetrieb-Phase). Die legacy {@code daily_metrics}-Tabelle bleibt vorerst
 * erhalten.
 *
 * <h3>Berechnungsreihenfolge</h3>
 * <ol>
 *   <li>Phase 1: DAILY_STRAIN21, DAILY_TRIMP (aus activity_metrics)</li>
 *   <li>Phase 2: EF7, EF28 (Rolling-EF aus activity_metrics)</li>
 *   <li>Phase 3: ACUTE7, CHRONIC28, ACWR, ACWR_FLAG (aus body_metrics DAILY_STRAIN21)</li>
 *   <li>Phase 4: READINESS_SCORE, RECOMMENDATION (aus ACWR_FLAG + activity_metrics)</li>
 *   <li>Phase 5: COACH_CARD (Titel + Bullets)</li>
 * </ol>
 */
@Service
public class MetricsKernelService {

    private static final Logger log = LoggerFactory.getLogger(MetricsKernelService.class);

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    @Autowired
    private BodyMetricRepository bodyMetricRepository;

    @Autowired
    private SleepDataRepository sleepDataRepository;

    @Autowired
    private CoachCardService coachCardService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Öffentliche API ───────────────────────────────────────────────────

    /**
     * Berechnet alle Metriken für {@code user} am {@code date} und persistiert sie in body_metrics.
     */
    public void computeForDate(User user, LocalDate date) {
        log.info("metrics_kernel date={} userId={} trigger=computeForDate", date, user.getId());
        computeDailyStrain(user, date);
        computeRollingEf(user, date);
        computeAcwr(user, date);
        computeReadiness(user, date);
        computeCoachCard(user, date);
        log.debug("metrics_kernel date={} userId={} status=done", date, user.getId());
    }

    /**
     * Backfill: Berechnet alle Metriken für jeden Tag im Bereich [{@code from}, {@code to}]
     * chronologisch (ältestes Datum zuerst, damit Fenster-Aggregationen korrekte Vortageswerte lesen).
     */
    public void computeForDateRange(User user, LocalDate from, LocalDate to) {
        log.info("metrics_kernel_backfill userId={} from={} to={}", user.getId(), from, to);
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            computeForDate(user, d);
        }
        log.info("metrics_kernel_backfill userId={} status=done", user.getId());
    }

    // ── Phase 1: Tagesbelastung ───────────────────────────────────────────

    private void computeDailyStrain(User user, LocalDate date) {
        Double strain = activityMetricsRepository.sumStrain21ByUserIdAndDate(user.getId(), date);
        if (strain == null) strain = 0.0;

        Double trimp = activityMetricsRepository.sumTrimpByUserIdAndDate(user.getId(), date);
        if (trimp == null) trimp = 0.0;

        log.debug("metrics_kernel phase=1_strain date={} userId={} strain21={} trimp={}", date, user.getId(), strain, trimp);
        upsertDaily(user, MetricType.DAILY_STRAIN21, strain, "au", date, null, null, null);
        upsertDaily(user, MetricType.DAILY_TRIMP, trimp, "au", date, null, null, null);
    }

    // ── Phase 2: Rolling Efficiency Factor ───────────────────────────────

    private void computeRollingEf(User user, LocalDate date) {
        List<ActivityMetrics> window28 = activityMetricsRepository
                .findWithEfByUserIdAndDateRange(user.getId(), date.minusDays(27), date);

        Double ef7  = averageEfInWindow(window28, date.minusDays(6), date);
        Double ef28 = averageEfInWindow(window28, date.minusDays(27), date);

        log.debug("metrics_kernel phase=2_ef date={} userId={} ef7={} ef28={}", date, user.getId(), ef7, ef28);
        if (ef7  != null) upsertDaily(user, MetricType.EF7,  ef7,  "m/s/bpm", date, null, null, null);
        if (ef28 != null) upsertDaily(user, MetricType.EF28, ef28, "m/s/bpm", date, null, null, null);
    }

    private Double averageEfInWindow(List<ActivityMetrics> candidates,
                                     LocalDate windowStart, LocalDate windowEnd) {
        double sum = 0.0;
        int count = 0;
        for (ActivityMetrics am : candidates) {
            LocalDate actDate = am.getCompletedTraining().getTrainingDate();
            if (!actDate.isBefore(windowStart) && !actDate.isAfter(windowEnd)) {
                sum += am.getEfficiencyFactor();
                count++;
            }
        }
        return count > 0 ? sum / count : null;
    }

    // ── Phase 3: ACWR ─────────────────────────────────────────────────────

    private void computeAcwr(User user, LocalDate date) {
        List<BodyMetric> window28 = bodyMetricRepository
                .findByUserIdAndMetricTypeAndRecordedAtBetween(
                        user.getId(), MetricType.DAILY_STRAIN21, date.minusDays(27), date);

        double acute7    = sumBodyMetricValues(window28, date.minusDays(6), date);
        double sum28     = sumBodyMetricValues(window28, date.minusDays(27), date);
        double chronic28 = sum28 / 4.0;

        Double acwr    = chronic28 > 0.0 ? acute7 / chronic28 : null;
        String flag    = computeAcwrFlag(acwr);
        String message = computeAcwrMessage(flag);

        log.debug("metrics_kernel phase=3_acwr date={} userId={} acute7={} chronic28={} acwr={} flag={}",
                date, user.getId(), acute7, chronic28, acwr, flag);
        upsertDaily(user, MetricType.ACUTE7,    acute7,    "au",    date, null, null, null);
        upsertDaily(user, MetricType.CHRONIC28,  chronic28, "au",    date, null, null, null);
        upsertDaily(user, MetricType.ACWR,       acwr != null ? acwr : 0.0, "ratio", date, null, null, null);
        upsertDaily(user, MetricType.ACWR_FLAG,  0.0, "", date, flag, null, message);
    }

    private double sumBodyMetricValues(List<BodyMetric> metrics, LocalDate from, LocalDate to) {
        double sum = 0.0;
        for (BodyMetric bm : metrics) {
            if (!bm.getRecordedAt().isBefore(from) && !bm.getRecordedAt().isAfter(to)
                    && bm.getValue() != null) {
                sum += bm.getValue();
            }
        }
        return sum;
    }

    private String computeAcwrFlag(Double acwr) {
        if (acwr == null)  return null;
        if (acwr < 0.8)    return AcwrFlag.BLUE.name();
        if (acwr <= 1.3)   return AcwrFlag.GREEN.name();
        if (acwr <= 1.6)   return AcwrFlag.ORANGE.name();
        return AcwrFlag.RED.name();
    }

    private String computeAcwrMessage(String flag) {
        if (flag == null) return null;
        return switch (flag) {
            case "BLUE"   -> "Unterbelastung – Training steigern";
            case "GREEN"  -> "Optimale Belastung";
            case "ORANGE" -> "Erhöhte Belastung – Verletzungsrisiko beachten";
            case "RED"    -> "Hohes Verletzungsrisiko – Belastung reduzieren";
            default       -> null;
        };
    }

    // ── Phase 4: Readiness ────────────────────────────────────────────────

    private record Deduction(int amount, String reason) {}

    private void computeReadiness(User user, LocalDate date) {
        List<Deduction> deductions = new ArrayList<>();
        boolean redFlag = false;

        // 1. ACWR flag
        String acwrFlagStr = bodyMetricRepository
                .findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNull(
                        user.getId(), MetricType.ACWR_FLAG, date)
                .map(BodyMetric::getStringValue)
                .orElse(null);

        if ("RED".equals(acwrFlagStr)) {
            deductions.add(new Deduction(30, "Hohes ACWR – Verletzungsrisiko (ROT)"));
            redFlag = true;
        } else if ("ORANGE".equals(acwrFlagStr)) {
            deductions.add(new Deduction(18, "Erhöhtes ACWR – Belastung beachten (ORANGE)"));
        }

        // 2. Yesterday strain
        Double yesterdayStrain = bodyMetricRepository
                .findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNull(
                        user.getId(), MetricType.DAILY_STRAIN21, date.minusDays(1))
                .map(BodyMetric::getValue)
                .orElse(null);

        if (yesterdayStrain != null && yesterdayStrain > 14.0) {
            deductions.add(new Deduction(12, "Hohe gestrige Belastung (strain21 > 14)"));
        }

        // 3. Last eligible decoupling
        List<ActivityMetrics> latestDecoupling = activityMetricsRepository
                .findEligibleDecouplingByUserId(user.getId(), PageRequest.of(0, 1));
        Double lastDecouplingPct = latestDecoupling.isEmpty() ? null
                : latestDecoupling.get(0).getDecouplingPct();

        if (lastDecouplingPct != null && lastDecouplingPct > 10.0) {
            deductions.add(new Deduction(10, "Starkes Herzdriften zuletzt (>10%)"));
        } else if (lastDecouplingPct != null && lastDecouplingPct > 5.0) {
            deductions.add(new Deduction(5, "Leichtes Herzdriften zuletzt (>5%)"));
        }

        // 4. Z4+Z5 minutes (last 2 days)
        double z45Sum = activityMetricsRepository
                .sumZ4Z5MinByUserIdAndDateRange(user.getId(), date.minusDays(1), date);

        if (z45Sum > 20.0) {
            deductions.add(new Deduction(8, "Viele Hochintensivminuten (>20 min Z4/Z5)"));
        }

        // 5. Sleep/HRV (nur wenn SleepData für Vortag vorhanden)
        SleepData lastNight = sleepDataRepository
                .findByUserIdAndRecordedAt(user.getId(), date.minusDays(1))
                .orElse(null);

        if (lastNight != null) {
            Integer sleepScore = lastNight.getSleepScore();
            if (sleepScore != null) {
                if (sleepScore < 40) {
                    deductions.add(new Deduction(25, "Sehr schlechter Schlaf (Score < 40)"));
                } else if (sleepScore < 60) {
                    deductions.add(new Deduction(15, "Schlechter Schlaf (Score 40–59)"));
                } else if (sleepScore < 75) {
                    deductions.add(new Deduction(8, "Mäßiger Schlaf (Score 60–74)"));
                }
            }

            String hrvStatus = lastNight.getHrvStatus();
            if (hrvStatus != null) {
                String hrv = hrvStatus.toLowerCase();
                if (hrv.equals("poor") || hrv.equals("unbalanced")) {
                    deductions.add(new Deduction(20, "HRV-Status beeinträchtigt"));
                } else if (hrv.equals("low") || hrv.equals("below_normal")) {
                    deductions.add(new Deduction(10, "HRV unter Basiswert"));
                }
            }

            Integer bodyBattery = lastNight.getBodyBattery();
            if (bodyBattery != null) {
                if (bodyBattery < 25) {
                    deductions.add(new Deduction(20, "Sehr niedrige Body Battery (< 25)"));
                } else if (bodyBattery < 50) {
                    deductions.add(new Deduction(10, "Niedrige Body Battery (25–49)"));
                }
                // Body Battery als eigene BodyMetric speichern
                upsertDaily(user, MetricType.BODY_BATTERY, (double) bodyBattery, "%", date, null, null, null);
            }
        }

        // Gesamtabzug berechnen und auf [0,100] clampen
        int totalDeduction = deductions.stream().mapToInt(Deduction::amount).sum();
        int score = Math.max(0, Math.min(100, 100 - totalDeduction));

        // Top-3 Gründe nach Abzugsbetrag absteigend
        List<String> topReasons = deductions.stream()
                .sorted(Comparator.comparingInt(Deduction::amount).reversed())
                .limit(3)
                .map(Deduction::reason)
                .toList();

        // Recommendation
        String recommendation;
        if (score < 30)      recommendation = Recommendation.REST.name();
        else if (score < 50) recommendation = Recommendation.EASY.name();
        else if (score < 70) recommendation = Recommendation.MODERATE.name();
        else                 recommendation = Recommendation.HARD.name();

        if (redFlag && !Recommendation.REST.name().equals(recommendation)) {
            recommendation = Recommendation.EASY.name();
        }

        String reasonsJson;
        try {
            reasonsJson = objectMapper.writeValueAsString(topReasons);
        } catch (JsonProcessingException e) {
            reasonsJson = "[]";
        }

        log.debug("metrics_kernel phase=4_readiness date={} userId={} score={} recommendation={} sleepData={}",
                date, user.getId(), score, recommendation, lastNight != null);
        upsertDaily(user, MetricType.READINESS_SCORE, (double) score, "score", date, null, reasonsJson, null);
        upsertDaily(user, MetricType.RECOMMENDATION,  0.0, "", date, recommendation, null, null);
    }

    // ── Phase 5: Coach Card ───────────────────────────────────────────────

    private void computeCoachCard(User user, LocalDate date) {
        // Inputs aus body_metrics lesen
        String recommendationStr = bodyMetricRepository
                .findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNull(
                        user.getId(), MetricType.RECOMMENDATION, date)
                .map(BodyMetric::getStringValue)
                .orElse(null);

        String acwrFlagStr = bodyMetricRepository
                .findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNull(
                        user.getId(), MetricType.ACWR_FLAG, date)
                .map(BodyMetric::getStringValue)
                .orElse(null);

        int readinessScore = bodyMetricRepository
                .findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNull(
                        user.getId(), MetricType.READINESS_SCORE, date)
                .map(bm -> bm.getValue().intValue())
                .orElse(80);

        Double yesterdayStrain = bodyMetricRepository
                .findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNull(
                        user.getId(), MetricType.DAILY_STRAIN21, date.minusDays(1))
                .map(BodyMetric::getValue)
                .orElse(null);

        List<ActivityMetrics> latestDecoupling = activityMetricsRepository
                .findEligibleDecouplingByUserId(user.getId(), PageRequest.of(0, 1));
        Double lastDecouplingPct = latestDecoupling.isEmpty() ? null
                : latestDecoupling.get(0).getDecouplingPct();

        double z45Sum = activityMetricsRepository
                .sumZ4Z5MinByUserIdAndDateRange(user.getId(), date.minusDays(1), date);

        Recommendation recommendation = recommendationStr != null
                ? Recommendation.valueOf(recommendationStr) : null;
        AcwrFlag acwrFlag = acwrFlagStr != null ? AcwrFlag.valueOf(acwrFlagStr) : null;

        CoachCardService.CoachCard card = coachCardService.generate(
                recommendation, acwrFlag, readinessScore, yesterdayStrain, lastDecouplingPct, z45Sum);

        String bulletsJson;
        try {
            bulletsJson = objectMapper.writeValueAsString(card.bullets());
        } catch (JsonProcessingException e) {
            bulletsJson = "[]";
        }

        log.debug("metrics_kernel phase=5_coach date={} userId={} title=\"{}\"", date, user.getId(), card.title());
        upsertDaily(user, MetricType.COACH_CARD, 0.0, "", date, null, bulletsJson, card.title());
    }

    // ── Upsert-Hilfsmethode ───────────────────────────────────────────────

    /**
     * Upsert eines tagesbezogenen Metrik-Eintrags (sourceActivityId = null).
     * Findet einen bestehenden Eintrag per userId + metricType + recordedAt + sourceActivityId IS NULL
     * und aktualisiert ihn; andernfalls wird ein neuer Eintrag angelegt.
     */
    private void upsertDaily(User user, String metricType, Double value, String unit,
                             LocalDate recordedAt, String stringValue,
                             String reasonsJson, String auxiliaryText) {
        BodyMetric metric = bodyMetricRepository
                .findByUserIdAndMetricTypeAndRecordedAtAndSourceActivityIdIsNull(
                        user.getId(), metricType, recordedAt)
                .orElse(new BodyMetric());

        metric.setUser(user);
        metric.setMetricType(metricType);
        metric.setValue(value);
        metric.setUnit(unit);
        metric.setRecordedAt(recordedAt);
        metric.setSourceActivityId(null);
        metric.setStringValue(stringValue);
        metric.setReasonsJson(reasonsJson);
        metric.setAuxiliaryText(auxiliaryText);

        bodyMetricRepository.save(metric);
    }
}
