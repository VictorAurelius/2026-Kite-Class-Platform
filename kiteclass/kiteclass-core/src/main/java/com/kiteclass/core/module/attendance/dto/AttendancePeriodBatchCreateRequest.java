package com.kiteclass.core.module.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch wrapper for {@link AttendancePeriodCreateRequest} — typically one full
 * class (≤42 students) for one tiết. The upper bound of 60 leaves headroom for
 * combined bộ môn classes; values above the cap are rejected at the controller
 * boundary so the upsert loop has a predictable worst case.
 *
 * <p>Idempotency: the service upserts per (studentId, subjectSectionId, date,
 * periodNo, instance_id) so resubmitting the same batch updates rather than
 * duplicates rows. The DB-level unique index is the backstop.
 *
 * @since GAP-323b Phase 1B (Wave 18b2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePeriodBatchCreateRequest {

    @NotEmpty
    @Size(max = 60)
    @Valid
    private List<AttendancePeriodCreateRequest> entries;
}
