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
 * Body wrapper for {@code POST /api/v1/attendance/class/{classId}/batch}
 * (GAP-268a class-overview save).
 *
 * <p>The {@code classId} and {@code date} arrive on the request envelope
 * (URL path + query string) so the body carries only per-cell attendance
 * cells. Capacity ≤ 200 leaves headroom for multi-period × multi-student
 * grids (e.g. 10 tiết × 20 students = 200 cells).
 *
 * @since GAP-268a (Wave 51 Bucket B)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassBatchAttendanceRequest {

    @NotEmpty
    @Size(max = 200)
    @Valid
    private List<ClassBatchAttendanceEntry> entries;
}
