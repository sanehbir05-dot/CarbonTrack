package com.carbontrack.activity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carbontrack.activity.dto.ActivityDto;
import com.carbontrack.activity.dto.ActivityRequest;
import com.carbontrack.activity.dto.UploadResult;
import com.carbontrack.common.BadRequestException;
import com.carbontrack.common.Category;
import com.carbontrack.common.CurrentUser;
import com.carbontrack.common.GlobalExceptionHandler;
import com.carbontrack.common.JwtService;
import com.carbontrack.common.PageResponse;
import com.carbontrack.common.RecordSource;
import com.carbontrack.common.Role;
import com.carbontrack.common.Scope;
import com.carbontrack.common.SecurityConfig;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ActivityController.class)
@Import({SecurityConfig.class, JwtService.class, CurrentUser.class, GlobalExceptionHandler.class})
class ActivityControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean ActivityService service;
    @MockitoBean CsvImportService csvImport;

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "data.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void uploadReturnsImportedFailedAndRowErrors() throws Exception {
        when(csvImport.importCsv(eq(42L), eq(7L), any(InputStream.class))).thenReturn(
                new UploadResult(2, 1, List.of(new UploadResult.RowError(4, "facility 'X' not found"))));

        mvc.perform(multipart("/api/activities/upload").file(csv("facilityName,category,quantity,year,month\n"))
                .header("Authorization", bearer(Role.ANALYST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(2))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.errors[0].row").value(4))
            .andExpect(jsonPath("$.errors[0].message").value("facility 'X' not found"));
    }

    @Test
    void uploadOfEmptyFileIs400() throws Exception {
        mvc.perform(multipart("/api/activities/upload").file(csv(""))
                .header("Authorization", bearer(Role.ADMIN)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("CSV file is empty"));
        verify(csvImport, never()).importCsv(any(), any(), any());
    }

    @Test
    void uploadWithoutFilePartIs400() throws Exception {
        mvc.perform(multipart("/api/activities/upload").header("Authorization", bearer(Role.ADMIN)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Missing required part 'file'"));
    }

    @Test
    void uploadWithBadHeaderIs400WithServiceMessage() throws Exception {
        when(csvImport.importCsv(any(), any(), any(InputStream.class)))
            .thenThrow(new BadRequestException("CSV header must be facilityName,category,quantity,year,month"));

        mvc.perform(multipart("/api/activities/upload").file(csv("a,b\n1,2\n"))
                .header("Authorization", bearer(Role.ADMIN)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                    org.hamcrest.Matchers.startsWith("CSV header must be")));
    }

    @Test
    void uploadRequiresAuth() throws Exception {
        mvc.perform(multipart("/api/activities/upload").file(csv("x"))).andExpect(status().isUnauthorized());
    }

    private String bearer(Role role) {
        return "Bearer " + jwt.generate(7L, 42L, role);
    }

    private static String body(String qty, String category, int month) {
        return "{\"facilityId\":3,\"category\":\"" + category + "\",\"quantity\":" + qty
                + ",\"year\":2025,\"month\":" + month + "}";
    }

    private static ActivityDto dto() {
        return new ActivityDto(1L, 3L, "Pune Plant", Category.ELECTRICITY, Scope.SCOPE_2, new BigDecimal("1000"),
                "kWh", 2025, 5, new BigDecimal("710.000"), 11L, new BigDecimal("0.710000"), "CEA",
                RecordSource.MANUAL, Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void analystCanCreateAndCompanyAndUserComeFromToken() throws Exception {
        when(service.create(eq(42L), eq(7L), any(ActivityRequest.class))).thenReturn(dto());

        mvc.perform(post("/api/activities").header("Authorization", bearer(Role.ANALYST))
                .contentType(MediaType.APPLICATION_JSON).content(body("1000", "ELECTRICITY", 5)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.co2eKg").value(710.0))
            .andExpect(jsonPath("$.facilityName").value("Pune Plant"))
            .andExpect(jsonPath("$.factorSource").value("CEA"))
            .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void zeroOrNegativeQuantityIs400WithContractMessage() throws Exception {
        for (String qty : List.of("0", "-5")) {
            mvc.perform(post("/api/activities").header("Authorization", bearer(Role.ADMIN))
                    .contentType(MediaType.APPLICATION_JSON).content(body(qty, "ELECTRICITY", 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("quantity must be positive"))
                .andExpect(jsonPath("$.path").value("/api/activities"));
        }
        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void monthOutOfRangeIs400() throws Exception {
        mvc.perform(post("/api/activities").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(body("10", "DIESEL", 13)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("month must be between 1 and 12"));
    }

    @Test
    void unknownCategoryListsAllowedValues() throws Exception {
        mvc.perform(post("/api/activities").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(body("10", "COAL", 1)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                    org.hamcrest.Matchers.containsString("category must be one of [ELECTRICITY, DIESEL")));
    }

    @Test
    void clientCannotSupplyCompanyId() throws Exception {
        when(service.create(eq(42L), eq(7L), any(ActivityRequest.class))).thenReturn(dto());

        // An extra companyId in the body is ignored; the token's company (42) is what the service receives.
        mvc.perform(post("/api/activities").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":999,\"facilityId\":3,\"category\":\"ELECTRICITY\",\"quantity\":1,\"year\":2025,\"month\":1}"))
            .andExpect(status().isCreated());
        verify(service).create(eq(42L), eq(7L), any(ActivityRequest.class));
    }

    @Test
    void analystCannotDelete() throws Exception {
        mvc.perform(delete("/api/activities/1").header("Authorization", bearer(Role.ANALYST)))
            .andExpect(status().isForbidden());
        verify(service, never()).delete(any(), any());
    }

    @Test
    void adminDeleteReturns204() throws Exception {
        mvc.perform(delete("/api/activities/1").header("Authorization", bearer(Role.ADMIN)))
            .andExpect(status().isNoContent());
        verify(service).delete(42L, 1L);
    }

    @Test
    void listPassesFiltersAndDefaultsAndReturnsPagedShape() throws Exception {
        when(service.list(42L, 3L, Category.DIESEL, 2025, 0, 20))
            .thenReturn(new PageResponse<>(List.of(dto()), 0, 20, 1, 1));

        mvc.perform(get("/api/activities?facilityId=3&category=DIESEL&year=2025")
                .header("Authorization", bearer(Role.ANALYST)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void invalidCategoryFilterIs400() throws Exception {
        mvc.perform(get("/api/activities?category=COAL").header("Authorization", bearer(Role.ANALYST)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuth() throws Exception {
        mvc.perform(get("/api/activities")).andExpect(status().isUnauthorized());
    }
}
