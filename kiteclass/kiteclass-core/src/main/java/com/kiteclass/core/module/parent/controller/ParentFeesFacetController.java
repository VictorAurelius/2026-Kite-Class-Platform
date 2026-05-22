package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ParentFeeFacetResponse;
import com.kiteclass.core.module.parent.service.ParentFeesFacetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Read-only fees facet for the parent portal (GAP-321b Phase 1B
 * foundation — Wave 18b2 Bucket C).
 *
 * <p>Phase 1B v1: invoices are paged from existing {@code Invoice} table;
 * date-range narrowing accepted in the API surface but not yet applied
 * (concrete query in GAP-321b.1). The contract is published now so the FE
 * fee drill-down can wire against it.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
@Tag(name = "Parent Fees", description = "Parent-side invoice reads (GAP-321b Phase 1B)")
public class ParentFeesFacetController {

    private final ParentFeesFacetService service;

    @PreAuthorize("@authz.hasAccessToChild(#childId)")
    @GetMapping("/children/{childId}/fees")
    @Operation(summary = "List fees/invoices for one of the parent's linked children",
            description = "BR-PARENT-FACET-FEES-001: 403 PARENT_FACET_FORBIDDEN if parent is not linked. "
                    + "Per-resource authz via @authz.hasAccessToChild (OWASP A01 defense-in-depth) — Wave 105 Bucket E0.")
    public ResponseEntity<ApiResponse<Page<ParentFeeFacetResponse>>> getChildFees(
            @PathVariable Long childId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId,
            @PageableDefault(size = 20, sort = {"id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Long id = requireParentId(parentId);
        Page<ParentFeeFacetResponse> page =
                service.getFeesForChild(id, childId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    private Long requireParentId(Long parentId) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return parentId;
    }
}
