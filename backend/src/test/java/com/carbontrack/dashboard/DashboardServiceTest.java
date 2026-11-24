package com.carbontrack.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carbontrack.common.Category;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.common.Scope;
import com.carbontrack.dashboard.DashboardRepository.CategoryTotal;
import com.carbontrack.dashboard.DashboardRepository.FacilityTotal;
import com.carbontrack.dashboard.DashboardRepository.MonthScopeTotal;
import com.carbontrack.dashboard.DashboardRepository.ScopeTotal;
import com.carbontrack.dashboard.dto.CategoryShare;
import com.carbontrack.dashboard.dto.DashboardSummary;
import com.carbontrack.dashboard.dto.FacilityShare;
import com.carbontrack.dashboard.dto.MonthlyPoint;
import com.carbontrack.facility.FacilityService;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Long C = 42L;

    @Mock DashboardRepository repo;
    @Mock FacilityService facilities;
    @InjectMocks DashboardService service;

    private static BigDecimal kg(String v) {
        return new BigDecimal(v);
    }

    @Test
    void monthlyAlwaysReturnsTwelveZeroFilledRows() {
        when(repo.sumByMonthAndScope(C, 2025)).thenReturn(List.of(
                new MonthScopeTotal(3, Scope.SCOPE_1, kg("100.000")),
                new MonthScopeTotal(3, Scope.SCOPE_2, kg("50.500")),
                new MonthScopeTotal(12, Scope.SCOPE_3, kg("7.000"))));

        List<MonthlyPoint> rows = service.monthly(C, 2025, null);

        assertEquals(12, rows.size());
        assertEquals(1, rows.get(0).month());
        assertEquals(12, rows.get(11).month());
        assertEquals(0, rows.get(0).totalKg().signum());
        assertEquals(2025, rows.get(0).year());

        MonthlyPoint march = rows.get(2);
        assertEquals(kg("100.000"), march.scope1Kg());
        assertEquals(kg("50.500"), march.scope2Kg());
        assertEquals(0, march.scope3Kg().signum());
        assertEquals(0, kg("150.500").compareTo(march.totalKg()));
        assertEquals(0, kg("7").compareTo(rows.get(11).totalKg()));
    }

    @Test
    void monthlyWithNoDataIsStillTwelveZeroRows() {
        when(repo.sumByMonthAndScope(C, 2025)).thenReturn(List.of());

        List<MonthlyPoint> rows = service.monthly(C, 2025, null);

        assertEquals(12, rows.size());
        assertEquals(true, rows.stream().allMatch(r -> r.totalKg().signum() == 0));
    }

    @Test
    void monthlyForFacilityChecksOwnershipAndUsesFacilityQuery() {
        when(repo.sumByMonthAndScopeForFacility(C, 2025, 5L)).thenReturn(List.of());

        service.monthly(C, 2025, 5L);

        verify(facilities).getOwned(C, 5L);
        verify(repo, never()).sumByMonthAndScope(C, 2025);
    }

    @Test
    void monthlyForAnotherCompanysFacilityIsNotFound() {
        when(facilities.getOwned(C, 5L)).thenThrow(new NotFoundException("Facility not found"));

        assertThrows(NotFoundException.class, () -> service.monthly(C, 2025, 5L));
        verify(repo, never()).sumByMonthAndScopeForFacility(C, 2025, 5L);
    }

    @Test
    void monthlyDefaultsToLastFullYear() {
        int lastYear = Year.now().getValue() - 1;
        when(repo.sumByMonthAndScope(C, lastYear)).thenReturn(List.of());

        assertEquals(lastYear, service.monthly(C, null, null).get(0).year());
    }

    @Test
    void summaryTotalsScopesChangeAndTops() {
        when(repo.sumByScope(C, 2025)).thenReturn(List.of(
                new ScopeTotal(Scope.SCOPE_1, kg("300.000")), new ScopeTotal(Scope.SCOPE_2, kg("500.000"))));
        when(repo.sumByScope(C, 2024)).thenReturn(List.of(new ScopeTotal(Scope.SCOPE_2, kg("1000.000"))));
        when(repo.sumByCategory(C, 2025)).thenReturn(List.of(
                new CategoryTotal(Category.ELECTRICITY, Scope.SCOPE_2, kg("500.000")),
                new CategoryTotal(Category.DIESEL, Scope.SCOPE_1, kg("300.000"))));
        when(repo.sumByFacility(C, 2025)).thenReturn(List.of(new FacilityTotal(1L, "Pune Plant", kg("800.000"))));

        DashboardSummary s = service.summary(C, 2025);

        assertEquals(0, kg("800").compareTo(s.totalCo2eKg()));
        assertEquals(0, kg("300").compareTo(s.scope1Kg()));
        assertEquals(0, kg("500").compareTo(s.scope2Kg()));
        assertEquals(0, s.scope3Kg().signum());
        assertEquals(kg("-20.0"), s.changeVsPrevYearPct());
        assertEquals(Category.ELECTRICITY, s.topCategory());
        assertEquals("Pune Plant", s.topFacility());
    }

    @Test
    void summaryChangeIsNullWithoutPreviousYearData() {
        when(repo.sumByScope(C, 2025)).thenReturn(List.of(new ScopeTotal(Scope.SCOPE_1, kg("100.000"))));
        when(repo.sumByScope(C, 2024)).thenReturn(List.of());
        when(repo.sumByCategory(C, 2025)).thenReturn(List.of());
        when(repo.sumByFacility(C, 2025)).thenReturn(List.of());

        assertNull(service.summary(C, 2025).changeVsPrevYearPct());
    }

    @Test
    void summaryIncreaseIsPositiveAndRoundedToOneDecimal() {
        when(repo.sumByScope(C, 2025)).thenReturn(List.of(new ScopeTotal(Scope.SCOPE_1, kg("1000.000"))));
        when(repo.sumByScope(C, 2024)).thenReturn(List.of(new ScopeTotal(Scope.SCOPE_1, kg("300.000"))));
        when(repo.sumByCategory(C, 2025)).thenReturn(List.of());
        when(repo.sumByFacility(C, 2025)).thenReturn(List.of());

        // (1000 - 300) / 300 = 233.333...%
        assertEquals(kg("233.3"), service.summary(C, 2025).changeVsPrevYearPct());
    }

    @Test
    void summaryForYearWithNoDataIsZerosAndNulls() {
        when(repo.sumByScope(C, 2025)).thenReturn(List.of());
        when(repo.sumByScope(C, 2024)).thenReturn(List.of());
        when(repo.sumByCategory(C, 2025)).thenReturn(List.of());
        when(repo.sumByFacility(C, 2025)).thenReturn(List.of());

        DashboardSummary s = service.summary(C, 2025);

        assertEquals(0, s.totalCo2eKg().signum());
        assertNull(s.changeVsPrevYearPct());
        assertNull(s.topCategory());
        assertNull(s.topFacility());
    }

    @Test
    void byCategoryPctIsShareOfYearTotalToOneDecimal() {
        when(repo.sumByCategory(C, 2025)).thenReturn(List.of(
                new CategoryTotal(Category.ELECTRICITY, Scope.SCOPE_2, kg("500.000")),
                new CategoryTotal(Category.DIESEL, Scope.SCOPE_1, kg("300.000")),
                new CategoryTotal(Category.LPG, Scope.SCOPE_1, kg("200.000"))));

        List<CategoryShare> shares = service.byCategory(C, 2025);

        assertEquals(kg("50.0"), shares.get(0).pct());
        assertEquals(kg("30.0"), shares.get(1).pct());
        assertEquals(kg("20.0"), shares.get(2).pct());
        assertEquals(Scope.SCOPE_2, shares.get(0).scope());
    }

    @Test
    void byFacilityPctRoundsHalfUp() {
        when(repo.sumByFacility(C, 2025)).thenReturn(List.of(
                new FacilityTotal(1L, "A", kg("2.000")), new FacilityTotal(2L, "B", kg("1.000"))));

        List<FacilityShare> shares = service.byFacility(C, 2025);

        assertEquals(kg("66.7"), shares.get(0).pct());
        assertEquals(kg("33.3"), shares.get(1).pct());
    }

    @Test
    void byFacilityIsEmptyWhenNoData() {
        when(repo.sumByFacility(C, 2025)).thenReturn(List.of());

        assertEquals(List.of(), service.byFacility(C, 2025));
    }

    @Test
    void monthlyTotalsAndYearsDelegateToTheRepositoryForTheCompany() {
        var totals = List.of(new DashboardRepository.MonthTotal(2023, 1, kg("10.000")));
        when(repo.sumByMonth(C)).thenReturn(totals);
        when(repo.findYears(C)).thenReturn(List.of(2023, 2024));

        assertEquals(totals, service.monthlyTotals(C));
        assertEquals(List.of(2023, 2024), service.years(C));
    }

    @Test
    void yearTotalSumsAllScopesAndIsZeroWithoutData() {
        when(repo.sumByScope(C, 2025)).thenReturn(List.of(
                new ScopeTotal(Scope.SCOPE_1, kg("1.500")), new ScopeTotal(Scope.SCOPE_3, kg("2.250"))));
        when(repo.sumByScope(C, 2020)).thenReturn(List.of());

        assertEquals(0, kg("3.75").compareTo(service.yearTotal(C, 2025)));
        assertEquals(0, service.yearTotal(C, 2020).signum());
    }
}
