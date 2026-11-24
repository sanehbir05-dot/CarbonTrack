package com.carbontrack.incentive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carbontrack.common.Tier;
import com.carbontrack.company.Company;
import com.carbontrack.company.CompanyRepository;
import com.carbontrack.dashboard.DashboardService;
import com.carbontrack.incentive.dto.IncentiveStatus;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncentiveServiceTest {

    private static final Long C = 42L;

    @Mock IncentiveRuleRepository rules;
    @Mock CompanyRepository companies;
    @Mock DashboardService dashboard;
    @InjectMocks IncentiveService service;

    private Company company;

    private static IncentiveRule rule(Tier tier, String min, int points, String benefit) {
        IncentiveRule r = new IncentiveRule();
        r.setTier(tier);
        r.setName(tier.name());
        r.setMinReductionPct(new BigDecimal(min));
        r.setCreditPoints(points);
        r.setBenefit(benefit);
        return r;
    }

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setBaselineYear(2023);
        lenient().when(companies.findById(C)).thenReturn(Optional.of(company));
    }

    private void withRules() {
        when(rules.findAllByOrderByMinReductionPctAsc()).thenReturn(List.of(
                rule(Tier.BRONZE, "5.00", 100, "b"), rule(Tier.SILVER, "10.00", 250, "s"),
                rule(Tier.GOLD, "20.00", 500, "g")));
    }

    @Test
    void demoCompanyIsSilverWithGoldNext() {
        withRules();
        when(dashboard.yearTotal(C, 2023)).thenReturn(new BigDecimal("1000.000"));
        when(dashboard.yearTotal(C, 2025)).thenReturn(new BigDecimal("880.000"));

        IncentiveStatus s = service.status(C, 2025);

        assertEquals(Tier.SILVER, s.tier());
        assertEquals(250, s.creditPoints());
        assertEquals("s", s.benefit());
        assertEquals(new BigDecimal("12.00"), s.reductionPct());
        assertEquals(2023, s.baselineYear());
        assertEquals(Tier.GOLD, s.nextTier().tier());
        assertEquals(new BigDecimal("20.00"), s.nextTier().minReductionPct());
        assertEquals(new BigDecimal("8.00"), s.nextTier().gapPct());
    }

    @Test
    void goldHasNoNextTier() {
        withRules();
        when(dashboard.yearTotal(C, 2023)).thenReturn(new BigDecimal("1000.000"));
        when(dashboard.yearTotal(C, 2025)).thenReturn(new BigDecimal("700.000"));

        IncentiveStatus s = service.status(C, 2025);

        assertEquals(Tier.GOLD, s.tier());
        assertNull(s.nextTier());
    }

    @Test
    void emissionsRisingGivesNoneAndGapToBronze() {
        withRules();
        when(dashboard.yearTotal(C, 2023)).thenReturn(new BigDecimal("1000.000"));
        when(dashboard.yearTotal(C, 2025)).thenReturn(new BigDecimal("1030.000"));

        IncentiveStatus s = service.status(C, 2025);

        assertEquals(Tier.NONE, s.tier());
        assertEquals(0, s.creditPoints());
        assertNull(s.benefit());
        assertEquals(new BigDecimal("-3.00"), s.reductionPct());
        assertEquals(Tier.BRONZE, s.nextTier().tier());
        assertEquals(new BigDecimal("8.00"), s.nextTier().gapPct());
    }

    @Test
    void companyWithoutDataHasNoBaselineAndTierNone() {
        company.setBaselineYear(null);
        withRules();
        when(dashboard.yearTotal(C, 2025)).thenReturn(BigDecimal.ZERO.setScale(3));

        IncentiveStatus s = service.status(C, 2025);

        assertNull(s.baselineYear());
        assertEquals(0, s.baselineKg().signum());
        assertEquals(Tier.NONE, s.tier());
        assertEquals(new BigDecimal("0.00"), s.reductionPct());
        assertEquals(new BigDecimal("5.00"), s.nextTier().gapPct());
        verify(dashboard, never()).yearTotal(C, 2023);
    }

    @Test
    void zeroBaselineDoesNotDivideByZero() {
        withRules();
        when(dashboard.yearTotal(C, 2023)).thenReturn(BigDecimal.ZERO.setScale(3));
        when(dashboard.yearTotal(C, 2025)).thenReturn(new BigDecimal("500.000"));

        IncentiveStatus s = service.status(C, 2025);

        assertEquals(Tier.NONE, s.tier());
        assertEquals(new BigDecimal("0.00"), s.reductionPct());
    }

    @Test
    void yearWithNoDataIsNotTreatedAsAFullReduction() {
        withRules();
        when(dashboard.yearTotal(C, 2023)).thenReturn(new BigDecimal("1000.000"));
        when(dashboard.yearTotal(C, 2022)).thenReturn(BigDecimal.ZERO.setScale(3));

        IncentiveStatus s = service.status(C, 2022);

        assertEquals(Tier.NONE, s.tier());
        assertEquals(new BigDecimal("0.00"), s.reductionPct());
    }

    @Test
    void missingYearDefaultsToLastFullYear() {
        withRules();
        int lastYear = Year.now().getValue() - 1;
        when(dashboard.yearTotal(any(), anyInt())).thenReturn(new BigDecimal("1000.000"));

        assertEquals(lastYear, service.status(C, null).year());
    }

    @Test
    void rulesComeBackLowestFirst() {
        withRules();

        List<Tier> tiers = service.rules().stream().map(r -> r.tier()).toList();

        assertEquals(List.of(Tier.BRONZE, Tier.SILVER, Tier.GOLD), tiers);
    }
}
