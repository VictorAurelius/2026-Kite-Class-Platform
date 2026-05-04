package com.kiteclass.core.module.attendance.dto;

import com.kiteclass.core.common.constant.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update request for a single per-period attendance row (PATCH).
 *
 * <p>{@code version} is required so JPA's {@code @Version} optimistic-lock
 * mechanism can reject stale writes — concurrent GVCN edits in the same period
 * window resolve via "first save wins, second save 409s with current state".
 *
 * @since GAP-323b Phase 1B (Wave 18b2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendancePeriodUpdateRequest {

    @NotNull
    private AttendanceStatus status;

    @Size(max = 500)
    private String notes;

    /**
     * Version of the row the client read. Stale value triggers
     * {@link org.springframework.dao.OptimisticLockingFailureException}
     * which the controller advice translates into HTTP 409.
     */
    @NotNull
    private Long version;
}
