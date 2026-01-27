package com.kiteclass.gateway.module.auth.controller;

import com.kiteclass.gateway.common.constant.MessageCodes;
import com.kiteclass.gateway.common.exception.BusinessException;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.LoginResponse;
import com.kiteclass.gateway.module.auth.dto.RefreshTokenRequest;
import com.kiteclass.gateway.module.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthController}.
 */
@WebFluxTest(AuthController.class)
@Import(com.kiteclass.gateway.module.user.controller.TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/login - Success")
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "Test@123");
        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .user(LoginResponse.UserInfo.builder()
                        .id(1L)
                        .email("test@example.com")
                        .name("Test User")
                        .roles(Arrays.asList("OWNER"))
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.accessToken").isEqualTo("access-token")
                .jsonPath("$.data.refreshToken").isEqualTo("refresh-token")
                .jsonPath("$.data.tokenType").isEqualTo("Bearer")
                .jsonPath("$.data.user.email").isEqualTo("test@example.com")
                .jsonPath("$.data.user.roles[0]").isEqualTo("OWNER");
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Invalid Credentials")
    void shouldReturnUnauthorizedForInvalidCredentials() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new BusinessException(
                        MessageCodes.AUTH_INVALID_CREDENTIALS,
                        HttpStatus.UNAUTHORIZED
                )));

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Validation Error")
    void shouldReturnBadRequestForValidationError() {
        // Given - invalid email format
        String invalidRequest = """
                {
                    "email": "invalid-email",
                    "password": "Test@123"
                }
                """;

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Missing Fields")
    void shouldReturnBadRequestForMissingFields() {
        // Given - missing password
        String invalidRequest = """
                {
                    "email": "test@example.com"
                }
                """;

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Success")
    void shouldRefreshTokenSuccessfully() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        LoginResponse response = LoginResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .user(LoginResponse.UserInfo.builder()
                        .id(1L)
                        .email("test@example.com")
                        .name("Test User")
                        .roles(Arrays.asList("OWNER"))
                        .build())
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(Mono.just(response));

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.accessToken").isEqualTo("new-access-token")
                .jsonPath("$.data.refreshToken").isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Invalid Token")
    void shouldReturnUnauthorizedForInvalidRefreshToken() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(Mono.error(new BusinessException(
                        MessageCodes.AUTH_REFRESH_TOKEN_INVALID,
                        HttpStatus.UNAUTHORIZED
                )));

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Success")
    void shouldLogoutSuccessfully() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

        when(authService.logout(any())).thenReturn(Mono.empty());

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Success")
    void shouldSendForgotPasswordEmail() {
        // Given
        String request = """
                {
                    "email": "test@example.com"
                }
                """;

        when(authService.forgotPassword(any())).thenReturn(Mono.empty());

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Success")
    void shouldResetPasswordSuccessfully() {
        // Given
        String request = """
                {
                    "token": "reset-token",
                    "newPassword": "NewPassword@123"
                }
                """;

        when(authService.resetPassword(any())).thenReturn(Mono.empty());

        // When/Then
        webTestClient.post()
                .uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").exists();
    }
}
