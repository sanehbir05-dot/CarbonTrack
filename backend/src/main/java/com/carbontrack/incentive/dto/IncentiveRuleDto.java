package com.carbontrack.incentive.dto;

import com.carbontrack.common.Tier;
import com.carbontrack.incentive.IncentiveRule;
import java.math.BigDecimal;

public record IncentiveRuleDto(Long id, Tier tier, String name, BigDecimal minReductionPct, Integer creditPoints,
                               String benefit) {

    public static IncentiveRuleDto from(IncentiveRule r) {
        return new IncentiveRuleDto(r.getId(), r.getTier(), r.getName(), r.getMinReductionPct(),
                r.getCreditPoints(), r.getBenefit());
    }
}
