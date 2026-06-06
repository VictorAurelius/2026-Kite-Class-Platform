package com.kiteclass.core.module.auth.service;

import com.kiteclass.core.module.auth.entity.AuthCredential;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AuthTokenService} — HS512 access token minting
 * (Wave auth-1/auth-2, GAP-1010). Verifies the claim set the gateway forwards
 * (sub/role/email/tenantId/referenceId/type), the {@code jti} claim (GAP-1013e),
 * the TTL, and the ≥64-byte HS512 key fail-fast guard.
 */
@DisplayName("AuthTokenService — HS512 access token")
class AuthTokenServiceTest {

    /** 64-byte secret — minimum required for HS512. */
    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"; // gitleaks:allow

    private AuthTokenService tokenService;
    private final UUID userUuid = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private final UUID instanceId = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @BeforeEach
    void setUp() {
        tokenService = new AuthTokenService(SECRET, Duration.ofHours(12));
        tokenService.init();
    }

    private AuthCredential credential() {
        return AuthCredential.builder()
                .id(5L)
                .userUuid(userUuid)
                .entityType("TEACHER")
                .entityId(42L)
                .email("teacher@example.com")
                .passwordHash("irrelevant")
                .instanceId(instanceId)
                .enabled(true)
                .build();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Test
    @DisplayName("mints token carrying role/email/tenantId/referenceId/type + sub")
    void mintAccessToken_claimSet() {
        Claims claims = parse(tokenService.mintAccessToken(credential()));

        assertThat(claims.getSubject()).isEqualTo(userUuid.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("TEACHER");
        assertThat(claims.get("email", String.class)).isEqualTo("teacher@example.com");
        assertThat(claims.get("tenantId", String.class)).isEqualTo(instanceId.toString());
        assertThat(claims.get("referenceId", Number.class).longValue()).isEqualTo(42L);
        assertThat(claims.get("type", String.class)).isEqualTo("access");
    }

    @Test
    @DisplayName("includes a unique jti claim for revocation (GAP-1013e)")
    void mintAccessToken_includesJti() {
        Claims a = parse(tokenService.mintAccessToken(credential()));
        Claims b = parse(tokenService.mintAccessToken(credential()));

        assertThat(a.getId()).isNotBlank();
        assertThat(b.getId()).isNotBlank();
        assertThat(a.getId()).isNotEqualTo(b.getId()); // unique per token
    }

    @Test
    @DisplayName("TTL = configured 12h on both the claim and the accessor")
    void mintAccessToken_ttl() {
        Claims claims = parse(tokenService.mintAccessToken(credential()));

        long ttlMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(ttlMs).isEqualTo(Duration.ofHours(12).toMillis());
        assertThat(tokenService.accessTtlSeconds()).isEqualTo(43200L);
    }

    @Test
    @DisplayName("init() fails fast when secret < 64 bytes (HS512 requirement)")
    void init_rejectsShortSecret() {
        AuthTokenService weak = new AuthTokenService("too-short", Duration.ofHours(12));

        assertThatThrownBy(weak::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("64 bytes");
    }
}
