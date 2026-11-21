package com.carbontrack.incentive;

import com.carbontrack.common.NotFoundException;
import com.carbontrack.common.Tier;
import com.carbontrack.company.Company;
import com.carbontrack.company.CompanyRepository;
import com.carbontrack.dashboard.DashboardService;
import com.carbontrack.incentive.IncentiveCalculator.Result;
import com.carbontrack.incentive.dto.IncentiveRuleDto;
import com.carbontrack.incentive.dto.IncentiveStatus;
import com.carbontrack.incentive.dto.IncentiveStatus.NextTier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The incentive scheme is simulated for this project. Every method takes the caller's companyId. */
@Service
@Transactional(readOnly = true)
public class IncentiveService {

    private final IncentiveRuleRepository rules;
    private final CompanyRepository companies;
    private final DashboardService dashboard;

    public IncentiveService(IncentiveRuleRepository rules, CompanyRepository companies, DashboardService dashboard) {
        this.rules = rules;
        this.companies = companies;
        this.dashboard = dashboard;
    }

    public List<IncentiveRuleDto> rules() {
        return rules.findAllByOrderByMinReductionPctAsc().stream().map(IncentiveRuleDto::from).toList();
    }

    public IncentiveStatus status(Long companyId, Integer yearParam) {
        int year = yearParam != null ? yearParam : DashboardService.defaultYear();
        Company company = companies.findById(companyId).orElseThrow(() -> new NotFoundException("Company not found"));

        Integer baselineYear = company.getBaselineYear();
        BigDecimal baselineKg = baselineYear == null ? BigDecimal.ZERO.setScale(3)
                : dashboard.yearTotal(companyId, baselineYear);
        BigDecimal currentKg = dashboard.yearTotal(companyId, year);

        BigDecimal exact = IncentiveCalculator.reductionPct(baselineKg, currentKg);
        Result result = IncentiveCalculator.evaluate(rules.findAllByOrderByMinReductionPctAsc(), exact);

        IncentiveRule achieved = result.achieved();
        NextTier next = result.next() == null ? null : new NextTier(result.next().getTier(),
                result.next().getMinReductionPct(), IncentiveCalculator.gapPct(result.next(), exact));

        return new IncentiveStatus(year, baselineYear, baselineKg, currentKg,
                exact.setScale(2, RoundingMode.HALF_UP),
                achieved == null ? Tier.NONE : achieved.getTier(),
                achieved == null ? 0 : achieved.getCreditPoints(),
                achieved == null ? null : achieved.getBenefit(),
                next);
    }
}
