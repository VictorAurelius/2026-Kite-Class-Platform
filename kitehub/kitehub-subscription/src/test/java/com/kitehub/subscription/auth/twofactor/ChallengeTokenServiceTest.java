package com.kitehub.subscription.auth.twofactor;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link ChallengeTokenService} (GAP-516).
 */
@DisplayName("ChallengeTokenService — issue + verify")
class ChallengeTokenServiceTest {

    private final ChallengeTokenService svc =
        new ChallengeTokenService("test-challenge-secret-32-chars-aaaaaa");

    @Test
    @DisplayName("issue + verify happy path returns same userId + purpose")
    void roundTrip() {
        UUID userId = UUID.randomUUID();
        String tok = svc.issue(userId, ChallengeTokenService.Purpose.TWO_FACTOR_VERIFY);
        ChallengeTokenService.Verified verified = svc.verify(tok);
        assertThat(verified.getUserId()).isEqualTo(userId);
        assertThat(verified.getPurpose())
            .isEqualTo(ChallengeTokenService.Purpose.TWO_FACTOR_VERIFY);
    }

    @Test
    @DisplayName("verify rejects garbage with INVALID reason")
    void garbageInvalid() {
        assertThatExceptionOfType(ChallengeTokenService.ChallengeTokenException.class)
            .isThrownBy(() -> svc.verify("not-a-jwt"))
            .matches(ex -> ex.getReason() == ChallengeTokenService.FailureReason.INVALID);
    }

    @Test
    @DisplayName("verify rejects token signed by a different key as INVALID")
    void wrongSignature() {
        // Hand-build an expired token with a different secret.
        SecretKey wrong = Keys.hmacShaKeyFor(
            "completely-different-secret-32by".getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String bad = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("type", "challenge")
            .claim("purpose", "TWO_FACTOR_VERIFY")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
            .signWith(wrong)
            .compact();

        assertThatExceptionOfType(ChallengeTokenService.ChallengeTokenException.class)
            .isThrownBy(() -> svc.verify(bad))
            .matches(ex -> ex.getReason() == ChallengeTokenService.FailureReason.INVALID);
    }

    @Test
    @DisplayName("verify rejects token with wrong purpose marker as INVALID")
    void wrongType() {
        // Hand-build a JWT signed with the SAME secret but type != "challenge".
        SecretKey same = Keys.hmacShaKeyFor(
            "test-challenge-secret-32-chars-aaaaaa".getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String wrongType = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("type", "access") // wrong on purpose
            .claim("purpose", "TWO_FACTOR_VERIFY")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
            .signWith(same)
            .compact();
        assertThatExceptionOfType(ChallengeTokenService.ChallengeTokenException.class)
            .isThrownBy(() -> svc.verify(wrongType));
    }
}
