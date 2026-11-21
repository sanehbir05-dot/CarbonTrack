package com.carbontrack.target;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carbontrack.common.CurrentUser;
import com.carbontrack.common.GlobalExceptionHandler;
import com.carbontrack.common.JwtService;
import com.carbontrack.common.Role;
import com.carbontrack.common.SecurityConfig;
import com.carbontrack.common.TargetStatus;
import com.carbontrack.target.dto.TargetDto;
import com.carbontrack.target.dto.TargetRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TargetController.class)
@Import({SecurityConfig.class, JwtService.class, CurrentUser.class, GlobalExceptionHandler.class})
class TargetControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean TargetService service;

    private String bearer(Role role) {
        return "Bearer " + jwt.generate(7L, 42L, role);
    }

    private static TargetDto dto() {
        return new TargetDto(1L, "20% by 2027", 2023, 2027, new BigDecimal("20.00"), new BigDecimal("1000.000"),
                new BigDecimal("800.000"), 2025, new BigDecimal("880.000"), new BigDecimal("60.0"),
                TargetStatus.ON_TRACK);
    }

    private static String body(String pct, int baseline, int target) {
        return "{\"name\":\"20% by 2027\",\"baselineYear\":" + baseline + ",\"targetYear\":" + target
                + ",\"reductionPct\":" + pct + "}";
    }

    @Test
    void listReturnsContractFields() throws Exception {
        when(service.list(42L)).thenReturn(List.of(dto()));

        mvc.perform(get("/api/targets").header("Authorization", bearer(Role.ANALYST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("20% by 2027"))
            .andExpect(jsonPath("$[0].baselineKg").value(1000.0))
            .andExpect(jsonPath("$[0].targetKg").value(800.0))
            .andExpect(jsonPath("$[0].currentYear").value(2025))
            .andExpect(jsonPath("$[0].currentKg").value(880.0))
            .andExpect(jsonPath("$[0].achievedPct").value(60.0))
            .andExpect(jsonPath("$[0].status").value("ON_TRACK"));
    }

    @Test
    void adminCanCreate() throws Exception {
        when(service.create(eq(42L), any(TargetRequest.class))).thenReturn(dto());

        mvc.perform(post("/api/targets").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(body("20", 2023, 2027)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void analystCannotCreateOrDelete() throws Exception {
        mvc.perform(post("/api/targets").header("Authorization", bearer(Role.ANALYST))
                .contentType(MediaType.APPLICATION_JSON).content(body("20", 2023, 2027)))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/targets/1").header("Authorization", bearer(Role.ANALYST)))
            .andExpect(status().isForbidden());
        verify(service, never()).create(any(), any());
        verify(service, never()).delete(any(), any());
    }

    @Test
    void adminDeleteReturns204() throws Exception {
        mvc.perform(delete("/api/targets/5").header("Authorization", bearer(Role.ADMIN)))
            .andExpect(status().isNoContent());
        verify(service).delete(42L, 5L);
    }

    @Test
    void reductionOfZeroOrOver100IsRejected() throws Exception {
        mvc.perform(post("/api/targets").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(body("0", 2023, 2027)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("reductionPct must be greater than 0"));
        mvc.perform(post("/api/targets").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(body("100.5", 2023, 2027)))
            .andExpect(status().isBadRequest());
        verify(service, never()).create(any(), any());
    }

    @Test
    void missingFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/targets").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuth() throws Exception {
        mvc.perform(get("/api/targets")).andExpect(status().isUnauthorized());
    }
}
