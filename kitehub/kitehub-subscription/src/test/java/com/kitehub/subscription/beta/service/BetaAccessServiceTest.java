package com.kitehub.subscription.beta.service;

import com.kitehub.subscription.beta.dto.BetaApproveCommand;
import com.kitehub.subscription.beta.dto.BetaRejectCommand;
import com.kitehub.subscription.beta.dto.BetaRequestDto;
import com.kitehub.subscription.beta.dto.BetaSignupCommand;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BetaAccessService} (GAP-372 Wave 33 Bucket C).
 *
 * <p>Covers state-machine transitions + token expiry handling + duplicate-PENDING
 * idempotency + outbox publish on approve.</p>
 */
@DisplayName("BetaAccessService")
class BetaAccessServiceTest {

    private BetaAccessRequestRepository repository;
    private SubscriptionEventEmitter eventEmitter;
    private BetaAccessService service;

    @BeforeEach
    void setUp() {
        repository = mock(BetaAccessRequestRepository.class);
        // Real emitter against a mock outbox repo so we can assert outbox writes.
        SubscriptionOutboxRepository outboxRepo = mock(SubscriptionOutboxRepository.class);
        eventEmitter = new SubscriptionEventEmitter(outboxRepo);
        service = new BetaAccessService(repository, eventEmitter);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("submitRequest creates PENDING when no duplicate")
    void submitRequestCreatesPending() {
        when(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("new@x.com"), eq(BetaAccessRequestStatus.PENDING)))
                .thenReturn(Optional.empty());

        BetaRequestDto dto = new BetaRequestDto(
                "new@x.com", "New", "New Org", "P2_CENTER_OWNER", null, "");
        BetaAccessRequest result = service.submitRequest(dto);

        assertThat(result.getStatus()).isEqualTo(BetaAccessRequestStatus.PENDING);
        assertThat(result.getEmail()).isEqualTo("new@x.com");
        verify(repository).save(any(BetaAccessRequest.class));
    }

    @Test
    @DisplayName("submitRequest returns existing PENDING on duplicate (idempotent)")
    void submitRequestIdempotent() {
        BetaAccessRequest existing = BetaAccessRequest.builder()
                .id(42L).email("dup@x.com").name("Dup").orgName("DO")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
        when(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("dup@x.com"), eq(BetaAccessRequestStatus.PENDING)))
                .thenReturn(Optional.of(existing));

        BetaRequestDto dto = new BetaRequestDto(
                "dup@x.com", "Dup", "DO", "P1_SOLO_TEACHER", null, "");
        BetaAccessRequest result = service.submitRequest(dto);

