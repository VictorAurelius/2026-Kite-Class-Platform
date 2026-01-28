package com.kiteclass.gateway.module.auth.service;

import com.kiteclass.gateway.common.constant.MessageCodes;
import com.kiteclass.gateway.common.constant.UserStatus;
import com.kiteclass.gateway.common.constant.UserType;
import com.kiteclass.gateway.common.exception.BusinessException;
import com.kiteclass.gateway.config.EmailProperties;
import com.kiteclass.gateway.module.auth.dto.LoginRequest;
import com.kiteclass.gateway.module.auth.dto.RefreshTokenRequest;
import com.kiteclass.gateway.module.auth.entity.RefreshToken;
import com.kiteclass.gateway.module.auth.repository.PasswordResetTokenRepository;
import com.kiteclass.gateway.module.auth.repository.RefreshTokenRepository;
import com.kiteclass.gateway.module.auth.service.impl.AuthServiceImpl;
import com.kiteclass.gateway.module.user.entity.Role;
import com.kiteclass.gateway.module.user.entity.User;
import com.kiteclass.gateway.module.user.repository.UserRepository;
import com.kiteclass.gateway.module.user.repository.UserRoleRepository;
import com.kiteclass.gateway.security.jwt.JwtProperties;
import com.kiteclass.gateway.security.jwt.JwtTokenProvider;
import com.kiteclass.gateway.service.EmailService;
import com.kiteclass.gateway.service.ProfileFetcher;
import com.kiteclass.gateway.service.dto.StudentProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link AuthServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private EmailProperties emailProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private ProfileFetcher profileFetcher;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role testRole;
    private String testEmail = "test@example.com";
    private String testPassword = "Test@123";
    private String encodedPassword = "$2a$10$test";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email(testEmail)
                .passwordHash(encodedPassword)
                .name("Test User")
                .status(UserStatus.ACTIVE)
                .deleted(false)
                .failedLoginAttempts(0)
                .build();

        testRole = Role.builder()
                .id(1L)
                .code("OWNER")
                .name("Owner")
                .build();
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest request = new LoginRequest(testEmail, testPassword);
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        when(userRepository.findByEmailAndDeletedFalse(testEmail)).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));
        when(userRoleRepository.findRolesByUserId(1L)).thenReturn(Flux.just(testRole));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), any())).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn(refreshToken);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(profileFetcher.fetchProfile(any(UserType.class), any())).thenReturn(null);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(Mono.just(new RefreshToken()));

        // When/Then
        StepVerifier.create(authService.login(request))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.accessToken()).isEqualTo(accessToken);
                    assertThat(response.refreshToken()).isEqualTo(refreshToken);
                    assertThat(response.tokenType()).isEqualTo("Bearer");
                    assertThat(response.user().email()).isEqualTo(testEmail);
                    assertThat(response.user().roles()).contains("OWNER");
                })
                .verifyComplete();

        // Verify user's last login was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getLastLoginAt()).isNotNull();
        assertThat(userCaptor.getValue().getFailedLoginAttempts()).isZero();
    }

    @Test
    @DisplayName("Should login successfully with student profile from Core service")
    void shouldLoginSuccessfullyWithStudentProfile() {
        // Given
        testUser.setUserType(UserType.STUDENT);
        testUser.setReferenceId(100L);

        LoginRequest request = new LoginRequest(testEmail, testPassword);
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        StudentProfileResponse studentProfile = new StudentProfileResponse(
                100L,
                "Test Student",
                "student@example.com",
                "0123456789",
                LocalDate.of(2010, 5, 15),
                "MALE",
                "https://example.com/avatar.jpg",
                "ACTIVE"
        );

        when(userRepository.findByEmailAndDeletedFalse(testEmail)).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));
        when(userRoleRepository.findRolesByUserId(1L)).thenReturn(Flux.just(testRole));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), any())).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn(refreshToken);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(profileFetcher.fetchProfile(eq(UserType.STUDENT), eq(100L))).thenReturn(studentProfile);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(Mono.just(new RefreshToken()));

        // When/Then
        StepVerifier.create(authService.login(request))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.accessToken()).isEqualTo(accessToken);
                    assertThat(response.refreshToken()).isEqualTo(refreshToken);
                    assertThat(response.tokenType()).isEqualTo("Bearer");
                    assertThat(response.user().email()).isEqualTo(testEmail);
                    assertThat(response.user().roles()).contains("OWNER");

                    // Verify profile is included
                    assertThat(response.user().profile()).isNotNull();
                    assertThat(response.user().profile()).isInstanceOf(StudentProfileResponse.class);
                    StudentProfileResponse profile = (StudentProfileResponse) response.user().profile();
                    assertThat(profile.id()).isEqualTo(100L);
                    assertThat(profile.name()).isEqualTo("Test Student");
                    assertThat(profile.email()).isEqualTo("student@example.com");
                })
                .verifyComplete();

        verify(profileFetcher).fetchProfile(UserType.STUDENT, 100L);
    }

    @Test
    @DisplayName("Should login successfully for ADMIN with null profile")
    void shouldLoginSuccessfullyForAdminWithNullProfile() {
        // Given
        testUser.setUserType(UserType.ADMIN);
        testUser.setReferenceId(null);

        LoginRequest request = new LoginRequest(testEmail, testPassword);
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        when(userRepository.findByEmailAndDeletedFalse(testEmail)).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));
        when(userRoleRepository.findRolesByUserId(1L)).thenReturn(Flux.just(testRole));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), any())).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn(refreshToken);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(profileFetcher.fetchProfile(eq(UserType.ADMIN), eq(null))).thenReturn(null);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(Mono.just(new RefreshToken()));

        // When/Then
        StepVerifier.create(authService.login(request))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.user().profile()).isNull(); // ADMIN has no profile
                })
                .verifyComplete();

        verify(profileFetcher).fetchProfile(UserType.ADMIN, null);
    }

    @Test
    @DisplayName("Should fail login with invalid credentials")
    void shouldFailLoginWithInvalidCredentials() {
        // Given
        LoginRequest request = new LoginRequest(testEmail, "WrongPassword");

        when(userRepository.findByEmailAndDeletedFalse(testEmail)).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches("WrongPassword", encodedPassword)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        // When/Then
        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof BusinessException &&
                        ((BusinessException) error).getCode().equals(MessageCodes.AUTH_INVALID_CREDENTIALS)
                )
                .verify();

        // Verify failed attempts incremented
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should lock account after max failed attempts")
    void shouldLockAccountAfterMaxFailedAttempts() {
        // Given
        testUser.setFailedLoginAttempts(4); // One more attempt will lock
        LoginRequest request = new LoginRequest(testEmail, "WrongPassword");

        when(userRepository.findByEmailAndDeletedFalse(testEmail)).thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches("WrongPassword", encodedPassword)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(testUser));

        // When/Then
        StepVerifier.create(authService.login(request))
                .expectError(BusinessException.class)
                .verify();

        // Verify account was locked
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getFailedLoginAttempts()).isEqualTo(5);
        assertThat(userCaptor.getValue().getLockedUntil()).isNotNull();
        assertThat(userCaptor.getValue().getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Should reject login for locked account")
    void shouldRejectLoginForLockedAccount() {
        // Given
        testUser.setLockedUntil(Instant.now().plusSeconds(1800)); // Locked for 30 minutes
        LoginRequest request = new LoginRequest(testEmail, testPassword);

        when(userRepository.findByEmailAndDeletedFalse(testEmail)).thenReturn(Mono.just(testUser));

        // When/Then
        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof BusinessException &&
                        ((BusinessException) error).getCode().equals(MessageCodes.AUTH_ACCOUNT_LOCKED)
                )
                .verify();

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Should reject login for inactive account")
    void shouldRejectLoginForInactiveAccount() {
        // Given
        testUser.setStatus(UserStatus.INACTIVE);
        LoginRequest request = new LoginRequest(testEmail, testPassword);

        when(userRepository.findByEmailAndDeletedFalse(testEmail)).thenReturn(Mono.just(testUser));

        // When/Then
        StepVerifier.create(authService.login(request))
                .expectErrorMatches(error ->
                        error instanceof BusinessException &&
                        ((BusinessException) error).getCode().equals(MessageCodes.AUTH_ACCOUNT_INACTIVE)
                )
                .verify();

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Given
        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        RefreshToken tokenEntity = RefreshToken.builder()
                .id(1L)
                .token(oldRefreshToken)
                .userId(1L)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest(oldRefreshToken);

        when(refreshTokenRepository.findByToken(oldRefreshToken)).thenReturn(Mono.just(tokenEntity));
        when(userRepository.findById(1L)).thenReturn(Mono.just(testUser));
        when(refreshTokenRepository.delete(tokenEntity)).thenReturn(Mono.empty());
        when(userRoleRepository.findRolesByUserId(1L)).thenReturn(Flux.just(testRole));
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), any())).thenReturn(newAccessToken);
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn(newRefreshToken);
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(profileFetcher.fetchProfile(any(UserType.class), any())).thenReturn(null);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(Mono.just(new RefreshToken()));

        // When/Then
        StepVerifier.create(authService.refreshToken(request))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.accessToken()).isEqualTo(newAccessToken);
                    assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
                })
                .verifyComplete();

        verify(refreshTokenRepository).delete(tokenEntity);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void shouldRejectExpiredRefreshToken() {
        // Given
        String expiredRefreshToken = "expired-token";

        RefreshToken tokenEntity = RefreshToken.builder()
                .id(1L)
                .token(expiredRefreshToken)
                .userId(1L)
                .expiresAt(Instant.now().minusSeconds(3600)) // Expired 1 hour ago
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest(expiredRefreshToken);

        when(refreshTokenRepository.findByToken(expiredRefreshToken)).thenReturn(Mono.just(tokenEntity));
        when(refreshTokenRepository.delete(tokenEntity)).thenReturn(Mono.empty());

        // When/Then
        StepVerifier.create(authService.refreshToken(request))
                .expectErrorMatches(error ->
                        error instanceof BusinessException &&
                        ((BusinessException) error).getCode().equals(MessageCodes.AUTH_REFRESH_TOKEN_EXPIRED)
                )
                .verify();

        verify(refreshTokenRepository).delete(tokenEntity);
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() {
        // Given
        String refreshToken = "refresh-token";
        RefreshToken tokenEntity = RefreshToken.builder()
                .id(1L)
                .token(refreshToken)
                .userId(1L)
                .build();

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Mono.just(tokenEntity));
        when(refreshTokenRepository.delete(tokenEntity)).thenReturn(Mono.empty());

        // When/Then
        StepVerifier.create(authService.logout(refreshToken))
                .verifyComplete();

        verify(refreshTokenRepository).delete(tokenEntity);
    }

    @Test
    @DisplayName("Should handle logout with non-existent token gracefully")
    void shouldHandleLogoutWithNonExistentToken() {
        // Given
        String nonExistentToken = "non-existent-token";

        when(refreshTokenRepository.findByToken(nonExistentToken)).thenReturn(Mono.empty());

        // When/Then
        StepVerifier.create(authService.logout(nonExistentToken))
                .verifyComplete();

        verify(refreshTokenRepository, never()).delete(any());
    }
}
