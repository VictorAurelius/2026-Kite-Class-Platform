package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.FileComplaintRequest;
import com.kiteclass.core.module.parent.dto.ParentComplaintResponse;
import com.kiteclass.core.module.parent.service.ParentComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parent-side complaint write endpoint (Wave 19 — GAP-321c Phase 1C v1).
 *
 * <p>{@code POST /api/v1/parent/complaints} — accepts free-text
 * complaint scoped by linked-student id; persists a row in
 * {@code parent_complaint_queue}; returns the new id.
 *
 * <p>BR-PARENT-PORTAL-013 scope guard. Full workflow (4-level
 * escalation, attachments, resolver UI) lands in GAP-339.
 *
 * @since 2.19.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parent/complaints")
@RequiredArgsConstructor
@Tag(name = "Parent Complaints",
        description = "Parent complaint write surface v1 (GAP-321c)")
public class ParentComplaintController {

    private final ParentComplaintService service;

    @PostMapping
    @Operation(summary = "File a complaint scoped to a linked child",
            description = "BR-PARENT-PORTAL-013: 403 PARENT_FACET_FORBIDDEN if not linked.")
    public ResponseEntity<ApiResponse<ParentComplaintResponse>> fileComplaint(
            @Valid @RequestBody FileComplaintRequest request,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long parentId) {
        Long id = requireParentId(parentId);
        ParentComplaintResponse response = service.fileComplaint(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    private Long requireParentId(Long parentId) {
        if (parentId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return parentId;
    }
}
