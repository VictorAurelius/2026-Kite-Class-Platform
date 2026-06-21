package com.kiteclass.core.module.k12.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.k12.dto.request.BulkPublishRequest;
import com.kiteclass.core.module.k12.dto.response.BulkPublishResponse;
import com.kiteclass.core.module.k12.entity.SubjectGrade;
import com.kiteclass.core.module.k12.exception.IllegalGradeTransitionException;
import com.kiteclass.core.module.k12.service.SubjectGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * SubjectGrade lifecycle endpoints — Tổ trưởng review + Hiệu trưởng publish
 * + bulk publish action (§360.4).
 *
 * <p>RBAC enforcement (Tổ trưởng-per-subject vs Hiệu trưởng) depends on
 * GAP-058 role hierarchy and is out of scope for §360.4 — callers' identity
 * is carried on {@code X-User-Reference-Id} (Gateway-injected). Phase 1C
 * remainder follow-up gap (360.2) wires real role checks.
 *
 * <p>Reference: api-contract.md endpoints + UC-GRADEBOOK-* in
 * {@code documents/01-business/kiteclass/multi-subject-gradebook/use-cases.md}.
 *
 * @since 5.x (Wave 24 Bucket B — GAP-360 §360.1 + §360.4)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/grades/subjects")
@RequiredArgsConstructor
@Tag(name = "Subject Gradebook", description = "K-12 multi-subject gradebook (GAP-360)")
public class SubjectGradeController {

    private final SubjectGradeService subjectGradeService;

    // GAP-1491 (OWASP A01): coarse method-level guard excludes STUDENT/PARENT + non-teaching
    // roles. Fine-grained Tổ trưởng-per-subject vs Hiệu trưởng RBAC remains GAP-058/360.2 scope.
    @PostMapping("/{id}/submit-for-review")
    @PreAuthorize("hasAnyRole('TEACHER', 'OWNER', 'ADMIN', 'PRINCIPAL', 'PLATFORM_ADMIN')")
    @Operation(summary = "GV bộ môn submits a DRAFT grade for Tổ trưởng review")
    public ResponseEntity<ApiResponse<Long>> submitForReview(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long submitterId) {
        Long submitter = requireUser(submitterId);
        Long gradeId = subjectGradeService.submitForReview(id, submitter);
        return ResponseEntity.ok(ApiResponse.success(gradeId, "Submitted for review"));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('TEACHER', 'OWNER', 'ADMIN', 'PRINCIPAL', 'PLATFORM_ADMIN')")
    @Operation(summary = "Tổ trưởng marks DRAFT → REVIEWED")
    public ResponseEntity<ApiResponse<SubjectGrade>> review(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long reviewerId) {
        Long reviewer = requireUser(reviewerId);
        SubjectGrade grade = subjectGradeService.review(id, reviewer);
        return ResponseEntity.ok(ApiResponse.success(grade, "Grade reviewed"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'OWNER', 'ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Hiệu trưởng marks REVIEWED → PUBLISHED")
    public ResponseEntity<ApiResponse<SubjectGrade>> publish(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long publisherId) {
        Long publisher = requireUser(publisherId);
        SubjectGrade grade = subjectGradeService.publish(id, publisher);
        return ResponseEntity.ok(ApiResponse.success(grade, "Grade published"));
    }

    @PostMapping("/bulk-publish")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'OWNER', 'ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Hiệu trưởng publishes a batch of REVIEWED grades — best-effort")
    public ResponseEntity<ApiResponse<BulkPublishResponse>> bulkPublish(
            @Valid @RequestBody BulkPublishRequest request,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long publisherId) {
        Long publisher = requireUser(publisherId);
        int published = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        for (Long gradeId : request.gradeIds()) {
            try {
                subjectGradeService.publish(gradeId, publisher);
                published++;
            } catch (IllegalGradeTransitionException ex) {
                skipped++;
                errors.add(gradeId + ": " + ex.getCode());
            } catch (BusinessException ex) {
                skipped++;
                errors.add(gradeId + ": " + ex.getCode());
            }
        }
        log.info("Bulk publish by user {}: {} published / {} skipped (of {} requested)",
                publisher, published, skipped, request.gradeIds().size());
        BulkPublishResponse body = new BulkPublishResponse(published, skipped, errors);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    private Long requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("AUTH_REQUIRED", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }
}
