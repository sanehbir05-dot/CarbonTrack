package com.carbontrack.target;

import com.carbontrack.common.TargetStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Target maths (shared contract A7, rule 6). Pure functions, no database.
 *
 * <pre>
 * targetKg    = baselineKg x (1 - reductionPct / 100)
 * achievedPct = (baselineKg - currentKg) / (baselineKg - targetKg) x 100, clamped to 0..100
 * ACHIEVED    if currentKg <= targetKg
 * ON_TRACK    if the actual reduction is at least the straight-line expected reduction for the year
 * AT_RISK     otherwise
 * </pre>
 *
 * <p>Straight line: the expected reduction grows evenly from 0 in the baseline year to the full
 * reduction in the target year (clamped outside that range).
 *
 * <p>Two safeguards that the contract does not spell out. Neither can divide by zero:
 * <ul>
 *   <li>No baseline emissions: everything computed from the baseline is zero and the status is AT_RISK.</li>
 *   <li>No emissions recorded in the reporting year: it is reported as AT_RISK with 0% achieved. Reading
 *       "nothing logged" as a 100% reduction would wrongly show the target as achieved.</li>
 * </ul>
 */
public final class TargetCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_KG = BigDecimal.ZERO.setScale(3);
    private static final BigDecimal ZERO_PCT = BigDecimal.ZERO.setScale(1);

    private TargetCalculator() {
    }

    public record Progress(BigDecimal baselineKg, BigDecimal targetKg, BigDecimal currentKg,
                           BigDecimal achievedPct, TargetStatus status) {
    }

    public static Progress evaluate(int baselineYear, int targetYear, BigDecimal reductionPct,
                                    BigDecimal baselineKg, BigDecimal currentKg, int currentYear) {
        if (baselineKg.signum() <= 0) {
            return new Progress(ZERO_KG, ZERO_KG, currentKg, ZERO_PCT, TargetStatus.AT_RISK);
        }
        BigDecimal targetKg = baselineKg.multiply(HUNDRED.subtract(reductionPct))
                .divide(HUNDRED, 3, RoundingMode.HALF_UP);
        if (currentKg.signum() <= 0) {
            return new Progress(baselineKg, targetKg, currentKg, ZERO_PCT, TargetStatus.AT_RISK);
        }

        BigDecimal neededReduction = baselineKg.subtract(targetKg);
        BigDecimal actualReduction = baselineKg.subtract(currentKg);

        BigDecimal achievedPct;
        if (neededReduction.signum() <= 0) {
            achievedPct = currentKg.compareTo(targetKg) <= 0 ? HUNDRED.setScale(1) : ZERO_PCT;
        } else {
            achievedPct = actualReduction.multiply(HUNDRED)
                    .divide(neededReduction, 1, RoundingMode.HALF_UP)
                    .max(ZERO_PCT).min(HUNDRED.setScale(1));
        }

        TargetStatus status;
        if (currentKg.compareTo(targetKg) <= 0) {
            status = TargetStatus.ACHIEVED;
        } else {
            BigDecimal expected = neededReduction.multiply(timeFraction(baselineYear, targetYear, currentYear));
            status = actualReduction.compareTo(expected) >= 0 ? TargetStatus.ON_TRACK : TargetStatus.AT_RISK;
        }
        return new Progress(baselineKg, targetKg, currentKg, achievedPct, status);
    }

    /** Share of the baseline-to-target period that has passed, clamped to 0..1. */
    static BigDecimal timeFraction(int baselineYear, int targetYear, int currentYear) {
        int span = targetYear - baselineYear;
        if (span <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal f = BigDecimal.valueOf(currentYear - baselineYear)
                .divide(BigDecimal.valueOf(span), 10, RoundingMode.HALF_UP);
        return f.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }
}
