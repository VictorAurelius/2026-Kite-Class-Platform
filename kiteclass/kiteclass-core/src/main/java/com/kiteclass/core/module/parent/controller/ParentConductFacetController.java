package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ParentConductFacetResponse;
import com.kiteclass.core.module.parent.service.ParentConductFacetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only conduct (hạnh kiểm) facet for the parent portal (GAP-321b
 * Phase 1B foundation — Wave 18b2 Bucket C).
 *
 * <p>Phase 1B v1 stub: backing schema for digital hạnh kiểm rating is not
 * yet present; endpoint returns an empty list after the scope guard
 * succeeds. The contract is published now so the FE conduct view can wire
 * against it. Concrete data source lands in GAP-321b.1.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
@Tag(name = "Parent Conduct", description = "Parent-side hạnh kiểm reads (GAP-321b Phase 1B)")
public class ParentConductFacetController {

    private final ParentConductFacetService service;

    @PreAuthorize("@authz.hasAccessToChild(#childId)")
    @GetMapping("/children/{childId}/conduct")
    @Operation(summary = "List conduct ratings for one of the parent's linked children",
            description = "BR-PARENT-FACET-CONDUCT-001: 403 PARENT_FACET_FORBIDDEN if parent is not linked. "
                    + "Per-resource authz via @authz.hasAccessToChild (OWASP A01 defense-in-depth) — Wave 105 Bucket E0.")
    public ResponseEntity<ApiResponse<List<ParentConductFacetResponse>>> getChildConduct(
            @PathVariable Long childId,
            @RequestParam(required = false) String period,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId) {
        Long id = requireParentId(parentId);
        List<ParentConductFacetResponse> items = service.getConductForChild(id, childId, period);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    private Long requireParentId(Long parentId) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return parentId;
    }
}
