package com.carbontrack.dashboard;

import com.carbontrack.activity.ActivityRecord;
import com.carbontrack.common.Category;
import com.carbontrack.common.Scope;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * All aggregation happens in SQL (SUM and GROUP BY). Records are never loaded into memory.
 * Every query is filtered by company_id.
 */
public interface DashboardRepository extends Repository<ActivityRecord, Long> {

    record ScopeTotal(Scope scope, BigDecimal kg) {}

    record MonthScopeTotal(int month, Scope scope, BigDecimal kg) {}

    record CategoryTotal(Category category, Scope scope, BigDecimal kg) {}

    record FacilityTotal(Long facilityId, String facilityName, BigDecimal kg) {}

    record MonthTotal(int year, int month, BigDecimal kg) {}

    @Query("select distinct r.year from ActivityRecord r where r.companyId = :companyId order by r.year")
    List<Integer> findYears(@Param("companyId") Long companyId);

    @Query("""
            select new com.carbontrack.dashboard.DashboardRepository$ScopeTotal(r.scope, sum(r.co2eKg))
            from ActivityRecord r
            where r.companyId = :companyId and r.year = :year
            group by r.scope""")
    List<ScopeTotal> sumByScope(@Param("companyId") Long companyId, @Param("year") int year);

    @Query("""
            select new com.carbontrack.dashboard.DashboardRepository$MonthScopeTotal(r.month, r.scope, sum(r.co2eKg))
            from ActivityRecord r
            where r.companyId = :companyId and r.year = :year
            group by r.month, r.scope""")
    List<MonthScopeTotal> sumByMonthAndScope(@Param("companyId") Long companyId, @Param("year") int year);

    @Query("""
            select new com.carbontrack.dashboard.DashboardRepository$MonthScopeTotal(r.month, r.scope, sum(r.co2eKg))
            from ActivityRecord r
            where r.companyId = :companyId and r.year = :year and r.facility.id = :facilityId
            group by r.month, r.scope""")
    List<MonthScopeTotal> sumByMonthAndScopeForFacility(@Param("companyId") Long companyId,
                                                        @Param("year") int year,
                                                        @Param("facilityId") Long facilityId);

    @Query("""
            select new com.carbontrack.dashboard.DashboardRepository$CategoryTotal(r.category, r.scope, sum(r.co2eKg))
            from ActivityRecord r
            where r.companyId = :companyId and r.year = :year
            group by r.category, r.scope
            order by sum(r.co2eKg) desc, r.category asc""")
    List<CategoryTotal> sumByCategory(@Param("companyId") Long companyId, @Param("year") int year);

    @Query("""
            select new com.carbontrack.dashboard.DashboardRepository$FacilityTotal(f.id, f.name, sum(r.co2eKg))
            from ActivityRecord r join r.facility f
            where r.companyId = :companyId and r.year = :year
            group by f.id, f.name
            order by sum(r.co2eKg) desc, f.name asc""")
    List<FacilityTotal> sumByFacility(@Param("companyId") Long companyId, @Param("year") int year);

    @Query("""
            select new com.carbontrack.dashboard.DashboardRepository$MonthTotal(r.year, r.month, sum(r.co2eKg))
            from ActivityRecord r
            where r.companyId = :companyId
            group by r.year, r.month
            order by r.year, r.month""")
    List<MonthTotal> sumByMonth(@Param("companyId") Long companyId);
}
