package com.kiteclass.core.module.attendance.dto;

import com.kiteclass.core.common.constant.AttendanceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single entry inside the class-overview batch save request.
 *
 * <p>Compared to {@link AttendancePeriodCreateRequest}, this entry omits
 * {@code classId} and {@code date} because both are carried at the request
 * envelope level (URL path + query). The result is a more compact payload
 * for the teacher overview UI which marks attendance for one class on one
 * date across all 1-10 periods at once.
 *
 * @since GAP-268a (Wave 51 Bucket B)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassBatchAttendanceEntry {

    @NotNull
    private Long studentId;

    @NotNull
    private Long subjectSectionId;

    /**
     * Tiết number 1..10 per TT 22/2021/TT-BGDĐT (V51 enforces at DB level).
     */
    @NotNull
    @Min(1)
    @Max(10)
    private Integer periodNo;

    @NotNull
    private AttendanceStatus status;

    @Size(max = 500)
    private String notes;
}
