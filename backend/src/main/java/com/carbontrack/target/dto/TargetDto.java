package com.carbontrack.target.dto;

import com.carbontrack.common.TargetStatus;
import com.carbontrack.target.Target;
import com.carbontrack.target.TargetCalculator;
import java.math.BigDecimal;

/** currentYear is the reporting year (the last full calendar year). All emissions are in kg. */
public record TargetDto(Long id, String name, Integer baselineYear, Integer targetYear, BigDecimal reductionPct,
                        BigDecimal baselineKg, BigDecimal targetKg, Integer currentYear, BigDecimal currentKg,
                        BigDecimal achievedPct, TargetStatus status) {

    public static TargetDto of(Target t, int currentYear, TargetCalculator.Progress p) {
        return new TargetDto(t.getId(), t.getName(), t.getBaselineYear(), t.getTargetYear(), t.getReductionPct(),
                p.baselineKg(), p.targetKg(), currentYear, p.currentKg(), p.achievedPct(), p.status());
    }
}
