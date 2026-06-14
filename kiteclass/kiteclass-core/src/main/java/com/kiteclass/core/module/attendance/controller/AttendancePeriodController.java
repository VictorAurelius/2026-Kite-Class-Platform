package com.kiteclass.core.module.attendance.controller;

import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodBatchCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodUpdateRequest;
import com.kiteclass.core.module.attendance.dto.DailyAttendanceRollupResponse;
import com.kiteclass.core.module.attendance.service.AttendancePeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for K-12 per-period attendance.
 *
 * <p>Phase 1A (Wave 18b1) shipped four read-only GETs. Phase 1B (Wave 18b2,
 * GAP-323b) adds idempotent batch upsert, single-row PATCH with optimistic
 * lock, and an on-demand daily roll-up endpoint. Mobile UI, offline queue,
 * and the load-test rig are tracked separately in GAP-323b §1B.2-1B.4.
 *
 * <p><strong>GAP-1300 — recording-teacher authz hardening.</strong> The write
 * endpoints ({@code upsertBatch}, {@code update}) are (1) role-gated so STUDENT/PARENT
 * are blocked entirely, and (2) derive the recording teacher ({@code recordedBy}) from
 * the authenticated principal (gateway-injected {@code X-User-Reference-Id} →
 * {@link UserContext#getCurrentReferenceId()}). The former client-supplied
 * {@code X-Teacher-Id} request header — which the gateway does NOT control (per GAP-814)
 * and was therefore spoofable — is no longer read as an identity source, so a caller can
 * no longer attribute attendance records to an arbitrary teacher. {@code recorded_by} is a
 * NOT NULL column identifying the recording GVCN / bộ môn, so the realistic recorder is a
 * TEACHER carrying a numeric reference id; OWNER/ADMIN/STAFF (no reference id) are gated in
 * for defense-in-depth but do not record per-period attendance in normal operation.
 *
 * @since GAP-323 Phase 1A (Wave 18b1); Phase 1B GAP-323b (Wave 18b2)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/attendance/periods")
@RequiredArgsConstructor
@Tag(name = "Attendance — Period (K-12)",
     description = "Per-period attendance API for K-12 schools (GAP-323 / GAP-323b).")
public class AttendancePeriodController {

    private final AttendancePeriodService service;

    /**
     * Resolve the recording teacher's numeric id from the authenticated principal
     * (gateway-injected {@code X-User-Reference-Id} → {@link UserContext}), NOT from any
     * client-supplied header (GAP-1300). Returns {@code null} for OWNER/ADMIN/STAFF, who
     * carry no numeric reference id.
     *
     * @return the authenticated teacher's reference id, or {@code null} for admin/owner/staff
     */
    private Long actingTeacherId() {
        return UserContext.getCurrentReferenceId();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single per-period attendance record by ID.")
    public ResponseEntity<AttendancePeriodResponse> findById(
            @Parameter(description = "Period attendance row ID") @PathVariable Long id) {
        log.debug("GET /api/v1/attendance/periods/{}", id);
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Page attendance for a student in a date range.",
            description = "Used by parent portal feeds and student attendance history.")
    public ResponseEntity<Page<AttendancePeriodResponse>> findByStudent(
            @Parameter(description = "Student ID") @PathVariable Long studentId,
            @Parameter(description = "From date (inclusive, ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "To date (inclusive, ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50, sort = {"date", "periodNo"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("GET /api/v1/attendance/periods/students/{} from={} to={}",
                studentId, from, to);
        return ResponseEntity.ok(service.findByStudent(studentId, from, to, pageable));
    }

    @GetMapping("/classes/{classId}")
    @PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToClass(#classId)")
    @Operation(summary = "Daily roster: all periods + all students for a class on one date.",
            description = "Daily roll-up (vắng cả ngày = vắng ≥7 tiết) deferred to GAP-323b. "
                    + "Wave 105 Bucket C: per-class authz guard (OWASP A01).")
    public ResponseEntity<List<AttendancePeriodResponse>> findByClassAndDate(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @Parameter(description = "Lesson date (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("GET /api/v1/attendance/periods/classes/{} date={}", classId, date);
        return ResponseEntity.ok(service.findByClassAndDate(classId, date));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')")
    @Operation(summary = "Idempotent batch upsert of per-period attendance.",
            description = "Records attendance for a list of (student, subject section, "
                    + "date, period_no) tuples. Resubmitting the same batch updates "
                    + "rather than duplicates rows (V50 unique index). The recording GVCN / "
                    + "bộ môn is derived from the authenticated principal "
                    + "(X-User-Reference-Id), NOT a client header (GAP-1300).")
    public ResponseEntity<List<AttendancePeriodResponse>> upsertBatch(
            @Valid @RequestBody AttendancePeriodBatchCreateRequest request) {
        Long teacherId = actingTeacherId();
        log.info("POST /api/v1/attendance/periods - batch={} by teacher={}",
                request.getEntries().size(), teacherId);
        List<AttendancePeriodResponse> out = service.upsertBatch(request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')")
    @Operation(summary = "Update a single period-attendance row.",
            description = "Optimistic-lock conflict (stale version) returns HTTP 409 "
                    + "with code OPTIMISTIC_LOCK_CONFLICT. The recording teacher is derived "
                    + "from the authenticated principal (X-User-Reference-Id), NOT a client "
                    + "header (GAP-1300).")
    public ResponseEntity<AttendancePeriodResponse> update(
            @Parameter(description = "Row ID") @PathVariable Long id,
            @Valid @RequestBody AttendancePeriodUpdateRequest request) {
        Long teacherId = actingTeacherId();
        log.info("PATCH /api/v1/attendance/periods/{} by teacher={}", id, teacherId);
        return ResponseEntity.ok(service.update(id, request, teacherId));
    }

    @GetMapping("/daily-rollup")
    @PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToClass(#classId)")
    @Operation(summary = "Daily roll-up across one class for a date range.",
            description = "Per-(student, date) counts plus the boolean allDayAbsent "
                    + "(absent + late ≥ 7 tiết per TT 22/2021/TT-BGDĐT). Phase 1B v1 "
                    + "is on-demand aggregation; the materialized-view path described "
                    + "in GAP-323b §1B.4 is deferred to a follow-up PR. "
                    + "Wave 105 Bucket C: per-class authz guard (OWASP A01).")
    public ResponseEntity<List<DailyAttendanceRollupResponse>> dailyRollup(
            @Parameter(description = "Class ID", required = true)
            @RequestParam Long classId,
            @Parameter(description = "From date (inclusive, ISO-8601)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "To date (inclusive, ISO-8601)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.debug("GET /api/v1/attendance/periods/daily-rollup classId={} from={} to={}",
                classId, from, to);
        return ResponseEntity.ok(service.dailyRollupForClass(classId, from, to));
    }

    @GetMapping("/subject-sections/{subjectSectionId}")
    @Operation(summary = "Page attendance for a SubjectSection in a date range.",
            description = "Bộ môn (subject teacher) review surface — references "
                    + "SubjectSection from GAP-054 Phase 1.")
    public ResponseEntity<Page<AttendancePeriodResponse>> findBySubjectSection(
            @Parameter(description = "SubjectSection ID")
            @PathVariable Long subjectSectionId,
            @Parameter(description = "From date (inclusive, ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "To date (inclusive, ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50, sort = {"date", "periodNo"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("GET /api/v1/attendance/periods/subject-sections/{} from={} to={}",
                subjectSectionId, from, to);
        return ResponseEntity.ok(service.findBySubjectSection(
                subjectSectionId, from, to, pageable));
    }
}
