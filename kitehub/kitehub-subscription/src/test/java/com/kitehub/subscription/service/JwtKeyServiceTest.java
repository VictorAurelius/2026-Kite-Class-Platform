package com.kitehub.subscription.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link JwtKeyService} dual-key rotation (GAP-520).
 *
 * @since 1.0.0 (Wave 72a GAP-520)
 */
@DisplayName("JwtKeyService — Dual-Key Rotation (GAP-520)")
class JwtKeyServiceTest {

    private static final String CURRENT = "current-secret-at-least-32-chars-long-xxxxx";
    private static final String PREVIOUS = "previous-secret-at-least-32-chars-long-yyyy";
    private static final String OTHER = "totally-different-key-not-matching-either-zzzz";

    @Test
    @DisplayName("missing current secret → IllegalStateException at boot")
    void missingCurrentSecretFails() {
        assertThatIllegalStateException()
            .isThrownBy(() -> new JwtKeyService("", "", new SimpleMeterRegistry()));
    }

    @Test
    @DisplayName("short current secret → IllegalStateException at boot")
    void shortCurrentSecretFails() {
        assertThatIllegalStateException()
            .isThrownBy(() -> new JwtKeyService("too-short", "", new SimpleMeterRegistry()));
    }

    @Test
    @DisplayName("short previous secret → IllegalStateException at boot")
    void shortPreviousSecretFails() {
        assertThatIllegalStateException()
            .isThrownBy(() -> new JwtKeyService(CURRENT, "too-short", new SimpleMeterRegistry()));
    }

    @Test
    @DisplayName("token signed with current key validates")
    void currentKeyValidates() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        JwtKeyService svc = new JwtKeyService(CURRENT, "", reg);

        String token = signWith(CURRENT);
        var claims = svc.parse(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("user-1");
        // Counter is registered eagerly; verify it has not incremented for the
        // current-key happy path.
        assertThat(reg.get("jwt.verify.fallback").counter().count()).isZero();
    }

    @Test
    @DisplayName("token signed with previous key validates + emits fallback metric")
    void previousKeyFallbackValidates() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        JwtKeyService svc = new JwtKeyService(CURRENT, PREVIOUS, reg);

        String token = signWith(PREVIOUS);
        var claims = svc.parse(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(reg.get("jwt.verify.fallback").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("token signed with unknown key fails when previous configured")
    void unknownKeyFailsWithPreviousConfigured() {
        JwtKeyService svc = new JwtKeyService(CURRENT, PREVIOUS, new SimpleMeterRegistry());
        String token = signWith(OTHER);

        assertThatExceptionOfType(JwtException.class)
            .isThrownBy(() -> svc.parse(token));
    }

    @Test
    @DisplayName("token signed with unknown key fails when no previous configured")
    void unknownKeyFailsWithoutPrevious() {
        JwtKeyService svc = new JwtKeyService(CURRENT, "", new SimpleMeterRegistry());
        String token = signWith(OTHER);

        assertThatExceptionOfType(JwtException.class)
            .isThrownBy(() -> svc.parse(token));
    }

    @Test
    @DisplayName("signingKey() always returns the current key")
    void signingKeyIsCurrent() {
        JwtKeyService svc = new JwtKeyService(CURRENT, PREVIOUS, new SimpleMeterRegistry());
        // Sign with the returned key; current-key path verifies without fallback.
        String token = Jwts.builder()
            .subject("user-1")
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(svc.signingKey())
            .compact();
        assertThat(svc.parse(token).getPayload().getSubject()).isEqualTo("user-1");
    }

    private static String signWith(String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .subject("user-1")
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(key)
            .compact();
    }
}
