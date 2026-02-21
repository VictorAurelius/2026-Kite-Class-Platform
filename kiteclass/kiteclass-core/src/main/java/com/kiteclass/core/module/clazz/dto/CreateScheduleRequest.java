package com.kiteclass.core.module.clazz.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Request DTO for creating a class schedule and generating sessions.
 *
 * <p>The system will generate ClassSession records for all matching
 * days between the class's startDate and endDate.
 *
 * @param daysOfWeek  Days of the week when class meets (required, at least 1)
 * @param startTime   Session start time (required)
 * @param endTime     Session end time (required, must be after startTime)
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public record CreateScheduleRequest(

        @NotEmpty(message = "Phải chọn ít nhất 1 ngày học trong tuần")
        List<DayOfWeek> daysOfWeek,

        @NotNull(message = "Giờ bắt đầu không được để trống")
        LocalTime startTime,

        @NotNull(message = "Giờ kết thúc không được để trống")
        LocalTime endTime
) {
}
