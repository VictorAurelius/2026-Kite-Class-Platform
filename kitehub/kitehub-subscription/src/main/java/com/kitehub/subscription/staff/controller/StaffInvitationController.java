package com.kitehub.subscription.staff.controller;

import com.kitehub.platform.domain.entity.User;
import com.kitehub.subscription.repository.UserRepository;
import com.kitehub.subscription.staff.dto.AcceptStaffInvitationRequest;
import com.kitehub.subscription.staff.dto.AcceptStaffInvitationResponse;
import com.kitehub.subscription.staff.dto.CreateStaffInvitationRequest;
import com.kitehub.subscription.staff.dto.StaffInvitationResponse;
import com.kitehub.subscription.staff.entity.StaffInvitation;
import com.kitehub.subscription.staff.entity.StaffInvitationAuditEntry;
import com.kitehub.subscription.staff.entity.StaffInvitationAuditEntry.EventType;
import com.kitehub.subscription.staff.entity.StaffInvitationStatus;
import com.kitehub.subscription.staff.repository.StaffInvitationAuditRepository;
import com.kitehub.subscription.staff.service.StaffInvitationService;
import com.kitehub.subscription.staff.service.StaffInvitationService.InvitationIssued;
import com.kitehub.subscription.client.EmailServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Owner→Staff invitation flow (Wave 80 Bucket B, GAP-561b).
 *
 * <p>Schema source-of-truth: {@code documents/01-business/roles/api-contract.md}.
 * Matches MSW handler shape at
 * {@code kitehub/kitehub-frontend/src/test/msw/handlers/staff-invitations.ts}
 * (Wave 79 Bucket 0 Foundation, PR #1364).</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST   /api/v1/staff-invitations}                 — Owner only — issue invite</li>
 *   <li>{@code GET    /api/v1/staff-invitations}                 — Owner only — list own tenant</li>
 *   <li>{@code GET    /api/v1/staff-invitations/by-token/{token}}— Public (recipient preview)</li>
 *   <li>{@code POST   /api/v1/staff-invitations/{token}/accept}  — Public — accept + set password</li>
 *   <li>{@code POST   /api/v1/staff-invitations/{id}/resend}     — Owner only — re-send email</li>
 *   <li>{@code DELETE /api/v1/staff-invitations/{id}}            — Owner only — revoke</li>
 * </ul>
 *
 * <p>Security per {@code pre-launch-owasp-rest-hardening-checklist.md} §2.1
 * (A01 Broken Access Control): every Owner-only endpoint has explicit
 * {@code @PreAuthorize} with both canonical OWNER and legacy aliases
 * (PLATFORM_ADMIN, ADMIN) during 30-day backward-compat window (cutoff
 * 2026-06-14, Wave 81 cleanup).</p>
 *
 * <p>Tenant scoping: gateway-forwarded {@code X-Tenant-Id} header (same pattern
 * as {@code OnboardingProgressController}). Missing → 403
 * {@code TENANT_CONTEXT_MISSING}.</p>
 *
 * @since Wave 79 Bucket B — skeleton (501 stubs);
 *        Wave 80 Bucket B — full impl per GAP-561b.
 */
@RestController
@RequestMapping("/api/v1/staff-invitations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Staff Invitations", description = "Owner→Staff invitation flow (GAP-561 / GAP-561b)")
public class StaffInvitationController {

    /**
     * Spring Security SpEL accepting OWNER + legacy aliases.
     * Wave 81 cleanup will collapse to just {@code hasRole('OWNER')}.
     */
    static final String OWNER_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    /** VN date pattern: "Thứ Hai, 22/05/2026". */
    private static final DateTimeFormatter VN_DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));

    private final StaffInvitationService service;
    private final StaffInvitationAuditRepository auditRepository;
    private final UserRepository userRepository;
    private final EmailServiceClient emailServiceClient;

    @Value("${kitehub.staff.invitation.base-url:https://kitehub.me}")
    private String inviteBaseUrl;

    /**
     * Password encoder for new staff users created at invitation acceptance.
     * Bean defined inline (not in SecurityConfig) to avoid widening that config
     * surface; this is the only place we hash passwords for staff onboarding.
     */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ── Owner endpoints ─────────────────────────────────────────────────

    @Operation(summary = "Owner issues a staff invitation")
    @PostMapping
    @PreAuthorize(OWNER_AUTHZ)
    @Transactional
    public ResponseEntity<StaffInvitationResponse> create(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-User-Id", required = false) String ownerHeader,
            @Valid @RequestBody CreateStaffInvitationRequest request) {
        UUID tenantId = requireTenant(tenantHeader);
        UUID ownerId = requireUser(ownerHeader);

        // Idempotency: re-invite same email → revoke old PENDING + create new.
        revokePendingForEmail(tenantId, ownerId, request.getEmail());

        InvitationIssued issued = service.create(tenantId, ownerId,
                request.getEmail(), request.getFullName());
        StaffInvitation inv = issued.invitation();

        emitAudit(inv, EventType.CREATED, ownerId,
                "Invitation created for " + inv.getEmail());

        // Best-effort email dispatch — failure logged, invitation row still valid.
        dispatchInviteEmail(inv, issued.rawToken(), resolveOwnerName(ownerId),
                EventType.SENT, ownerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(StaffInvitationResponse.from(inv));
    }

    @Operation(summary = "Owner lists invitations for own tenant")
    @GetMapping
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<List<StaffInvitationResponse>> list(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader) {
        UUID tenantId = requireTenant(tenantHeader);
        List<StaffInvitationResponse> rows = service.listByTenant(tenantId).stream()
                .map(StaffInvitationResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rows);
    }

    @Operation(summary = "Owner re-sends invitation email (rotates token)")
    @PostMapping("/{id}/resend")
    @PreAuthorize(OWNER_AUTHZ)
    @Transactional
    public ResponseEntity<StaffInvitationResponse> resend(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-User-Id", required = false) String ownerHeader,
            @PathVariable("id") UUID invitationId) {
        UUID tenantId = requireTenant(tenantHeader);
        UUID ownerId = requireUser(ownerHeader);

        StaffInvitation existing = service.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("INVITATION_NOT_FOUND"));
        if (!existing.getTenantId().equals(tenantId)) {
            throw new InvitationNotFoundException("INVITATION_NOT_FOUND");
        }

        InvitationIssued issued = service.resend(invitationId);
        StaffInvitation inv = issued.invitation();

        emitAudit(inv, EventType.RESENT, ownerId,
                "Invitation token rotated + email re-dispatched");

        dispatchInviteEmail(inv, issued.rawToken(), resolveOwnerName(ownerId),
                EventType.SENT, ownerId);

        return ResponseEntity.ok(StaffInvitationResponse.from(inv));
    }

    @Operation(summary = "Owner revokes a pending invitation")
    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER_AUTHZ)
    @Transactional
    public ResponseEntity<Void> revoke(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestHeader(value = "X-User-Id", required = false) String ownerHeader,
            @PathVariable("id") UUID id) {
        UUID tenantId = requireTenant(tenantHeader);
        UUID ownerId = requireUser(ownerHeader);

        StaffInvitation existing = service.findById(id)
                .orElseThrow(() -> new InvitationNotFoundException("INVITATION_NOT_FOUND"));
        if (!existing.getTenantId().equals(tenantId)) {
            throw new InvitationNotFoundException("INVITATION_NOT_FOUND");
        }

        StaffInvitation revoked = service.revoke(id, ownerId);
        emitAudit(revoked, EventType.REVOKED, ownerId,
                "Invitation revoked by owner");
        return ResponseEntity.noContent().build();
    }

    // ── Public endpoints (token-validated) ───────────────────────────────

    @Operation(summary = "Recipient fetches invitation details by token (public)")
    @GetMapping("/by-token/{token}")
    public ResponseEntity<StaffInvitationResponse> getByToken(@PathVariable("token") String token) {
        Optional<StaffInvitation> inv = service.findActiveByToken(token);
        return inv.map(i -> ResponseEntity.ok(StaffInvitationResponse.from(i)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.GONE).build());
    }

    @Operation(summary = "Recipient accepts invitation + sets password (public)")
    @PostMapping("/{token}/accept")
    @Transactional
    public ResponseEntity<AcceptStaffInvitationResponse> accept(
            @PathVariable("token") String token,
            @Valid @RequestBody AcceptStaffInvitationRequest request) {
        StaffInvitation inv = service.findActiveByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException("INVALID_OR_EXPIRED_TOKEN"));

        // Defense in depth: enforce password complexity per §2.3 OWASP A07.
        validatePasswordOrThrow(request.getPassword());

        // Reject duplicate user creation — should not happen unless concurrent accept.
        if (userRepository.existsByEmail(inv.getEmail())) {
            throw new InvitationStateException("USER_ALREADY_EXISTS");
        }

        // Create user row scoped to invitation's tenant + STAFF role.
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(inv.getEmail())
                .name(request.getFullName() == null ? inv.getFullName() : request.getFullName())
                .role("STAFF")
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .emailVerified(true)
                .build();
        User saved = userRepository.save(user);

        StaffInvitation accepted = service.accept(inv.getId(), saved.getId());
        emitAudit(accepted, EventType.ACCEPTED, null,
                "Invitation accepted; user " + saved.getId() + " created");

        return ResponseEntity.ok(AcceptStaffInvitationResponse.builder()
                .userId(saved.getId())
                .email(saved.getEmail())
                .fullName(saved.getName())
                .role(saved.getRole())
                .tenantId(accepted.getTenantId())
                .build());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private void revokePendingForEmail(UUID tenantId, UUID ownerId, String email) {
        service.listByTenant(tenantId).stream()
                .filter(i -> i.getStatus() == StaffInvitationStatus.PENDING
                        && email.toLowerCase().trim().equals(i.getEmail()))
                .findFirst()
                .ifPresent(stale -> {
                    StaffInvitation revoked = service.revoke(stale.getId(), ownerId);
                    emitAudit(revoked, EventType.REVOKED, ownerId,
                            "Auto-revoked on re-invite (idempotency)");
                });
    }

    private void dispatchInviteEmail(StaffInvitation inv, String rawToken,
                                     String ownerName, EventType audit, UUID actorUserId) {
        String inviteUrl = inviteBaseUrl + "/staff/accept-invite?token=" + rawToken;
        String expiresHuman = inv.getExpiresAt() == null
                ? ""
                : inv.getExpiresAt().format(VN_DATE_FMT);

        emailServiceClient.sendInviteStaffEmail(
                inv.getEmail(),
                inv.getFullName(),
                ownerName,
                resolveTenantName(inv.getTenantId()),
                "STAFF",
                inviteUrl,
                expiresHuman);

        emitAudit(inv, audit, actorUserId, "invite-staff email dispatched");
    }

    private String resolveOwnerName(UUID ownerId) {
        if (ownerId == null) {
            return "Chủ trung tâm";
        }
        return userRepository.findById(ownerId)
                .map(User::getName)
                .orElse("Chủ trung tâm");
    }

    /**
     * Display name fallback: tenant slug not always reachable from this module;
     * Phase 1 BETA uses generic label. Phase 1.5 will plumb branding/tenant
     * service for the real display name.
     */
    private String resolveTenantName(UUID tenantId) {
        return "Trung tâm KiteHub";
    }

    private void emitAudit(StaffInvitation inv, EventType eventType,
                           UUID actorUserId, String details) {
        StaffInvitationAuditEntry entry = StaffInvitationAuditEntry.builder()
                .invitationId(inv.getId())
                .tenantId(inv.getTenantId())
                .email(inv.getEmail())
                .eventType(eventType)
                .actorUserId(actorUserId)
                .details(details)
                .build();
        auditRepository.save(entry);
    }

    private UUID requireTenant(String tenantHeader) {
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new TenantContextMissingException("X-Tenant-Id header missing");
        }
        try {
            return UUID.fromString(tenantHeader);
        } catch (IllegalArgumentException ex) {
            throw new TenantContextMissingException("X-Tenant-Id header malformed");
        }
    }

    private UUID requireUser(String userHeader) {
        if (userHeader == null || userHeader.isBlank()) {
            // Owner not identified — should be set by gateway. Allow null for legacy
            // callers; downstream audit shows actorUserId=null which is acceptable
            // for Phase 1 BETA.
            return null;
        }
        try {
            return UUID.fromString(userHeader);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void validatePasswordOrThrow(String password) {
        if (password == null || password.length() < 12) {
            throw new InvitationValidationException("WEAK_PASSWORD",
                    "Password must be at least 12 characters");
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!(hasUpper && hasLower && hasDigit)) {
            throw new InvitationValidationException("WEAK_PASSWORD",
                    "Password must mix upper, lower, digit");
        }
    }

    // ── exception handlers ──────────────────────────────────────────────

    static class TenantContextMissingException extends RuntimeException {
        TenantContextMissingException(String message) { super(message); }
    }

    static class InvitationNotFoundException extends RuntimeException {
        InvitationNotFoundException(String code) { super(code); }
    }

    static class InvitationStateException extends RuntimeException {
        InvitationStateException(String code) { super(code); }
    }

    static class InvitationValidationException extends RuntimeException {
        final String code;
        InvitationValidationException(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    @ExceptionHandler(TenantContextMissingException.class)
    public ResponseEntity<ProblemDetail> handleTenantMissing(TenantContextMissingException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        body.setProperty("error", "TENANT_CONTEXT_MISSING");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(InvitationNotFoundException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        body.setProperty("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InvitationStateException.class)
    public ResponseEntity<ProblemDetail> handleStateConflict(InvitationStateException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        body.setProperty("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvitationValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(InvitationValidationException ex) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        body.setProperty("error", ex.code);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        String code = ex.getMessage() != null ? ex.getMessage() : "INVALID_STATE";
        HttpStatus status = switch (code) {
            case "INVITATION_ALREADY_PENDING", "INVITATION_ALREADY_USED",
                 "INVITATION_REVOKED", "USER_ALREADY_EXISTS"
                -> HttpStatus.CONFLICT;
            case "INVITATION_EXPIRED" -> HttpStatus.GONE;
            case "STAFF_LIMIT_REACHED" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "INVITATION_NOT_REVOCABLE", "INVITATION_NOT_RESENDABLE"
                -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, code);
        body.setProperty("error", code);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        String code = ex.getMessage() != null ? ex.getMessage() : "INVALID_REQUEST";
        HttpStatus status = "INVITATION_NOT_FOUND".equals(code)
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, code);
        body.setProperty("error", code);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage() == null ? "INVALID_REQUEST" : fe.getDefaultMessage())
                .orElse("INVALID_REQUEST");
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        body.setProperty("error", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
