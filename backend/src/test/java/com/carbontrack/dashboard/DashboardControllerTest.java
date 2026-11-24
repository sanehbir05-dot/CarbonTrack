package com.carbontrack.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carbontrack.common.Category;
import com.carbontrack.common.CurrentUser;
import com.carbontrack.common.GlobalExceptionHandler;
import com.carbontrack.common.JwtService;
import com.carbontrack.common.Role;
import com.carbontrack.common.Scope;
import com.carbontrack.common.SecurityConfig;
import com.carbontrack.dashboard.dto.CategoryShare;
import com.carbontrack.dashboard.dto.DashboardSummary;
import com.carbontrack.dashboard.dto.FacilityShare;
import com.carbontrack.dashboard.dto.MonthlyPoint;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, JwtService.class, CurrentUser.class, GlobalExceptionHandler.class})
class DashboardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean DashboardService service;

    private String bearer() {
        return "Bearer " + jwt.generate(7L, 42L, Role.ANALYST);
    }

    @Test
    void yearsReturnsPlainArray() throws Exception {
        when(service.years(42L)).thenReturn(List.of(2023, 2024, 2025));

        mvc.perform(get("/api/dashboard/years").header("Authorization", bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value(2023))
            .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void summaryHasContractFieldsAndNullChange() throws Exception {
        when(service.summary(42L, 2025)).thenReturn(new DashboardSummary(2025, new BigDecimal("800.000"),
                new BigDecimal("300.000"), new BigDecimal("500.000"), BigDecimal.ZERO, null,
                Category.ELECTRICITY, "Pune Plant"));

        mvc.perform(get("/api/dashboard/summary?year=2025").header("Authorization", bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.year").value(2025))
            .andExpect(jsonPath("$.totalCo2eKg").value(800.0))
            .andExpect(jsonPath("$.scope1Kg").value(300.0))
            .andExpect(jsonPath("$.scope2Kg").value(500.0))
            .andExpect(jsonPath("$.scope3Kg").value(0))
            .andExpect(jsonPath("$.changeVsPrevYearPct").doesNotExist())
            .andExpect(jsonPath("$.topCategory").value("ELECTRICITY"))
            .andExpect(jsonPath("$.topFacility").value("Pune Plant"));
    }

    @Test
    void summaryWithoutYearPassesNullSoServiceAppliesDefault() throws Exception {
        when(service.summary(42L, null)).thenReturn(new DashboardSummary(2025, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, null));

        mvc.perform(get("/api/dashboard/summary").header("Authorization", bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.year").value(2025));
    }

    @Test
    void monthlyPassesFacilityFilter() throws Exception {
        when(service.monthly(42L, 2025, 3L)).thenReturn(List.of(new MonthlyPoint(2025, 1, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE)));

        mvc.perform(get("/api/dashboard/monthly?year=2025&facilityId=3").header("Authorization", bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].month").value(1))
            .andExpect(jsonPath("$[0].totalKg").value(1));
    }

    @Test
    void byCategoryAndByFacilityShapes() throws Exception {
        when(service.byCategory(42L, 2025)).thenReturn(List.of(
                new CategoryShare(Category.DIESEL, Scope.SCOPE_1, new BigDecimal("300.000"), new BigDecimal("37.5"))));
        when(service.byFacility(42L, 2025)).thenReturn(List.of(
                new FacilityShare(1L, "Pune Plant", new BigDecimal("800.000"), new BigDecimal("100.0"))));

        mvc.perform(get("/api/dashboard/by-category?year=2025").header("Authorization", bearer()))
            .andExpect(jsonPath("$[0].category").value("DIESEL"))
            .andExpect(jsonPath("$[0].scope").value("SCOPE_1"))
            .andExpect(jsonPath("$[0].pct").value(37.5));
        mvc.perform(get("/api/dashboard/by-facility?year=2025").header("Authorization", bearer()))
            .andExpect(jsonPath("$[0].facilityId").value(1))
            .andExpect(jsonPath("$[0].facilityName").value("Pune Plant"))
            .andExpect(jsonPath("$[0].pct").value(100.0));
    }

    @Test
    void nonNumericYearIs400() throws Exception {
        mvc.perform(get("/api/dashboard/summary?year=abc").header("Authorization", bearer()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuth() throws Exception {
        mvc.perform(get("/api/dashboard/years")).andExpect(status().isUnauthorized());
    }
}
