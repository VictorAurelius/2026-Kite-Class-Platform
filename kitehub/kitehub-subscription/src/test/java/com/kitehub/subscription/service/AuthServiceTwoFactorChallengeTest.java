package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.auth.twofactor.ChallengeTokenService;
import com.kitehub.subscription.dto.LoginRequest;
import com.kitehub.subscription.dto.LoginResponse;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Verifies AuthService.login returns a challenge_token path when 2FA is
 * enrolled OR required for enrollment (GAP-516).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService — 2FA challenge_token path (GAP-516)")
class AuthServiceTwoFactorChallengeTest {

    @Mock InstanceRepository instanceRepository;
    @Mock InstanceService instanceService;
    @Mock UserRepository userRepository;
    @Mock CaptchaService captchaService;
    @Mock EmailSenderService emailSenderService;
    @Mock JwtKeyService jwtKeyService;
    @Mock ChallengeTokenService challengeTokenService;

    AuthService service;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    User user;

    private static final String PASSWORD = "CorrectHorseBatteryStaple-2026";

    @BeforeEach
    void setUp() {
        service = new AuthService(
            instanceRepository, instanceService, userRepository,
            captchaService, emailSenderService, jwtKeyService,
            null /* loginAuditService — not exercised here */,
            challengeTokenService
        );
        ReflectionTestUtils.setField(service, "jwtSecret", "x".repeat(32));

        SecretKey key = Keys.hmacShaKeyFor("x".repeat(64).getBytes(StandardCharsets.UTF_8));
        when(jwtKeyService.signingKey()).thenReturn(key);

        user = User.builder()
            .id(UUID.randomUUID())
            .email("admin@kitehub.me")
            .name("Admin")
            .passwordHash(encoder.encode(PASSWORD))
            .role("PLATFORM_ADMIN")
            .emailVerified(true)
            .build();

        when(userRepository.findByEmail("admin@kitehub.me")).thenReturn(Optional.of(user));
    }

    private static LoginRequest loginReq(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    @Test
    @DisplayName("totp_enrolled_at set → requires2fa + challenge_token")
    void enrolledUser_getsChallenge() {
        user.setTotpEnrolledAt(LocalDateTime.now());
        user.setTotpRequired(true);
        when(challengeTokenService.issue(eq(user.getId()),
            eq(ChallengeTokenService.Purpose.TWO_FACTOR_VERIFY))).thenReturn("CT-VERIFY");

        LoginResponse r = service.login(loginReq("admin@kitehub.me", PASSWORD));

        assertThat(r.getRequires2fa()).isTrue();
        assertThat(r.getChallengeToken()).isEqualTo("CT-VERIFY");
        assertThat(r.getAccessToken()).isNull();
        assertThat(r.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("totp_required=true but enrolled_at NULL → requires2fa_enrollment + challenge_token")
    void enrollRequired_getsEnrollChallenge() {
        user.setTotpRequired(true);
        user.setTotpEnrolledAt(null);
        when(challengeTokenService.issue(eq(user.getId()),
            eq(ChallengeTokenService.Purpose.TWO_FACTOR_ENROLL))).thenReturn("CT-ENROLL");

        LoginResponse r = service.login(loginReq("admin@kitehub.me", PASSWORD));

        assertThat(r.getRequires2faEnrollment()).isTrue();
        assertThat(r.getChallengeToken()).isEqualTo("CT-ENROLL");
        assertThat(r.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("totp_required=false + not enrolled → existing token-issuing path")
    void notRequired_getsTokens() {
        user.setTotpRequired(false);
        user.setTotpEnrolledAt(null);
        when(instanceService.getInstancesByOwner(any())).thenReturn(java.util.List.of());

        LoginResponse r = service.login(loginReq("admin@kitehub.me", PASSWORD));

        assertThat(r.getRequires2fa()).isNull();
        assertThat(r.getRequires2faEnrollment()).isNull();
        assertThat(r.getChallengeToken()).isNull();
        assertThat(r.getAccessToken()).isNotBlank();
        assertThat(r.getRefreshToken()).isNotBlank();
    }
}
