package com.kitehub.subscription.staff.controller;

import com.kitehub.subscription.staff.dto.CreateStaffInvitationRequest;
import com.kitehub.subscription.staff.dto.StaffInvitationResponse;
import com.kitehub.subscription.staff.entity.StaffInvitation;
import com.kitehub.subscription.staff.service.StaffInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Owner→Staff invitation flow (Wave 79 Bucket B, GAP-561).
 *
 * <p>Schema source-of-truth: {@code documents/01-business/roles/api-contract.md}.
 * Matches MSW handler shape at
 * {@code kitehub/kitehub-frontend/src/test/msw/handlers/staff-invitations.ts}
 * (Wave 79 Bucket 0 Foundation, PR #1364).</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST   /api/v1/staff-invitations}                  — Owner only</li>
 *   <li>{@code GET    /api/v1/staff-invitations}                  — Owner only (list own tenant)</li>
 *   <li>{@code GET    /api/v1/staff-invitations/by-token/{token}} — Public (recipient lookup)</li>
 *   <li>{@code POST   /api/v1/staff-invitations/accept}           — Public + valid token</li>
 *   <li>{@code DELETE /api/v1/staff-invitations/{id}}             — Owner only (revoke)</li>
 * </ul>
 *
 * <p><strong>This is a skeleton controller</strong> — full happy-path
 * orchestration (email dispatch, user account creation on accept, audit log
 * wiring) is deferred to GAP-561b Wave 80 Bucket. Endpoints return canonical
 * shapes for the MSW handlers + Bucket 0 contract so FE bucket (this PR's
 * follow-up B2) can build against a stable contract.</p>
 *
 * <p>Security per {@code pre-launch-owasp-rest-hardening-checklist.md} §2.1
 * (A01 Broken Access Control): every Owner-only endpoint has explicit
 * {@code @PreAuthorize} with both canonical OWNER and legacy aliases
 * (PLATFORM_ADMIN, ADMIN) during 30-day backward-compat window (cutoff
 * 2026-06-14, Wave 81 cleanup).</p>
 *
 * @since Wave 79 — GAP-561 / GAP-562
 */
@RestController
@RequestMapping("/api/v1/staff-invitations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Staff Invitations", description = "Owner→Staff invitation flow (GAP-561 Wave 79)")
public class StaffInvitationController {

    /**
     * Spring Security SpEL accepting OWNER + legacy aliases.
     * Wave 81 cleanup will collapse to just {@code hasRole('OWNER')}.
     */
    static final String OWNER_AUTHZ =
            "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')";

    private final StaffInvitationService service;

    @Operation(summary = "Owner issues a staff invitation")
    @PostMapping
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<StaffInvitationResponse> create(
            @Valid @RequestBody CreateStaffInvitationRequest request) {
        // Skeleton: real impl resolves invitedBy + tenantId from SecurityContext;
        // GAP-561b follow-up wires kitehub-email dispatch with raw token.
        log.info("StaffInvitationController.create called (skeleton — GAP-561b follow-up)");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(summary = "Owner lists invitations for own tenant")
    @GetMapping
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<List<StaffInvitationResponse>> list() {
        // Skeleton: tenantId from SecurityContext required; GAP-561b follow-up.
        log.info("StaffInvitationController.list called (skeleton — GAP-561b follow-up)");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(summary = "Recipient fetches invitation details by token")
    @GetMapping("/by-token/{token}")
    public ResponseEntity<StaffInvitationResponse> getByToken(@PathVariable("token") String token) {
        return service.findActiveByToken(token)
                .map(inv -> ResponseEntity.ok(StaffInvitationResponse.from(inv)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Owner revokes a pending invitation")
    @DeleteMapping("/{id}")
    @PreAuthorize(OWNER_AUTHZ)
    public ResponseEntity<StaffInvitationResponse> revoke(@PathVariable("id") UUID id) {
        // Skeleton: real impl reads revokedBy from SecurityContext; GAP-561b follow-up.
        log.info("StaffInvitationController.revoke called id={} (skeleton — GAP-561b follow-up)", id);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Compiled-but-unused helper to ensure entity ↔ response mapping is
     * exercised at compile time so the skeleton DTOs don't bit-rot before
     * GAP-561b lands the full flow.
     */
    @SuppressWarnings("unused")
    private List<StaffInvitationResponse> toResponseList(List<StaffInvitation> entities) {
        return entities.stream()
                .map(StaffInvitationResponse::from)
                .collect(Collectors.toList());
    }
}
