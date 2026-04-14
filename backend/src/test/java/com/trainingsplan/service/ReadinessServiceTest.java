package com.trainingsplan.service;

import com.trainingsplan.entity.AcwrFlag;
import com.trainingsplan.entity.DailyMetrics;
import com.trainingsplan.entity.Recommendation;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.ActivityMetricsRepository;
import com.trainingsplan.repository.DailyMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regression tests for the time-weighted readiness logic in {@link ReadinessService}.
 *
 * <p>Validates that a hard training session several days in the past still
 * lowers today's readiness score — specifically the bug scenario where a hard
 * Saturday session was not reflected in Tuesday's score.
 */
class ReadinessServiceTest {

    private DailyMetricsRepository dailyMetricsRepository;
    private ActivityMetricsRepository activityMetricsRepository;
    private CoachCardService coachCardService;
    private ReadinessService service;

    private static final LocalDate TUESDAY = LocalDate.of(2026, 4, 14);
    private User user;

    @BeforeEach
    void setUp() {
        dailyMetricsRepository = mock(DailyMetricsRepository.class);
        activityMetricsRepository = mock(ActivityMetricsRepository.class);
        coachCardService = mock(CoachCardService.class);

        service = new ReadinessService();
        injectField(service, "dailyMetricsRepository", dailyMetricsRepository);
        injectField(service, "activityMetricsRepository", activityMetricsRepository);
        injectField(service, "coachCardService", coachCardService);

        user = new User();

        // Defaults: no ACWR flag, no activities
        when(dailyMetricsRepository.findByUserIdAndDate(any(), any()))
                .thenReturn(Optional.empty());
        when(dailyMetricsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityMetricsRepository.findEligibleDecouplingByUserId(any(), any()))
                .thenReturn(List.of());
        when(activityMetricsRepository.sumZ4Z5MinByUserIdAndDateRange(any(), any(), any()))
                .thenReturn(0.0);
        when(activityMetricsRepository.sumZ4Z5MinByUserIdAndDateRangeGrouped(any(), any(), any()))
                .thenReturn(List.of());
        when(coachCardService.generate(any(), any(), anyInt(), any(), any(), anyDouble()))
                .thenReturn(new CoachCardService.CoachCard("title", List.of()));
    }

    // ── Base case: no load → score = 100 ─────────────────────────────────────

    @Test
    void noActivity_scoreIs100() {
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        service.compute(user, TUESDAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(100, saved.getReadinessScore());
        assertEquals(Recommendation.HARD, saved.getRecommendation());
    }

    // ── Core bug scenario: hard Saturday session → Tuesday score must drop ──

    @Test
    void hardSaturdaySession_threeDaysAgo_stillDropsScore() {
        // Samstag (T-3) war hart: strain21 = 18. Sonntag und Montag waren Erholungstage.
        // Gewichtung: T-3 × 0.30 = 5.4 → allein zu wenig.
        // Realistisch: wenn User regelmäßig trainiert, hat er auch T-1 und T-2 etwas Strain.
        // Wir simulieren: T-1=6, T-2=5, T-3=18 → 6*0.80 + 5*0.55 + 18*0.30 = 4.8+2.75+5.4 = 12.95
        List<DailyMetrics> strainWindow = new ArrayList<>();
        strainWindow.add(dailyMetricsOn(TUESDAY.minusDays(3), 18.0));
        strainWindow.add(dailyMetricsOn(TUESDAY.minusDays(2), 5.0));
        strainWindow.add(dailyMetricsOn(TUESDAY.minusDays(1), 6.0));
        strainWindow.add(dailyMetricsOn(TUESDAY, 0.0));
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(strainWindow);

        service.compute(user, TUESDAY);

        DailyMetrics saved = captureLastSave();
        // Erwartung: moderate Strain-Deduction (−6) wird angewandt
        assertEquals(94, saved.getReadinessScore(),
                "Hard Saturday session should still lower Tuesday's score below 100");
        assertTrue(saved.getReasonsJson().contains("akute Belastung"),
                "Reasons should mention acute load; got: " + saved.getReasonsJson());
    }

    @Test
    void veryHardRecentLoad_triggersHighStrainDeduction() {
        // T-3=20, T-2=12, T-1=8 → 8*0.80 + 12*0.55 + 20*0.30 = 6.4+6.6+6.0 = 19.0 → "high" (−12)
        List<DailyMetrics> strainWindow = new ArrayList<>();
        strainWindow.add(dailyMetricsOn(TUESDAY.minusDays(3), 20.0));
        strainWindow.add(dailyMetricsOn(TUESDAY.minusDays(2), 12.0));
        strainWindow.add(dailyMetricsOn(TUESDAY.minusDays(1), 8.0));
        strainWindow.add(dailyMetricsOn(TUESDAY, 0.0));
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(strainWindow);

        service.compute(user, TUESDAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(88, saved.getReadinessScore());
    }

    @Test
    void redAcwrFlag_capsRecommendationAtEasy() {
        DailyMetrics todayMetrics = new DailyMetrics();
        todayMetrics.setDate(TUESDAY);
        todayMetrics.setAcwrFlag(AcwrFlag.RED);

        when(dailyMetricsRepository.findByUserIdAndDate(user.getId(), TUESDAY))
                .thenReturn(Optional.of(todayMetrics));
        when(dailyMetricsRepository.findByUserIdAndDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        service.compute(user, TUESDAY);

        DailyMetrics saved = captureLastSave();
        assertEquals(70, saved.getReadinessScore(), "100 − 30 (RED flag) = 70");
        assertEquals(Recommendation.EASY, saved.getRecommendation(),
                "RED flag must cap recommendation at EASY even with score ≥ 70");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private DailyMetrics dailyMetricsOn(LocalDate date, double strain) {
        DailyMetrics dm = new DailyMetrics();
        dm.setDate(date);
        dm.setDailyStrain21(strain);
        return dm;
    }

    private DailyMetrics captureLastSave() {
        ArgumentCaptor<DailyMetrics> captor = ArgumentCaptor.forClass(DailyMetrics.class);
        verify(dailyMetricsRepository, atLeastOnce()).save(captor.capture());
        List<DailyMetrics> all = captor.getAllValues();
        return all.get(all.size() - 1);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject field: " + fieldName, e);
        }
    }
}
