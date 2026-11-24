package com.carbontrack.target.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TargetRequest(
        @NotBlank(message = "is required") @Size(max = 200, message = "must be at most 200 characters") String name,
        @NotNull(message = "is required") @Min(value = 2000, message = "must be 2000 or later") Integer baselineYear,
        @NotNull(message = "is required") @Max(value = 2100, message = "must be 2100 or earlier") Integer targetYear,
        @NotNull(message = "is required")
        @DecimalMin(value = "0", inclusive = false, message = "must be greater than 0")
        @DecimalMax(value = "100", message = "must be at most 100")
        @Digits(integer = 3, fraction = 2, message = "must have at most 2 decimals")
        BigDecimal reductionPct) {
}
