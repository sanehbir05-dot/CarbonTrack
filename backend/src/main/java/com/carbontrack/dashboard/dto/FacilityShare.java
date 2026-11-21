package com.carbontrack.dashboard.dto;

import java.math.BigDecimal;

/** {@code pct} is the share of the year total, to one decimal place. */
public record FacilityShare(Long facilityId, String facilityName, BigDecimal co2eKg, BigDecimal pct) {
}
