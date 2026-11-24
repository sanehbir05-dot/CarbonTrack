package com.carbontrack.incentive;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Incentive maths (shared contract A7, rule 7). Pure functions, no database.
 *
 * <pre>
 * reductionPct = (baselineKg - currentKg) / baselineKg x 100
 * tier         = the highest rule whose minReductionPct is met, else NONE
 * </pre>
 *
 * <p>Safeguards that the contract does not spell out. Neither can divide by zero:
 * <ul>
 *   <li>No baseline emissions: the reduction is 0, so the tier is NONE.</li>
 *   <li>No emissions recorded in the reporting year: the reduction is 0, so the tier is NONE. Reading
 *       "nothing logged" as a 100% reduction would wrongly award the top tier.</li>
 * </ul>
 */
public final class IncentiveCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private IncentiveCalculator() {
    }

    /** {@code achieved} is null for tier NONE. {@code next} is null when already at the top tier. */
    public record Result(IncentiveRule achieved, IncentiveRule next) {
    }

    /** Exact reduction (scale 10) so tier decisions never depend on display rounding. Can be negative. */
    public static BigDecimal reductionPct(BigDecimal baselineKg, BigDecimal currentKg) {
        if (baselineKg.signum() <= 0 || currentKg.signum() <= 0) {
            return BigDecimal.ZERO.setScale(10);
        }
        return baselineKg.subtract(currentKg).multiply(HUNDRED).divide(baselineKg, 10, RoundingMode.HALF_UP);
    }

    /** @param rulesAscending rules sorted by minReductionPct, lowest first */
    public static Result evaluate(List<IncentiveRule> rulesAscending, BigDecimal reductionPct) {
        IncentiveRule achieved = null;
        IncentiveRule next = null;
        for (IncentiveRule rule : rulesAscending) {
            if (reductionPct.compareTo(rule.getMinReductionPct()) >= 0) {
                achieved = rule;
            } else if (next == null) {
                next = rule;
            }
        }
        return new Result(achieved, next);
    }

    /** minReductionPct - reductionPct, never below 0. */
    public static BigDecimal gapPct(IncentiveRule next, BigDecimal reductionPct) {
        return next.getMinReductionPct().subtract(reductionPct).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
