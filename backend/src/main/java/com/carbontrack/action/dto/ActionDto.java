package com.carbontrack.action.dto;

import com.carbontrack.action.ReductionAction;
import com.carbontrack.common.ActionStatus;
import com.carbontrack.common.Category;
import java.math.BigDecimal;
import java.time.LocalDate;

/** facilityId and facilityName are null for a company-wide action. */
public record ActionDto(Long id, String title, Long facilityId, String facilityName, Category category,
                        BigDecimal expectedSavingKg, LocalDate startDate, ActionStatus status) {

    public static ActionDto from(ReductionAction a) {
        return new ActionDto(a.getId(), a.getTitle(),
                a.getFacility() == null ? null : a.getFacility().getId(),
                a.getFacility() == null ? null : a.getFacility().getName(),
                a.getCategory(), a.getExpectedSavingKg(), a.getStartDate(), a.getStatus());
    }
}
