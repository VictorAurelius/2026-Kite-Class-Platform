package com.kitehub.subscription.beta.service;

import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.exception.InviteTokenAlreadyUsedException;
import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InviteTokenService} (GAP-534 Wave 77 Bucket D).
 *
 * <p>Covers the three behavior branches of {@code validateAndConsume}:
 * <ul>
 *   <li>First consume: row-count==1 → returns refreshed entity</li>
 *   <li>Reuse attempt: row-count==0 → throws {@link InviteTokenAlreadyUsedException}
 *       + increments reuse-counter</li>
 *   <li>Lifecycle gate: not APPROVED / expired → throws {@code IllegalStateException}</li>
 * </ul></p>
 */
@DisplayName("InviteTokenService")
class InviteTokenServiceTest {

    private BetaAccessRequestRepository repository;
    private MeterRegistry meterRegistry;
    private InviteTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(BetaAccessRequestRepository.class);
        meterRegistry = new SimpleMeterRegistry();
        service = new InviteTokenService(repository, meterRegistry);
    }

    @Test
    @DisplayName("validateAndConsume — first use → row consumed, entity refreshed")
    void firstConsumeSucceeds() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest entity = newApprovedEntity(token);
        BetaAccessRequest consumed = newApprovedEntity(token);
        consumed.setUsedAt(OffsetDateTime.now());
        consumed.setConsumedIp("203.0.113.10");
        consumed.setConsumedUserAgent("Mozilla/5.0");

        // findByInviteToken called twice: once for lifecycle gate, once for refresh.
        when(repository.findByInviteToken(token))
                .thenReturn(Optional.of(entity))
                .thenReturn(Optional.of(consumed));
        when(repository.consumeInviteToken(eq(token), any(), eq("203.0.113.10"), eq("Mozilla/5.0")))
                .thenReturn(1);

        BetaAccessRequest result = service.validateAndConsume(token, "203.0.113.10", "Mozilla/5.0");

        assertThat(result.getUsedAt()).isNotNull();
        assertThat(result.getConsumedIp()).isEqualTo("203.0.113.10");
        verify(repository).consumeInviteToken(eq(token), any(), eq("203.0.113.10"), eq("Mozilla/5.0"));
    }

    @Test
    @DisplayName("validateAndConsume — reuse attempt → 409 + counter increment")
    void reuseAttemptThrows() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest entity = newApprovedEntity(token);
        // First consume already happened — used_at populated.
        entity.setUsedAt(OffsetDateTime.now().minusMinutes(5));
        entity.setConsumedIp("198.51.100.7");
        when(repository.findByInviteToken(token)).thenReturn(Optional.of(entity));
        when(repository.consumeInviteToken(eq(token), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() ->
                service.validateAndConsume(token, "203.0.113.10", "Mozilla/5.0 attacker"))
                .isInstanceOf(InviteTokenAlreadyUsedException.class)
                .hasMessageContaining("Link đã được sử dụng");

        // Counter incremented.
        assertThat(meterRegistry.counter(InviteTokenService.METRIC_REUSE_ATTEMPTS).count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("validateAndConsume — token not found → IllegalStateException")
    void tokenNotFoundThrows() {
        UUID token = UUID.randomUUID();
        when(repository.findByInviteToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndConsume(token, "203.0.113.10", "UA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("validateAndConsume — PENDING status → IllegalStateException (lifecycle gate)")
    void wrongStatusThrows() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest entity = newApprovedEntity(token);
        entity.setStatus(BetaAccessRequestStatus.PENDING);
        when(repository.findByInviteToken(token)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.validateAndConsume(token, "203.0.113.10", "UA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    @DisplayName("validateAndConsume — expired token → IllegalStateException")
    void expiredTokenThrows() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest entity = newApprovedEntity(token);
        entity.setInviteTokenExpiry(OffsetDateTime.now().minusHours(1));
        when(repository.findByInviteToken(token)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.validateAndConsume(token, "203.0.113.10", "UA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("truncate — null safe + caps long strings")
    void truncateHandlesEdges() {
        assertThat(InviteTokenService.truncate(null, 10)).isNull();
        assertThat(InviteTokenService.truncate("short", 10)).isEqualTo("short");
        String over = "a".repeat(100);
        assertThat(InviteTokenService.truncate(over, 50)).hasSize(50);
    }

    private BetaAccessRequest newApprovedEntity(UUID token) {
        BetaAccessRequest e = BetaAccessRequest.builder()
                .id(1L)
                .email("hoamai@example.com")
                .name("Anh Tuan")
                .orgName("Truong Mam Non Hoa Mai")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(token)
                .inviteTokenExpiry(OffsetDateTime.now().plusHours(12))
                .consentGiven(true)
                .consentAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return e;
    }
}
