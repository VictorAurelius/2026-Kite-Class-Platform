package com.kiteclass.core.module.attendance.controller;

import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.dto.ClassBatchAttendanceRequest;
import com.kiteclass.core.module.attendance.service.AttendancePeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Class-overview attendance save endpoint (GAP-268a, Wave 51 Bucket B).
 *
 * <p>Convenience companion to {@link AttendancePeriodController} that lets the
 * teacher overview UI {@code (teacher)/teacher/attendance/[classId]} commit
 * an entire 1-10 tiết grid for one class on one date in a single round-trip.
 * Body contains per-cell entries (studentId / subjectSectionId / periodNo /
 * status / notes); {@code classId} and {@code date} are carried on the URL.
 *
 * <p>Implementation delegates to
 * {@link AttendancePeriodService#upsertClassBatch} which folds the class-level
 * fields into the existing per-row upsert path. The DB unique index
 * (student × subjectSection × date × periodNo) backs idempotency, so
 * resubmitting the same body updates rather than duplicates rows.
 *
 * @author KiteClass Team
 * @since GAP-268a (Wave 51 Bucket B)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/attendance/class")
@RequiredArgsConstructor
@Tag(name = "Attendance — Class Batch (GAP-268a)",
        description = "Class-overview save companion to per-tiết upsert")
public class AttendanceClassBatchController {

    private final AttendancePeriodService service;

    /**
     * Upserts attendance for one class on one date across all submitted
     * (period × student) cells in a single request.
     *
     * <p>Returns:
     * <ul>
     *   <li>201 + the upserted rows (entry order preserved) — happy path.</li>
     *   <li>400 + validation envelope when entries empty / period out of 1..10
     *       / batch &gt; 200 cells.</li>
     *   <li>409 — pass-through if a concurrent {@code @Version} bump beats
     *       this request (handled by the global handler).</li>
     * </ul>
     *
     * <p>Authorization: {@code X-Teacher-Id} header carries the recording
     * teacher / GVCN. Class-access policy is enforced by the underlying
     * upsert path (FK constraints + tenant scoping).
     */
    @PostMapping("/{classId}/batch")
    @PreAuthorize("@authz.hasAccessToClass(#classId)")
    @Operation(summary = "Save attendance for one class on one date in one round-trip",
            description = "GAP-268a: collapses 10 tiết × N students grid into one POST. "
                    + "Idempotent — resubmitting updates the same rows. "
                    + "Wave 105 Bucket C: per-class authz via @authz.hasAccessToClass(#classId) "
                    + "— teacher not assigned to classId returns 403 (OWASP A01 guard).")
    public ResponseEntity<List<AttendancePeriodResponse>> saveClassBatch(
            @Parameter(description = "Target class ID") @PathVariable Long classId,
            @Parameter(description = "Lesson date (ISO-8601)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody ClassBatchAttendanceRequest request,
            @Parameter(description = "Recording teacher / GVCN user ID", required = true)
            @RequestHeader("X-Teacher-Id") Long teacherId) {

        log.info("POST /api/v1/attendance/class/{}/batch date={} cells={} teacher={}",
                classId, date, request.getEntries().size(), teacherId);
        List<AttendancePeriodResponse> out =
                service.upsertClassBatch(classId, date, request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }
}
