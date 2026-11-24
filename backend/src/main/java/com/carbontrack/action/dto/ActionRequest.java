package com.carbontrack.action.dto;

import com.carbontrack.common.ActionStatus;
import com.carbontrack.common.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** facilityId, category, expectedSavingKg and startDate are optional. startDate is ISO yyyy-MM-dd. */
public record ActionRequest(
        @NotBlank(message = "is required") @Size(max = 200, message = "must be at most 200 characters") String title,
        Long facilityId,
        Category category,
        @DecimalMin(value = "0", inclusive = false, message = "must be positive")
        @Digits(integer = 15, fraction = 3, message = "must have at most 15 digits and 3 decimals")
        BigDecimal expectedSavingKg,
        LocalDate startDate,
        @NotNull(message = "is required") ActionStatus status) {
}
