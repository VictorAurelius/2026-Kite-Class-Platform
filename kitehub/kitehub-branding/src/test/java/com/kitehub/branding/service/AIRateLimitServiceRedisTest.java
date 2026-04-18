package com.kitehub.branding.service;

import com.kitehub.branding.config.AIRateLimitConfig;
import com.kitehub.branding.repository.AIUsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behavioural tests for {@link AIRateLimitService} when Redis is the primary
 * backing store. Uses a mocked {@link DistributedRateLimiter} to simulate
 * atomic INCR semantics — avoids the cost of spinning up Testcontainers while
 * still proving the Redis-first path is exercised.
 *
 * <p>Pairs with {@link AIRateLimitServiceTest} which covers the DB fallback
 * path (Redis unavailable).</p>
 */
@ExtendWith(MockitoExtension.class)
class AIRateLimitServiceRedisTest {

    @Mock
    private AIRateLimitConfig rateLimitConfig;

    @Mock
    private AIUsageLogRepository usageLogRepository;

    @Mock
    private DistributedRateLimiter distributedRateLimiter;

    private AIRateLimitService service;
    private UUID instanceId;

    @BeforeEach
    void setUp() {
        service = new AIRateLimitService(rateLimitConfig, usageLogRepository, distributedRateLimiter);
        instanceId = UUID.randomUUID();
    }

    @Test
    void recordUsage_redisIncrementsAtomically_doesNotHitDb() {
        // Given — Redis returns successively incremented values (atomic INCR)
        when(distributedRateLimiter.incrementDailyUsage(instanceId))
                .thenReturn(1L, 2L, 3L);

        // When — 3 concurrent-ish calls
        service.recordUsage(instanceId);
        service.recordUsage(instanceId);
        service.recordUsage(instanceId);

        // Then — DB is never touched
        verify(usageLogRepository, never()).incrementRequestCount(any(), any());
        verify(usageLogRepository, never()).save(any());
    }

    @Test
    void getCurrentUsage_redisPresent_returnsRedisValue() {
        // Given
        when(distributedRateLimiter.getDailyUsage(instanceId)).thenReturn(7L);

        // When
        int usage = service.getCurrentUsage(instanceId);

        // Then — Redis value is returned, DB untouched
        assertThat(usage).isEqualTo(7);
        verify(usageLogRepository, never()).findByInstanceIdAndUsageDate(any(), any());
    }

    @Test
    void isRateLimited_redisShowsOverLimit_returnsTrue() {
        // Given — Redis reports 5 requests, FREE limit is 3
        when(rateLimitConfig.getLimitForTier("FREE")).thenReturn(3);
        when(distributedRateLimiter.getDailyUsage(instanceId)).thenReturn(5L);

        // When
        boolean limited = service.isRateLimited(instanceId, "FREE");

        // Then
        assertThat(limited).isTrue();
    }

    @Test
    void recordUsage_redisFailsMidFlight_fallsBackToDb() {
        // Given — Redis returns -1 (e.g. connection failure)
        when(distributedRateLimiter.incrementDailyUsage(instanceId)).thenReturn(-1L);
        when(usageLogRepository.incrementRequestCount(any(), any())).thenReturn(1);

        // When
        service.recordUsage(instanceId);

        // Then — DB fallback path engaged
        verify(usageLogRepository).incrementRequestCount(any(), any());
    }

    @Test
    void recordUsage_redisFailsAndDbHasNoEntry_createsDbRow() {
        // Given
        when(distributedRateLimiter.incrementDailyUsage(instanceId)).thenReturn(-1L);
        when(usageLogRepository.incrementRequestCount(any(), any())).thenReturn(0);

        // When
        service.recordUsage(instanceId);

        // Then — new AIUsageLog created
        verify(usageLogRepository).save(any());
    }
}
