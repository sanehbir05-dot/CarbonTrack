package com.carbontrack.dashboard.dto;

import com.carbontrack.common.Category;
import com.carbontrack.common.Scope;
import java.math.BigDecimal;

/** {@code pct} is the share of the year total, to one decimal place. */
public record CategoryShare(Category category, Scope scope, BigDecimal co2eKg, BigDecimal pct) {
}
