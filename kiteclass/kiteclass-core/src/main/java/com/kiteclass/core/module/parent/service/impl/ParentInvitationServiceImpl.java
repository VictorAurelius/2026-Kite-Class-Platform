package com.kiteclass.core.module.parent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.ParentInvitationStatus;
import com.kiteclass.core.common.constant.ParentLinkType;
import com.kiteclass.core.common.constant.ParentRelationship;
import com.kiteclass.core.common.constant.ParentStatus;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.common.exception.DuplicateResourceException;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.outbox.OutboxEventWriter;
import com.kiteclass.core.module.auth.service.AuthCredentialProvisioningService;
import com.kiteclass.core.module.parent.config.ParentPortalProperties;
import com.kiteclass.core.module.parent.dto.ParentInvitationResponse;
import com.kiteclass.core.module.parent.dto.RedeemInvitationRequest;
import com.kiteclass.core.module.parent.dto.RedeemInvitationResult;
import com.kiteclass.core.module.parent.entity.Parent;
import com.kiteclass.core.module.parent.entity.ParentInvitation;
import com.kiteclass.core.module.parent.entity.ParentStudentLink;
import com.kiteclass.core.module.parent.event.ParentInvitationEmailEvent;
import com.kiteclass.core.module.parent.repository.ParentInvitationRepository;
import com.kiteclass.core.module.parent.repository.ParentRepository;
import com.kiteclass.core.module.parent.repository.ParentStudentLinkRepository;
import com.kiteclass.core.module.parent.service.ParentInvitationService;
import com.kiteclass.core.module.student.entity.Student;
import com.kiteclass.core.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link ParentInvitationService}.
 *
 * <p>Publishes invitation emails via RabbitMQ on the shared {@code
 * email.exchange} exchange with routing key {@code email.send} (see
 * {@code kitehub-subscription}'s {@code EmailQueueConfig}). Publishing is
 * best-effort — the invitation row is the source of truth; if the email
 * worker is down, an admin can regenerate the email from the stored token.
 *
 * <p>The expiry sweeper runs hourly ({@code @Scheduled(fixedRateString =
 * "${kiteclass.parent-portal.expire-sweep-ms:3600000}")}). Because this is a
 * single-instance job, an InnoDB-style advisory lock isn't required at MVP —
 * Wave 5 will wire it into the existing scheduler-lock infra if needed.
 *
 * @since 2.14.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentInvitationServiceImpl implements ParentInvitationService {

    static final String EMAIL_EXCHANGE = "email.exchange";
    static final String EMAIL_ROUTING_KEY = "email.send";
    static final String EMAIL_TEMPLATE = "parent-invitation";
    static final String EMAIL_TYPE = "parent-invitation";

    private final ParentRepository parentRepository;
    private final ParentInvitationRepository invitationRepository;
    private final ParentStudentLinkRepository linkRepository;
    private final StudentRepository studentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxEventWriter outbox;
    private final ObjectMapper objectMapper;
    private final ParentPortalProperties properties;
    private final AuthCredentialProvisioningService credentialProvisioning;

    @Override
    @Transactional
    public ParentInvitationResponse invite(
            UUID tenantId,
            Long studentId,
            String parentEmail,
            UUID invitedByUserId) {

        requireEnabled();
        log.info("Creating parent invitation: studentId={}, email={}, tenantId={}",
                studentId, parentEmail, tenantId);

        Student student = studentRepository.findByIdAndDeletedFalse(studentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "STUDENT_NOT_FOUND", (Object) studentId));

        // BR-PARENT-INV-002 — reject invite if a fully-redeemed Parent already
        // owns this email in the tenant. PENDING invitations are fine to
        // re-issue (e.g., user lost first email).
        if (parentRepository.existsByEmailAndInstanceIdAndDeletedFalse(parentEmail, tenantId)) {
            log.warn("Parent already exists for email {} in tenant {}", parentEmail, tenantId);
            throw new DuplicateResourceException("PARENT_EMAIL_EXISTS", (Object) parentEmail);
        }

        Instant now = Instant.now();
        ParentInvitation invitation = ParentInvitation.builder()
                .email(parentEmail)
                .student(student)
                .token(UUID.randomUUID().toString())
                .status(ParentInvitationStatus.PENDING)
                .expiresAt(now.plus(properties.invitationTtlHours(), ChronoUnit.HOURS))
                .invitedByUserId(invitedByUserId)
                .build();
        invitation.setInstanceId(tenantId);

        ParentInvitation saved = invitationRepository.save(invitation);
        publishInvitationEmail(saved, student);

        log.info("Parent invitation created: id={}, token={}, expiresAt={}",
                saved.getId(), saved.getToken(), saved.getExpiresAt());
        return toResponse(saved, student.getName(), /* includeToken = */ true);
    }

    @Override
    @Transactional
    public RedeemInvitationResult redeem(
            UUID tenantId,
            String token,
            RedeemInvitationRequest request) {

        requireEnabled();
        log.info("Redeeming parent invitation: token=***, tenantId={}", tenantId);

        ParentInvitation invitation = invitationRepository.findByTokenAndDeletedFalse(token)
                .orElseThrow(() -> new BusinessException(
                        "PARENT_INVITATION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!invitation.getInstanceId().equals(tenantId)) {
            // Defense in depth — the tenant filter should already have hidden
            // this row, but if the caller somehow bypassed it (e.g., raw
            // internal call), we still refuse.
            log.warn("Cross-tenant redemption attempt: invitationTenant={}, callerTenant={}",
                    invitation.getInstanceId(), tenantId);
            throw new BusinessException(
                    "PARENT_INVITATION_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (invitation.getStatus() != ParentInvitationStatus.PENDING) {
            throw new BusinessException(
                    "PARENT_INVITATION_ALREADY_USED", HttpStatus.BAD_REQUEST);
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(ParentInvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BusinessException(
                    "PARENT_INVITATION_EXPIRED", HttpStatus.BAD_REQUEST);
        }

        // Re-use an existing Parent when one already exists for this email
        // (e.g., second child redeems the same parent address).
        Parent parent = parentRepository
                .findByEmailAndInstanceIdAndDeletedFalse(invitation.getEmail(), tenantId)
                .orElseGet(() -> createParent(tenantId, invitation, request));

        // Promote PENDING → ACTIVE on first redemption; ACTIVE rows stay as-is.
        if (parent.getStatus() == ParentStatus.PENDING) {
            parent.setStatus(ParentStatus.ACTIVE);
            // Patch profile on first redemption so the parent-supplied values
            // (name, phone, relationship) win over the placeholder row we may
            // have created earlier.
            parent.setFullName(request.fullName());
            parent.setPhoneNumber(nullIfBlank(request.phoneNumber()));
            parent.setRelationship(parseRelationship(request.relationship()));
            parent = parentRepository.save(parent);
        }

        // Provision the parent's login credential (Wave auth-1, Option B — GAP-725).
        // Atomic with the parent/link in this @Transactional flow. Idempotent on email:
        // a returning parent (second-child redeem) keeps their existing password.
        credentialProvisioning.provisionParent(
                parent.getId(), invitation.getEmail(), tenantId, request.password());

        // Create the link if one doesn't exist for (parent, student).
        Student student = invitation.getStudent();
        if (!linkRepository.existsByParentIdAndStudentIdAndDeletedFalse(parent.getId(), student.getId())) {
            ParentStudentLink link = ParentStudentLink.builder()
                    .parent(parent)
                    .student(student)
                    .linkType(ParentLinkType.PRIMARY)
                    .build();
            link.setInstanceId(tenantId);
            linkRepository.save(link);
        }

        invitation.setStatus(ParentInvitationStatus.REDEEMED);
        invitation.setRedeemedAt(Instant.now());
        invitation.setRedeemedParentId(parent.getId());
        invitationRepository.save(invitation);

        List<Long> linkedStudentIds = linkRepository.findStudentIdsByParentId(parent.getId());
        log.info("Parent invitation redeemed: parentId={}, linkedStudents={}",
                parent.getId(), linkedStudentIds.size());

        return new RedeemInvitationResult(
                parent.getId(),
                parent.getEmail(),
                parent.getFullName(),
                parent.getPhoneNumber(),
                parent.getRelationship().name(),
                linkedStudentIds
        );
    }

    @Override
    @Transactional
    public int expireStale() {
        List<ParentInvitation> stale = invitationRepository
                .findByStatusAndExpiresAtBeforeAndDeletedFalse(
                        ParentInvitationStatus.PENDING, Instant.now());
        for (ParentInvitation inv : stale) {
            inv.setStatus(ParentInvitationStatus.EXPIRED);
        }
        invitationRepository.saveAll(stale);
        if (!stale.isEmpty()) {
            log.info("Expired {} stale parent invitations", stale.size());
        }
        return stale.size();
    }

    /**
     * Hourly sweeper — delegates to {@link #expireStale()} so tests can call
     * the business method directly without triggering scheduler machinery.
     */
    @Scheduled(fixedRateString = "${kiteclass.parent-portal.expire-sweep-ms:3600000}")
    public void scheduledExpireStale() {
        if (!properties.enabled()) {
            return;
        }
        try {
            expireStale();
        } catch (Exception e) {
            log.error("Failed to expire stale parent invitations", e);
        }
    }

    // ——— helpers ————————————————————————————————————————————————

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new BusinessException(
                    "PARENT_PORTAL_DISABLED", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private Parent createParent(UUID tenantId, ParentInvitation invitation, RedeemInvitationRequest request) {
        Parent parent = Parent.builder()
                .email(invitation.getEmail())
                .fullName(request.fullName())
                .phoneNumber(nullIfBlank(request.phoneNumber()))
                .relationship(parseRelationship(request.relationship()))
                .status(ParentStatus.ACTIVE)
                .build();
        parent.setInstanceId(tenantId);
        return parentRepository.save(parent);
    }

    private void publishInvitationEmail(ParentInvitation invitation, Student student) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("parentEmail", invitation.getEmail());
        vars.put("studentName", student.getName());
        vars.put("redeemUrl", properties.redeemBaseUrl() + invitation.getToken());
        vars.put("expiresAt", invitation.getExpiresAt().toString());
        vars.put("tokenTtlHours", properties.invitationTtlHours());

        ParentInvitationEmailEvent event = new ParentInvitationEmailEvent(
                invitation.getInstanceId(),
                invitation.getEmail(),
                "Lời mời liên kết tài khoản phụ huynh - KiteClass",
                EMAIL_TEMPLATE,
                vars,
                EMAIL_TYPE,
                Instant.now()
        );

        // Per design-patterns.md §3.5.1 Exception A: outbox-row first (reliability net),
        // then best-effort fast-path RabbitMQ publish.
        try {
            String payload = objectMapper.writeValueAsString(event);
            outbox.enqueue(EMAIL_ROUTING_KEY, "ParentInvitation",
                    invitation.getId().toString(), payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize parent invitation event for {}: {}",
                    invitation.getEmail(), ex.getMessage());
            // Continue to direct publish — partial degradation beats losing the email entirely
        }

        try {
            // Best-effort fast-path; outbox is the reliability net.
            rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, EMAIL_ROUTING_KEY, event);
        } catch (Exception e) {
            log.warn("Direct publish of parent invitation email for {} failed — outbox will retry: {}",
                    invitation.getEmail(), e.getMessage());
        }
    }

    private ParentInvitationResponse toResponse(ParentInvitation inv, String studentName, boolean includeToken) {
        return new ParentInvitationResponse(
                inv.getId(),
                inv.getEmail(),
                inv.getStudent().getId(),
                studentName,
                inv.getStatus().name(),
                inv.getExpiresAt(),
                includeToken ? inv.getToken() : null
        );
    }

    private static ParentRelationship parseRelationship(String raw) {
        if (raw == null || raw.isBlank()) {
            return ParentRelationship.GUARDIAN;
        }
        return ParentRelationship.valueOf(raw.trim().toUpperCase());
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