        assertThat(result.getId()).isEqualTo(42L);
        verify(repository, times(0)).save(any(BetaAccessRequest.class));
    }

    @Test
    @DisplayName("approveRequest issues token + publishes outbox event")
    void approveIssuesTokenAndPublishes() {
        BetaAccessRequest pending = BetaAccessRequest.builder()
                .id(5L).email("p@x.com").name("P").orgName("PO")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
        when(repository.findById(5L)).thenReturn(Optional.of(pending));
        SubscriptionOutboxRepository outboxRepo = mock(SubscriptionOutboxRepository.class);
        SubscriptionEventEmitter spyEmitter = new SubscriptionEventEmitter(outboxRepo);
        BetaAccessService localService = new BetaAccessService(repository, spyEmitter);

        BetaAccessRequest approved = localService.approveRequest(5L, new BetaApproveCommand("coord-1"));

        assertThat(approved.getStatus()).isEqualTo(BetaAccessRequestStatus.APPROVED);
        assertThat(approved.getInviteToken()).isNotNull();
        assertThat(approved.getInviteTokenExpiry()).isAfter(OffsetDateTime.now());
        assertThat(approved.getApprovedAt()).isNotNull();
        assertThat(approved.getApproverId()).isEqualTo("coord-1");
        // Outbox row written with correct event_type/topic.
        ArgumentCaptor<com.kitehub.subscription.outbox.SubscriptionOutboxEvent> captor =
                ArgumentCaptor.forClass(com.kitehub.subscription.outbox.SubscriptionOutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("beta.invite.sent");
        assertThat(captor.getValue().getTopic()).isEqualTo("email.beta.invite");
        assertThat(captor.getValue().getPayload()).contains("\"email\":\"p@x.com\"");
    }

    @Test
    @DisplayName("approveRequest fails when not PENDING")
    void approveBlocksNonPending() {
        BetaAccessRequest already = BetaAccessRequest.builder()
                .id(6L).email("x@x.com").name("X").orgName("X")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.APPROVED)
                .build();
        when(repository.findById(6L)).thenReturn(Optional.of(already));

        assertThatThrownBy(() -> service.approveRequest(6L, new BetaApproveCommand("c")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("rejectRequest captures rejectionReason")
    void rejectCapturesReason() {
        BetaAccessRequest pending = BetaAccessRequest.builder()
                .id(8L).email("r@x.com").name("R").orgName("RO")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.PENDING)
                .build();
        when(repository.findById(8L)).thenReturn(Optional.of(pending));

        BetaAccessRequest rejected = service.rejectRequest(
                8L, new BetaRejectCommand("coord-2", "K-12 not in scope"));

        assertThat(rejected.getStatus()).isEqualTo(BetaAccessRequestStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("K-12 not in scope");
        assertThat(rejected.getRejectedAt()).isNotNull();
    }

    @Test
    @DisplayName("validateToken returns TOKEN_EXPIRED for expired APPROVED row")
    void validateTokenExpiredCase() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest approved = BetaAccessRequest.builder()
                .id(11L).email("e@x.com").name("E").orgName("EO")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(token)
                .inviteTokenExpiry(OffsetDateTime.now().minusHours(1))
                .build();
        when(repository.findByInviteToken(token)).thenReturn(Optional.of(approved));

        BetaTokenValidationResponse resp = service.validateToken(token);

        assertThat(resp.valid()).isFalse();
        assertThat(resp.errorCode()).isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    @DisplayName("validateToken returns valid pre-fill for APPROVED non-expired")
    void validateTokenHappy() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest approved = BetaAccessRequest.builder()
                .id(12L).email("ok@x.com").name("Ok").orgName("OkO")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(token)
                .inviteTokenExpiry(OffsetDateTime.now().plusHours(6))
                .build();
        when(repository.findByInviteToken(token)).thenReturn(Optional.of(approved));

        BetaTokenValidationResponse resp = service.validateToken(token);

        assertThat(resp.valid()).isTrue();
        assertThat(resp.email()).isEqualTo("ok@x.com");
        assertThat(resp.persona()).isEqualTo("P2_CENTER_OWNER");
    }

    @Test
    @DisplayName("validateToken returns ALREADY_USED for SIGNED_UP rows")
    void validateTokenAlreadyUsed() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest used = BetaAccessRequest.builder()
                .id(13L).email("u@x.com").name("U").orgName("UO")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.SIGNED_UP)
                .inviteToken(token)
                .build();
        when(repository.findByInviteToken(token)).thenReturn(Optional.of(used));

        BetaTokenValidationResponse resp = service.validateToken(token);

        assertThat(resp.valid()).isFalse();
        assertThat(resp.errorCode()).isEqualTo("ALREADY_USED");
    }

    @Test
    @DisplayName("completeBetaSignup flips to SIGNED_UP + clears token")
    void completeSignupClearsToken() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest approved = BetaAccessRequest.builder()
                .id(20L).email("s@x.com").name("S").orgName("SO")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(token)
                .inviteTokenExpiry(OffsetDateTime.now().plusHours(12))
                .build();
        when(repository.findByInviteToken(token)).thenReturn(Optional.of(approved));

        BetaAccessRequest result = service.completeBetaSignup(
                new BetaSignupCommand(token, "password123!", "abc-school"));

        assertThat(result.getStatus()).isEqualTo(BetaAccessRequestStatus.SIGNED_UP);
        assertThat(result.getInviteToken()).isNull();
        assertThat(result.getInviteTokenExpiry()).isNull();
    }

    @Test
    @DisplayName("completeBetaSignup blocks expired tokens")
    void completeSignupBlocksExpired() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest approved = BetaAccessRequest.builder()
                .id(21L).email("z@x.com").name("Z").orgName("ZO")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(token)
                .inviteTokenExpiry(OffsetDateTime.now().minusMinutes(1))
                .build();
        when(repository.findByInviteToken(token)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.completeBetaSignup(
                new BetaSignupCommand(token, "password123!", "abc-school")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("completeBetaSignup unknown token throws IllegalArgument")
    void completeSignupUnknownToken() {
        UUID token = UUID.randomUUID();
        when(repository.findByInviteToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeBetaSignup(
                new BetaSignupCommand(token, "password123!", "abc-school")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
