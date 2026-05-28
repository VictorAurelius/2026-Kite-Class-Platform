package com.kiteclass.core.module.staff.service.impl;

import com.kiteclass.core.common.constant.StaffInvitationStatus;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.staff.dto.AcceptStaffInviteRequest;
import com.kiteclass.core.module.staff.dto.AcceptStaffInviteResult;
import com.kiteclass.core.module.staff.dto.StaffInvitationResponse;
import com.kiteclass.core.module.staff.entity.StaffInvitation;
import com.kiteclass.core.module.staff.repository.StaffInvitationRepository;
import com.kiteclass.core.module.staff.service.StaffInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default {@link StaffInvitationService} implementation.
 *
 * <p>Pattern mirrors {@code ParentInvitationServiceImpl} but without the
 * Parent + ParentStudentLink saga — staff identity is provisioned at the
 * Gateway layer (User row with role binding); kiteclass-core's job is only
 * to track the invitation lifecycle and validate the redemption flow.
 *
 * <p>No audit-service injection in this MVP — when staff-invitation audit
 * logging is added (sister GAP-659 split), the audit service MUST use
 * {@code Propagation.REQUIRES_NEW} per
 * {@code .claude/rules/audit-service-isolation.md}.
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffInvitationServiceImpl implements StaffInvitationService {

    private final StaffInvitationRepository invitationRepository;

    /**
     * Invitation TTL — default 168h (7 days). Override per environment via
     * {@code kiteclass.staff-invite.invitation-ttl-hours}.
     */
    @Value("${kiteclass.staff-invite.invitation-ttl-hours:168}")
    private long ttlHours;

    @Override
    @Transactional
    public StaffInvitationResponse invite(UUID tenantId, String email, String role, Long inviterId) {
        log.info("Issuing staff invitation: email={}, role={}, tenantId={}, inviterId={}",
                email, role, tenantId, inviterId);

        String normalizedEmail = email.trim().toLowerCase();

        StaffInvitation invitation = StaffInvitation.builder()
                .email(normalizedEmail)
                .role(role)
                .token(UUID.randomUUID().toString())
                .status(StaffInvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(Duration.ofHours(ttlHours)))
                .invitedByUserId(inviterId)
                .build();
        invitation.setInstanceId(tenantId);
        invitation = invitationRepository.save(invitation);

        log.info("Staff invitation created: id={}, expiresAt={}",
                invitation.getId(), invitation.getExpiresAt());

        return toResponse(invitation, /* includeToken */ true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffInvitationResponse> listForTenant(UUID tenantId) {
        log.debug("Listing staff invitations for tenantId={}", tenantId);

        // Hibernate filter clamps to current tenant; we additionally use
        // status-driven sorting to surface PENDING rows first for the
        // Owner-side dashboard.
        List<StaffInvitation> rows = invitationRepository
                .findByStatusAndDeletedFalseOrderByCreatedAtDesc(StaffInvitationStatus.PENDING);

        return rows.stream()
                .filter(r -> r.getInstanceId().equals(tenantId))
                .map(r -> toResponse(r, /* includeToken */ false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revoke(UUID tenantId, Long invitationId) {
        log.info("Revoking staff invitation: id={}, tenantId={}", invitationId, tenantId);

        StaffInvitation invitation = invitationRepository.findById(invitationId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "STAFF_INVITATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!invitation.getInstanceId().equals(tenantId)) {
            // Defense in depth — tenant filter should already have hidden this row.
            log.warn("Cross-tenant revoke attempt: invitationTenant={}, callerTenant={}",
                    invitation.getInstanceId(), tenantId);
            throw new BusinessException(
                    "STAFF_INVITATION_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (invitation.getStatus() != StaffInvitationStatus.PENDING) {
            // Already resolved — no-op per idempotent semantics; surface 409 so
            // FE can distinguish "already cancelled" from "successfully cancelled".
            throw new BusinessException(
                    "STAFF_INVITATION_NOT_PENDING", HttpStatus.CONFLICT);
        }

        invitation.setStatus(StaffInvitationStatus.REVOKED);
        invitationRepository.save(invitation);
        log.info("Staff invitation revoked: id={}", invitation.getId());
    }

    @Override
    @Transactional
    public AcceptStaffInviteResult accept(UUID tenantId, String token, AcceptStaffInviteRequest request) {
        log.info("Accepting staff invitation: token=***, tenantId={}", tenantId);

        StaffInvitation invitation = invitationRepository.findByTokenAndDeletedFalse(token)
                .orElseThrow(() -> new BusinessException(
                        "STAFF_INVITATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!invitation.getInstanceId().equals(tenantId)) {
            log.warn("Cross-tenant accept attempt: invitationTenant={}, callerTenant={}",
                    invitation.getInstanceId(), tenantId);
            throw new BusinessException(
                    "STAFF_INVITATION_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (invitation.getStatus() == StaffInvitationStatus.ACCEPTED) {
            throw new BusinessException(
                    "STAFF_INVITATION_ALREADY_ACCEPTED", HttpStatus.BAD_REQUEST);
        }
        if (invitation.getStatus() == StaffInvitationStatus.REVOKED) {
            throw new BusinessException(
                    "STAFF_INVITATION_REVOKED", HttpStatus.BAD_REQUEST);
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(StaffInvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BusinessException(
                    "STAFF_INVITATION_EXPIRED", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        invitation.setStatus(StaffInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(now);
        // BLOCKED ON GAP-786 — user provisioning on accept NOT IMPLEMENTED.
        // Service marks invitation ACCEPTED + sets acceptedAt but DOES NOT
        // create user record. Password from request dropped. Feature
        // non-functional end-to-end (staff cannot login after accept).
        // Architecture decision Option A/B/C pending — see GAP-786 §Proposed Fix.
        // NOTE: prior comment referenced GAP-779 which is unrelated (/me endpoint).
        invitationRepository.save(invitation);

        log.info("Staff invitation accepted: id={}, email={}, role={}",
                invitation.getId(), invitation.getEmail(), invitation.getRole());

        return new AcceptStaffInviteResult(
                invitation.getId(),
                tenantId,
                invitation.getEmail(),
                request.fullName(),
                invitation.getRole(),
                now
        );
    }

    private StaffInvitationResponse toResponse(StaffInvitation invitation, boolean includeToken) {
        return new StaffInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                includeToken ? invitation.getToken() : null,
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getInvitedByUserId(),
                invitation.getAcceptedAt(),
                invitation.getAcceptedUserId(),
                invitation.getCreatedAt()
        );
    }
}
