package com.trainingsplan.service;

import com.trainingsplan.entity.UserVo2MaxState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests the pure computeNextState() function. FIT parsing, persistence and
 * eligibility filtering are covered by integration tests.
 */
class Vo2MaxAggregationServiceTest {

    private Vo2MaxAggregationService service;

    @BeforeEach
    void setUp() {
        service = new Vo2MaxAggregationService();
        ReflectionTestUtils.setField(service, "tauDays", 18.0);
        ReflectionTestUtils.setField(service, "outlierClipThreshold", 3.0);
        ReflectionTestUtils.setField(service, "hysteresisThreshold", 0.5);
        ReflectionTestUtils.setField(service, "coldStartCount", 3);
        ReflectionTestUtils.setField(service, "minDurationMinutes", 15.0);
        ReflectionTestUtils.setField(service, "minHrCoveragePercent", 80.0);
        ReflectionTestUtils.setField(service, "minDeltaDays", 0.5);
    }

    @Test
    void firstWorkout_setsStateDirectly() {
        UserVo2MaxState s = service.computeNextState(null, 55.0, ts(0));

        assertThat(s.getVo2maxInternal()).isEqualTo(55.0);
        assertThat(s.getVo2maxDisplayed()).isEqualTo(55);
        assertThat(s.getEligibleWorkoutCount()).isEqualTo(1);
    }

    @Test
    void coldStart_usesIncrementalMean() {
        UserVo2MaxState s1 = service.computeNextState(null, 55.0, ts(0));
        UserVo2MaxState s2 = service.computeNextState(s1, 57.0, ts(1));
        UserVo2MaxState s3 = service.computeNextState(s2, 53.0, ts(2));

        assertThat(s2.getVo2maxInternal()).isEqualTo(56.0);
        assertThat(s3.getVo2maxInternal()).isEqualTo(55.0);
        assertThat(s3.getEligibleWorkoutCount()).isEqualTo(3);
        assertThat(s3.getVo2maxDisplayed()).isEqualTo(55);
    }

    @Test
    void afterColdStart_outlierIsClippedTo3() {
        UserVo2MaxState prev = stateAt(55.0, 55, 3, ts(10));

        // Δt = 1 day, α ≈ 0.0540
        UserVo2MaxState s = service.computeNextState(prev, 65.0, ts(11));
        double alpha = 1.0 - Math.exp(-1.0 / 18.0);
        double expected = alpha * 58.0 + (1 - alpha) * 55.0;

        assertThat(s.getVo2maxInternal()).isCloseTo(expected, within(1e-9));
        assertThat(s.getEligibleWorkoutCount()).isEqualTo(4);
    }

    @Test
    void afterColdStart_normalDeltaUsesRawValue() {
        UserVo2MaxState prev = stateAt(55.0, 55, 3, ts(10));
        UserVo2MaxState s = service.computeNextState(prev, 57.0, ts(11));

        double alpha = 1.0 - Math.exp(-1.0 / 18.0);
        double expected = alpha * 57.0 + (1 - alpha) * 55.0;

        assertThat(s.getVo2maxInternal()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void ewma_largeGapGivesHigherWeight() {
        UserVo2MaxState prev = stateAt(55.0, 55, 3, ts(0));
        UserVo2MaxState s = service.computeNextState(prev, 58.0, ts(30));

        double alpha = 1.0 - Math.exp(-30.0 / 18.0);
        double expected = alpha * 58.0 + (1 - alpha) * 55.0;

        assertThat(alpha).isCloseTo(0.8111, within(1e-3));
        assertThat(s.getVo2maxInternal()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void hysteresis_displayStaysWhenInternalDriftsLessThanHalf() {
        UserVo2MaxState prev = stateAt(55.0, 55, 5, ts(0));
        // Tiny change → delta < 0.5 and candidate still 55
        UserVo2MaxState s = service.computeNextState(prev, 55.4, ts(1));

        assertThat(s.getVo2maxDisplayed()).isEqualTo(55);
    }

    @Test
    void hysteresis_displayJumpsOnceThresholdIsCrossed() {
        UserVo2MaxState prev = stateAt(55.6, 55, 5, ts(0));
        // Push internal up to ≥ 55.5 (candidate=56, |Δ to displayed 55| ≥ 0.5)
        UserVo2MaxState s = service.computeNextState(prev, 60.0, ts(1));

        assertThat(s.getVo2maxInternal()).isGreaterThan(55.5);
        assertThat(s.getVo2maxDisplayed()).isEqualTo(56);
    }

    @Test
    void minDeltaDays_preventsZeroAlphaOnSameDay() {
        UserVo2MaxState prev = stateAt(55.0, 55, 5, ts(0));
        // Same timestamp — Δt clamped to 0.5, alpha ≈ 0.0274
        UserVo2MaxState s = service.computeNextState(prev, 58.0, ts(0));

        double alpha = 1.0 - Math.exp(-0.5 / 18.0);
        double expected = alpha * 58.0 + (1 - alpha) * 55.0;

        assertThat(s.getVo2maxInternal()).isCloseTo(expected, within(1e-9));
    }

    // ---------- helpers ----------

    private static LocalDateTime ts(int dayOffset) {
        return LocalDateTime.of(2026, 4, 1, 12, 0).plusDays(dayOffset);
    }

    private static UserVo2MaxState stateAt(double internal, int displayed, int count, LocalDateTime at) {
        UserVo2MaxState s = new UserVo2MaxState();
        s.setVo2maxInternal(internal);
        s.setVo2maxDisplayed(displayed);
        s.setEligibleWorkoutCount(count);
        s.setLastUpdateAt(at);
        return s;
    }
}
