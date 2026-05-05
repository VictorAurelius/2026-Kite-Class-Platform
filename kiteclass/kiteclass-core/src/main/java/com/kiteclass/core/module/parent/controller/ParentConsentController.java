package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ParentalConsent;
import com.kiteclass.core.module.parent.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Parent-side settings surface for PDPL granular consent (Wave 19 —
 * GAP-321c Phase 1C v1).
 *
 * <p>{@code GET /api/v1/parent/consent?childId=...} → returns the
 * current {@link ParentalConsent} blob for the requested child.
 *
 * <p>{@code PUT /api/v1/parent/consent?childId=...} with body
 * <code>{"updates": {"fees": true, "conduct": false}}</code> →
 * applies the partial update + bumps the version.
 *
 * <p>BR-PARENT-PORTAL-011..012 — scope guard reuses the link existence
 * check inside ConsentService (a parent without an active link receives
 * 404 PARENT_CONSENT_LINK_NOT_FOUND on PUT and the default consent on
 * GET).
 *
 * @author KiteClass Team
 * @since 2.19.0 (Wave 19 — GAP-321c Phase 1C v1)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parent/consent")
@RequiredArgsConstructor
@Tag(name = "Parent Consent",
        description = "PDPL granular consent (GAP-321c Phase 1C v1)")
public class ParentConsentController {

    private final ConsentService consentService;

    @GetMapping
    @Operation(summary = "Read current parental consent for a linked child",
            description = "BR-PARENT-PORTAL-011: returns default consent when no link exists.")
    public ResponseEntity<ApiResponse<ParentalConsent>> getConsent(
            @RequestParam @NotNull Long childId,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId) {
        Long id = requireParentId(parentId);
        return ResponseEntity.ok(ApiResponse.success(
                consentService.getConsent(id, childId)));
    }

    @PutMapping
    @Operation(summary = "Update per-field parental consent flags + bump version",
            description = "BR-PARENT-PORTAL-012: 404 if parent is not linked to child.")
    public ResponseEntity<ApiResponse<ParentalConsent>> updateConsent(
            @RequestParam @NotNull Long childId,
            @RequestBody @NotNull UpdateRequest body,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId) {
        Long id = requireParentId(parentId);
        if (body == null || body.updates() == null) {
            throw new BusinessException("BAD_REQUEST", HttpStatus.BAD_REQUEST);
        }
        ParentalConsent next = consentService.bumpConsent(id, childId, body.updates());
        return ResponseEntity.ok(ApiResponse.success(next));
    }

    /**
     * Request body for PUT — sparse map of field → granted/revoked.
     */
    public record UpdateRequest(Map<String, Boolean> updates) {
    }

    private Long requireParentId(Long parentId) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return parentId;
    }
}
