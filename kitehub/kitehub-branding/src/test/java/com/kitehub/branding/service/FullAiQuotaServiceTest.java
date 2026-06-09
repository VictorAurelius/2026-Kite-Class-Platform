package com.kitehub.branding.service;

import com.kitehub.branding.config.AIRateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GAP-1119 — FULL_AI monthly cost quota gate:
 * ENTERPRISE unlimited / PREMIUM limited / FREE+BASIC ineligible.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FullAiQuotaService (GAP-1119 FULL_AI cost quota)")
class FullAiQuotaServiceTest {

    @Mock private DistributedRateLimiter distributedRateLimiter;

    private final AIRateLimitConfig config = new AIRateLimitConfig(); // defaults: premium=5, enterprise=-1
    private FullAiQuotaService service;
    private UUID instanceId;

    @BeforeEach
    void setUp() {
        service = new FullAiQuotaService(config, distributedRateLimiter);
        instanceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("ENTERPRISE → always allowed (unlimited), counter never read")
    void enterpriseAlwaysAllowed() {
        assertThat(service.canUseFullAi(instanceId, "ENTERPRISE")).isTrue();
        verify(distributedRateLimiter, never()).getMonthlyFullAiUsage(instanceId);
    }

    @Test
    @DisplayName("PREMIUM under monthly quota → allowed")
    void premiumUnderQuotaAllowed() {
        when(distributedRateLimiter.getMonthlyFullAiUsage(instanceId)).thenReturn(4L); // quota 5
        assertThat(service.canUseFullAi(instanceId, "PREMIUM")).isTrue();
    }

    @Test
    @DisplayName("PREMIUM at monthly quota → rejected")
    void premiumAtQuotaRejected() {
        when(distributedRateLimiter.getMonthlyFullAiUsage(instanceId)).thenReturn(5L); // quota 5
        assertThat(service.canUseFullAi(instanceId, "PREMIUM")).isFalse();
    }

    @Test
    @DisplayName("PREMIUM with Redis unavailable (-1) → fail open (allowed)")
    void premiumRedisUnavailableFailsOpen() {
        when(distributedRateLimiter.getMonthlyFullAiUsage(instanceId)).thenReturn(-1L);
        assertThat(service.canUseFullAi(instanceId, "PREMIUM")).isTrue();
    }

    @Test
    @DisplayName("FREE / BASIC / null → not FULL_AI-eligible (rejected)")
    void ineligibleTiersRejected() {
        assertThat(service.canUseFullAi(instanceId, "FREE")).isFalse();
        assertThat(service.canUseFullAi(instanceId, "BASIC")).isFalse();
        assertThat(service.canUseFullAi(instanceId, null)).isFalse();
        verify(distributedRateLimiter, never()).getMonthlyFullAiUsage(instanceId);
    }

    @Test
    @DisplayName("recordFullAiUsage: PREMIUM increments; ENTERPRISE + FREE are no-ops")
    void recordOnlyForFiniteQuota() {
        service.recordFullAiUsage(instanceId, "PREMIUM");
        verify(distributedRateLimiter).incrementMonthlyFullAiUsage(instanceId);

        service.recordFullAiUsage(instanceId, "ENTERPRISE");
        service.recordFullAiUsage(instanceId, "FREE");
        // still only the single PREMIUM increment
        verify(distributedRateLimiter).incrementMonthlyFullAiUsage(instanceId);
    }
}
