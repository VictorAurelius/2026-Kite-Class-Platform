package com.kiteclass.core.module.clazz.dto;

import com.kiteclass.core.module.clazz.entity.Class.LocationType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Request DTO for updating an existing class.
 *
 * <p>All fields are optional (partial update pattern).
 * Business rules apply depending on class status:
 * <ul>
 *   <li>SCHEDULED: All fields editable</li>
 *   <li>IN_PROGRESS: Only description, locationDetail editable; schedule/dates locked</li>
 *   <li>COMPLETED/CANCELLED: Read-only, no updates allowed</li>
 * </ul>
 *
 * @param name          New class name (optional, 5-200 chars)
 * @param description   New description (optional, max 2000 chars)
 * @param schedule      New schedule text (SCHEDULED only)
 * @param locationType  New location type (SCHEDULED only)
 * @param locationDetail New location detail (SCHEDULED and IN_PROGRESS)
 * @param startDate     New start date (SCHEDULED only)
 * @param endDate       New end date (SCHEDULED only)
 * @param maxStudents   New max students (>= current_enrolled)
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public record UpdateClassRequest(

        @Size(min = 5, max = 200, message = "Tên lớp học phải từ 5 đến 200 ký tự")
        String name,

        @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
        String description,

        @Size(max = 200, message = "Lịch học không được vượt quá 200 ký tự")
        String schedule,

        LocationType locationType,

        @Size(max = 200, message = "Địa điểm không được vượt quá 200 ký tự")
        String locationDetail,

        LocalDate startDate,

        LocalDate endDate,

        @Min(value = 1, message = "Số học sinh tối đa phải ít nhất là 1")
        @Max(value = 500, message = "Số học sinh tối đa không được vượt quá 500")
        Integer maxStudents
) {
}
