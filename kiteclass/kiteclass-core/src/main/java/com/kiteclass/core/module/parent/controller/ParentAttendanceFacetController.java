package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.parent.service.ParentAttendanceFacetService;
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
 * Read-only attendance facet for the parent portal (GAP-321b Phase 1B
 * foundation — Wave 18b2 Bucket C).
 *
 * <p>Sister of {@code ParentTranscriptController}; identity carried on
 * {@code X-User-Reference-Id} (Gateway-injected from
 * {@code users.reference_id} when {@code userType = PARENT}). Scope guard
 * delegated to {@link ParentAttendanceFacetService} which enforces the
 * {@code ParentStudentLink} edge.
 *
 * @author KiteClass Team
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parent")
@RequiredArgsConstructor
@Tag(name = "Parent Attendance", description = "Parent-side period attendance reads (GAP-321b Phase 1B)")
public class ParentAttendanceFacetController {

    private final ParentAttendanceFacetService service;

    /**
     * Returns a page of period attendance entries for one of the parent's
     * linked children, restricted to {@code [from, to]}.
     *
     * <p>Returns:
     * <ul>
     *   <li>200 + page of {@link AttendancePeriodResponse}.</li>
     *   <li>401 {@code AUTH_REQUIRED} — header missing.</li>
     *   <li>400 {@code BAD_REQUEST} — childId/from/to missing or inverted.</li>
     *   <li>403 {@code PARENT_FACET_FORBIDDEN} — no active link.</li>
     * </ul>
     */
    @PreAuthorize("@authz.hasAccessToChild(#childId)")
    @GetMapping("/children/{childId}/attendance")
    @Operation(summary = "List period attendance for one of the parent's linked children",
            description = "BR-PARENT-FACET-ATT-001: 403 PARENT_FACET_FORBIDDEN if parent is not linked. "
                    + "Per-resource authz via @authz.hasAccessToChild (OWASP A01 defense-in-depth) — Wave 105 Bucket E0.")
    public ResponseEntity<ApiResponse<Page<AttendancePeriodResponse>>> getChildAttendance(
            @PathVariable Long childId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId,
            @PageableDefault(size = 50, sort = {"date", "periodNo"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Long id = requireParentId(parentId);
        Page<AttendancePeriodResponse> page =
                service.getAttendanceForChild(id, childId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    private Long requireParentId(Long parentId) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return parentId;
    }
}
