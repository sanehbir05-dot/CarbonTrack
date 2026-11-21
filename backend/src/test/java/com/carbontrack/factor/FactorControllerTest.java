package com.carbontrack.factor;

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
import com.carbontrack.factor.dto.FactorDto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FactorController.class)
@Import({SecurityConfig.class, JwtService.class, CurrentUser.class, GlobalExceptionHandler.class})
class FactorControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean FactorService service;

    @Test
    void returnsContractFieldsAndPassesYear() throws Exception {
        when(service.list(2025)).thenReturn(List.of(new FactorDto(1L, Category.ELECTRICITY, Scope.SCOPE_2, "kWh",
                new BigDecimal("0.710000"), "CEA", "IN", 2025)));

        mvc.perform(get("/api/factors?year=2025").header("Authorization", "Bearer " + jwt.generate(1L, 1L, Role.ANALYST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].category").value("ELECTRICITY"))
            .andExpect(jsonPath("$[0].scope").value("SCOPE_2"))
            .andExpect(jsonPath("$[0].valueKgCo2ePerUnit").value(0.71))
            .andExpect(jsonPath("$[0].region").value("IN"))
            .andExpect(jsonPath("$[0].year").value(2025));
    }

    @Test
    void nonNumericYearIs400() throws Exception {
        mvc.perform(get("/api/factors?year=abc").header("Authorization", "Bearer " + jwt.generate(1L, 1L, Role.ANALYST)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuth() throws Exception {
        mvc.perform(get("/api/factors")).andExpect(status().isUnauthorized());
    }
}
