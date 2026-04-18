package com.kitehub.branding.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DistributedRateLimiter} — verifies Redis key formatting,
 * TTL, concurrency gate semantics, and graceful fallback when Redis is absent.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DistributedRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private DistributedRateLimiter limiter;
    private UUID instanceId;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        limiter = new DistributedRateLimiter(providerOf(redisTemplate));
        instanceId = UUID.randomUUID();
    }

    /** Minimal ObjectProvider stub — covers only the getIfAvailable() path the SUT calls. */
    private static <T> ObjectProvider<T> providerOf(T bean) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = org.mockito.Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(bean);
        return provider;
    }

    // --- Availability ---------------------------------------------------------

    @Test
    void isAvailable_withoutRedis_returnsFalse() {
        DistributedRateLimiter offline = new DistributedRateLimiter(providerOf(null));
        assertThat(offline.isAvailable()).isFalse();
        assertThat(offline.incrementDailyUsage(instanceId)).isEqualTo(-1L);
        assertThat(offline.getDailyUsage(instanceId)).isEqualTo(-1L);
        assertThat(offline.tryAcquireConcurrencySlot(instanceId, 5)).isFalse();
        assertThat(offline.getConcurrencyCount(instanceId)).isEqualTo(-1L);
        // Release is a no-op when Redis is unavailable — should not throw.
        offline.releaseConcurrencySlot(instanceId);
    }

    // --- Daily counter --------------------------------------------------------

    @Test
    void incrementDailyUsage_firstHit_setsTtl() {
        when(valueOps.increment(anyString())).thenReturn(1L);

        long count = limiter.incrementDailyUsage(instanceId);

        assertThat(count).isEqualTo(1L);
        verify(redisTemplate).expire(anyString(), eq(Duration.ofDays(1)));
    }

    @Test
    void incrementDailyUsage_subsequentHits_doesNotResetTtl() {
        when(valueOps.increment(anyString())).thenReturn(5L);

        long count = limiter.incrementDailyUsage(instanceId);

        assertThat(count).isEqualTo(5L);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void incrementDailyUsage_redisThrows_returnsMinusOne() {
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        long count = limiter.incrementDailyUsage(instanceId);

        assertThat(count).isEqualTo(-1L);
    }

    @Test
    void getDailyUsage_missingKey_returnsZero() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertThat(limiter.getDailyUsage(instanceId)).isZero();
    }

    @Test
    void getDailyUsage_returnsStoredValue() {
        when(valueOps.get(anyString())).thenReturn("42");

        assertThat(limiter.getDailyUsage(instanceId)).isEqualTo(42L);
    }

    // --- Concurrency ----------------------------------------------------------

    @Test
    void tryAcquireConcurrencySlot_underCap_returnsTrue() {
        when(valueOps.increment(anyString())).thenReturn(2L);

        boolean acquired = limiter.tryAcquireConcurrencySlot(instanceId, 5);

        assertThat(acquired).isTrue();
        // No rollback.
        verify(valueOps, never()).decrement(anyString());
    }

    @Test
    void tryAcquireConcurrencySlot_atCap_returnsTrue() {
        when(valueOps.increment(anyString())).thenReturn(5L);

        assertThat(limiter.tryAcquireConcurrencySlot(instanceId, 5)).isTrue();
        verify(valueOps, never()).decrement(anyString());
    }

    @Test
    void tryAcquireConcurrencySlot_overCap_rollsBack_returnsFalse() {
        when(valueOps.increment(anyString())).thenReturn(6L);

        boolean acquired = limiter.tryAcquireConcurrencySlot(instanceId, 5);

        assertThat(acquired).isFalse();
        verify(valueOps).decrement(anyString());
    }

    @Test
    void releaseConcurrencySlot_clampsAtZero_whenNegative() {
        when(valueOps.decrement(anyString())).thenReturn(-1L);

        limiter.releaseConcurrencySlot(instanceId);

        verify(valueOps).set(anyString(), eq("0"));
    }

    @Test
    void releaseConcurrencySlot_normal_doesNotClamp() {
        when(valueOps.decrement(anyString())).thenReturn(2L);

        limiter.releaseConcurrencySlot(instanceId);

        verify(valueOps, never()).set(anyString(), anyString());
    }

    // --- Key format -----------------------------------------------------------

    @Test
    void keyFormat_dailyContainsDate() {
        String key = DistributedRateLimiter.dailyKey(instanceId, java.time.LocalDate.of(2026, 4, 18));
        assertThat(key).isEqualTo("ai:ratelimit:" + instanceId + ":2026-04-18");
    }

    @Test
    void keyFormat_concurrencyContainsInstanceId() {
        String key = DistributedRateLimiter.concurrencyKey(instanceId);
        assertThat(key).isEqualTo("ai:concurrency:" + instanceId);
    }
}
