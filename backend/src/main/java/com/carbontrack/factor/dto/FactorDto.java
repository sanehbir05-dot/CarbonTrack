package com.carbontrack.factor.dto;

import com.carbontrack.common.Category;
import com.carbontrack.common.Scope;
import com.carbontrack.factor.EmissionFactor;
import java.math.BigDecimal;

public record FactorDto(Long id, Category category, Scope scope, String unit, BigDecimal valueKgCo2ePerUnit,
                        String source, String region, Integer year) {

    public static FactorDto from(EmissionFactor f) {
        return new FactorDto(f.getId(), f.getCategory(), f.getScope(), f.getUnit(), f.getValueKgCo2ePerUnit(),
                f.getSource(), f.getRegion(), f.getYear());
    }
}
