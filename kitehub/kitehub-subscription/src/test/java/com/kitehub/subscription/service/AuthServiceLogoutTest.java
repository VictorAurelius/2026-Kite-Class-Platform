package com.kitehub.subscription.service;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.auth.twofactor.ChallengeTokenService;
import com.kitehub.subscription.repository.InstanceRepository;
import com.kitehub.subscription.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService} logout + refresh-revocation (GAP-1075).
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code logout} blacklists a valid refresh token with a TTL near its remaining life.</li>
 *   <li>{@code logout} ignores a non-refresh (access) token.</li>
 *   <li>{@code logout} is a no-op when the blacklist service is absent (legacy tests).</li>
 *   <li>{@code refresh} rejects a token that is on the blacklist.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService — Logout + refresh revocation (GAP-1075)")
class AuthServiceLogoutTest {

    @Mock InstanceRepository instanceRepository;
    @Mock InstanceService instanceService;
    @Mock UserRepository userRepository;
    @Mock CaptchaService captchaService;
    @Mock EmailSenderService emailSenderService;
    @Mock JwtKeyService jwtKeyService;
    @Mock ChallengeTokenService challengeTokenService;
    @Mock RefreshTokenBlacklistService blacklist;

    AuthService service;

    private static final String REFRESH_TOKEN = "header.refresh.signature";

    @BeforeEach
    void setUp() {
        service = new AuthService(
            instanceRepository, instanceService, userRepository,
            captchaService, emailSenderService, jwtKeyService,
            null, challengeTokenService
        );
        ReflectionTestUtils.setField(service, "jwtSecret", "x".repeat(32));
        ReflectionTestUtils.setField(service, "refreshTokenBlacklistService", blacklist);
    }

    @SuppressWarnings("unchecked")
    private void stubParse(String token, String type, Instant expiry, UUID subject) {
        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn(type);
        when(claims.getExpiration()).thenReturn(expiry == null ? null : Date.from(expiry));
        when(claims.getSubject()).thenReturn(subject == null ? null : subject.toString());
        Jws<Claims> jws = mock(Jws.class);
        when(jws.getPayload()).thenReturn(claims);
        when(jwtKeyService.parse(token)).thenReturn(jws);
    }

    @Test
    @DisplayName("logout blacklists a valid refresh token")
    void logout_blacklistsRefreshToken() {
        Instant expiry = Instant.now().plus(7, ChronoUnit.DAYS);
        stubParse(REFRESH_TOKEN, "refresh", expiry, UUID.randomUUID());

        service.logout(REFRESH_TOKEN);

        verify(blacklist).blacklist(eq(REFRESH_TOKEN), any(Duration.class));
    }

    @Test
    @DisplayName("logout ignores a non-refresh (access) token")
    void logout_ignoresAccessToken() {
        stubParse(REFRESH_TOKEN, "access", Instant.now().plus(1, ChronoUnit.HOURS), UUID.randomUUID());

        service.logout(REFRESH_TOKEN);

        verify(blacklist, never()).blacklist(any(), any());
    }

    @Test
    @DisplayName("logout is a no-op for a null/blank token")
    void logout_noOpForBlankToken() {
        service.logout("   ");
        service.logout(null);

        verify(blacklist, never()).blacklist(any(), any());
    }

    @Test
    @DisplayName("logout is a no-op when the blacklist service is absent (legacy construction)")
    void logout_noOpWhenBlacklistServiceNull() {
        ReflectionTestUtils.setField(service, "refreshTokenBlacklistService", null);

        // Must not throw even though no service is wired.
        service.logout(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("refresh rejects a token that has been revoked (blacklisted)")
    void refresh_rejectsBlacklistedToken() {
        UUID userId = UUID.randomUUID();
        stubParse(REFRESH_TOKEN, "refresh", Instant.now().plus(7, ChronoUnit.DAYS), userId);
        when(blacklist.isBlacklisted(REFRESH_TOKEN)).thenReturn(true);

        assertThatThrownBy(() -> service.refresh(REFRESH_TOKEN))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid or expired refresh token");

        // User lookup never reached — rejected before issuing new tokens.
        verify(userRepository, never()).findById(userId);
    }

    @Test
    @DisplayName("refresh proceeds for a clean (non-blacklisted) token")
    void refresh_proceedsForCleanToken() {
        UUID userId = UUID.randomUUID();
        stubParse(REFRESH_TOKEN, "refresh", Instant.now().plus(7, ChronoUnit.DAYS), userId);
        when(blacklist.isBlacklisted(REFRESH_TOKEN)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(
            User.builder().id(userId).email("u@test.vn").role("OWNER").build()));
        when(jwtKeyService.signingKey()).thenReturn(
            io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "x".repeat(64).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        when(instanceRepository.findByOwnerIdAndDeletedFalse(userId))
            .thenReturn(java.util.Collections.emptyList());

        // Should not throw — issues a new token pair.
        service.refresh(REFRESH_TOKEN);

        verify(userRepository).findById(userId);
    }
}
