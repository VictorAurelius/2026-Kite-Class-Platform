package com.kiteclass.core.module.staff.controller;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.staff.dto.AcceptStaffInviteRequest;
import com.kiteclass.core.module.staff.dto.AcceptStaffInviteResult;
import com.kiteclass.core.module.staff.dto.InviteStaffRequest;
import com.kiteclass.core.module.staff.dto.StaffInvitationResponse;
import com.kiteclass.core.module.staff.entity.StaffInvitation;
import com.kiteclass.core.module.staff.repository.StaffInvitationRepository;
import com.kiteclass.core.module.staff.service.StaffInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * REST surface for the staff-invitation flow (Wave meta-6 Bucket A — GAP-772).
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST   /api/v1/staff-invitations}              — Owner issues an invite</li>
 *   <li>{@code GET    /api/v1/staff-invitations}              — Owner lists PENDING invites</li>
 *   <li>{@code DELETE /api/v1/staff-invitations/{id}}         — Owner cancels (REVOKED)</li>
 *   <li>{@code POST   /api/v1/staff-invitations/{token}/accept} — Public staff acceptance
 *       (consumed by Gateway during {@code POST /api/v1/auth/register-staff/{token}})</li>
 * </ul>
 *
 * <p>Authorization model: write endpoints (invite + revoke) require ADMIN or
 * OWNER role; list is also restricted. Public accept endpoint requires only
 * the {@code X-Tenant-Id} header populated by the Gateway from the tenant
 * sub-domain — no auth required for the invitee (they don't yet have a
 * session).
 *
 * <p>Mirror of {@link com.kiteclass.core.module.parent.controller.ParentInvitationController}
 * adapted for staff scope (no student id; role-bearing invite).
 *
 * @since 2026-05-27 (Wave meta-6 Bucket A — GAP-772)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/staff-invitations")
@RequiredArgsConstructor
@Tag(name = "Staff Invitation",
        description = "Owner provisions STAFF/TEACHER/MANAGER role via token-based invitations (GAP-772)")
public class StaffInvitationController {

    private final StaffInvitationService invitationService;
    private final StaffInvitationRepository invitationRepository;

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "OWNER", "PLATFORM_ADMIN");

    /**
     * RBAC check matching kiteclass-core convention (gateway forwards roles
     * via X-User-Roles header; SecurityConfig is permitAll, so @PreAuthorize
     * has no Authentication object to evaluate). Mirrors the pattern from
     * VettingController.requireSafeguardingOfficer.
     */
    private static void requireOwnerOrAdmin(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            throw new BusinessException("ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }
        List<String> roles = Arrays.stream(rolesHeader.split("[,\\s]+"))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList();
        if (roles.stream().noneMatch(ALLOWED_ROLES::contains)) {
            throw new BusinessException("ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Owner issues an invitation. Inviter must be authenticated via Gateway;
     * their user id arrives on {@code X-User-Id}.
     */
    @PostMapping
    @Operation(summary = "Owner: issue a staff invitation")
    public ResponseEntity<ApiResponse<StaffInvitationResponse>> invite(
            @Valid @RequestBody InviteStaffRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        requireOwnerOrAdmin(roles);
        UUID tenantId = TenantContext.getCurrentTenant();
        // Bug #13 follow-up: kiteclass-core legacy UserContext uses ThreadLocal<Long>
        // but gateway forwards X-User-Id as UUID string (from JWT sub claim).
        // Result: UserContext.getCurrentUser() always null for UUID-style users.
        // Audit field invitedByUserId is nullable; allow null inviterId until the
        // Long→UUID refactor lands. Walk-unblock workaround.
        Long inviterId = UserContext.getCurrentUser();

        StaffInvitationResponse response = invitationService.invite(
                tenantId, request.email(), request.role(), inviterId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Lời mời nhân viên đã được gửi"));
    }

    /**
     * Owner-side list of PENDING invitations for the current tenant. Token
     * field omitted server-side to reduce leak surface.
     */
    @GetMapping
    @Operation(summary = "Owner: list pending staff invitations")
    public ResponseEntity<ApiResponse<List<StaffInvitationResponse>>> list(
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        requireOwnerOrAdmin(roles);
        UUID tenantId = TenantContext.getCurrentTenant();
        List<StaffInvitationResponse> rows = invitationService.listForTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    /**
     * Owner cancels a PENDING invitation (transition to REVOKED). Idempotent
     * surface: already-resolved rows return 409 so FE can distinguish states.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Owner: cancel a pending staff invitation")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        requireOwnerOrAdmin(roles);
        UUID tenantId = TenantContext.getCurrentTenant();
        invitationService.revoke(tenantId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Lời mời đã bị hủy"));
    }

    /**
     * Public preview endpoint — FE accept-invite page calls this to render
     * tenant/role/expiry before showing password form.
     *
     * <p>Bug #15 walk fix (2026-05-28): FE expects {@code GET /by-token/{token}}
     * shape {id, tenantId, email, fullName, role, status, expiresAt} but Wave
     * meta-6 BE shipped without it. Public — token IS the auth.
     */
    @GetMapping("/by-token/{token}")
    @Operation(summary = "Public: preview a staff invitation by token (FE accept page)")
    public ResponseEntity<Map<String, Object>> previewByToken(@PathVariable String token) {
        StaffInvitation inv = invitationRepository.findByTokenAndDeletedFalse(token)
                .orElseThrow(() -> new BusinessException("INVITATION_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(java.time.Instant.now())) {
            throw new BusinessException("INVITATION_EXPIRED", HttpStatus.GONE);
        }
        Map<String, Object> preview = new java.util.HashMap<>();
        preview.put("id", String.valueOf(inv.getId()));
        preview.put("tenantId", String.valueOf(inv.getInstanceId()));
        preview.put("email", inv.getEmail());
        preview.put("fullName", "");
        preview.put("role", inv.getRole());
        preview.put("status", inv.getStatus() != null ? inv.getStatus().name() : null);
        preview.put("expiresAt", inv.getExpiresAt() != null ? inv.getExpiresAt().toString() : null);
        return ResponseEntity.ok(preview);
    }

    /**
     * Public acceptance endpoint — consumed by Gateway. Tenant context is
     * mandatory and arrives via {@code X-Tenant-Id} header (Gateway populates
     * it from the tenant sub-domain). No invitee authentication required.
     */
    @PostMapping("/{token}/accept")
    @Operation(summary = "Public: accept a staff invitation (called by Gateway)")
    public ResponseEntity<ApiResponse<AcceptStaffInviteResult>> accept(
            @PathVariable String token,
            @Valid @RequestBody AcceptStaffInviteRequest request,
            @RequestHeader(value = "X-Tenant-Id") String tenantHeader) {

        UUID tenantId = UUID.fromString(tenantHeader);
        AcceptStaffInviteResult result = invitationService.accept(tenantId, token, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Kích hoạt tài khoản nhân viên thành công"));
    }
}
