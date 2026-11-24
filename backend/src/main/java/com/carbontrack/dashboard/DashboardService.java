package com.carbontrack.dashboard;

import com.carbontrack.common.Scope;
import com.carbontrack.dashboard.DashboardRepository.CategoryTotal;
import com.carbontrack.dashboard.DashboardRepository.FacilityTotal;
import com.carbontrack.dashboard.DashboardRepository.MonthScopeTotal;
import com.carbontrack.dashboard.DashboardRepository.MonthTotal;
import com.carbontrack.dashboard.dto.CategoryShare;
import com.carbontrack.dashboard.dto.DashboardSummary;
import com.carbontrack.dashboard.dto.FacilityShare;
import com.carbontrack.dashboard.dto.MonthlyPoint;
import com.carbontrack.facility.FacilityService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every method takes the caller's companyId. It never comes from the request. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(3);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DashboardRepository repo;
    private final FacilityService facilities;

    public DashboardService(DashboardRepository repo, FacilityService facilities) {
        this.repo = repo;
        this.facilities = facilities;
    }

    /** Rule A7.5: endpoints with a year parameter default to the last full calendar year. */
    public static int defaultYear() {
        return Year.now().getValue() - 1;
    }

    /** Years that have data, ascending. */
    public List<Integer> years(Long companyId) {
        return repo.findYears(companyId);
    }

    public DashboardSummary summary(Long companyId, Integer yearParam) {
        int year = yearParam != null ? yearParam : defaultYear();
        Map<Scope, BigDecimal> scopes = scopeTotals(companyId, year);
        BigDecimal total = scopes.values().stream().reduce(ZERO, BigDecimal::add);

        BigDecimal prevTotal = scopeTotals(companyId, year - 1).values().stream().reduce(ZERO, BigDecimal::add);
        BigDecimal change = null;
        if (prevTotal.signum() > 0) {
            change = total.subtract(prevTotal).multiply(HUNDRED).divide(prevTotal, 1, RoundingMode.HALF_UP);
        }

        List<CategoryTotal> byCategory = repo.sumByCategory(companyId, year);
        List<FacilityTotal> byFacility = repo.sumByFacility(companyId, year);
        return new DashboardSummary(year, total, scopes.get(Scope.SCOPE_1), scopes.get(Scope.SCOPE_2),
                scopes.get(Scope.SCOPE_3), change,
                byCategory.isEmpty() ? null : byCategory.get(0).category(),
                byFacility.isEmpty() ? null : byFacility.get(0).facilityName());
    }

    /** Always 12 rows (January to December). Months without data are zero. */
    public List<MonthlyPoint> monthly(Long companyId, Integer yearParam, Long facilityId) {
        int year = yearParam != null ? yearParam : defaultYear();
        List<MonthScopeTotal> rows;
        if (facilityId == null) {
            rows = repo.sumByMonthAndScope(companyId, year);
        } else {
            facilities.getOwned(companyId, facilityId); // 404 for another company's facility
            rows = repo.sumByMonthAndScopeForFacility(companyId, year, facilityId);
        }
        return IntStream.rangeClosed(1, 12).mapToObj(m -> {
            BigDecimal s1 = sum(rows, m, Scope.SCOPE_1);
            BigDecimal s2 = sum(rows, m, Scope.SCOPE_2);
            BigDecimal s3 = sum(rows, m, Scope.SCOPE_3);
            return new MonthlyPoint(year, m, s1, s2, s3, s1.add(s2).add(s3));
        }).toList();
    }

    public List<CategoryShare> byCategory(Long companyId, Integer yearParam) {
        int year = yearParam != null ? yearParam : defaultYear();
        List<CategoryTotal> rows = repo.sumByCategory(companyId, year);
        BigDecimal total = rows.stream().map(CategoryTotal::kg).reduce(ZERO, BigDecimal::add);
        return rows.stream()
                .map(r -> new CategoryShare(r.category(), r.scope(), r.kg(), pct(r.kg(), total)))
                .toList();
    }

    public List<FacilityShare> byFacility(Long companyId, Integer yearParam) {
        int year = yearParam != null ? yearParam : defaultYear();
        List<FacilityTotal> rows = repo.sumByFacility(companyId, year);
        BigDecimal total = rows.stream().map(FacilityTotal::kg).reduce(ZERO, BigDecimal::add);
        return rows.stream()
                .map(r -> new FacilityShare(r.facilityId(), r.facilityName(), r.kg(), pct(r.kg(), total)))
                .toList();
    }

    /**
     * Total emission (kg) for every month that has data, in chronological order. Months without data
     * are not included. Reused by the AI forecast.
     */
    public List<MonthTotal> monthlyTotals(Long companyId) {
        return repo.sumByMonth(companyId);
    }

    /** Total emission (kg) of a whole year. Zero when there is no data. Reused by targets and incentives. */
    public BigDecimal yearTotal(Long companyId, int year) {
        return scopeTotals(companyId, year).values().stream().reduce(ZERO, BigDecimal::add);
    }

    private Map<Scope, BigDecimal> scopeTotals(Long companyId, int year) {
        Map<Scope, BigDecimal> totals = new EnumMap<>(Scope.class);
        for (Scope s : Scope.values()) {
            totals.put(s, ZERO);
        }
        repo.sumByScope(companyId, year).forEach(t -> totals.put(t.scope(), t.kg()));
        return totals;
    }

    private static BigDecimal sum(List<MonthScopeTotal> rows, int month, Scope scope) {
        return rows.stream().filter(r -> r.month() == month && r.scope() == scope)
                .map(MonthScopeTotal::kg).reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal pct(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return part.multiply(HUNDRED).divide(total, 1, RoundingMode.HALF_UP);
    }
}
