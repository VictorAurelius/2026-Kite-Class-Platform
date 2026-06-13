package com.kitehub.subscription.service;

import com.kitehub.subscription.service.SsoCodeService.SsoPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SsoCodeService} (GAP-1138, ADR-040 Option A).
 *
 * <p>Covers the security-critical contract: single-use consume (Redis GETDEL),
 * TTL clamp ≤60s, high-entropy code, identity round-trip, and rejection of
 * blank / missing / malformed codes.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SsoCodeService — one-time SSO exchange code (GAP-1138)")
class SsoCodeServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    private static final String SEP = "\u001f";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private SsoCodeService service(long ttlSeconds) {
        return new SsoCodeService(redis, ttlSeconds);
    }

    @Test
    @DisplayName("issueCode stores userId|email|role under prefixed key with the configured TTL")
    void issueCode_storesIdentityTupleWithTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);
        SsoCodeService svc = service(60);

        String code = svc.issueCode(USER_ID, "owner@test.vn", "OWNER");

        assertThat(code).isNotBlank();
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCap = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(keyCap.capture(), valCap.capture(), ttlCap.capture());

        assertThat(keyCap.getValue())
            .startsWith(SsoCodeService.KEY_PREFIX)
            .endsWith(code);
        assertThat(valCap.getValue()).isEqualTo(USER_ID + SEP + "owner@test.vn" + SEP + "OWNER");
        assertThat(ttlCap.getValue()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("issueCode generates a high-entropy URL-safe code (no padding, ≥32 chars)")
    void issueCode_generatesUrlSafeHighEntropyCode() {
        when(redis.opsForValue()).thenReturn(valueOps);
        SsoCodeService svc = service(60);

        String a = svc.issueCode(USER_ID, "a@test.vn", "OWNER");
        String b = svc.issueCode(USER_ID, "a@test.vn", "OWNER");

        // 256-bit base64url → 43 chars, URL-safe alphabet only, no '=' padding.
        assertThat(a).hasSizeGreaterThanOrEqualTo(32).matches("[A-Za-z0-9_-]+");
        assertThat(a).isNotEqualTo(b); // unique per issue
    }

    @Test
    @DisplayName("TTL is clamped to ≤60s per ADR-040 even when configured higher")
    void ttl_clampedToMax60() {
        when(redis.opsForValue()).thenReturn(valueOps);
        SsoCodeService svc = service(120);

        assertThat(svc.ttlSeconds()).isEqualTo(60);
        svc.issueCode(USER_ID, "a@test.vn", "OWNER");
        ArgumentCaptor<Duration> ttlCap = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(anyString(), anyString(), ttlCap.capture());
        assertThat(ttlCap.getValue()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("TTL below the cap is honored as-is")
    void ttl_belowCapHonored() {
        SsoCodeService svc = service(30);
        assertThat(svc.ttlSeconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("TTL non-positive is clamped to a 1s positive minimum")
    void ttl_nonPositiveClampedToOne() {
        SsoCodeService svc = service(0);
        assertThat(svc.ttlSeconds()).isEqualTo(1);
    }

    @Test
    @DisplayName("consumeCode uses GETDEL (single-use) and round-trips the identity tuple")
    void consumeCode_singleUseRoundTrip() {
        when(redis.opsForValue()).thenReturn(valueOps);
        SsoCodeService svc = service(60);
        String value = USER_ID + SEP + "owner@test.vn" + SEP + "OWNER";
        when(valueOps.getAndDelete(SsoCodeService.KEY_PREFIX + "good-code")).thenReturn(value);

        Optional<SsoPrincipal> principal = svc.consumeCode("good-code");

        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo(USER_ID);
        assertThat(principal.get().email()).isEqualTo("owner@test.vn");
        assertThat(principal.get().role()).isEqualTo("OWNER");
        // GETDEL deletes on first read — proving single-use atomicity.
        verify(valueOps).getAndDelete(SsoCodeService.KEY_PREFIX + "good-code");
    }

    @Test
    @DisplayName("consumeCode returns empty when the code is absent (expired / already used / replayed)")
    void consumeCode_emptyWhenAbsent() {
        when(redis.opsForValue()).thenReturn(valueOps);
        SsoCodeService svc = service(60);
        when(valueOps.getAndDelete(anyString())).thenReturn(null);

        assertThat(svc.consumeCode("replayed-or-expired")).isEmpty();
    }

    @Test
    @DisplayName("consumeCode returns empty for a null or blank code (no Redis call)")
    void consumeCode_emptyForBlankCode() {
        SsoCodeService svc = service(60);

        assertThat(svc.consumeCode(null)).isEmpty();
        assertThat(svc.consumeCode("  ")).isEmpty();
        // No Redis interaction for a blank code.
        org.mockito.Mockito.verifyNoInteractions(redis);
    }

    @Test
    @DisplayName("consumeCode returns empty for a malformed stored payload")
    void consumeCode_emptyForMalformedValue() {
        when(redis.opsForValue()).thenReturn(valueOps);
        SsoCodeService svc = service(60);
        when(valueOps.getAndDelete(anyString())).thenReturn("not-a-valid-tuple");

        assertThat(svc.consumeCode("corrupt")).isEmpty();
    }

    @Test
    @DisplayName("issueCode with null email/role stores empty segments and round-trips back to null")
    void issueCode_nullEmailRole_roundTripsToNull() {
        when(redis.opsForValue()).thenReturn(valueOps);
        SsoCodeService svc = service(60);

        svc.issueCode(USER_ID, null, null);
        ArgumentCaptor<String> valCap = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(anyString(), valCap.capture(), eq(Duration.ofSeconds(60)));
        assertThat(valCap.getValue()).isEqualTo(USER_ID + SEP + "" + SEP + "");

        // Feed the captured value back through consume to verify empty→null.
        lenient().when(valueOps.getAndDelete(anyString())).thenReturn(valCap.getValue());
        Optional<SsoPrincipal> principal = svc.consumeCode("x");
        assertThat(principal).isPresent();
        assertThat(principal.get().email()).isNull();
        assertThat(principal.get().role()).isNull();
    }
}
