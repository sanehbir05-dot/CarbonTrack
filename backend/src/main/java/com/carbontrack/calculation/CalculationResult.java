package com.carbontrack.calculation;

import com.carbontrack.factor.EmissionFactor;
import java.math.BigDecimal;

/** The factor that was used and the resulting emission in kg CO2e (scale 3). */
public record CalculationResult(EmissionFactor factor, BigDecimal co2eKg) {
}
