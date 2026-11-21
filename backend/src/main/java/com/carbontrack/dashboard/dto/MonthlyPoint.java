package com.carbontrack.dashboard.dto;

import java.math.BigDecimal;

public record MonthlyPoint(int year, int month, BigDecimal scope1Kg, BigDecimal scope2Kg, BigDecimal scope3Kg,
                           BigDecimal totalKg) {
}
