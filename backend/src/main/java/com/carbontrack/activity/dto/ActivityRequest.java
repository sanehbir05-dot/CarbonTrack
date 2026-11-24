package com.carbontrack.activity.dto;

import com.carbontrack.common.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** The client never sends unit, scope or CO2e. The backend derives them. */
public record ActivityRequest(
        @NotNull(message = "is required") Long facilityId,
        @NotNull(message = "is required") Category category,
        @NotNull(message = "is required")
        @DecimalMin(value = "0", inclusive = false, message = "must be positive")
        @Digits(integer = 13, fraction = 3, message = "must have at most 13 digits and 3 decimals")
        BigDecimal quantity,
        @NotNull(message = "is required") Integer year,
        @NotNull(message = "is required")
        @Min(value = 1, message = "must be between 1 and 12")
        @Max(value = 12, message = "must be between 1 and 12")
        Integer month) {
}
