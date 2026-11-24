package com.carbontrack.facility;

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
import com.carbontrack.common.FacilityType;
import com.carbontrack.common.GlobalExceptionHandler;
import com.carbontrack.common.JwtService;
import com.carbontrack.common.Role;
import com.carbontrack.common.SecurityConfig;
import com.carbontrack.facility.dto.FacilityDto;
import com.carbontrack.facility.dto.FacilityRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FacilityController.class)
@Import({SecurityConfig.class, JwtService.class, CurrentUser.class, GlobalExceptionHandler.class})
class FacilityControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean FacilityService service;

    private static final String BODY = "{\"name\":\"Pune Plant\",\"city\":\"Pune\",\"state\":\"MH\",\"type\":\"PLANT\"}";

    private String bearer(Role role) {
        return "Bearer " + jwt.generate(1L, 42L, role);
    }

    @Test
    void listUsesCompanyFromTokenAndIsOpenToAnalyst() throws Exception {
        when(service.list(42L)).thenReturn(List.of(new FacilityDto(1L, "Pune Plant", "Pune", "MH", FacilityType.PLANT)));

        mvc.perform(get("/api/facilities").header("Authorization", bearer(Role.ANALYST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Pune Plant"))
            .andExpect(jsonPath("$[0].type").value("PLANT"));
    }

    @Test
    void analystCannotCreate() throws Exception {
        mvc.perform(post("/api/facilities").header("Authorization", bearer(Role.ANALYST))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
        verify(service, never()).create(any(), any());
    }

    @Test
    void analystCannotDelete() throws Exception {
        mvc.perform(delete("/api/facilities/1").header("Authorization", bearer(Role.ANALYST)))
            .andExpect(status().isForbidden());
        verify(service, never()).delete(any(), any());
    }

    @Test
    void adminCanCreate() throws Exception {
        when(service.create(eq(42L), any(FacilityRequest.class)))
            .thenReturn(new FacilityDto(7L, "Pune Plant", "Pune", "MH", FacilityType.PLANT));

        mvc.perform(post("/api/facilities").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void adminDeleteReturns204() throws Exception {
        mvc.perform(delete("/api/facilities/7").header("Authorization", bearer(Role.ADMIN)))
            .andExpect(status().isNoContent());
        verify(service).delete(42L, 7L);
    }

    @Test
    void invalidTypeIs400() throws Exception {
        mvc.perform(post("/api/facilities").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\",\"type\":\"CASTLE\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void missingNameIs400() throws Exception {
        mvc.perform(post("/api/facilities").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PLANT\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(get("/api/facilities")).andExpect(status().isUnauthorized());
    }
}
