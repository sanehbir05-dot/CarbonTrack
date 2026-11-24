package com.carbontrack.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carbontrack.action.dto.ActionDto;
import com.carbontrack.action.dto.ActionRequest;
import com.carbontrack.common.ActionStatus;
import com.carbontrack.common.Category;
import com.carbontrack.common.CurrentUser;
import com.carbontrack.common.GlobalExceptionHandler;
import com.carbontrack.common.JwtService;
import com.carbontrack.common.Role;
import com.carbontrack.common.SecurityConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ActionController.class)
@Import({SecurityConfig.class, JwtService.class, CurrentUser.class, GlobalExceptionHandler.class})
class ActionControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean ActionService service;

    private String bearer(Role role) {
        return "Bearer " + jwt.generate(7L, 42L, role);
    }

    private static ActionDto dto() {
        return new ActionDto(1L, "Solar roof", 3L, "Pune Plant", Category.ELECTRICITY, new BigDecimal("12000.000"),
                LocalDate.of(2026, 1, 1), ActionStatus.PLANNED);
    }

    private static final String VALID = "{\"title\":\"Solar roof\",\"facilityId\":3,\"category\":\"ELECTRICITY\","
            + "\"expectedSavingKg\":12000,\"startDate\":\"2026-01-01\",\"status\":\"PLANNED\"}";

    @Test
    void listIsOpenToAnalystAndHasAllFields() throws Exception {
        when(service.list(42L)).thenReturn(List.of(dto()));

        mvc.perform(get("/api/actions").header("Authorization", bearer(Role.ANALYST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].title").value("Solar roof"))
            .andExpect(jsonPath("$[0].facilityId").value(3))
            .andExpect(jsonPath("$[0].facilityName").value("Pune Plant"))
            .andExpect(jsonPath("$[0].category").value("ELECTRICITY"))
            .andExpect(jsonPath("$[0].expectedSavingKg").value(12000.0))
            .andExpect(jsonPath("$[0].startDate").value("2026-01-01"))
            .andExpect(jsonPath("$[0].status").value("PLANNED"));
    }

    @Test
    void adminCanCreate() throws Exception {
        when(service.create(eq(42L), any(ActionRequest.class))).thenReturn(dto());

        mvc.perform(post("/api/actions").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void analystCanCreate() throws Exception {
        when(service.create(eq(42L), any(ActionRequest.class))).thenReturn(dto());
        mvc.perform(post("/api/actions").header("Authorization", bearer(Role.ANALYST))
                .contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isCreated());
        verify(service).create(eq(42L), any(ActionRequest.class));
    }

    @Test
    void onlyTitleAndStatusAreRequired() throws Exception {
        when(service.create(eq(42L), any(ActionRequest.class))).thenReturn(dto());

        mvc.perform(post("/api/actions").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\",\"status\":\"DONE\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void missingTitleOrStatusIs400() throws Exception {
        mvc.perform(post("/api/actions").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("title is required"));
        mvc.perform(post("/api/actions").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("status is required"));
    }

    @Test
    void invalidStatusListsAllowedValues() throws Exception {
        mvc.perform(post("/api/actions").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\",\"status\":\"LATER\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("status must be one of [PLANNED, IN_PROGRESS, DONE]"));
    }

    @Test
    void nonPositiveSavingAndBadDateAreRejected() throws Exception {
        mvc.perform(post("/api/actions").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"status\":\"DONE\",\"expectedSavingKg\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("expectedSavingKg must be positive"));
        mvc.perform(post("/api/actions").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"status\":\"DONE\",\"startDate\":\"01/02/2026\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuth() throws Exception {
        mvc.perform(get("/api/actions")).andExpect(status().isUnauthorized());
    }
}
