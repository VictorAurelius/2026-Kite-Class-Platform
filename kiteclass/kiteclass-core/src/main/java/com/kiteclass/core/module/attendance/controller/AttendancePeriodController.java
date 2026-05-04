package com.kiteclass.core.module.attendance.controller;

import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.service.AttendancePeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only REST controller for K-12 per-period attendance.
 *
 * <p>Phase 1A surface only — write endpoints (POST/PATCH/DELETE) ship in
 * GAP-323b along with idempotency, GVCN mobile UI, and concurrent
 * load-test coverage.
 *
 * @since GAP-323 Phase 1A (Wave 18b1)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/attendance/periods")
@RequiredArgsConstructor
@Tag(name = "Attendance — Period (K-12)",
     description = "Per-period attendance read-only API (Phase 1A, GAP-323).")
public class AttendancePeriodController {

    private final AttendancePeriodService service;

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
    @Operation(summary = "Daily roster: all periods + all students for a class on one date.",
            description = "Daily roll-up (vắng cả ngày = vắng ≥7 tiết) deferred to GAP-323b.")
    public ResponseEntity<List<AttendancePeriodResponse>> findByClassAndDate(
            @Parameter(description = "Class ID") @PathVariable Long classId,
            @Parameter(description = "Lesson date (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("GET /api/v1/attendance/periods/classes/{} date={}", classId, date);
        return ResponseEntity.ok(service.findByClassAndDate(classId, date));
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
