package com.kitehub.subscription.staff.service;

import com.kitehub.subscription.staff.entity.StaffInvitation;
import com.kitehub.subscription.staff.entity.StaffInvitationStatus;
import com.kitehub.subscription.staff.repository.StaffInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Staff invitation domain service skeleton (Wave 79 Bucket B).
 *
 * <p>Implements the BR-ROLE-INVITE-001..005 contracts from
 * {@code documents/01-business/roles/rules.md}.</p>
 *
 * <p><strong>Scope of this skeleton:</strong> entity persistence, token
 * hashing, lifecycle transitions. Full happy-path orchestration (email
 * dispatch via {@code kitehub-email}, user account creation on accept,
 * audit logging) is deferred to follow-up GAP-561b Wave 80 Bucket — see
 * §10 of {@code documents/04-quality/gaps/GAP-561-*.md}.</p>
 *
 * @since Wave 79 — GAP-561
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffInvitationService {

    /** Per BR-ROLE-INVITE-LIMIT — Phase 1 BETA cap per tenant. */
    public static final int STAFF_MAX_PER_TENANT = 50;

    /** Per BR-ROLE-INVITE-TTL — default invitation validity. */
    public static final int INVITATION_TTL_DAYS = 7;

    private final StaffInvitationRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Issue a new staff invitation.
     *
     * <p>Returns the persisted invitation along with the raw token to be
     * emailed to the recipient. The raw token is NEVER persisted — only its
     * SHA-256 hash. Callers must dispatch the raw token immediately and
     * discard.</p>
     *
     * @param tenantId target tenant
     * @param invitedBy issuing owner user id
     * @param email recipient email (validated by caller)
     * @param fullName recipient full name
     * @return pair of saved invitation + raw token to email
     */
    @Transactional
    public InvitationIssued create(UUID tenantId, UUID invitedBy, String email, String fullName) {
        // BR-ROLE-INVITE-IDEMPOTENT: same tenant+email pending → 409 at controller
        Optional<StaffInvitation> existing = repository.findPendingByTenantAndEmail(tenantId, email);
        if (existing.isPresent()) {
            throw new IllegalStateException("INVITATION_ALREADY_PENDING");
        }
        long active = repository.countByTenantIdAndStatus(tenantId, StaffInvitationStatus.PENDING)
                + repository.countByTenantIdAndStatus(tenantId, StaffInvitationStatus.ACCEPTED);
        if (active >= STAFF_MAX_PER_TENANT) {
            throw new IllegalStateException("STAFF_LIMIT_REACHED");
        }

        String rawToken = generateRawToken();
        StaffInvitation inv = StaffInvitation.builder()
                .tenantId(tenantId)
                .invitedBy(invitedBy)
                .email(email.toLowerCase().trim())
                .fullName(fullName.trim())
                .tokenHash(hashToken(rawToken))
                .status(StaffInvitationStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusDays(INVITATION_TTL_DAYS))
                .build();

        StaffInvitation saved = repository.save(inv);
        log.info("Staff invitation created tenantId={} invitationId={} expiresAt={}",
                tenantId, saved.getId(), saved.getExpiresAt());
        return new InvitationIssued(saved, rawToken);
    }

    /** Lookup pending invitation by raw token (rehashes for comparison). */
    @Transactional(readOnly = true)
    public Optional<StaffInvitation> findByRawToken(String rawToken) {
        return repository.findByTokenHash(hashToken(rawToken));
    }

    /** Owner revokes a pending invitation. Idempotent — already-revoked returns existing. */
    @Transactional
    public StaffInvitation revoke(UUID invitationId, UUID revokedByUserId) {
        StaffInvitation inv = repository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("INVITATION_NOT_FOUND"));
        if (inv.getStatus() == StaffInvitationStatus.REVOKED) {
            return inv;
        }
        if (inv.getStatus() != StaffInvitationStatus.PENDING) {
            throw new IllegalStateException("INVITATION_NOT_REVOCABLE");
        }
        inv.setStatus(StaffInvitationStatus.REVOKED);
        inv.setRevokedAt(OffsetDateTime.now());
        inv.setRevokedBy(revokedByUserId);
        return repository.save(inv);
    }

    /** All invitations for a tenant, newest first. */
    @Transactional(readOnly = true)
    public List<StaffInvitation> listByTenant(UUID tenantId) {
        return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    /** Lookup pending invitation by raw token sent in email. */
    @Transactional(readOnly = true)
    public Optional<StaffInvitation> findActiveByToken(String rawToken) {
        return repository.findByTokenHash(hashToken(rawToken))
                .filter(inv -> inv.getStatus() == StaffInvitationStatus.PENDING && !inv.isExpired());
    }

    // ---- helpers ----

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex of token. Used both at create + at accept-time comparison. */
    static String hashToken(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is part of every JVM; if absent the JVM is broken.
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }

    /** Result wrapper for {@link #create}. */
    public record InvitationIssued(StaffInvitation invitation, String rawToken) {}
}
