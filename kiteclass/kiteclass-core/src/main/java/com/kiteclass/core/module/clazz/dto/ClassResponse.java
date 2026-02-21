package com.kiteclass.core.module.clazz.dto;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.module.clazz.entity.Class.LocationType;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for Class entity.
 *
 * @param id              Class ID
 * @param courseId        Parent course ID
 * @param name            Class name
 * @param description     Class description
 * @param schedule        Schedule text
 * @param locationType    IN_PERSON or ONLINE
 * @param locationDetail  Room or URL
 * @param startDate       Start date
 * @param endDate         End date
 * @param maxStudents     Maximum students
 * @param currentEnrolled Current enrolled count
 * @param classCode       Self-enrollment code
 * @param codeExpiresAt   Code expiry time
 * @param status          Current lifecycle status
 * @param startedAt       When class started
 * @param completedAt     When class completed
 * @param cancelledAt     When class cancelled
 * @param createdAt       Creation timestamp
 * @param updatedAt       Last update timestamp
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
public record ClassResponse(
        Long id,
        Long courseId,
        String name,
        String description,
        String schedule,
        LocationType locationType,
        String locationDetail,
        LocalDate startDate,
        LocalDate endDate,
        Integer maxStudents,
        Integer currentEnrolled,
        String classCode,
        Instant codeExpiresAt,
        ClassStatus status,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {
}
