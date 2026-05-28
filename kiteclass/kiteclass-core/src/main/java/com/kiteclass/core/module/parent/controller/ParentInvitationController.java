package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.InviteParentRequest;
import com.kiteclass.core.module.parent.dto.ParentInvitationResponse;
import com.kiteclass.core.module.parent.dto.RedeemInvitationRequest;
import com.kiteclass.core.module.parent.dto.RedeemInvitationResult;
import com.kiteclass.core.module.parent.service.ParentInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST surface for the parent-invitation flow.
 *
 * <p>Two endpoints are exposed here:
 * <ul>
 *   <li>{@code POST /api/v1/parent-invitations} — admin / teacher issues an
 *       invitation for a specific child. Authentication + role checks happen
 *       at the Gateway layer; Core trusts the {@code X-User-Id} header.</li>
 *   <li>{@code POST /api/v1/parent-invitations/redeem/{token}} — public
 *       endpoint consumed by the Gateway during parent signup. The public
 *       Gateway route is {@code POST /api/v1/auth/register-parent/{token}};
 *       it forwards the caller's tenant context and password here.</li>
 * </ul>
 *
 * @since 2.14.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parent-invitations")
@RequiredArgsConstructor
@Tag(name = "Parent Invitation", description = "Parent onboarding via token-based invitations (GAP-052a)")
public class ParentInvitationController {

    private final ParentInvitationService invitationService;

    /**
     * Issues an invitation. The inviter (admin/teacher) must be authenticated
     * via the Gateway; their user id arrives on {@code X-User-Id}.
     */
    @PostMapping
    @Operation(summary = "Invite a parent to link to a student")
    public ResponseEntity<ApiResponse<ParentInvitationResponse>> invite(
            @Valid @RequestBody InviteParentRequest request) {

        UUID tenantId = TenantContext.getCurrentTenant();
        UUID inviterId = UserContext.getCurrentUser();
        if (inviterId == null) {
            // The gateway is expected to have enforced auth already; bail out
            // explicitly rather than silently creating an un-attributed row.
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }

        ParentInvitationResponse response = invitationService.invite(
                tenantId, request.studentId(), request.parentEmail(), inviterId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Lời mời đã được gửi"));
    }

    /**
     * Public redemption endpoint. The tenant context is mandatory here too —
     * the redeeming user arrives via a tenant-scoped sub-domain, so the
     * Gateway layer populates {@code X-Tenant-Id} before forwarding.
     */
    @PostMapping("/redeem/{token}")
    @Operation(summary = "Redeem a parent invitation token (public)")
    public ResponseEntity<ApiResponse<RedeemInvitationResult>> redeem(
            @PathVariable String token,
            @Valid @RequestBody RedeemInvitationRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = true) String tenantHeader) {

        UUID tenantId = UUID.fromString(tenantHeader);
        RedeemInvitationResult result = invitationService.redeem(tenantId, token, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Kích hoạt tài khoản thành công"));
    }
}
