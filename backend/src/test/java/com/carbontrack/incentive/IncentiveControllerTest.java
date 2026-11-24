package com.carbontrack.incentive;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carbontrack.common.CurrentUser;
import com.carbontrack.common.GlobalExceptionHandler;
import com.carbontrack.common.JwtService;
import com.carbontrack.common.Role;
import com.carbontrack.common.SecurityConfig;
import com.carbontrack.common.Tier;
import com.carbontrack.incentive.dto.IncentiveRuleDto;
import com.carbontrack.incentive.dto.IncentiveStatus;
import com.carbontrack.incentive.dto.IncentiveStatus.NextTier;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IncentiveController.class)
@Import({SecurityConfig.class, JwtService.class, CurrentUser.class, GlobalExceptionHandler.class})
class IncentiveControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean IncentiveService service;

    private String bearer() {
        return "Bearer " + jwt.generate(7L, 42L, Role.ANALYST);
    }

    @Test
    void rulesHaveContractFields() throws Exception {
        when(service.rules()).thenReturn(List.of(
                new IncentiveRuleDto(1L, Tier.BRONZE, "Bronze", new BigDecimal("5.00"), 100, "Simulated benefit")));

        mvc.perform(get("/api/incentives/rules").header("Authorization", bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tier").value("BRONZE"))
            .andExpect(jsonPath("$[0].minReductionPct").value(5.0))
            .andExpect(jsonPath("$[0].creditPoints").value(100))
            .andExpect(jsonPath("$[0].benefit").value("Simulated benefit"));
    }

    @Test
    void statusIncludesNextTier() throws Exception {
        when(service.status(42L, 2025)).thenReturn(new IncentiveStatus(2025, 2023, new BigDecimal("1000.000"),
                new BigDecimal("880.000"), new BigDecimal("12.00"), Tier.SILVER, 250, "b",
                new NextTier(Tier.GOLD, new BigDecimal("20.00"), new BigDecimal("8.00"))));

        mvc.perform(get("/api/incentives/status?year=2025").header("Authorization", bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value("SILVER"))
            .andExpect(jsonPath("$.reductionPct").value(12.0))
            .andExpect(jsonPath("$.baselineYear").value(2023))
            .andExpect(jsonPath("$.nextTier.tier").value("GOLD"))
            .andExpect(jsonPath("$.nextTier.gapPct").value(8.0));
    }

    @Test
    void nextTierIsNullAtTheTop() throws Exception {
        when(service.status(42L, null)).thenReturn(new IncentiveStatus(2025, 2023, new BigDecimal("1000.000"),
                new BigDecimal("700.000"), new BigDecimal("30.00"), Tier.GOLD, 500, "g", null));

        mvc.perform(get("/api/incentives/status").header("Authorization", bearer()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value("GOLD"))
            .andExpect(jsonPath("$.nextTier").value((Object) null));
    }

    @Test
    void requiresAuth() throws Exception {
        mvc.perform(get("/api/incentives/rules")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/incentives/status")).andExpect(status().isUnauthorized());
    }
}
