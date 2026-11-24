package com.carbontrack.calculation;

import com.carbontrack.common.BadRequestException;
import com.carbontrack.common.Category;
import com.carbontrack.factor.EmissionFactor;
import com.carbontrack.factor.EmissionFactorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * co2eKg = quantity x factor value (shared contract A7, rules 1 and 2).
 *
 * <p>Factor selection for a category and activity year:
 * <ol>
 *   <li>Only regions IN and GLOBAL are considered.</li>
 *   <li>Take the factor with the greatest year that is at or before the activity year.
 *       If none is that old, take the earliest year available.</li>
 *   <li>If that year has both an IN and a GLOBAL factor, prefer IN.</li>
 * </ol>
 */
@Service
public class FactorBasedEmissionCalculator implements EmissionCalculator {

    static final String REGION_IN = "IN";
    static final String REGION_GLOBAL = "GLOBAL";
    private static final int SCALE = 3;

    private final EmissionFactorRepository factors;

    public FactorBasedEmissionCalculator(EmissionFactorRepository factors) {
        this.factors = factors;
    }

    @Override
    @Transactional(readOnly = true)
    public CalculationResult calculate(Category category, BigDecimal quantity, int year) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(quantity, "quantity");

        EmissionFactor factor = selectFactor(category, year);
        BigDecimal co2e = quantity.multiply(factor.getValueKgCo2ePerUnit()).setScale(SCALE, RoundingMode.HALF_UP);
        return new CalculationResult(factor, co2e);
    }

    private EmissionFactor selectFactor(Category category, int year) {
        List<EmissionFactor> candidates =
                factors.findByCategoryAndRegionIn(category, List.of(REGION_IN, REGION_GLOBAL));
        if (candidates.isEmpty()) {
            throw new BadRequestException("No emission factor is available for category " + category);
        }

        List<EmissionFactor> atOrBefore = candidates.stream().filter(f -> f.getYear() <= year).toList();
        if (!atOrBefore.isEmpty()) {
            // Latest year wins, then IN over GLOBAL.
            return atOrBefore.stream()
                    .max(Comparator.comparing(EmissionFactor::getYear).thenComparing(this::inPreference))
                    .orElseThrow();
        }
        // Activity is older than every factor: use the earliest one available.
        return candidates.stream()
                .min(Comparator.comparing(EmissionFactor::getYear).thenComparing(f -> -inPreference(f)))
                .orElseThrow();
    }

    /** Higher is better: IN = 1, GLOBAL = 0. */
    private int inPreference(EmissionFactor f) {
        return REGION_IN.equals(f.getRegion()) ? 1 : 0;
    }
}
