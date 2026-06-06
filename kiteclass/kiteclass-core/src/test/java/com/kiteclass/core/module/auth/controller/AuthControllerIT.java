package com.kiteclass.core.module.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.module.auth.dto.LoginRequest;
import com.kiteclass.core.module.auth.dto.LoginResponse;
import com.kiteclass.core.module.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC tests for {@link AuthController} — {@code POST /api/v1/tenant-auth/login}
 * (Wave auth-1/auth-2, GAP-1010). Verifies HTTP 200 happy path, uniform 401 on
 * bad credentials, and 400 on bean-validation failure.
 */
@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController — tenant-auth login")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AuthService authService() {
            return mock(AuthService.class);
        }
    }

    @Test
    @DisplayName("valid credentials → 200 + access token in body")
    void login_success_returns200() throws Exception {
        LoginResponse response = new LoginResponse(
                "signed.jwt.token", "Bearer", 43200L, "PARENT", 7L,
                "11111111-1111-1111-1111-111111111111");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tenant-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("parent@example.com", "Password1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.role").value("PARENT"))
                .andExpect(jsonPath("$.data.referenceId").value(7));
    }

    @Test
    @DisplayName("invalid credentials → uniform 401 INVALID_CREDENTIALS")
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/tenant-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("parent@example.com", "WrongPass9#"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("blank email → 400 bean-validation failure")
    void login_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tenant-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("", "Password1!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("malformed email → 400 bean-validation failure")
    void login_malformedEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tenant-auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("not-an-email", "Password1!"))))
                .andExpect(status().isBadRequest());
    }
}
