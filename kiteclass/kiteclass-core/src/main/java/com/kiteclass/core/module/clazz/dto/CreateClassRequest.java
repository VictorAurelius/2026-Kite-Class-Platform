package com.kiteclass.core.module.clazz.dto;

import com.kiteclass.core.module.clazz.entity.Class.LocationType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Request DTO for creating a new class within a course.
 *
 * <p>The courseId is provided via path variable in the controller,
 * not included in the request body.
 *
 * @param name          Class name (5-200 chars, required)
 * @param description   Class description (optional, max 2000 chars)
 * @param schedule      Schedule text (optional, max 200 chars)
 * @param locationType  IN_PERSON or ONLINE (optional, defaults to IN_PERSON)
 * @param locationDetail Room or URL (optional, max 200 chars)
 * @param startDate     Start date (optional)
 * @param endDate       End date (optional, must be after startDate)
 * @param maxStudents   Max students (required, 1-500)
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public record CreateClassRequest(

        @NotBlank(message = "Tên lớp học không được để trống")
        @Size(min = 5, max = 200, message = "Tên lớp học phải từ 5 đến 200 ký tự")
        String name,

        @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
        String description,

        @Size(max = 200, message = "Lịch học không được vượt quá 200 ký tự")
        String schedule,

        LocationType locationType,

        @Size(max = 200, message = "Địa điểm không được vượt quá 200 ký tự")
        String locationDetail,

        // GAP-1423: classes.start_date is NOT NULL — validate here so a missing date
        // returns 400 (clear field error) instead of a DB constraint violation → 500.
        @NotNull(message = "Ngày bắt đầu không được để trống")
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Số học sinh tối đa không được để trống")
        @Min(value = 1, message = "Số học sinh tối đa phải ít nhất là 1")
        @Max(value = 500, message = "Số học sinh tối đa không được vượt quá 500")
        Integer maxStudents
) {
}
