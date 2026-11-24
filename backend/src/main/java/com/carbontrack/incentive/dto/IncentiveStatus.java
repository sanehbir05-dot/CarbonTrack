package com.carbontrack.incentive.dto;

import com.carbontrack.common.Tier;
import java.math.BigDecimal;

/** {@code nextTier} is null when already at the top tier. {@code baselineYear} is null if there is no data yet. */
public record IncentiveStatus(int year, Integer baselineYear, BigDecimal baselineKg, BigDecimal currentKg,
                              BigDecimal reductionPct, Tier tier, int creditPoints, String benefit,
                              NextTier nextTier) {

    public record NextTier(Tier tier, BigDecimal minReductionPct, BigDecimal gapPct) {
    }
}
