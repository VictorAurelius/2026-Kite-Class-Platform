package com.kiteclass.core.module.clazz.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.clazz.dto.CancelClassRequest;
import com.kiteclass.core.module.clazz.dto.ClassCodeResponse;
import com.kiteclass.core.module.clazz.dto.ClassResponse;
import com.kiteclass.core.module.clazz.dto.ClassSessionResponse;
import com.kiteclass.core.module.clazz.dto.CreateClassRequest;
import com.kiteclass.core.module.clazz.dto.CreateScheduleRequest;
import com.kiteclass.core.module.clazz.dto.GenerateClassCodeRequest;
import com.kiteclass.core.module.clazz.dto.RecurrenceRuleDto;
import com.kiteclass.core.module.clazz.dto.RescheduleClassRequest;
import com.kiteclass.core.module.clazz.dto.UpdateClassRequest;
import com.kiteclass.core.module.clazz.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Class management.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>CRUD operations on classes within courses</li>
 *   <li>Lifecycle transitions (start, complete, cancel)</li>
 *   <li>Class code generation</li>
 *   <li>Schedule and session management</li>
 * </ul>
 *
 * <p>Base paths:
 * <ul>
 *   <li>/api/v1/courses/{courseId}/classes — class CRUD</li>
 *   <li>/api/v1/classes/{classId}/** — class operations</li>
 * </ul>
 *
 * <p><strong>OWASP A01 authz classification (GAP-837):</strong>
 * <ul>
 *   <li><strong>OWNED → {@code @authz.hasAccessToClass(#classId)}:</strong>
 *       updateClass, deleteClass, startClass, completeClass, cancelClass,
 *       rescheduleClass, generateClassCode, createSchedule, generateFromRecurrence
 *       — mutating / lifecycle ops restricted to class teacher (or platform admin)</li>
 *   <li><strong>SHARED READ → tenant-filter only:</strong> getClass, listClasses,
 *       listSessions — readable by any authenticated tenant member (students enrolled,
 *       parents linked, teachers reviewing course catalog). Tightening would block
 *       student/parent flows that legitimately read class info.</li>
 *   <li><strong>CREATE → tenant-filter + service-side teacher binding:</strong>
 *       createClass(courseId) — any tenant member with course-scoped permission may
 *       create; service sets {@code classes.teacher_id = X-User-Id} so subsequent
 *       OWNED ops correctly attribute ownership. Tighter course-ownership guard
 *       deferred (would require {@code hasAccessToCourse} helper, GAP-837 follow-up).</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    // =========================================================================
    // Flat tenant-scoped list under /classes
    // =========================================================================

    /**
     * Lists all classes for the current tenant with pagination (flat list).
     *
     * <p>Backs the Owner dashboard. Classified <strong>SHARED READ</strong> per the
     * controller-level OWASP A01 note — readable by any authenticated tenant member;
     * tenant isolation is enforced by the Hibernate {@code tenantFilter} so no
     * cross-tenant leak is possible. Mirrors the course-scoped {@link #listClasses}.
     *
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @param sort sort criteria "field,direction" (default "createdAt,desc")
     * @return 200 OK with paginated class list scoped to the current tenant
     */
    @GetMapping("/api/v1/classes")
    public ResponseEntity<ApiResponse<PageResponse<ClassResponse>>> listAllClasses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        log.debug("GET /api/v1/classes?page={}&size={}&sort={}", page, size, sort);

        // Parse "field,direction" — derived JPQL query sorts on entity property names
        // (camelCase), so do NOT convert to snake_case.
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        PageResponse<ClassResponse> response = classService.listAllClasses(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =========================================================================
    // CRUD under /courses/{courseId}/classes
    // =========================================================================

    /**
     * Creates a new class within a course.
     *
     * @param courseId course ID
     * @param request  class creation data
     * @return 201 Created with class response
     */
    @PostMapping("/api/v1/courses/{courseId}/classes")
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateClassRequest request) {
        log.debug("POST /api/v1/courses/{}/classes", courseId);
        ClassResponse response = classService.createClass(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo lớp học thành công"));
    }

    /**
     * Lists classes for a course with pagination.
     *
     * @param courseId course ID
     * @param page     page number (default 0)
     * @param size     page size (default 20)
     * @return 200 OK with paginated class list
     */
    @GetMapping("/api/v1/courses/{courseId}/classes")
    public ResponseEntity<ApiResponse<PageResponse<ClassResponse>>> listClasses(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/v1/courses/{}/classes", courseId);
        PageResponse<ClassResponse> response = classService.listClasses(courseId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =========================================================================
    // Class operations under /classes/{classId}
    // =========================================================================

    /**
     * Gets class details by ID.
     *
     * @param classId class ID
     * @return 200 OK with class response
     */
    @GetMapping("/api/v1/classes/{classId}")
    public ResponseEntity<ApiResponse<ClassResponse>> getClass(
            @PathVariable Long classId) {
        log.debug("GET /api/v1/classes/{}", classId);
        return ResponseEntity.ok(ApiResponse.success(classService.getClass(classId)));
    }

    /**
     * Updates a class.
     *
     * <p><strong>OWASP A01 per-resource guard (GAP-837):</strong> mutating a
     * class is an OWNED operation — only the class's teacher (or platform admin)
     * may update. Tenant filter alone would let a teacher in tenant A modify a
     * class owned by another teacher within the same tenant.
     *
     * @param classId class ID
     * @param request fields to update
     * @return 200 OK with updated class
     */
    @PatchMapping("/api/v1/classes/{classId}")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
            @PathVariable Long classId,
            @Valid @RequestBody UpdateClassRequest request) {
        log.debug("PATCH /api/v1/classes/{}", classId);
        return ResponseEntity.ok(ApiResponse.success(
                classService.updateClass(classId, request), "Cập nhật lớp học thành công"));
    }

    /**
     * Deletes a class (SCHEDULED with 0 enrollments only).
     *
     * @param classId class ID
     * @return 204 No Content
     */
    @DeleteMapping("/api/v1/classes/{classId}")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<Void> deleteClass(@PathVariable Long classId) {
        log.debug("DELETE /api/v1/classes/{}", classId);
        classService.deleteClass(classId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Lifecycle transitions
    // =========================================================================

    /**
     * Starts a class (SCHEDULED → IN_PROGRESS).
     *
     * @param classId class ID
     * @return 200 OK with updated class
     */
    @PostMapping("/api/v1/classes/{classId}/start")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<ClassResponse>> startClass(
            @PathVariable Long classId) {
        log.debug("POST /api/v1/classes/{}/start", classId);
        return ResponseEntity.ok(ApiResponse.success(
                classService.startClass(classId), "Lớp học đã bắt đầu"));
    }

    /**
     * Completes a class (IN_PROGRESS → COMPLETED).
     *
     * @param classId class ID
     * @return 200 OK with updated class
     */
    @PostMapping("/api/v1/classes/{classId}/complete")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<ClassResponse>> completeClass(
            @PathVariable Long classId) {
        log.debug("POST /api/v1/classes/{}/complete", classId);
        return ResponseEntity.ok(ApiResponse.success(
                classService.completeClass(classId), "Lớp học đã hoàn thành"));
    }

    /**
     * Cancels a class (SCHEDULED/IN_PROGRESS → CANCELLED).
     *
     * @param classId class ID
     * @param request cancellation reason
     * @return 200 OK with updated class
     */
    @PostMapping("/api/v1/classes/{classId}/cancel")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<ClassResponse>> cancelClass(
            @PathVariable Long classId,
            @Valid @RequestBody CancelClassRequest request) {
        log.debug("POST /api/v1/classes/{}/cancel", classId);
        return ResponseEntity.ok(ApiResponse.success(
                classService.cancelClass(classId, request), "Lớp học đã bị hủy"));
    }

    /**
     * Reschedules a class (preserves status SCHEDULED; mutates dates + writes audit + publishes Outbox event).
     *
     * <p>Wave beta-readiness-4 Bucket D — GAP-291. Reschedule notification classification = OPERATIONAL
     * (bypass marketing_consented gate per cross-bucket LOCKED decision §3.6).
     *
     * @param classId class ID
     * @param request newStartDate + newEndDate + mandatory reasonCategory + optional notes
     * @return 200 OK with updated class
     */
    @PostMapping("/api/v1/classes/{classId}/reschedule")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<ClassResponse>> rescheduleClass(
            @PathVariable Long classId,
            @Valid @RequestBody RescheduleClassRequest request) {
        log.debug("POST /api/v1/classes/{}/reschedule", classId);
        return ResponseEntity.ok(ApiResponse.success(
                classService.rescheduleClass(classId, request),
                "Đã đổi lịch lớp học thành công"));
    }

    // =========================================================================
    // Class code
    // =========================================================================

    /**
     * Generates or regenerates a class enrollment code.
     *
     * @param classId class ID
     * @param request optional custom code and expiry
     * @return 200 OK with code response
     */
    @PostMapping("/api/v1/classes/{classId}/generate-code")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<ClassCodeResponse>> generateClassCode(
            @PathVariable Long classId,
            @Valid @RequestBody GenerateClassCodeRequest request) {
        log.debug("POST /api/v1/classes/{}/generate-code", classId);
        return ResponseEntity.ok(ApiResponse.success(
                classService.generateClassCode(classId, request), "Mã lớp học đã được tạo"));
    }

    // =========================================================================
    // Schedule & Sessions
    // =========================================================================

    /**
     * Creates a class schedule and generates sessions.
     *
     * @param classId class ID
     * @param request schedule configuration (days, times)
     * @return 201 Created with generated sessions
     */
    @PostMapping("/api/v1/classes/{classId}/schedule")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<List<ClassSessionResponse>>> createSchedule(
            @PathVariable Long classId,
            @Valid @RequestBody CreateScheduleRequest request) {
        log.debug("POST /api/v1/classes/{}/schedule", classId);
        List<ClassSessionResponse> sessions = classService.createSchedule(classId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sessions,
                        "Đã tạo " + sessions.size() + " buổi học"));
    }

    /**
     * Lists all sessions for a class.
     *
     * @param classId class ID
     * @return 200 OK with ordered session list
     */
    @GetMapping("/api/v1/classes/{classId}/sessions")
    public ResponseEntity<ApiResponse<List<ClassSessionResponse>>> listSessions(
            @PathVariable Long classId) {
        log.debug("GET /api/v1/classes/{}/sessions", classId);
        return ResponseEntity.ok(ApiResponse.success(classService.listSessions(classId)));
    }

    /**
     * Generates {@link ClassSessionResponse} entries from a structured RFC 5545
     * RRULE subset (GAP-290 Wave 18a).
     *
     * <p>Idempotent on edit per BR-CLASS-009 — past or attended sessions preserved,
     * future {@code SCHEDULED} sessions regenerated.
     *
     * @param classId class ID
     * @param rule    recurrence rule (Phase 1: WEEKLY only)
     * @return 200 OK with merged session list (preserved + new) ordered by sessionNumber
     * @since GAP-290 Wave 18a
     */
    @PostMapping("/api/v1/classes/{classId}/sessions/generate-from-recurrence")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<List<ClassSessionResponse>>> generateFromRecurrence(
            @PathVariable Long classId,
            @Valid @RequestBody RecurrenceRuleDto rule) {
        log.debug("POST /api/v1/classes/{}/sessions/generate-from-recurrence", classId);
        List<ClassSessionResponse> sessions = classService.generateSessionsFromRecurrence(classId, rule);
        return ResponseEntity.ok(ApiResponse.success(sessions,
                "Đã tạo " + sessions.size() + " buổi học (lịch lặp lại)"));
    }
}
