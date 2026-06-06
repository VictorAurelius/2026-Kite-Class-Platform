package com.kiteclass.core.module.auth.service;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.auth.dto.LoginRequest;
import com.kiteclass.core.module.auth.dto.LoginResponse;
import com.kiteclass.core.module.auth.entity.AuthCredential;
import com.kiteclass.core.module.auth.repository.AuthCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} — KC-native login (Wave auth-1/auth-2, GAP-1010).
 *
 * <p>Covers the happy path + the three uniform-401 failure modes (unknown email /
 * wrong password / disabled credential) + the email-masking helper (GAP-1013d).
 * Uses a REAL {@link BCryptPasswordEncoder} (the service constructs its own) so the
 * password verification branch is genuinely exercised.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — KC-native login")
class AuthServiceTest {

    @Mock
    private AuthCredentialRepository credentialRepository;

    @Mock
    private AuthTokenService tokenService;

    private AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UUID tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID userUuid = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final String GOOD_PASSWORD = "Password1!";

    @BeforeEach
    void setUp() {
        authService = new AuthService(credentialRepository, tokenService);
    }

    private AuthCredential credential(boolean enabled) {
        return AuthCredential.builder()
                .id(1L)
                .userUuid(userUuid)
                .entityType("PARENT")
                .entityId(7L)
                .email("parent@example.com")
                .passwordHash(encoder.encode(GOOD_PASSWORD))
                .instanceId(tenantId)
                .enabled(enabled)
                .build();
    }

    @Test
    @DisplayName("happy path → returns access token + identity claims")
    void login_success() {
        AuthCredential cred = credential(true);
        when(credentialRepository.findByEmailIgnoreCase("parent@example.com"))
                .thenReturn(Optional.of(cred));
        when(tokenService.mintAccessToken(cred)).thenReturn("signed.jwt.token");
        when(tokenService.accessTtlSeconds()).thenReturn(43200L);

        LoginResponse response = authService.login(new LoginRequest("parent@example.com", GOOD_PASSWORD));

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(43200L);
        assertThat(response.role()).isEqualTo("PARENT");
        assertThat(response.referenceId()).isEqualTo(7L);
        assertThat(response.tenantId()).isEqualTo(tenantId.toString());
    }

    @Test
    @DisplayName("unknown email → uniform 401, no token minted")
    void login_unknownEmail_uniform401() {
        when(credentialRepository.findByEmailIgnoreCase("ghost@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", GOOD_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS")
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(tokenService, never()).mintAccessToken(any());
    }

    @Test
    @DisplayName("wrong password → uniform 401, no token minted")
    void login_wrongPassword_uniform401() {
        when(credentialRepository.findByEmailIgnoreCase("parent@example.com"))
                .thenReturn(Optional.of(credential(true)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("parent@example.com", "WrongPass9#")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS")
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);

        verify(tokenService, never()).mintAccessToken(any());
    }

    @Test
    @DisplayName("disabled credential → uniform 401 even with correct password")
    void login_disabled_uniform401() {
        when(credentialRepository.findByEmailIgnoreCase("parent@example.com"))
                .thenReturn(Optional.of(credential(false)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("parent@example.com", GOOD_PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");

        verify(tokenService, never()).mintAccessToken(any());
    }

    @Test
    @DisplayName("maskEmail masks the local part (GAP-1013d)")
    void maskEmail_masksLocalPart() {
        assertThat(AuthService.maskEmail("nguyen@example.com")).isEqualTo("n***@example.com");
        assertThat(AuthService.maskEmail("a@b.vn")).isEqualTo("a***@b.vn");
        assertThat(AuthService.maskEmail("noat")).isEqualTo("n***");
        assertThat(AuthService.maskEmail("")).isEqualTo("***");
        assertThat(AuthService.maskEmail(null)).isEqualTo("***");
    }
}
