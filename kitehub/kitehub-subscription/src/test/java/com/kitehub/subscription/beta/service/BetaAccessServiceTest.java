package com.kitehub.subscription.beta.service;

import com.kitehub.subscription.beta.dto.BetaApproveCommand;
import com.kitehub.subscription.beta.dto.BetaClaimCodeExchangeResponse;
import com.kitehub.subscription.beta.dto.BetaRejectCommand;
import com.kitehub.subscription.beta.dto.BetaRequestDto;
import com.kitehub.subscription.beta.dto.BetaSignupCommand;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import com.kitehub.subscription.outbox.SubscriptionOutboxRepository;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
    private SubscriptionOutboxRepository outboxRepo;
    private SubscriptionEventEmitter eventEmitter;
    private MeterRegistry meterRegistry;
    private BetaAccessService service;

    @BeforeEach
    void setUp() {
        repository = mock(BetaAccessRequestRepository.class);
        // Real emitter against a mock outbox repo so we can assert outbox writes.
        outboxRepo = mock(SubscriptionOutboxRepository.class);
        eventEmitter = new SubscriptionEventEmitter(outboxRepo);
        meterRegistry = new SimpleMeterRegistry();
        // EmailServiceClient null in unit tests — production wiring uses the real bean.
        service = new BetaAccessService(repository, eventEmitter, meterRegistry, null);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Wave 105 Bucket E0 Bug 3 + Bucket A — submitRequest uses saveAndFlush()
        // to surface V55 partial unique violation as DataIntegrityViolationException
        // at point-of-insert (rather than end-of-txn). Mirror save() behavior.
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("submitRequest creates PENDING when no duplicate")
    void submitRequestCreatesPending() {
        when(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("new@x.com"), eq(BetaAccessRequestStatus.PENDING)))
                .thenReturn(Optional.empty());

        BetaRequestDto dto = new BetaRequestDto(
                "new@x.com", "New", "New Org", "P2_CENTER_OWNER", null, "", true);
        BetaAccessRequest result = service.submitRequest(dto);

        assertThat(result.getStatus()).isEqualTo(BetaAccessRequestStatus.PENDING);
        assertThat(result.getEmail()).isEqualTo("new@x.com");
        // GAP-385: consent fields persisted on insert.
        assertThat(result.isConsentGiven()).isTrue();
        assertThat(result.getConsentAt()).isNotNull();
        verify(repository).saveAndFlush(any(BetaAccessRequest.class));
        // GAP-385: consent-given audit event emitted via outbox (no direct rabbit).
        ArgumentCaptor<com.kitehub.subscription.outbox.SubscriptionOutboxEvent> captor =
                ArgumentCaptor.forClass(com.kitehub.subscription.outbox.SubscriptionOutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("beta.consent.given");
        assertThat(captor.getValue().getTopic()).isEqualTo("audit.beta.consent");
        assertThat(captor.getValue().getPayload()).contains("\"email\":\"new@x.com\"");
    }

    @Test
    @DisplayName("Wave 105 Bucket A — submitRequest race-loser path: DataIntegrityViolation -> return winning PENDING row")
    void submitRequestRaceLoserReturnsWinningRow() {
        // Setup: existence-check returns empty (we're the race-leader path... initially),
        // then INSERT throws (race-loser path — winning concurrent INSERT beat us),
        // then re-read returns the winning row.
        BetaAccessRequest winningRow = BetaAccessRequest.builder()
                .id(99L).email("race@x.com").name("R").orgName("RO")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
        when(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("race@x.com"), eq(BetaAccessRequestStatus.PENDING)))
                .thenReturn(Optional.empty())  // race-leader check: clear
                .thenReturn(Optional.of(winningRow)); // post-DIVE re-read: winning row
        when(repository.saveAndFlush(any())).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"idx_beta_request_email_unique_pending\""));

        BetaRequestDto dto = new BetaRequestDto(
                "race@x.com", "R", "RO", "P2_CENTER_OWNER", null, "", true);
        BetaAccessRequest result = service.submitRequest(dto);

        // Race-loser returns the winning row (not throws).
        assertThat(result.getId()).isEqualTo(99L);
        verify(repository).saveAndFlush(any(BetaAccessRequest.class));
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
                "dup@x.com", "Dup", "DO", "P1_SOLO_TEACHER", null, "", true);
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
        BetaAccessService localService = new BetaAccessService(repository, spyEmitter, meterRegistry, null);

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

    // ── GAP-387: Micrometer counter tests ───────────────────────────────

    @Test
    @DisplayName("submitRequest increments beta_signup_requests_total{persona} counter")
    void submitRequestIncrementsSignupCounter() {
        when(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("metric@x.com"), eq(BetaAccessRequestStatus.PENDING)))
                .thenReturn(Optional.empty());

        BetaRequestDto dto = new BetaRequestDto(
                "metric@x.com", "Metric", "MO", "P2_CENTER_OWNER", null, "", true);
        service.submitRequest(dto);

        double count = meterRegistry.counter(
                "beta_signup_requests_total", "persona", "P2_CENTER_OWNER").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("approveRequest increments beta_signup_approvals_total{persona} counter")
    void approveRequestIncrementsApprovalCounter() {
        BetaAccessRequest pending = BetaAccessRequest.builder()
                .id(101L).email("a@x.com").name("A").orgName("AO")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
        when(repository.findById(101L)).thenReturn(Optional.of(pending));

        service.approveRequest(101L, new BetaApproveCommand("coord-x"));

        double count = meterRegistry.counter(
                "beta_signup_approvals_total", "persona", "P1_SOLO_TEACHER").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("rejectRequest increments beta_signup_rejections_total{persona} counter")
    void rejectRequestIncrementsRejectionCounter() {
        BetaAccessRequest pending = BetaAccessRequest.builder()
                .id(102L).email("r@x.com").name("R").orgName("RO")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .build();
        when(repository.findById(102L)).thenReturn(Optional.of(pending));

        service.rejectRequest(102L, new BetaRejectCommand("coord-y", "out of scope"));

        double count = meterRegistry.counter(
                "beta_signup_rejections_total", "persona", "P2_CENTER_OWNER").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordHoneypotRejection increments beta_honeypot_rejections_total counter")
    void recordHoneypotRejectionIncrementsCounter() {
        service.recordHoneypotRejection();
        service.recordHoneypotRejection();

        double count = meterRegistry.counter("beta_honeypot_rejections_total").count();
        assertThat(count).isEqualTo(2.0);
    }

    // ── GAP-388 Wave 36 Bucket A — security cluster ──────────────────────

    @Test
    @DisplayName("GAP-388 388-A — recordHoneypotRejection(email, ip) increments counter + accepts audit context")
    void recordHoneypotRejectionWithAuditContext() {
        service.recordHoneypotRejection("bot@spam.tld", "203.0.113.7");

        double count = meterRegistry.counter("beta_honeypot_rejections_total").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("GAP-388 388-B — approveRequest emits a 6-digit numeric claimCode + clears it on signup")
    void approveRequestEmitsClaimCodeClearedOnSignup() {
        // approve transition
        BetaAccessRequest pending = BetaAccessRequest.builder()
                .id(501L).email("b@x.com").name("B").orgName("BO")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
        when(repository.findById(501L)).thenReturn(Optional.of(pending));
        when(repository.findByClaimCode(any())).thenReturn(Optional.empty());

        BetaAccessRequest approved = service.approveRequest(501L, new BetaApproveCommand("coord-z"));

        assertThat(approved.getClaimCode())
                .as("claim code populated post-approve")
                .isNotNull()
                .hasSize(6)
                .matches("[0-9]{6}");
        assertThat(approved.getInviteToken()).isNotNull();
        UUID issued = approved.getInviteToken();

        // signup completion clears both
        when(repository.findByInviteToken(issued)).thenReturn(Optional.of(approved));
        BetaAccessRequest signed = service.completeBetaSignup(
                new com.kitehub.subscription.beta.dto.BetaSignupCommand(issued, "p@ssword12", "abc-tenant"));
        assertThat(signed.getClaimCode()).as("claim code cleared post-signup").isNull();
        assertThat(signed.getInviteToken()).isNull();
    }

    @Test
    @DisplayName("GAP-388 388-B — exchangeClaimCode returns invite_token + pre-fill on APPROVED + valid")
    void exchangeClaimCodeHappyPath() {
        UUID token = UUID.randomUUID();
        BetaAccessRequest approved = BetaAccessRequest.builder()
                .id(601L).email("ok@x.com").name("Ok").orgName("OkOrg")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(token)
                .inviteTokenExpiry(OffsetDateTime.now().plusHours(12))
                .claimCode("123456")
                .build();
        when(repository.findByClaimCode("123456")).thenReturn(Optional.of(approved));

        BetaClaimCodeExchangeResponse resp = service.exchangeClaimCode("123456");

        assertThat(resp.valid()).isTrue();
        assertThat(resp.inviteToken()).isEqualTo(token);
        assertThat(resp.email()).isEqualTo("ok@x.com");
        assertThat(resp.errorCode()).isNull();
    }

    @Test
    @DisplayName("GAP-388 388-B — exchangeClaimCode returns CODE_NOT_FOUND on unknown / wrong code")
    void exchangeClaimCodeWrongCode() {
        when(repository.findByClaimCode(any())).thenReturn(Optional.empty());

        BetaClaimCodeExchangeResponse resp = service.exchangeClaimCode("999999");

        assertThat(resp.valid()).isFalse();
        assertThat(resp.errorCode()).isEqualTo("CODE_NOT_FOUND");
        assertThat(resp.inviteToken()).isNull();
    }

    @Test
    @DisplayName("GAP-388 388-B — exchangeClaimCode returns CODE_EXPIRED when token TTL elapsed")
    void exchangeClaimCodeExpired() {
        BetaAccessRequest expired = BetaAccessRequest.builder()
                .id(602L).email("exp@x.com").name("Exp").orgName("ExpOrg")
                .persona("P1_SOLO_TEACHER")
                .status(BetaAccessRequestStatus.APPROVED)
                .inviteToken(UUID.randomUUID())
                .inviteTokenExpiry(OffsetDateTime.now().minusMinutes(1))
                .claimCode("000111")
                .build();
        when(repository.findByClaimCode("000111")).thenReturn(Optional.of(expired));

        BetaClaimCodeExchangeResponse resp = service.exchangeClaimCode("000111");

        assertThat(resp.valid()).isFalse();
        assertThat(resp.errorCode()).isEqualTo("CODE_EXPIRED");
    }

    @Test
    @DisplayName("GAP-388 388-C — submitRequest(dto, ip) accepts first IP and rejects 2nd from different IP")
    void submitRequestPerEmailRateLimitRejects() {
        when(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("rl@x.com"), eq(BetaAccessRequestStatus.PENDING)))
                .thenReturn(Optional.empty());
        BetaRequestDto dto = new BetaRequestDto(
                "rl@x.com", "RL", "RLO", "P2_CENTER_OWNER", null, "", true);

        // 1st attempt — succeeds + caches IP
        BetaAccessRequest first = service.submitRequest(dto, "203.0.113.1");
        assertThat(first.getStatus()).isEqualTo(BetaAccessRequestStatus.PENDING);

        // 2nd attempt from a different IP within window — rejected with 429-style exception
        assertThatThrownBy(() -> service.submitRequest(dto, "198.51.100.9"))
                .isInstanceOf(com.kitehub.subscription.beta.service.BetaRateLimitExceededException.class);

        double rateLimitCount = meterRegistry.counter("beta_rate_limit_rejections_total").count();
        assertThat(rateLimitCount).isEqualTo(1.0);
    }

    @Test
    @DisplayName("GAP-388 388-C — submitRequest(dto, ip) allows 2nd attempt from SAME IP (idempotent return)")
    void submitRequestPerEmailRateLimitAllowsSameIp() {
        BetaAccessRequest existing = BetaAccessRequest.builder()
                .id(701L).email("same@x.com").name("Same").orgName("SO")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .build();
        // First call: no existing PENDING → creates one. Subsequent: returns existing.
        when(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("same@x.com"), eq(BetaAccessRequestStatus.PENDING)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));

        BetaRequestDto dto = new BetaRequestDto(
                "same@x.com", "Same", "SO", "P2_CENTER_OWNER", null, "", true);

        BetaAccessRequest a = service.submitRequest(dto, "203.0.113.5");
        BetaAccessRequest b = service.submitRequest(dto, "203.0.113.5");
        assertThat(a).isNotNull();
        assertThat(b).isNotNull();
        // Same IP → no rate-limit rejection counter increment.
        double rateLimitCount = meterRegistry.counter("beta_rate_limit_rejections_total").count();
        assertThat(rateLimitCount).isEqualTo(0.0);
    }

    // ── Wave 105 Bucket E0 tests ──────────────────────────────────────

    @Test
    @DisplayName("Bug 2 (A4) — submitRequest strips HTML tags from name + orgName")
    void submitRequestSanitizesXss() {
        when(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("xss@x.com"), eq(BetaAccessRequestStatus.PENDING)))
                .thenReturn(Optional.empty());

        BetaRequestDto dto = new BetaRequestDto(
                "xss@x.com",
                "<script>alert(1)</script>Trần Thị Hồng",
                "Sky Education <img src=x onerror=alert(1)>",
                "P2_CENTER_OWNER",
                "<iframe>bad</iframe>",
                "",
                true);

        BetaAccessRequest result = service.submitRequest(dto);

        // HTML tags stripped (executable XSS vectors neutralized);
        // residual TEXT content (e.g. `alert(1)`) remains as harmless text —
        // React/HtmlEscape auto-escapes any leftover symbols.
        assertThat(result.getName()).doesNotContain("<script>");
        assertThat(result.getName()).doesNotContain("</script>");
        assertThat(result.getName()).contains("Trần Thị Hồng");
        assertThat(result.getOrgName()).doesNotContain("<img");
        assertThat(result.getOrgName()).doesNotContain("onerror=");
        assertThat(result.getOrgName()).contains("Sky Education");
        assertThat(result.getReferralSource()).doesNotContain("<iframe>");
        assertThat(result.getReferralSource()).doesNotContain("</iframe>");
    }

    @Test
    @DisplayName("Bug 2 (A4) — sanitizeFreeText helper round-trip")
    void sanitizeFreeTextHelper() {
        // Direct unit test of the sanitizer helper
        assertThat(BetaAccessService.sanitizeFreeText(null)).isNull();
        assertThat(BetaAccessService.sanitizeFreeText("Trần Thị Hồng"))
                .isEqualTo("Trần Thị Hồng");
        // <script> + </script> tags stripped; `alert(1)` remains as harmless text
        assertThat(BetaAccessService.sanitizeFreeText("<script>alert(1)</script>"))
                .isEqualTo("alert(1)");
        // Pure HTML tag → empty
        assertThat(BetaAccessService.sanitizeFreeText("<br/>"))
                .isEqualTo("");
        assertThat(BetaAccessService.sanitizeFreeText("Normal text & symbols"))
                .isEqualTo("Normal text &amp; symbols");
        // Residual `<` without close becomes &lt; via HtmlEscape
        assertThat(BetaAccessService.sanitizeFreeText("price < 1000"))
                .contains("&lt;");
    }

    @Test
    @DisplayName("Bug 3 (A1) — concurrent INSERT race returns existing row gracefully")
    void submitRequestHandlesConcurrentRaceGracefully() {
        BetaAccessRequest winner = BetaAccessRequest.builder()
                .id(99L).email("race@x.com").name("Race").orgName("RO")
                .persona("P2_CENTER_OWNER")
                .status(BetaAccessRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
        // 1st call (initial existence check) → empty (race window passes)
        // 2nd call (post-DataIntegrityViolationException re-query) → winner row
        when(repository.findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("race@x.com"), eq(BetaAccessRequestStatus.PENDING)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        // saveAndFlush simulates DB partial unique index collision
        when(repository.saveAndFlush(any())).thenThrow(
                new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint "
                                + "\"idx_beta_request_email_unique_pending\""));

        BetaRequestDto dto = new BetaRequestDto(
                "race@x.com", "Race", "RO", "P2_CENTER_OWNER", null, "", true);
        BetaAccessRequest result = service.submitRequest(dto);

        // Race loser gets the same winner row back — graceful idempotent return
        assertThat(result.getId()).isEqualTo(99L);
        verify(repository, times(1)).saveAndFlush(any(BetaAccessRequest.class));
        verify(repository, times(2)).findFirstByEmailAndStatusOrderByCreatedAtDesc(
                eq("race@x.com"), eq(BetaAccessRequestStatus.PENDING));
    }
}
