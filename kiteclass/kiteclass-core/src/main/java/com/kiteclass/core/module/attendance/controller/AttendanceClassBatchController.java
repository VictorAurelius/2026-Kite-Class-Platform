package com.kiteclass.core.module.attendance.controller;

import com.kiteclass.core.common.context.UserContext;
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
     * Resolve the recording teacher's numeric id from the authenticated principal
     * (gateway-injected {@code X-User-Reference-Id} → {@link UserContext}), NOT from any
     * client-supplied header (GAP-1300). Returns {@code null} for OWNER/ADMIN/STAFF.
     *
     * @return the authenticated teacher's reference id, or {@code null} for admin/owner/staff
     */
    private Long actingTeacherId() {
        return UserContext.getCurrentReferenceId();
    }

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
     * <p>Authorization: the recording teacher / GVCN is derived from the authenticated
     * principal ({@code X-User-Reference-Id} → {@link UserContext}), NOT a client-supplied
     * header (GAP-1300). Per-class access is enforced by
     * {@code @authz.hasAccessToClass(#classId)}.
     */
    @PostMapping("/{classId}/batch")
    @PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToClass(#classId)")
    @Operation(summary = "Save attendance for one class on one date in one round-trip",
            description = "GAP-268a: collapses 10 tiết × N students grid into one POST. "
                    + "Idempotent — resubmitting updates the same rows. "
                    + "Wave 105 Bucket C: per-class authz via @authz.hasAccessToClass(#classId) "
                    + "— teacher not assigned to classId returns 403 (OWASP A01 guard). "
                    + "GAP-1300: recording teacher from X-User-Reference-Id, not client header.")
    public ResponseEntity<List<AttendancePeriodResponse>> saveClassBatch(
            @Parameter(description = "Target class ID") @PathVariable Long classId,
            @Parameter(description = "Lesson date (ISO-8601)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody ClassBatchAttendanceRequest request) {

        Long teacherId = actingTeacherId();
        log.info("POST /api/v1/attendance/class/{}/batch date={} cells={} teacher={}",
                classId, date, request.getEntries().size(), teacherId);
        List<AttendancePeriodResponse> out =
                service.upsertClassBatch(classId, date, request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }
}
