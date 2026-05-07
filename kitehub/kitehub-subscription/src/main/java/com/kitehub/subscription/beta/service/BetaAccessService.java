package com.kitehub.subscription.beta.service;

import com.kitehub.subscription.beta.dto.BetaApproveCommand;
import com.kitehub.subscription.beta.dto.BetaRejectCommand;
import com.kitehub.subscription.beta.dto.BetaRequestDto;
import com.kitehub.subscription.beta.dto.BetaSignupCommand;
import com.kitehub.subscription.beta.dto.BetaTokenValidationResponse;
import com.kitehub.subscription.beta.entity.BetaAccessRequest;
import com.kitehub.subscription.beta.entity.BetaAccessRequestStatus;
import com.kitehub.subscription.beta.repository.BetaAccessRequestRepository;
import com.kitehub.subscription.service.migration.SubscriptionEventEmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Beta access request lifecycle service (GAP-372 Wave 33).
 *
 * <p>Encapsulates the 3-stage Phase 1 BETA invite flow + enforces status
 * transitions per {@link BetaAccessRequestStatus} state machine:</p>
 *
 * <ol>
 *   <li>{@link #submitRequest(BetaRequestDto)} — public submission, PENDING</li>
 *   <li>{@link #approveRequest(Long, BetaApproveCommand)} — coordinator approve;
 *       issues {@code inviteToken}, expiry +24h, publishes
 *       {@code beta.invite.sent} event via Outbox (consumed by kitehub-email)</li>
 *   <li>{@link #rejectRequest(Long, BetaRejectCommand)} — coordinator reject</li>
 *   <li>{@link #completeBetaSignup(BetaSignupCommand)} — invitee redeems token,
 *       row flipped to SIGNED_UP and token cleared</li>
 * </ol>
 *
 * <p>Per {@code design-patterns.md §3.5}, the {@code beta.invite.sent} event is
 * published via the per-module subscription outbox — no direct {@code rabbitTemplate.send}.</p>
 *
 * @since Wave 33 — GAP-372
 */
@Service
@Slf4j
public class BetaAccessService {

    /** 24h TTL per gap spec. */
    static final long INVITE_TOKEN_TTL_HOURS = 24L;

    /** Outbox event type for invite-issued events. */
    static final String EVENT_TYPE_INVITE_SENT = "beta.invite.sent";

    /** Outbox topic / routing key for kitehub-email consumer. */
    static final String TOPIC_INVITE_SENT = "email.beta.invite";

    private final BetaAccessRequestRepository repository;
    private final SubscriptionEventEmitter eventEmitter;

    public BetaAccessService(BetaAccessRequestRepository repository, SubscriptionEventEmitter eventEmitter) {
        this.repository = repository;
        this.eventEmitter = eventEmitter;
    }

    /**
     * Submit a fresh beta access request.
     *
     * <p>Idempotency: a duplicate submit from the same email while an existing
     * PENDING row is open returns the existing row instead of creating a second
     * one (avoids inbox spam from honest mistakes; coordinator only sees one).</p>
     */
    @Transactional
    public BetaAccessRequest submitRequest(BetaRequestDto dto) {
        Optional<BetaAccessRequest> existingPending = repository
                .findFirstByEmailAndStatusOrderByCreatedAtDesc(dto.email(), BetaAccessRequestStatus.PENDING);
        if (existingPending.isPresent()) {
            log.info("Duplicate PENDING beta request for {}, returning existing id={}",
                    dto.email(), existingPending.get().getId());
            return existingPending.get();
        }

        BetaAccessRequest entity = BetaAccessRequest.builder()
                .email(dto.email())
                .name(dto.name())
                .orgName(dto.orgName())
                .persona(dto.persona())
                .referralSource(dto.referralSource())
                .status(BetaAccessRequestStatus.PENDING)
                .build();
        BetaAccessRequest saved = repository.save(entity);
        log.info("Beta access request submitted: id={} email={} persona={}",
                saved.getId(), saved.getEmail(), saved.getPersona());
        return saved;
    }

    /** Paginated coordinator listing, default ordered by createdAt desc. */
    public Page<BetaAccessRequest> listByStatus(BetaAccessRequestStatus status, Pageable pageable) {
        return repository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    /**
     * Coordinator approve transition. Issues the {@code inviteToken} + publishes
     * the invite-sent event via Outbox in the same transaction (per
     * {@code design-patterns.md §3.5}).
     */
    @Transactional
    public BetaAccessRequest approveRequest(Long requestId, BetaApproveCommand cmd) {
        BetaAccessRequest entity = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Beta request not found: " + requestId));

        if (entity.getStatus() != BetaAccessRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot approve request in state " + entity.getStatus() + " (must be PENDING)");
        }

        OffsetDateTime now = OffsetDateTime.now();
        entity.setStatus(BetaAccessRequestStatus.APPROVED);
        entity.setApproverId(cmd.approverId());
        entity.setApprovedAt(now);
        entity.setInviteToken(UUID.randomUUID());
        entity.setInviteTokenExpiry(now.plusHours(INVITE_TOKEN_TTL_HOURS));
        entity.setInviteSentAt(now);

        BetaAccessRequest saved = repository.save(entity);

        // Outbox publish — consumed by kitehub-email to deliver invite link.
        // No direct rabbitTemplate.send (design-patterns.md §3.5).
        String payload = String.format(
                "{\"requestId\":%d,\"email\":\"%s\",\"name\":\"%s\",\"orgName\":\"%s\",\"persona\":\"%s\",\"token\":\"%s\",\"expiresAt\":\"%s\"}",
                saved.getId(),
                SubscriptionEventEmitter.escape(saved.getEmail()),
                SubscriptionEventEmitter.escape(saved.getName()),
                SubscriptionEventEmitter.escape(saved.getOrgName()),
                SubscriptionEventEmitter.escape(saved.getPersona()),
                saved.getInviteToken(),
                saved.getInviteTokenExpiry()
        );
        eventEmitter.emit((UUID) null, EVENT_TYPE_INVITE_SENT, TOPIC_INVITE_SENT, payload);

        log.info("Beta access request approved: id={} email={} approver={}",
                saved.getId(), saved.getEmail(), cmd.approverId());
        return saved;
    }

    /** Coordinator reject transition. Captures rejectionReason. */
    @Transactional
    public BetaAccessRequest rejectRequest(Long requestId, BetaRejectCommand cmd) {
        BetaAccessRequest entity = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Beta request not found: " + requestId));

        if (entity.getStatus() != BetaAccessRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot reject request in state " + entity.getStatus() + " (must be PENDING)");
        }

        entity.setStatus(BetaAccessRequestStatus.REJECTED);
        entity.setApproverId(cmd.approverId());
        entity.setRejectedAt(OffsetDateTime.now());
        entity.setRejectionReason(cmd.rejectionReason());
        BetaAccessRequest saved = repository.save(entity);
        log.info("Beta access request rejected: id={} email={} approver={}",
                saved.getId(), saved.getEmail(), cmd.approverId());
        return saved;
    }

    /**
     * Validate invite token at signup pre-fill time.
     *
     * <p>Returns a structured response so the FE can distinguish:
     * not-found / expired / already-used cases without leaking which (the
     * controller maps all to 404 in the response status; this DTO contains the
     * granular reason for the FE error message).</p>
     */
    public BetaTokenValidationResponse validateToken(UUID token) {
        Optional<BetaAccessRequest> opt = repository.findByInviteToken(token);
        if (opt.isEmpty()) {
            return BetaTokenValidationResponse.invalid("TOKEN_NOT_FOUND");
        }
        BetaAccessRequest entity = opt.get();
        if (entity.getStatus() == BetaAccessRequestStatus.SIGNED_UP) {
            return BetaTokenValidationResponse.invalid("ALREADY_USED");
        }
        if (entity.getStatus() != BetaAccessRequestStatus.APPROVED) {
            return BetaTokenValidationResponse.invalid("TOKEN_NOT_FOUND");
        }
        if (entity.isTokenExpired()) {
            return BetaTokenValidationResponse.invalid("TOKEN_EXPIRED");
        }
        return BetaTokenValidationResponse.ok(
                entity.getEmail(), entity.getName(), entity.getOrgName(), entity.getPersona());
    }

    /**
     * Complete the invitee signup — flips status to SIGNED_UP + clears token.
     *
     * <p>The actual tenant provisioning (subdomain reservation, owner-user
     * creation, password hashing) is delegated to the standard registration
     * service in a follow-up wire-up. This method is the gate that confirms a
     * valid token is being redeemed; the controller calls the registration
     * pipeline AFTER this returns success.</p>
     */
    @Transactional
    public BetaAccessRequest completeBetaSignup(BetaSignupCommand cmd) {
        BetaAccessRequest entity = repository.findByInviteToken(cmd.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite token"));

        if (entity.getStatus() != BetaAccessRequestStatus.APPROVED) {
            throw new IllegalStateException(
                    "Cannot complete signup in state " + entity.getStatus() + " (must be APPROVED)");
        }
        if (entity.isTokenExpired()) {
            throw new IllegalStateException("Invite token expired");
        }

        entity.setStatus(BetaAccessRequestStatus.SIGNED_UP);
        entity.setInviteToken(null);
        entity.setInviteTokenExpiry(null);
        BetaAccessRequest saved = repository.save(entity);
        log.info("Beta signup completed: id={} email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    /** Single-row read used by coordinator detail view. */
    public Optional<BetaAccessRequest> getById(Long id) {
        return repository.findById(id);
    }
}
