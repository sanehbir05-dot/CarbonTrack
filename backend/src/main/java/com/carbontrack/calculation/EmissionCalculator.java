package com.carbontrack.calculation;

import com.carbontrack.common.Category;
import java.math.BigDecimal;

/**
 * Converts an activity quantity to kg CO2e. The seeder and the activity service both depend on this
 * signature, so do not change it without telling the team.
 */
public interface EmissionCalculator {

    CalculationResult calculate(Category category, BigDecimal quantity, int year);
}
