package com.carbontrack.dashboard.dto;

import com.carbontrack.common.Category;
import java.math.BigDecimal;

/**
 * {@code changeVsPrevYearPct} is null when the previous year has no data.
 * {@code topCategory} and {@code topFacility} (a name) are null when the year has no data.
 */
public record DashboardSummary(int year, BigDecimal totalCo2eKg, BigDecimal scope1Kg, BigDecimal scope2Kg,
                               BigDecimal scope3Kg, BigDecimal changeVsPrevYearPct, Category topCategory,
                               String topFacility) {
}
