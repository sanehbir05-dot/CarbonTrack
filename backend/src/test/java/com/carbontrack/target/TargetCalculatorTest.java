package com.carbontrack.target;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.carbontrack.common.TargetStatus;
import com.carbontrack.target.TargetCalculator.Progress;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TargetCalculatorTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    /** Baseline 2023 = 1000 kg, 20% target by 2027 (target 800 kg), reporting year 2025. */
    private static Progress demo(String currentKg, int targetYear, int currentYear) {
        return TargetCalculator.evaluate(2023, targetYear, d("20"), d("1000.000"), d(currentKg), currentYear);
    }

    @Test
    void computesTargetKgAndAchievedPct() {
        Progress p = demo("880.000", 2027, 2025);

        assertEquals(d("800.000"), p.targetKg());
        assertEquals(d("1000.000"), p.baselineKg());
        assertEquals(d("880.000"), p.currentKg());
        assertEquals(d("60.0"), p.achievedPct());     // 120 of the 200 kg needed
    }

    @Test
    void onTrackWhenReductionIsAheadOfStraightLine() {
        // 2025 is halfway from 2023 to 2027, so 100 kg is expected. 120 kg achieved.
        assertEquals(TargetStatus.ON_TRACK, demo("880.000", 2027, 2025).status());
    }

    @Test
    void onTrackWhenExactlyOnTheStraightLine() {
        assertEquals(TargetStatus.ON_TRACK, demo("900.000", 2027, 2025).status());
    }

    @Test
    void atRiskWhenBehindTheStraightLine() {
        assertEquals(TargetStatus.AT_RISK, demo("901.000", 2027, 2025).status());
    }

    @Test
    void atRiskWhenTargetYearIsSooner() {
        // 20% by 2026: two thirds of 200 kg = 133.3 kg expected by 2025, only 120 achieved.
        assertEquals(TargetStatus.AT_RISK, demo("880.000", 2026, 2025).status());
    }

    @Test
    void achievedWhenAtOrBelowTarget() {
        assertEquals(TargetStatus.ACHIEVED, demo("800.000", 2027, 2025).status());
        assertEquals(TargetStatus.ACHIEVED, demo("700.000", 2027, 2025).status());
    }

    @Test
    void achievedPctIsClampedTo100() {
        assertEquals(d("100.0"), demo("700.000", 2027, 2025).achievedPct());
        assertEquals(d("100.0"), demo("800.000", 2027, 2025).achievedPct());
    }

    @Test
    void achievedPctIsClampedToZeroWhenEmissionsRose() {
        Progress p = demo("1100.000", 2027, 2025);

        assertEquals(d("0.0"), p.achievedPct());
        assertEquals(TargetStatus.AT_RISK, p.status());
    }

    @Test
    void afterTheTargetYearOnlyMeetingTheTargetCounts() {
        // Past 2027 the full reduction is expected, so 850 kg (15%) is no longer on track.
        assertEquals(TargetStatus.AT_RISK, demo("850.000", 2027, 2028).status());
        assertEquals(TargetStatus.ACHIEVED, demo("790.000", 2027, 2028).status());
    }

    @Test
    void beforeTheBaselineYearNothingIsExpectedYet() {
        Progress ahead = TargetCalculator.evaluate(2025, 2030, d("20"), d("1000.000"), d("950.000"), 2024);
        Progress rose = TargetCalculator.evaluate(2025, 2030, d("20"), d("1000.000"), d("1100.000"), 2024);

        assertEquals(TargetStatus.ON_TRACK, ahead.status());
        assertEquals(TargetStatus.AT_RISK, rose.status());
    }

    @Test
    void zeroBaselineReturnsZerosAndAtRiskWithoutDividing() {
        Progress p = TargetCalculator.evaluate(2023, 2027, d("20"), BigDecimal.ZERO, d("500.000"), 2025);

        assertEquals(0, p.baselineKg().signum());
        assertEquals(0, p.targetKg().signum());
        assertEquals(d("0.0"), p.achievedPct());
        assertEquals(TargetStatus.AT_RISK, p.status());
    }

    @Test
    void noEmissionsInReportingYearIsAtRiskNotAchieved() {
        Progress p = demo("0.000", 2027, 2025);

        assertEquals(TargetStatus.AT_RISK, p.status());
        assertEquals(d("0.0"), p.achievedPct());
        assertEquals(d("800.000"), p.targetKg());
    }

    @Test
    void tinyBaselineWhereRoundingLeavesNothingToReduceDoesNotDivideByZero() {
        // 1% of 0.001 kg rounds to 0 kg needed.
        Progress p = TargetCalculator.evaluate(2023, 2027, d("1"), d("0.001"), d("0.001"), 2025);

        assertEquals(d("100.0"), p.achievedPct());          // nothing left to reduce and current <= target
        assertEquals(TargetStatus.ACHIEVED, p.status());
    }

    @Test
    void timeFractionIsClamped() {
        assertEquals(0, TargetCalculator.timeFraction(2023, 2027, 2020).signum());
        assertEquals(0, d("0.5").compareTo(TargetCalculator.timeFraction(2023, 2027, 2025)));
        assertEquals(0, BigDecimal.ONE.compareTo(TargetCalculator.timeFraction(2023, 2027, 2035)));
    }
}
