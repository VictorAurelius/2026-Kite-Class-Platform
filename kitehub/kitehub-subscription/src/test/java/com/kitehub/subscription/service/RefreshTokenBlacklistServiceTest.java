package com.kitehub.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RefreshTokenBlacklistService} (GAP-1075).
 *
 * <p>Covers: hash-keyed write with TTL, presence read, non-positive TTL clamp, null/blank
 * guards, and the fail-open contract (a Redis exception never propagates).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenBlacklistService — refresh-token revocation (GAP-1075)")
class RefreshTokenBlacklistServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    RefreshTokenBlacklistService service;

    private static final String TOKEN = "header.payload.signature";

    @BeforeEach
    void setUp() {
        service = new RefreshTokenBlacklistService(redis);
    }

    @Test
    @DisplayName("blacklist stores a prefixed hash key with the supplied TTL")
    void blacklist_writesHashKeyWithTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);
        Duration ttl = Duration.ofHours(2);

        service.blacklist(TOKEN, ttl);

        // Key is prefixed + hashed (raw token never stored), value "1", TTL passed through.
        verify(valueOps).set(startsWith(RefreshTokenBlacklistService.KEY_PREFIX), eq("1"), eq(ttl));
    }

    @Test
    @DisplayName("blacklist clamps a non-positive TTL to a small positive minimum")
    void blacklist_clampsNonPositiveTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);

        service.blacklist(TOKEN, Duration.ofSeconds(-5));

        verify(valueOps).set(anyString(), eq("1"), eq(Duration.ofSeconds(1)));
    }

    @Test
    @DisplayName("blacklist is a no-op for a null or blank token")
    void blacklist_noOpForBlankToken() {
        service.blacklist("  ", Duration.ofHours(1));
        service.blacklist(null, Duration.ofHours(1));

        verify(redis, never()).opsForValue();
    }

    @Test
    @DisplayName("isBlacklisted returns true when the hash key exists")
    void isBlacklisted_trueWhenKeyPresent() {
        when(redis.hasKey(startsWith(RefreshTokenBlacklistService.KEY_PREFIX))).thenReturn(true);

        assertThat(service.isBlacklisted(TOKEN)).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted returns false when the key is absent")
    void isBlacklisted_falseWhenKeyAbsent() {
        when(redis.hasKey(anyString())).thenReturn(false);

        assertThat(service.isBlacklisted(TOKEN)).isFalse();
    }

    @Test
    @DisplayName("isBlacklisted fails open (returns false) on a Redis error")
    void isBlacklisted_failsOpenOnRedisError() {
        when(redis.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));

        assertThat(service.isBlacklisted(TOKEN)).isFalse();
    }

    @Test
    @DisplayName("blacklist fails open (swallows) on a Redis error")
    void blacklist_failsOpenOnRedisError() {
        when(redis.opsForValue()).thenReturn(valueOps);
        // set throws — must not propagate out of blacklist()
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
            .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        // No exception expected.
        service.blacklist(TOKEN, Duration.ofHours(1));
    }

    @Test
    @DisplayName("every lookup queries a prefixed hash key (raw token never used as key)")
    void lookup_alwaysUsesPrefixedHashKey() {
        when(redis.hasKey(anyString())).thenReturn(false);

        service.isBlacklisted(TOKEN);
        service.isBlacklisted("other.token.value");

        verify(redis, org.mockito.Mockito.times(2))
            .hasKey(startsWith(RefreshTokenBlacklistService.KEY_PREFIX));
    }
}
