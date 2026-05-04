package com.kiteclass.core.module.childprotection.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.childprotection.dto.VettingCreateRequest;
import com.kiteclass.core.module.childprotection.dto.VettingResponse;
import com.kiteclass.core.module.childprotection.dto.VettingTransitionRequest;
import com.kiteclass.core.module.childprotection.entity.Vetting;
import com.kiteclass.core.module.childprotection.enums.VettingStatus;
import com.kiteclass.core.module.childprotection.service.VettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Vetting workflow REST endpoints (GAP-322b Phase 1B foundation).
 *
 * <p>RBAC (BR-VETTING-003): only callers carrying the
 * {@code SAFEGUARDING_OFFICER} role on the {@code X-User-Roles} header
 * forwarded by the Gateway may read or write Vetting records. Anyone else
 * receives 403 {@code VETTING_RBAC_DENIED}. Teachers without an APPROVED
 * vetting record are blocked from student-PII endpoints by a separate
 * filter (Phase 1B follow-up — out of scope for this foundation PR).
 *
 * <p>Phase 1B foundation scope: CRUD + state transition + soft delete. No
 * file upload UI yet (the {@code VettingDocumentStorage} contract is
 * satisfied by a stub — actual upload endpoint ships Phase 1B follow-up).
 *
 * @since Wave 18b2 Bucket B — GAP-322b Phase 1B foundation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vettings")
@RequiredArgsConstructor
@Tag(name = "Vetting", description = "Staff vetting workflow (GAP-322b Phase 1B foundation)")
public class VettingController {

    /** Role name authorised to read/write Vetting records. */
    static final String SAFEGUARDING_OFFICER = "SAFEGUARDING_OFFICER";

    private final VettingService vettingService;

    @GetMapping
    @Operation(summary = "List vetting records (RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<ApiResponse<Page<VettingResponse>>> list(
            @RequestParam(value = "status", required = false) VettingStatus status,
            Pageable pageable,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        requireSafeguardingOfficer(roles);
        Page<VettingResponse> page = vettingService.findAll(status, pageable)
                .map(VettingResponse::from);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vetting record by id (RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<ApiResponse<VettingResponse>> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        requireSafeguardingOfficer(roles);
        Vetting v = vettingService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(VettingResponse.from(v)));
    }

    @PostMapping
    @Operation(summary = "Create a new vetting record in PENDING (RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<ApiResponse<VettingResponse>> create(
            @Valid @RequestBody VettingCreateRequest req,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        requireSafeguardingOfficer(roles);
        Vetting created = vettingService.create(
                req.teacherId(),
                req.lltpNumber(),
                req.policeCheckDetails(),
                req.expiresAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(VettingResponse.from(created)));
    }

    @PatchMapping("/{id}/transition")
    @Operation(summary = "Transition vetting status per BR-VETTING-001 state machine "
            + "(RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<ApiResponse<VettingResponse>> transition(
            @PathVariable Long id,
            @Valid @RequestBody VettingTransitionRequest req,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long actorUserId
    ) {
        requireSafeguardingOfficer(roles);
        Vetting updated = vettingService.transition(id, req.targetStatus(), actorUserId);
        return ResponseEntity.ok(ApiResponse.success(VettingResponse.from(updated)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a vetting record (RBAC: SAFEGUARDING_OFFICER only)")
    public ResponseEntity<Void> softDelete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        requireSafeguardingOfficer(roles);
        vettingService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reject the request unless the {@code X-User-Roles} header contains the
     * SAFEGUARDING_OFFICER role. Header is comma- or space-separated; matching
     * is case-insensitive.
     *
     * @param rolesHeader value of the {@code X-User-Roles} header (may be null)
     * @throws BusinessException 403 {@code VETTING_RBAC_DENIED} if missing role
     */
    static void requireSafeguardingOfficer(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            throw new BusinessException("VETTING_RBAC_DENIED", HttpStatus.FORBIDDEN);
        }
        List<String> roles = Arrays.stream(rolesHeader.split("[,\\s]+"))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList();
        if (!roles.contains(SAFEGUARDING_OFFICER)) {
            throw new BusinessException("VETTING_RBAC_DENIED", HttpStatus.FORBIDDEN);
        }
    }
}
