package com.kitehub.branding.service;

import com.kitehub.branding.config.AIRateLimitConfig;
import com.kitehub.branding.domain.entity.AIUsageLog;
import com.kitehub.branding.repository.AIUsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AIRateLimitService} — DB fallback path.
 *
 * <p>{@link DistributedRateLimiter} is mocked to simulate Redis being
 * unavailable ({@code incrementDailyUsage} / {@code getDailyUsage} return -1),
 * which forces the service onto the JPA fallback path. Dedicated Redis tests
 * live in {@link AIRateLimitServiceRedisTest}.</p>
 *
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIRateLimitServiceTest {

    @Mock
    private AIRateLimitConfig rateLimitConfig;

    @Mock
    private AIUsageLogRepository usageLogRepository;

    @Mock
    private DistributedRateLimiter distributedRateLimiter;

    @InjectMocks
    private AIRateLimitService rateLimitService;

    private UUID instanceId;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();
        // Default: Redis unavailable — every test falls back to DB.
        when(distributedRateLimiter.getDailyUsage(any(UUID.class))).thenReturn(-1L);
    }

    @Test
    void isRateLimited_underLimit_returnsFalse() {
        // Given
        when(rateLimitConfig.getLimitForTier("FREE")).thenReturn(3);
        AIUsageLog log = new AIUsageLog(instanceId, LocalDate.now());
        log.setRequestCount(2);
        when(usageLogRepository.findByInstanceIdAndUsageDate(eq(instanceId), any(LocalDate.class)))
                .thenReturn(Optional.of(log));

        // When
        boolean result = rateLimitService.isRateLimited(instanceId, "FREE");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isRateLimited_atLimit_returnsTrue() {
        // Given
        when(rateLimitConfig.getLimitForTier("FREE")).thenReturn(3);
        AIUsageLog log = new AIUsageLog(instanceId, LocalDate.now());
        log.setRequestCount(3);
        when(usageLogRepository.findByInstanceIdAndUsageDate(eq(instanceId), any(LocalDate.class)))
                .thenReturn(Optional.of(log));

        // When
        boolean result = rateLimitService.isRateLimited(instanceId, "FREE");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isRateLimited_overLimit_returnsTrue() {
        // Given
        when(rateLimitConfig.getLimitForTier("BASIC")).thenReturn(10);
        AIUsageLog log = new AIUsageLog(instanceId, LocalDate.now());
        log.setRequestCount(15);
        when(usageLogRepository.findByInstanceIdAndUsageDate(eq(instanceId), any(LocalDate.class)))
                .thenReturn(Optional.of(log));

        // When
        boolean result = rateLimitService.isRateLimited(instanceId, "BASIC");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isRateLimited_enterprise_unlimited_returnsFalse() {
        // Given
        when(rateLimitConfig.getLimitForTier("ENTERPRISE")).thenReturn(-1);

        // When
        boolean result = rateLimitService.isRateLimited(instanceId, "ENTERPRISE");

        // Then
        assertThat(result).isFalse();
        // Should not even check the database for unlimited tier
        verify(usageLogRepository, never()).findByInstanceIdAndUsageDate(any(), any());
    }

    @Test
    void isRateLimited_noUsageYet_returnsFalse() {
        // Given
        when(rateLimitConfig.getLimitForTier("FREE")).thenReturn(3);
        when(usageLogRepository.findByInstanceIdAndUsageDate(eq(instanceId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        boolean result = rateLimitService.isRateLimited(instanceId, "FREE");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void recordUsage_redisSucceeds_doesNotTouchDb() {
        // Given — Redis happy path (returns new count)
        when(distributedRateLimiter.incrementDailyUsage(instanceId)).thenReturn(4L);

        // When
        rateLimitService.recordUsage(instanceId);

        // Then — no DB calls
        verify(usageLogRepository, never()).incrementRequestCount(any(), any());
        verify(usageLogRepository, never()).save(any());
    }

    @Test
    void recordUsage_redisDown_existingDbEntry_incrementsCount() {
        // Given — Redis signals failure with -1
        when(distributedRateLimiter.incrementDailyUsage(instanceId)).thenReturn(-1L);
        when(usageLogRepository.incrementRequestCount(eq(instanceId), any(LocalDate.class)))
                .thenReturn(1);

        // When
        rateLimitService.recordUsage(instanceId);

        // Then
        verify(usageLogRepository).incrementRequestCount(eq(instanceId), any(LocalDate.class));
        verify(usageLogRepository, never()).save(any());
    }

    @Test
    void recordUsage_redisDown_noExistingEntry_createsNewLog() {
        // Given
        when(distributedRateLimiter.incrementDailyUsage(instanceId)).thenReturn(-1L);
        when(usageLogRepository.incrementRequestCount(eq(instanceId), any(LocalDate.class)))
                .thenReturn(0);

        // When
        rateLimitService.recordUsage(instanceId);

        // Then
        ArgumentCaptor<AIUsageLog> captor = ArgumentCaptor.forClass(AIUsageLog.class);
        verify(usageLogRepository).save(captor.capture());

        AIUsageLog saved = captor.getValue();
        assertThat(saved.getInstanceId()).isEqualTo(instanceId);
        assertThat(saved.getUsageDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getRequestCount()).isEqualTo(1);
    }

    @Test
    void getCurrentUsage_existingEntry_returnsCount() {
        // Given
        AIUsageLog log = new AIUsageLog(instanceId, LocalDate.now());
        log.setRequestCount(5);
        when(usageLogRepository.findByInstanceIdAndUsageDate(eq(instanceId), any(LocalDate.class)))
                .thenReturn(Optional.of(log));

        // When
        int usage = rateLimitService.getCurrentUsage(instanceId);

        // Then
        assertThat(usage).isEqualTo(5);
    }

    @Test
    void getCurrentUsage_noEntry_returnsZero() {
        // Given
        when(usageLogRepository.findByInstanceIdAndUsageDate(eq(instanceId), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        int usage = rateLimitService.getCurrentUsage(instanceId);

        // Then
        assertThat(usage).isZero();
    }

    @Test
    void getRemainingRequests_withinLimit_returnsRemaining() {
        // Given
        when(rateLimitConfig.getLimitForTier("BASIC")).thenReturn(10);
        AIUsageLog log = new AIUsageLog(instanceId, LocalDate.now());
        log.setRequestCount(3);
        when(usageLogRepository.findByInstanceIdAndUsageDate(eq(instanceId), any(LocalDate.class)))
                .thenReturn(Optional.of(log));

        // When
        int remaining = rateLimitService.getRemainingRequests(instanceId, "BASIC");

        // Then
        assertThat(remaining).isEqualTo(7);
    }

    @Test
    void getRemainingRequests_unlimited_returnsNegativeOne() {
        // Given
        when(rateLimitConfig.getLimitForTier("ENTERPRISE")).thenReturn(-1);

        // When
        int remaining = rateLimitService.getRemainingRequests(instanceId, "ENTERPRISE");

        // Then
        assertThat(remaining).isEqualTo(-1);
    }

    @Test
    void getRemainingRequests_exceeded_returnsZero() {
        // Given
        when(rateLimitConfig.getLimitForTier("FREE")).thenReturn(3);
        AIUsageLog log = new AIUsageLog(instanceId, LocalDate.now());
        log.setRequestCount(5);
        when(usageLogRepository.findByInstanceIdAndUsageDate(eq(instanceId), any(LocalDate.class)))
                .thenReturn(Optional.of(log));

        // When
        int remaining = rateLimitService.getRemainingRequests(instanceId, "FREE");

        // Then
        assertThat(remaining).isZero();
    }

    @Test
    void getDailyLimit_delegatesToConfig() {
        // Given
        when(rateLimitConfig.getLimitForTier("PREMIUM")).thenReturn(50);

        // When
        int limit = rateLimitService.getDailyLimit("PREMIUM");

        // Then
        assertThat(limit).isEqualTo(50);
    }
}
