package com.kitehub.subscription.scheduler;

import com.kitehub.platform.domain.entity.Instance;
import com.kitehub.subscription.config.DomainVerificationConfig;
import com.kitehub.subscription.repository.DomainVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DomainVerificationTimeoutScheduler} (GAP-1024 timeout edge).
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DomainVerificationTimeoutScheduler Unit Tests")
class DomainVerificationTimeoutSchedulerTest {

    @Mock
    private DomainVerificationRepository domainVerificationRepository;

    @Mock
    private DomainVerificationConfig domainVerificationConfig;

    @InjectMocks
    private DomainVerificationTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(domainVerificationConfig.getTimeoutHours()).thenReturn(48);
    }

    private Instance pendingInstance(String domain) {
        Instance i = new Instance();
        i.setId(UUID.randomUUID());
        i.setCustomDomain(domain);
        i.setDomainVerifyToken("kitehub-verify=" + UUID.randomUUID());
        i.setDomainStatus(Instance.DomainStatus.PENDING_VERIFY);
        return i;
    }

    @Test
    @DisplayName("Stale PENDING_VERIFY instances are flipped to FAILED")
    void expireStalePendingVerifications_flipsStaleToFailed() {
        // Given - 2 stale PENDING_VERIFY instances older than timeout
        Instance a = pendingInstance("a.example.com");
        Instance b = pendingInstance("b.example.com");
        when(domainVerificationRepository.findByDomainStatusAndUpdatedAtBeforeAndDeletedFalse(
                eq(Instance.DomainStatus.PENDING_VERIFY), any(LocalDateTime.class)))
            .thenReturn(List.of(a, b));

        // When
        scheduler.expireStalePendingVerifications();

        // Then - both saved as FAILED
        ArgumentCaptor<Instance> captor = ArgumentCaptor.forClass(Instance.class);
        verify(domainVerificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
            .allMatch(i -> i.getDomainStatus() == Instance.DomainStatus.FAILED);
    }

    @Test
    @DisplayName("No stale instances → no saves")
    void expireStalePendingVerifications_noStale_noSaves() {
        // Given - no stale instances
        when(domainVerificationRepository.findByDomainStatusAndUpdatedAtBeforeAndDeletedFalse(
                eq(Instance.DomainStatus.PENDING_VERIFY), any(LocalDateTime.class)))
            .thenReturn(List.of());

        // When
        scheduler.expireStalePendingVerifications();

        // Then - nothing saved
        verify(domainVerificationRepository, never()).save(any(Instance.class));
    }

    @Test
    @DisplayName("Threshold uses configured timeout-hours (now - 48h)")
    void expireStalePendingVerifications_usesConfiguredThreshold() {
        // Given
        when(domainVerificationRepository.findByDomainStatusAndUpdatedAtBeforeAndDeletedFalse(
                eq(Instance.DomainStatus.PENDING_VERIFY), any(LocalDateTime.class)))
            .thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now().minusHours(48);

        // When
        scheduler.expireStalePendingVerifications();

        // Then - query threshold is ~now-48h (within a small wall-clock window)
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(domainVerificationRepository).findByDomainStatusAndUpdatedAtBeforeAndDeletedFalse(
            eq(Instance.DomainStatus.PENDING_VERIFY), captor.capture());
        LocalDateTime after = LocalDateTime.now().minusHours(48);
        assertThat(captor.getValue())
            .isAfterOrEqualTo(before.minusSeconds(5))
            .isBeforeOrEqualTo(after.plusSeconds(5));
    }
}
