package com.kiteclass.core.module.clazz.dto;

import com.kiteclass.core.module.clazz.RescheduleReasonCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for rescheduling a class (Wave beta-readiness-4 Bucket D — GAP-291).
 *
 * <p>Per cross-bucket LOCKED decision §3.6:
 * <ul>
 *   <li>Reschedule preserves attendance + grade history (NO new ClassStatus)</li>
 *   <li>reasonCategory is MANDATORY (FE form dropdown enforces)</li>
 *   <li>reasonNotes optional, max 2000 chars</li>
 * </ul>
 *
 * @param newStartDate    New start date (required, must be ≥ today)
 * @param newEndDate      New end date (required, must be > newStartDate)
 * @param reasonCategory  Reason for reschedule (required, dropdown)
 * @param reasonNotes     Optional free-text notes (max 2000 chars)
 *
 * @author KiteClass Team
 * @since Wave beta-readiness-4 Bucket D (GAP-291)
 */
public record RescheduleClassRequest(

        @NotNull(message = "Ngày bắt đầu mới không được để trống")
        LocalDate newStartDate,

        @NotNull(message = "Ngày kết thúc mới không được để trống")
        LocalDate newEndDate,

        @NotNull(message = "Vui lòng chọn lý do đổi lịch")
        RescheduleReasonCategory reasonCategory,

        @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự")
        String reasonNotes
) {
}
