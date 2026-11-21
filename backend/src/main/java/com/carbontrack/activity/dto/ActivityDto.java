package com.carbontrack.activity.dto;

import com.carbontrack.activity.ActivityRecord;
import com.carbontrack.common.Category;
import com.carbontrack.common.RecordSource;
import com.carbontrack.common.Scope;
import java.math.BigDecimal;
import java.time.Instant;

public record ActivityDto(Long id, Long facilityId, String facilityName, Category category, Scope scope,
                          BigDecimal quantity, String unit, Integer year, Integer month, BigDecimal co2eKg,
                          Long factorId, BigDecimal factorValue, String factorSource, RecordSource source,
                          Instant createdAt) {

    public static ActivityDto from(ActivityRecord r) {
        return new ActivityDto(r.getId(), r.getFacility().getId(), r.getFacility().getName(), r.getCategory(),
                r.getScope(), r.getQuantity(), r.getUnit(), r.getYear(), r.getMonth(), r.getCo2eKg(),
                r.getFactor().getId(), r.getFactor().getValueKgCo2ePerUnit(), r.getFactor().getSource(),
                r.getSource(), r.getCreatedAt());
    }
}
