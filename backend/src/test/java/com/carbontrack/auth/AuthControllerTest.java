package com.carbontrack.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carbontrack.auth.dto.AuthResponse;
import com.carbontrack.auth.dto.RegisterRequest;
import com.carbontrack.auth.dto.UserDto;
import com.carbontrack.common.ConflictException;
import com.carbontrack.common.CurrentUser;
import com.carbontrack.common.JwtService;
import com.carbontrack.common.Role;
import com.carbontrack.common.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, CurrentUser.class,
        com.carbontrack.common.GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean AuthService service;

    private static final String VALID = """
            {"companyName":"Acme","industry":"Steel","name":"Asha","email":"a@b.com","password":"Secret@123"}""";

    @Test
    void registerReturnsTokenAndUser() throws Exception {
        UserDto user = new UserDto(1L, "Asha", "a@b.com", Role.ADMIN, 2L, "Acme", null);
        when(service.register(any(RegisterRequest.class))).thenReturn(new AuthResponse("tok", user));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.token").value("tok"))
            .andExpect(jsonPath("$.user.role").value("ADMIN"))
            .andExpect(jsonPath("$.user.companyId").value(2));
    }

    @Test
    void registerValidationErrorUsesContractBody() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyName\":\"\",\"name\":\"A\",\"email\":\"nope\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/auth/register"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(service.register(any(RegisterRequest.class))).thenThrow(new ConflictException("exists"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void meWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meWithBadTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer garbage"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidTokenReturnsUser() throws Exception {
        when(service.me(1L)).thenReturn(new UserDto(1L, "Asha", "a@b.com", Role.ADMIN, 2L, "Acme", 2023));
        String token = jwt.generate(1L, 2L, Role.ADMIN);

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("a@b.com"))
            .andExpect(jsonPath("$.baselineYear").value(2023));
    }
}
